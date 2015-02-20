package io.sweers.palettehelper

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.ActionBarActivity
import android.support.v7.graphics.Palette
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import com.afollestad.materialdialogs.MaterialDialog
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersSimpleAdapter

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays

import android.support.v7.graphics.Palette.Swatch
import com.afollestad.materialdialogs.GravityEnum
import android.widget.EditText
import android.preference.PreferenceManager
import com.afollestad.materialdialogs.Theme
import android.text.InputType
import kotlin.properties.Delegates
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES10
import timber.log.Timber
import java.io.FileNotFoundException
import android.view.Gravity

public class PaletteDetailActivity : ActionBarActivity() {

    class object {
        val KEY_URI = "uri_path"
        val KEY_CAMERA = "camera"
    }

    val DEFAULT_NUM_COLORS = 16
    val toolbar: Toolbar by bindView(R.id.toolbar)
    val gridView: StickyGridHeadersGridView by bindView(R.id.gv)
    val imageView: ImageView by bindView(R.id.iv)
    var bitmap: Bitmap by Delegates.notNull()

    // Extension functions to Swatch to get hex values
    public fun Swatch.rgbHex(): String = rgbToHex(getRgb())
    public fun Swatch.titleHex(): String = rgbToHex(getTitleTextColor())
    public fun Swatch.bodyHex(): String = rgbToHex(getBodyTextColor())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_palette_detail)

        Timber.d("Starting up DetailActivity")
        PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_ENTER, ANALYTICS_NAV_DETAIL)

        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.bringToFront()

        Timber.d("Reading intent.")
        val intent = getIntent()
        val action = intent.getAction()
        val type: String? = intent.getType()
        when {
            Intent.ACTION_SEND == action && type != null && type.startsWith("image/") -> {
                Timber.d("Received via app share, trying to resolve uri")
                PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_SHARE, ANALYTICS_NAV_DETAIL)
                val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (imageUri != null) {
                    try {
                        Timber.d("Trying to retrieve bitmap")
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri)
                    } catch (e: IOException) {
                        Timber.e("Bitmap could not be retrieved")
                        Toast.makeText(this, "Could not resolve image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Timber.e("No image URI provided?")
                }
            }
            intent.hasExtra(KEY_URI) -> {
                Timber.d("Uri specified, trying to decode file")
                val path = intent.getStringExtra(KEY_URI)
                try {
                    // TODO This is blocking, and can wait for a download to finish. Need to background this
                    val imageStream = getContentResolver().openInputStream(Uri.parse(path));
                    bitmap = BitmapFactory.decodeStream(imageStream)
                } catch (e: FileNotFoundException) {
                    errorOut()
                }
            }
            intent.hasExtra(KEY_CAMERA) -> {
                Timber.d("Path specified, trying to decode file")
                val path = intent.getStringExtra(KEY_CAMERA)
                bitmap = BitmapFactory.decodeFile(path)
            }
            else -> {
                errorOut()
            }
        }

        Timber.d("Setting up imageview with bitmap")
        val maxTextureSize = IntArray(1)
        GLES10.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
        val maxBitmapDimension = Math.max(maxTextureSize[0], 2048);
        var width = bitmap.getWidth()
        var height = bitmap.getHeight()
        if (width > maxBitmapDimension || height > maxBitmapDimension) {
            Timber.d("Gotta scale the bitmap")
            if (width > height) {
                // landscape
                val ratio = width.toFloat() / maxBitmapDimension.toFloat()
                width = maxBitmapDimension;
                height = (height / ratio).toInt()
            } else if (height > width) {
                // portrait
                val ratio = height.toFloat() / maxBitmapDimension.toFloat()
                height = maxBitmapDimension;
                width = (width / ratio).toInt()
            } else {
                // square
                height = maxBitmapDimension;
                width = maxBitmapDimension;
            }

            imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, width, height, true))
        } else {
            imageView.setImageBitmap(bitmap)
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_key_default", true)) {
            generatePalette(bitmap, DEFAULT_NUM_COLORS)
        } else {
            Timber.d("Prompting for number of colors first")
            promptForNumColors(bitmap)
        }
    }

    private fun errorOut() {
        Timber.e("Given an intent, but we can't do anything with the provided info.")
        Toast.makeText(this, "Invalid arguments", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun promptForNumColors(bitmap: Bitmap) {
        // TODO Find a way to stick this info into the dialog that doesn't suck
//        val info = "You may have seen above that you can specify the palette size. The higher the number, the longer it takes to generate a palette. The lower the number, the less colors we have to choose from. The best number to use depends on the image type:\n* Contact images/avatars: optimal values are 24-32\n* Landscapes: optimal values are 8-16\n\nThe default value is 16 which is good compromise and works well in most situations."
        val input = EditText(this)
        input.setInputType(InputType.TYPE_CLASS_NUMBER)
        input.setTextColor(Color.BLACK)
        MaterialDialog.Builder(this)
                .title("Number of colors?")
                .customView(input, false)
                .positiveText("Generate")
                .negativeText("Cancel")
                .neutralText("Default")
                .autoDismiss(false)
                .callback(object : MaterialDialog.ButtonCallback() {
                    override fun onPositive(dialog: MaterialDialog) {
                        val result = validateInput()
                        if (result.isValid) {
                            generatePalette(bitmap, result.value)
                            dialog.dismiss()
                            PaletteHelperApplication.mixPanel.trackMisc(ANALYTICS_KEY_NUMCOLORS, result.value.toString())
                        }
                    }

                    override fun onNegative(dialog: MaterialDialog) {
                        dialog.dismiss()
                        finish()
                    }

                    override fun onNeutral(dialog: MaterialDialog) {
                        dialog.dismiss()
                        generatePalette(bitmap, DEFAULT_NUM_COLORS)
                    }

                    data inner class Result(val isValid: Boolean, val value: Int)
                    fun validateInput(): Result {
                        val inputText: String = input.getText().toString()
                        try {
                            val number = java.lang.Integer.parseInt(inputText)
                            if (number < 1) {
                                input.setError("Must be greater than 1")
                                return Result(false, -1)
                            }
                            return Result(true, number)
                        } catch (e: Exception) {
                            input.setError("Invalid input")
                            return Result(false, -1)
                        }
                    }
                })
                .cancelListener({dialog ->
                    dialog.dismiss()
                    finish()
                })
                .show()
    }

    private fun generatePalette(bitmap: Bitmap, numColors: Int = 16) {
        Timber.d("Generating palette")
        Palette.generateAsync(bitmap, numColors, { palette ->
            Timber.d("Palette generation done with ${palette.getSwatches().size()} colors extracted of ${numColors} requested")
            val swatches = ArrayList(Arrays.asList<Swatch>(*array(
                    palette.getVibrantSwatch(),
                    palette.getMutedSwatch(),
                    palette.getDarkVibrantSwatch(),
                    palette.getDarkMutedSwatch(),
                    palette.getLightVibrantSwatch(),
                    palette.getLightMutedSwatch())
            ))
            swatches.addAll(palette.getSwatches())

            Timber.d("Setting up adapter with swatches")
            val adapter = ResultsAdapter(swatches)
            gridView.setAdapter(adapter)
            gridView.setOnItemClickListener(adapter)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        PaletteHelperApplication.mixPanel.flush()
    }

    private inner class ResultsAdapter(private val swatches: List<Swatch>) : BaseAdapter(),
            AdapterView.OnItemClickListener, StickyGridHeadersSimpleAdapter {

        private val swatchNames = array(
                "Vibrant",
                "Muted",
                "DarkVibrant",
                "DarkMuted",
                "LightVibrant",
                "LightMuted"
        )

        override fun getCount(): Int {
            return swatches.size()
        }

        override fun getItem(position: Int): Swatch? {
            return swatches.get(position)
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun isEnabled(position: Int): Boolean {
            return getItem(position) != null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val swatch = getItem(position)
            var holder: ViewHolder
            var convertViewCopy = convertView

            if (convertViewCopy == null) {
                convertViewCopy = getLayoutInflater().inflate(R.layout.swatch_cell, parent, false)
                holder = ViewHolder()
                holder.text = convertViewCopy?.findViewById(R.id.hex) as TextView
                convertViewCopy?.setTag(holder)
            } else {
                holder = convertViewCopy?.getTag() as ViewHolder
            }

            if (swatch == null) {
                holder.text?.setText("No swatch for ${swatchNames[position]} :(")
                holder.text?.setTextColor(Color.parseColor("#ADADAD"))
                convertViewCopy?.setBackgroundColor(Color.parseColor("#252626"))
            } else {
                convertViewCopy?.setBackgroundColor(swatch.getRgb())
                holder.text?.setTextColor(swatch.getTitleTextColor())
                val hex = rgbToHex(swatch.getRgb())
                if (position < 6) {
                    holder.text?.setText("${swatchNames[position]}\n${hex}")
                } else {
                    holder.text?.setText(hex)
                }
            }
            return convertViewCopy as View
        }

        override fun getHeaderId(position: Int): Long {
            if (position < 6) {
                return 0
            } else {
                return 1
            }
        }

        override fun getHeaderView(position: Int, convertView: View?, parent: ViewGroup): View {
            val textView = getLayoutInflater().inflate(R.layout.header_row, parent, false) as TextView
            textView.setBackgroundColor(Color.WHITE)
            textView.setTextColor(Color.BLACK)
            textView.setGravity(Gravity.START)
            val text: String
            if (getHeaderId(position).toInt() == 0) {
                text = "Primary Swatches"
            } else {
                text = "All Swatches"
            }
            textView.setText(text)
            return textView
        }

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            Timber.d("Swatch item clicked")
            val swatch = getItem(position)
            val title = if (position < 6)  swatchNames[position] else "Lorem ipsum"
            if (swatch != null) {
                Timber.d("Swatch wasn't null, building dialog")
                MaterialDialog.Builder(this@PaletteDetailActivity)
                        .theme(if (swatch.getHsl()[2] > 0.5f) Theme.LIGHT else Theme.DARK)
                        .titleGravity(GravityEnum.CENTER)
                        .titleColor(swatch.getTitleTextColor())
                        .title(title)
                        .backgroundColor(swatch.getRgb())
                        .contentColor(swatch.getBodyTextColor())
                        .content("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
                        .positiveText("Done")
                        .positiveColor(swatch.getBodyTextColor())
                        .neutralText("Values")
                        .neutralColor(swatch.getBodyTextColor())
                        .callback(object : MaterialDialog.ButtonCallback() {
                            override fun onPositive(dialog: MaterialDialog) = dialog.dismiss()
                            override fun onNeutral(dialog: MaterialDialog) {
                                dialog.dismiss()
                                showValues(swatch)
                            }
                        })
                        .show()
            }
        }

        private fun showValues(swatch: Swatch) {
            Timber.d("Showing values")
            val items = array(
                    "RGB: ${swatch.rgbHex()}",
                    "Title: ${swatch.titleHex()}",
                    "Body: ${swatch.bodyHex()}",
                    "Hue: ${swatch.getHsl()[0]}",
                    "Saturation: ${swatch.getHsl()[1]}",
                    "Luminosity: ${swatch.getHsl()[2]}",
                    "Population: ${swatch.getPopulation()}"
            )
            MaterialDialog.Builder(this@PaletteDetailActivity)
                    .theme(if (swatch.getHsl()[2] > 0.5f) Theme.LIGHT else Theme.DARK)
                    .backgroundColor(swatch.getRgb())
                    .contentColor(swatch.getBodyTextColor())
                    .items(items)
                    .itemsCallback(object : MaterialDialog.ListCallback {
                        override fun onSelection(dialog: MaterialDialog?, view: View?, position: Int, value: CharSequence?) {
                            when (position) {
                                0 -> copyAndNotify(this@PaletteDetailActivity, swatch.rgbHex())
                                1 -> copyAndNotify(this@PaletteDetailActivity, swatch.titleHex())
                                2 -> copyAndNotify(this@PaletteDetailActivity, swatch.bodyHex())
                                3 -> copyAndNotify(this@PaletteDetailActivity, swatch.getHsl()[0].toString())
                                4 -> copyAndNotify(this@PaletteDetailActivity, swatch.getHsl()[1].toString())
                                5 -> copyAndNotify(this@PaletteDetailActivity, swatch.getHsl()[2].toString())
                                6 -> copyAndNotify(this@PaletteDetailActivity, swatch.getPopulation().toString())
                            }
                            dialog?.dismiss()
                        }
                    })
                    .forceStacking(true)
                    .positiveText("Copy all to clipboard")
                    .positiveColor(swatch.getBodyTextColor())
                    .callback(object : MaterialDialog.ButtonCallback() {
                        override fun onPositive(dialog: MaterialDialog) {
                            copyAndNotify(this@PaletteDetailActivity, swatch.toString())
                            dialog.dismiss()
                        }
                    })
                    .show()
        }

        inner class ViewHolder {
            var text: TextView? = null
        }
    }
}
