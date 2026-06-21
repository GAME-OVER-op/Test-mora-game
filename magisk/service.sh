#!/system/bin/sh
# Mora module boot service.
PKG="com.mora.gamespace"

# --- wait for full boot ---
while [ "$(getprop sys.boot_completed)" != "1" ]; do
  sleep 5
done
sleep 5

# --- system tweaks (kept from original) ---
setprop debug.graphics.game_default_frame_rate.disabled true
iw dev wlan0 set power_save off 2>/dev/null

# --- COOLER bootstrap: make sure the fan can actually spin ---
# Master cooler switch defaults ON if it has never been set.
if [ -z "$(getprop persist.perf.fan.status)" ]; then
  resetprop -n persist.perf.fan.status 1 2>/dev/null || setprop persist.perf.fan.status 1
fi
# Kernel-level Nubia/RedMagic fan enable flag.
settings put global nubia_parts_fan_enable 1 2>/dev/null
# Relax DAC perms on the fan nodes (SELinux is handled by sepolicy.rule).
for N in /sys/kernel/fan/fan_enable /sys/kernel/fan/fan_speed_level; do
  [ -e "$N" ] && chmod 0666 "$N" 2>/dev/null
done

# --- frequency / gpu sysfs nodes readable for the app rings ---
for N in \
  /sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq \
  /sys/devices/system/cpu/cpu7/cpufreq/cpuinfo_max_freq \
  /sys/class/kgsl/kgsl-3d0/gpuclk \
  /sys/class/kgsl/kgsl-3d0/max_gpuclk ; do
  [ -e "$N" ] && chmod 0644 "$N" 2>/dev/null
done

# --- grant our app every permission / appop it needs ---
grant_app() {
  pm path "$1" >/dev/null 2>&1 || return 0
  pm grant "$1" android.permission.WRITE_SECURE_SETTINGS 2>/dev/null
  pm grant "$1" android.permission.PACKAGE_USAGE_STATS 2>/dev/null
  appops set "$1" WRITE_SETTINGS allow 2>/dev/null
  appops set "$1" SYSTEM_ALERT_WINDOW allow 2>/dev/null
  appops set "$1" GET_USAGE_STATS allow 2>/dev/null
  cmd appops set "$1" android:system_alert_window allow 2>/dev/null
  dumpsys deviceidle whitelist +"$1" >/dev/null 2>&1
}
grant_app "$PKG"
grant_app "$PKG.debug"

# --- make the user avatar readable for the HUD (fallback handled by app) ---
PHOTO="/data/system/users/0/photo.png"
if [ -e "$PHOTO" ]; then
  chmod o+x /data/system /data/system/users /data/system/users/0 2>/dev/null
  chmod 0644 "$PHOTO" 2>/dev/null
fi

# --- start the daemon ---
setsid perf_daemon >/dev/null 2>&1 &
