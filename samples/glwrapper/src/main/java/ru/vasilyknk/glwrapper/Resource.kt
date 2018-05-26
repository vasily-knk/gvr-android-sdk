package ru.vasilyknk.glwrapper

interface Resource {
    fun free()
    fun isValid(): Boolean
}

