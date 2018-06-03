package ru.vasilyknk.glwrapper

class ResourceHolder : AutoCloseable {
    private val resources = mutableListOf<Resource>()

    fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Program = addResource(Program(vertexShaderCode, fragmentShaderCode))
    fun createBufferObject(target: Int): BufferObject = addResource(BufferObject(target))
    fun createTexture(target: Int): Texture = addResource(Texture(target))

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
