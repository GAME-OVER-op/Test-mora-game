package com.mora.gamespace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()
        setContent { GameSpaceRoot() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}

@Composable
private fun GameSpaceRoot() {
    var splashVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3600)
        splashVisible = false
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            GameLobby()
            AnimatedVisibility(
                visible = splashVisible,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(700)),
            ) {
                StartAnimation(onFinished = { splashVisible = false })
            }
        }
    }
}

@Composable
private fun StartAnimation(onFinished: () -> Unit) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        factory = {
            VideoView(it).apply {
                setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.start_animation}"))
                setOnPreparedListener { mp ->
                    mp.isLooping = false
                    mp.setVolume(0f, 0f)
                    start()
                }
                setOnCompletionListener { onFinished() }
                setOnErrorListener { _, _, _ ->
                    onFinished()
                    true
                }
            }
        }
    )
}

private data class GameCard(
    val title: String,
    val packageName: String,
    val imageRes: Int,
    val lastPlayed: String = "<1h",
)

private val demoGames = listOf(
    GameCard("Call of Duty", "com.activision.callofduty.shooter", R.drawable.performance_logo),
    GameCard("Mobile Legends: Bang Bang", "com.mobile.legends", R.drawable.pure_logo),
    GameCard("MT Manager", "bin.mt.plus", R.drawable.data_manager, "tool"),
)

@Composable
private fun GameLobby() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selected = demoGames[selectedIndex]
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().background(Color(0xFF050506))) {
        Image(
            painter = painterResource(R.drawable.gamespace_wallpaper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.96f,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.20f),
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.26f),
                    )
                )
        )

        TopHud(Modifier.align(Alignment.TopCenter))
        LeftGameRail(
            games = demoGames,
            selectedIndex = selectedIndex,
            onSelected = { selectedIndex = it },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 34.dp, top = 34.dp),
        )
        CoreReactor(Modifier.align(Alignment.Center).size(420.dp))
        SelectedGamePanel(
            game = selected,
            onLaunch = {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(selected.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 34.dp, top = 56.dp),
        )
        BottomDock(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp))
    }
}

@Composable
private fun TopHud(modifier: Modifier = Modifier) {
    val now = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("REDMAGIC", color = Color.White.copy(alpha = .9f), fontSize = 28.sp, letterSpacing = 7.sp, fontWeight = FontWeight.Light)
            Text(now, color = Color.White.copy(alpha = .75f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            BatteryMini(21)
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFF6F86), Color(0xFF3A0B14))))
                    .border(1.dp, Color.White.copy(.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("M", color = Color.White, fontWeight = FontWeight.Bold) }
        }
        Text("●", color = Color.White.copy(alpha = .72f), fontSize = 30.sp)
    }
}

@Composable
private fun BatteryMini(percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.width(26.dp).height(12.dp).border(1.dp, Color.White.copy(.6f), RoundedCornerShape(2.dp)).padding(2.dp)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(percent / 100f).background(Color(0xFF70E6A0), RoundedCornerShape(1.dp)))
        }
        Text("$percent%", color = Color.White.copy(alpha = .78f), fontSize = 12.sp)
    }
}

