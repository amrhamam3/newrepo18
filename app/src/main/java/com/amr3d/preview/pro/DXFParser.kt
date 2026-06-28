package com.amr3d.preview.pro

import android.content.Context
import android.net.Uri

// استثناء مخصص للمشروع متوافق مع معماريتك
class StlParseException(message: String) : Exception(message)

// فئة البيانات الأساسية للـ Rendering ثلاثي الأبعاد بمشروعك
data class StlModel(
    val vertices: FloatArray, 
    val normals: FloatArray, 
    val boundingBox: FloatArray
)

object DXFParser {
    private data class DxfPair(val code: Int, val value: String)

    fun parse(context: Context, uri: Uri): StlModel {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader(Charsets.ISO_8859_1).readText()
        } ?: throw StlParseException("تعذر فتح ملف dxf")

        val rawLines = text.lines()
        val pairs = mutableListOf<DxfPair>()
        var idx = 0
        
        // تحويل الهيكل النصي إلى أزواج ثنائية (كود وقيمة)
        while (idx < rawLines.size - 1) {
            val code = rawLines[idx].trim().toIntOrNull()
            val value = rawLines[idx + 1].trim()
            if (code != null) {
                pairs.add(DxfPair(code, value))
            }
            idx += 2
        }

        // تحديد بداية ونهاية قسم الكيانات (ENTITIES)
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
        
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        var pos = entStart
        while (pos  x1 = pairs[pos].value.toFloatOrNull() ?: 0f
                            20 -> y1 = pairs[pos].value.toFloatOrNull() ?: 0f
                            30 -> z1 = pairs[pos].value.toFloatOrNull() ?: 0f
                            11 -> x2 = pairs[pos].value.toFloatOrNull() ?: 0f
                            21 -> y2 = pairs[pos].value.toFloatOrNull() ?: 0f
                            31 -> z2 = pairs[pos].value.toFloatOrNull() ?: 0f
                        }
                        pos++
                    }
                    
                    // حساب النقاط والحدود للخط الأول والثاني مباشرة وبشكل تتابعي آمن للـ Compiler
                    minX = minOf(minX, x1, x2); maxX = maxOf(maxX, x1, x2)
                    minY = minOf(minY, y1, y2); maxY = maxOf(maxY, y1, y2)
                    minZ = minOf(minZ, z1, z2); maxZ = maxOf(maxZ, z1, z2)
                    
                    vertices.add(x1); vertices.add(y1); vertices.add(z1)
                    normals.add(0f); normals.add(0f); normals.add(1f)
                    
                    vertices.add(x2); vertices.add(y2); vertices.add(z2)
                    normals.add(0f); normals.add(0f); normals.add(1f)
                    continue
                } 
                
                if (entityType == "LWPOLYLINE") {
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
                    
                    // ربط النقاط بشكل متتالي ليتناسب مع آلية الرندرة لديك
                    for (i in 0 until polyVertices.size - 1) {
                        val p1 = polyVertices[i]
                        val p2 = polyVertices[i + 1]
                        
                        minX = minOf(minX, p1.first, p2.first); maxX = maxOf(maxX, p1.first, p2.first)
                        minY = minOf(minY, p1.second, p2.second); maxY = maxOf(maxY, p1.second, p2.second)
                        minZ = minOf(minZ, elevation); maxZ = maxOf(maxZ, elevation)
                        
                        vertices.add(p1.first); vertices.add(p1.second); vertices.add(elevation)
                        normals.add(0f); normals.add(0f); normals.add(1f)
                        
                        vertices.add(p2.first); vertices.add(p2.second); vertices.add(elevation)
                        normals.add(0f); normals.add(0f); normals.add(1f)
                    }
                    continue
                }
            }
            pos++
        }

        // حماية نظام الرؤية من الانهيار إذا كان الملف فارغاً
        if (vertices.isEmpty()) {
            minX = 0f; maxX = 0f; minY = 0f; maxY = 0f; minZ = 0f; maxZ = 0f
        }

        val boundingBox = floatArrayOf(minX, maxX, minY, maxY, minZ, maxZ)
        return StlModel(vertices.toFloatArray(), normals.toFloatArray(), boundingBox)
    }
}
