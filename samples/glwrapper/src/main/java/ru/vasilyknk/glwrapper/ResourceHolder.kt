package ru.vasilyknk.glwrapper

class ResourceHolder : AutoCloseable {
    private val resources = mutableListOf<Resource>()

    fun createProgram(): Program = addResource(Program())
    fun createBufferObject(target: Int): BufferObject = addResource(BufferObject(target))

    private fun <T : Resource> addResource(res: T): T {
        resources.add(res)
        return res
    }

    override fun close() {
        for (res in resources.asReversed()) {
            if (res.isValid())
                res.close()
        }

        resources.clear()
    }
}
