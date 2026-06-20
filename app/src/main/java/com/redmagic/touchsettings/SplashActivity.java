package com.redmagic.touchsettings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Plays the GameSpace intro video (res/raw/start_animation.mp4) on launch, then
 * opens MainActivity. Tap anywhere to skip; any error also forwards immediately.
 */
public class SplashActivity extends AppCompatActivity {

    private boolean forwarded = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        VideoView video = findViewById(R.id.intro_video);
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.start_animation);
        video.setVideoURI(uri);

        video.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            try { mp.setVolume(1f, 1f); } catch (Exception ignored) {}
            video.start();
        });
        video.setOnCompletionListener(mp -> goNext());
        video.setOnErrorListener((mp, what, extra) -> { goNext(); return true; });

        findViewById(R.id.intro_root).setOnClickListener(v -> goNext());
    }

    private void goNext() {
        if (forwarded) return;
        forwarded = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
