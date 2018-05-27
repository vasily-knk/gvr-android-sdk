package ru.vasilyknk.glwrapper

import android.opengl.GLES20
import android.util.Log

class Program internal constructor(vertexShaderCode: String, fragmentShaderCode: String) : Resource {
    private var id: Int? = initProgram(vertexShaderCode, fragmentShaderCode)

    fun use() {
        GLES20.glUseProgram(checkedId())
    }

    fun getAttribLocation(name: String) = GLES20.glGetAttribLocation(checkedId(), name)
    fun getUniformLocation(name: String) = GLES20.glGetUniformLocation(checkedId(), name)

    override fun close() {
        val checkedId = id ?: return
        GLES20.glDeleteProgram(checkedId)
    }

    override fun isValid() = (id != null)

    private fun checkedId(): Int {
        return id ?: throw RuntimeException("Invalid program")
    }
}


private fun initProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
    val id = GLES20.glCreateProgram()

    GLES20.glAttachShader(id, loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode))
    GLES20.glAttachShader(id, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode))
    GLES20.glLinkProgram(id)
    GLES20.glUseProgram(id)

    return id
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