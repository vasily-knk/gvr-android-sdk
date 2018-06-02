package ru.vasilyknk.glwrapper

interface Resource : AutoCloseable {
    fun isValid(): Boolean
}


