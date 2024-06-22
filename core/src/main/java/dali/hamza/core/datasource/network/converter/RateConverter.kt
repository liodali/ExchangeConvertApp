package dali.hamza.core.datasource.network.converter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import dali.hamza.core.datasource.network.models.RatesCurrenciesDataAPI

class RateConverter {
    @FromJson
    fun fromJson(reader: JsonReader): RatesCurrenciesDataAPI {
        val map: MutableMap<String, Double> = mutableMapOf()
        var source = ""
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.peek()) {
                JsonReader.Token.NAME -> {
                    val fieldName = reader.nextName()
                    if (fieldName == "rates") {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            map[reader.nextName()] = reader.nextDouble()
                        }
                        reader.endObject()
                    } else if (fieldName == "base") {
                        source = reader.nextString()
                    }

                }

                else -> reader.skipValue()
            }
        }

        reader.endObject()
        return RatesCurrenciesDataAPI(
            source = source,
            quotes = map
        )
    }
}