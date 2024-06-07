package com.example.camerafilter.ui

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.camerafilter.databinding.ActivityMainBinding
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.openCam.setOnClickListener {
            TedPermission.create()
                .setDeniedMessage("Camera Permission Required")
                .setPermissions(Manifest.permission.CAMERA)
                .setPermissionListener(object : PermissionListener {
                    override fun onPermissionGranted() {
                        startActivity(Intent(this@MainActivity, CameraActivity::class.java))
                    }

                    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    }

                })
                .check()
        }
    }


}