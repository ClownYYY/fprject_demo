package com.deniscerri.ytdlnis.database

import androidx.room.TypeConverter
import com.deniscerri.ytdlnis.database.models.Format
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class Converters {
    @TypeConverter
    fun stringToListOfFormats(value: String?): ArrayList<Format> {
        val listType: Type = object : TypeToken<ArrayList<Format?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun listOfFormatsToString(list: ArrayList<Format?>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun formatToString(format: Format): String = Gson().toJson(format)

    @TypeConverter
    fun stringToFormat(string: String): Format = Gson().fromJson(string, Format::class.java)

}