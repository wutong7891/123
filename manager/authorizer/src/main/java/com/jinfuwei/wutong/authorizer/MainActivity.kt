package com.jinfuwei.wutong.authorizer

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AuthorizerScreen() }
    }
}

@Composable
private fun AuthorizerScreen() {
    val context = LocalContext.current
    var deviceIdInput by remember { mutableStateOf("") }
    var activationCode by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Wutong 设备授权器",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text("离线签发与单台设备绑定的激活码。请勿向任何人分发本授权器 APK。")
                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "安全提示：授权私钥位于此 APK 内，持有本 APK 的人可以签发任意设备激活码。请仅在受控手机上安装。",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = deviceIdInput,
                    onValueChange = {
                        deviceIdInput = it
                        activationCode = ""
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("目标设备 ID") },
                    minLines = 2,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = deviceIdInput.isNotBlank(),
                    onClick = {
                        val normalized = normalizeDeviceId(deviceIdInput)
                        if (normalized == null) {
                            message = "设备 ID 格式错误"
                            activationCode = ""
                        } else {
                            activationCode = runCatching { sign(normalized) }.getOrElse {
                                message = "签发失败：授权密钥不可用"
                                ""
                            }
                            if (activationCode.isNotBlank()) message = "激活码已生成"
                        }
                    },
                ) {
                    Text("生成设备激活码")
                }
                if (activationCode.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = activationCode,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        label = { Text("专属激活码") },
                        minLines = 4,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("Wutong Activation Code", activationCode),
                            )
                            message = "激活码已复制"
                        },
                    ) {
                        Text("复制激活码")
                    }
                }
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(message, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun normalizeDeviceId(raw: String): String? {
    var compact = raw.trim().uppercase().filterNot { it == '-' || it.isWhitespace() }
    if (compact.startsWith("WT")) compact = compact.removePrefix("WT")
    if (compact.length != 32 || compact.any { it !in '0'..'9' && it !in 'A'..'F' }) return null
    return "WT-" + compact.chunked(4).joinToString("-")
}

private fun sign(deviceId: String): String {
    check(BuildConfig.LICENSE_PRIVATE_KEY_B64.isNotBlank())
    val privateKey = KeyFactory.getInstance("EC").generatePrivate(
        PKCS8EncodedKeySpec(Base64.decode(BuildConfig.LICENSE_PRIVATE_KEY_B64, Base64.DEFAULT)),
    )
    val signature = Signature.getInstance("SHA256withECDSA").run {
        initSign(privateKey)
        update("WUTONG-ACTIVATION-V1|$deviceId".toByteArray(StandardCharsets.UTF_8))
        sign()
    }
    return "WT1." + Base64.encodeToString(
        signature,
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
}
