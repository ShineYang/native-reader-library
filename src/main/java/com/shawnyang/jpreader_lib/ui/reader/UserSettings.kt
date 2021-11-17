package com.shawnyang.jpreader_lib.ui.reader

/**
 * @author ShineYang
 * @date 2021/9/2
 * description:
 */
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.loper7.tab_expand.ext.toPx
import org.json.JSONArray
import org.readium.r2.navigator.R2BasicWebView
import org.readium.r2.navigator.R2WebView
import org.readium.r2.navigator.epub.fxl.R2FXLLayout
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.*
import java.io.File
import com.shawnyang.jpreader_lib.R
import org.readium.r2.navigator.extensions.color

class UserSettings(var preferences: SharedPreferences, val context: Context, private val UIPreset: MutableMap<ReadiumCSSName, Boolean>) {

    lateinit var resourcePager: R2ViewPager

    private val appearanceValues = listOf("readium-default-on", "readium-sepia-on", "readium-night-on")
    private val fontFamilyValues =
            listOf("Original", "PT Serif", "Roboto", "Source Sans Pro", "Vollkorn", "OpenDyslexic", "AccessibleDfA", "IA Writer Duospace")
    private val textAlignmentValues = listOf("justify", "start")
    private val columnCountValues = listOf("auto", "1", "2")

    private var fontSize = 125f
    private var fontOverride = false
    private var fontFamily = 0
    private var appearance = 0
    private var verticalScroll = false

    //Advanced settings
    private var publisherDefaults = false
    private var textAlignment = 0
    private var columnCount = 0
    private var wordSpacing = 0f
    private var letterSpacing = 0f
    private var pageMargins = 1.5f
    private var lineHeight = 1.25f

    private var userProperties: UserProperties

    init {
        appearance = preferences.getInt(APPEARANCE_REF, appearance)
        verticalScroll = preferences.getBoolean(SCROLL_REF, verticalScroll)
        fontFamily = preferences.getInt(FONT_FAMILY_REF, fontFamily)
        if (fontFamily != 0) {
            fontOverride = true
        }
        publisherDefaults = preferences.getBoolean(PUBLISHER_DEFAULT_REF, publisherDefaults)
        textAlignment = preferences.getInt(TEXT_ALIGNMENT_REF, textAlignment)
        columnCount = preferences.getInt(COLUMN_COUNT_REF, columnCount)


        fontSize = preferences.getFloat(FONT_SIZE_REF, fontSize)
        wordSpacing = preferences.getFloat(WORD_SPACING_REF, wordSpacing)
        letterSpacing = preferences.getFloat(LETTER_SPACING_REF, letterSpacing)
        pageMargins = preferences.getFloat(PAGE_MARGINS_REF, pageMargins)
        lineHeight = preferences.getFloat(LINE_HEIGHT_REF, lineHeight)
        userProperties = getUserSettings()

        //Setting up screen brightness
//        val backLightValue = preferences.getInt("reader_brightness", 50).toFloat() / 100
//        val layoutParams = (context as AppCompatActivity).window.attributes
//        layoutParams.screenBrightness = backLightValue
//        context.window.attributes = layoutParams
    }

    private fun getUserSettings(): UserProperties {

        val userProperties = UserProperties()
        // Publisher default system
        userProperties.addSwitchable("readium-advanced-off", "readium-advanced-on", publisherDefaults, PUBLISHER_DEFAULT_REF, PUBLISHER_DEFAULT_NAME)
        // Font override
        userProperties.addSwitchable("readium-font-on", "readium-font-off", fontOverride, FONT_OVERRIDE_REF, FONT_OVERRIDE_NAME)
        // Column count
        userProperties.addEnumerable(columnCount, columnCountValues, COLUMN_COUNT_REF, COLUMN_COUNT_NAME)
        // Appearance
        userProperties.addEnumerable(appearance, appearanceValues, APPEARANCE_REF, APPEARANCE_NAME)
        // Page margins
        userProperties.addIncremental(pageMargins, 0.5f, 4f, 0.25f, "", PAGE_MARGINS_REF, PAGE_MARGINS_NAME)
        // Text alignment
        userProperties.addEnumerable(textAlignment, textAlignmentValues, TEXT_ALIGNMENT_REF, TEXT_ALIGNMENT_NAME)
        // Font family
        userProperties.addEnumerable(fontFamily, fontFamilyValues, FONT_FAMILY_REF, FONT_FAMILY_NAME)
        // Font size
        userProperties.addIncremental(fontSize, 100f, 300f, 25f, "%", FONT_SIZE_REF, FONT_SIZE_NAME)
        // Line height
        userProperties.addIncremental(lineHeight, 1f, 2f, 0.25f, "", LINE_HEIGHT_REF, LINE_HEIGHT_NAME)
        // Word spacing
        userProperties.addIncremental(wordSpacing, 0f, 0.5f, 0.25f, "rem", WORD_SPACING_REF, WORD_SPACING_NAME)
        // Letter spacing
        userProperties.addIncremental(letterSpacing, 0f, 0.5f, 0.0625f, "em", LETTER_SPACING_REF, LETTER_SPACING_NAME)
        // Scroll
        userProperties.addSwitchable("readium-scroll-on", "readium-scroll-off", verticalScroll, SCROLL_REF, SCROLL_NAME)

        return userProperties
    }

