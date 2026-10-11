package com.marvis.silentcamera.ui

import android.content.Intent
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marvis.silentcamera.MainViewModel
import com.marvis.silentcamera.R
import com.marvis.silentcamera.core.*
import com.marvis.silentcamera.modes.*

@Composable
fun ModeScreen(model:MainViewModel,onPermission:()->Unit,onCamera:()->Unit) {
    val context=LocalContext.current
    val report=model.report
    val colors=MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=20.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Card(colors=CardDefaults.cardColors(containerColor=colors.primaryContainer),shape=RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(stringResource(R.string.device),style=MaterialTheme.typography.labelLarge,color=colors.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text(if(report==null)stringResource(R.string.detect_required)else "${report.manufacturer} ${report.model}",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
                    if(report!=null)Text(stringResource(R.string.android_user,report.androidVersion,report.sdk,report.identity.userId),style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick=model::refresh,enabled=!model.busy,contentPadding=PaddingValues(0.dp)) {
                        Icon(Icons.Default.Refresh,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(stringResource(R.string.refresh))
                    }
                }
            }
        }
        if(model.busy)item{Column{LinearProgressIndicator(Modifier.fillMaxWidth());Text(stringResource(R.string.busy),Modifier.padding(top=6.dp),style=MaterialTheme.typography.bodySmall)}}
        item {
            OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.VerifiedUser,null,tint=colors.primary)
                        Column(Modifier.padding(start=10.dp)){
                            Text(stringResource(R.string.shizuku),style=MaterialTheme.typography.titleSmall)
                            Text(stringResource(if(report?.shizukuGranted==true)R.string.connected else if(report?.shizukuRunning==true)R.string.not_authorized else R.string.not_running),style=MaterialTheme.typography.bodySmall)
                        }
                    }
                    if(report?.shizukuGranted!=true) {
                        FilledTonalButton(onClick=onPermission,enabled=!model.busy){Text(stringResource(R.string.request_permission))}
                        Row {
                            TextButton(onClick={
                                context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let{safeOpen(context,it)}
                                    ?: openUrl(context,"https://shizuku.rikka.app/download/")
                            }){Text(stringResource(R.string.open_shizuku))}
                            TextButton(onClick={openUrl(context,"https://shizuku.rikka.app/guide/setup/")}){Text(stringResource(R.string.setup_guide))}
                        }
                    }
                }
            }
        }
        item{Text(stringResource(R.string.all_modes),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}
        items(SilenceMode.entries,key={it.name}){mode ->
            val assessment=report?.assessments?.firstOrNull{it.mode==mode}
                ?: ModeAssessment(mode,Availability.DETECT_REQUIRED,Requirement.READY)
            val selected=model.selectedMode==mode
            OutlinedCard(onClick={model.selectMode(mode)},enabled=!model.busy,
                border=BorderStroke(if(selected)2.dp else 1.dp,if(selected)colors.primary else colors.outlineVariant),
                colors=CardDefaults.outlinedCardColors(containerColor=if(selected)colors.surfaceVariant else colors.surface),shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(stringResource(modeTitle(mode)),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                        if(selected)Icon(Icons.Default.CheckCircle,null,tint=colors.primary)
                    }
                    Text(stringResource(when(assessment.availability){Availability.AVAILABLE->R.string.available;Availability.NEEDS_PERMISSION->R.string.permission_needed;Availability.EXPERIMENTAL->R.string.experimental;Availability.UNSUPPORTED->R.string.unsupported;Availability.DETECT_REQUIRED->R.string.detect_required}),
                        style=MaterialTheme.typography.labelMedium,color=if(assessment.availability==Availability.UNSUPPORTED)colors.onSurfaceVariant else colors.primary)
                    if(selected) {
                        Text(stringResource(modeDescription(mode)),style=MaterialTheme.typography.bodyMedium)
                        Text(stringResource(requirementText(assessment.requirement)),style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                        if(mode==SilenceMode.CSC && report?.shizukuGranted==true)Text(stringResource(R.string.csc_status,report.cscValue ?: stringResource(R.string.missing_value)),style=MaterialTheme.typography.bodySmall)
                        if(mode==SilenceMode.AUDIO_GROUP && report?.group!=null)Text(stringResource(R.string.group_status,report.group.id),style=MaterialTheme.typography.bodySmall)
                        if(mode==SilenceMode.APP_AUDIO) {
                            Text(stringResource(R.string.select_camera),style=MaterialTheme.typography.labelLarge)
                            if(report?.cameras.isNullOrEmpty())Text(stringResource(R.string.no_camera_apps),style=MaterialTheme.typography.bodySmall)
                            report?.cameras?.forEach{app ->
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                                    RadioButton(model.selectedPackage==app.packageName,onClick={model.selectPackage(app.packageName)},enabled=!model.busy)
                                    Column(Modifier.weight(1f)){Text(app.label,style=MaterialTheme.typography.bodyMedium);Text(app.packageName,style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)}
                                }
                            }
                        }
                        when(mode) {
                            SilenceMode.CAMERA->Button(onClick=onCamera,Modifier.fillMaxWidth()){Text(stringResource(R.string.open_camera))}
                            SilenceMode.MAGISK->{
                                Button(onClick={openUrl(context,"https://github.com/AwHsR15/SilentCamera/raw/main/release/SilentCamera-v2.0-magisk.zip")},Modifier.fillMaxWidth()){Text(stringResource(R.string.download_module))}
                                TextButton(onClick={openUrl(context,"https://github.com/AwHsR15/SilentCamera/blob/main/docs/MAGISK.md")}){Text(stringResource(R.string.module_guide))}
                            }
                            else->{
                                if(mode==SilenceMode.ROOT_PROPERTY)OutlinedButton(onClick=model::checkRoot,enabled=!model.busy){Text(stringResource(R.string.check_root))}
                                Text(stringResource(if(model.hasBackup())R.string.has_backup else R.string.no_backup),style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                                val canApply=assessment.availability in setOf(Availability.AVAILABLE,Availability.EXPERIMENTAL) && (mode!=SilenceMode.APP_AUDIO || model.selectedPackage!=null)
                                Button(onClick=model::applyMode,enabled=canApply && !model.busy,modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.apply))}
                                OutlinedButton(onClick=model::restoreMode,enabled=model.hasBackup() && !model.busy && (if(mode==SilenceMode.ROOT_PROPERTY)report?.rootAvailable==true else report?.shizukuGranted==true),modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.restore))}
                            }
                        }
                        model.result?.let{outcome ->
                            HorizontalDivider();Text(stringResource(outcomeText(outcome.outcome)),color=if(outcome.successful)colors.primary else colors.error,style=MaterialTheme.typography.bodyMedium)
                        }
                        if(model.result?.outcome==Outcome.APPLIED) {
                            Text(stringResource(R.string.verification),style=MaterialTheme.typography.titleSmall)
                            Text(stringResource(when(model.verification){"quiet"->R.string.confirmed_quiet;"audible"->R.string.confirmed_audible;else->R.string.untested}),style=MaterialTheme.typography.bodySmall)
                            TextButton(onClick={safeOpen(context,Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))}){Text(stringResource(R.string.stock_camera))}
                            OutlinedButton(onClick={model.confirmPhoto(true)},Modifier.fillMaxWidth()){Text(stringResource(R.string.quiet))}
                            TextButton(onClick={model.confirmPhoto(false)}){Text(stringResource(R.string.audible))}
                        }
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.compatibility_note),style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
            TextButton(onClick={openUrl(context,"https://github.com/AwHsR15/SilentCamera/blob/main/docs/research/shutter-muting-approaches.md")}){Text(stringResource(R.string.other_methods))}
            Text(stringResource(R.string.release_preview),style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
        }
    }
}
