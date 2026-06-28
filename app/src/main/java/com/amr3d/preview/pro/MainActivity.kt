package com.amr3d.preview.pro

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    // SoundPool للصوت بأسلوب PS5
    private lateinit var soundPool: SoundPool
    private var navSoundId = 0
    private var soundLoaded = false

    // Fragments — lazy لتجنب إنشاء غير ضروري
    private val viewerFragment   by lazy { ViewerFragment() }
    private val fileBrowserFragment by lazy { FileBrowserFragment() }
    private val slicerFragment   by lazy { SlicerFragment() }
    private val historyFragment  by lazy { HistoryFragment() }
    private val settingsFragment by lazy { SettingsFragment() }

    // تتبع الصفحة الحالية للانيميشن الاتجاهي
    private val navOrder = listOf(
        R.id.nav_viewer, R.id.nav_files, R.id.nav_slicer,
        R.id.nav_history, R.id.nav_settings
    )
    private var currentNavIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSound()
        bottomNav = findViewById(R.id.bottomNav)
        setupNavigation()

        // فتح من خارج التطبيق
        val fileUri = intent?.data
        if (fileUri != null) {
            showFragment(viewerFragment, 0, 0)
            bottomNav.selectedItemId = R.id.nav_viewer
            viewerFragment.loadFile(fileUri)
        } else {
            showFragment(viewerFragment, 0, 0)
        }
    }

    private fun initSound() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build()
        soundPool.setOnLoadCompleteListener { _, _, status ->
            soundLoaded = (status == 0)
        }
        navSoundId = soundPool.load(this, R.raw.nav_click, 1)
    }

    private fun playNavSound() {
        if (soundLoaded) {
            soundPool.play(navSoundId, 0.6f, 0.6f, 1, 0, 1.0f)
        }
    }

    private fun setupNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            val newIndex = navOrder.indexOf(item.itemId)
            if (newIndex == currentNavIndex) return@setOnItemSelectedListener false

            playNavSound()

            val goingRight = newIndex > currentNavIndex
            val enterAnim = if (goingRight) R.anim.slide_in_right else R.anim.slide_in_left
            val exitAnim  = if (goingRight) R.anim.slide_out_left else R.anim.slide_out_right

            val fragment = when (item.itemId) {
                R.id.nav_viewer   -> viewerFragment
                R.id.nav_files    -> fileBrowserFragment.also { f ->
                    f.fileSelectedListener = object : FileBrowserFragment.OnFileSelectedListener {
                        override fun onFileSelected(file: File) {
                            val uri = Uri.fromFile(file)
                            HistoryFragment.addToHistory(this@MainActivity, file.absolutePath)
                            viewerFragment.loadFile(uri)
                            bottomNav.selectedItemId = R.id.nav_viewer
                        }
                    }
                }
                R.id.nav_slicer   -> slicerFragment
                R.id.nav_history  -> historyFragment.also { f ->
                    f.fileSelectedListener = object : HistoryFragment.OnFileSelectedListener {
                        override fun onFileSelected(file: File) {
                            viewerFragment.loadFile(Uri.fromFile(file))
                            bottomNav.selectedItemId = R.id.nav_viewer
                        }
                    }
                }
                R.id.nav_settings -> settingsFragment
                else -> viewerFragment
            }

            showFragment(fragment, enterAnim, exitAnim)
            currentNavIndex = newIndex
            true
        }
    }

    fun showFragment(fragment: Fragment, enterAnim: Int, exitAnim: Int) {
        val tx = supportFragmentManager.beginTransaction()
        if (enterAnim != 0) tx.setCustomAnimations(enterAnim, exitAnim)
        tx.replace(R.id.fragmentContainer, fragment)
        tx.commitAllowingStateLoss()
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            viewerFragment.loadFile(uri)
            bottomNav.selectedItemId = R.id.nav_viewer
            showFragment(viewerFragment, R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }

    companion object {
        fun getUserName(context: Context): String =
            context.getSharedPreferences("amr3d_prefs", Context.MODE_PRIVATE)
                .getString("user_name", "") ?: ""

        fun saveUserName(context: Context, name: String) =
            context.getSharedPreferences("amr3d_prefs", Context.MODE_PRIVATE)
                .edit().putString("user_name", name).apply()
    }
}