    private fun makeJson(): JSONArray {
        val array = JSONArray()
        for (userProperty in userProperties.properties) {
            array.put(userProperty.getJson())
        }
        return array
    }


    fun saveChanges() {
        val json = makeJson()
        val dir = File(context.filesDir.path + "/" + Injectable.Style.rawValue + "/")
        dir.mkdirs()
        val file = File(dir, "UserProperties.json")
        file.printWriter().use { out ->
            out.println(json)
        }
    }

    private fun updateEnumerable(enumerable: Enumerable) {
        preferences.edit().putInt(enumerable.ref, enumerable.index).apply()
        saveChanges()
    }


    private fun updateSwitchable(switchable: Switchable) {
        preferences.edit().putBoolean(switchable.ref, switchable.on).apply()
        saveChanges()
    }

    private fun updateIncremental(incremental: Incremental) {
        preferences.edit().putFloat(incremental.ref, incremental.value).apply()
        saveChanges()
    }

    fun updateViewCSS(ref: String) {
        for (i in 0 until resourcePager.childCount) {
            val webView = resourcePager.getChildAt(i).findViewById(R.id.webView) as? R2WebView
            webView?.let {
                applyCSS(webView, ref)
            } ?: run {
                val zoomView = resourcePager.getChildAt(i).findViewById(R.id.r2FXLLayout) as R2FXLLayout
                val webView1 = zoomView.findViewById(R.id.firstWebView) as? R2BasicWebView
                val webView2 = zoomView.findViewById(R.id.secondWebView) as? R2BasicWebView
                val webViewSingle = zoomView.findViewById(R.id.webViewSingle) as? R2BasicWebView

                webView1?.let {
                    applyCSS(webView1, ref)
                }
                webView2?.let {
                    applyCSS(webView2, ref)
                }
                webViewSingle?.let {
                    applyCSS(webViewSingle, ref)
                }
            }
        }
    }

    private fun applyCSS(view: R2BasicWebView, ref: String) {
        val userSetting = userProperties.getByRef<UserProperty>(ref)
        view.setProperty(userSetting.name, userSetting.toString())
    }


    fun userSettingsPopUp(): PopupWindow {

        val layoutInflater = LayoutInflater.from(context)
        val layout = layoutInflater.inflate(R.layout.popup_window_user_settings, null)
        val userSettingsPopup = PopupWindow(context)
        userSettingsPopup.contentView = layout
        userSettingsPopup.setBackgroundDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.bg_pop_menu, null))
        userSettingsPopup.elevation = 5.toPx().toFloat()
        userSettingsPopup.width = ListPopupWindow.WRAP_CONTENT
        userSettingsPopup.height = ListPopupWindow.WRAP_CONTENT
        userSettingsPopup.isOutsideTouchable = true
        userSettingsPopup.isFocusable = true

        val fontSpinner: Spinner = layout.findViewById(R.id.spinner_action_settings_intervall_values) as Spinner
        val fontFamily = (userProperties.getByRef<Enumerable>(FONT_FAMILY_REF))
        val fontOverride = (userProperties.getByRef<Switchable>(FONT_OVERRIDE_REF))
        val appearance = userProperties.getByRef<Enumerable>(APPEARANCE_REF)
        val fontSize = userProperties.getByRef<Incremental>(FONT_SIZE_REF)
