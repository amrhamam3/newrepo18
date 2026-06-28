package com.amr3d.preview.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.File

class FileBrowserFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var currentPathText: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var emptyText: TextView

    // Stack للتنقل للخلف
    private val pathStack = ArrayDeque<File>()
    private var currentPath = Environment.getExternalStorageDirectory()
    private val supportedExtensions = setOf("stl", "dxf")

    interface OnFileSelectedListener {
        fun onFileSelected(file: File)
    }
    var fileSelectedListener: OnFileSelectedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_file_browser, container, false)
        listView        = view.findViewById(R.id.fileList)
        currentPathText = view.findViewById(R.id.currentPath)
        btnBack         = view.findViewById(R.id.btnBackDir)
        emptyText       = TextView(requireContext()).also { it.visibility = View.GONE }

        btnBack.setOnClickListener { navigateUp() }

        checkPermissionAndLoad()
        return view
    }

    private fun checkPermissionAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ — الوصول عبر SAF أو MediaStore لذا نحاول مباشرة
            loadDirectory(currentPath)
        } else {
            val perm = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), perm)
                == PackageManager.PERMISSION_GRANTED) {
                loadDirectory(currentPath)
            } else {
                requestPermissions(arrayOf(perm), 100)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
            loadDirectory(currentPath)
        else
            Toast.makeText(context, "يلزم إذن الوصول للملفات", Toast.LENGTH_LONG).show()
    }

    private fun navigateUp() {
        if (pathStack.isNotEmpty()) {
            currentPath = pathStack.removeLast()
            loadDirectory(currentPath)
        } else {
            Toast.makeText(context, "أنت في المجلد الجذر", Toast.LENGTH_SHORT).show()
        }
        updateBackButton()
    }

    private fun updateBackButton() {
        btnBack.alpha = if (pathStack.isEmpty()) 0.4f else 1.0f
    }

    private fun loadDirectory(dir: File) {
        currentPathText.text = dir.absolutePath

        val allFiles = try { dir.listFiles() } catch (e: Exception) { null }

        if (allFiles == null) {
            Toast.makeText(context, "لا يمكن الوصول لهذا المجلد", Toast.LENGTH_SHORT).show()
            return
        }

        // المجلدات: أظهر فقط المجلدات التي تحتوي STL/DXF (بحث عميق) أو في المجلد الجذر
        val isRoot = pathStack.isEmpty()
        val dirs = allFiles
            .filter { f ->
                f.isDirectory && !f.isHidden && f.canRead() &&
                (isRoot || containsSupportedFiles(f))
            }
            .sortedBy { it.name.lowercase() }

        // ملفات STL/DXF فقط
        val files = allFiles
            .filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            .sortedBy { it.name.lowercase() }

        val entries = dirs + files

        if (entries.isEmpty()) {
            listView.visibility = View.GONE
            // show empty hint inline
            Toast.makeText(context, "لا توجد ملفات STL/DXF في هذا المجلد", Toast.LENGTH_SHORT).show()
            listView.visibility = View.VISIBLE
        }

        val names = entries.map { f ->
            if (f.isDirectory) {
                val count = (f.listFiles()?.count {
                    it.isFile && it.extension.lowercase() in supportedExtensions
                } ?: 0)
                val hint = if (count > 0) " ($count ملف)" else ""
                "📁  ${f.name}$hint"
            } else {
                val ext = f.extension.uppercase()
                val sizeKB = f.length() / 1024
                val sizeStr = if (sizeKB >= 1024) "${"%.1f".format(sizeKB/1024f)} MB"
                              else "$sizeKB KB"
                val icon = if (ext == "STL") "🧊" else "📐"
                "$icon  ${f.name}\n    $sizeStr"
            }
        }

        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_list_item_2,
            android.R.id.text1, names)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val file = entries[position]
            if (file.isDirectory) {
                pathStack.addLast(currentPath)
                currentPath = file
                updateBackButton()
                loadDirectory(file)
            } else {
                fileSelectedListener?.onFileSelected(file)
            }
        }

        updateBackButton()
    }

    // إعادة تحديث عند العودة للصفحة
    override fun onResume() {
        super.onResume()
        loadDirectory(currentPath)
    }

    /** بحث سريع عن STL/DXF داخل المجلد (مستوى واحد فقط) */
    private fun containsSupportedFiles(dir: File): Boolean {
        return try {
            dir.listFiles()?.any { f ->
                (f.isFile && f.extension.lowercase() in supportedExtensions) ||
                (f.isDirectory && f.listFiles()?.any { it.isFile && it.extension.lowercase() in supportedExtensions } == true)
            } ?: false
        } catch (_: Exception) { false }
    }
}
