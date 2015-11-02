package io.sweers.palettehelper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import butterknife.bindView
import com.bumptech.glide.Glide
import uk.co.senab.photoview.PhotoView

/**
 * Displays an image using the PhotoView library
 */
class PhotoActivity: AppCompatActivity() {

    val photoView: PhotoView by bindView(R.id.photo_view)

    companion object {
        val EXTRA_URI = "extra_uil_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        val intent = getIntent()
        val uri = intent.getStringExtra(EXTRA_URI)

        Glide.with(this)
                .load(uri)
                .crossFade()
                .into(photoView)
    }
}
