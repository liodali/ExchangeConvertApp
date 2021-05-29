package dali.hamza.core.datasource.network.converter

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonReader
import dali.hamza.core.datasource.network.models.CurrenciesDataAPI
import dali.hamza.core.datasource.network.models.CurrencyData

class CurrencyConverter {
    @FromJson
    fun fromJson(reader: JsonReader): CurrenciesDataAPI {
        val map: MutableMap<String, CurrencyData> = mutableMapOf()
        var isSuccess = false
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.peek()) {
                JsonReader.Token.NAME -> {
                    val fieldName = reader.nextName()
                    when (fieldName) {
                        "symbols" -> {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.peek()) {
                                    JsonReader.Token.NAME -> {
                                        val key = reader.nextName()
                                        reader.beginObject()
                                        while (reader.hasNext()) {
                                            when (reader.peek()) {
                                                JsonReader.Token.NAME -> {
                                                    val subName = reader.nextName()
                                                    if (subName == "description") {
                                                        val desc = reader.nextString()
                                                        map[key] = CurrencyData(
                                                            code = key,
                                                            description = desc
                                                        )
                                                    }
                                                }
                                                else -> reader.skipValue()
                                            }
                                        }
                                        reader.endObject()
                                    }
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                        }
                        "success" -> {
                            isSuccess = reader.nextBoolean()
                        }
                    }

                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return CurrenciesDataAPI(
            isSuccess,
            symbols = mapOf("symbols" to map)
        )
    }
}