package com.google.vr.sdk.samples.treasurehunt

import android.opengl.GLES20
import org.joml.Matrix4f
import org.joml.Matrix4fc
import ru.vasilyknk.glwrapper.Program
import ru.vasilyknk.glwrapper.VertexArrayObject
import ru.vasilyknk.glwrapper.VertexAttrib

class Cube(private val context: CubemapContext) {
    val cubeVAO: VertexArrayObject
    private val cubeFoundVAO: VertexArrayObject
    private val cubeProgram: Program
    private val uniformIndices: UniformIndices

    private val mv = Matrix4f()
    private val mvp = Matrix4f()

    private class UniformIndices(prog: Program) {
        val model               = prog.getUniformLocation("u_Model")
        val modelView           = prog.getUniformLocation("u_MVMatrix")
        val modelViewProjection = prog.getUniformLocation("u_MVP")
        val lightPos            = prog.getUniformLocation("u_LightPos")
    }

    init {
        val uvs = createCubeTexCoords()

        val cubeVertices    = context.createFloatBuffer(WorldLayoutData.CUBE_COORDS)
        val cubeColors      = context.createFloatBuffer(WorldLayoutData.CUBE_COLORS)
        val cubeFoundColors = context.createFloatBuffer(WorldLayoutData.CUBE_FOUND_COLORS)
        val cubeNormals     = context.createFloatBuffer(WorldLayoutData.CUBE_NORMALS)
        val cubeUVs         = context.createFloatBuffer(uvs)

        cubeProgram = context.rh.createProgram(
                context.readRawTextFileUnsafe(R.raw.light_vertex),
                context.readRawTextFileUnsafe(R.raw.passthrough_fragment))

        val positionIndex = cubeProgram.getAttribLocation("a_Position")
        val normalIndex   = cubeProgram.getAttribLocation("a_Normal")
        val colorIndex    = cubeProgram.getAttribLocation("a_Color")
        val uvIndex       = cubeProgram.getAttribLocation("a_UV")

        val attribs = arrayOf(
                VertexAttrib(positionIndex, 3, GLES20.GL_FLOAT, false, 0, 0),
                VertexAttrib(normalIndex, 3, GLES20.GL_FLOAT, false, 0, 1),
                VertexAttrib(colorIndex, 4, GLES20.GL_FLOAT, false, 0, 2),
                VertexAttrib(uvIndex, 2, GLES20.GL_FLOAT, false, 0, 3))

        cubeVAO = context.rh.createVertexArrayObject(attribs, arrayOf(cubeVertices, cubeNormals, cubeColors, cubeUVs))

        cubeFoundVAO = context.rh.createVertexArrayObject(attribs, arrayOf(cubeVertices, cubeNormals, cubeFoundColors, cubeUVs))

        uniformIndices = UniformIndices(cubeProgram)
    }

    fun render(m: Matrix4fc) {
        val sceneParams = context.sceneParams

        sceneParams.view.mul(m, mv)
        sceneParams.proj.mul(mv, mvp)

        with (cubeProgram.use()) {
            setUniform(uniformIndices.lightPos, sceneParams.viewLightPos)
            setUniform(uniformIndices.model, m, false)
            setUniform(uniformIndices.modelView, mv, false)
            setUniform(uniformIndices.modelViewProjection, mvp, false)

            // val vao = /*if (isLookingAtObject()) cubeFoundVAO else*/ cubeVAO

            with (cubeVAO.bind()) {
                drawArrays(GLES20.GL_TRIANGLES, 0, 36)
            }
        }
    }

}