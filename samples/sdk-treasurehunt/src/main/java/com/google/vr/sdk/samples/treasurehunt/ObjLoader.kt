package com.google.vr.sdk.samples.treasurehunt

import android.opengl.GLES20
import ru.vasilyknk.glwrapper.ResourceHolder
import ru.vasilyknk.glwrapper.VertexArrayObject
import java.io.InputStream
import de.javagl.obj.ObjData
import de.javagl.obj.ObjUtils
import de.javagl.obj.ObjReader
import de.javagl.obj.Obj
import ru.vasilyknk.glwrapper.VertexAttrib

class ObjModel(
    val vao: VertexArrayObject,
    val geoms: Array<MeshGeom>
)

class ObjLoader(val rh: ResourceHolder) {
    fun load(inputStream: InputStream): ObjModel {
        val srcObj = ObjReader.read(inputStream)

        // Prepare the Obj so that its structure is suitable for
        // rendering with OpenGL:
        // 1. Triangulate it
        // 2. Make sure that texture coordinates are not ambiguous
        // 3. Make sure that normals are not ambiguous
        // 4. Convert it to single-indexed data
        val obj = ObjUtils.convertToRenderable(srcObj)

        // Obtain the data from the OBJ, as direct buffers:
        val indices   = ObjData.getFaceVertexIndices(obj)
        val vertices  = ObjData.getVertices(obj)
        val texCoords = ObjData.getTexCoords(obj, 2)
        val normals   = ObjData.getNormals(obj)

        val indicesBO   = rh.createBufferObject(GLES20.GL_ELEMENT_ARRAY_BUFFER)
        val verticesBO  = rh.createBufferObject(GLES20.GL_ARRAY_BUFFER)
        val texCoordsBO = rh.createBufferObject(GLES20.GL_ARRAY_BUFFER)
        val normalsBO   = rh.createBufferObject(GLES20.GL_ARRAY_BUFFER)

        indicesBO  .setData(indices  , indices  .remaining() * 4)
        verticesBO .setData(vertices , vertices .remaining() * 4)
        texCoordsBO.setData(texCoords, texCoords.remaining() * 4)
        normalsBO  .setData(normals  , normals  .remaining() * 4)

        val attribs = arrayOf(
                VertexAttrib(0, 3, GLES20.GL_FLOAT, false, 0, 0),
                VertexAttrib(1, 2, GLES20.GL_FLOAT, false, 0, 1),
                VertexAttrib(2, 3, GLES20.GL_FLOAT, false, 0, 2))

        val vao = rh.createVertexArrayObject(attribs,
                buffers=arrayOf(verticesBO, texCoordsBO, normalsBO),
                indices=indicesBO)

        val geoms = arrayOf<MeshGeom>(MeshGeom.Elements(GLES20.GL_TRIANGLES, indices.capacity(), GLES20.GL_UNSIGNED_INT, 0))

        return ObjModel(vao, geoms)
    }
}