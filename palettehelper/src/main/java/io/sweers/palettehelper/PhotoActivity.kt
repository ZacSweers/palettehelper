package io.sweers.palettehelper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.transition.ChangeImageTransform
import android.transition.Explode
import android.transition.Transition
import android.view.View
import android.view.Window
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
        val EXTRA_TRANSITION_NAME = "photo_hero"

        public fun launch(activity: Activity, transitionView: View, url: String) {
            val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                    transitionView, EXTRA_TRANSITION_NAME)
            val intent: Intent = Intent(activity, PhotoActivity::class.java)
            intent.putExtra(EXTRA_URI, url);
            ActivityCompat.startActivity(activity, intent, options.toBundle());
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        if (!ImageLoader.getInstance().isInited) {
            val config: ImageLoaderConfiguration = ImageLoaderConfiguration.Builder(applicationContext).build();
            ImageLoader.getInstance().init(config);
        }

        val intent = intent
        val uri = intent.getStringExtra(EXTRA_URI)

        ImageLoader.getInstance().displayImage(uri, photoView);
    }
}
