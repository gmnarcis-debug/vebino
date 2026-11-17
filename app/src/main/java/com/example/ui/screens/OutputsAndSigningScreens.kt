package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import red.line.vebino.R
import com.example.core.JalaliCalendar
import com.example.core.ValidationUtils
import com.example.core.appString
import com.example.data.local.db.BuildResultEntity
import com.example.data.local.db.SigningKeyMetadataEntity
import com.example.data.local.prefs.UserPreferences
import com.example.domain.model.AppLanguage
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.CalendarType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.Webino3DGlassIcon
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.WebinoScreenHeader
import com.example.ui.components.localizeNumber
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintGreen
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WebinoThemeTokens
import java.io.File

@Composable
fun OutputsScreen(
    prefs: UserPreferences,
    outputs: List<BuildResultEntity>,
    onVerifyOutput: (Long) -> Unit,
    onDeleteOutput: (Long) -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val isFa = prefs.language == AppLanguage.FA
    val tokens = WebinoThemeTokens.colors
    var pendingSaveFile by remember { mutableStateOf<BuildResultEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<BuildResultEntity?>(null) }

    val safSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destUri ->
        val item = pendingSaveFile
        if (destUri != null && item != null) {
            runCatching {
                val src = File(item.filePath)
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    src.inputStream().use { it.copyTo(out) }
                }
            }.onSuccess {
                onNotify("فایل در مسیر انتخابی ذخیره شد.")
            }.onFailure {
                onNotify("خطا در ذخیره فایل در حافظه دستگاه.")
            }
        }
        pendingSaveFile = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Webino3DGlassIcon(type = Webino3DIconType.OUTPUTS, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = appString(R.string.outputs_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isFa) "فایل‌های امضاشده و آماده نصب" else "Signed builds ready for distribution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (outputs.isNotEmpty()) {
                    StatusBadge(
                        text = if (isFa) "${localizeNumber(outputs.size, prefs.language, prefs.usePersianDigits)} خروجی" else "${outputs.size} files",
                        color = MintGreen,
                    )
                }
            }
        }

        if (outputs.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Android,
                    icon3DType = Webino3DIconType.OUTPUTS,
                    title = appString(R.string.outputs_empty_title),
                    subtitle = appString(R.string.outputs_empty_sub),
                )
            }
        } else {
            items(outputs, key = { it.id }) { item ->
                val isApk = item.format != BuildOutputFormat.AAB.name
                GlassCard(
                    borderColor = if (item.isVerified) SuccessEmerald.copy(alpha = 0.35f) else WarningAmber.copy(alpha = 0.35f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Webino3DGlassIcon(
                                type = if (isApk) Webino3DIconType.OUTPUTS else Webino3DIconType.BUILD,
                                size = 42.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.appName} • ${item.format}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${item.packageName} • v${item.versionName}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        textDirection = TextDirection.Ltr,
                                    ),
                                    color = SkyBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(
                            text = if (item.isVerified) "امضا و تأییدشده" else "نیاز به بررسی",
                            color = if (item.isVerified) SuccessEmerald else WarningAmber,
                            icon = Icons.Rounded.Verified,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "${JalaliCalendar.formatDateTime(item.createdAt, prefs.calendarType == CalendarType.JALALI, isFa, prefs.usePersianDigits)} • ${ValidationUtils.formatFileSize(item.fileSizeBytes, isFa, prefs.usePersianDigits)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.validationSummary.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.validationSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessEmerald,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = tokens.subtleSurface,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (isApk) {
                                IconButton(onClick = { installOrOpenOutput(context, item.filePath, onNotify) }) {
                                    Icon(Icons.Rounded.InstallMobile, contentDescription = "نصب APK", tint = SuccessEmerald)
                                }
                            }
                            IconButton(onClick = { shareOutput(context, item.filePath, item.appName, onNotify) }) {
                                Icon(Icons.Rounded.Share, contentDescription = "اشتراک‌گذاری", tint = SkyBlue)
                            }
                            IconButton(
                                onClick = {
                                    pendingSaveFile = item
                                    val ext = if (isApk) "apk" else "aab"
                                    safSaveLauncher.launch("${item.packageName}_v${item.versionName}.$ext")
                                }
                            ) {
                                Icon(Icons.Rounded.SaveAlt, contentDescription = "ذخیره با SAF", tint = ElectricViolet)
                            }
                            IconButton(onClick = { onVerifyOutput(item.id) }) {
                                Icon(Icons.Rounded.VerifiedUser, contentDescription = "بررسی امضا")
                            }
                            IconButton(onClick = { deleteCandidate = item }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = ErrorCoral)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    deleteCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("حذف فایل خروجی") },
            text = { Text("فایل ${target.appName} (${target.format}) حذف شود؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteOutput(target.id)
                        deleteCandidate = null
                    }
                ) { Text("حذف", color = ErrorCoral) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("انصراف") }
            },
        )
    }
}

