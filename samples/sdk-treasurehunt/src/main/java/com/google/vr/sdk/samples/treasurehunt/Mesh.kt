package com.google.vr.sdk.samples.treasurehunt

import android.opengl.GLES20
import org.joml.Matrix4fc
import ru.vasilyknk.glwrapper.Program
import ru.vasilyknk.glwrapper.VertexArrayObject

class MeshProg(val prog: Program) {
    val model               = prog.getUniformLocation("u_Model")
    val modelView           = prog.getUniformLocation("u_MVMatrix")
    val modelViewProjection = prog.getUniformLocation("u_MVP")
    val lightPos            = prog.getUniformLocation("u_LightPos")
}

interface MeshGeom {
    fun draw()

    class Arrays(val mode: Int, val first: Int, val count: Int) : MeshGeom {
        override fun draw() {
            GLES20.glDrawArrays(mode, first, count)
        }
    }

    class Elements(val mode: Int, val count: Int, val type: Int, val firstIndex: Int) : MeshGeom {
        override fun draw() {
            GLES20.glDrawElements(mode, count, type, firstIndex)
        }
    }
}

class Mesh(
    val prog: MeshProg,
    val vao: VertexArrayObject,
    val geoms: Array<MeshGeom>
)

class MeshRenderer {
    val meshParams = MeshParams()

    fun update(mesh: Mesh, sceneParams: SceneParams, m: Matrix4fc) {
        meshParams.update(sceneParams, m)

        val cubeProgram = mesh.prog.prog

        cubeProgram.use()
        cubeProgram.setUniform(mesh.prog.lightPos, sceneParams.viewLightPos)
        cubeProgram.setUniform(mesh.prog.model, m, false)
        cubeProgram.setUniform(mesh.prog.modelView, meshParams.mv, false)
        cubeProgram.setUniform(mesh.prog.modelViewProjection, meshParams.mvp, false)
    }

    fun render(mesh: Mesh) {
        mesh.prog.prog.use()
        mesh.vao.bind()

        for (geom in mesh.geoms)
            geom.draw()
    }
}