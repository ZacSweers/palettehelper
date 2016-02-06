package io.sweers.palettehelper.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AppCompatActivity
import android.support.v7.graphics.Palette
import android.support.v7.graphics.Palette.Swatch
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.Element
import android.support.v8.renderscript.RenderScript
import android.support.v8.renderscript.ScriptIntrinsicBlur
import android.util.Pair
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import butterknife.bindView
import com.afollestad.materialdialogs.GravityEnum
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.sweers.palettehelper.PaletteHelperApplication
import io.sweers.palettehelper.R
import io.sweers.palettehelper.ui.widget.ElasticDragDismissFrameLayout
import io.sweers.palettehelper.util.*
import io.sweers.rxpalette.asObservable
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.*

@Suppress("NOTHING_TO_INLINE")
class PaletteDetailActivity : AppCompatActivity() {

    val draggableFrame: ElasticDragDismissFrameLayout by bindView(R.id.draggable_frame)
    val backButton: ImageView by bindView(R.id.back)
    val imageViewContainer: FrameLayout by bindView(R.id.image_view_container)
    val imageView: ImageView by bindView(R.id.image_view)
    val imageViewBackground: ImageView by bindView(R.id.image_view_background)
    val recyclerView: RecyclerView by bindView(R.id.grid_view)

    companion object {
        val KEY_URI = "uri_path"
        val KEY_CAMERA = "camera"
        val DEFAULT_NUM_COLORS = 16
    }

