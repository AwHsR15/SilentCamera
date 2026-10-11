package com.marvis.silentcamera.ui

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object AppPreferences {
    fun language(context:Context)=context.getSharedPreferences("preferences",0).getString("language","system").orEmpty()
    fun theme(context:Context)=context.getSharedPreferences("preferences",0).getString("theme","system").orEmpty()
    fun save(context:Context,key:String,value:String){context.getSharedPreferences("preferences",0).edit().putString(key,value).apply()}
    fun localized(context:Context):Context {
        val language=language(context)
        if(language=="system")return context
        return context.createConfigurationContext(Configuration(context.resources.configuration).apply{setLocale(Locale.forLanguageTag(language))})
    }
}
