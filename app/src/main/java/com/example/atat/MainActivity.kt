package com.example.atat

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val tvInfo = findViewById<TextView>(R.id.tvInfo)
        val btnOpenGallery = findViewById<Button>(R.id.btnOpenGallery)
        
        tvInfo.text = """
            📸 ATAT
            
            Aplikasi ini terdaftar sebagai aplikasi kamera dan camcorder di sistem.
            
            🎯 CARA MENGGUNAKAN:
            
            1. Buka aplikasi lain yang membutuhkan kamera (WhatsApp, Bank, dll)
            2. Saat diminta mengambil foto/video, pilih 'atat'
            3. Pilih gambar/video dari galeri
            4. Media yang dipilih akan dikembalikan sebagai hasil jepretan/rekaman
            
            📷 MENDUKUNG:
            • Foto (ACTION_IMAGE_CAPTURE)
            • Video (ACTION_VIDEO_CAPTURE)
            
            👇 Tekan tombol di bawah untuk preview
        """.trimIndent()
        
        btnOpenGallery.setOnClickListener {
            // Buka GalleryPickerActivity langsung sebagai demo
            startActivity(Intent(this, GalleryPickerActivity::class.java))
        }
    }
}
