package com.example.camerafilter.renderer

import android.opengl.GLES20
import com.example.camerafilter.gl_utils.ShaderUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GLRenderer {

    private var programId = -1
    private var aPositionHandle = 0
    private var uTextureSamplerHandle = 0
    private var aTextureCoordHandle = 0

    private val bos = IntArray(2)
    private val textures = IntArray(1)

    fun initShader() {
        val fragmentShader = """varying highp vec2 vTexCoord;
uniform sampler2D sTexture;
void main() {
   highp vec4 rgba = texture2D(sTexture , vTexCoord);
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
        for (texture in textures) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
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
    }

    fun drawFrame(texture: Int) {
        GLES20.glUseProgram(programId)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glUniform1i(uTextureSamplerHandle, 0)
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
    }

    fun release() {
        GLES20.glDeleteProgram(programId)
        GLES20.glDeleteTextures(textures.size, textures, 0)
        GLES20.glDeleteBuffers(bos.size, bos, 0)
    }

}