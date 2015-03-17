package io.sweers.palettehelper

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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

import java.util.ArrayList
import java.util.Arrays

import android.support.v7.graphics.Palette.Swatch
import com.afollestad.materialdialogs.GravityEnum
import android.widget.EditText
import android.preference.PreferenceManager
import com.afollestad.materialdialogs.Theme
import timber.log.Timber
import android.view.Gravity
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.assist.FailReason
import android.os.Handler
import android.os.Looper

public class PaletteDetailActivity : ActionBarActivity() {

    class object {
        val KEY_URI = "uri_path"
        val KEY_CAMERA = "camera"
    }

    val DEFAULT_NUM_COLORS = 16
    val toolbar: Toolbar by bindView(R.id.toolbar)
    val gridView: StickyGridHeadersGridView by bindView(R.id.gv)
    val imageView: ImageView by bindView(R.id.iv)
    var active = true;

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

        Timber.d("Reading intent.")
        val intent = getIntent()
        val action = intent.getAction()
        val type: String? = intent.getType()
        var imageUri: String? = null
        when {
            Intent.ACTION_SEND == action && type != null && type.startsWith("image/") -> {
                Timber.d("Received via app share, trying to resolve uri")
                PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_SHARE, ANALYTICS_NAV_DETAIL)
                imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM).toString()
            }
            intent.hasExtra(KEY_URI) -> {
                Timber.d("Uri specified, trying to decode file")
                imageUri = intent.getStringExtra(KEY_URI)
            }
            intent.hasExtra(KEY_CAMERA) -> {
                Timber.d("Path specified, trying to decode file")
                imageUri = "file://${intent.getStringExtra(KEY_CAMERA)}"
            }
            Intent.ACTION_SEND == action -> {
                imageUri = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
            else -> {
                errorOut()
            }
        }

        if (imageUri != null) {
            val dialog = MaterialDialog.Builder(this)
                    .content(R.string.detail_loading_image)
                    .progress(true, 0)
                    .build()

            val runnable: Runnable = Runnable { if (active) dialog.show() }
            val handler = Handler(Looper.getMainLooper());
            handler.postDelayed(runnable, 500)  // Wait half a second before showing the dialog to avoid flashing effect if it loads fast

            ImageLoader.getInstance().init(ImageLoaderConfiguration.Builder(this).build());
            ImageLoader.getInstance().displayImage(imageUri, imageView, object : SimpleImageLoadingListener() {
                override fun onLoadingComplete(imageUri: String, view: View, loadedImage: Bitmap) {
                    handler.removeCallbacks(runnable)
                    if (dialog.isShowing()) {
                        dialog.dismiss()
                    }
                    if (PreferenceManager.getDefaultSharedPreferences(this@PaletteDetailActivity).getBoolean("pref_key_default", true)) {
                        generatePalette(loadedImage, DEFAULT_NUM_COLORS)
                    } else {
                        Timber.d("Prompting for number of colors first")
                        promptForNumColors(loadedImage)
                    }
                }

                override fun onLoadingFailed(imageUri: String?, view: View?, failReason: FailReason?) {
                    Timber.e("Invalid imageUri: %s", failReason?.getType()?.name())
                    errorOut()
                }
            });
        } else {
            Timber.e("Invalid imageUri")
            errorOut()
        }
    }

    override fun onResume() {
        super.onResume()
        active = true
    }

    override fun onPause() {
        super.onPause()
        active = false
    }

    private fun errorOut() {
        Timber.e("Given an intent, but we can't do anything with the provided info.")
        Toast.makeText(this, getString(R.string.detail_invalid_input), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun promptForNumColors(bitmap: Bitmap) {
        val inputView = View.inflate(this, R.layout.colors_prompt, null);
        val input = inputView.findViewById(R.id.et) as EditText
        MaterialDialog.Builder(this)
                .title(R.string.dialog_num_colors)
                .customView(inputView, true)
                .positiveText(R.string.dialog_generate)
                .negativeText(R.string.dialog_cancel)
                .neutralText(R.string.dialog_default)
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
                                input.setError(getString(R.string.detail_must_be_greater_than_one))
                                return Result(false, -1)
                            }
                            return Result(true, number)
                        } catch (e: Exception) {
                            input.setError(getString(R.string.detail_invalid_input))
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

        private val swatchNames = getResources().getStringArray(R.array.swatches)

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
                holder.text?.setText(getString(R.string.detail_no_swatch, swatchNames[position]))
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
                text = getString(R.string.detail_primary_swatches)
            } else {
                text = getString(R.string.detail_all_swatches)
            }
            textView.setText(text)
            return textView
        }

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            Timber.d("Swatch item clicked")
            val swatch = getItem(position)
            val title = if (position < 6)  swatchNames[position] else getString(R.string.detail_lorem)
            if (swatch != null) {
                Timber.d("Swatch wasn't null, building dialog")
                MaterialDialog.Builder(this@PaletteDetailActivity)
                        .theme(if (swatch.getHsl()[2] > 0.5f) Theme.LIGHT else Theme.DARK)
                        .titleGravity(GravityEnum.CENTER)
                        .titleColor(swatch.getTitleTextColor())
                        .title(title)
                        .backgroundColor(swatch.getRgb())
                        .contentColor(swatch.getBodyTextColor())
                        .content(R.string.detail_lorem_full)
                        .positiveText(R.string.dialog_done)
                        .positiveColor(swatch.getBodyTextColor())
                        .neutralText(R.string.dialog_values)
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
            val items = getString(R.string.dialog_values_list, swatch.rgbHex(), swatch.titleHex(), swatch.bodyHex(), swatch.getHsl()[0], swatch.getHsl()[1], swatch.getHsl()[2], swatch.getPopulation()).split("\n")
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
                    .positiveText(R.string.dialog_copy_all)
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
