package com.marvis.silentcamera.ui

import android.content.Intent
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marvis.silentcamera.MainViewModel
import com.marvis.silentcamera.R
import com.marvis.silentcamera.core.*
import com.marvis.silentcamera.modes.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeScreen(model:MainViewModel,onPermission:()->Unit,onCamera:()->Unit) {
    var showDetails by remember{mutableStateOf(false)}
    ModeDashboard(model.report,model.selectedMode,model.busy,onRefresh=model::refresh,onPermission=onPermission,onCamera=onCamera,
        onMode={mode ->
            if(!model.busy) {
                if(model.selectedMode!=mode)model.selectMode(mode)
                if(mode==SilenceMode.CAMERA)onCamera() else showDetails=true
            }
        })
    if(showDetails)ModalBottomSheet(onDismissRequest={showDetails=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        containerColor=MaterialTheme.colorScheme.surface,tonalElevation=0.dp) {
        ModeDetails(model,onPermission,onCamera)
    }
}

@Composable
fun ModeDashboard(report:DeviceReport?,selectedMode:SilenceMode,busy:Boolean,onRefresh:()->Unit,onPermission:()->Unit,onCamera:()->Unit,onMode:(SilenceMode)->Unit) {
    val colors=MaterialTheme.colorScheme
    val columns=if(LocalConfiguration.current.screenWidthDp<360 || LocalDensity.current.fontScale>1.25f)1 else 2
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=24.dp,end=24.dp,top=8.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_title),style=MaterialTheme.typography.headlineLarge)
                    Text(stringResource(R.string.home_subtitle),Modifier.padding(top=5.dp),style=MaterialTheme.typography.bodyMedium,color=colors.onSurfaceVariant)
                }
                IconButton(onClick=onRefresh,enabled=!busy){Icon(Icons.Default.Refresh,stringResource(R.string.refresh),tint=colors.onSurfaceVariant)}
            }
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Smartphone,null,Modifier.size(18.dp),tint=colors.onSurfaceVariant)
                Text(if(report==null)stringResource(R.string.detect_required) else "${report.manufacturer} ${report.model}",Modifier.weight(1f),
                    style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis,color=colors.onSurfaceVariant)
                if(report!=null)StatusPill("Android ${report.androidVersion}")
            }
        }
        item {
            Card(onClick=onCamera,shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF18392B))) {
                Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF18392B),Color(0xFF10261D)))).padding(22.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(stringResource(R.string.camera_hero_label),style=MaterialTheme.typography.labelMedium,color=Color(0xFF91CDB0))
                        Text(stringResource(R.string.camera_hero_title),style=MaterialTheme.typography.titleLarge,color=Color.White)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text(stringResource(R.string.open_camera),style=MaterialTheme.typography.labelLarge,color=Color.White)
                            Icon(Icons.Default.ArrowForward,null,Modifier.padding(start=8.dp).size(16.dp),tint=Color.White)
                        }
                    }
                    Surface(Modifier.size(72.dp),shape=RoundedCornerShape(24.dp),color=Color.White.copy(alpha=0.07f)) {
                        Box(contentAlignment=Alignment.Center){Icon(Icons.Default.CameraAlt,null,Modifier.size(34.dp),tint=Color(0xFFB7E9CF))}
                    }
                }
            }
        }
        item {
            Surface(shape=RoundedCornerShape(18.dp),color=colors.surface) {
                Row(Modifier.fillMaxWidth().padding(start=16.dp,end=6.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser,null,Modifier.size(21.dp),tint=if(report?.shizukuGranted==true)colors.primary else colors.onSurfaceVariant)
                    Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                        Text("Shizuku",style=MaterialTheme.typography.titleSmall)
                        Text(stringResource(if(report?.shizukuGranted==true)R.string.connected else if(report?.shizukuRunning==true)R.string.not_authorized else R.string.not_running),
                            style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                    }
                    TextButton(onClick=onPermission,enabled=!busy){Text(stringResource(if(report?.shizukuGranted==true)R.string.refresh else R.string.connect_action))}
                }
            }
        }
        if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth(),color=colors.primary,trackColor=colors.primaryContainer)}
        item {
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(stringResource(R.string.modes_heading),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                    Text("06",style=MaterialTheme.typography.labelLarge,color=colors.onSurfaceVariant)
                }
                SilenceMode.entries.chunked(columns).forEach { modes ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        modes.forEach {mode ->
                            val assessment=report?.assessments?.firstOrNull{it.mode==mode}
                                ?: ModeAssessment(mode,Availability.DETECT_REQUIRED,Requirement.READY)
                            ModeTile(mode,assessment.availability,selectedMode==mode,busy,Modifier.weight(1f)){onMode(mode)}
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info,null,Modifier.padding(top=2.dp).size(15.dp),tint=colors.onSurfaceVariant)
                Text(stringResource(R.string.compatibility_note),style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ModeTile(mode:SilenceMode,availability:Availability,selected:Boolean,busy:Boolean,modifier:Modifier,onClick:()->Unit) {
    val colors=MaterialTheme.colorScheme
    OutlinedCard(onClick=onClick,enabled=!busy,modifier=modifier,shape=RoundedCornerShape(22.dp),
        border=BorderStroke(1.dp,if(selected)colors.primary else colors.outlineVariant),
        colors=CardDefaults.outlinedCardColors(containerColor=colors.surface)) {
        Column(Modifier.fillMaxWidth().heightIn(min=144.dp).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Surface(shape=RoundedCornerShape(12.dp),color=if(selected)colors.primaryContainer else colors.surfaceVariant) {
                    Icon(modeIcon(mode),null,Modifier.padding(9.dp).size(21.dp),tint=if(selected)colors.primary else colors.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                if(selected)Icon(Icons.Default.CheckCircle,null,Modifier.size(16.dp),tint=colors.primary)
            }
            Text(stringResource(modeTitle(mode)),style=MaterialTheme.typography.titleSmall,minLines=2)
            AvailabilityPill(availability)
        }
    }
}

fun modeIcon(mode:SilenceMode):ImageVector=when(mode){
    SilenceMode.CSC->Icons.Default.Tune;SilenceMode.AUDIO_GROUP->Icons.Default.VolumeOff
    SilenceMode.APP_AUDIO->Icons.Default.Apps;SilenceMode.ROOT_PROPERTY->Icons.Default.Security
    SilenceMode.MAGISK->Icons.Default.Layers;SilenceMode.CAMERA->Icons.Default.CameraAlt
}
@Composable
fun StatusPill(text:String,accent:Boolean=false) {
    val c=MaterialTheme.colorScheme
    Surface(shape=RoundedCornerShape(7.dp),color=if(accent)c.primaryContainer else c.surfaceVariant) {
        Text(text,Modifier.padding(horizontal=8.dp,vertical=4.dp),style=MaterialTheme.typography.labelMedium,color=if(accent)c.primary else c.onSurfaceVariant)
    }
}
@Composable
private fun AvailabilityPill(availability:Availability) {
    StatusPill(stringResource(when(availability){Availability.AVAILABLE->R.string.available;Availability.NEEDS_PERMISSION->R.string.permission_needed;
        Availability.EXPERIMENTAL->R.string.experimental_short;Availability.UNSUPPORTED->R.string.unsupported_short;Availability.DETECT_REQUIRED->R.string.detect_required}),
        availability==Availability.AVAILABLE)
}

@Composable
private fun ModeDetails(model:MainViewModel,onPermission:()->Unit,onCamera:()->Unit) {
    val context=LocalContext.current
    val colors=MaterialTheme.colorScheme
    val mode=model.selectedMode
    val report=model.report
    val assessment=report?.assessments?.firstOrNull{it.mode==mode} ?: ModeAssessment(mode,Availability.DETECT_REQUIRED,Requirement.READY)
    LazyColumn(Modifier.fillMaxWidth(),contentPadding=PaddingValues(start=24.dp,end=24.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Surface(shape=RoundedCornerShape(16.dp),color=colors.primaryContainer){Icon(modeIcon(mode),null,Modifier.padding(13.dp).size(25.dp),tint=colors.primary)}
                Column(Modifier.weight(1f)) {
                    Text(stringResource(modeTitle(mode)),style=MaterialTheme.typography.titleLarge)
                    AvailabilityPill(assessment.availability)
                }
            }
        }
        item {
            Text(stringResource(modeDescription(mode)),style=MaterialTheme.typography.bodyMedium)
            Text(stringResource(requirementText(assessment.requirement)),Modifier.padding(top=8.dp),style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
        }
        if(assessment.availability==Availability.NEEDS_PERMISSION && mode!=SilenceMode.ROOT_PROPERTY)item {
            FilledTonalButton(onClick=onPermission,enabled=!model.busy,modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.request_permission))}
            Row {
                TextButton(onClick={context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let{safeOpen(context,it)} ?: openUrl(context,"https://shizuku.rikka.app/download/")}){Text(stringResource(R.string.open_shizuku))}
                TextButton(onClick={openUrl(context,"https://shizuku.rikka.app/guide/setup/")}){Text(stringResource(R.string.setup_guide))}
            }
        }
        if(mode==SilenceMode.CSC && report?.shizukuGranted==true)item{Text(stringResource(R.string.csc_status,report.cscValue ?: stringResource(R.string.missing_value)),style=MaterialTheme.typography.bodySmall)}
        if(mode==SilenceMode.AUDIO_GROUP && report?.group!=null)item{Text(stringResource(R.string.group_status,report.group.id),style=MaterialTheme.typography.bodySmall)}
        if(mode==SilenceMode.APP_AUDIO)item {
            Text(stringResource(R.string.select_camera),style=MaterialTheme.typography.titleSmall)
            if(report?.cameras.isNullOrEmpty())Text(stringResource(R.string.no_camera_apps),style=MaterialTheme.typography.bodySmall)
            report?.cameras?.forEach {app ->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(model.selectedPackage==app.packageName,onClick={model.selectPackage(app.packageName)},enabled=!model.busy)
                    Column(Modifier.weight(1f)){Text(app.label,style=MaterialTheme.typography.bodyMedium);Text(app.packageName,style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)}
                }
            }
        }
        item {
            when(mode) {
                SilenceMode.CAMERA->Button(onClick=onCamera,modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.open_camera))}
                SilenceMode.MAGISK->{
                    Button(onClick={openUrl(context,"https://github.com/AwHsR15/SilentCamera/raw/main/release/SilentCamera-v2.1-magisk.zip")},modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.download_module))}
                    TextButton(onClick={openUrl(context,"https://github.com/AwHsR15/SilentCamera/blob/main/docs/MAGISK.md")}){Text(stringResource(R.string.module_guide))}
                }
                else->{
                    if(mode==SilenceMode.ROOT_PROPERTY)FilledTonalButton(onClick=model::checkRoot,enabled=!model.busy,modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.check_root))}
                    Row(Modifier.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Icon(if(model.hasBackup())Icons.Default.Shield else Icons.Default.Info,null,Modifier.size(16.dp),tint=colors.onSurfaceVariant)
                        Text(stringResource(if(model.hasBackup())R.string.has_backup else R.string.no_backup),style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                    }
                    val canApply=assessment.availability in setOf(Availability.AVAILABLE,Availability.EXPERIMENTAL) && (mode!=SilenceMode.APP_AUDIO || model.selectedPackage!=null)
                    Button(onClick=model::applyMode,enabled=canApply && !model.busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text(stringResource(if(model.busy)R.string.busy else R.string.apply))}
                    OutlinedButton(onClick=model::restoreMode,enabled=model.hasBackup() && !model.busy && (if(mode==SilenceMode.ROOT_PROPERTY)report?.rootAvailable==true else report?.shizukuGranted==true),
                        modifier=Modifier.fillMaxWidth().padding(top=8.dp).heightIn(min=48.dp)){Icon(Icons.Default.Restore,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(stringResource(R.string.restore))}
                }
            }
        }
        model.result?.let {outcome ->item {
            Surface(shape=RoundedCornerShape(16.dp),color=if(outcome.successful)colors.primaryContainer else colors.errorContainer) {
                Text(stringResource(outcomeText(outcome.outcome)),Modifier.fillMaxWidth().padding(16.dp),style=MaterialTheme.typography.bodyMedium,
                    color=if(outcome.successful)colors.onPrimaryContainer else colors.onErrorContainer)
            }
        }}
        if(model.result?.outcome==Outcome.APPLIED)item {
            Text(stringResource(R.string.verification),style=MaterialTheme.typography.titleSmall)
            Text(stringResource(when(model.verification){"quiet"->R.string.confirmed_quiet;"audible"->R.string.confirmed_audible;else->R.string.untested}),style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={safeOpen(context,Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))}){Text(stringResource(R.string.stock_camera))}
            FilledTonalButton(onClick={model.confirmPhoto(true)},modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.quiet))}
            TextButton(onClick={model.confirmPhoto(false)},modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.audible))}
        }
        item{TextButton(onClick={openUrl(context,"https://github.com/AwHsR15/SilentCamera/blob/main/docs/research/shutter-muting-approaches.md")}){Text(stringResource(R.string.other_methods))}}
    }
}
