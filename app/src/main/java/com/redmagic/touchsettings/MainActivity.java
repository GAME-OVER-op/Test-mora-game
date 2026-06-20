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
 * Writes the SAME Settings.Global keys, in the SAME per-game encoding, that the
 * original GameSpace control panel uses (see NubiaTouchDb), so GameSpace itself
 * reads the values back correctly instead of resetting them to 0.
 *
 * Format per key: "<package>+<value>," comma-separated list; multi-int values
 * (gyro X/Y, edge-protection switch+range) are joined with '&'.
 */
public class MainActivity extends AppCompatActivity {

    // Settings.Global keys (verified against TouchOperationBean$OperationTypeParams).
    private static final String KEY_SAMPLE_RATE = "NubiaperformanceTouchSampleRate";
    private static final String KEY_SENSITIVE   = "NubiaperformanceTouchSen";
    private static final String KEY_FOLLOW      = "NubiaperformanceTouchFollow";
    private static final String KEY_MICRO       = "NubiaperformanceTouchMicroSensitive";
    private static final String KEY_GYRO        = "NubiaperformanceGyroSen";
    private static final String KEY_PROTECT     = "PerformanceTouchProtectLev";

    // Touch sliders store one of {-2,-1,0,1,2} (TOUCH_SCREEN_LEVEL); SeekBar max=4, center=2.
    private static final int[] TOUCH_SCREEN_LEVEL = { -2, -1, 0, 1, 2 };

    private EditText targetPackage;
    private TextView rootStatus;

    private TextView sampleHigh, sampleUltra;
    private TextView rangeSmall, rangeMiddle, rangeBig;
    private TextView gyroXValue, gyroYValue;
    private SwitchCompat preventSwitch;
    private int protectRange = 0; // 0=small,1=middle,2=big

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

        // ---- sample rate pills (stored value = Hz: 480 / 960) ----
        sampleHigh.setSelected(true);
        sampleHigh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                sampleHigh.setSelected(true);
                sampleUltra.setSelected(false);
                apply(KEY_SAMPLE_RATE, new int[]{ 480 });
            }
        });
        sampleUltra.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                sampleHigh.setSelected(false);
                sampleUltra.setSelected(true);
                apply(KEY_SAMPLE_RATE, new int[]{ 960 });
            }
        });

        // ---- -2..2 sliders (max=4, center=2) ----
        bindLevelSeek(R.id.seek_sensitive, KEY_SENSITIVE);
        bindLevelSeek(R.id.seek_follow, KEY_FOLLOW);
        bindLevelSeek(R.id.seek_micro, KEY_MICRO);

        // ---- gyro X / Y -> single key, value "x&y" ----
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
                apply(KEY_GYRO, new int[]{ x, y });
            }
        };
        gyroX.setOnSeekBarChangeListener(gyroListener);
        gyroY.setOnSeekBarChangeListener(gyroListener);

        // ---- edge protection: value "<switch>&<range>" ----
        preventSwitch.setChecked(true);
        preventSwitch.setOnCheckedChangeListener((b, checked) -> applyProtect());
        selectRange(0);
        rangeSmall.setOnClickListener(v -> { selectRange(0); applyProtect(); });
        rangeMiddle.setOnClickListener(v -> { selectRange(1); applyProtect(); });
        rangeBig.setOnClickListener(v -> { selectRange(2); applyProtect(); });

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
                int idx = s.getProgress();
                if (idx < 0) idx = 0;
                if (idx >= TOUCH_SCREEN_LEVEL.length) idx = TOUCH_SCREEN_LEVEL.length - 1;
                apply(key, new int[]{ TOUCH_SCREEN_LEVEL[idx] });
            }
        });
    }

    private void selectRange(int idx) {
        protectRange = idx;
        rangeSmall.setSelected(idx == 0);
        rangeMiddle.setSelected(idx == 1);
        rangeBig.setSelected(idx == 2);
    }

    private void applyProtect() {
        int on = preventSwitch.isChecked() ? 1 : 0;
        apply(KEY_PROTECT, new int[]{ on, protectRange });
    }

    /**
     * Read-modify-write the per-game value in the exact GameSpace format, then
     * persist it as root. Requires a target package (the original never writes a
     * value without one).
     */
    private void apply(String key, int[] values) {
        String pkg = targetPackage.getText().toString().trim();
        if (TextUtils.isEmpty(pkg)) {
            Toast.makeText(this, "Укажите пакет игры (например com.tencent.ig)", Toast.LENGTH_SHORT).show();
            return;
        }
        String current = RootShell.getGlobal(key);
        String toSave = NubiaTouchDb.buildSaveString(current, pkg, values);
        String result = RootShell.putGlobal(key, toSave);
        if (result.startsWith("ERROR") || result.toLowerCase().contains("denied")) {
            Toast.makeText(this, "Не удалось (нужен root): " + key, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, key + " = " + NubiaTouchDb.encodeValue(values), Toast.LENGTH_SHORT).show();
        }
    }
}
