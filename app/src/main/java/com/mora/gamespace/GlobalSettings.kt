package com.mora.gamespace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ACCENT = Color(0xFFFF5A4A)

/**
 * Global settings opened from the gear in the top corner of the lobby.
 * Mirrors the original Mora web UI's power + color settings, restyled to the GameSpace look.
 * All values are persisted via [MoraLeds] / [MoraCooler] (prefs + best-effort system props).
 */
@Composable
internal fun GlobalSettingsSheet(onClose: () -> Unit) {
    val context = LocalContext.current
    var fanCooler by remember { mutableStateOf(MoraCooler.isOn(context)) }
    var saver by remember { mutableStateOf(MoraLeds.saverOn(context)) }
    var charging by remember { mutableStateOf(MoraLeds.charging(context)) }
    var notif by remember { mutableStateOf(MoraLeds.notif(context)) }
    var normal by remember { mutableStateOf(MoraLeds.normal(context)) }
    var gaming by remember { mutableStateOf(MoraLeds.gaming(context)) }

    Box(Modifier.fillMaxSize().background(Color(0xFF050608))) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Настройки", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Готово",
                    color = ACCENT,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onClose() },
                )
            }
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                GsSection("ПИТАНИЕ") {
                    GsSwitchRow("Кулер (вентилятор)", fanCooler) {
                        fanCooler = it
                        MoraCooler.setOn(context, it)
                    }
                    GsSwitchRow("Энергосбережение", saver) {
                        saver = it
                        MoraLeds.setSaver(context, it)
                    }
                }

                GsSection("ЗАРЯДКА — ПОДСВЕТКА") {
                    GsSwitchRow("Включить при зарядке", charging.enabled) {
                        charging = charging.copy(enabled = it)
                        MoraLeds.setCharging(context, charging)
                    }
                    FanPicker("Вентилятор", charging.fan) {
                        charging = charging.copy(fan = it)
                        MoraLeds.setCharging(context, charging)
                    }
                    ExtPicker("Внешняя подсветка", charging.ext, allowNone = true) {
                        charging = charging.copy(ext = it)
                        MoraLeds.setCharging(context, charging)
                    }
                }

                GsSection("УВЕДОМЛЕНИЯ — ПОДСВЕТКА") {
                    GsSwitchRow("Включить", notif.enabled) {
                        notif = notif.copy(enabled = it)
                        MoraLeds.setNotif(context, notif)
                    }
                    GsPillRow("Остановка", listOf(0 to "До экрана", 1 to "По времени"), if (notif.forSeconds) 1 else 0) {
                        notif = notif.copy(forSeconds = it == 1)
                        MoraLeds.setNotif(context, notif)
                    }
                    if (notif.forSeconds) {
                        GsPillRow("Секунды", listOf(5 to "5с", 10 to "10с", 20 to "20с", 30 to "30с", 60 to "60с"), notif.seconds) {
                            notif = notif.copy(seconds = it)
                            MoraLeds.setNotif(context, notif)
                        }
                    }
                    ExtPickerRequired("Режим и цвет", notif.ext) {
                        notif = notif.copy(ext = it)
                        MoraLeds.setNotif(context, notif)
                    }
                }

                GsSection("РЕЖИМ NORMAL") {
                    FanPicker("Вентилятор", normal.fan) {
                        normal = normal.copy(fan = it)
                        MoraLeds.setNormal(context, normal)
                    }
                    ExtPicker("Внешняя подсветка", normal.ext, allowNone = true) {
                        normal = normal.copy(ext = it)
                        MoraLeds.setNormal(context, normal)
                    }
                }

                GsSection("РЕЖИМ GAMING") {
                    FanPicker("Вентилятор", gaming.fan) {
                        gaming = gaming.copy(fan = it)
                        MoraLeds.setGaming(context, gaming)
                    }
                    ExtPicker("Внешняя подсветка", gaming.ext, allowNone = true) {
                        gaming = gaming.copy(ext = it)
                        MoraLeds.setGaming(context, gaming)
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun GsSection(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(18.dp))
    Text(title, color = ACCENT.copy(alpha = .9f), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF181B22), Color(0xFF0E1014))))
            .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun GsSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White.copy(alpha = .9f), fontSize = 15.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFC22A30),
                checkedBorderColor = ACCENT,
            ),
        )
    }
}

@Composable
private fun GsPill(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) Color(0xFF1E2230) else Color(0xFF14171E))
            .border(1.dp, if (active) ACCENT else Color.White.copy(alpha = .12f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = if (active) Color.White else Color.White.copy(alpha = .65f), fontSize = 13.sp)
    }
}

@Composable
private fun GsPillRow(label: String, options: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    Spacer(Modifier.height(8.dp))
    Text(label, color = Color.White.copy(alpha = .55f), fontSize = 12.sp)
    Spacer(Modifier.height(6.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (code, name) ->
            GsPill(name, selected == code) { onSelect(code) }
        }
    }
}

@Composable
private fun FanPicker(label: String, fan: MoraLeds.Fan?, onChange: (MoraLeds.Fan?) -> Unit) {
    Spacer(Modifier.height(10.dp))
    Text(label, color = Color.White.copy(alpha = .75f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GsPill("Выкл", fan == null) { onChange(null) }
        MoraLeds.FAN_MODES.forEach { (code, name) ->
            GsPill(name, fan?.mode == code) { onChange(MoraLeds.Fan(code, fan?.color ?: 13)) }
        }
    }
    if (fan != null) {
        GsPillRow("Цвет", MoraLeds.FAN_COLORS, fan.color) { c -> onChange(MoraLeds.Fan(fan.mode, c)) }
    }
}

@Composable
private fun ExtPicker(label: String, ext: MoraLeds.Ext?, allowNone: Boolean, onChange: (MoraLeds.Ext?) -> Unit) {
    Spacer(Modifier.height(10.dp))
    Text(label, color = Color.White.copy(alpha = .75f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (allowNone) GsPill("Выкл", ext == null) { onChange(null) }
        MoraLeds.EXT_MODES.forEach { (code, name) ->
            GsPill(name, ext?.mode == code) { onChange(MoraLeds.Ext(code, ext?.color ?: 6)) }
        }
    }
    if (ext != null) {
        GsPillRow("Цвет", MoraLeds.EXT_COLORS, ext.color) { c -> onChange(MoraLeds.Ext(ext.mode, c)) }
    }
}

@Composable
private fun ExtPickerRequired(label: String, ext: MoraLeds.Ext, onChange: (MoraLeds.Ext) -> Unit) {
    Spacer(Modifier.height(10.dp))
    Text(label, color = Color.White.copy(alpha = .75f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    GsPillRow("Режим", MoraLeds.EXT_MODES, ext.mode) { m -> onChange(MoraLeds.Ext(m, ext.color)) }
    GsPillRow("Цвет", MoraLeds.EXT_COLORS, ext.color) { c -> onChange(MoraLeds.Ext(ext.mode, c)) }
}
