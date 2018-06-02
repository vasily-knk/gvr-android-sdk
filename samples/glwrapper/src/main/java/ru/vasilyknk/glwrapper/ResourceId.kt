package ru.vasilyknk.glwrapper

internal class ResourceId constructor(private var id: Int?) {
    fun isValid() = (id != null)

    fun get(): Int {
        return id ?: throw RuntimeException("Invalid resource")
    }

    fun set(newId: Int) {
        if (id != null)
            throw RuntimeException("Resource already initialized")

        id = newId
    }
}