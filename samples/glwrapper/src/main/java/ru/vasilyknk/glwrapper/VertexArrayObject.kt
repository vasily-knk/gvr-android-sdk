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

class VertexArrayObject(val attribs: Array<VertexAttrib>, val buffers: Array<Buffer>) {
    fun use() {
        for (attrib in attribs) {
            val buf = buffers[attrib.bufferIndex]

            GLES20.glVertexAttribPointer(attrib.index, attrib.size, attrib.type, attrib.normalized, attrib.stride, buf)
            GLES20.glEnableVertexAttribArray(attrib.index)
        }
    }

    fun unUse() {
        for (attrib in attribs) {
            GLES20.glDisableVertexAttribArray(attrib.index)
        }
    }
}