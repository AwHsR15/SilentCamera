package com.marvis.silentcamera.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.marvis.silentcamera.R
import com.marvis.silentcamera.camera.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

@Composable
fun CameraScreen() {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current
    val scope=rememberCoroutineScope()
    val main=remember(context){ContextCompat.getMainExecutor(context)}
    var permission by remember{mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
    var storage by remember{mutableStateOf(Build.VERSION.SDK_INT>=29 || ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)}
    DisposableEffect(lifecycle,context) {
        val observer=LifecycleEventObserver{_,event ->
            if(event==Lifecycle.Event.ON_RESUME) {
                permission=ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
                storage=Build.VERSION.SDK_INT>=29 || ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose{lifecycle.lifecycle.removeObserver(observer)}
    }
    val askCamera=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){permission=it}
    val askStorage=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){storage=it}
    var front by rememberSaveable{mutableStateOf(false)}
    var frameMode by rememberSaveable{mutableStateOf(false)}
    var grid by rememberSaveable{mutableStateOf(true)}
    var torch by remember{mutableStateOf(false)}
    var flash by remember{mutableStateOf(false)}
    var saving by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf<Int?>(null)}
    var error by remember{mutableStateOf("")}
    var lastPhoto by rememberSaveable{mutableStateOf<String?>(null)}
    var camera by remember{mutableStateOf<Camera?>(null)}
    var capture by remember{mutableStateOf<ImageCapture?>(null)}
    var zoom by remember{mutableFloatStateOf(0f)}
    var otherLens by remember{mutableStateOf(false)}
    var frameSize by remember{mutableStateOf("")}
    val preview=remember(context){PreviewView(context).apply{implementationMode=PreviewView.ImplementationMode.COMPATIBLE;scaleType=PreviewView.ScaleType.FIT_CENTER}}
    val latest=remember{AtomicReference<PreviewFrame?>(null)}
    val analyzer=remember{Executors.newSingleThreadExecutor()}
    val saveExecutor=remember{Executors.newSingleThreadExecutor()}
    DisposableEffect(Unit){onDispose{analyzer.shutdown();saveExecutor.shutdown();latest.set(null)}}
    DisposableEffect(permission,front) {
        var disposed=false
        var provider:ProcessCameraProvider?=null
        var analysis:ImageAnalysis?=null
        var imageCapture:ImageCapture?=null
        var rotationListener:OrientationEventListener?=null
        if(permission) {
            val future=ProcessCameraProvider.getInstance(context)
            future.addListener({
                if(!disposed)try {
                    provider=future.get()
                    var selector=if(front)CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    if(!provider!!.hasCamera(selector)) {
                        selector=if(front)CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                        front=!front
                    }
                    check(provider!!.hasCamera(selector)){"Requested camera is unavailable"}
                    otherLens=provider!!.hasCamera(if(front)CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA)
                    val previewCase=Preview.Builder().build().also{it.setSurfaceProvider(preview.surfaceProvider)}
                    imageCapture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    @Suppress("DEPRECATION")
                    val analysisCase=ImageAnalysis.Builder().setTargetResolution(android.util.Size(1280,720)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    var lastTime=0L
                    analysisCase.setAnalyzer(analyzer){image ->
                        try {
                            val now=System.nanoTime()
                            if(now-lastTime>300_000_000L){
                                val frame=FrameConverter.convert(image)
                                latest.set(frame);lastTime=now
                                val size=com.marvis.silentcamera.core.FramePixels.rotatedSize(frame.width,frame.height,frame.rotation)
                                main.execute{if(!disposed)frameSize="${size.first} × ${size.second}"}
                            }
                        }catch(_:Exception){latest.set(null)}finally{image.close()}
                    }
                    analysis=analysisCase
                    provider!!.unbindAll()
                    camera=provider!!.bindToLifecycle(lifecycle,selector,previewCase,imageCapture,analysisCase)
                    capture=imageCapture;error="";torch=false;flash=false;zoom=0f
                    rotationListener=object:OrientationEventListener(context){
                        override fun onOrientationChanged(orientation:Int){
                            if(orientation==ORIENTATION_UNKNOWN)return
                            val rotation=when(orientation){in 45..134->Surface.ROTATION_270;in 135..224->Surface.ROTATION_180;in 225..314->Surface.ROTATION_90;else->Surface.ROTATION_0}
                            imageCapture?.targetRotation=rotation;analysis?.targetRotation=rotation
                        }
                    }.also{it.enable()}
                }catch(e:Exception){camera=null;capture=null;status=R.string.camera_failed;error=e.message.orEmpty()}
            },main)
        }
        onDispose{disposed=true;rotationListener?.disable();analysis?.clearAnalyzer();provider?.unbindAll();camera=null;capture=null;latest.set(null)}
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.mode_camera),style=MaterialTheme.typography.headlineSmall)
        if(!permission) {
            Text(stringResource(R.string.camera_permission))
            Button(onClick={askCamera.launch(Manifest.permission.CAMERA)}){Text(stringResource(R.string.allow_camera))}
            Text(stringResource(R.string.permission_retry),style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={safeOpen(context,Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}){Text(stringResource(R.string.app_settings))}
        } else {
            Box(Modifier.fillMaxWidth().height(380.dp).background(Color.Black)) {
                AndroidView(factory={preview},modifier=Modifier.fillMaxSize())
                Canvas(Modifier.fillMaxSize().pointerInput(camera){detectTapGestures{offset ->
                    camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(preview.meteringPointFactory.createPoint(offset.x,offset.y)).build())
                }}) {
                    if(grid)for(i in 1..2){
                        drawLine(Color.White.copy(alpha=0.4f),Offset(size.width*i/3,0f),Offset(size.width*i/3,size.height))
                        drawLine(Color.White.copy(alpha=0.4f),Offset(0f,size.height*i/3),Offset(size.width,size.height*i/3))
                    }
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(frameMode.not(),onClick={frameMode=false},enabled=!saving,label={Text(stringResource(R.string.full_quality))})
                FilterChip(frameMode,onClick={frameMode=true},enabled=!saving,label={Text(stringResource(R.string.preview_frame))})
            }
            Text(stringResource(if(frameMode)R.string.frame_note else R.string.photo_note),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(frameMode && frameSize.isNotBlank())Text(frameSize,style=MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                IconButton(onClick={front=!front},enabled=otherLens && !saving){Icon(Icons.Default.Cameraswitch,stringResource(R.string.flip_camera))}
                IconButton(onClick={grid=!grid}){Icon(Icons.Default.GridOn,stringResource(R.string.grid),tint=if(grid)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}
                if(camera?.cameraInfo?.hasFlashUnit()==true) {
                    IconButton(onClick={torch=!torch;camera?.cameraControl?.enableTorch(torch)}){Icon(if(torch)Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,stringResource(R.string.torch))}
                    if(!frameMode)IconButton(onClick={flash=!flash;capture?.flashMode=if(flash)ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF}){Icon(if(flash)Icons.Default.FlashOn else Icons.Default.FlashOff,stringResource(R.string.flash))}
                }
            }
            if(camera?.cameraInfo?.zoomState?.value?.maxZoomRatio?.let{it>1f}==true)Slider(zoom,onValueChange={zoom=it;camera?.cameraControl?.setLinearZoom(it)})
            if(!storage){
                Text(stringResource(R.string.storage_permission))
                Button(onClick={askStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)}){Text(stringResource(R.string.allow_storage))}
            }
            Button(onClick={
                saving=true;status=null;error=""
                if(frameMode) {
                    val frame=latest.get()
                    if(frame==null){saving=false;status=R.string.no_preview_frame}
                    else scope.launch {
                        try {
                            val uri=withContext(Dispatchers.IO){val bitmap=frame.bitmap();try{PhotoStorage.save(context,bitmap)}finally{bitmap.recycle()}}
                            lastPhoto=uri.toString();status=R.string.saved
                        }catch(e:Exception){status=R.string.save_failed;error=e.message.orEmpty()}finally{saving=false}
                    }
                } else {
                    try {
                        val options=ImageCapture.OutputFileOptions.Builder(context.contentResolver,MediaStore.Images.Media.EXTERNAL_CONTENT_URI,PhotoStorage.values()).build()
                        val current=capture ?: error("Camera not ready")
                        current.takePicture(options,saveExecutor,object:ImageCapture.OnImageSavedCallback{
                            override fun onImageSaved(output:ImageCapture.OutputFileResults){main.execute{lastPhoto=output.savedUri?.toString();saving=false;status=R.string.saved}}
                            override fun onError(exception:ImageCaptureException){main.execute{saving=false;status=R.string.save_failed;error=exception.message.orEmpty()}}
                        })
                    }catch(e:Exception){saving=false;status=R.string.save_failed;error=e.message.orEmpty()}
                }
            },enabled=storage && capture!=null && !saving,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){
                Icon(Icons.Default.CameraAlt,null);Spacer(Modifier.width(8.dp));Text(stringResource(if(saving)R.string.busy else if(frameMode)R.string.capture_frame else R.string.capture_photo))
            }
            Text(stringResource(R.string.supported_camera_note),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        status?.let{Text(stringResource(it),color=if(it==R.string.saved)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)}
        if(error.isNotBlank())Text(error.take(500),style=MaterialTheme.typography.bodySmall)
        lastPhoto?.let{uri->OutlinedButton(onClick={safeOpen(context,Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri),"image/jpeg").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}){Text(stringResource(R.string.open_last_photo))}}
    }
}
