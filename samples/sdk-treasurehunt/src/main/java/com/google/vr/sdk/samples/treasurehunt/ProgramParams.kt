package com.google.vr.sdk.samples.treasurehunt

import ru.vasilyknk.glwrapper.Program

class CubeParams(cubeProgram: Program) {
    val model               = cubeProgram.getUniformLocation("u_Model")
    val modelView           = cubeProgram.getUniformLocation("u_MVMatrix")
    val modelViewProjection = cubeProgram.getUniformLocation("u_MVP")
    val lightPos            = cubeProgram.getUniformLocation("u_LightPos")
}

class FloorParams(floorProgram: Program) {
    val position             = floorProgram.getAttribLocation("a_Position")
    val normal               = floorProgram.getAttribLocation("a_Normal")
    val color                = floorProgram.getAttribLocation("a_Color")

    val model                = floorProgram.getUniformLocation("u_Model")
    val modelView            = floorProgram.getUniformLocation("u_MVMatrix")
    val modelViewProjection  = floorProgram.getUniformLocation("u_MVP")
    val lightPos             = floorProgram.getUniformLocation("u_LightPos")
}