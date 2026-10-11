package com.marvis.silentcamera

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
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
    val cameraTab=tab==1
    Scaffold(containerColor=if(cameraTab)androidx.compose.ui.graphics.Color(0xFF101412) else scheme.background,
        topBar={
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=24.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.CameraAlt,null,Modifier.size(24.dp),tint=if(cameraTab)androidx.compose.ui.graphics.Color(0xFF80D8AE) else scheme.primary)
                Text("SilentCamera",Modifier.weight(1f).padding(start=10.dp),style=MaterialTheme.typography.titleLarge,
                    color=if(cameraTab)androidx.compose.ui.graphics.Color.White else scheme.onSurface)
                IconButton(onClick={settings=true}){Icon(Icons.Default.Tune,stringResource(R.string.settings),
                    tint=if(cameraTab)androidx.compose.ui.graphics.Color.White else scheme.onSurfaceVariant)}
            }
        },
        bottomBar={
            Column(Modifier.background(scheme.surface)) {
                HorizontalDivider(color=scheme.outlineVariant)
                NavigationBar(containerColor=scheme.surface,tonalElevation=0.dp) {
                    listOf(R.string.tab_modes to Icons.Default.Widgets,R.string.tab_camera to Icons.Default.CameraAlt,R.string.tab_diagnostics to Icons.AutoMirrored.Filled.Assignment)
                        .forEachIndexed{i,(label,icon)->
                            NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(icon,null,Modifier.size(22.dp))},label={Text(stringResource(label))},
                                colors=NavigationBarItemDefaults.colors(indicatorColor=scheme.primaryContainer,selectedIconColor=scheme.primary,selectedTextColor=scheme.primary,
                                    unselectedIconColor=scheme.onSurfaceVariant,unselectedTextColor=scheme.onSurfaceVariant))
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencesDialog(onClose:()->Unit,onTheme:(String)->Unit,onLanguage:(String)->Unit) {
    val context=LocalContext.current
    var theme by remember{mutableStateOf(AppPreferences.theme(context))}
    val language=AppPreferences.language(context)
    ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.surface,tonalElevation=0.dp) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start=24.dp,end=24.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings),style=MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.language),Modifier.padding(top=16.dp),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("system" to stringResource(R.string.system),"zh-CN" to "简体中文","ja" to "日本語","en" to "English").forEach{(value,label)->
                PreferenceOption(label,language==value){onLanguage(value)}
            }
            HorizontalDivider(Modifier.padding(vertical=8.dp),color=MaterialTheme.colorScheme.outlineVariant)
            Text(stringResource(R.string.appearance),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("system" to R.string.system,"light" to R.string.light,"dark" to R.string.dark).forEach{(value,label)->
                PreferenceOption(stringResource(label),theme==value){theme=value;onTheme(value)}
            }
        }
    }
}
@Composable
private fun PreferenceOption(label:String,selected:Boolean,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected=selected,role=Role.RadioButton,onClick=onClick).heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
        RadioButton(selected,onClick=null)
    }
}
