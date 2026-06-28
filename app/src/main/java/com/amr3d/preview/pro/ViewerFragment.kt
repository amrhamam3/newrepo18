package com.amr3d.preview.pro

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ViewerFragment : Fragment() {

    // Views
    private lateinit var glViewerView: GLViewerView
    private lateinit var emptyStateText: TextView
    private lateinit var btnOpenFile: Button
    private lateinit var btnWhatsapp: ImageButton
    private lateinit var btnMeasureTool: ToggleButton
    private lateinit var btnInspect: Button
    private lateinit var btnResetView: Button
    private lateinit var btnWireframe: ToggleButton
    private lateinit var btnMaterial: Button
    private lateinit var btnUnit: Button
    private lateinit var btnExport: Button
    private lateinit var btnLightToggle: ToggleButton
    private lateinit var viewCube: ViewCubeView
    private lateinit var viewCubeLarge: ViewCubeView
    private lateinit var btnViewCubeToggle: View
    private lateinit var viewCubePanel: View
    private lateinit var btnViewBack: Button
    private lateinit var btnViewLeft: Button
    private lateinit var btnViewBottom: Button
    private lateinit var measurementCard: CardView
    private lateinit var measurementText: TextView
    private lateinit var inspectionCard: CardView
    private lateinit var inspectionText: TextView
    private lateinit var lightWheelContainer: ViewGroup
    private lateinit var lightWheel: SemiCircleLightView
    private lateinit var btnCloseLightWheel: ImageButton

    // شريط التحميل
    private lateinit var loadingContainer: View
    private lateinit var loadingProgress: ProgressBar
    private lateinit var loadingText: TextView

    // نص الترحيب
    private lateinit var welcomeText: TextView

    private var currentModel: STLModel? = null
    private var measureModeOn = false
    private var currentUnit = MeasurementUnit.MM

    private val openDocumentLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}
            loadFile(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_viewer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupWelcome()
        wireUpListeners()
        animateToolbarEntrance()
    }

    private fun bindViews(v: View) {
        glViewerView        = v.findViewById(R.id.glViewerView)
        emptyStateText      = v.findViewById(R.id.emptyStateText)
        btnOpenFile         = v.findViewById(R.id.btnOpenFile)
        btnWhatsapp         = v.findViewById(R.id.btnWhatsapp)
        btnMeasureTool      = v.findViewById(R.id.btnMeasureTool)
        btnInspect          = v.findViewById(R.id.btnInspect)
        btnResetView        = v.findViewById(R.id.btnResetView)
        btnWireframe        = v.findViewById(R.id.btnWireframe)
        btnMaterial         = v.findViewById(R.id.btnMaterial)
        btnUnit             = v.findViewById(R.id.btnUnit)
        btnExport           = v.findViewById(R.id.btnExport)
        btnLightToggle      = v.findViewById(R.id.btnLightToggle)
        viewCube            = v.findViewById(R.id.viewCube)
        viewCubeLarge       = v.findViewById(R.id.viewCubeLarge)
        btnViewCubeToggle   = v.findViewById(R.id.btnViewCubeToggle)
        viewCubePanel       = v.findViewById(R.id.viewCubePanel)
        btnViewBack         = v.findViewById(R.id.btnViewBack)
        btnViewLeft         = v.findViewById(R.id.btnViewLeft)
        btnViewBottom       = v.findViewById(R.id.btnViewBottom)
        measurementCard     = v.findViewById(R.id.measurementCard)
        measurementText     = v.findViewById(R.id.measurementText)
        inspectionCard      = v.findViewById(R.id.inspectionCard)
        inspectionText      = v.findViewById(R.id.inspectionText)
        lightWheelContainer = v.findViewById(R.id.lightWheelContainer)
        lightWheel          = v.findViewById(R.id.lightWheel)
        btnCloseLightWheel  = v.findViewById(R.id.btnCloseLightWheel)
        loadingContainer    = v.findViewById(R.id.loadingContainer)
        loadingProgress     = v.findViewById(R.id.loadingProgress)
        loadingText         = v.findViewById(R.id.loadingText)
        welcomeText         = v.findViewById(R.id.welcomeText)
    }

    // ══ ترحيب بالمستخدم ══
    private fun setupWelcome() {
        val ctx = requireContext()
        val savedName = MainActivity.getUserName(ctx)
        if (savedName.isEmpty()) {
            // أول مرة — اطلب الاسم
            showNameDialog(ctx)
        } else {
            showWelcomeName(savedName)
        }
    }

    private fun showNameDialog(ctx: Context) {
        val input = EditText(ctx).apply {
            hint = "اكتب اسمك هنا"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setTextColor(0xFFF2F3F5.toInt())
            setHintTextColor(0xFF9CA3AF.toInt())
            setPadding(40, 24, 40, 24)
            textSize = 16f
        }
        AlertDialog.Builder(ctx)
            .setTitle("🎮  مرحباً بك في Amr3D Preview")
            .setMessage("ما اسمك؟")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("ابدأ") { _, _ ->
                val name = input.text.toString().trim().ifEmpty { "صديقي" }
                MainActivity.saveUserName(ctx, name)
                showWelcomeName(name)
                showWelcomeAnimation(name)
            }
            .show()
    }

    private fun showWelcomeName(name: String) {
        welcomeText.text = "👋  مرحباً، $name"
        welcomeText.visibility = View.VISIBLE
    }

    private fun showWelcomeAnimation(name: String) {
        val overlay = view?.findViewById<TextView>(R.id.welcomeOverlay) ?: return
        overlay.text = "🎮  أهلاً وسهلاً\n$name"
        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        overlay.animate().alpha(1f).setDuration(400).withEndAction {
            overlay.animate().alpha(0f).setStartDelay(1800).setDuration(500).withEndAction {
                overlay.visibility = View.GONE
            }.start()
        }.start()
    }

    private fun animateToolbarEntrance() {
        val topBar    = view?.findViewById<View>(R.id.topBar) ?: return
        val btmBar    = view?.findViewById<View>(R.id.displayToolbar) ?: return
        val btmBar2   = view?.findViewById<View>(R.id.bottomToolbar) ?: return
        topBar.translationY = -180f; topBar.alpha = 0f
        topBar.animate().translationY(0f).alpha(1f).setDuration(400)
            .setInterpolator(DecelerateInterpolator(2f)).start()
        btmBar.translationY = 180f; btmBar.alpha = 0f
        btmBar.animate().translationY(0f).alpha(1f).setDuration(400)
            .setStartDelay(80).setInterpolator(DecelerateInterpolator(2f)).start()
        btmBar2.translationY = 180f; btmBar2.alpha = 0f
        btmBar2.animate().translationY(0f).alpha(1f).setDuration(400)
            .setStartDelay(160).setInterpolator(DecelerateInterpolator(2f)).start()
    }

    private fun wireUpListeners() {
        btnOpenFile.setOnClickListener {
            animBtn(it); openDocumentLauncher.launch(arrayOf("*/*"))
        }
        btnMeasureTool.setOnCheckedChangeListener { btn, isChecked ->
            animBtn(btn); measureModeOn = isChecked
            if (!isChecked) { glViewerView.stlRenderer.clearMeasurementPoints(); measurementCard.visibility = View.GONE }
            else { inspectionCard.visibility = View.GONE; Toast.makeText(context, "اضغط على نقطتين على الموديل", Toast.LENGTH_LONG).show() }
        }
        btnInspect.setOnClickListener {
            animBtn(it)
            currentModel?.let { m -> showInspectionReport(m) }
                ?: Toast.makeText(context, "افتح ملف أولاً", Toast.LENGTH_SHORT).show()
        }
        btnResetView.setOnClickListener  { animBtn(it); resetCamera() }
        btnWhatsapp.setOnClickListener   { animBtn(it); openWhatsapp() }
        btnWireframe.setOnCheckedChangeListener { btn, c -> animBtn(btn); glViewerView.stlRenderer.wireframeMode = c }
        btnMaterial.setOnClickListener   { animBtn(it); showMaterialPicker() }
        btnUnit.setOnClickListener       { animBtn(it); cycleUnit() }
        btnExport.setOnClickListener     { animBtn(it); exportCurrentView() }
        btnLightToggle.setOnCheckedChangeListener { btn, c ->
            animBtn(btn)
            lightWheelContainer.visibility = if (c) View.VISIBLE else View.GONE
        }
        btnCloseLightWheel.setOnClickListener {
            lightWheelContainer.visibility = View.GONE; btnLightToggle.isChecked = false
        }
        lightWheel.onAngleChanged = { angle ->
            glViewerView.queueEvent { glViewerView.stlRenderer.lightAngle = angle }
        }
        viewCube.onFaceSelected = { face -> jumpToView(face.rotX, face.rotY) }
        viewCubeLarge.onFaceSelected = { face ->
            jumpToView(face.rotX, face.rotY)
            viewCubePanel.visibility = View.GONE
        }
        btnViewCubeToggle.setOnClickListener {
            val showing = viewCubePanel.visibility == View.VISIBLE
            viewCubePanel.visibility = if (showing) View.GONE else View.VISIBLE
        }
        btnViewBack.setOnClickListener   { jumpToView(-10f, 180f) }
        btnViewLeft.setOnClickListener   { jumpToView(-10f, -90f) }
        btnViewBottom.setOnClickListener { jumpToView(89f, 0f) }
        glViewerView.onSingleTap = { x, y -> if (measureModeOn) handleMeasurementTap(x, y) }
        inspectionCard.setOnClickListener  { inspectionCard.visibility = View.GONE }
        measurementCard.setOnClickListener {
            measurementCard.visibility = View.GONE
            glViewerView.stlRenderer.clearMeasurementPoints()
        }
    }

    private fun animBtn(v: View) {
        v.animate().scaleX(0.87f).scaleY(0.87f).setDuration(70)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(140)
                    .setInterpolator(OvershootInterpolator(2.2f)).start()
            }.start()
    }

    // ══ تحميل الملف مع شريط التقدم ══
    fun loadFile(uri: Uri) {
        showLoadingBar("جارٍ فتح الملف...", 0)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val model = withContext(Dispatchers.IO) {
                    // محاكاة تقدم مرحلي
                    val job = launch {
                        var p = 5
                        while (p < 90) {
                            delay(120)
                            p = (p + (5..15).random()).coerceAtMost(90)
                            withContext(Dispatchers.Main) {
                                updateLoadingBar("جارٍ تحليل الملف...", p)
                            }
                        }
                    }
                    val m = try {
                        STLParser.parse(requireContext(), uri)
                    } catch (e: Exception) {
                        // محاولة DXF
                        DXFParser.parse(requireContext(), uri)
                    } finally {
                        job.cancel()
                    }
                    m
                }

                if (!isAdded) return@launch
                updateLoadingBar("جارٍ التحضير للعرض...", 95)
                delay(150)
                updateLoadingBar("اكتمل ✓", 100)
                delay(300)
                hideLoadingBar()

                currentModel = model
                glViewerView.stlRenderer.setModel(model)
                emptyStateText.visibility  = View.GONE
                welcomeText.visibility     = View.GONE
                view?.background = null
                inspectionCard.visibility  = View.GONE
                measurementCard.visibility = View.GONE
                btnMeasureTool.isChecked   = false
                btnWireframe.isChecked     = false

                // حفظ المسار الحقيقي (ليس SAF URI)
                try {
                    val cursor = requireContext().contentResolver.query(uri,
                        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME,
                                android.provider.MediaStore.MediaColumns.DATA),
                        null, null, null)
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val dataIdx = c.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                            if (dataIdx >= 0 && !c.isNull(dataIdx)) {
                                val realPath = c.getString(dataIdx)
                                if (realPath != null) HistoryFragment.addToHistory(requireContext(), realPath)
                            }
                        }
                    }
                } catch (_: Exception) { }
                Toast.makeText(context, "✅  ${model.triangleCount} مثلث", Toast.LENGTH_SHORT).show()

            } catch (e: SecurityException) {
                if (!isAdded) return@launch
                hideLoadingBar()
                Toast.makeText(context, "خطأ في الأذونات — حاول مرة أخرى", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                if (!isAdded) return@launch
                hideLoadingBar()
                Toast.makeText(context, "تعذر قراءة الملف: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoadingBar(msg: String, progress: Int) {
        loadingContainer.visibility = View.VISIBLE
        loadingProgress.progress = progress
        loadingText.text = msg
    }

    private fun updateLoadingBar(msg: String, progress: Int) {
        loadingProgress.progress = progress
        loadingText.text = msg
    }

    private fun hideLoadingBar() {
        loadingContainer.animate().alpha(0f).setDuration(300).withEndAction {
            loadingContainer.visibility = View.GONE
            loadingContainer.alpha = 1f
        }.start()
    }

    fun loadSTLFile(uri: Uri) = loadFile(uri)

    private fun jumpToView(targetRotX: Float, targetRotY: Float) {
        val r = glViewerView.stlRenderer
        val sx = r.rotationX; val sy = r.rotationY
        android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350; interpolator = DecelerateInterpolator(2f)
            addUpdateListener { a ->
                val t = a.animatedValue as Float
                r.rotationX = sx + (targetRotX - sx) * t
                r.rotationY = sy + (targetRotY - sy) * t
            }
        }.start()
    }

    private fun resetCamera() {
        val r = glViewerView.stlRenderer
        r.rotationX = -25f; r.rotationY = 35f
        r.scaleFactor = 1f; r.panX = 0f; r.panY = 0f
        glViewerView.queueEvent { r.updateProjection() }
    }

    private fun showMaterialPicker() {
        val materials = STLRenderer.Material.values()
        val icons = listOf("🔵","🔩","🪵","🪨","🟠","⬛","🟡")
        val matItems = materials.map { "${icons[it.id]}  ${it.nameAr}" }
        val bgItems = listOf("── خلفية ──","🌑 داكن","⬛ أسود","🌫️ رمادي","⬜ أبيض","🌊 كحلي")
        val bgColors = listOf(null,
            floatArrayOf(0.10f,0.11f,0.13f), floatArrayOf(0.02f,0.02f,0.02f),
            floatArrayOf(0.22f,0.24f,0.27f), floatArrayOf(0.92f,0.92f,0.92f),
            floatArrayOf(0.05f,0.08f,0.18f))
        val all = (listOf("── المادة ──") + matItems + bgItems).toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("🎨  المادة والمظهر")
            .setItems(all) { _, w ->
                when {
                    w == 0 -> {}
                    w <= materials.size -> { glViewerView.stlRenderer.setMaterial(materials[w-1])
                        Toast.makeText(context, materials[w-1].nameAr, Toast.LENGTH_SHORT).show() }
                    else -> bgColors.getOrNull(w - materials.size - 1)?.let { c ->
                        glViewerView.stlRenderer.setBackgroundColor(c[0], c[1], c[2]) }
                }
            }.show()
    }

    private fun cycleUnit() {
        currentUnit = when (currentUnit) {
            MeasurementUnit.MM   -> MeasurementUnit.CM
            MeasurementUnit.CM   -> MeasurementUnit.INCH
            MeasurementUnit.INCH -> MeasurementUnit.MM
        }
        btnUnit.text = currentUnit.label
        currentModel?.let { if (inspectionCard.visibility == View.VISIBLE) showInspectionReport(it) }
        val pts = glViewerView.stlRenderer.getMeasurementPoints()
        if (pts.size == 2) updateMeasurementText(pts[0], pts[1])
    }

    private fun exportCurrentView() {
        if (currentModel == null) { Toast.makeText(context,"افتح ملف أولاً",Toast.LENGTH_SHORT).show(); return }
        val r = glViewerView.stlRenderer
        val w = r.getSurfaceWidth(); val h = r.getSurfaceHeight()
        if (w <= 0 || h <= 0) return
        glViewerView.queueEvent {
            val bmp = r.captureFrame(w, h)
            requireActivity().runOnUiThread { saveAndShareBitmap(bmp) }
        }
    }

    private fun saveAndShareBitmap(bitmap: Bitmap) {
        try {
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "Amr3D_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "تصدير الصورة"))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر الحفظ", Toast.LENGTH_LONG).show()
        }
    }

    private fun openWhatsapp() {
        val phone = "201009172167"
        val msg = Uri.encode("مرحبًا، عندي استفسار بخصوص تطبيق Amr3D Preview")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=$phone&text=$msg")).apply { setPackage("com.whatsapp") })
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone?text=$msg")))
        }
    }

    private fun handleMeasurementTap(screenX: Float, screenY: Float) {
        val model = currentModel ?: return
        val r = glViewerView.stlRenderer
        val ray = RayPicker.screenPointToRay(screenX, screenY,
            r.getSurfaceWidth(), r.getSurfaceHeight(),
            r.getCurrentModelMatrix(), r.getCurrentViewMatrix(), r.getCurrentProjectionMatrix())
        val hit = RayPicker.findClosestIntersection(ray, model) ?: run {
            Toast.makeText(context,"لم يتم تحديد نقطة",Toast.LENGTH_SHORT).show(); return
        }
        r.addMeasurementPoint(hit)
        val pts = r.getMeasurementPoints()
        if (pts.size == 2) updateMeasurementText(pts[0], pts[1])
        else { measurementText.text = "نقطة أولى محددة — اضغط على نقطة ثانية"; measurementCard.visibility = View.VISIBLE }
    }

    private fun updateMeasurementText(p1: FloatArray, p2: FloatArray) {
        val d = MeasurementTools.distanceBetween(p1, p2, currentUnit)
        measurementText.text = String.format(Locale.US,"المسافة: %.3f %s", d, currentUnit.label)
        measurementCard.visibility = View.VISIBLE
    }

    private fun showInspectionReport(model: STLModel) {
        if (inspectionCard.visibility == View.VISIBLE) { inspectionCard.visibility = View.GONE; return }
        val report = MeasurementTools.inspect(model, currentUnit)
        val u = report.unit.label
        inspectionText.text = "📐 أبعاد الموديل\n─────────────────\n" +
            "الطول (X):    ${"%.2f".format(report.width)} $u\n" +
            "العرض (Y):   ${"%.2f".format(report.depth)} $u\n" +
            "الارتفاع (Z): ${"%.2f".format(report.height)} $u"
        inspectionCard.visibility = View.VISIBLE
        measurementCard.visibility = View.GONE
    }
}
