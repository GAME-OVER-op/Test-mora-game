package com.redmagic.touchsettings;

import android.os.Bundle;
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
 * Standalone replica of the RedMagic / nubia Game Space
 * "Touch screen parameter adjustment" settings screen.
 *
 * It writes the same Settings.Global keys the original GameSpace panel writes:
 *   NubiaperformanceTouchSampleRate, NubiaperformanceTouchSen,
 *   NubiaperformanceTouchFollow, NubiaperformanceTouchMicroSensitive,
 *   NubiaperformanceGyroSen, plus the edge-protection (TouchProtectOpen) value.
 *
 * Values are stored per game as "<package>@<value>" when a target package is set,
 * matching the original format. Writing requires root (handled by RootShell).
 */
public class MainActivity extends AppCompatActivity {

    // Settings.Global keys used by the original GameSpace control panel.
    private static final String KEY_SAMPLE_RATE = "NubiaperformanceTouchSampleRate";
    private static final String KEY_SENSITIVE   = "NubiaperformanceTouchSen";
    private static final String KEY_FOLLOW      = "NubiaperformanceTouchFollow";
    private static final String KEY_MICRO       = "NubiaperformanceTouchMicroSensitive";
    private static final String KEY_GYRO        = "NubiaperformanceGyroSen";
    private static final String KEY_PROTECT     = "TouchProtectOpen";

    private EditText targetPackage;
    private TextView rootStatus;

    private TextView sampleHigh, sampleUltra;
    private TextView rangeSmall, rangeMiddle, rangeBig;
    private TextView gyroXValue, gyroYValue;
    private SwitchCompat preventSwitch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        targetPackage = findViewById(R.id.target_package);
        rootStatus = findViewById(R.id.root_status);

        sampleHigh = findViewById(R.id.sample_high);
        sampleUltra = findViewById(R.id.sample_ultra);
        rangeSmall = findViewById(R.id.range_small);
        rangeMiddle = findViewById(R.id.range_middle);
        rangeBig = findViewById(R.id.range_big);
        gyroXValue = findViewById(R.id.gyro_x_value);
        gyroYValue = findViewById(R.id.gyro_y_value);
        preventSwitch = findViewById(R.id.prevent_touch_switch);

        // ---- sample rate pills ----
        sampleHigh.setSelected(true);
        sampleHigh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                sampleHigh.setSelected(true);
                sampleUltra.setSelected(false);
                apply(KEY_SAMPLE_RATE, "480");
            }
        });
        sampleUltra.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                sampleHigh.setSelected(false);
                sampleUltra.setSelected(true);
                apply(KEY_SAMPLE_RATE, "960");
            }
        });

        // ---- -2..2 sliders (max=4, center=2) ----
        bindLevelSeek(R.id.seek_sensitive, KEY_SENSITIVE);
        bindLevelSeek(R.id.seek_follow, KEY_FOLLOW);
        bindLevelSeek(R.id.seek_micro, KEY_MICRO);

        // ---- gyro X / Y (0..200 %) -> single combined key "x,y" ----
        SeekBar gyroX = findViewById(R.id.seek_gyro_x);
        SeekBar gyroY = findViewById(R.id.seek_gyro_y);
        SeekBar.OnSeekBarChangeListener gyroListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                gyroXValue.setText(((SeekBar) findViewById(R.id.seek_gyro_x)).getProgress() + "%");
                gyroYValue.setText(((SeekBar) findViewById(R.id.seek_gyro_y)).getProgress() + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {
                int x = ((SeekBar) findViewById(R.id.seek_gyro_x)).getProgress();
                int y = ((SeekBar) findViewById(R.id.seek_gyro_y)).getProgress();
                apply(KEY_GYRO, x + "," + y);
            }
        };
        gyroX.setOnSeekBarChangeListener(gyroListener);
        gyroY.setOnSeekBarChangeListener(gyroListener);

        // ---- edge protection ----
        preventSwitch.setChecked(true);
        preventSwitch.setOnCheckedChangeListener((b, checked) ->
                apply(KEY_PROTECT, checked ? "1" : "0"));
        selectRange(0);
        rangeSmall.setOnClickListener(v -> selectRangeAndApply(0));
        rangeMiddle.setOnClickListener(v -> selectRangeAndApply(1));
        rangeBig.setOnClickListener(v -> selectRangeAndApply(2));

        rootStatus.setText(RootShell.isRootAvailable()
                ? "Root: доступ есть"
                : getString(R.string.root_required));
    }

    private void bindLevelSeek(int seekId, final String key) {
        SeekBar sb = findViewById(seekId);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                int level = s.getProgress() - 2; // -2..2
                apply(key, String.valueOf(level));
            }
        });
    }

    private void selectRange(int idx) {
        rangeSmall.setSelected(idx == 0);
        rangeMiddle.setSelected(idx == 1);
        rangeBig.setSelected(idx == 2);
    }

    private void selectRangeAndApply(int idx) {
        selectRange(idx);
        apply(KEY_PROTECT + "Range", String.valueOf(idx));
    }

    /** Builds "<pkg>@<value>" when a target package is set, then writes it as root. */
    private void apply(String key, String value) {
        String pkg = targetPackage.getText().toString().trim();
        String payload = TextUtils.isEmpty(pkg) ? value : pkg + "@" + value;
        String result = RootShell.putGlobal(key, payload);
        if (result.startsWith("ERROR") || result.toLowerCase().contains("not found")
                || result.toLowerCase().contains("denied")) {
            Toast.makeText(this, "Не удалось (нужен root): " + key, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, key + " = " + payload, Toast.LENGTH_SHORT).show();
        }
    }
}
