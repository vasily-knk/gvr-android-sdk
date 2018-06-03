package ru.vasilyknk.glwrapper

import android.opengl.GLES20
import android.opengl.GLES30
import java.nio.Buffer

class Texture internal constructor(val target: Int) : Resource {
    private val id: ResourceId

    init {
        val arr = IntArray(1)
        GLES20.glGenTextures(1, arr, 0)
        id = ResourceId(arr[0])
    }

    fun bind() {
        GLES20.glBindTexture(target, id.get())
    }

    fun setParam(param: Int, value: Int) {
        bind()
        GLES20.glTexParameteri(target, param, value)
    }

    fun generateMipmap() {
        bind()
        GLES20.glGenerateMipmap(target)
    }

    fun setStorage2D(levels: Int, internalFormat: Int, width: Int, height: Int) {
        bind()
        GLES30.glTexStorage2D(target, levels, internalFormat, width, height)
    }

    fun setSubImage2D(level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, data: Buffer) {
        bind()
        GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data)
    }

    override fun close() {
        if (id.isValid())
            GLES20.glDeleteTextures(1, intArrayOf(id.get()), 0)
    }

    override fun isValid() = id.isValid()
    
}