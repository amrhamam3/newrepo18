package com.amr3d.preview.pro

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val prefs = requireContext().getSharedPreferences("amr3d_prefs", Context.MODE_PRIVATE)

        // وحدة القياس
        val unitGroup = view.findViewById<RadioGroup>(R.id.unitGroup)
        when (prefs.getString("unit", "MM")) {
            "MM"   -> unitGroup.check(R.id.radioMM)
            "CM"   -> unitGroup.check(R.id.radioCM)
            "INCH" -> unitGroup.check(R.id.radioInch)
        }
        unitGroup.setOnCheckedChangeListener { _, id ->
            val unit = when (id) {
                R.id.radioMM   -> "MM"
                R.id.radioCM   -> "CM"
                R.id.radioInch -> "INCH"
                else -> "MM"
            }
            prefs.edit().putString("unit", unit).apply()
        }

        // تغيير الاسم
        view.findViewById<Button>(R.id.btnChangeName).setOnClickListener {
            val input = EditText(requireContext()).apply {
                hint = "اسمك الجديد"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setTextColor(0xFFF2F3F5.toInt())
                setHintTextColor(0xFF9CA3AF.toInt())
                setPadding(40, 24, 40, 24)
                textSize = 16f
                val saved = MainActivity.getUserName(requireContext())
                if (saved.isNotEmpty()) setText(saved)
            }
            AlertDialog.Builder(requireContext())
                .setTitle("👤  تغيير الاسم")
                .setView(input)
                .setPositiveButton("حفظ") { _, _ ->
                    val name = input.text.toString().trim().ifEmpty { "صديقي" }
                    MainActivity.saveUserName(requireContext(), name)
                    Toast.makeText(context, "✅ تم الحفظ: $name", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        // الإصدار
        val name = MainActivity.getUserName(requireContext())
        val greeting = if (name.isNotEmpty()) "مرحباً $name 👋\n\n" else ""
        view.findViewById<TextView>(R.id.tvVersion).text =
            "${greeting}🎮  Amr3D Preview Pro\nالإصدار 7.0\nAmr Hamam 3D © 2026"

        return view
    }
}
