package com.amr3d.preview.pro

import android.content.Context
import android.net.Uri

// 1. الفئات المساعدة لبيانات المجسم والاستثناءات
class StlParseException(message: String) : Exception(message)
data class StlModel(val vertices: FloatArray, val normals: FloatArray, val boundingBox: FloatArray)

object DxfParser {
    private data class DxfPair(val code: Int, val value: String)

    fun parse(context: Context, uri: Uri): StlModel {
        // قراءة الملف بالكامل بترميز متوافق مع ملفات الـ CAD
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader(Charsets.ISO_8859_1).readText()
        } ?: throw StlParseException("تعذر فتح ملف dxf")

        val rawLines = text.lines()
        val pairs = mutableListOf<DxfPair>()
        var idx = 0
        
        // تحويل النص إلى أزواج (كود وقيمة)
        while (idx < rawLines.size - 1) {
            val code = rawLines[idx].trim().toIntOrNull()
            val value = rawLines[idx + 1].trim()
            if (code != null) pairs.add(DxfPair(code, value))
            idx += 2
        }

        // 2. تحديد بداية ونهاية قسم العناصر (ENTITIES)
        var entStart = -1
        var entEnd = pairs.size
        for (k in pairs.indices) {
            if (pairs[k].code == 2 && pairs[k].value.uppercase() == "ENTITIES") entStart = k
            if (entStart > 0 && pairs[k].code == 0 && pairs[k].value.uppercase() == "ENDSEC" && k > entStart) {
                entEnd = k
                break
            }
        }

        if (entStart < 0) throw StlParseException("لم يتم العثور على قسم entities")

        // المصفوفات الديناميكية لتجميع النقاط
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        
        // متغيرات حساب أبعاد المجسم (Bounding Box)
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        // دالة مساعدة داخلية لتحديث الأبعاد وإضافة النقاط
        fun addVertex(x: Float, y: Float, z: Float) {
            minX = minOf(minX, x); maxX = maxOf(maxX, x)
            minY = minOf(minY, y); maxY = maxOf(maxY, y)
            minZ = minOf(minZ, z); maxZ = maxOf(maxZ, z)
            vertices.add(x)
            vertices.add(y)
            vertices.add(z)
            // إضافة مَواجهات افتراضية (Normal) لكل نقطة
            normals.addAll(listOf(0f, 0f, 1f))
        }

        // 3. قراءة وتحليل العناصر داخل القسم
        var pos = entStart
        while (pos  {
                        pos++
                        var x1 = 0f; var y1 = 0f; var z1 = 0f
                        var x2 = 0f; var y2 = 0f; var z2 = 0f
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
                        addVertex(x1, y1, z1)
                        addVertex(x2, y2, z2)
                        continue
                    }

                    // معالجة الخطوط المتصلة (أوتوكاد القياسي)
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
                        
                        // ربط النقاط المتتالية كخطوط ثنائية
                        for (i in 0 until polyVertices.size - 1) {
                            val p1 = polyVertices[i]
                            val p2 = polyVertices[i + 1]
                            addVertex(p1.first, p1.second, elevation)
                            addVertex(p2.first, p2.second, elevation)
                        }
                        continue
                    }
                }
            }
            pos++
        }

        // إذا لم يتم العثور على أي نقاط، نضع قيم افتراضية للـ Bounding Box لمنع الخطأ
        if (vertices.isEmpty()) {
            minX = 0f; maxX = 0f; minY = 0f; maxY = 0f; minZ = 0f; maxZ = 0f
        }

        val boundingBox = floatArrayOf(minX, maxX, minY, maxY, minZ, maxZ)
        return StlModel(vertices.toFloatArray(), normals.toFloatArray(), boundingBox)
    }
}
