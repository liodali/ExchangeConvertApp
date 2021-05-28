package dali.hamza.core.datasource.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import dali.hamza.core.datasource.network.models.CurrenciesDataAPI
import dali.hamza.core.datasource.network.models.CurrencyData

class CurrencyConverter {


    @FromJson
    fun fromJson(reader: JsonReader): CurrenciesDataAPI {
        val map: MutableMap<String, String> = mutableMapOf()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.peek()) {
                JsonReader.Token.NAME -> {
                    val fieldName = reader.nextName()
                    if (fieldName == "currencies") {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            map[reader.nextName()] = reader.nextString()
                        }
                        reader.endObject()
                    }

                }
                else -> reader.skipValue()
            }
        }

        reader.endObject()
        return CurrenciesDataAPI(
            currencies = mapOf("currencies" to CurrencyData(map))
        )
    }
}