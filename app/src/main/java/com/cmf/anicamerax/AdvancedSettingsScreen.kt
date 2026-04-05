package com.cmf.anicamerax

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Все параметры LMC с ключами из flame_lmc8.4r18_mediatek_v1.xml
    val lmcParams = remember {
        mutableStateListOf(
            LmcParam("Sharpness", 20f, 0f..50f, "libsabresharp"),
            LmcParam("Saturation", 30f, 0f..50f, "libsaturation"),
            LmcParam("Contrast", 40f, 0f..50f, "libcontrast"),
            LmcParam("Denoise", 25f, 0f..50f, "libdenoise"),
            LmcParam("HDR Boost", 35f, 0f..50f, "libhdr2"),
            LmcParam("Gain Large", 30f, 0f..50f, "libgainlarge"),
            LmcParam("Sabre Contrast", 40f, 0f..50f, "libsabrecontrast"),
            LmcParam("Highlight", 30f, 0f..50f, "libhightlight2"),
            LmcParam("Dehaze", 20f, 0f..50f, "libdehazedexpo"),
            LmcParam("Gamma Curve", 3f, 1f..5f, "prefgammacurvepreset"),
            LmcParam("Smoothing", 25f, 0f..50f, "libsmoothingnew")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LMC 8.4r18 MediaTek") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Параметры из flame_lmc8.4r18_mediatek_v1.xml",
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 14.sp,
                color = androidx.compose.ui.graphics.Color.Gray
            )

            lmcParams.forEach { param ->
                LmcSlider(param)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    exportToXml(context, lmcParams)
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            ) {
                Text("Экспорт в XML")
            }
        }
    }
}

data class LmcParam(
    val name: String,
    var value: Float,
    val range: ClosedFloatingPointRange<Float>,
    val xmlKey: String
)

@Composable
private fun LmcSlider(param: LmcParam) {
    var value by remember(param.xmlKey) { mutableFloatStateOf(param.value) }
    param.value = value // синхронизация

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(param.name, fontSize = 16.sp)
            Text(value.toInt().toString(), fontSize = 16.sp)
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            valueRange = param.range,
            steps = if (param.range.endInclusive <= 5) 4 else 49
        )
        Text(
            "XML: ${param.xmlKey}=${"%.1f".format(value)}",
            fontSize = 12.sp,
            color = androidx.compose.ui.graphics.Color.Gray,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// Экспорт параметров в XML (упрощённый формат)
private fun exportToXml(context: Context, params: List<LmcParam>) {
    val xmlContent = buildString {
        append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        append("<flame_lmc84r18_mediatek>\n")
        params.forEach { param ->
            append("    <${param.xmlKey}>${"%.1f".format(param.value)}</${param.xmlKey}>\n")
        }
        append("</flame_lmc84r18_mediatek>")
    }

    try {
        val file = File(context.getExternalFilesDir(null), "flame_lmc8.4r18_mediatek_v1.xml")
        file.writeText(xmlContent)
        Toast.makeText(context, "✅ XML сохранён: ${file.path}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "❌ Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}