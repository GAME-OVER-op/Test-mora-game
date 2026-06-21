#!/system/bin/sh
# ---------------------------------------------------------------------------
# GameSpace (com.mora.gamespace) permission bootstrap.
# Grants ONLY our app what it needs to read/write the Nubia/RedMagic game
# tuning settings and read the CPU/GPU frequency sysfs nodes.
# Run once as root:  su -c 'sh /sdcard/grant_gamespace_permissions.sh'
# A proper persistent SELinux policy (sepolicy.rule) will be written later.
# ---------------------------------------------------------------------------
PKG="com.mora.gamespace"
PKG_DEBUG="com.mora.gamespace.debug"

grant_for() {
  local p="$1"
  pm path "$p" >/dev/null 2>&1 || return 0
  echo "[*] granting permissions to $p"
  # Allow writing Settings.Global (NubiaperformanceMode, touch keys, game_strengthen_mode_*)
  pm grant "$p" android.permission.WRITE_SECURE_SETTINGS 2>/dev/null
  appops set "$p" WRITE_SETTINGS allow 2>/dev/null
  # Draw-over-other-apps: required for the trigger calibration overlay window
  appops set "$p" SYSTEM_ALERT_WINDOW allow 2>/dev/null
  cmd appops set "$p" android:system_alert_window allow 2>/dev/null
  # Usage access (read foreground game), ignore battery optimisation
  appops set "$p" GET_USAGE_STATS allow 2>/dev/null
  dumpsys deviceidle whitelist +"$p" >/dev/null 2>&1
}

grant_for "$PKG"
grant_for "$PKG_DEBUG"

# Make the frequency sysfs nodes world-readable so the rings work without su.
# (Temporary until the sepolicy.rule is in place; survives until reboot.)
for NODE in \
  /sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq \
  /sys/devices/system/cpu/cpu7/cpufreq/cpuinfo_max_freq \
  /sys/class/kgsl/kgsl-3d0/gpuclk \
  /sys/class/kgsl/kgsl-3d0/max_gpuclk \
  /sys/kernel/fan/fan_speed_level \
  /sys/kernel/fan/fan_enable ; do
  [ -e "$NODE" ] && chmod 0644 "$NODE" 2>/dev/null && echo "[*] chmod 0644 $NODE"
done

# Make the current user's avatar readable so the HUD can show it (fallback: rm_logo).
# /data/system needs traverse (o+x) for a non-system app to reach the file.
PHOTO="/data/system/users/0/photo.png"
if [ -e "$PHOTO" ]; then
  chmod o+x /data/system /data/system/users /data/system/users/0 2>/dev/null
  chmod 0644 "$PHOTO" 2>/dev/null && echo "[*] chmod 0644 $PHOTO"
else
  echo "[i] no user avatar at $PHOTO (app will use rm_logo)"
fi

echo "[+] done. Reboot is NOT required; re-run after each reboot until sepolicy.rule is installed."
