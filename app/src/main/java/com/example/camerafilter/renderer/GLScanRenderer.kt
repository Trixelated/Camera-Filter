package com.example.camerafilter.renderer

import android.opengl.GLES20
import com.example.camerafilter.gl_utils.ShaderUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GLScanRenderer {

    private var programId = -1
    private var aPositionHandle = 0
    private var sTextureSamplerHandle = 0
    private var uTextureSamplerHandle = 0
    private var aTextureCoordHandle = 0
    private var scanHeightHandle = 0

    private val bos = IntArray(2)
    private val textures = IntArray(2)
    private val frameBuffers = IntArray(2)


    fun initShader() {
        val fragmentShader = """varying highp vec2 vTexCoord;
uniform sampler2D sTexture;
uniform sampler2D uTexture;
uniform highp float scanHeight;
void main() {
   highp float fy = 1.0 - vTexCoord.y;
   if(fy > scanHeight){       highp vec4 rgba = texture2D(sTexture , vTexCoord);
       gl_FragColor = rgba;
   }else{       highp vec4 rgba = texture2D(uTexture , vTexCoord);
       gl_FragColor = rgba;
   }
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
        sTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture")
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "uTexture")
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord")
        scanHeightHandle = GLES20.glGetUniformLocation(programId, "scanHeight")
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
        for (i in textures.indices) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i])
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
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glGenFramebuffers(frameBuffers.size, frameBuffers, 0)
    }

    private var width = 0;
    private  var height:Int = 0
    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0])
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
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
            textures[0], 0
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[1])
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

    private var textureIndex = 0
    fun drawFrame(texture: Int, scanHeight: Float) {
        val index = textureIndex
        textureIndex = (index + 1) % 2
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[textureIndex])
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(programId)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glUniform1i(sTextureSamplerHandle, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[index])
        GLES20.glUniform1i(uTextureSamplerHandle, 1)
        GLES20.glUniform1f(scanHeightHandle, scanHeight)
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

    fun getTexture(): Int {
        return textures[textureIndex]
    }

    fun release() {
        GLES20.glDeleteProgram(programId)
        GLES20.glDeleteFramebuffers(frameBuffers.size, frameBuffers, 0)
        GLES20.glDeleteTextures(textures.size, textures, 0)
        GLES20.glDeleteBuffers(bos.size, bos, 0)
    }

}