package io.sweers.palettehelper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.util.Patterns
import android.view.View
import android.webkit.WebView
import android.widget.EditText
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.properties.Delegates

public class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.d("Starting up MainActivity.")
        PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_ENTER, ANALYTICS_NAV_MAIN)
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction().add(R.id.container, SettingsFragment.newInstance()).commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PaletteHelperApplication.mixPanel.flush()
    }

    public class SettingsFragment : PreferenceFragment() {

        private var imagePath: String by Delegates.notNull()
        private val REQUEST_LOAD_IMAGE = 1
        private val REQUEST_IMAGE_CAPTURE = 2

        companion object {
            public fun newInstance(): SettingsFragment {
                return SettingsFragment()
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            Timber.d("Starting up PreferenceFragment.")
            retainInstance = true
            addPreferencesFromResource(R.xml.main_activity_pref_screen)

            // Hide pick intent option if it's not possible. Should be rare though
            Timber.d("Checking for pick intent.")
            if (createPickIntent() == null) {
                Timber.d("No pick option available, disabling.")
                (findPreference("pref_key_cat_palette") as PreferenceCategory).removePreference(findPreference("pref_key_open"))
            }

            // Hide the camera option if it's not possible
            Timber.d("Checking for camera intent.")
            if (createCameraIntent() == null) {
                Timber.d("No camera option available, disabling.")
                (findPreference("pref_key_cat_palette") as PreferenceCategory).removePreference(findPreference("pref_key_camera"))
            }
        }

        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
            Timber.d("Clicked preference ${preference.key}")
            when (preference.key) {
                "pref_key_open" -> {
                    PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_MAIN, ANALYTICS_NAV_INTERNAL)
                    val i = createPickIntent()
                    startActivityForResult(i, REQUEST_LOAD_IMAGE)
                    return true
                }
                "pref_key_camera" -> {
                    PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_MAIN, ANALYTICS_NAV_CAMERA)
                    dispatchTakePictureIntent()
                    return true;
                }
                "pref_key_url" -> {
                    PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_MAIN, ANALYTICS_NAV_URL)
                    val inputView = View.inflate(activity, R.layout.basic_edittext_dialog, null);
                    val input = inputView.findViewById(R.id.et) as EditText
                    val clipText = getClipData(activity)

                    // If there's a URL in the clipboard, guess that that's what they want to retrieve and autofill
                    if (Patterns.WEB_URL.matcher(clipText).matches()) {
                        input.setText(clipText)
                        input.setSelection(clipText.length)
                    }
                    MaterialDialog.Builder(activity)
                            .title(R.string.main_open_url)
                            .customView(inputView, false)
                            .positiveText(R.string.dialog_done)
                            .negativeText(R.string.dialog_cancel)
                            .autoDismiss(false)
                            .onPositive { dialog, dialogAction ->
                                var isValid: Boolean
                                val inputText: String = input.text.toString().trim().replace(" ", "")
                                if (Patterns.WEB_URL.matcher(inputText).matches()) {
                                    isValid = true
                                    input.error = ""
                                } else {
                                    isValid = false
                                    input.error = getString(R.string.main_open_url_error)
                                }
                                if (isValid) {
                                    dialog.dismiss()
                                    val intent = Intent(Intent.ACTION_SEND)
                                    intent.setClass(activity, PaletteDetailActivity::class.java)
                                    intent.putExtra(Intent.EXTRA_TEXT, inputText)
                                    startActivity(intent)
                                }
                            }
                            .onNegative { dialog, dialogAction ->
                                dialog?.dismiss()
                            }
                            .show()
                    return true;
                }
                "pref_key_dev" -> {
                    MaterialDialog.Builder(activity)
                        .title(R.string.main_about)
                        .content(Html.fromHtml(getString(R.string.about_body)))
                        .positiveText(R.string.dialog_done)
                        .show()
                    return true;
                }
                "pref_key_licenses" -> {
                    val webView = WebView(activity);
                    webView.loadUrl("file:///android_asset/licenses.html");
                    MaterialDialog.Builder(activity)
                            .title(R.string.main_licenses)
                            .customView(webView, false)
                            .positiveText(R.string.dialog_done)
                            .show();
                    return true
                }
                "pref_key_source" -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse("https://github.com/hzsweers/palettehelper"))
                    startActivity(intent)
                    return true;
                }
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference)
        }

        /**
         * For images captured from the camera, we need to create a File first to tell the camera
         * where to store the image.
         *
         * @return the File created for the image to be store under.
         */
        fun createImageFile(): File {
            Timber.d("Creating imageFile")
            // Create an image file name
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date());
            val imageFileName = "JPEG_" + timeStamp + "_";
            val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            val imageFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
                    );

            // Save a file: path for use with ACTION_VIEW intents
            imagePath = imageFile.absolutePath;
            return imageFile;
        }

        /**
         * This checks to see if there is a suitable activity to handle the `ACTION_PICK` intent
         * and returns it if found. `ACTION_PICK` is for picking an image from an external app.
         *
         * @return A prepared intent if found.
         */
        fun createPickIntent(): Intent? {
            val picImageIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            if (picImageIntent.resolveActivity(activity.packageManager) != null) {
                return picImageIntent
            } else {
                return null
            }
        }

        /**
         * This checks to see if there is a suitable activity to handle the `ACTION_IMAGE_CAPTURE`
         * intent and returns it if found. `ACTION_IMAGE_CAPTURE` is for letting another app take
         * a picture from the camera and store it in a file that we specify.
         *
         * @return A prepared intent if found.
         */
        fun createCameraIntent(): Intent? {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
                return takePictureIntent
            } else {
                return null
            }
        }

        /**
         * This utility function combines the camera intent creation and image file creation, and
         * ultimately fires the intent.
         *
         * @see createCameraIntent()
         * @see createImageFile()
         */
        fun dispatchTakePictureIntent() {
            val takePictureIntent = createCameraIntent()
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent != null) {
                // Create the File where the photo should go
                try {
                    val imageFile = createImageFile();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
                    Timber.d("Dispatching intent to take a picture.")
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            Timber.d("Received activity result.")

            if (resultCode == Activity.RESULT_OK) {
                val intent = Intent(activity, PaletteDetailActivity::class.java)
                if (requestCode == REQUEST_LOAD_IMAGE && data != null) {
                    Timber.d("Activity result - loading image from internal storage.")
                    val selectedImage = data.data
                    if (selectedImage != null) {
                        intent.putExtra(PaletteDetailActivity.KEY_URI, selectedImage.toString())
                        startActivity(intent)
                    } else {
                        Toast.makeText(activity, R.string.generic_error, Toast.LENGTH_SHORT).show()
                    }
                } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    Timber.d("Activity result - loading image from camera capture.")
                    intent.putExtra(PaletteDetailActivity.KEY_CAMERA, imagePath)
                    startActivity(intent);
                }
            }
        }

    }
}
