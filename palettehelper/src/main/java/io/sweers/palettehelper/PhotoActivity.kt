package io.sweers.palettehelper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import butterknife.bindView
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
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

        if (!ImageLoader.getInstance().isInited()) {
            val config: ImageLoaderConfiguration = ImageLoaderConfiguration.Builder(getApplicationContext()).build();
            ImageLoader.getInstance().init(config);
        }

        val intent = getIntent()
        val uri = intent.getStringExtra(EXTRA_URI)

        ImageLoader.getInstance().displayImage(uri, photoView);
    }
}
