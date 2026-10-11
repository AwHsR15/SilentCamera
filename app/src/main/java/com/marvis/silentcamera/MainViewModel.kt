package com.marvis.silentcamera

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marvis.silentcamera.core.*
import com.marvis.silentcamera.modes.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application:Application):AndroidViewModel(application) {
    private val repository=ModeRepository(application)
    var report by mutableStateOf<DeviceReport?>(null);private set
    var busy by mutableStateOf(false);private set
    var selectedMode by mutableStateOf(SilenceMode.CAMERA);private set
    var selectedPackage by mutableStateOf<String?>(null);private set
    var result by mutableStateOf<OperationResult?>(null);private set
    var verification by mutableStateOf("untested");private set
    val logs=mutableStateListOf<String>()
    private var initialized=false

    fun refresh()=work { null to repository.detect() }
    fun selectMode(mode:SilenceMode) {
        if(busy)return
        selectedMode=mode;result=null;verification="untested"
    }
    fun selectPackage(pkg:String) {if(!busy){selectedPackage=pkg;result=null;verification="untested"}}
    fun applyMode()=operate(false)
    fun restoreMode()=operate(true)
    private fun operate(restore:Boolean) {
        val mode=selectedMode;val pkg=selectedPackage
        verification="untested"
        work {
            val outcome=if(restore)repository.restore(mode,pkg) else repository.apply(mode,pkg)
            val commands=repository.recentCommands()
            val detected=repository.detect()
            outcome to detected.copy(diagnostics=commands+detected.diagnostics)
        }
    }
    fun checkRoot()=work {
        val granted=repository.requestRoot()
        val detected=repository.detect()
        (if(granted)null else OperationResult(Outcome.FAILED,getApplication<Application>().getString(R.string.root_denied))) to detected
    }
    fun confirmPhoto(quiet:Boolean) {verification=if(quiet)"quiet" else "audible"}
    fun hasBackup():Boolean=report?.backupKeys?.contains(repository.backupKey(selectedMode,selectedPackage))==true
    fun clearLogs(){if(!busy)logs.clear()}
    fun diagnosticText():String {
        val device=report
        return listOf("SilentCamera ${BuildConfig.VERSION_NAME}",
            "${device?.manufacturer.orEmpty()} ${device?.model.orEmpty()} Android ${device?.androidVersion.orEmpty()} API ${device?.sdk ?: 0}",
            "user=${device?.identity?.userId ?: -1}, mode=$selectedMode, package=${selectedPackage.orEmpty()}",
            "Shizuku running=${device?.shizukuRunning}, granted=${device?.shizukuGranted}, root=${device?.rootAvailable}",
            "Source=${BuildConfig.SOURCE_COMMIT}","Photo confirmation=$verification",logs.joinToString("\n\n")).joinToString("\n")
    }

    private fun work(task:()->Pair<OperationResult?,DeviceReport>) {
        if(busy)return
        busy=true
        viewModelScope.launch {
            try {
                val (outcome,detected)=withContext(Dispatchers.IO){task()}
                report=detected
                if(!initialized) {
                    selectedMode=when {
                        ModePolicy.supportsCsc(detected.manufacturer)->SilenceMode.CSC
                        detected.group!=null->SilenceMode.AUDIO_GROUP
                        else->SilenceMode.CAMERA
                    };initialized=true
                }
                if(detected.cameras.none{it.packageName==selectedPackage}) {
                    selectedPackage=detected.cameras.firstOrNull()?.packageName
                    if(selectedMode==SilenceMode.APP_AUDIO){result=null;verification="untested"}
                }
                if(outcome!=null) {result=outcome;log("${outcome.outcome}: ${outcome.detail}")}
                detected.diagnostics.forEach(::log)
            }catch(e:CancellationException){throw e}
            catch(e:Exception){result=OperationResult(Outcome.FAILED,e.message.orEmpty());log("Error: ${e.message}")}
            finally{busy=false}
        }
    }
    private fun log(text:String) {
        logs.add("${SimpleDateFormat("HH:mm:ss",Locale.ROOT).format(Date())}  ${text.take(6000)}")
        while(logs.size>80)logs.removeAt(0)
    }
    override fun onCleared(){repository.close();super.onCleared()}
}
