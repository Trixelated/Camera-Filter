package com.example.camerafilter.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.example.camerafilter.databinding.ActivityCameraBinding
import com.google.android.material.button.MaterialButton

class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            var isScrolling = false

            scroll.post {
                val totalWidth = scroll.width
                filterWb.post {
                    val itemWidth = filterWb.width
                    val space = totalWidth.div(2).minus(itemWidth / 2)

                    spaceLeft.updateLayoutParams {
                        width = space
                    }

                    spaceRight.updateLayoutParams {
                        width = space
                    }

                    filterWb.isSelected = true

                    filterWb.setOnClickListener {
                       it.updateSelection(scanFilter)
                        scanBtn.isSelected = false
                        scanBtn.isVisible = false

                        isScrolling = true
                        scroll.scrollTo(space - itemWidth.div(2), 0)
                        if (!cameraView.modeColorFilter) {

                            cameraView.modeColorFilter = true
                        }
                        isScrolling = false
                    }

                    scanFilter.setOnClickListener {
                        it.updateSelection(filterWb)
                        scanBtn.isVisible = true

                        isScrolling = true
                        scroll.scrollTo(space + itemWidth, 0)
                        if (cameraView.modeColorFilter) {
                            cameraView.modeColorFilter = false
                        }
                        isScrolling = false
                    }

                }

                scroll.setOnTouchListener { _, event ->
                   when(event.action){
                       MotionEvent.ACTION_DOWN ->{
                           isScrolling = true
                       }

                       MotionEvent.ACTION_UP ->{
                           isScrolling = false
                       }
                   }
                    false
                }

                scroll.setOnScrollChangeListener { view, i, i2, i3, i4 ->
                  if (!isScrolling){

                      Log.i(Companion.TAG, "onCreate: XScroll $i, ${scroll.width}, ${filterWb.width.div(2).plus(10)}")
                      if (i >= filterWb.width.div(2).plus(10)){
                          scanFilter.performClick()
                      }else{
                          filterWb.performClick()
                      }
                  }
                }


            }

            scanBtn.setOnClickListener {
                cameraView.setScanVideo(!cameraView.isScanVideo())
                it.isSelected = cameraView.isScanVideo()
            }


        }

    }

    private fun View.updateSelection(btn: MaterialButton) {
        isSelected = true
        btn.isSelected = false
    }

    companion object {
        private const val TAG = "CameraActivity"
    }
}