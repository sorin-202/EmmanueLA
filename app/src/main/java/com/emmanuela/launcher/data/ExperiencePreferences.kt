package com.emmanuela.launcher.data

import org.json.JSONObject

data class ExperiencePreferences(
    val homeEnabled:Boolean=true,
    val homeStyleOverride:Boolean=false,
    val homeFont:String="Inherit",
    val homeTextScale:Float=1f,
    val homeItalic:Boolean=false,
    val homeTextWeight:String="Inherit",
    val widgetBackgroundOpacity:Float=.25f,
    val widgetCornerRadius:Float=18f,
    val homeWaveStrength:Float=48f,
    val appWaveStrength:Float=48f,
    val drawerBeforeHomeAlphabet:Boolean=true,
    val contactsSearch:Boolean=true,
    val contactPackages:Set<String> = emptySet(),
    val wellbeingComponent:String="",
    val weatherWebsite:String="",
    val advanced: Boolean = false,
    val weatherLocationName:String="",
    val language: String = "system",
    val homeSpacing: Int = 10,
    val appSpacing: Int = 14,
    val appAlignment: String = "Left",
    val appIcons: Boolean = false,
    val appTextScale: Float = 1f,
    val searchAliases: Boolean = true,
    val searchPackages: Boolean = true,
    val autoLaunch: Boolean = true,
    val autoLaunchDelay: Int = 0,
    val cursorStyle: String = "Normal",
    val cursorCharacter: String = "_",
    val alphabetHaptics: Boolean = true,
    val homeAlphabet: Boolean = false,
    val homeAlphabetStyle: String = "Minimal",
    val homeAlphabetAnimation: String = "Wave",
    val homeAlphabetHaptics: Boolean = true,
    val homeAlphabetPosition: String = "Right",
    val folderSpacing: Int = 16,
    val folderSort: String = "Name",
    val folderClose: Boolean = true,
    val folderLabelScale: Float = 1f,
    val folderAnimation: String = "Standard",
    val wallpaperFit: String = "Fill",
    val wallpaperOrder: String = "Sequential",
    val motion: String = "Standard",
    val motionSpeed: String = "Fast",
    val reduceMotion: Boolean = false,
    val textOpacity: Float = 1f,
    val textWeight: String = "Regular",
    val accentColor: Long = 0L,
    val textColor: Long = 0L,
    val edgeToEdge: Boolean = true,
    val gestureSensitivity: String = "Medium",
    val gestureHaptics: Boolean = true,
    val bottomSize: String = "Medium",
    val bottomPosition: String = "Bottom",
    val digestEnabled: Boolean = false,
    val digestInterval: Int = 30,
    val hideFromSearch: Boolean = true,
    val hidePrivateSearch: Boolean = false,
    val autoLock: String = "Immediately",
    val authentication: String = "Biometric + device credential",
    val warningPercent: Int = 80,
    val gentleReminder: Boolean = false,
    val reminderMinutes: Int = 30,
    val widgetScales: Map<String,Float> = emptyMap(),
    val widgetAlignments: Map<String,String> = emptyMap()
)
object ExperienceCodec {
    fun encode(p:ExperiencePreferences)=JSONObject().apply {
        put("homeStyleOverride",p.homeStyleOverride);put("homeEnabled",p.homeEnabled);put("homeFont",p.homeFont);put("homeTextScale",p.homeTextScale.toDouble());put("homeItalic",p.homeItalic);put("homeTextWeight",p.homeTextWeight)
        put("widgetBackgroundOpacity",p.widgetBackgroundOpacity.toDouble());put("widgetCornerRadius",p.widgetCornerRadius.toDouble());put("homeWaveStrength",p.homeWaveStrength.toDouble());put("drawerBeforeHomeAlphabet",p.drawerBeforeHomeAlphabet)
        put("appWaveStrength",p.appWaveStrength.toDouble())
        put("contactsSearch",p.contactsSearch);put("contactPackages",org.json.JSONArray(p.contactPackages.toList()));put("wellbeingComponent",p.wellbeingComponent);put("weatherWebsite",p.weatherWebsite)
        put("advanced",p.advanced);put("weatherLocationName",p.weatherLocationName)
        put("language",p.language)
        put("homeSpacing",p.homeSpacing)
        put("appSpacing",p.appSpacing)
        put("appAlignment",p.appAlignment)
        put("appIcons",p.appIcons)
        put("appTextScale",p.appTextScale.toDouble())
        put("searchAliases",p.searchAliases)
        put("searchPackages",p.searchPackages)
        put("autoLaunch",p.autoLaunch)
        put("autoLaunchDelay",p.autoLaunchDelay)
        put("cursorStyle",p.cursorStyle)
        put("cursorCharacter",p.cursorCharacter)
        put("alphabetHaptics",p.alphabetHaptics)
        put("homeAlphabet",p.homeAlphabet)
        put("homeAlphabetPosition",p.homeAlphabetPosition)
        put("homeAlphabetStyle",p.homeAlphabetStyle);put("homeAlphabetAnimation",p.homeAlphabetAnimation);put("homeAlphabetHaptics",p.homeAlphabetHaptics)
        put("folderSpacing",p.folderSpacing)
        put("folderSort",p.folderSort)
        put("folderClose",p.folderClose)
        put("folderLabelScale",p.folderLabelScale.toDouble())
        put("folderAnimation",p.folderAnimation)
        put("wallpaperFit",p.wallpaperFit)
        put("wallpaperOrder",p.wallpaperOrder)
        put("motion",p.motion)
        put("motionSpeed",p.motionSpeed)
        put("reduceMotion",p.reduceMotion)
        put("textOpacity",p.textOpacity.toDouble())
        put("textWeight",p.textWeight)
        put("accentColor",p.accentColor)
        put("textColor",p.textColor)
        put("edgeToEdge",p.edgeToEdge)
        put("gestureSensitivity",p.gestureSensitivity)
        put("gestureHaptics",p.gestureHaptics)
        put("bottomSize",p.bottomSize)
        put("bottomPosition",p.bottomPosition)
        put("digestEnabled",p.digestEnabled)
        put("digestInterval",p.digestInterval)
        put("hideFromSearch",p.hideFromSearch)
        put("hidePrivateSearch",p.hidePrivateSearch)
        put("autoLock",p.autoLock)
        put("authentication",p.authentication)
        put("warningPercent",p.warningPercent)
        put("gentleReminder",p.gentleReminder)
        put("reminderMinutes",p.reminderMinutes)
        put("widgetScales",JSONObject(p.widgetScales));put("widgetAlignments",JSONObject(p.widgetAlignments))
    }
    fun decode(o:JSONObject):ExperiencePreferences {
        val d=ExperiencePreferences()
        fun choice(key:String,default:String,allowed:List<String>)=o.optString(key,default).also{require(it in allowed)}
        val scales=o.optJSONObject("widgetScales")?:JSONObject();val align=o.optJSONObject("widgetAlignments")?:JSONObject()
        require(scales.length()<=6 && align.length()<=6)
        val contactPackages=o.optJSONArray("contactPackages")?:org.json.JSONArray()
        require(contactPackages.length()<=100)
        return ExperiencePreferences(
            homeStyleOverride=o.optBoolean("homeStyleOverride"),
            homeEnabled=o.optBoolean("homeEnabled",true),
            homeFont=choice("homeFont","Inherit",listOf("Inherit","Sans","Serif","Monospace","Custom")),
            homeTextScale=o.optDouble("homeTextScale",1.0).toFloat().also{require(it in .7f..1.5f)},
            homeItalic=o.optBoolean("homeItalic"),
            homeTextWeight=choice("homeTextWeight","Inherit",listOf("Inherit","Regular","Medium","Bold")),
            widgetBackgroundOpacity=o.optDouble("widgetBackgroundOpacity",.25).toFloat().also{require(it in 0f..1f)},
            widgetCornerRadius=o.optDouble("widgetCornerRadius",18.0).toFloat().also{require(it in 0f..40f)},
            appWaveStrength=o.optDouble("appWaveStrength",48.0).toFloat().also{require(it in 16f..80f)},
            homeWaveStrength=o.optDouble("homeWaveStrength",48.0).toFloat().also{require(it in 16f..80f)},
            drawerBeforeHomeAlphabet=o.optBoolean("drawerBeforeHomeAlphabet",true),
            contactsSearch=o.optBoolean("contactsSearch",true),
            contactPackages=List(contactPackages.length()){contactPackages.getString(it).also{pkg->require(pkg.length in 1..255)}}.toSet(),
            wellbeingComponent=o.optString("wellbeingComponent").also{require(it.length<=512 && (it.isEmpty()||it.contains('/')))},
            weatherWebsite=o.optString("weatherWebsite").also{require(it.length<=2048 && (it.isEmpty()||it.startsWith("https://")||it.startsWith("http://")))},
            advanced=o.optBoolean("advanced",d.advanced),
            weatherLocationName=o.optString("weatherLocationName").also{require(it.length<=160)},
            language=choice("language",d.language,listOf("system","en","ro","de","fr","es","it","pl")),
            homeSpacing=o.optInt("homeSpacing",d.homeSpacing).also{require(it in 0..32)},
            appSpacing=o.optInt("appSpacing",d.appSpacing).also{require(it in 0..32)},
            appAlignment=choice("appAlignment",d.appAlignment,listOf("Left","Center","Right")),
            appIcons=o.optBoolean("appIcons",d.appIcons),
            appTextScale=o.optDouble("appTextScale",d.appTextScale.toDouble()).toFloat().also{require(it in .7f..1.5f)},
            searchAliases=o.optBoolean("searchAliases",d.searchAliases),
            searchPackages=o.optBoolean("searchPackages",d.searchPackages),
            autoLaunch=o.optBoolean("autoLaunch",d.autoLaunch),
            autoLaunchDelay=o.optInt("autoLaunchDelay",d.autoLaunchDelay).also{require(it in 0..1500)},
            cursorStyle=choice("cursorStyle",d.cursorStyle,listOf("Normal","Block","Underline","Thin","Thick","Custom")),
            cursorCharacter=o.optString("cursorCharacter",d.cursorCharacter).also{require(it.length<=2)},
            alphabetHaptics=o.optBoolean("alphabetHaptics",d.alphabetHaptics),
            homeAlphabet=o.optBoolean("homeAlphabet",d.homeAlphabet),
            homeAlphabetStyle=choice("homeAlphabetStyle",d.homeAlphabetStyle,listOf("Minimal","Compact")),
            homeAlphabetAnimation=choice("homeAlphabetAnimation",d.homeAlphabetAnimation,listOf("None","Wave","Bubble","Fade")),
            homeAlphabetHaptics=o.optBoolean("homeAlphabetHaptics",d.homeAlphabetHaptics),
            homeAlphabetPosition=choice("homeAlphabetPosition",d.homeAlphabetPosition,listOf("Left","Right")),
            folderSpacing=o.optInt("folderSpacing",d.folderSpacing).also{require(it in 0..32)},
            folderSort=choice("folderSort",d.folderSort,listOf("Name","Manual")),
            folderClose=o.optBoolean("folderClose",d.folderClose),
            folderLabelScale=o.optDouble("folderLabelScale",d.folderLabelScale.toDouble()).toFloat().also{require(it in .7f..1.5f)},
            folderAnimation=choice("folderAnimation",d.folderAnimation,listOf("Off","Standard")),
            wallpaperFit=choice("wallpaperFit",d.wallpaperFit,listOf("Fit","Fill")),
            wallpaperOrder=choice("wallpaperOrder",d.wallpaperOrder,listOf("Random","Sequential")),
            motion=choice("motion",d.motion,listOf("Off","Standard","Fluid")),
            motionSpeed=choice("motionSpeed",d.motionSpeed,listOf("Fast","Normal","Slow")),
            reduceMotion=o.optBoolean("reduceMotion",d.reduceMotion),
            textOpacity=o.optDouble("textOpacity",d.textOpacity.toDouble()).toFloat().also{require(it in .4f..1f)},
            textWeight=choice("textWeight",d.textWeight,listOf("Regular","Medium","Bold")),
            accentColor=o.optLong("accentColor",d.accentColor),
            textColor=o.optLong("textColor",d.textColor),
            edgeToEdge=o.optBoolean("edgeToEdge",d.edgeToEdge),
            gestureSensitivity=choice("gestureSensitivity",d.gestureSensitivity,listOf("Low","Medium","High")),
            gestureHaptics=o.optBoolean("gestureHaptics",d.gestureHaptics),
            bottomSize=choice("bottomSize",d.bottomSize,listOf("Small","Medium","Large","Custom")),
            bottomPosition=choice("bottomPosition",d.bottomPosition,listOf("Bottom")),
            digestEnabled=o.optBoolean("digestEnabled",d.digestEnabled),
            digestInterval=o.optInt("digestInterval",d.digestInterval).also{require(it in 15..120)},
            hideFromSearch=o.optBoolean("hideFromSearch",d.hideFromSearch),
            hidePrivateSearch=o.optBoolean("hidePrivateSearch",d.hidePrivateSearch),
            autoLock=choice("autoLock",d.autoLock,listOf("Immediately","30 seconds","Screen off")),
            authentication=choice("authentication",d.authentication,listOf("Biometric + device credential","Device credential")),
            warningPercent=o.optInt("warningPercent",d.warningPercent).also{require(it in 50..100)},
            gentleReminder=o.optBoolean("gentleReminder",d.gentleReminder),
            reminderMinutes=o.optInt("reminderMinutes",d.reminderMinutes).also{require(it in 5..120)},
            widgetScales=scales.keys().asSequence().associateWith{require(it in listOf("clock","date","battery","usage","weather","location"));scales.getDouble(it).toFloat().also{n->require(n in .5f..2f)}},
            widgetAlignments=align.keys().asSequence().associateWith{require(it in listOf("clock","date","battery","usage","weather","location"));align.getString(it).also{v->require(v in listOf("Left","Center","Right"))}}
        )
    }
}
