package org.rivchain.paramotor_monitor.db

import org.json.JSONException
import org.json.JSONObject

class Settings {

    companion object {

        @Throws(JSONException::class)
        fun importJSON(obj: JSONObject): Settings {
            val s = Settings()
            return s
        }

        @Throws(JSONException::class)
        fun exportJSON(s: Settings): JSONObject {
            val obj = JSONObject()
            return obj
        }
    }
}