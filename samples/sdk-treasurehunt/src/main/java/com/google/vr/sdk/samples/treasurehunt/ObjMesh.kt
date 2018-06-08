package com.google.vr.sdk.samples.treasurehunt

import org.joml.Matrix4fc
import ru.vasilyknk.glwrapper.VertexArrayObject

class ObjMesh(val context: CubemapContext) {
    private val mesh: Mesh

    init {
        val loader = ObjLoader(context.rh)
        val inputStream = context.openRawResource(R.raw.helicopter)

        val obj = loader.load(inputStream)

        val program = context.rh.createProgram(
                context.readRawTextFileUnsafe(R.raw.obj_vertex),
                context.readRawTextFileUnsafe(R.raw.obj_fragment))

        val meshProg = MeshProg(program)

        mesh = Mesh(meshProg, obj.vao, obj.geoms)
    }

    fun render(m: Matrix4fc, meshRenderer: MeshRenderer) {
        meshRenderer.update(mesh, context.sceneParams, m)
        meshRenderer.render(mesh)
    }
}