private fun installOrOpenOutput(context: Context, filePath: String, onNotify: (String) -> Unit) {
    val file = File(filePath)
    if (!file.exists()) {
        onNotify("فایل خروجی یافت نشد.")
        return
    }
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        onNotify("امکان اجرای مستقیم نصبکننده در این محیط وجود ندارد؛ از دکمه ذخیره یا اشتراکگذاری استفاده کنید.")
    }
}

private fun shareOutput(context: Context, filePath: String, appName: String, onNotify: (String) -> Unit) {
    val file = File(filePath)
    if (!file.exists()) {
        onNotify("فایل یافت نشد.")
        return
    }
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, appName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, appName))
    }.onFailure {
        onNotify("خطا در اشتراکگذاری فایل.")
    }
}

@Composable
fun SigningManagerScreen(
    keys: List<SigningKeyMetadataEntity>,
    onBack: () -> Unit,
    onCreateKey: (alias: String, password: String, cn: String, org: String, country: String, years: Int) -> Unit,
    onImportKey: (Uri, String) -> Unit,
    onSetDefault: (Long) -> Unit,
    onChangePassword: (Long, String) -> Unit,
    onVerifyKey: (Long) -> Unit,
    onDeleteKey: (Long) -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val tokens = WebinoThemeTokens.colors
    var showCreateDialog by remember { mutableStateOf(false) }
    var importUriPending by remember { mutableStateOf<Uri?>(null) }
    var passwordChangeKey by remember { mutableStateOf<SigningKeyMetadataEntity?>(null) }
    var backupKeyPending by remember { mutableStateOf<SigningKeyMetadataEntity?>(null) }
    var deleteKeyCandidate by remember { mutableStateOf<SigningKeyMetadataEntity?>(null) }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importUriPending = uri
    }

    val backupSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-pkcs12")) { destUri ->
        val key = backupKeyPending
        if (destUri != null && key != null) {
            runCatching {
                val src = File(key.keystorePath)
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    src.inputStream().use { it.copyTo(out) }
                }
            }.onSuccess { onNotify("پشتیبان رمزنگاریشده Keystore ذخیره شد.") }
                .onFailure { onNotify("خطا در تهیه نسخه پشتیبان.") }
        }
        backupKeyPending = null
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            WebinoScreenHeader(
                title = "مدیریت امضای دیجیتال و Keystore",
                subtitle = "کلیدهای امضای PKCS12 / RSA-2048",
                iconType = Webino3DIconType.SIGNING,
                onBack = onBack,
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GlassCard(borderColor = WarningAmber.copy(alpha = 0.45f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = WarningAmber)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "الزام امضای سازگار برای بروزرسانی اپلیکیشن",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "سیستم‌عامل اندروید و فروشگاه‌هایی مثل مایکت، کافه‌بازار و گوگل‌پلی تنها زمانی اجازه نصب یا انتشار آپدیت نسخه جدید را می‌دهند که با همان کلید قبلی (گواهی یکسان) امضا شده باشد. از کلید خود حتماً نسخه پشتیبان تهیه کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.45f)),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = ElectricViolet)
                        Spacer(Modifier.width(6.dp))
                        Text("ساخت کلید جدید")
                    }
                    OutlinedButton(
                        onClick = { importPicker.launch(arrayOf("*/*")) },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SkyBlue.copy(alpha = 0.45f)),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = SkyBlue)
                        Spacer(Modifier.width(6.dp))
                        Text("وارد کردن Keystore")
                    }
                }
            }

            items(keys, key = { it.id }) { key ->
                GlassCard(borderColor = if (key.isDefault) SkyBlue.copy(alpha = 0.6f) else tokens.navPillBorder) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Webino3DGlassIcon(type = Webino3DIconType.SIGNING, size = 38.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(key.alias, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${key.commonName} (${key.keystoreType})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (key.isDefault) {
                            StatusBadge(text = "کلید پیش‌فرض", color = SuccessEmerald)
                        } else {
                            TextButton(onClick = { onSetDefault(key.id) }) { Text("انتخاب پیش‌فرض") }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(tokens.subtleSurface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "SHA-256: ${key.sha256Fingerprint}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                textDirection = TextDirection.Ltr,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = { onVerifyKey(key.id) }) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = "بررسی اعتبار امضا", tint = SuccessEmerald)
                        }
                        IconButton(onClick = { passwordChangeKey = key }) {
                            Icon(Icons.Rounded.LockReset, contentDescription = "تغییر رمز")
                        }
                        IconButton(
                            onClick = {
                                backupKeyPending = key
                                backupSaver.launch("${key.alias}_backup.p12")
                            }
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = "پشتیبان‌گیری امن", tint = CyanAccent)
                        }
                        if (!key.isDefault) {
                            IconButton(onClick = { deleteKeyCandidate = key }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = ErrorCoral)
                            }
                        }
                    }
                }
            }
        }
    }

    deleteKeyCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteKeyCandidate = null },
            title = { Text("تأیید حذف کلید امضا") },
            text = {
                Text(
                    "آیا از حذف کلید «${target.alias}» اطمینان دارید؟ در صورت نداشتن نسخه پشتیبان، دیگر امکان امضای آپدیت برنامه‌های قبلی با این کلید وجود نخواهد داشت."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteKey(target.id)
                        deleteKeyCandidate = null
                    }
                ) { Text("حذف نهایی", color = ErrorCoral) }
            },
            dismissButton = {
                TextButton(onClick = { deleteKeyCandidate = null }) { Text("انصراف") }
            },
        )
    }

    if (showCreateDialog) {
        var alias by remember { mutableStateOf("my_release_key") }
        var password by remember { mutableStateOf("") }
        var cn by remember { mutableStateOf("My Company") }
        var org by remember { mutableStateOf("Webino Apps") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("ایجاد کلید امضای RSA 2048") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = alias, onValueChange = { alias = it }, label = { Text("نام مستعار (Alias)") }, singleLine = true)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("رمز عبور Keystore (حداقل ۶ کاراکتر)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                    OutlinedTextField(value = cn, onValueChange = { cn = it }, label = { Text("نام صادرکننده (CN)") }, singleLine = true)
                    OutlinedTextField(value = org, onValueChange = { org = it }, label = { Text("سازمان (O)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (password.length >= 6) {
                            onCreateKey(alias, password, cn, org, "IR", 25)
                            showCreateDialog = false
                        } else {
                            onNotify("رمز باید حداقل ۶ کاراکتر باشد.")
                        }
                    }
                ) { Text("ایجاد کلید") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("لغو") } },
        )
    }

    importUriPending?.let { uri ->
        var pwd by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { importUriPending = null },
            title = { Text("رمز Keystore را وارد کنید") },
            text = {
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text("رمز فایل Keystore") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onImportKey(uri, pwd)
                        importUriPending = null
                    }
                ) { Text("وارد کردن") }
            },
            dismissButton = { TextButton(onClick = { importUriPending = null }) { Text("لغو") } },
        )
    }

    passwordChangeKey?.let { key ->
        var newPwd by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { passwordChangeKey = null },
            title = { Text("تغییر رمز کلید ${key.alias}") },
            text = {
                OutlinedTextField(
                    value = newPwd,
                    onValueChange = { newPwd = it },
                    label = { Text("رمز جدید (حداقل ۶ کاراکتر)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPwd.length >= 6) {
                            onChangePassword(key.id, newPwd)
                            passwordChangeKey = null
                        } else {
                            onNotify("رمز جدید باید حداقل ۶ کاراکتر باشد.")
                        }
                    }
                ) { Text("ذخیره رمز") }
            },
            dismissButton = { TextButton(onClick = { passwordChangeKey = null }) { Text("لغو") } },
        )
    }
}
