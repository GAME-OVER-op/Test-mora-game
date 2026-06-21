package com.mora.gamespace

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaPlayer
import android.net.Uri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer as VlcMediaPlayer
import java.io.File
import java.io.FileOutputStream
import android.os.BatteryManager
import android.os.Bundle
import android.view.TextureView
import android.view.Surface as AndroidSurface
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.LinearInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
    // Simple, smooth transition: the intro video plays fullscreen with sound; when it ends
    // the lobby panel comes forward (fade + scale up), like before the staged rework.
    var introFinished by remember { mutableStateOf(false) }
    var animationsReady by remember { mutableStateOf(false) }
    var bgmEnabled by remember { mutableStateOf(true) }

    BackgroundMusic(enabled = bgmEnabled && introFinished)

    LaunchedEffect(introFinished) {
        if (introFinished) {
            delay(650) // let the panel finish entering before the fan starts spinning
            animationsReady = true
        }
    }

    val panelAlpha by animateFloatAsState(
        targetValue = if (introFinished) 1f else 0f,
        animationSpec = tween(620, easing = FastOutSlowInEasing),
        label = "panelAlpha",
    )
    val panelScale by animateFloatAsState(
        targetValue = if (introFinished) 1f else 0.92f,
        animationSpec = tween(620, easing = FastOutSlowInEasing),
        label = "panelScale",
    )

    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (!introFinished) {
                StartAnimation(onFinished = { introFinished = true })
            } else {
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
                        animationsEnabled = animationsReady,
                    )
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

    // Background music plays through the VLC engine (libVLC) with its own FFmpeg-based decoders,
    // independent of the device's system codecs. Created only when the lobby is ready, fully
    // torn down on dispose.
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
            mp.volume = 0 // start silent; faded in by the effect below
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

    // Smoothly fade in on foreground, fade out + pause when the app is hidden to background.
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
    val packageName: String,
    val title: String,
    val icon: ImageBitmap? = null,
    val fallbackRes: Int? = null,
    val lastPlayed: String = "",
)

@Composable
private fun GameIcon(card: GameCard, modifier: Modifier = Modifier) {
    val icon = card.icon
    val fallback = card.fallbackRes
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
    } else if (fallback != null) {
        Image(painterResource(fallback), contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        Box(modifier.background(Color(0xFF1A1D22)))
    }
}

