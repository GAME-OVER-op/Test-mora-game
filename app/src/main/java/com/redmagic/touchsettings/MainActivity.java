package com.redmagic.touchsettings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
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
 * Nothing is shown or written until the user types a full package name and
 * presses «Поиск». After that the current values for THAT package are loaded
 * and every control writes for that locked package.
 */
public class MainActivity extends AppCompatActivity {

    // ---- per-game keys (verified against TouchOperationBean$OperationTypeParams) ----
    private static final String KEY_SAMPLE_RATE = "NubiaperformanceTouchSampleRate";
    private static final String KEY_SENSITIVE   = "NubiaperformanceTouchSen";
    private static final String KEY_FOLLOW      = "NubiaperformanceTouchFollow";
    private static final String KEY_MICRO       = "NubiaperformanceTouchMicroSensitive";
    private static final String KEY_PERF_MODE   = "NubiaperformanceMode";            // 1/2/3
    private static final String KEY_DISPLAY     = "game_strengthen_mode_value";       // default/racing/shooting
    // ---- global keys ----
    private static final String KEY_WIFI_LOW_LATENCY = "gsc_wifi_low_latency_mode";   // 0/1
    // ---- global per-game list (pkg+index,pkg+index,...) ----
    private static final String KEY_STRENGTHEN_LIST = "db_game_strengthen_mode_list";

    // Real GameSpace performance modes (nubia_game_performance_mode_*):
    //   1 = Баланс, 2 = Подъем, 3 = За пределами
    private static final int MODE_BALANCE = 1;
    private static final int MODE_BOOST   = 2;
    private static final int MODE_BEYOND  = 3;

    // Touch sliders store one of {-2,-1,0,1,2} (TOUCH_SCREEN_LEVEL); SeekBar max=4, center=2.
    private static final int[] TOUCH_SCREEN_LEVEL = { -2, -1, 0, 1, 2 };

    private EditText targetPackage;
    private TextView searchButton;
    private TextView rootStatus;
    private TextView hintSearch;
    private View contentContainer;

    private TextView sampleHigh, sampleUltra;
    private TextView modeBalance, modeBoost, modeBeyond;
    private TextView gammaDefault, gammaRacing, gammaShooting;
    private SwitchCompat wifiSwitch;
    private SeekBar seekSensitive, seekFollow, seekMicro;
    private LinearLayout strengthenListContainer;

    private RingView ringCpu, ringGpu;

    /** Locked package; null until the user presses Поиск. No writes happen while null. */
    @Nullable private String currentPackage = null;

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
        searchButton = findViewById(R.id.search_button);
        rootStatus = findViewById(R.id.root_status);
        hintSearch = findViewById(R.id.hint_search);
        contentContainer = findViewById(R.id.content_container);

        ringCpu = findViewById(R.id.ring_cpu);
        ringGpu = findViewById(R.id.ring_gpu);

        modeBalance = findViewById(R.id.mode_balance);
        modeBoost = findViewById(R.id.mode_performance);   // middle pill = Подъем
        modeBeyond = findViewById(R.id.mode_beyond);
        modeBalance.setOnClickListener(v -> { selectMode(MODE_BALANCE); apply(KEY_PERF_MODE, new int[]{ MODE_BALANCE }); });
        modeBoost.setOnClickListener(v -> { selectMode(MODE_BOOST); apply(KEY_PERF_MODE, new int[]{ MODE_BOOST }); });
        modeBeyond.setOnClickListener(v -> { selectMode(MODE_BEYOND); apply(KEY_PERF_MODE, new int[]{ MODE_BEYOND }); });

        sampleHigh = findViewById(R.id.sample_high);
        sampleUltra = findViewById(R.id.sample_ultra);
        sampleHigh.setOnClickListener(v -> { selectSample(480); apply(KEY_SAMPLE_RATE, new int[]{ 480 }); });
        sampleUltra.setOnClickListener(v -> { selectSample(960); apply(KEY_SAMPLE_RATE, new int[]{ 960 }); });

        seekSensitive = findViewById(R.id.seek_sensitive);
        seekFollow = findViewById(R.id.seek_follow);
        seekMicro = findViewById(R.id.seek_micro);
        bindLevelSeek(seekSensitive, KEY_SENSITIVE);
        bindLevelSeek(seekFollow, KEY_FOLLOW);
        bindLevelSeek(seekMicro, KEY_MICRO);

        wifiSwitch = findViewById(R.id.wifi_switch);
        wifiSwitch.setOnCheckedChangeListener((b, checked) -> {
            if (currentPackage == null) return;
            String res = RootShell.putGlobal(KEY_WIFI_LOW_LATENCY, checked ? "1" : "0");
            toastResult(res, KEY_WIFI_LOW_LATENCY + " = " + (checked ? 1 : 0));
        });

