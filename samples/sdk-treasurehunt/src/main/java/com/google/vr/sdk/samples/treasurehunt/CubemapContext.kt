package com.google.vr.sdk.samples.treasurehunt

import ru.vasilyknk.glwrapper.ResourceHolder
import ru.vasilyknk.glwrapper.Uniforms
import ru.vasilyknk.glwrapper.VertexArrayObject

import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f
import ru.vasilyknk.glwrapper.Program

interface CubemapContext {
    fun getRH(): ResourceHolder
    fun getUniforms(): Uniforms
    fun readRawTextFileUnsafe(resId: Int): String
    fun getCubeVAO(): VertexArrayObject
}

class SceneParams {
    val view: Matrix4f = Matrix4f()
    val proj: Matrix4f = Matrix4f()
    val viewLightPos: Vector3f = Vector3f()
}

class MeshParams {
    val mv = Matrix4f()
    val mvp = Matrix4f()

    fun update(sceneParams: SceneParams, m: Matrix4fc) {
        sceneParams.view.mul(m, mv)
        sceneParams.proj.mul(mv, mvp)
    }
}

class MeshProg(val prog: Program) {
    val model               = prog.getUniformLocation("u_Model")
    val modelView           = prog.getUniformLocation("u_MVMatrix")
    val modelViewProjection = prog.getUniformLocation("u_MVP")
    val lightPos            = prog.getUniformLocation("u_LightPos")
}



