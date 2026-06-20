package com.mora.gamespace

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer as VlcMediaPlayer
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.os.BatteryManager
import android.os.Bundle
import android.view.TextureView
import android.view.Surface as AndroidSurface
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.LinearInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.State
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private enum class LaunchStage {
    IntroVideo,
    DisposeIntro,
    PanelEntering,
    Ready,
}

@Composable
private fun GameSpaceRoot() {
    var launchStage by remember { mutableStateOf(LaunchStage.IntroVideo) }
    var bgmEnabled by remember { mutableStateOf(true) }

    // Строгая последовательность, чтобы не было наложения тяжёлых экранов:
    // 1) только видео;
    // 2) видео полностью удаляется из Compose/TextureView/MediaPlayer;
    // 3) только потом создаётся панель;
    // 4) музыка стартует только после завершения выхода панели.
    BackgroundMusic(enabled = bgmEnabled && launchStage == LaunchStage.Ready)

    LaunchedEffect(launchStage) {
        when (launchStage) {
            LaunchStage.DisposeIntro -> {
                delay(220) // даём TextureView/MediaPlayer полностью освободиться
                launchStage = LaunchStage.PanelEntering
            }
            LaunchStage.PanelEntering -> {
                delay(920) // длительность выхода панели
                launchStage = LaunchStage.Ready
            }
            else -> Unit
        }
    }

    val panelAlpha by animateFloatAsState(
        targetValue = when (launchStage) {
            LaunchStage.PanelEntering, LaunchStage.Ready -> 1f
            else -> 0f
        },
        animationSpec = tween(860, easing = FastOutSlowInEasing),
        label = "panelAlpha",
    )
    val panelScale by animateFloatAsState(
        targetValue = when (launchStage) {
            LaunchStage.PanelEntering, LaunchStage.Ready -> 1f
            else -> .92f
        },
        animationSpec = tween(860, easing = FastOutSlowInEasing),
        label = "panelScale",
    )

    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            when (launchStage) {
                LaunchStage.IntroVideo -> {
                    StartAnimation(
                        onFinished = {
                            if (launchStage == LaunchStage.IntroVideo) {
                                launchStage = LaunchStage.DisposeIntro
                            }
                        }
                    )
                }
                LaunchStage.DisposeIntro -> {
                    // намеренно пустой чёрный кадр: intro уже удалён, панель ещё не создана
                }
                LaunchStage.PanelEntering,
                LaunchStage.Ready -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = panelAlpha
                                scaleX = panelScale
                                scaleY = panelScale
                            }
                    ) {
                        GameLobby(
                            bgmEnabled = bgmEnabled,
                            onBgmToggle = { bgmEnabled = !bgmEnabled },
                            animationsEnabled = launchStage == LaunchStage.Ready,
                        )
                    }
                }
            }
        }
    }
}

private class VlcMusicController {
    var libVlc: LibVLC? = null
    var player: VlcMediaPlayer? = null
}

@Composable
private fun BackgroundMusic(enabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { VlcMusicController() }

    // Background music plays through the VLC engine (libVLC) with its own FFmpeg-based
    // decoders, independent of the device's system codecs. Created only when the second
    // screen is ready, fully torn down on dispose.
    DisposableEffect(enabled) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }
        val trackFile = File(context.cacheDir, "bgm_loop.m4a")
        runCatching {
            if (!trackFile.exists() || trackFile.length() == 0L) {
                context.resources.openRawResource(R.raw.bgm_loop).use { input ->
                    FileOutputStream(trackFile).use { output -> input.copyTo(output) }
                }
            }
        }
        runCatching {
            val vlc = LibVLC(context, arrayListOf("--no-video", "--audio-resampler=soxr"))
            val mp = VlcMediaPlayer(vlc)
            val media = Media(vlc, Uri.fromFile(trackFile)).apply {
                addOption(":input-repeat=65535")
                addOption(":no-video")
            }
            mp.media = media
            media.release()
            mp.volume = 0 // start silent; LaunchedEffect fades it in
            mp.play()
            controller.libVlc = vlc
            controller.player = mp
        }
        onDispose {
            val mp = controller.player
            val vlc = controller.libVlc
            controller.player = null
            controller.libVlc = null
            runCatching { mp?.stop() }
            runCatching { mp?.release() }
            runCatching { vlc?.release() }
        }
    }

    // Observe app foreground/background.
    var inForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> inForeground = true
                Lifecycle.Event.ON_STOP -> inForeground = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Smoothly fade music in on foreground and out (then pause) when hidden to background.
    LaunchedEffect(enabled, inForeground) {
        val mp = controller.player ?: return@LaunchedEffect
        if (inForeground) {
            runCatching { if (!mp.isPlaying) mp.play() }
            var v = 0
            while (v < 100) {
                runCatching { mp.volume = v }
                delay(26)
                v += 10
            }
            runCatching { mp.volume = 100 }
        } else {
            var v = 100
            while (v > 0) {
                runCatching { mp.volume = v }
                delay(40)
                v -= 8
            }
            runCatching { mp.volume = 0 }
            runCatching { mp.pause() }
        }
    }
}

