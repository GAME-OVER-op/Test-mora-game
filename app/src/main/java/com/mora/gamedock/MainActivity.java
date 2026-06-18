package com.mora.gamedock;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final RootShell root = new RootShell();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean running = true;

    private CircleGaugeView cpuGauge, gpuGauge;
    private TextView batteryText, tempText, rootStatus;
    private final TextView[] perfButtons = new TextView[4];
    private int perfSelected = 1;

    private long lastTotal = 0, lastIdle = 0;

    // sysfs RedMagic 9 Pro (SD8Gen3 / Adreno 750)
    private static final String CPU0 = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    private static final String CPU7 = "/sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq";
    private static final String GPU_BUSY = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage";
    private static final String GPU_CLK = "/sys/class/kgsl/kgsl-3d0/gpuclk";
    private static final String GPU_DEVFREQ = "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq";
    private static final String BATT_CAP = "/sys/class/power_supply/battery/capacity";
    private static final String BATT_TEMP = "/sys/class/power_supply/battery/temp";

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);

        cpuGauge = findViewById(R.id.cpuGauge);
        gpuGauge = findViewById(R.id.gpuGauge);
        cpuGauge.setLabel("CPU");
        gpuGauge.setLabel("GPU");
        batteryText = findViewById(R.id.batteryText);
        tempText = findViewById(R.id.tempText);
        rootStatus = findViewById(R.id.rootStatus);

        setupPerfButtons();

        RecyclerView rv = findViewById(R.id.tileGrid);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        rv.setAdapter(new TileAdapter(defaultTiles()));

        startMonitor();
    }

    private void setupPerfButtons() {
        LinearLayout row = findViewById(R.id.perfRow);
        String[] names = {"\u042d\u043a\u043e", "\u0411\u0430\u043b\u0430\u043d\u0441", "\u0412\u044b\u0441\u043e\u043a\u0438\u0439", "\u041c\u043e\u043d\u0441\u0442\u0440"};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            TextView b = new TextView(this);
            b.setText(names[i]);
            b.setTextColor(Color.WHITE);
            b.setTextSize(13);
            b.setPadding(30, 14, 30, 14);
            b.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(8, 0, 8, 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> selectPerf(idx));
            perfButtons[i] = b;
            row.addView(b);
        }
        selectPerf(perfSelected);
    }

    private void selectPerf(int idx) {
        perfSelected = idx;
        for (int i = 0; i < perfButtons.length; i++) {
            perfButtons[i].setBackgroundColor(i == idx ? 0xFFE93363 : 0xFF1E1E1E);
        }
        // TODO mora: запись режима, например:
        // root.run("echo " + idx + " > /sys/.../perf_mode");
    }

    private List<Tile> defaultTiles() {
        List<Tile> t = new ArrayList<>();
        t.add(new Tile("\u0423\u0441\u043a\u043e\u0440\u0435\u043d\u0438\u0435", "\u26a1", true));
        t.add(new Tile("\u041d\u0435 \u0431\u0435\u0441\u043f\u043e\u043a\u043e\u0438\u0442\u044c", "\ud83d\udd15", false));
        t.add(new Tile("\u0411\u043b\u043e\u043a \u043a\u0430\u0441\u0430\u043d\u0438\u0439", "\u270b", false));
        t.add(new Tile("\u0421\u0435\u0442\u044c", "\ud83d\udcf6", true));
        t.add(new Tile("\u0417\u0430\u043f\u0438\u0441\u044c", "\u23fa", false));
        t.add(new Tile("\u0421\u043a\u0440\u0438\u043d\u0448\u043e\u0442", "\ud83d\udcf7", false));
        t.add(new Tile("4D \u0432\u0438\u0431\u0440\u043e", "\ud83d\udcf3", true));
        t.add(new Tile("\u0421\u0432\u0435\u0442", "\ud83d\udca1", false));
        t.add(new Tile("\u0424\u0438\u043b\u044c\u0442\u0440", "\ud83c\udfa8", false));
        return t;
    }

    private void startMonitor() {
        new Thread(() -> {
            root.start();
            while (running) {
                final Metrics m = readMetrics();
                ui.post(() -> updateUi(m));
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
            }
        }).start();
    }

    private void updateUi(Metrics m) {
        rootStatus.setText(root.available ? "\u25cf LIVE (root)" : "\u25cf \u043d\u0435\u0442 root \u2014 \u0447\u0430\u0441\u0442\u0438\u0447\u043d\u044b\u0435 \u0434\u0430\u043d\u043d\u044b\u0435");
        rootStatus.setTextColor(root.available ? 0xFF4CD964 : 0xFFFFB020);
        cpuGauge.setProgress((float) m.cpuLoad, String.format(Locale.US, "%.2f GHz", m.cpuFreqGhz));
        gpuGauge.setProgress((float) m.gpuBusy, Math.round(m.gpuFreqMhz) + " MHz");
        batteryText.setText(m.battery + "%");
        tempText.setText(String.format(Locale.US, "%.1f\u00b0C", m.tempC));
    }

    private String readS(String path) {
        return root.available ? root.cat(path) : Sysfs.readString(path);
    }

    private long readL(String path, long def) {
        return root.available ? root.catLong(path, def) : Sysfs.readLong(path, def);
    }

    private Metrics readMetrics() {
        Metrics m = new Metrics();

        String stat = readS("/proc/stat");
        if (stat != null) {
            for (String line : stat.split("\n")) {
                if (line.startsWith("cpu ") || line.startsWith("cpu\t")) {
                    String[] p = line.trim().split("\\s+");
                    try {
                        long total = 0;
                        for (int i = 1; i < p.length; i++) total += Long.parseLong(p[i]);
                        long idle = Long.parseLong(p[4]) + (p.length > 5 ? Long.parseLong(p[5]) : 0);
                        long dT = total - lastTotal, dI = idle - lastIdle;
                        if (lastTotal != 0 && dT > 0) m.cpuLoad = (dT - dI) / (double) dT;
                        lastTotal = total;
                        lastIdle = idle;
                    } catch (Exception ignored) {}
                    break;
                }
            }
        }

        long f = readL(CPU7, 0);
        if (f <= 0) f = readL(CPU0, 0);
        m.cpuFreqGhz = f / 1_000_000.0;

        String gb = readS(GPU_BUSY);
        if (gb != null && !gb.isEmpty()) {
            try { m.gpuBusy = Integer.parseInt(gb.trim().split("\\s+")[0]) / 100.0; } catch (Exception ignored) {}
        }

        long g = readL(GPU_CLK, 0);
        if (g <= 0) g = readL(GPU_DEVFREQ, 0);
        m.gpuFreqMhz = g / 1_000_000.0;

        m.battery = (int) readL(BATT_CAP, 0);
        long temp = readL(BATT_TEMP, 0);
        m.tempC = temp > 1000 ? temp / 1000.0 : temp / 10.0;

        return m;
    }

    @Override
    protected void onDestroy() {
        running = false;
        root.stop();
        super.onDestroy();
    }
}
