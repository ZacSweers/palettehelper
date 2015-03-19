package io.sweers.palettehelper

import android.support.v7.app.ActionBarActivity
import android.os.Bundle
import uk.co.senab.photoview.PhotoView
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration

/**
 * Created by hsweers on 3/19/15.
 */
class PhotoActivity: ActionBarActivity() {

    class object {
        val EXTRA_URI = "extra_uil_uri"
    }

    val photoView: PhotoView by bindView(R.id.photo_view)

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