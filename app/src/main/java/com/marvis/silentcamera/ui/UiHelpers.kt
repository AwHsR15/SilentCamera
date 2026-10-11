package com.marvis.silentcamera.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.marvis.silentcamera.R
import com.marvis.silentcamera.core.*
import com.marvis.silentcamera.modes.Requirement

fun safeOpen(context:Context,intent:Intent) {
    runCatching{context.startActivity(intent)}.onFailure{Toast.makeText(context,R.string.no_handler,Toast.LENGTH_SHORT).show()}
}
fun openUrl(context:Context,url:String)=safeOpen(context,Intent(Intent.ACTION_VIEW,Uri.parse(url)))
fun modeTitle(mode:SilenceMode)=when(mode){
    SilenceMode.CSC->R.string.mode_csc;SilenceMode.AUDIO_GROUP->R.string.mode_group
    SilenceMode.APP_AUDIO->R.string.mode_app;SilenceMode.ROOT_PROPERTY->R.string.mode_root
    SilenceMode.MAGISK->R.string.mode_magisk;SilenceMode.CAMERA->R.string.mode_camera
}
fun modeDescription(mode:SilenceMode)=when(mode){
    SilenceMode.CSC->R.string.mode_csc_desc;SilenceMode.AUDIO_GROUP->R.string.mode_group_desc
    SilenceMode.APP_AUDIO->R.string.mode_app_desc;SilenceMode.ROOT_PROPERTY->R.string.mode_root_desc
    SilenceMode.MAGISK->R.string.mode_magisk_desc;SilenceMode.CAMERA->R.string.mode_camera_desc
}
fun requirementText(requirement:Requirement)=when(requirement){
    Requirement.READY->R.string.req_ready;Requirement.SHIZUKU->R.string.req_shizuku
    Requirement.SAMSUNG_ONLY->R.string.req_samsung;Requirement.AUDIO_POLICY->R.string.req_audio
    Requirement.SETTING_UNREADABLE->R.string.req_setting_unreadable
    Requirement.EXPERIMENTAL_AUDIO->R.string.req_app;Requirement.ROOT->R.string.req_root
    Requirement.ROOT_READY->R.string.req_root_ready;Requirement.MODULE->R.string.req_module
    Requirement.CAMERA_PERMISSION->R.string.req_camera
}
fun outcomeText(outcome:Outcome)=when(outcome){
    Outcome.APPLIED->R.string.applied;Outcome.RESTORED->R.string.restored;Outcome.FAILED->R.string.failed
    Outcome.ROLLBACK_FAILED->R.string.rollback_failed;Outcome.NO_BACKUP->R.string.no_original
    Outcome.IDENTITY_MISMATCH->R.string.identity_mismatch
}
