package com.redmagic.touchsettings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

/**
 * Standalone replica of the RedMagic / nubia Game Space control panel.
 *
 * Writes the SAME Settings.Global keys, in the SAME per-game encoding, that the
 * original GameSpace uses (see NubiaTouchDb), so GameSpace reads the values back
 * correctly instead of resetting them to 0.
 *
 * Cards: Performance (live CPU/GPU rings + mode selector), Touch adjust,
 * Network, Display (light gamma).
 */
public class MainActivity extends AppCompatActivity {

    // ---- per-game keys (verified against TouchOperationBean$OperationTypeParams) ----
    private static final String KEY_SAMPLE_RATE = "NubiaperformanceTouchSampleRate";
    private static final String KEY_SENSITIVE   = "NubiaperformanceTouchSen";
    private static final String KEY_FOLLOW      = "NubiaperformanceTouchFollow";
    private static final String KEY_MICRO       = "NubiaperformanceTouchMicroSensitive";
    private static final String KEY_PERF_MODE   = "NubiaperformanceMode";            // 0/1/2
    private static final String KEY_DISPLAY     = "game_strengthen_mode_value";       // default/racing/shooting
    // ---- global keys ----
    private static final String KEY_WIFI_LOW_LATENCY = "gsc_wifi_low_latency_mode";   // 0/1

    // Touch sliders store one of {-2,-1,0,1,2} (TOUCH_SCREEN_LEVEL); SeekBar max=4, center=2.
    private static final int[] TOUCH_SCREEN_LEVEL = { -2, -1, 0, 1, 2 };

    private EditText targetPackage;
    private TextView rootStatus;

    private TextView sampleHigh, sampleUltra;
    private TextView modeBalance, modePerformance, modeBeyond;
    private TextView gammaDefault, gammaRacing, gammaShooting;
    private SwitchCompat wifiSwitch;

    private RingView ringCpu, ringGpu;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshRings();
            ui.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        targetPackage = findViewById(R.id.target_package);
        rootStatus = findViewById(R.id.root_status);

        // ---- performance rings ----
        ringCpu = findViewById(R.id.ring_cpu);
        ringGpu = findViewById(R.id.ring_gpu);

        // ---- performance mode pills ----
        modeBalance = findViewById(R.id.mode_balance);
        modePerformance = findViewById(R.id.mode_performance);
        modeBeyond = findViewById(R.id.mode_beyond);
        selectMode(0);
        modeBalance.setOnClickListener(v -> { selectMode(0); apply(KEY_PERF_MODE, new int[]{ 0 }); });
        modePerformance.setOnClickListener(v -> { selectMode(1); apply(KEY_PERF_MODE, new int[]{ 1 }); });
        modeBeyond.setOnClickListener(v -> { selectMode(2); apply(KEY_PERF_MODE, new int[]{ 2 }); });

        // ---- sample rate pills (value = Hz: 480 / 960) ----
        sampleHigh = findViewById(R.id.sample_high);
        sampleUltra = findViewById(R.id.sample_ultra);
        sampleHigh.setSelected(true);
        sampleHigh.setOnClickListener(v -> {
            sampleHigh.setSelected(true);
            sampleUltra.setSelected(false);
            apply(KEY_SAMPLE_RATE, new int[]{ 480 });
        });
        sampleUltra.setOnClickListener(v -> {
            sampleHigh.setSelected(false);
            sampleUltra.setSelected(true);
            apply(KEY_SAMPLE_RATE, new int[]{ 960 });
        });

        // ---- -2..2 sliders ----
        bindLevelSeek(R.id.seek_sensitive, KEY_SENSITIVE);
        bindLevelSeek(R.id.seek_follow, KEY_FOLLOW);
        bindLevelSeek(R.id.seek_micro, KEY_MICRO);

        // ---- network: WiFi low latency (global) ----
        wifiSwitch = findViewById(R.id.wifi_switch);
        wifiSwitch.setChecked(RootShell.getGlobalInt(KEY_WIFI_LOW_LATENCY, 0) == 1);
        wifiSwitch.setOnCheckedChangeListener((b, checked) -> {
            String res = RootShell.putGlobal(KEY_WIFI_LOW_LATENCY, checked ? "1" : "0");
            toastResult(res, KEY_WIFI_LOW_LATENCY + " = " + (checked ? 1 : 0));
        });