@Composable
private fun StartAnimation(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.fillMaxSize().background(Color.Black),
        factory = { viewContext ->
            val textureView = TextureView(viewContext)
            textureView.isOpaque = true
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var player: MediaPlayer? = null
                    private var surface: AndroidSurface? = null
                    private var finished = false
                    private var sourceVideoWidth = 0
                    private var sourceVideoHeight = 0

                    fun applyCenterCropTransform(viewWidth: Int, viewHeight: Int) {
                        if (viewWidth <= 0 || viewHeight <= 0 || sourceVideoWidth <= 0 || sourceVideoHeight <= 0) return

                        // Correct GameSpace-style usage: keep the original video untouched,
                        // and center-crop it at render time. This fills the display without
                        // distorting the frame and without re-encoding the MP4.
                        // TextureView by default stretches the decoded buffer to the view bounds.
                        // To get correct center-crop without squashing, scale relative to that default fit.
                        val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()
                        val videoAspect = sourceVideoWidth.toFloat() / sourceVideoHeight.toFloat()
                        val scaleX: Float
                        val scaleY: Float
                        if (videoAspect > viewAspect) {
                            scaleX = videoAspect / viewAspect
                            scaleY = 1f
                        } else {
                            scaleX = 1f
                            scaleY = viewAspect / videoAspect
                        }
                        val matrix = Matrix().apply {
                            setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
                        }
                        textureView.setTransform(matrix)
                    }

                    override fun onSurfaceTextureAvailable(texture: android.graphics.SurfaceTexture, width: Int, height: Int) {
                        surface = AndroidSurface(texture)
                        player = MediaPlayer().apply {
                            setDataSource(context, Uri.parse("android.resource://${context.packageName}/${R.raw.start_animation}"))
                            setSurface(surface)
                            isLooping = false
                            setVolume(1f, 1f)
                            setOnVideoSizeChangedListener { _, w, h ->
                                sourceVideoWidth = w
                                sourceVideoHeight = h
                                applyCenterCropTransform(textureView.width, textureView.height)
                            }
                            setOnPreparedListener { mediaPlayer ->
                                sourceVideoWidth = mediaPlayer.videoWidth
                                sourceVideoHeight = mediaPlayer.videoHeight
                                applyCenterCropTransform(textureView.width, textureView.height)
                                mediaPlayer.start()
                            }
                            setOnCompletionListener {
                                if (!finished) {
                                    finished = true
                                    onFinished()
                                }
                            }
                            setOnErrorListener { _, _, _ ->
                                if (!finished) {
                                    finished = true
                                    onFinished()
                                }
                                true
                            }
                            prepareAsync()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(texture: android.graphics.SurfaceTexture, width: Int, height: Int) {
                        applyCenterCropTransform(width, height)
                    }
                    override fun onSurfaceTextureUpdated(texture: android.graphics.SurfaceTexture) = Unit
                    override fun onSurfaceTextureDestroyed(texture: android.graphics.SurfaceTexture): Boolean {
                        runCatching { player?.stop() }
                        player?.release()
                        player = null
                        surface?.release()
                        surface = null
                        return true
                    }
                }
            textureView
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
private fun GameLobby(bgmEnabled: Boolean, onBgmToggle: () -> Unit, animationsEnabled: Boolean) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var fanEnabled by remember { mutableStateOf(true) }
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

        // Cooling fan sits BEHIND the cards/HUD (drawn first = lowest layer).
        CoreReactor(Modifier.align(Alignment.Center).size(420.dp), animationsEnabled = animationsEnabled)
        TopHud(Modifier.align(Alignment.TopCenter))
        LeftGameRail(
            games = demoGames,
            selectedIndex = selectedIndex,
            onSelected = { selectedIndex = it },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 34.dp, top = 34.dp),
        )
        SelectedGamePanel(
            game = selected,
            onLaunch = {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(selected.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 34.dp, top = 66.dp),
        )
        BottomDock(
            fanEnabled = fanEnabled,
            bgmEnabled = bgmEnabled,
            onFanToggle = { fanEnabled = !fanEnabled },
            onBgmToggle = onBgmToggle,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
        )
    }
}

@Composable
private fun TopHud(modifier: Modifier = Modifier) {
    val hud by rememberHudState()
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("REDMAGIC", color = Color.White.copy(alpha = .9f), fontSize = 28.sp, letterSpacing = 7.sp, fontWeight = FontWeight.Light)
            Text(hud.time, color = Color.White.copy(alpha = .75f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            BatteryMini(hud.batteryPercent)
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFF6F86), Color(0xFF3A0B14))))
                    .border(1.dp, Color.White.copy(.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("M", color = Color.White, fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TopIconButton(R.drawable.add_shortcut_icon)
            TopIconButton(R.drawable.chat_assistant_settings)
        }
    }
}

