package com.marvis.silentcamera.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.marvis.silentcamera.MainViewModel
import com.marvis.silentcamera.R

@Composable
fun DiagnosticsScreen(model:MainViewModel) {
    val context=LocalContext.current
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Text(stringResource(R.string.diagnostics_title),style=MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.diagnostics_subtitle),Modifier.padding(top=6.dp,bottom=16.dp),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                TextButton(onClick={
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("SilentCamera",model.diagnosticText()))
                    Toast.makeText(context,R.string.copied,Toast.LENGTH_SHORT).show()
                }){Text(stringResource(R.string.copy))}
                TextButton(onClick={safeOpen(context,Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,model.diagnosticText())},context.getString(R.string.share)))}){Text(stringResource(R.string.share))}
                TextButton(onClick=model::clearLogs,enabled=!model.busy){Text(stringResource(R.string.clear))}
            }
            Surface(Modifier.fillMaxWidth().padding(top=12.dp),shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp)){
            model.report?.let{report ->
                Text("${report.manufacturer} ${report.model}\nAndroid ${report.androidVersion} · API ${report.sdk}\nUser ${report.identity.userId}",style=MaterialTheme.typography.bodyMedium)
                report.cameras.forEach{Text("${it.label} ${it.version}\n${it.packageName}",Modifier.padding(top=8.dp),style=MaterialTheme.typography.bodySmall)}
            }
            model.result?.let{Text(stringResource(outcomeText(it.outcome)),Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.primary)}
            }
            }
        }
        if(model.logs.isEmpty())item{
            Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface){
                Column(Modifier.padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Icon(Icons.Default.Terminal,null,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.log_empty_title),style=MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.empty_logs),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(model.logs.toList()){line ->
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
                SelectionContainer {Text(line,Modifier.padding(14.dp),style=MaterialTheme.typography.bodySmall,fontFamily=FontFamily.Monospace)}
            }
        }
    }
}
