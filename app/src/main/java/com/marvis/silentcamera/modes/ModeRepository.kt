package com.marvis.silentcamera.modes

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.marvis.silentcamera.core.*
import com.marvis.silentcamera.shell.RootExecutor
import com.marvis.silentcamera.shell.ShizukuExecutor
import org.json.JSONArray

data class CameraApp(val packageName:String,val label:String,val version:String)
enum class Requirement { READY, SHIZUKU, SAMSUNG_ONLY, SETTING_UNREADABLE, AUDIO_POLICY, EXPERIMENTAL_AUDIO, ROOT, ROOT_READY, MODULE, CAMERA_PERMISSION }
data class ModeAssessment(val mode:SilenceMode,val availability:Availability,val requirement:Requirement)
data class DeviceReport(
    val manufacturer:String,val model:String,val androidVersion:String,val sdk:Int,
    val identity:DeviceIdentity,val cameras:List<CameraApp>,val assessments:List<ModeAssessment>,
    val shizukuRunning:Boolean,val shizukuGranted:Boolean,val rootAvailable:Boolean,
    val cscValue:String?=null,val group:AudioGroup?=null,val rootProperties:List<String> = emptyList(),
    val diagnostics:List<String> = emptyList(),val backupKeys:Set<String> = emptySet(),
)

class ModeRepository(private val context:Context) {
    val shell=ShizukuExecutor(context)
    private val root=RootExecutor()
    private val backups=AndroidBackupStore(context)
    private val identity=DeviceIdentity(Build.FINGERPRINT,Process.myUid()/100_000)
    private var rootAvailable=false
    private val reportExecutor=CommandExecutor { args ->
        val r=shell.execute(args)
        commandLog.add("\$ ${args.joinToString(" ")}\nexit=${r.exitCode}\n${r.stdout.take(4000)}${if(r.stderr.isBlank())"" else "\n${r.stderr.take(1000)}"}")
        r
    }
    private val commandLog=mutableListOf<String>()

    fun requestRoot():Boolean {
        rootAvailable=root.execute(listOf("id","-u")).let{it.isSuccess && it.stdout.trim()=="0"}
        if(rootAvailable)rootAvailable=root.execute(listOf("resetprop","--help")).let{it.isSuccess && (it.stdout+it.stderr).contains("resetprop",ignoreCase=true)}
        return rootAvailable
    }

    @Suppress("DEPRECATION")
    fun detect():DeviceReport {
        commandLog.clear()
        val running=ShizukuExecutor.available();val granted=running && ShizukuExecutor.granted()
        val cameras=context.packageManager.queryIntentActivities(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE),PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { ri ->
                val pkg=ri.activityInfo.packageName
                if(!ModePolicy.validPackage(pkg))null else runCatching{
                    CameraApp(pkg,ri.loadLabel(context.packageManager).toString(),context.packageManager.getPackageInfo(pkg,0).versionName.orEmpty())
                }.getOrNull()
            }.distinctBy{it.packageName}.sortedBy{it.label}
        var csc:String?=null;var cscReadable=false;var group:AudioGroup?=null
        if(granted) {
            if(ModePolicy.supportsCsc(Build.MANUFACTURER)) runCatching{CscTarget(reportExecutor,identity.userId).read()}
                .onSuccess{csc=it;cscReadable=true}.onFailure{commandLog.add("CSC setting unreadable: ${it.message}")}
            group=runCatching{readAudioGroup()}.onFailure{commandLog.add("Audio policy unavailable: ${it.message}")}.getOrNull()
        }
        val props=if(rootAvailable) CommandPolicy.properties.filter{key ->
            root.execute(listOf("getprop",key)).let{it.isSuccess && it.stdout.trim() in setOf("0","1","true","false")}
        }.sorted() else emptyList()
        val assessments=SilenceMode.entries.map{mode ->when(mode) {
            SilenceMode.CSC -> if(!ModePolicy.supportsCsc(Build.MANUFACTURER)) ModeAssessment(mode,Availability.UNSUPPORTED,Requirement.SAMSUNG_ONLY)
                else if(!granted) ModeAssessment(mode,Availability.NEEDS_PERMISSION,Requirement.SHIZUKU)
                else if(!cscReadable)ModeAssessment(mode,Availability.UNSUPPORTED,Requirement.SETTING_UNREADABLE)
                else ModeAssessment(mode,Availability.AVAILABLE,Requirement.READY)
            SilenceMode.AUDIO_GROUP -> if(!granted) ModeAssessment(mode,Availability.NEEDS_PERMISSION,Requirement.SHIZUKU)
                else if(group==null) ModeAssessment(mode,Availability.UNSUPPORTED,Requirement.AUDIO_POLICY)
                else ModeAssessment(mode,Availability.EXPERIMENTAL,Requirement.READY)
            SilenceMode.APP_AUDIO -> if(!granted) ModeAssessment(mode,Availability.NEEDS_PERMISSION,Requirement.SHIZUKU)
                else if(cameras.isEmpty()) ModeAssessment(mode,Availability.UNSUPPORTED,Requirement.CAMERA_PERMISSION)
                else ModeAssessment(mode,Availability.EXPERIMENTAL,Requirement.EXPERIMENTAL_AUDIO)
            SilenceMode.ROOT_PROPERTY -> if(!rootAvailable) ModeAssessment(mode,Availability.NEEDS_PERMISSION,Requirement.ROOT)
                else if(props.isEmpty())ModeAssessment(mode,Availability.UNSUPPORTED,Requirement.ROOT)
                else ModeAssessment(mode,Availability.EXPERIMENTAL,Requirement.ROOT_READY)
            SilenceMode.MAGISK -> ModeAssessment(mode,Availability.EXPERIMENTAL,Requirement.MODULE)
            SilenceMode.CAMERA -> ModeAssessment(mode,Availability.AVAILABLE,Requirement.CAMERA_PERMISSION)
        }}
        return DeviceReport(Build.MANUFACTURER,Build.MODEL,Build.VERSION.RELEASE,Build.VERSION.SDK_INT,identity,cameras,assessments,
            running,granted,rootAvailable,csc,group,props,commandLog.toList(),backups.keys())
    }