private data class HudState(val time: String, val batteryPercent: Int)

@Composable
private fun rememberHudState(): State<HudState> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(readHudState(context)) }

    // Time update: once per minute is enough for HH:mm and avoids constant second-screen work.
    LaunchedEffect(Unit) {
        while (true) {
            state.value = state.value.copy(time = currentTimeText())
            delay(60_000)
        }
    }

    // Battery update: event-driven, no polling/registerReceiver loop.
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                state.value = HudState(
                    time = currentTimeText(),
                    batteryPercent = batteryPercentFromIntent(intent),
                )
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (sticky != null) {
            state.value = HudState(currentTimeText(), batteryPercentFromIntent(sticky))
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return state
}

private fun readHudState(context: Context): HudState {
    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    return HudState(
        time = currentTimeText(),
        batteryPercent = sticky?.let { batteryPercentFromIntent(it) } ?: 0,
    )
}

private fun currentTimeText(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun batteryPercentFromIntent(intent: Intent): Int {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 0
}

@Composable
private fun TopIconButton(iconRes: Int) {
    Box(
        Modifier.size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(.35f))
            .border(1.dp, Color.White.copy(.18f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(28.dp), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun BatteryMini(percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Image(
            painterResource(batteryIconFor(percent)),
            contentDescription = null,
            modifier = Modifier.width(28.dp).height(14.dp),
            contentScale = ContentScale.Fit,
        )
        Text("$percent%", color = Color.White.copy(alpha = .78f), fontSize = 12.sp)
    }
}

private fun batteryIconFor(percent: Int): Int = when {
    percent <= 10 -> R.drawable.battery5
    percent <= 30 -> R.drawable.battery20
    percent <= 50 -> R.drawable.battery40
    percent <= 70 -> R.drawable.battery60
    percent <= 90 -> R.drawable.battery80
    else -> R.drawable.battery100
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
private fun CoreReactor(modifier: Modifier = Modifier, animationsEnabled: Boolean) {
    Box(modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ReactorView(it) },
            update = { it.animationsEnabled = animationsEnabled },
        )
        Box(Modifier.size(138.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF3B0710), Color.Black))).border(3.dp, Color(0xFF26313A), CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.rm_logo), contentDescription = null, modifier = Modifier.size(102.dp), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun SelectedGamePanel(game: GameCard, onLaunch: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(292.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(116.dp))
        Text(game.title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RedLaunchButton("Запуск игры", onLaunch)
            SpeedButton()
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Image(painterResource(R.drawable.exo_ic_chevron_right), contentDescription = null, modifier = Modifier.size(30.dp), contentScale = ContentScale.Fit)
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Image(painterResource(R.drawable.exo_ic_chevron_left), contentDescription = null, modifier = Modifier.size(30.dp), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun SpeedButton() {
    Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF260507).copy(.72f)).border(1.dp, Color(0xFFFF604A), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.exo_ic_speed), contentDescription = null, modifier = Modifier.size(32.dp), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun BottomDock(
    fanEnabled: Boolean,
    bgmEnabled: Boolean,
    onFanToggle: () -> Unit,
    onBgmToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab(iconRes = R.drawable.gamelobby_icon, title = "Game Lobby", selected = true)
            BottomTab(iconRes = R.drawable.top_indicator_expand, title = "Высокопроизвод", selected = false)
        }
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundDockButton(iconRes = if (fanEnabled) R.drawable.cooling_fan_on else R.drawable.cooling_fan_off, onClick = onFanToggle)
            RoundDockButton(iconRes = if (bgmEnabled) R.drawable.bgm_on else R.drawable.bgm_off, onClick = onBgmToggle)
        }
    }
}

@Composable
private fun BottomTab(iconRes: Int, title: String, selected: Boolean) {
    Surface(
        modifier = Modifier.width(if (selected) 178.dp else 198.dp).height(54.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF15171B).copy(alpha = .72f),
        border = BorderStroke(1.dp, if (selected) Color(0xFFFF604A).copy(.68f) else Color.White.copy(.22f)),
    ) {
        Row(
            Modifier.fillMaxSize().background(
                if (selected) Brush.horizontalGradient(listOf(Color(0xFF8C1820).copy(.82f), Color(0xFF1B1D22).copy(.76f)))
                else Brush.horizontalGradient(listOf(Color(0xFF25282F).copy(.78f), Color(0xFF101217).copy(.72f)))
            ).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(30.dp), contentScale = ContentScale.Fit)
            Text(title, color = if (selected) Color.White else Color.White.copy(.72f), fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun RoundDockButton(iconRes: Int, onClick: () -> Unit) {
    Box(
        Modifier.size(50.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF3A3D44).copy(.90f), Color(0xFF0F1116).copy(.88f))))
            .border(1.dp, Color.White.copy(.20f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(31.dp), contentScale = ContentScale.Fit)
    }
}


private class ReactorView(context: Context) : View(context) {

    // The static ring is drawn ONCE into a hardware layer. The visible motion is ONLY a
    // GPU-driven View.rotation (a fan spins; it does not "breathe", so there is no scale
    // pulse). onDraw is NOT called every frame, so there is no per-frame CPU cost.

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var rotationAnimator: ObjectAnimator? = null

    init {
        // Cache this view as a hardware layer so rotation is a pure GPU transform.
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    var animationsEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) startAnimators() else stopAnimators()
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (animationsEnabled) startAnimators()
    }

    override fun onDetachedFromWindow() {
        stopAnimators()
        super.onDetachedFromWindow()
    }

    private fun startAnimators() {
        if (rotationAnimator == null) {
            rotationAnimator = ObjectAnimator.ofFloat(this, View.ROTATION, 0f, 360f).apply {
                duration = 9_000L // равномерное вращение вентилятора
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
            }
        }
        rotationAnimator?.let { if (!it.isStarted) it.start() }
    }

    private fun stopAnimators() {
        rotationAnimator?.cancel()
        rotation = 0f
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = kotlin.math.min(width, height) / 2f

        // --- Base disc (this whole view is layered UNDER the surrounding cards) ---
        fillPaint.shader = null
        fillPaint.color = android.graphics.Color.argb(150, 9, 11, 15)
        canvas.drawCircle(cx, cy, r * 0.985f, fillPaint)
        fillPaint.color = android.graphics.Color.argb(42, 255, 38, 64)
        canvas.drawCircle(cx, cy, r * 0.80f, fillPaint)

        // --- Outer rim rings ---
        strokePaint.shader = null
        strokePaint.color = android.graphics.Color.argb(80, 255, 70, 80)
        strokePaint.strokeWidth = r * 0.020f
        canvas.drawCircle(cx, cy, r * 0.965f, strokePaint)
        strokePaint.color = android.graphics.Color.argb(26, 255, 255, 255)
        strokePaint.strokeWidth = r * 0.006f
        canvas.drawCircle(cx, cy, r * 0.905f, strokePaint)

        // --- Fan blades ---
        val blades = 13
        val ri = r * 0.34f
        val ro = r * 0.90f
        val bladeShader = android.graphics.LinearGradient(
            cx, cy - ro, cx, cy - ri,
            android.graphics.Color.argb(185, 74, 82, 92),
            android.graphics.Color.argb(120, 24, 28, 34),
            android.graphics.Shader.TileMode.CLAMP,
        )
        for (i in 0 until blades) {
            val deg = i * 360f / blades
            canvas.save()
            canvas.rotate(deg, cx, cy)
            val path = android.graphics.Path()
            // A tapered, slightly swept blade pointing "up" from the hub.
            path.moveTo(cx - r * 0.040f, cy - ri)
            path.quadTo(cx - r * 0.235f, cy - r * 0.60f, cx - r * 0.120f, cy - ro)
            path.quadTo(cx + r * 0.005f, cy - ro - r * 0.028f, cx + r * 0.170f, cy - ro)
            path.quadTo(cx + r * 0.080f, cy - r * 0.55f, cx + r * 0.050f, cy - ri)
            path.close()
            fillPaint.shader = bladeShader
            canvas.drawPath(path, fillPaint)
            fillPaint.shader = null
            // leading-edge red highlight
            strokePaint.color = android.graphics.Color.argb(70, 255, 116, 120)
            strokePaint.strokeWidth = r * 0.006f
            canvas.drawPath(path, strokePaint)
            canvas.restore()
        }

        // --- Hub ring (behind the center logo) ---
        strokePaint.color = android.graphics.Color.argb(130, 38, 49, 58)
        strokePaint.strokeWidth = r * 0.022f
        canvas.drawCircle(cx, cy, ri * 0.98f, strokePaint)
    }
}