        gammaDefault = findViewById(R.id.gamma_default);
        gammaRacing = findViewById(R.id.gamma_racing);
        gammaShooting = findViewById(R.id.gamma_shooting);
        gammaDefault.setOnClickListener(v -> { selectGamma("default"); applyDisplay("default"); });
        gammaRacing.setOnClickListener(v -> { selectGamma("racing"); applyDisplay("racing"); });
        gammaShooting.setOnClickListener(v -> { selectGamma("shooting"); applyDisplay("shooting"); });

        // search button + keyboard "search" action both trigger the load
        searchButton.setOnClickListener(v -> doSearch());
        targetPackage.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); return true; }
            return false;
        });

        // global db_game_strengthen_mode_list editor (independent of the searched package)
        strengthenListContainer = findViewById(R.id.strengthen_list_container);
        findViewById(R.id.strengthen_list_add).setOnClickListener(v -> addStrengthenRow("", "0"));
        findViewById(R.id.strengthen_list_save).setOnClickListener(v -> saveStrengthenList());
        loadStrengthenList();

        rootStatus.setText(RootShell.isRootAvailable()
                ? "Root: доступ есть"
                : getString(R.string.root_required));
    }

    /** Validates the package, reveals the panels and loads the saved values. */
    private void doSearch() {
        String pkg = targetPackage.getText().toString().trim();
        if (TextUtils.isEmpty(pkg) || !pkg.contains(".")) {
            Toast.makeText(this, R.string.msg_enter_package, Toast.LENGTH_SHORT).show();
            return;
        }
        currentPackage = pkg;
        hintSearch.setVisibility(View.GONE);
        contentContainer.setVisibility(View.VISIBLE);
        loadValuesForPackage(pkg);
        ui.removeCallbacks(ticker);
        ui.post(ticker);
        Toast.makeText(this, getString(R.string.msg_loaded, pkg), Toast.LENGTH_SHORT).show();
    }

    /** Reads the saved per-game values and reflects them in the UI (no writes here). */
    private void loadValuesForPackage(String pkg) {
        // performance mode (default = Баланс)
        int mode = NubiaTouchDb.parse(RootShell.getGlobal(KEY_PERF_MODE), pkg, new int[]{ MODE_BALANCE })[0];
        selectMode(mode);

        // sample rate (default 480)
        int sample = NubiaTouchDb.parse(RootShell.getGlobal(KEY_SAMPLE_RATE), pkg, new int[]{ 480 })[0];
        selectSample(sample);

        // -2..2 sliders (default 0 -> progress 2)
        setSeekFromLevel(seekSensitive, NubiaTouchDb.parse(RootShell.getGlobal(KEY_SENSITIVE), pkg, new int[]{ 0 })[0]);
        setSeekFromLevel(seekFollow, NubiaTouchDb.parse(RootShell.getGlobal(KEY_FOLLOW), pkg, new int[]{ 0 })[0]);
        setSeekFromLevel(seekMicro, NubiaTouchDb.parse(RootShell.getGlobal(KEY_MICRO), pkg, new int[]{ 0 })[0]);

        // light gamma (default)
        selectGamma(NubiaTouchDb.parseString(RootShell.getGlobal(KEY_DISPLAY), pkg, "default"));

        // wifi low latency is GLOBAL (not per-game)
        wifiSwitch.setOnCheckedChangeListener(null);
        wifiSwitch.setChecked(RootShell.getGlobalInt(KEY_WIFI_LOW_LATENCY, 0) == 1);
        wifiSwitch.setOnCheckedChangeListener((b, checked) -> {
            String res = RootShell.putGlobal(KEY_WIFI_LOW_LATENCY, checked ? "1" : "0");
            toastResult(res, KEY_WIFI_LOW_LATENCY + " = " + (checked ? 1 : 0));
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (currentPackage != null) ui.post(ticker);
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

    private void bindLevelSeek(SeekBar sb, final String key) {
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                if (currentPackage == null) return;
                int idx = s.getProgress();
                if (idx < 0) idx = 0;
                if (idx >= TOUCH_SCREEN_LEVEL.length) idx = TOUCH_SCREEN_LEVEL.length - 1;
                apply(key, new int[]{ TOUCH_SCREEN_LEVEL[idx] });
            }
        });
    }

    private void setSeekFromLevel(SeekBar sb, int level) {
        int progress = 2; // default center (level 0)
        for (int i = 0; i < TOUCH_SCREEN_LEVEL.length; i++) {
            if (TOUCH_SCREEN_LEVEL[i] == level) { progress = i; break; }
        }
        sb.setProgress(progress);
    }

    private void selectMode(int mode) {
        modeBalance.setSelected(mode == MODE_BALANCE);
        modeBoost.setSelected(mode == MODE_BOOST);
        modeBeyond.setSelected(mode == MODE_BEYOND);
    }

    private void selectSample(int rate) {
        sampleHigh.setSelected(rate != 960);
        sampleUltra.setSelected(rate == 960);
    }

    private void selectGamma(String value) {
        gammaDefault.setSelected("default".equals(value));
        gammaRacing.setSelected("racing".equals(value));
        gammaShooting.setSelected("shooting".equals(value));
    }

    // ---------- db_game_strengthen_mode_list (global, extendable) ----------

    /** Reads the global list "pkg+index,pkg+index," and renders one editable row per entry. */
    private void loadStrengthenList() {
        strengthenListContainer.removeAllViews();
        String raw = RootShell.getGlobal(KEY_STRENGTHEN_LIST);
        if (!TextUtils.isEmpty(raw)) {
            for (String entry : raw.split(",")) {
                if (TextUtils.isEmpty(entry) || !entry.contains("+")) continue;
                int p = entry.indexOf("+");
                addStrengthenRow(entry.substring(0, p), entry.substring(p + 1));
            }
        }
        if (strengthenListContainer.getChildCount() == 0) addStrengthenRow("", "0");
    }

    /** Adds one editable row: package (text) + index (number) + remove button. */
    private void addStrengthenRow(String pkg, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(8);
        row.setLayoutParams(rowLp);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.card_bg);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        EditText pkgEt = new EditText(this);
        pkgEt.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        pkgEt.setHint("com.example.game");
        pkgEt.setText(pkg);
        pkgEt.setTextColor(0xFFFFFFFF);
        pkgEt.setHintTextColor(0x66FFFFFF);
        pkgEt.setTextSize(14);
        pkgEt.setTypeface(pkgEt.getTypeface(), android.graphics.Typeface.BOLD);
        pkgEt.setShadowLayer(2f, 0f, 2f, 0xFF000000);
        pkgEt.setBackgroundColor(0x00000000);
        pkgEt.setSingleLine(true);
        pkgEt.setTag("pkg");

        EditText valEt = new EditText(this);
        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(dp(56), LinearLayout.LayoutParams.WRAP_CONTENT);
        valLp.setMarginStart(dp(8));
        valEt.setLayoutParams(valLp);
        valEt.setText(value);
        valEt.setTextColor(0xCCFFFFFF);
        valEt.setTextSize(13);
        valEt.setGravity(android.view.Gravity.CENTER);
        valEt.setBackgroundColor(0x00000000);
        valEt.setSingleLine(true);
        valEt.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        valEt.setTag("val");

        TextView del = new TextView(this);
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        delLp.setMarginStart(dp(8));
        del.setLayoutParams(delLp);
        del.setText("✕");
        del.setGravity(android.view.Gravity.CENTER);
        del.setTextColor(0xFFE60012);
        del.setTextSize(16);
        del.setOnClickListener(v -> strengthenListContainer.removeView(row));

        row.addView(pkgEt);
        row.addView(valEt);
        row.addView(del);
        strengthenListContainer.addView(row);
    }

    /** Rebuilds "pkg+index," from the rows and writes it back to the global setting. */
    private void saveStrengthenList() {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < strengthenListContainer.getChildCount(); i++) {
            View child = strengthenListContainer.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) child;
            EditText pkgEt = row.findViewWithTag("pkg");
            EditText valEt = row.findViewWithTag("val");
            if (pkgEt == null || valEt == null) continue;
            String pkg = pkgEt.getText().toString().trim();
            String val = valEt.getText().toString().trim();
            if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(val)) continue;
            sb.append(pkg).append("+").append(val).append(",");
            count++;
        }
        toastResult(RootShell.putGlobal(KEY_STRENGTHEN_LIST, sb.toString()),
                getString(R.string.strengthen_list_saved, count));
    }

    /** dp -> px helper for the dynamically built rows. */
    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    /** Per-game int value -> exact GameSpace format (read-modify-write). */
    private void apply(String key, int[] values) {
        if (currentPackage == null) { needSearch(); return; }
        String current = RootShell.getGlobal(key);
        String toSave = NubiaTouchDb.buildSaveString(current, currentPackage, values);
        toastResult(RootShell.putGlobal(key, toSave), key + " = " + NubiaTouchDb.encodeValue(values));
    }

    /** Per-game string value (display modes). */
    private void applyDisplay(String value) {
        if (currentPackage == null) { needSearch(); return; }
        String current = RootShell.getGlobal(KEY_DISPLAY);
        String toSave = NubiaTouchDb.buildSaveString(current, currentPackage, value);
        toastResult(RootShell.putGlobal(KEY_DISPLAY, toSave), KEY_DISPLAY + " = " + value);
    }

    private void needSearch() {
        Toast.makeText(this, R.string.msg_enter_package, Toast.LENGTH_SHORT).show();
    }

    private void toastResult(String result, String okMsg) {
        if (result != null && (result.startsWith("ERROR") || result.toLowerCase().contains("denied"))) {
            Toast.makeText(this, "Не удалось (нужен root)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, okMsg, Toast.LENGTH_SHORT).show();
        }
    }
}