    private fun readAudioGroup():AudioGroup {
        val help=reportExecutor.execute(listOf("cmd","audio","help")).requireOutput()
        check(help.contains("adj-group-volume")){"Volume group CLI unsupported"}
        val mappings=JSONArray(shell.audioGroups())
        val candidates=(0 until mappings.length()).map{mappings.getJSONObject(it)}.filter {
            it.optString("name")=="AUDIO_STREAM_ENFORCED_AUDIBLE" && it.optJSONArray("streams")?.let { s ->s.length()==1 && s.optInt(0)==7 }==true
        }
        check(candidates.size==1){"No isolated enforced-audible group mapping"}
        val id=candidates.single().getInt("id");check(id>=0)
        val dump=reportExecutor.execute(listOf("dumpsys","audio")).requireOutput()
        check(ModePolicy.isolatedEnforcedStream(dump)){"Enforced stream is aliased, shared or its isolation is unproven"}
        val header="- VOLUME GROUP AUDIO_STREAM_ENFORCED_AUDIBLE:"
        check(dump.split(header).size==2){"Audio group state is ambiguous"}
        val injected=dump.replace(header,"$header\n   Id: $id")
        val group=AudioGroupParser.parse(injected) ?: error("Cannot restore group state exactly (missing or differing route volumes)")
        check(Regex("(?m)^\\s*Min:\\s*0\\s*$").containsMatchIn(injected.substringAfter(header).substringBefore("- VOLUME GROUP"))) {"Group minimum is not zero"}
        return group
    }

    fun apply(mode:SilenceMode,pkg:String?):OperationResult = operation(mode,pkg,false)
    fun restore(mode:SilenceMode,pkg:String?):OperationResult = operation(mode,pkg,true)
    private fun operation(mode:SilenceMode,pkg:String?,restore:Boolean):OperationResult = try {
        commandLog.clear()
        val key=backupKey(mode,pkg)
        val target:StateTarget
        val desired:String?
        when(mode) {
            SilenceMode.CSC -> {check(ModePolicy.supportsCsc(Build.MANUFACTURER)){"Samsung-only setting"};target=CscTarget(reportExecutor,identity.userId);desired="0"}
            SilenceMode.APP_AUDIO -> {
                check(pkg!=null && ModePolicy.validPackage(pkg) && detectCameraPackages().contains(pkg)){"Select an installed camera app"}
                target=AppAudioTarget(reportExecutor,identity.userId,pkg);desired="ignore"
            }
            SilenceMode.AUDIO_GROUP -> {
                val t=AudioGroupTarget(reportExecutor,::readAudioGroup);target=t;desired=if(restore)null else t.mutedState(readAudioGroup())
            }
            SilenceMode.ROOT_PROPERTY -> {
                check(rootAvailable){"Root authorization required"}
                val keys=if(restore) backups.get(key)?.value?.lines()?.map{it.substringBefore('=')} ?: emptyList()
                    else CommandPolicy.properties.filter{root.execute(listOf("getprop",it)).let{r ->r.isSuccess && r.stdout.trim() in setOf("0","1","true","false")}}.sorted()
                check(keys.isNotEmpty() && keys.all{it in CommandPolicy.properties}){"No supported root properties detected"}
                target=PropertyTarget(root,keys);desired=keys.joinToString("\n"){"$it=${if(it=="audio.camerasound.force")"false" else "0"}"}
            }
            else -> error("This mode uses its dedicated camera/module screen")
        }
        val tx=SettingTransaction(key,identity,backups,target)
        if(restore)tx.restore() else tx.apply(desired)
    }catch(e:Exception){OperationResult(Outcome.FAILED,e.message.orEmpty())}

    @Suppress("DEPRECATION")
    private fun detectCameraPackages():Set<String> = context.packageManager
        .queryIntentActivities(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE),PackageManager.MATCH_DEFAULT_ONLY)
        .map{it.activityInfo.packageName}.toSet()

    fun backupKey(mode:SilenceMode,pkg:String?):String="${mode.name}:${identity.userId}${if(mode==SilenceMode.APP_AUDIO)":${pkg.orEmpty()}" else ""}"
    fun recentCommands()=commandLog.toList()
    fun close()=shell.close()
}