//        val fontSpinner: Spinner = layout.findViewById(R.id.spinner_action_settings_intervall_values) as Spinner
        val fonts = context.resources.getStringArray(R.array.font_list)
        val dataAdapter = object : ArrayAdapter<String>(context, R.layout.item_spinner_font, fonts) {

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v: View? = super.getDropDownView(position, null, parent)
                // Makes the selected font appear in dark
                // If this is the selected item position
                if (position == fontFamily.index) {
                    v!!.setBackgroundColor(context.color(R.color.colorPrimaryDark))
                    v.findViewById<TextView>(android.R.id.text1).setTextColor(Color.WHITE)

                } else {
                    // for other views
                    v!!.setBackgroundColor(Color.WHITE)
                    v.findViewById<TextView>(android.R.id.text1).setTextColor(Color.BLACK)

                }
                return v
            }
        }

        fun findIndexOfId(id: Int, list: MutableList<RadioButton>): Int {
            for (i in 0..list.size) {
                if (list[i].id == id) {
                    return i
                }
            }
            return 0
        }


        // Font family
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontSpinner.adapter = dataAdapter
        fontSpinner.setSelection(fontFamily.index)
        fontSpinner.contentDescription = "Font Family"
        fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                fontFamily.index = pos
                fontOverride.on = (pos != 0)
                updateSwitchable(fontOverride)
                updateEnumerable(fontFamily)
                updateViewCSS(FONT_OVERRIDE_REF)
                updateViewCSS(FONT_FAMILY_REF)
            }

            override fun onNothingSelected(parent: AdapterView<out Adapter>?) {
                // fontSpinner.setSelection(selectedFontIndex)
            }
        }


        // Appearance
        val appearanceGroup = layout.findViewById(R.id.appearance) as RadioGroup
        val appearanceRadios = mutableListOf<RadioButton>()
        appearanceRadios.add(layout.findViewById(R.id.appearance_default) as RadioButton)
        (layout.findViewById(R.id.appearance_default) as RadioButton).contentDescription = "Appearance Default"
        appearanceRadios.add(layout.findViewById(R.id.appearance_sepia) as RadioButton)
        (layout.findViewById(R.id.appearance_sepia) as RadioButton).contentDescription = "Appearance Sepia"
        appearanceRadios.add(layout.findViewById(R.id.appearance_night) as RadioButton)
        (layout.findViewById(R.id.appearance_night) as RadioButton).contentDescription = "Appearance Night"

        UIPreset[ReadiumCSSName.appearance]?.let {
            appearanceGroup.isEnabled = false
            for (appearanceRadio in appearanceRadios) {
                appearanceRadio.isEnabled = false
            }
        } ?: run {
            appearanceRadios[appearance.index].isChecked = true

            appearanceGroup.setOnCheckedChangeListener { _, id ->
                val i = findIndexOfId(id, list = appearanceRadios)
                appearance.index = i
                when (i) {
                    0 -> {
                        resourcePager.setBackgroundColor(Color.parseColor("#ffffff"))
                        //(resourcePager.focusedChild?.findViewById(R.id.book_title) as? TextView)?.setTextColor(Color.parseColor("#000000"))
                    }
                    1 -> {
                        resourcePager.setBackgroundColor(Color.parseColor("#faf4e8"))
                        //(resourcePager.focusedChild?.findViewById(R.id.book_title) as? TextView)?.setTextColor(Color.parseColor("#000000"))
                    }
                    2 -> {
                        resourcePager.setBackgroundColor(Color.parseColor("#000000"))
                        //(resourcePager.focusedChild?.findViewById(R.id.book_title) as? TextView)?.setTextColor(Color.parseColor("#ffffff"))
                    }
                }
                updateEnumerable(appearance)
                updateViewCSS(APPEARANCE_REF)
            }
        }


        // Font size
        val fontDecreaseButton = layout.findViewById(R.id.font_decrease) as ImageButton
        val fontIncreaseButton = layout.findViewById(R.id.font_increase) as ImageButton

        UIPreset[ReadiumCSSName.fontSize]?.let {
            fontDecreaseButton.isEnabled = false
            fontIncreaseButton.isEnabled = false
        } ?: run {
            fontDecreaseButton.setOnClickListener {
                fontSize.decrement()
                updateIncremental(fontSize)
                updateViewCSS(FONT_SIZE_REF)
            }

            fontIncreaseButton.setOnClickListener {
                fontSize.increment()
                updateIncremental(fontSize)
                updateViewCSS(FONT_SIZE_REF)
            }
        }

        return userSettingsPopup
    }
}