package ru.vasilyknk.glwrapper

import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log
import org.joml.Matrix4fc
import org.joml.Vector3fc
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Program internal constructor(vertexShaderCode: String, fragmentShaderCode: String): Resource {
    private val id = ResourceId(initProgram(vertexShaderCode, fragmentShaderCode))

    companion object {
        private val floatBuffer: FloatBuffer
        init {
            val bb = ByteBuffer.allocateDirect(1024)
            bb.order(ByteOrder.nativeOrder())
            floatBuffer = bb.asFloatBuffer()
        }
    }

    inner class Usage : AutoCloseable {
        init {
            GLES30.glUseProgram(id.get())
        }

        fun setUniform(index: Int, vec: Vector3fc) {
            vec.get(floatBuffer)
            GLES20.glUniform3fv(index, 1, floatBuffer)
        }

        fun setUniform(index: Int, matrix: Matrix4fc, transpose: Boolean) {
            matrix.get(floatBuffer)
            GLES20.glUniformMatrix4fv(index, 1, transpose, floatBuffer)
        }

        override fun close() {}
    }

    fun use(): Usage {
        return Usage()
    }

    fun getAttribLocation(name: String) = GLES30.glGetAttribLocation(id.get(), name)
    fun getUniformLocation(name: String) = GLES30.glGetUniformLocation(id.get(), name)

    override fun close() {
        if (id.isValid())
            GLES30.glDeleteProgram(id.get())
    }

    override fun isValid() = id.isValid()
}

private fun loadShader(type: Int, code: String): Int {
    val shader = GLES30.glCreateShader(type)
    GLES30.glShaderSource(shader, code)
    GLES30.glCompileShader(shader)

    // Get the compilation status.
    val compileStatus = IntArray(1)
    GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)

    // If the compilation failed, delete the shader.
    if (compileStatus[0] == 0) {
        Log.e("glwrapper", "Error compiling shader: " + GLES30.glGetShaderInfoLog(shader))
        GLES30.glDeleteShader(shader)
        throw RuntimeException("Error creating shader.")
    }

    return shader
}

private fun initProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
    val newId = GLES30.glCreateProgram()

    GLES30.glAttachShader(newId, loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode))
    GLES30.glAttachShader(newId, loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode))
    GLES30.glLinkProgram(newId)
    GLES30.glUseProgram(newId)

    return newId
}