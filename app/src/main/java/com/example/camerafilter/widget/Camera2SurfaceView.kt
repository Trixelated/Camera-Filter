package com.example.camerafilter.widget

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.PermissionChecker
import com.example.camerafilter.gl_utils.EGLUtils
import com.example.camerafilter.renderer.GLFilterRenderer
import com.example.camerafilter.renderer.GLRenderer
import com.example.camerafilter.renderer.GLScanRenderer
import com.example.camerafilter.renderer.GLVideoRenderer
import java.util.Collections

class Camera2SurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs) {


    private val mEglUtils: EGLUtils = EGLUtils()
    private val videoRenderer: GLVideoRenderer = GLVideoRenderer()
    private val mRenderer: GLRenderer = GLRenderer()
    private val scanRenderer: GLScanRenderer = GLScanRenderer()
    private val filterRenderer: GLFilterRenderer = GLFilterRenderer()

    private var mCameraId: String? = null
    private var mCameraManager: CameraManager? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null
    private var mCameraDevice: CameraDevice? = null
    private var mHandler: Handler? = null

    private var screenWidth = -1
    private var screenHeight: Int = 0
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0
    private val rect = Rect()

    private var cameraHandler: Handler? = null
    private var cameraThread: HandlerThread? = null

    private var isScan = false
    private var isf = false
    private var scanHeight = 0f
    private var pixelHeight = 0f
    private val speed = 2
    var modeColorFilter = true
        set(value) {
            if (value) {
                isScan = false
            }
            field = value
        }

    fun isScanVideo(): Boolean {
        return isScan
    }

    fun setScanVideo(scan: Boolean) {
        isScan = scan
    }

    init {
        init()
    }

    private fun init() {
        cameraThread = HandlerThread("Camera2Thread")
        cameraThread?.start()
        cameraHandler = Handler(cameraThread!!.looper)
        initCamera2()
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                cameraHandler?.post {
                    mEglUtils.initEGL(holder.surface)
                    GLES20.glEnable(GLES20.GL_BLEND)
                    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
                    mRenderer.initShader()
                    videoRenderer.initShader()
                    scanRenderer.initShader()
                    filterRenderer.initShader()
                    videoRenderer.setOnFrameAvailableListener {
                        cameraHandler?.post(Runnable Run@{
                            if (mCameraCaptureSession == null) {
                                return@Run
                            }

                            videoRenderer.drawFrame()
                            var videoTexture: Int = videoRenderer.getTexture()

                            if (modeColorFilter) {
                                filterRenderer.drawFrame(videoRenderer.getTexture())
                                videoTexture = filterRenderer.getTexture()
                            } else {
                                if (isScan) {
                                    if (!isf) {
                                        scanHeight = pixelHeight * speed
                                    } else {
                                        scanHeight += pixelHeight * speed
                                    }
                                    if (scanHeight < 2.0) {
                                        var fh = scanHeight
                                        if (scanHeight >= 1.0) {
                                            scanHeight = 3.0f
                                            fh = 1.0f
                                        }
                                        scanRenderer.drawFrame(videoRenderer.getTexture(), fh)
                                    }
                                    videoTexture = scanRenderer.getTexture()
                                }
                                isf = isScan
                            }

                            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
                            GLES20.glViewport(rect.left, rect.top, rect.width(), rect.height())
                            mRenderer.drawFrame(videoTexture)
                            mEglUtils.swap()
                        })
                    }
                    if (screenWidth != -1) {
                        openCamera2()
                    }
                }
            }

            override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, w: Int, h: Int) {
                val sw = screenWidth
                screenWidth = w
                screenHeight = h
                cameraHandler?.post {
                    val mPreviewSize: Size =
                        getPreferredPreviewSize(mSizes, screenWidth, screenHeight)
                    previewWidth = mPreviewSize.height
                    previewHeight = mPreviewSize.width
                    pixelHeight = 1.0f / previewHeight
                    val left: Int
                    val top: Int
                    val viewWidth: Int
                    val viewHeight: Int
                    val sh: Float = screenWidth * 1.0f / screenHeight
                    val vh: Float = previewWidth * 1.0f / previewHeight
                    if (sh < vh) {
                        left = 0
                        viewWidth = screenWidth
                        viewHeight = (previewHeight * 1.0f / previewWidth * viewWidth).toInt()
                        top = (screenHeight - viewHeight) / 2
                    } else {
                        top = 0
                        viewHeight = screenHeight
                        viewWidth = (previewWidth * 1.0f / previewHeight * viewHeight).toInt()
                        left = (screenWidth - viewWidth) / 2
                    }
                    rect.left = left
                    rect.top = top
                    rect.right = left + viewWidth
                    rect.bottom = top + viewHeight
                    videoRenderer.setSize(mPreviewSize.width, mPreviewSize.height)
                    scanRenderer.setSize(mPreviewSize.width, mPreviewSize.height)
                    filterRenderer.setSize(mPreviewSize.width, mPreviewSize.height)
                    if (sw == -1) {
                        openCamera2()
                    }
                }
            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                cameraHandler?.post {
                    if (mCameraCaptureSession != null) {
                        mCameraCaptureSession?.getDevice()?.close()
                        mCameraCaptureSession?.close()
                        mCameraCaptureSession = null
                    }
                    GLES20.glDisable(GLES20.GL_BLEND)
                    videoRenderer.release()
                    mRenderer.release()
                    scanRenderer.release()
                    mEglUtils.release()
                }
            }
        })
    }

    private lateinit var mSizes: Array<Size>

    private fun initCamera2() {
        val handlerThread = HandlerThread("CameraThread")
        handlerThread.start()
        mHandler = Handler(handlerThread.looper)
        mCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            assert(mCameraManager != null)
            val CameraIdList = mCameraManager!!.cameraIdList
            mCameraId = CameraIdList[0]
            val characteristics = mCameraManager!!.getCameraCharacteristics(mCameraId!!)
            characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (map != null) {
                mSizes = map.getOutputSizes(SurfaceTexture::class.java)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("WrongConstant")
    private fun openCamera2() {
        if (PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                mCameraManager!!.openCamera(mCameraId!!, stateCallback, mHandler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            takePreview()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            if (mCameraDevice != null) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
        }

        override fun onError(cameraDevice: CameraDevice, i: Int) {}
    }

    private fun takePreview() {
        try {
            val builder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(videoRenderer.getSurface()!!)
            mCameraDevice?.createCaptureSession(
                listOf(videoRenderer.getSurface()),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        if (null == mCameraDevice) return
                        mCameraCaptureSession = cameraCaptureSession
                        builder.set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        )
                        builder.set(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                        )
                        val previewRequest = builder.build()
                        try {
                            mCameraCaptureSession!!.setRepeatingRequest(
                                previewRequest,
                                null,
                                mHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {}
                },
                mHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getPreferredPreviewSize(sizes: Array<Size>, width: Int, height: Int): Size {
        val collectorSizes: MutableList<Size> = ArrayList()
        for (option in sizes) {
            if (width > height) {
                if (option.width > width && option.height > height) {
                    collectorSizes.add(option)
                }
            } else {
                if (option.height > width && option.width > height) {
                    collectorSizes.add(option)
                }
            }
        }

        return if (collectorSizes.size > 0) {
            Collections.min(
                collectorSizes
            ) { s1, s2 -> java.lang.Long.signum((s1.width * s1.height - s2.width * s2.height).toLong()) }
        } else sizes[0]
    }

}