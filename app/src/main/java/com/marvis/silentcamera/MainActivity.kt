package com.marvis.silentcamera

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marvis.silentcamera.ui.*
import com.marvis.silentcamera.shell.ShizukuExecutor
import rikka.shizuku.Shizuku

class MainActivity:ComponentActivity() {
    private val model:MainViewModel by viewModels()
    private val permissionListener=Shizuku.OnRequestPermissionResultListener{_,_->runOnUiThread{model.refresh()}}
    private val receivedListener=Shizuku.OnBinderReceivedListener{runOnUiThread{model.refresh()}}
    private val deadListener=Shizuku.OnBinderDeadListener{runOnUiThread{model.refresh()}}
    override fun attachBaseContext(newBase:Context){super.attachBaseContext(AppPreferences.localized(newBase))}
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState);enableEdgeToEdge()
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(receivedListener)
        Shizuku.addBinderDeadListener(deadListener)
        setContent {
            var theme by remember{mutableStateOf(AppPreferences.theme(this))}
            SilentCameraTheme(theme) {
                AppScreen(model,::requestShizuku,
                    onTheme={theme=it;AppPreferences.save(this,"theme",it)},
                    onLanguage={AppPreferences.save(this,"language",it);recreate()})
            }
        }
    }
    override fun onResume(){super.onResume();model.refresh()}
    override fun onDestroy(){
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(receivedListener)
        Shizuku.removeBinderDeadListener(deadListener)
        super.onDestroy()
    }
    private fun requestShizuku() {
        if(!ShizukuExecutor.available()) {
            safeOpen(this,Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://shizuku.rikka.app/guide/setup/")));return
        }
        if(ShizukuExecutor.granted()){model.refresh();return}
        if(Shizuku.shouldShowRequestPermissionRationale()) {
            packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let{safeOpen(this,it)};return
        }
        runCatching{Shizuku.requestPermission(20)}.onFailure{model.refresh()}
    }
}

@Composable
private fun AppScreen(model:MainViewModel,onPermission:()->Unit,onTheme:(String)->Unit,onLanguage:(String)->Unit) {
    var tab by rememberSaveable{mutableIntStateOf(0)}
    var settings by remember{mutableStateOf(false)}
    val scheme=MaterialTheme.colorScheme
    Scaffold(
        topBar={
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=20.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(scheme.primaryContainer,RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center){
                    Icon(Icons.Default.CameraAlt,null,tint=scheme.primary)
                }
                Column(Modifier.weight(1f).padding(start=12.dp)) {
                    Text("SilentCamera",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text(stringResource(R.string.tagline),style=MaterialTheme.typography.bodySmall,color=scheme.onSurfaceVariant)
                }
                IconButton(onClick={settings=true}){Icon(Icons.Default.Tune,stringResource(R.string.settings))}
            }
        },
        bottomBar={
            NavigationBar {
                listOf(R.string.tab_modes to Icons.Default.Widgets,R.string.tab_camera to Icons.Default.CameraAlt,R.string.tab_diagnostics to Icons.AutoMirrored.Filled.Assignment)
                    .forEachIndexed{i,(label,icon)->
                        NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(icon,null)},label={Text(stringResource(label))})
                    }
            }
        },
    ) {padding ->
        Box(Modifier.fillMaxSize().padding(padding)){
            when(tab){0->ModeScreen(model,onPermission,{tab=1});1->CameraScreen();else->DiagnosticsScreen(model)}
        }
    }
    if(settings)PreferencesDialog(onClose={settings=false},onTheme,onLanguage)
}

@Composable
private fun PreferencesDialog(onClose:()->Unit,onTheme:(String)->Unit,onLanguage:(String)->Unit) {
    val context=LocalContext.current
    var theme by remember{mutableStateOf(AppPreferences.theme(context))}
    val language=AppPreferences.language(context)
    AlertDialog(onDismissRequest=onClose,title={Text(stringResource(R.string.settings))},
        text={Column(Modifier.verticalScroll(rememberScrollState())){
            Text(stringResource(R.string.language),style=MaterialTheme.typography.titleSmall)
            listOf("system" to stringResource(R.string.system),"zh-CN" to "简体中文","ja" to "日本語","en" to "English").forEach{(value,label)->
                PreferenceOption(label,language==value){onLanguage(value)}
            }
            Spacer(Modifier.height(12.dp));Text(stringResource(R.string.appearance),style=MaterialTheme.typography.titleSmall)
            listOf("system" to R.string.system,"light" to R.string.light,"dark" to R.string.dark).forEach{(value,label)->
                PreferenceOption(stringResource(label),theme==value){theme=value;onTheme(value)}
            }
        }},confirmButton={TextButton(onClick=onClose){Text(stringResource(R.string.close))}})
}
@Composable
private fun PreferenceOption(label:String,selected:Boolean,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        RadioButton(selected,onClick);TextButton(onClick=onClick){Text(label)}
    }
}
