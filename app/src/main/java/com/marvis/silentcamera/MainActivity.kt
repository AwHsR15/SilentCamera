package com.marvis.silentcamera

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            viewModel.refreshShizukuState()
            Toast.makeText(
                this,
                if (granted) "Shizuku 权限已获取" else "Shizuku 权限被拒绝",
                Toast.LENGTH_SHORT
            ).show()
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        viewModel.refreshShizukuState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        viewModel.refreshShizukuState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)

        setContent {
            SilentCameraTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestPermission = ::requestShizukuPermission,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshShizukuState()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    }

    private fun requestShizukuPermission() {
        if (!ShizukuShell.isAvailable()) {
            Toast.makeText(this, "Shizuku 未运行，请先启动 Shizuku", Toast.LENGTH_SHORT).show()
            viewModel.refreshShizukuState()
            return
        }
        if (ShizukuShell.isGranted()) {
            Toast.makeText(this, "已拥有 Shizuku 权限", Toast.LENGTH_SHORT).show()
            viewModel.refreshShizukuState()
            return
        }
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(this, "请在 Shizuku 中手动授予权限", Toast.LENGTH_LONG).show()
            return
        }
        Shizuku.requestPermission(0)
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  UI
// ──────────────────────────────────────────────────────────────────────────

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onRequestPermission: () -> Unit,
) {
    val isBusy by viewModel.isBusy
    val running by viewModel.shizukuRunning
    val granted by viewModel.shizukuGranted
    val shutter by viewModel.shutterState
    val logs = viewModel.logs

    Scaffold(
        containerColor = AppColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            HeaderSection()

            Spacer(Modifier.height(16.dp))

            if (isBusy) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape),
                    color = AppColors.Primary,
                    trackColor = AppColors.SurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }

            ShizukuStatusCard(running = running, granted = granted)

            Spacer(Modifier.height(12.dp))

            ShutterStatusCard(state = shutter, enabled = running && granted && !isBusy)

            Spacer(Modifier.height(16.dp))

            ActionSection(
                isBusy = isBusy,
                granted = granted,
                onSilence = viewModel::executeSilence,
                onCheck = viewModel::checkStatus,
                onRequestPermission = onRequestPermission,
            )

            Spacer(Modifier.height(16.dp))

            LogConsole(
                logs = logs,
                onClear = viewModel::clearLog,
                onCopy = { context ->
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("SilentCamera 日志", viewModel.logText()))
                    Toast.makeText(context, "日志已复制", Toast.LENGTH_SHORT).show()
                },
            )

            Spacer(Modifier.height(12.dp))

            FooterHint()
        }
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AppColors.Primary, AppColors.PrimaryDark)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.NoPhotography,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                "SilentCamera",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface,
            )
            Text(
                "三星日版 · 快门静音工具",
                fontSize = 13.sp,
                color = AppColors.OnSurfaceMuted,
            )
        }
    }
}

@Composable
private fun ShizukuStatusCard(running: Boolean, granted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusPill(
                modifier = Modifier.weight(1f),
                label = "Shizuku 服务",
                value = if (running) "运行中" else "未运行",
                color = if (running) AppColors.Success else AppColors.Error,
                icon = if (running) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            )
            StatusPill(
                modifier = Modifier.weight(1f),
                label = "授权状态",
                value = if (granted) "已授权" else "未授权",
                color = if (granted) AppColors.Success else AppColors.Warning,
                icon = if (granted) Icons.Filled.VerifiedUser else Icons.Filled.GppMaybe,
            )
        }
    }
}

@Composable
private fun StatusPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
) {
    val animColor by animateColorAsState(color, label = "pillColor")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SurfaceVariant)
            .padding(vertical = 12.dp, horizontal = 14.dp),
    ) {
        Text(label, fontSize = 11.sp, color = AppColors.OnSurfaceMuted)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = animColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = animColor)
        }
    }
}

@Composable
private fun ShutterStatusCard(state: ShutterState, enabled: Boolean) {
    val (label, desc, color, icon) = when (state) {
        ShutterState.SILENCED -> Quad("已静音", "强制快门音已关闭", AppColors.Success, Icons.AutoMirrored.Filled.VolumeOff)
        ShutterState.FORCED -> Quad("强制响铃", "日版强制快门音开启中", AppColors.Warning, Icons.AutoMirrored.Filled.VolumeUp)
        ShutterState.UNSET -> Quad("未设置", "未写入该项（通常视为已关闭）", AppColors.Info, Icons.AutoMirrored.Filled.HelpOutline)
        ShutterState.UNKNOWN -> Quad(
            if (enabled) "未知" else "待检测",
            if (enabled) "点击「状态检查」获取当前状态" else "请先启动并授权 Shizuku",
            AppColors.OnSurfaceMuted, Icons.Filled.Sync,
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("当前快门状态", fontSize = 11.sp, color = AppColors.OnSurfaceMuted)
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                Text(desc, fontSize = 12.sp, color = AppColors.OnSurfaceMuted)
            }
        }
    }
}

@Composable
private fun ActionSection(
    isBusy: Boolean,
    granted: Boolean,
    onSilence: () -> Unit,
    onCheck: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Button(
        onClick = onSilence,
        enabled = !isBusy,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Primary,
            contentColor = Color(0xFF0E1A05),
        ),
    ) {
        Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("一键静音", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }

    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onCheck,
            enabled = !isBusy,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.Troubleshoot, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("状态检查")
        }
        OutlinedButton(
            onClick = onRequestPermission,
            enabled = !isBusy,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (granted) "已授权" else "请求权限")
        }
    }
}

@Composable
private fun LogConsole(
    logs: List<LogLine>,
    onClear: () -> Unit,
    onCopy: (Context) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 新日志自动滚动到底部
    LaunchedEffect(logs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0F0D)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Terminal,
                    contentDescription = null,
                    tint = AppColors.OnSurfaceMuted,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "运行日志",
                    fontSize = 13.sp,
                    color = AppColors.OnSurfaceMuted,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onCopy(context) }, enabled = logs.isNotEmpty()) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "复制",
                        tint = AppColors.OnSurfaceMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onClear, enabled = logs.isNotEmpty()) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "清空",
                        tint = AppColors.OnSurfaceMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            HorizontalDivider(color = AppColors.SurfaceVariant)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 320.dp)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
            ) {
                if (logs.isEmpty()) {
                    Text(
                        "等待操作…",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = AppColors.OnSurfaceMuted,
                    )
                } else {
                    logs.forEach { line ->
                        Text(
                            text = line.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = if (line.level == LogLevel.HEADER || line.level == LogLevel.STEP)
                                FontWeight.Bold else FontWeight.Normal,
                            color = colorFor(line.level),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterHint() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = AppColors.Warning,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "小贴士：系统大版本 OTA 更新后，该设置可能被重置，需要重新执行一次。",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = AppColors.OnSurfaceMuted,
        )
    }
}

private fun colorFor(level: LogLevel): Color = when (level) {
    LogLevel.HEADER -> AppColors.Primary
    LogLevel.STEP -> AppColors.Secondary
    LogLevel.SUCCESS -> AppColors.Success
    LogLevel.WARN -> AppColors.Warning
    LogLevel.ERROR -> AppColors.Error
    LogLevel.INFO -> AppColors.OnSurface.copy(alpha = 0.85f)
}

/** 简单四元组，便于在 when 中解构。 */
private data class Quad(
    val label: String,
    val desc: String,
    val color: Color,
    val icon: ImageVector,
)
