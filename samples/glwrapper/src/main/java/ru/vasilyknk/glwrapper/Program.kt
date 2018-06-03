package ru.vasilyknk.glwrapper

import android.opengl.GLES30
import android.util.Log

class Program internal constructor(vertexShaderCode: String, fragmentShaderCode: String): Resource {
    private val id = ResourceId(initProgram(vertexShaderCode, fragmentShaderCode))

    fun use() {
        GLES30.glUseProgram(id.get())
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