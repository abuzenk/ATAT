package com.example.atat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.atat.adapters.MediaAdapter

class GalleryPickerActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button
    private lateinit var ivPreview: ImageView
    private lateinit var tvSelected: TextView
    private lateinit var tvMediaType: TextView
    private lateinit var previewLayout: View
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    
    private var selectedMediaUri: Uri? = null
    private var mediaList = mutableListOf<Uri>()
    private lateinit var adapter: MediaAdapter
    private var currentFilter = FilterType.ALL
    
    private var isCalledByExternalApp = false
    private var isVideoRequest = false
    private var outputUri: Uri? = null
    
    private val requiredPermissions = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        }
        else -> {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    enum class FilterType {
        ALL, IMAGES, VIDEOS
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            loadMediaFromGallery()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_picker)
        
        initViews()
        setupToolbar()
        checkIntentSource()
        checkPermissions()
        setupClickListeners()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnCancel = findViewById(R.id.btnCancel)
        ivPreview = findViewById(R.id.ivPreview)
        tvSelected = findViewById(R.id.tvSelected)
        tvMediaType = findViewById(R.id.tvMediaType)
        previewLayout = findViewById(R.id.previewLayout)
        toolbar = findViewById(R.id.toolbar)
        
        recyclerView.layoutManager = GridLayoutManager(this, 3)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            finishWithCancel()
        }
    }
    
    private fun checkIntentSource() {
        val action = intent.action
        
        isVideoRequest = action == MediaStore.ACTION_VIDEO_CAPTURE
        
        isCalledByExternalApp = action != null && (
                action == MediaStore.ACTION_IMAGE_CAPTURE ||
                action == MediaStore.ACTION_VIDEO_CAPTURE ||
                action == Intent.ACTION_GET_CONTENT ||
                action == Intent.ACTION_PICK
        )
        
        title = when {
            isVideoRequest -> "Pilih Video"
            action == MediaStore.ACTION_IMAGE_CAPTURE -> "Pilih Foto"
            else -> "atat"
        }
        
        supportActionBar?.title = title
        
        if (intent.hasExtra(MediaStore.EXTRA_OUTPUT)) {
            outputUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
            }
        }
    }
    
    private fun checkPermissions() {
        val allPermissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allPermissionsGranted) {
            loadMediaFromGallery()
        } else {
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Izin Diperlukan")
            .setMessage("Aplikasi memerlukan izin untuk mengakses media. Silakan berikan izin di pengaturan.")
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Tutup") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun loadMediaFromGallery(filter: FilterType = currentFilter) {
        mediaList.clear()
        
        val collections = when (filter) {
            FilterType.IMAGES -> listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            FilterType.VIDEOS -> listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            FilterType.ALL -> listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
        }
        
        for (collection in collections) {
            val isVideo = collection == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            
            val projection = arrayOf(
                if (isVideo) MediaStore.Video.Media._ID else MediaStore.Images.Media._ID,
                if (isVideo) MediaStore.Video.Media.DATE_ADDED else MediaStore.Images.Media.DATE_ADDED
            )
            
            val sortOrder = "${if (isVideo) MediaStore.Video.Media.DATE_ADDED else MediaStore.Images.Media.DATE_ADDED} DESC"
            
            val cursor = contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )
            
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(
                    if (isVideo) MediaStore.Video.Media._ID else MediaStore.Images.Media._ID
                )
                
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val uri = Uri.withAppendedPath(collection, id.toString())
                    mediaList.add(uri)
                }
            }
        }
        
        setupAdapter()
    }
    
    private fun setupAdapter() {
        adapter = MediaAdapter(isVideoRequest) { uri ->
            selectedMediaUri = uri
            updatePreview(uri)
        }
        
        recyclerView.adapter = adapter
        adapter.submitList(mediaList)
        
        if (mediaList.isEmpty()) {
            Toast.makeText(this, "Tidak ada media ditemukan", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updatePreview(uri: Uri) {
        tvSelected.text = "1 media dipilih"
        
        if (isVideoRequest || isVideoFile(uri)) {
            try {
                val bitmap = contentResolver.loadThumbnail(uri, android.util.Size(200, 200), null)
                ivPreview.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Glide.with(this).load(uri).into(ivPreview)
            }
            tvMediaType.text = "Video"
            tvMediaType.visibility = View.VISIBLE
        } else {
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(ivPreview)
            tvMediaType.text = "Foto"
            tvMediaType.visibility = View.VISIBLE
        }
    }
    
    private fun isVideoFile(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri)
        return mimeType?.startsWith("video/") == true
    }
    
    private fun setupClickListeners() {
        btnConfirm.setOnClickListener {
            returnSelectedMedia()
        }
        
        btnCancel.setOnClickListener {
            finishWithCancel()
        }
    }
    
    private fun returnSelectedMedia() {
        if (selectedMediaUri == null) {
            Toast.makeText(this, 
                if (isVideoRequest) "Pilih video terlebih dahulu" else "Pilih gambar terlebih dahulu", 
                Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isCalledByExternalApp) {
            if (isVideoRequest && !isVideoFile(selectedMediaUri!!)) {
                Toast.makeText(this, "Harap pilih file video", Toast.LENGTH_SHORT).show()
                return
            }
            if (!isVideoRequest && isVideoFile(selectedMediaUri!!)) {
                Toast.makeText(this, "Harap pilih file gambar", Toast.LENGTH_SHORT).show()
                return
            }
            
            handleExternalReturn()
        } else {
            Toast.makeText(this, "Media dipilih: $selectedMediaUri", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK, Intent().apply { data = selectedMediaUri })
            finish()
        }
    }
    
    private fun handleExternalReturn() {
        try {
            if (outputUri != null) {
                contentResolver.openInputStream(selectedMediaUri!!)?.use { inputStream ->
                    contentResolver.openOutputStream(outputUri!!)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                val resultIntent = Intent()
                resultIntent.data = outputUri
                resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setResult(RESULT_OK, resultIntent)
            } else {
                val resultIntent = Intent()
                resultIntent.data = selectedMediaUri
                resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setResult(RESULT_OK, resultIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal memproses media", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
        }
        
        finish()
    }
    
    private fun finishWithCancel() {
        setResult(RESULT_CANCELED)
        finish()
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_picker, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter_all -> {
                currentFilter = FilterType.ALL
                isVideoRequest = false
                supportActionBar?.title = "Semua Media"
                loadMediaFromGallery(FilterType.ALL)
                true
            }
            R.id.action_filter_images -> {
                currentFilter = FilterType.IMAGES
                isVideoRequest = false
                supportActionBar?.title = "Gambar"
                loadMediaFromGallery(FilterType.IMAGES)
                true
            }
            R.id.action_filter_videos -> {
                currentFilter = FilterType.VIDEOS
                isVideoRequest = true
                supportActionBar?.title = "Video"
                loadMediaFromGallery(FilterType.VIDEOS)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onBackPressed() {
        finishWithCancel()
    }
}
