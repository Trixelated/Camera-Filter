package com.example.camerafilter.renderer

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface
import com.example.camerafilter.gl_utils.ShaderUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GLVideoRenderer {

    private var programId = -1
    private var aPositionHandle = 0
    private var uTextureSamplerHandle = 0
    private var aTextureCoordHandle = 0
    private var uSTMatrixHandle = 0


    private val bos = IntArray(2)
    private val textures = IntArray(2)
    private val frameBuffers = IntArray(1)

    private val mSTMatrix = FloatArray(16)

    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    fun initShader() {
        val fragmentShader = """#extension GL_OES_EGL_image_external : require
varying highp vec2 vTexCoord;
uniform samplerExternalOES sTexture;
uniform highp mat4 uSTMatrix;
void main() {
   highp vec2 tx_transformed = (uSTMatrix * vec4(vTexCoord, 0, 1.0)).xy;   highp vec4 rgba = texture2D(sTexture , tx_transformed);
   gl_FragColor = rgba;
}"""

        val vertexShader = """attribute vec4 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;
void main() {
  vTexCoord = aTexCoord;
  gl_Position = aPosition;
}"""
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader)
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture")
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord")
        uSTMatrixHandle = GLES20.glGetUniformLocation(programId, "uSTMatrix")
        val vertexData = floatArrayOf(
            1f, -1f, 0f,
            -1f, -1f, 0f,
            1f, 1f, 0f,
            -1f, 1f, 0f
        )
        val textureVertexData = floatArrayOf(
            1f, 0f,
            0f, 0f,
            1f, 1f,
            0f, 1f
        )
        val vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexData)
        vertexBuffer.position(0)
        val textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(textureVertexData)
        textureVertexBuffer.position(0)
        GLES20.glGenBuffers(2, bos, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertexData.size * 4,
            vertexBuffer,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[1])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            textureVertexData.size * 4,
            textureVertexBuffer,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glGenTextures(textures.size, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1])
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glGenFramebuffers(frameBuffers.size, frameBuffers, 0)
        if (surfaceTexture != null) {
            surfaceTexture!!.release()
        }
        surfaceTexture = SurfaceTexture(textures[0])
        if (surface != null) {
            surface!!.release()
        }
        surface = Surface(surfaceTexture)
    }

    fun setOnFrameAvailableListener(listener: OnFrameAvailableListener?) {
        if (surfaceTexture == null) {
            return
        }
        surfaceTexture!!.setOnFrameAvailableListener(listener)
    }

    fun getSurface(): Surface? {
        return if (surfaceTexture == null) {
            null
        } else surface
    }

    private var width = 0;
    private var height: Int = 0
    fun setSize(width: Int, height: Int) {
        if (surfaceTexture == null) {
            return
        }
        this.width = width
        this.height = height
        surfaceTexture!!.setDefaultBufferSize(width, height)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0])
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1])
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
            textures[1], 0
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun getTexture(): Int {
        return textures[1]
    }

    fun drawFrame() {
        if (surfaceTexture == null) {
            return
        }
        surfaceTexture!!.updateTexImage()
        surfaceTexture!!.getTransformMatrix(mSTMatrix)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0])
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(programId)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glUniform1i(uTextureSamplerHandle, 0)
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, mSTMatrix, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[0])
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(
            aPositionHandle, 3, GLES20.GL_FLOAT, false,
            0, 0
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[1])
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle)
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun release() {
        GLES20.glDeleteProgram(programId)
        GLES20.glDeleteFramebuffers(frameBuffers.size, frameBuffers, 0)
        GLES20.glDeleteTextures(textures.size, textures, 0)
        GLES20.glDeleteBuffers(bos.size, bos, 0)
        if (surfaceTexture != null) {
            surfaceTexture!!.release()
            surfaceTexture = null
        }
        if (surface != null) {
            surface!!.release()
            surface = null
        }
    }

}