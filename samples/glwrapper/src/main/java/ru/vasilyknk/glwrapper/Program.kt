package ru.vasilyknk.glwrapper

import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log

class Program : Resource {
    private val id = ResourceId(null)

    fun init(vertexShaderCode: String, fragmentShaderCode: String) {
        val newId = GLES20.glCreateProgram()

        GLES20.glAttachShader(newId, loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode))
        GLES20.glAttachShader(newId, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode))
        GLES20.glLinkProgram(newId)
        GLES20.glUseProgram(newId)

        id.set(newId)
    }

    fun use() {
        GLES20.glUseProgram(id.get())
    }

    fun getAttribLocation(name: String) = GLES20.glGetAttribLocation(id.get(), name)
    fun getUniformLocation(name: String) = GLES20.glGetUniformLocation(id.get(), name)

    override fun close() {
        if (id.isValid())
            GLES20.glDeleteProgram(id.get())
    }

    override fun isValid() = id.isValid()
}

private fun loadShader(type: Int, code: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, code)
    GLES20.glCompileShader(shader)

    // Get the compilation status.
    val compileStatus = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

    // If the compilation failed, delete the shader.
    if (compileStatus[0] == 0) {
        Log.e("glwrapper", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader))
        GLES20.glDeleteShader(shader)
        throw RuntimeException("Error creating shader.")
    }

    return shader
}