@Composable
private fun GameLobby(bgmEnabled: Boolean, onBgmToggle: () -> Unit, animationsEnabled: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var allApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var games by remember { mutableStateOf<List<GameCard>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var fanEnabled by remember { mutableStateOf(true) }
    var showAddGames by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    suspend fun rebuildGames() {
        val apps = withContext(Dispatchers.IO) { InstalledApps.loadUserApps(context) }
        val pkgs = withContext(Dispatchers.IO) {
            NubiaSettings.parseStrengthenPackages(NubiaIo.getGlobal(context, NubiaSettings.KEY_STRENGTHEN_LIST))
        }
        allApps = apps
        val byPkg = apps.associateBy { it.packageName }
        games = pkgs.mapNotNull { pkg -> byPkg[pkg]?.let { GameCard(it.packageName, it.label, it.icon) } }
        if (selectedIndex >= games.size) selectedIndex = 0
    }

    LaunchedEffect(Unit) { rebuildGames() }

    val selected = games.getOrNull(selectedIndex)

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
            games = games,
            selectedIndex = selectedIndex,
            onSelected = { selectedIndex = it },
            onAddGames = { showAddGames = true },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 34.dp, top = 34.dp),
        )
        SelectedGamePanel(
            game = selected,
            onLaunch = {
                val pkg = selected?.packageName ?: return@SelectedGamePanel
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            },
            onOpenSettings = { if (selected != null) showSettings = true },
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

    if (showAddGames) {
        AddGamesScreen(
            apps = allApps,
            initiallySelected = games.map { it.packageName },
            onClose = { newSelected ->
                showAddGames = false
                scope.launch {
                    withContext(Dispatchers.IO) {
                        NubiaIo.putGlobal(
                            context,
                            NubiaSettings.KEY_STRENGTHEN_LIST,
                            NubiaSettings.buildStrengthenList(newSelected),
                        )
                    }
                    rebuildGames()
                }
            },
        )
    }

    val settingsGame = selected
    if (showSettings && settingsGame != null) {
        GameSettingsSheet(game = settingsGame, onClose = { showSettings = false })
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

    LaunchedEffect(Unit) {
        while (true) {
            state.value = state.value.copy(time = currentTimeText())
            delay(60_000)
        }
    }

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
private fun LeftGameRail(
    games: List<GameCard>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onAddGames: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    GameIcon(game, Modifier.size(48.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(game.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Row(
            modifier = Modifier.height(74.dp).fillMaxWidth().clickable { onAddGames() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(68.dp).clip(RoundedCornerShape(13.dp)).border(1.dp, Color.White.copy(.18f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Text("\uFF0B", color = Color.White.copy(.72f), fontSize = 36.sp, fontWeight = FontWeight.Light)
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
private fun SelectedGamePanel(
    game: GameCard?,
    onLaunch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(292.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(116.dp))
        Text(game?.title ?: "Добавьте игры", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RedLaunchButton("Запуск игры", onLaunch)
            SpeedButton(onOpenSettings)
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
private fun SpeedButton(onClick: () -> Unit) {
    Box(
        Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF260507).copy(.72f)).border(1.dp, Color(0xFFFF604A), RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
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

// ---------------------------------------------------------------------------
// Settings I/O helpers (privileged, NO root — direct Settings.Global access).
// ---------------------------------------------------------------------------

private suspend fun applyPerGameInt(context: Context, key: String, pkg: String, values: IntArray) = withContext(Dispatchers.IO) {
    val cur = NubiaIo.getGlobal(context, key)
    NubiaIo.putGlobal(context, key, NubiaSettings.buildSaveString(cur, pkg, values))
}

private suspend fun applyPerGameStr(context: Context, key: String, pkg: String, value: String) = withContext(Dispatchers.IO) {
    val cur = NubiaIo.getGlobal(context, key)
    NubiaIo.putGlobal(context, key, NubiaSettings.buildSaveString(cur, pkg, value))
}

private suspend fun applyGlobalValue(context: Context, key: String, value: String) = withContext(Dispatchers.IO) {
    NubiaIo.putGlobal(context, key, value)
}

private suspend fun readPerGameInt(context: Context, key: String, pkg: String, def: Int): Int = withContext(Dispatchers.IO) {
    NubiaSettings.parse(NubiaIo.getGlobal(context, key), pkg, intArrayOf(def))[0]
}

private suspend fun readPerGameStr(context: Context, key: String, pkg: String, def: String): String = withContext(Dispatchers.IO) {
    NubiaSettings.parseString(NubiaIo.getGlobal(context, key), pkg, def)
}

private suspend fun readGlobalIntValue(context: Context, key: String, def: Int): Int = withContext(Dispatchers.IO) {
    NubiaIo.getGlobalInt(context, key, def)
}

// ---------------------------------------------------------------------------
// "Добавить игры" screen
// ---------------------------------------------------------------------------

@Composable
private fun AddGamesScreen(
    apps: List<InstalledApp>,
    initiallySelected: List<String>,
    onClose: (List<String>) -> Unit,
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(initiallySelected) } }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val p by animateFloatAsState(if (visible) 1f else 0f, tween(320, easing = FastOutSlowInEasing), label = "addgames")

    BackHandler { onClose(selected.toList()) }

    val added = apps.filter { selected.contains(it.packageName) }
    val notAdded = apps.filter { !selected.contains(it.packageName) }

    Box(Modifier.fillMaxSize().background(Color(0xFF050608)).graphicsLayer { alpha = p }) {
        Column(Modifier.fillMaxSize().padding(horizontal = 30.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(Color.Black.copy(.4f))
                        .border(1.dp, Color.White.copy(.18f), RoundedCornerShape(9.dp)).clickable { onClose(selected.toList()) },
                    contentAlignment = Alignment.Center,
                ) { Text("\u2039", color = Color.White, fontSize = 26.sp) }
                Spacer(Modifier.width(14.dp))
                Text("Добавить игры", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("Готово", color = Color(0xFFFF7A66), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onClose(selected.toList()) })
            }
            LazyColumn(Modifier.weight(1f)) {
                item { SectionHeader("Добавлено (" + added.size + ")") }
                items(added, key = { it.packageName }) { app ->
                    AppRow(app, isAdded = true, onAdd = { if (!selected.contains(app.packageName)) selected.add(app.packageName) }, onRemove = { selected.remove(app.packageName) })
                }
                item { SectionHeader(notAdded.size.toString() + " не добавлено") }
                items(notAdded, key = { it.packageName }) { app ->
                    AppRow(app, isAdded = false, onAdd = { if (!selected.contains(app.packageName)) selected.add(app.packageName) }, onRemove = { selected.remove(app.packageName) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, color = Color.White.copy(.55f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun AppRow(app: InstalledApp, isAdded: Boolean, onAdd: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Fit)
        Text(app.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Pill(text = "Удалить", active = !isAdded, onClick = onRemove)
        Spacer(Modifier.width(8.dp))
        Pill(text = "Добавить", active = isAdded, onClick = onAdd)
    }
}

@Composable
private fun Pill(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val border by animateColorAsState(if (active) Color(0xFFFF6A4A) else Color.White.copy(.16f), tween(200), label = "pillBorder")
    val scale by animateFloatAsState(if (active) 1f else 0.97f, tween(180), label = "pillScale")
    val bg = if (active) {
        Brush.horizontalGradient(listOf(Color(0xFF8C1820), Color(0xFFC22A30)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF181B21), Color(0xFF101318)))
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (active) Color.White else Color.White.copy(.74f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------------
// Per-game settings sheet (opened by the SpeedButton next to "Запуск игры")
// ---------------------------------------------------------------------------

private enum class SettingsCategory(val title: String) {
    Performance("Производительность"),
    Touch("Настройки экрана"),
    Display("Цвета"),
    Network("Сеть"),
}

@Composable
private fun GameSettingsSheet(game: GameCard, onClose: () -> Unit) {
    var category by remember { mutableStateOf(SettingsCategory.Performance) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val p by animateFloatAsState(if (visible) 1f else 0f, tween(360, easing = FastOutSlowInEasing), label = "sheet")

    BackHandler { onClose() }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * p))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }
    ) {
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.86f)
                .graphicsLayer { translationY = (1f - p) * size.height }
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF170A0C), Color(0xFF0C0E12)), endY = 380f))
                .border(1.dp, Color(0xFFFF5A4A).copy(.20f), RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(26.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.width(46.dp).height(4.dp).clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF5A4A), Color(0xFFB11E26))))
                    )
                }
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        Modifier.width(224.dp).fillMaxHeight().background(Color(0xFF080A0D)).padding(vertical = 16.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                            GameIcon(game, Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)))
                            Text(game.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        SettingsCategory.values().forEach { cat ->
                            CategoryItem(cat.title, category == cat) { category = cat }
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight().padding(28.dp)) {
                        Crossfade(targetState = category, animationSpec = tween(260), label = "catContent") { cat ->
                            when (cat) {
                                SettingsCategory.Performance -> PerformanceTab(game.packageName)
                                SettingsCategory.Touch -> TouchTab(game.packageName)
                                SettingsCategory.Display -> DisplayTab(game.packageName)
                                SettingsCategory.Network -> NetworkTab()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(title: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Color(0xFF24181A) else Color.Transparent, tween(220), label = "catBg")
    val barH by animateFloatAsState(if (selected) 18f else 0f, tween(220), label = "catBar")
    val textColor by animateColorAsState(if (selected) Color.White else Color.White.copy(.62f), tween(220), label = "catTxt")
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(barH.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF5A4A)))
            Spacer(Modifier.width(10.dp))
            Text(title, color = textColor, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
private fun PerformanceTab(pkg: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember(pkg) { mutableIntStateOf(NubiaSettings.MODE_BALANCE) }
    var cpuKhz by remember { mutableStateOf(0f) }
    var gpuHz by remember { mutableStateOf(0f) }

    LaunchedEffect(pkg) { mode = readPerGameInt(context, NubiaSettings.KEY_PERF_MODE, pkg, NubiaSettings.MODE_BALANCE) }
    LaunchedEffect(Unit) {
        while (true) {
            cpuKhz = withContext(Dispatchers.IO) { FreqReader.cpuCur().toFloat() }
            gpuHz = withContext(Dispatchers.IO) { FreqReader.gpuCur().toFloat() }
            delay(1000)
        }
    }
    val cpuAnim by animateFloatAsState(cpuKhz, tween(900, easing = FastOutSlowInEasing), label = "cpuAnim")
    val gpuAnim by animateFloatAsState(gpuHz, tween(900, easing = FastOutSlowInEasing), label = "gpuAnim")

    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text("Режим производительности", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill("Баланс", mode == NubiaSettings.MODE_BALANCE) { mode = NubiaSettings.MODE_BALANCE; scope.launch { applyPerGameInt(context, NubiaSettings.KEY_PERF_MODE, pkg, intArrayOf(NubiaSettings.MODE_BALANCE)) } }
            Pill("Подъём", mode == NubiaSettings.MODE_BOOST) { mode = NubiaSettings.MODE_BOOST; scope.launch { applyPerGameInt(context, NubiaSettings.KEY_PERF_MODE, pkg, intArrayOf(NubiaSettings.MODE_BOOST)) } }
            Pill("За пределами", mode == NubiaSettings.MODE_BEYOND) { mode = NubiaSettings.MODE_BEYOND; scope.launch { applyPerGameInt(context, NubiaSettings.KEY_PERF_MODE, pkg, intArrayOf(NubiaSettings.MODE_BEYOND)) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(46.dp), verticalAlignment = Alignment.CenterVertically) {
            PerfRing(mode, true, fmtGhz(cpuAnim), "ГГц", "CPU")
            PerfRing(mode, false, fmtMhz(gpuAnim), "МГц", "GPU")
        }
        Text("Повышение частоты ЦП/ГП для требовательных игр. «За пределами» — максимум.", color = Color.White.copy(.6f), fontSize = 13.sp)
    }
}

@Composable
private fun PerfRing(mode: Int, cpu: Boolean, valueText: String, unit: String, label: String) {
    val accent = ringAccent(mode)
    val infinite = rememberInfiniteTransition(label = "perfBob")
    val bob by infinite.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(animation = tween(1700, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "perfBobY",
    )
    Box(Modifier.size(width = 168.dp, height = 210.dp), contentAlignment = Alignment.BottomCenter) {
        Image(
            painterResource(ringRes(mode, cpu)),
            contentDescription = null,
            modifier = Modifier.size(150.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            label,
            color = Color.White.copy(.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 66.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter).graphicsLayer { translationY = bob.dp.toPx() },
        ) {
            Text(
                valueText,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(shadow = Shadow(color = accent.copy(alpha = .95f), blurRadius = 30f)),
            )
            Text(unit, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun ringAccent(mode: Int): Color = when (mode) {
    NubiaSettings.MODE_BOOST -> Color(0xFFFFC53D)
    NubiaSettings.MODE_BEYOND -> Color(0xFFFF5A4A)
    else -> Color(0xFF4AA8FF)
}

private fun ringRes(mode: Int, cpu: Boolean): Int = when (mode) {
    NubiaSettings.MODE_BOOST -> if (cpu) R.drawable.rise_cpu else R.drawable.rise_gpu
    NubiaSettings.MODE_BEYOND -> if (cpu) R.drawable.beyond_cpu else R.drawable.beyond_gpu
    else -> if (cpu) R.drawable.balance_cpu else R.drawable.balance_gpu
}

private fun fmtGhz(khz: Float): String = if (khz <= 0f) "--" else String.format(Locale.US, "%.2f", khz / 1_000_000f)
private fun fmtMhz(hz: Float): String = if (hz <= 0f) "--" else (hz / 1_000_000f).roundToInt().toString()

@Composable
private fun TouchTab(pkg: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sample by remember(pkg) { mutableIntStateOf(480) }
    var sen by remember(pkg) { mutableIntStateOf(0) }
    var follow by remember(pkg) { mutableIntStateOf(0) }
    var micro by remember(pkg) { mutableIntStateOf(0) }

    LaunchedEffect(pkg) {
        sample = readPerGameInt(context, NubiaSettings.KEY_SAMPLE_RATE, pkg, 480)
        sen = readPerGameInt(context, NubiaSettings.KEY_SENSITIVE, pkg, 0)
        follow = readPerGameInt(context, NubiaSettings.KEY_FOLLOW, pkg, 0)
        micro = readPerGameInt(context, NubiaSettings.KEY_MICRO, pkg, 0)
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Частота дискретизации касания", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill("Высокая · 480 Гц", sample == 480) { sample = 480; scope.launch { applyPerGameInt(context, NubiaSettings.KEY_SAMPLE_RATE, pkg, intArrayOf(480)) } }
            Pill("Сверхвысокая · 960 Гц", sample == 960) { sample = 960; scope.launch { applyPerGameInt(context, NubiaSettings.KEY_SAMPLE_RATE, pkg, intArrayOf(960)) } }
        }
        LevelSlider("Чувствительность касания", sen) { v -> sen = v; scope.launch { applyPerGameInt(context, NubiaSettings.KEY_SENSITIVE, pkg, intArrayOf(v)) } }
        LevelSlider("Точность следования", follow) { v -> follow = v; scope.launch { applyPerGameInt(context, NubiaSettings.KEY_FOLLOW, pkg, intArrayOf(v)) } }
        LevelSlider("Микро-чувствительность", micro) { v -> micro = v; scope.launch { applyPerGameInt(context, NubiaSettings.KEY_MICRO, pkg, intArrayOf(v)) } }
    }
}

@Composable
private fun LevelSlider(label: String, level: Int, onLevel: (Int) -> Unit) {
    var idx by remember(level) { mutableStateOf(levelToIndex(level).toFloat()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color.White.copy(.82f), fontSize = 14.sp)
        Slider(
            value = idx,
            onValueChange = { idx = it },
            valueRange = 0f..4f,
            steps = 3,
            onValueChangeFinished = { onLevel(NubiaSettings.TOUCH_SCREEN_LEVEL[idx.roundToInt().coerceIn(0, 4)]) },
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF5A4A),
                activeTrackColor = Color(0xFFC22A30),
                inactiveTrackColor = Color.White.copy(.16f),
                activeTickColor = Color(0xFFFF8A7A),
                inactiveTickColor = Color.White.copy(.24f),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (n in NubiaSettings.TOUCH_SCREEN_LEVEL) {
                Text(n.toString(), color = Color.White.copy(.5f), fontSize = 11.sp)
            }
        }
    }
}

private fun levelToIndex(level: Int): Int {
    val i = NubiaSettings.TOUCH_SCREEN_LEVEL.indexOf(level)
    return if (i < 0) 2 else i
}

@Composable
private fun DisplayTab(pkg: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var disp by remember(pkg) { mutableStateOf("default") }
    LaunchedEffect(pkg) { disp = readPerGameStr(context, NubiaSettings.KEY_DISPLAY, pkg, "default") }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Цветовой режим экрана", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill("Обычный", disp == "default") { disp = "default"; scope.launch { applyPerGameStr(context, NubiaSettings.KEY_DISPLAY, pkg, "default") } }
            Pill("Гонки", disp == "racing") { disp = "racing"; scope.launch { applyPerGameStr(context, NubiaSettings.KEY_DISPLAY, pkg, "racing") } }
            Pill("Шутер", disp == "shooting") { disp = "shooting"; scope.launch { applyPerGameStr(context, NubiaSettings.KEY_DISPLAY, pkg, "shooting") } }
        }
        Text("Профиль усиления цвета под жанр игры.", color = Color.White.copy(.6f), fontSize = 13.sp)
    }
}

@Composable
private fun NetworkTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var wifi by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { wifi = readGlobalIntValue(context, NubiaSettings.KEY_WIFI_LOW_LATENCY, 0) == 1 }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Сеть", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("WiFi: режим низкой задержки", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Глобальный параметр (gsc_wifi_low_latency_mode)", color = Color.White.copy(.55f), fontSize = 12.sp)
            }
            Switch(
                checked = wifi,
                onCheckedChange = { checked -> wifi = checked; scope.launch { applyGlobalValue(context, NubiaSettings.KEY_WIFI_LOW_LATENCY, if (checked) "1" else "0") } },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFC22A30),
                    checkedBorderColor = Color(0xFFFF5A4A),
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Cooling fan / reactor — restored to the original v16 look (48 ticks),
// rotation only via a GPU layer (a fan spins, it does not "breathe").
// ---------------------------------------------------------------------------

private class ReactorView(context: Context) : View(context) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var rotationAnimator: ObjectAnimator? = null

    init {
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
                duration = 9_000L
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

        fillPaint.color = android.graphics.Color.argb(168, 10, 13, 17)
        canvas.drawCircle(cx, cy, r, fillPaint)

        fillPaint.color = android.graphics.Color.argb(46, 255, 32, 61)
        canvas.drawCircle(cx, cy, r * .84f, fillPaint)

        strokePaint.color = android.graphics.Color.argb(36, 255, 75, 82)
        strokePaint.strokeWidth = 18f
        canvas.drawCircle(cx, cy, r * .58f, strokePaint)

        for (i in 0 until 48) {
            val angle = Math.toRadians(i * 360.0 / 48.0)
            val inner = r * .89f
            val outer = if (i % 4 == 0) r * .985f else r * .94f
            strokePaint.color = if (i % 4 == 0) {
                android.graphics.Color.argb(36, 255, 255, 255)
            } else {
                android.graphics.Color.argb(14, 255, 255, 255)
            }
            strokePaint.strokeWidth = if (i % 4 == 0) 3f else 1.2f
            val sx = cx + kotlin.math.cos(angle).toFloat() * inner
            val sy = cy + kotlin.math.sin(angle).toFloat() * inner
            val ex = cx + kotlin.math.cos(angle).toFloat() * outer
            val ey = cy + kotlin.math.sin(angle).toFloat() * outer
            canvas.drawLine(sx, sy, ex, ey, strokePaint)
        }
    }
}
