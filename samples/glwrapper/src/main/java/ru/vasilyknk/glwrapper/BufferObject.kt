package ru.vasilyknk.glwrapper

import android.opengl.GLES20
import java.nio.Buffer

class BufferObject internal constructor (val target: Int) : Resource {
    private val id: ResourceId

    init {
        val arr = IntArray(1)
        GLES20.glGenBuffers(1, arr, 0)
        id = ResourceId(arr[0])
    }

    fun bind() {
        GLES20.glBindBuffer(target, id.get())
    }

    fun unbind() {
        GLES20.glBindBuffer(target, 0)
    }

    fun setData(data: Buffer, size: Int, usage: Int) {
        bind()

        GLES20.glBufferData(target, size, data, usage)
    }

    override fun close() {
        if (id.isValid())
            GLES20.glDeleteBuffers(1, intArrayOf(id.get()), 0)
    }

    override fun isValid() = id.isValid()
}

