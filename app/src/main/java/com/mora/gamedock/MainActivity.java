package com.mora.gamedock;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final RootShell root = new RootShell();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean running = true;

    private GlowRingView cpuRing, gpuRing;
    private FrameBorderView frame;
    private TextView cpuFreq, gpuFreq, batteryText, tempText, rootStatus, perfButton;

    private long lastTotal = 0, lastIdle = 0;

    // Режимы RedMagic и их цвета: Баланс (голубой), Подъём (жёлтый), За пределами (красный)
    private static final String[] MODE_NAMES = {"\u0411\u0430\u043b\u0430\u043d\u0441", "\u041f\u043e\u0434\u044a\u0451\u043c", "\u0417\u0430 \u043f\u0440\u0435\u0434\u0435\u043b\u0430\u043c\u0438"};
    private static final int[] MODE_COLORS = {0xFF2EA6FF, 0xFFFFC107, 0xFFFF2D46};
    private int mode = 2; // по умолчанию красный (как на скрине стока)

    // sysfs RedMagic (SD8Gen3 / Adreno 750)
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

        cpuRing = findViewById(R.id.cpuRing);
        gpuRing = findViewById(R.id.gpuRing);
        frame = findViewById(R.id.frame);

        cpuFreq = findViewById(R.id.cpuFreq);
        gpuFreq = findViewById(R.id.gpuFreq);
        batteryText = findViewById(R.id.batteryText);
        tempText = findViewById(R.id.tempText);
        rootStatus = findViewById(R.id.rootStatus);
        perfButton = findViewById(R.id.perfButton);
        perfButton.setOnClickListener(v -> applyMode((mode + 1) % MODE_NAMES.length));

        applyMode(mode);
        startMonitor();
    }

    private void applyMode(int m) {
        mode = m;
        int accent = MODE_COLORS[m];
        perfButton.setText(MODE_NAMES[m]);
        cpuRing.setAccent(accent);
        gpuRing.setAccent(accent);
        frame.setAccent(accent);

        float density = getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(8 * density);
        bg.setStroke((int) (1.5f * density), accent);
        bg.setColor((accent & 0x00FFFFFF) | (0x33 << 24));
        perfButton.setBackground(bg);
        // TODO mora: запись режима в систему / perf_daemon.
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
        rootStatus.setText(root.available ? "\u25cf LIVE (root)" : "\u25cf \u043d\u0435\u0442 root");
        rootStatus.setTextColor(root.available ? 0xFF4CD964 : 0xFFFFB020);
        cpuFreq.setText(String.format(Locale.US, "%.2f", m.cpuFreqGhz));
        gpuFreq.setText(String.valueOf(Math.round(m.gpuFreqMhz)));
        cpuRing.setProgress((float) m.cpuLoad);
        gpuRing.setProgress((float) m.gpuBusy);
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