    var active = true;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_palette_detail)

        Timber.d("Starting up DetailActivity")
        PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_ENTER, ANALYTICS_NAV_DETAIL)

        backButton.setOnClickListener { finish() }
        backButton.setBackgroundDrawable(createColorSelector(Color.WHITE))

        draggableFrame.addListener(object: ElasticDragDismissFrameLayout.SystemChromeFader(window) {
            override fun onDragDismissed() {
                this@PaletteDetailActivity.finish()
            }
        })

        Timber.d("Reading intent.")
        val intent = intent
        val action = intent.action
        val type: String? = intent.type
        var imageUri: String? = null
        when {
            Intent.ACTION_SEND == action && (type?.startsWith("image/") ?: false) -> {
                Timber.d("Received via app share, trying to resolve uri")
                PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_SHARE, ANALYTICS_NAV_DETAIL)
                imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM).toString()
            }
            intent.hasExtra(KEY_URI) -> {
                Timber.d("Uri specified, trying to decode file")
                PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_INTERNAL, ANALYTICS_NAV_DETAIL)
                imageUri = intent.getStringExtra(KEY_URI)
            }
            intent.hasExtra(KEY_CAMERA) -> {
                Timber.d("Path specified, trying to decode file")
                PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_CAMERA, ANALYTICS_NAV_DETAIL)
                imageUri = "file://${intent.getStringExtra(KEY_CAMERA)}"
            }
            Intent.ACTION_SEND == action -> {
                Timber.d("Received URL, trying to download")
                PaletteHelperApplication.mixPanel.trackNav(ANALYTICS_NAV_URL, ANALYTICS_NAV_DETAIL)
                imageUri = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
            else -> {
                errorOut()
            }
        }

        if (imageUri != null) {
            val dialog = MaterialDialog.Builder(this)
                    .content(R.string.detail_loading_image)
                    .theme(Theme.LIGHT)
                    .progress(true, 0)
                    .cancelListener { finish() }
                    .build()

            val runnable: Runnable = Runnable { if (active) dialog.show() }
            val handler = Handler(Looper.getMainLooper());
            handler.postDelayed(runnable, 500)  // Wait half a second before showing the dialog to avoid flashing effect if it loads fast

            Glide.with(this)
                    .load(imageUri)
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            handler.removeCallbacks(runnable)
                            val loadedImage = getGlideBitmap(resource)
                            if (loadedImage == null) {
                                errorOut()
                                return false
                            }

                            imageViewContainer.setOnClickListener {
                                val photoIntent = Intent(this@PaletteDetailActivity, PhotoActivity::class.java)
                                photoIntent.putExtra(PhotoActivity.EXTRA_URI, imageUri)
                                ActivityCompat.startActivity(this@PaletteDetailActivity, photoIntent, ActivityOptionsCompat.makeSceneTransitionAnimation(this@PaletteDetailActivity).toBundle())
                            }

                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                            if (PreferenceManager.getDefaultSharedPreferences(this@PaletteDetailActivity).getBoolean("pref_key_default", true)) {
                                display(loadedImage, DEFAULT_NUM_COLORS)
                            } else {
                                Timber.d("Prompting for number of colors first")
                                promptForNumColors(loadedImage)
                            }
                            return false
                        }

                        override fun onException(e: Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            Timber.e("Invalid imageUri: %s", e?.message)
                            errorOut()
                            return false
                        }

                    })
                    .fitCenter()
                    .crossFade()
                    .into(imageView)
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

    /**
     * Errors end up here, where we log it and let the user know before exiting the activity.
     */
    private fun errorOut() {
        Timber.e("Given an intent, but we can't do anything with the provided info.")
        Toast.makeText(this, getString(R.string.detail_invalid_input), Toast.LENGTH_SHORT).show()
        finish()
    }

    /**
     * If the user disables the default color count, the detail activity will route to this to
     * prompt the user to input the number of colors.
     *
     * @param bitmap the image bitmap to eventually feed into Palette.
     */
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
                .theme(Theme.LIGHT)
                .onPositive { dialog, dialogAction ->
                    var isValid: Boolean
                    val inputText: String = input.text.toString()
                    var number = DEFAULT_NUM_COLORS
                    try {
                        number = java.lang.Integer.parseInt(inputText)
                        if (number < 1) {
                            input.error = getString(R.string.detail_must_be_greater_than_one)
                            isValid = false
                        } else {
                            isValid = true
                        }
                    } catch (e: Exception) {
                        input.error = getString(R.string.detail_invalid_input)
                        isValid = false
                    }
                    if (isValid) {
                        display(bitmap, number)
                        dialog.dismiss()
                        PaletteHelperApplication.mixPanel.trackMisc(ANALYTICS_KEY_NUMCOLORS, number.toString())
                    }
                }
                .onNegative { dialog, dialogAction ->
                    dialog.dismiss()
                    finish()
                }
                .onNeutral { dialog, dialogAction ->
                    dialog.dismiss()
                    display(bitmap, DEFAULT_NUM_COLORS)
                }
                .cancelListener({dialog ->
                    dialog.dismiss()
                    finish()
                })
                .show()
    }

    /**
     *
     */
    private fun display(bitmap: Bitmap, numColors: Int = 16) {
        Observable
                .zip(generatePalette(bitmap, numColors), blur(bitmap),
                        { palette, blurredBitmap -> Pair(palette, blurredBitmap) })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { displayData ->
                    // Set blurred bitmap
                    imageViewBackground.setImageBitmap(displayData.second)

                    // Set up palette data
                    val palette = displayData.first
                    Timber.d("Palette generation done with ${palette.swatches.size} colors extracted of $numColors requested")
                    val swatches = ArrayList(palette.primarySwatches())
                    swatches.addAll(palette.uniqueSwatches())

                    val isDark: Boolean
                    val lightness = isDark(palette)
                    if (lightness == Lightness.UNKNOWN) {
                        isDark = isDark(bitmap, bitmap.width / 2, 0)
                    } else {
                        isDark = lightness == Lightness.DARK
                    }

                    if (!isDark) {
                        // make back icon dark on light images
                        backButton.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // color the status bar. Set a complementary dark color on L,
                        // light or dark color on M (with matching status bar icons)
                        var statusBarColor = window.statusBarColor
                        val topColor = getMostPopulousSwatch(palette)
                        if (topColor != null && (isDark || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
                            statusBarColor = scrimify(topColor.rgb, isDark, SCRIM_ADJUSTMENT)
                            // set a light status bar on M+
                            if (!isDark) {
                                setLightStatusBar(imageViewContainer)
                            }
                        }

                        if (statusBarColor != window.statusBarColor) {
                            ValueAnimator.ofArgb(window.statusBarColor, statusBarColor).apply {
                                addUpdateListener { animation -> window.statusBarColor = animation.animatedValue as Int }
                                duration = 1000
                                interpolator = FastOutSlowInInterpolator()
                                start()
                            }
                        }
                    }

                    Timber.d("Setting up adapter with swatches")
                    recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                    val adapter = ResultsAdapter(swatches)
                    recyclerView.adapter = adapter
                }
    }

    /**
     * Blurring function that returns an observable of blurring a bitmap
     */
    private fun blur(srcBitmap: Bitmap): Observable<Bitmap> {
        if (srcBitmap.width < 1 || srcBitmap.height < 1) {
            // Bitmap exists but has no actual size, nothing to blur.
            return Observable.empty();
        }

        var bitmap: Bitmap? = srcBitmap;
        // simulate a larger blur radius by downscaling the input image, as high radii are computationally very heavy
        bitmap = downscaleBitmap(bitmap, srcBitmap.width, srcBitmap.height, 250);
        bitmap ?: return Observable.empty()
        return Observable.create<Bitmap> { subscriber ->
            val rs: RenderScript = RenderScript.create(this)
            val input: Allocation = Allocation.createFromBitmap(
                    rs,
                    bitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT
            )

            val output: Allocation = Allocation.createTyped(rs, input.type)
            ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)).apply {
                setRadius(25f)
                setInput(input)
                forEach(output)
            }
            output.copyTo(bitmap)
            subscriber.onNext(bitmap)
        }
    }

    /**
     * Consolidated downscale function for Bitmaps. Tiny bitmaps sometimes downscale to the point
     * where they have a width or height less than 0, causing the whole process to barf. To avoid
     * the mess, we wrap it in a try/catch.
     *
     * @param bitmap Source bitmap
     * @param srcWidth Source width
     * @param srcHeight Source height
     * @param radius Initially requested blur radius
     * @return Downscaled bitmap if it worked, null if cleanup on aisle 5
     */
    private fun downscaleBitmap(bitmap: Bitmap?, srcWidth: Int, srcHeight: Int, radius: Int): Bitmap? {
        try {
            val destWidth: Int = (25f / radius * srcWidth).toInt()
            val destHeight: Int = (25f / radius * srcHeight).toInt()
            if (destWidth < 1 || destHeight < 1) {
                // Uh oh
                return null;
            }
            return Bitmap.createScaledBitmap(
                    bitmap,
                    (25f / radius * srcWidth).toInt(),
                    (25f / radius * srcHeight).toInt(),
                    true
            );
        } catch (e: RuntimeException) {
            // (╯°□°）╯︵ ┻━┻
            return null;
        }
    }

    /**
     * This is where the actual palette generation happens. Once the library calls back with the generated
     * palette, the gridview's list adapter is updated with the standard colors prefixed to a list
     * of *all* the colors.
     *
     * @param bitmap the image bitmap to feed into Palette
     * @param numColors the number of colors to generate, defaulting to 16
     */
    private fun generatePalette(bitmap: Bitmap, numColors: Int = 16): Observable<Palette> {
        return Palette.Builder(bitmap)
                .clearFilters()
                .maximumColorCount(numColors)
                .asObservable()
                .doOnSubscribe { Timber.d("Generating palette") }
    }

    override fun onDestroy() {
        PaletteHelperApplication.mixPanel.flush()
        super.onDestroy()
    }

    private inner class ResultsAdapter(private val swatches: List<Swatch?>) : RecyclerView.Adapter<ViewHolder>() {

        val VIEW_TYPE_HEADER = 0
        val VIEW_TYPE_SWATCH = 1

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position == 0 || position == 7) {
                // Header
                val layoutParams: StaggeredGridLayoutManager.LayoutParams = holder.itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams;
                layoutParams.isFullSpan = true;
                val text: String
                if (position == 0) {
                    text = getString(R.string.detail_primary_swatches)
                } else {
                    text = getString(R.string.detail_all_swatches)
                }
                (holder.itemView as TextView).text = text
            } else {
                if (position < swatches.size) {
                    val swatch = getItem(getAdjustedSwatchPosition(position))
                    if (swatch == null) {
                        holder.text?.text = getString(R.string.detail_no_swatch, swatchNames[getAdjustedSwatchPosition(position)])
                        holder.text?.setTextColor(Color.parseColor("#ADADAD"))
                        holder.itemView.setBackgroundColor(Color.parseColor("#252626"))
                        holder.itemView.isEnabled = false
                        holder.itemView.setOnClickListener(null)
                    } else {
                        var backgroundColor = swatch.rgb
                        if (backgroundColor == Color.TRANSPARENT) {
                            // Can't have transparent backgrounds apparently? I get crash reports for this
                            backgroundColor = Color.parseColor("#252626")
                        }
                        holder.itemView.setBackgroundColor(backgroundColor)
                        holder.text?.setTextColor(swatch.titleTextColor)
                        val hex = swatch.rgb.hex()
                        val adjustedPosition = getAdjustedSwatchPosition(position)
                        if (adjustedPosition < 6) {
                            holder.text?.text = "${swatchNames[adjustedPosition]}\n$hex"
                        } else {
                            holder.text?.text = hex
                        }
                        holder.itemView.isEnabled = true
                        holder.itemView.setOnClickListener { v -> onItemClick(adjustedPosition, swatch) }
                    }
                    holder.itemView.visibility = View.VISIBLE
                } else {
                    holder.itemView.visibility = View.GONE
                }
            }
        }

        override fun getItemCount(): Int {
            return swatches.size
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0 || position == 7) {
                return VIEW_TYPE_HEADER
            } else {
                return VIEW_TYPE_SWATCH
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder? {
            var holder: ViewHolder
            if (viewType == VIEW_TYPE_HEADER) {
                val textView = layoutInflater.inflate(R.layout.header_row, parent, false) as TextView
                textView.gravity = Gravity.START
                holder = ViewHolder(textView)
            } else {
                val view = layoutInflater.inflate(R.layout.swatch_cell, parent, false)
                holder = ViewHolder(view)
                holder.text = view.findViewById(R.id.hex) as TextView
            }
            return holder
        }

        private val swatchNames = resources.getStringArray(R.array.swatches)

        private fun getAdjustedSwatchPosition(position: Int) : Int {
            when {
                position > 0 && position < 7 -> return position - 1
                else -> return position - 2
            }
        }

        fun getItem(position: Int): Swatch? {
            return swatches[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        fun onItemClick(position: Int, swatch: Swatch) {
            Timber.d("Swatch item clicked")
            val title = if (position < 6)  swatchNames[position] else getString(R.string.detail_lorem)
            MaterialDialog.Builder(this@PaletteDetailActivity)
                    .theme(if (swatch.isLightColor()) Theme.LIGHT else Theme.DARK)
                    .titleGravity(GravityEnum.CENTER)
                    .titleColor(swatch.titleTextColor)
                    .title(title)
                    .backgroundColor(swatch.rgb)
                    .contentColor(swatch.bodyTextColor)
                    .content(R.string.detail_lorem_full)
                    .positiveText(R.string.dialog_done)
                    .positiveColor(swatch.bodyTextColor)
                    .neutralText(R.string.dialog_values)
                    .neutralColor(swatch.bodyTextColor)
                    .onPositive { dialog, dialogAction -> dialog.dismiss() }
                    .onNeutral { dialog, dialogAction ->
                        dialog.dismiss()
                        showValues(swatch)
                    }
                    .show()
        }

        private fun showValues(swatch: Swatch) {
            Timber.d("Showing values")
            val items = getString(
                    R.string.dialog_values_list,
                    swatch.rgb.hex(),
                    swatch.titleTextColor.hex(),
                    swatch.bodyTextColor.hex(),
                    swatch.hsl[0],
                    swatch.hsl[1],
                    swatch.hsl[2],
                    swatch.population)
                    .split("\n")
            MaterialDialog.Builder(this@PaletteDetailActivity)
                    .theme(if (swatch.isLightColor()) Theme.LIGHT else Theme.DARK)
                    .backgroundColor(swatch.rgb)
                    .contentColor(swatch.bodyTextColor)
                    .items(*items.toTypedArray())
                    .itemsCallback({ dialog, view, position, value ->
                        when (position) {
                            0 -> copyAndNotify(this@PaletteDetailActivity, swatch.rgb.hex())
                            1 -> copyAndNotify(this@PaletteDetailActivity, swatch.titleTextColor.hex())
                            2 -> copyAndNotify(this@PaletteDetailActivity, swatch.bodyTextColor.hex())
                            3 -> copyAndNotify(this@PaletteDetailActivity, swatch.hsl[0].toString())
                            4 -> copyAndNotify(this@PaletteDetailActivity, swatch.hsl[1].toString())
                            5 -> copyAndNotify(this@PaletteDetailActivity, swatch.hsl[2].toString())
                            6 -> copyAndNotify(this@PaletteDetailActivity, swatch.population.toString())
                        }
                        dialog?.dismiss()
                    })
                    .forceStacking(true)
                    .positiveText(R.string.dialog_copy_all)
                    .positiveColor(swatch.bodyTextColor)
                    .onPositive { dialog, dialogAction ->
                        copyAndNotify(this@PaletteDetailActivity, swatch.toString())
                        dialog.dismiss()
                    }
                    .show()
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var text: TextView? = null
    }
}
