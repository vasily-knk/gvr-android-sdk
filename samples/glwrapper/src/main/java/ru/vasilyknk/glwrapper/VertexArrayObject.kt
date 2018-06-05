package ru.vasilyknk.glwrapper

import android.opengl.GLES20
import android.opengl.GLES30
import java.nio.Buffer

data class VertexAttrib(
        val index: Int,
        val size: Int,
        val type: Int,
        val normalized: Boolean,
        val stride: Int,
        val bufferIndex: Int)


class VertexArrayObject internal constructor(attribs: Array<VertexAttrib>, buffers: Array<BufferObject>): Resource {
    private val id: ResourceId

    companion object {
        fun unbindAll() {
            GLES30.glBindVertexArray(0)
        }
    }

    inner class Bind internal constructor() : AutoCloseable {
        init {
            GLES30.glBindVertexArray(id.get())
        }

        fun drawArrays(mode: Int, first: Int, count: Int) {
            GLES30.glDrawArrays(mode, first, count)
        }

        override fun close() {}
    }


    init {
        val arr = IntArray(1)
        GLES30.glGenVertexArrays(1, arr, 0)
        id = ResourceId(arr[0])

        bind()

        for (attrib in attribs) {
            val buf = buffers[attrib.bufferIndex]

            buf.bind()
            GLES30.glVertexAttribPointer(attrib.index, attrib.size, attrib.type, attrib.normalized, attrib.stride, 0)
            GLES30.glEnableVertexAttribArray(attrib.index)
        }
    }

    fun bind() = Bind()

    override fun isValid() = id.isValid()

    override fun close() {
        if (id.isValid())
            GLES30.glDeleteVertexArrays(1, intArrayOf(id.get()), 0)
    }
}