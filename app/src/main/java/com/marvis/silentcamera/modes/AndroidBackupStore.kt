package com.marvis.silentcamera.modes

import android.content.Context
import com.marvis.silentcamera.core.*
import org.json.JSONObject

class AndroidBackupStore(context:Context):BackupStore {
    // Application backups are disabled: these originals belong to this device/user only.
    private val prefs=context.getSharedPreferences("mode_originals",Context.MODE_PRIVATE)
    override fun get(key:String):OriginalState? {
        val raw=prefs.getString(key,null) ?: return null
        val json=JSONObject(raw)
        check(json.has("value")){"Invalid original backup: missing value"}
        return OriginalState(DeviceIdentity(json.getString("fingerprint"),json.getInt("user")),
            if(json.isNull("value"))null else json.getString("value"))
    }
    override fun save(key:String,state:OriginalState):Boolean = prefs.edit().putString(key,JSONObject().apply {
        put("fingerprint",state.identity.fingerprint);put("user",state.identity.userId)
        put("value",state.value ?: JSONObject.NULL)
    }.toString()).commit()
    override fun remove(key:String):Boolean=prefs.edit().remove(key).commit()
    fun keys():Set<String> = prefs.all.keys
}
