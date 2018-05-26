package ru.vasilyknk.glwrapper

class Engine {
    fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Program {
        return Program(vertexShaderCode, fragmentShaderCode)
    }

}