package com.amr3d.preview.pro

import android.content.Context
import android.net.Uri

class StlParseException(message: String) : Exception(message)

data class StlModel(
    val vertices: FloatArray,
    val normals: FloatArray,
    val boundingBox: FloatArray
)

object DxfParser {
    private data class DxfPair(val code: Int, val value: String)

    fun parse(context: Context, uri: Uri): StlModel {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.ISO_8859_1).readText()
        } ?: throw StlParseException("تعذر فتح ملف dxf")

        val rawLines = text.lines()
        val pairs = mutableListOf<DxfPair>()
        var idx = 0
        
        while (idx < rawLines.size - 1) {
            val code = rawLines[idx].trim().toIntOrNull()
            val value = rawLines[idx + 1].trim()
            if (code != null) {
                pairs.add(DxfPair(code, value))
            }
            idx += 2
        }

        var entStart = -1
        var entEnd = pairs.size
        for (k in pairs.indices) {
            if (pairs[k].code == 2 && pairs[k].value.uppercase() == "ENTITIES") {
                entStart = k
            }
            if (entStart > 0 && pairs[k].code == 0 && pairs[k].value.uppercase() == "ENDSEC" && k > entStart) {
                entEnd = k
                break
            }
        }

        if (entStart < 0) throw StlParseException("لم يتم العثور على قسم entities")

        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        var pos = entStart
        while (pos  {
                        pos++
                        var x1 = 0f
                        var y1 = 0f
                        var z1 = 0f
                        var x2 = 0f
                        var y2 = 0f
                        var z2 = 0f
                        
                        while (pos < entEnd && pairs[pos].code != 0) {
                            when (pairs[pos].code) {
                                10 -> x1 = pairs[pos].value.toFloatOrNull() ?: 0f
                                20 -> y1 = pairs[pos].value.toFloatOrNull() ?: 0f
                                30 -> z1 = pairs[pos].value.toFloatOrNull() ?: 0f
                                11 -> x2 = pairs[pos].value.toFloatOrNull() ?: 0f
                                21 -> y2 = pairs[pos].value.toFloatOrNull() ?: 0f
                                31 -> z2 = pairs[pos].value.toFloatOrNull() ?: 0f
                            }
                            pos++
                        }
                        
                        // حساب حدود النقطة الأولى بشكل منفصل
                        if (x1 < minX) minX = x1
                        if (x1 > maxX) maxX = x1
                        if (y1 < minY) minY = y1
                        if (y1 > maxY) maxY = y1
                        if (z1 < minZ) minZ = z1
                        if (z1 > maxZ) maxZ = z1
                        
                        vertices.add(x1)
                        vertices.add(y1)
                        vertices.add(z1)
                        normals.add(0f)
                        normals.add(0f)
                        normals.add(1f)

                        // حساب حدود النقطة الثانية بشكل منفصل
                        if (x2 < minX) minX = x2
                        if (x2 > maxX) maxX = x2
                        if (y2 < minY) minY = y2
                        if (y2 > maxY) maxY = y2
                        if (z2 < minZ) minZ = z2
                        if (z2 > maxZ) maxZ = z2
                        
                        vertices.add(x2)
                        vertices.add(y2)
                        vertices.add(z2)
                        normals.add(0f)
                        normals.add(0f)
                        normals.add(1f)
                    }

                    "LWPOLYLINE" -> {
                        pos++
                        val polyVertices = mutableListOf<Pair<Float, Float>>()
                        var elevation = 0f
                        
                        while (pos < entEnd && pairs[pos].code != 0) {
                            when (pairs[pos].code) {
                                38 -> elevation = pairs[pos].value.toFloatOrNull() ?: 0f
                                10 -> {
                                    val nextX = pairs[pos].value.toFloatOrNull() ?: 0f
                                    var nextY = 0f
                                    if (pos + 1 < entEnd && pairs[pos + 1].code == 20) {
                                        nextY = pairs[pos + 1].value.toFloatOrNull() ?: 0f
                                        pos++
                                    }
                                    polyVertices.add(Pair(nextX, nextY))
                                }
                            }
                            pos++
                        }
                        
                        for (i in 0 until polyVertices.size - 1) {
                            val p1 = polyVertices[i]
                            val p2 = polyVertices[i + 1]
                            
                            // النقطة الأولى للضلع المتصل
                            if (p1.first < minX) minX = p1.first
                            if (p1.first > maxX) maxX = p1.first
                            if (p1.second < minY) minY = p1.second
                            if (p1.second > maxY) maxY = p1.second
                            if (elevation < minZ) minZ = elevation
                            if (elevation > maxZ) maxZ = elevation
                            
                            vertices.add(p1.first)
                            vertices.add(p1.second)
                            vertices.add(elevation)
                            normals.add(0f)
                            normals.add(0f)
                            normals.add(1f)

                            // النقطة الثانية للضلع المتصل
                            if (p2.first < minX) minX = p2.first
                            if (p2.first > maxX) maxX = p2.first
                            if (p2.second < minY) minY = p2.second
                            if (p2.second > maxY) maxY = p2.second
                            if (elevation < minZ) minZ = elevation
                            if (elevation > maxZ) maxZ = elevation
                            
                            vertices.add(p2.first)
                            vertices.add(p2.second)
                            vertices.add(elevation)
                            normals.add(0f)
                            normals.add(0f)
                            normals.add(1f)
                        }
                    }
                    else -> pos++
                }
            } else {
                pos++
            }
        }

        if (vertices.isEmpty()) {
            minX = 0f
            maxX = 0f
            minY = 0f
            maxY = 0f
            minZ = 0f
            maxZ = 0f
        }

        val boundingBox = floatArrayOf(minX, maxX, minY, maxY, minZ, maxZ)
        return StlModel(vertices.toFloatArray(), normals.toFloatArray(), boundingBox)
    }
}
