package me.weishu.kernelsu.license

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.provider.Settings
import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import me.weishu.kernelsu.BuildConfig

class DeviceActivation(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    val deviceId: String by lazy {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty()
        val digest = MessageDigest.getInstance("SHA-256").digest(
            "$DEVICE_DOMAIN|$androidId".toByteArray(StandardCharsets.UTF_8),
        )
        "WT-" + digest.take(16).joinToString("") { "%02X".format(it) }
            .chunked(4)
            .joinToString("-")
    }

    fun isActivated(): Boolean = verify(preferences.getString(ACTIVATION_CODE, null).orEmpty())

    fun activate(code: String): Boolean {
        if (!verify(code)) return false
        preferences.edit().putString(ACTIVATION_CODE, code.trim()).apply()
        return true
    }

    private fun verify(code: String): Boolean = runCatching {
        val compact = code.filterNot(Char::isWhitespace)
        if (!compact.startsWith(CODE_PREFIX)) return false
        if (BuildConfig.LICENSE_PUBLIC_KEY_B64.isBlank()) return false

        val signatureBytes = Base64.decode(
            compact.removePrefix(CODE_PREFIX),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        val publicKey = KeyFactory.getInstance("EC").generatePublic(
            X509EncodedKeySpec(Base64.decode(BuildConfig.LICENSE_PUBLIC_KEY_B64, Base64.DEFAULT)),
        )
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey)
            update(payload(deviceId))
            verify(signatureBytes)
        }
    }.getOrDefault(false)

    companion object {
        private const val DEVICE_DOMAIN = "com.jinfuwei.wutong"
        private const val PAYLOAD_PREFIX = "WUTONG-ACTIVATION-V1|"
        private const val CODE_PREFIX = "WT1."
        private const val PREFERENCES = "wutong_device_activation"
        private const val ACTIVATION_CODE = "activation_code"

        private fun payload(deviceId: String) =
            "$PAYLOAD_PREFIX$deviceId".toByteArray(StandardCharsets.UTF_8)
    }
}

@Composable
fun ActivationScreen(
    deviceId: String,
    onActivate: (String) -> Boolean,
) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
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
                    text = "wutong 设备激活",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text("此设备尚未授权。复制设备 ID 到 Wutong 设备授权器中生成专属激活码。")
                Spacer(Modifier.height(20.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("本机设备 ID", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = deviceId,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard.setPrimaryClip(ClipData.newPlainText("Wutong Device ID", deviceId))
                                message = "设备 ID 已复制"
                            },
                        ) {
                            Text("复制设备 ID")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; message = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("设备激活码") },
                    minLines = 3,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = code.isNotBlank(),
                    onClick = {
                        message = if (onActivate(code)) {
                            "激活成功，正在进入管理器…"
                        } else {
                            "激活码无效或不属于此设备"
                        }
                    },
                ) {
                    Text("验证并激活")
                }
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(message, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
