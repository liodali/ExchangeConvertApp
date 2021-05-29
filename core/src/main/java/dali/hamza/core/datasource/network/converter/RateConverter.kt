package dali.hamza.core.datasource.network.converter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import dali.hamza.core.datasource.network.models.CurrenciesDataAPI
import dali.hamza.core.datasource.network.models.CurrencyData
import dali.hamza.core.datasource.network.models.RateData
import dali.hamza.core.datasource.network.models.RatesCurrenciesDataAPI

class RateConverter {
    @FromJson
    fun fromJson(reader: JsonReader): RatesCurrenciesDataAPI {
        val map: MutableMap<String, Double> = mutableMapOf()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.peek()) {
                JsonReader.Token.NAME -> {
                    val fieldName = reader.nextName()
                    if (fieldName == "quotes") {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            map[reader.nextName()] = reader.nextDouble()
                        }
                        reader.endObject()
                    }

                }
                else -> reader.skipValue()
            }
        }

        reader.endObject()
        return RatesCurrenciesDataAPI(
            quotes = mapOf("quotes" to RateData(map))
        )
    }
}