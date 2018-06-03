package com.google.vr.sdk.samples.treasurehunt

import android.opengl.GLES30
import org.joml.*
import ru.vasilyknk.glwrapper.ResourceHolder
import ru.vasilyknk.glwrapper.Uniforms
import ru.vasilyknk.glwrapper.VertexArrayObject

import ru.vasilyknk.glwrapper.Program

interface CubemapContext {
    val rh: ResourceHolder
    val uniforms: Uniforms
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

fun createCubeTexCoords(): FloatArray {
    val numVerts = WorldLayoutData.CUBE_COORDS.size / 3

    fun extractVector3f(arr: FloatArray, offset: Int) = Vector3f(arr[offset + 0], arr[offset + 1], arr[offset + 2]).toImmutable()
    fun createTangent(normal: Vector3fc): Vector3fc =
        if (Math.abs(normal.z()) > 0.9f)
            Vector3f(1.0f, 0.0f, 0.0f)
        else
            Vector3f(0.0f, 0.0f, 1.0f)

    val result = FloatArray(numVerts * 2)

    for (i in 0 until numVerts) {
        val coord = extractVector3f(WorldLayoutData.CUBE_COORDS, i * 3)
        val normal = extractVector3f(WorldLayoutData.CUBE_NORMALS, i * 3)

        val tangent = createTangent(normal)
        val binormal = Vector3f(normal).cross(tangent).normalize().toImmutable()

        val uv = Vector2f(coord.dot(tangent), coord.dot(binormal))
                .mul(0.5f)
                .add(0.5f, 0.5f)

        result[i * 2 + 0]  = uv.x
        result[i * 2 + 1]  = uv.y
    }

    return result
}

