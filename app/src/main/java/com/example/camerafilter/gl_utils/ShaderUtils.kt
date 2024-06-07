package com.example.camerafilter.gl_utils

import android.opengl.GLES20
import android.util.Log
class  ShaderUtils {

    companion object{
        private const val TAG = "ShaderUtils"

        fun checkGlError(label: String) {
            var error: Int
            while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
                Log.e(ShaderUtils.TAG, "$label: glError $error")
                throw RuntimeException("$label: glError $error")
            }
        }

        fun createProgram(vertexSource: String?, fragmentSource: String?): Int {
            val vertexShader: Int = ShaderUtils.loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            if (vertexShader == 0) {
                return 0
            }
            val pixelShader: Int = ShaderUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            if (pixelShader == 0) {
                return 0
            }
            var program = GLES20.glCreateProgram()
            if (program != 0) {
                GLES20.glAttachShader(program, vertexShader)
                ShaderUtils.checkGlError("glAttachShader")
                GLES20.glAttachShader(program, pixelShader)
                ShaderUtils.checkGlError("glAttachShader")
                GLES20.glLinkProgram(program)
                val linkStatus = IntArray(1)
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    Log.e(TAG, "Could not link program: ")
                    Log.e(TAG, GLES20.glGetProgramInfoLog(program))
                    GLES20.glDeleteProgram(program)
                    program = 0
                }
            }
            return program
        }

        fun loadShader(shaderType: Int, source: String?): Int {
            var shader = GLES20.glCreateShader(shaderType)
            if (shader != 0) {
                GLES20.glShaderSource(shader, source)
                GLES20.glCompileShader(shader)
                val compiled = IntArray(1)
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
                if (compiled[0] == 0) {
                    Log.e(ShaderUtils.TAG, "Could not compile shader $shaderType:")
                    Log.e(ShaderUtils.TAG, GLES20.glGetShaderInfoLog(shader))
                    GLES20.glDeleteShader(shader)
                    shader = 0
                }
            }
            return shader
        }

    }





}