@Composable
private fun LeftGameRail(games: List<GameCard>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(250.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        games.forEachIndexed { index, game ->
            Row(
                modifier = Modifier.fillMaxWidth().height(74.dp).clickable { onSelected(index) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(68.dp)
                        .shadow(if (index == selectedIndex) 12.dp else 0.dp, RoundedCornerShape(13.dp))
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFF111318))
                        .border(
                            width = if (index == selectedIndex) 2.dp else 1.dp,
                            color = if (index == selectedIndex) Color(0xFFFF604A) else Color.White.copy(.12f),
                            shape = RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(game.imageRes), contentDescription = null, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
                }
                Column(Modifier.weight(1f)) {
                    Text(game.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(game.lastPlayed, color = Color.White.copy(.45f), fontSize = 10.sp)
                }
            }
        }
        Row(
            modifier = Modifier.height(74.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(68.dp).clip(RoundedCornerShape(13.dp)).border(1.dp, Color.White.copy(.18f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Text("＋", color = Color.White.copy(.72f), fontSize = 36.sp, fontWeight = FontWeight.Light)
            }
            Text("Добавить игры", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CoreReactor(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "reactor")
    val pulse by transition.animateFloat(
        initialValue = .82f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16000), RepeatMode.Restart),
        label = "rotation"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
            val r = size.minDimension / 2f
            drawCircle(Color(0xFF0A0D11).copy(alpha = .72f), r)
            drawCircle(Color(0xFFFF203D).copy(alpha = .20f * pulse), r * .84f)
            drawCircle(Color(0xFFFF4B52).copy(alpha = .15f), r * .58f, style = Stroke(width = 18f))
            repeat(64) { i ->
                val a = Math.toRadians((i * 360.0 / 64.0))
                val inner = r * .87f
                val outer = if (i % 4 == 0) r * .98f else r * .93f
                drawLine(
                    Color.White.copy(alpha = if (i % 4 == 0) .18f else .08f),
                    Offset(center.x + kotlin.math.cos(a).toFloat() * inner, center.y + kotlin.math.sin(a).toFloat() * inner),
                    Offset(center.x + kotlin.math.cos(a).toFloat() * outer, center.y + kotlin.math.sin(a).toFloat() * outer),
                    strokeWidth = if (i % 4 == 0) 4f else 2f,
                )
            }
        }
        Box(Modifier.size(130.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF3B0710), Color.Black))).border(3.dp, Color(0xFF26313A), CircleShape), contentAlignment = Alignment.Center) {
            Text("M", color = Color(0xFFFF5462), fontSize = 62.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SelectedGamePanel(game: GameCard, onLaunch: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(292.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallHexButton("▰")
            SmallHexButton("◈")
        }
        Spacer(Modifier.height(60.dp))
        Text(game.title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RedLaunchButton("Запуск игры", onLaunch)
            HexArrowButton()
        }
    }
}

@Composable
private fun RedLaunchButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.width(178.dp).height(54.dp).clip(RoundedCornerShape(7.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF701D1F), Color(0xFFA33136), Color(0xFF631719))))
            .border(1.dp, Color(0xFFFF604A), RoundedCornerShape(7.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("❯", color = Color.White.copy(.72f), fontSize = 30.sp)
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("❮", color = Color.White.copy(.72f), fontSize = 30.sp)
        }
    }
}

@Composable
private fun HexArrowButton() {
    Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF260507).copy(.72f)).border(1.dp, Color(0xFFFF604A), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Text(">", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun SmallHexButton(label: String) {
    Box(Modifier.size(38.dp).clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(.28f)).border(1.dp, Color.White.copy(.24f), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
        Text(label, color = Color.White.copy(.86f), fontSize = 18.sp)
    }
}

@Composable
private fun BottomDock(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 310.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
        SkewDock()
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            DockHex("▣")
            DockHex("▦")
            DockHex("♪")
            DockHex("✤")
        }
    }
}

@Composable
private fun SkewDock() {
    Surface(
        modifier = Modifier.width(410.dp).height(56.dp),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        color = Color(0xFF15171B).copy(alpha = .68f),
        border = BorderStroke(1.dp, Color.White.copy(.25f)),
    ) {
        Row(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF7E151B).copy(.78f), Color(0xFF16181D).copy(.78f)))), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("⌂", color = Color.White, fontSize = 26.sp)
            Text("Game Lobby", color = Color.White, fontSize = 12.sp)
            Text("▥", color = Color.White.copy(.78f), fontSize = 26.sp)
            Text("Высокопроизвод", color = Color.White.copy(.7f), fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun DockHex(label: String) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val p = Path().apply {
                moveTo(w * .50f, 0f)
                lineTo(w * .90f, h * .25f)
                lineTo(w * .90f, h * .75f)
                lineTo(w * .50f, h)
                lineTo(w * .10f, h * .75f)
                lineTo(w * .10f, h * .25f)
                close()
            }
            drawPath(p, Color(0xFF171A20).copy(.86f))
            drawPath(p, Color.White.copy(.20f), style = Stroke(width = 2f))
        }
        Text(label, color = Color.White.copy(.86f), fontSize = 23.sp)
    }
}
