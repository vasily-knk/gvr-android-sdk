package ru.vasilyknk.glwrapper

import android.opengl.GLES20
import java.nio.Buffer

data class VertexAttrib(
        val index: Int,
        val size: Int,
        val type: Int,
        val normalized: Boolean,
        val stride: Int,
        val bufferIndex: Int)


class VertexArrayObject(val attribs: Array<VertexAttrib>, val buffers: Array<BufferObject>) {
    fun use() {
        for (attrib in attribs) {
            val buf = buffers[attrib.bufferIndex]

            buf.bind()
            GLES20.glVertexAttribPointer(attrib.index, attrib.size, attrib.type, attrib.normalized, attrib.stride, 0)
            GLES20.glEnableVertexAttribArray(attrib.index)
        }
    }

    fun unUse() {
        for (attrib in attribs) {
            GLES20.glDisableVertexAttribArray(attrib.index)
            buffers[attrib.bufferIndex].unbind()
        }
    }

    fun useLock() = UseLock()

    inner class UseLock : AutoCloseable {
        init {
            use()
        }

        override fun close() {
            unUse()
        }
    }
}