        // ---- display / light gamma pills (default/racing/shooting) ----
        gammaDefault = findViewById(R.id.gamma_default);
        gammaRacing = findViewById(R.id.gamma_racing);
        gammaShooting = findViewById(R.id.gamma_shooting);
        selectGamma(0);
        gammaDefault.setOnClickListener(v -> { selectGamma(0); applyDisplay("default"); });
        gammaRacing.setOnClickListener(v -> { selectGamma(1); applyDisplay("racing"); });
        gammaShooting.setOnClickListener(v -> { selectGamma(2); applyDisplay("shooting"); });

        rootStatus.setText(RootShell.isRootAvailable()
                ? "Root: доступ есть"
                : getString(R.string.root_required));
    }

    @Override protected void onResume() {
        super.onResume();
        ui.post(ticker);
    }

    @Override protected void onPause() {
        super.onPause();
        ui.removeCallbacks(ticker);
    }

    /** Reads the live CPU/GPU frequencies and updates the rings. */
    private void refreshRings() {
        long cpuCur = FreqReader.cpuCur();
        long cpuMax = FreqReader.cpuMax();
        String[] u = new String[1];
        String cpuVal = FreqReader.formatKHz(cpuCur, u);
        ringCpu.setData(cpuCur, cpuMax, cpuVal, u[0], getString(R.string.perf_cpu));

        long gpuCur = FreqReader.gpuCur();
        long gpuMax = FreqReader.gpuMax();
        String gpuVal = FreqReader.formatHz(gpuCur, u);
        ringGpu.setData(gpuCur, gpuMax, gpuVal, u[0], getString(R.string.perf_gpu));
    }

    private void bindLevelSeek(int seekId, final String key) {
        SeekBar sb = findViewById(seekId);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                int idx = s.getProgress();
                if (idx < 0) idx = 0;
                if (idx >= TOUCH_SCREEN_LEVEL.length) idx = TOUCH_SCREEN_LEVEL.length - 1;
                apply(key, new int[]{ TOUCH_SCREEN_LEVEL[idx] });
            }
        });
    }

    private void selectMode(int idx) {
        modeBalance.setSelected(idx == 0);
        modePerformance.setSelected(idx == 1);
        modeBeyond.setSelected(idx == 2);
    }

    private void selectGamma(int idx) {
        gammaDefault.setSelected(idx == 0);
        gammaRacing.setSelected(idx == 1);
        gammaShooting.setSelected(idx == 2);
    }

    /** Per-game int value -> exact GameSpace format (read-modify-write). */
    private void apply(String key, int[] values) {
        String pkg = requirePackage();
        if (pkg == null) return;
        String current = RootShell.getGlobal(key);
        String toSave = NubiaTouchDb.buildSaveString(current, pkg, values);
        toastResult(RootShell.putGlobal(key, toSave), key + " = " + NubiaTouchDb.encodeValue(values));
    }

    /** Per-game string value (display modes). */
    private void applyDisplay(String value) {
        String pkg = requirePackage();
        if (pkg == null) return;
        String current = RootShell.getGlobal(KEY_DISPLAY);
        String toSave = NubiaTouchDb.buildSaveString(current, pkg, value);
        toastResult(RootShell.putGlobal(KEY_DISPLAY, toSave), KEY_DISPLAY + " = " + value);
    }

    @Nullable
    private String requirePackage() {
        String pkg = targetPackage.getText().toString().trim();
        if (TextUtils.isEmpty(pkg)) {
            Toast.makeText(this, "Укажите пакет игры (например com.tencent.ig)", Toast.LENGTH_SHORT).show();
            return null;
        }
        return pkg;
    }

    private void toastResult(String result, String okMsg) {
        if (result != null && (result.startsWith("ERROR") || result.toLowerCase().contains("denied"))) {
            Toast.makeText(this, "Не удалось (нужен root)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, okMsg, Toast.LENGTH_SHORT).show();
        }
    }
}
