package ru.vasilyknk.glwrapper

import android.opengl.GLES20
import org.joml.Matrix4fc
import org.joml.Vector3fc
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Uniforms {
    private val floatBuffer = allocateFloatBuffer()

    fun set(index: Int, vec: Vector3fc) {
        vec.get(floatBuffer)
        GLES20.glUniform3fv(index, 1, floatBuffer)
    }

    fun set(index: Int, matrix: Matrix4fc, transpose: Boolean) {
        matrix.get(floatBuffer)
        GLES20.glUniformMatrix4fv(index, 1, transpose, floatBuffer)
    }
}

private fun allocateFloatBuffer(): FloatBuffer {
    val bb = ByteBuffer.allocateDirect(1024 * 4)
    bb.order(ByteOrder.nativeOrder())
    return bb.asFloatBuffer()
}

