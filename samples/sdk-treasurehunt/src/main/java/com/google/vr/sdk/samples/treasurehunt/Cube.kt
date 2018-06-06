package com.google.vr.sdk.samples.treasurehunt

import android.opengl.GLES20
import org.joml.Matrix4f
import org.joml.Matrix4fc
import ru.vasilyknk.glwrapper.Program
import ru.vasilyknk.glwrapper.VertexArrayObject
import ru.vasilyknk.glwrapper.VertexAttrib

class Cube(private val context: CubemapContext) {
    private val mesh: Mesh
    private val foundMesh: Mesh

    init {
        val uvs = createCubeTexCoords()

        val cubeVertices    = context.createFloatBuffer(WorldLayoutData.CUBE_COORDS)
        val cubeColors      = context.createFloatBuffer(WorldLayoutData.CUBE_COLORS)
        val cubeFoundColors = context.createFloatBuffer(WorldLayoutData.CUBE_FOUND_COLORS)
        val cubeNormals     = context.createFloatBuffer(WorldLayoutData.CUBE_NORMALS)
        val cubeUVs         = context.createFloatBuffer(uvs)

        val cubeProgram = context.rh.createProgram(
                context.readRawTextFileUnsafe(R.raw.light_vertex),
                context.readRawTextFileUnsafe(R.raw.passthrough_fragment))

        val meshProg = MeshProg(cubeProgram)

        val positionIndex = cubeProgram.getAttribLocation("a_Position")
        val normalIndex   = cubeProgram.getAttribLocation("a_Normal")
        val colorIndex    = cubeProgram.getAttribLocation("a_Color")
        val uvIndex       = cubeProgram.getAttribLocation("a_UV")

        val attribs = arrayOf(
                VertexAttrib(positionIndex, 3, GLES20.GL_FLOAT, false, 0, 0),
                VertexAttrib(normalIndex, 3, GLES20.GL_FLOAT, false, 0, 1),
                VertexAttrib(colorIndex, 4, GLES20.GL_FLOAT, false, 0, 2),
                VertexAttrib(uvIndex, 2, GLES20.GL_FLOAT, false, 0, 3))

        val cubeVAO  = context.rh.createVertexArrayObject(attribs, arrayOf(cubeVertices, cubeNormals, cubeColors, cubeUVs))

        val cubeFoundVAO = context.rh.createVertexArrayObject(attribs, arrayOf(cubeVertices, cubeNormals, cubeFoundColors, cubeUVs))

        val cubeGeom = MeshGeom.Arrays(GLES20.GL_TRIANGLES, 0, 36)
        
        mesh = Mesh(meshProg, cubeVAO, arrayOf(cubeGeom))
        foundMesh = Mesh(meshProg, cubeFoundVAO, arrayOf(cubeGeom))
    }

    fun getCubeVAO() = mesh.vao

    fun render(m: Matrix4fc, meshRenderer: MeshRenderer) {
        val currentMesh = if (context.isLookingAtObject(m)) foundMesh else mesh

        meshRenderer.update(currentMesh, context.sceneParams, m)
        meshRenderer.render(currentMesh)
    }

}