// Color settings + battery-saver master switch live in system properties so the
// non-root Android app can read and write them directly (mirroring the games /
// fan props). The daemon merges them into the live UserConfig.
//
//   persist.perf.saver.status  = 1|0
//   persist.perf.led.charging  = <enabled>;<fan>;<ext>
//   persist.perf.led.notif     = <enabled>;<stopkind>;<seconds>;<ext>
//   persist.perf.led.normal    = <fan>;<ext>
//   persist.perf.led.gaming    = <fan>;<ext>
//
// Tokens:
//   <fan> = "-" (none) or "<modecode>.<colorcode>"
//   <ext> = "-" (none) or "<modecode>.<colorcode>"
//   <stopkind> = 0 (until_screen_on) | 1 (for_seconds)

use std::process::Command;

use crate::user_config::{
    ChargingConfig, ExternalLedColor, ExternalLedMode, ExternalLedSetting, FanLedColor,
    FanLedMode, FanLedSetting, NotificationsConfig, NotificationsStopKind, ProfileType,
    UserConfig,
};

pub const PROP_SAVER: &str = "persist.perf.saver.status";
pub const PROP_CHARGING: &str = "persist.perf.led.charging";
pub const PROP_NOTIF: &str = "persist.perf.led.notif";
pub const PROP_NORMAL: &str = "persist.perf.led.normal";
pub const PROP_GAMING: &str = "persist.perf.led.gaming";

fn getprop(name: &str) -> Option<String> {
    let out = Command::new("getprop").arg(name).output().ok()?;
    let s = String::from_utf8_lossy(&out.stdout).trim().to_string();
    if s.is_empty() {
        None
    } else {
        Some(s)
    }
}

fn setprop(name: &str, value: &str) {
    let _ = Command::new("setprop").arg(name).arg(value).status();
}

// ---- mode / color code maps ----

fn fan_mode_code(m: FanLedMode) -> u8 {
    match m {
        FanLedMode::Off => 0,
        FanLedMode::Flow => 1,
        FanLedMode::Breath => 2,
        FanLedMode::Blink => 3,
        FanLedMode::Static => 4,
    }
}

fn fan_mode_from_code(c: u8) -> FanLedMode {
    match c {
        1 => FanLedMode::Flow,
        2 => FanLedMode::Breath,
        3 => FanLedMode::Blink,
        4 => FanLedMode::Static,
        _ => FanLedMode::Off,
    }
}

fn fan_color_code(c: FanLedColor) -> u8 {
    match c {
        FanLedColor::Rose => 0,
        FanLedColor::Yellow => 1,
        FanLedColor::Green => 2,
        FanLedColor::Blue => 3,
        FanLedColor::Cyan => 4,
        FanLedColor::Purple => 5,
        FanLedColor::Orange => 6,
        FanLedColor::Mixed1 => 7,
        FanLedColor::Mixed2 => 8,
        FanLedColor::Mixed3 => 9,
        FanLedColor::Mixed4 => 10,
        FanLedColor::Mixed5 => 11,
        FanLedColor::Mixed6 => 12,
        FanLedColor::Mixed7 => 13,
    }
}

fn fan_color_from_code(c: u8) -> FanLedColor {
    match c {
        0 => FanLedColor::Rose,
        1 => FanLedColor::Yellow,
        2 => FanLedColor::Green,
        3 => FanLedColor::Blue,
        4 => FanLedColor::Cyan,
        5 => FanLedColor::Purple,
        6 => FanLedColor::Orange,
        7 => FanLedColor::Mixed1,
        8 => FanLedColor::Mixed2,
        9 => FanLedColor::Mixed3,
        10 => FanLedColor::Mixed4,
        11 => FanLedColor::Mixed5,
        12 => FanLedColor::Mixed6,
        _ => FanLedColor::Mixed7,
    }
}

fn ext_mode_code(m: ExternalLedMode) -> u8 {
    match m {
        ExternalLedMode::Static => 0,
        ExternalLedMode::Breath => 1,
        ExternalLedMode::Blink => 2,
        _ => 0,
    }
}

fn ext_mode_from_code(c: u8) -> ExternalLedMode {
    match c {
        1 => ExternalLedMode::Breath,
        2 => ExternalLedMode::Blink,
        _ => ExternalLedMode::Static,
    }
}

fn ext_color_code(c: ExternalLedColor) -> u8 {
    match c {
        ExternalLedColor::Multi => 0,
        ExternalLedColor::Red => 1,
        ExternalLedColor::Yellow => 2,
        ExternalLedColor::Blue => 3,
        ExternalLedColor::Green => 4,
        ExternalLedColor::Cyan => 5,
        ExternalLedColor::White => 6,
        ExternalLedColor::Purple => 7,
        ExternalLedColor::Pink => 8,
        ExternalLedColor::Orange => 9,
    }
}

fn ext_color_from_code(c: u8) -> ExternalLedColor {
    match c {
        0 => ExternalLedColor::Multi,
        1 => ExternalLedColor::Red,
        2 => ExternalLedColor::Yellow,
        3 => ExternalLedColor::Blue,
        4 => ExternalLedColor::Green,
        5 => ExternalLedColor::Cyan,
        6 => ExternalLedColor::White,
        7 => ExternalLedColor::Purple,
        8 => ExternalLedColor::Pink,
        _ => ExternalLedColor::Orange,
    }
}

// ---- token encode / parse ----

fn enc_fan(s: &Option<FanLedSetting>) -> String {
    match s {
        None => "-".to_string(),
        Some(v) => format!("{}.{}", fan_mode_code(v.mode), fan_color_code(v.color)),
    }
}

fn parse_fan(tok: &str) -> Option<FanLedSetting> {
    let tok = tok.trim();
    if tok.is_empty() || tok == "-" {
        return None;
    }
    let mut it = tok.split('.');
    let m = it.next().and_then(|v| v.trim().parse::<u8>().ok()).unwrap_or(0);
    let c = it.next().and_then(|v| v.trim().parse::<u8>().ok()).unwrap_or(13);
    Some(FanLedSetting {
        mode: fan_mode_from_code(m),
        color: fan_color_from_code(c),
    })
}

fn enc_ext(s: &Option<ExternalLedSetting>) -> String {
    match s {
        None => "-".to_string(),
        Some(v) => format!("{}.{}", ext_mode_code(v.mode), ext_color_code(v.color)),
    }
}

fn parse_ext(tok: &str) -> Option<ExternalLedSetting> {
    let tok = tok.trim();
    if tok.is_empty() || tok == "-" {
        return None;
    }
    let mut it = tok.split('.');
    let m = it.next().and_then(|v| v.trim().parse::<u8>().ok()).unwrap_or(0);
    let c = it.next().and_then(|v| v.trim().parse::<u8>().ok()).unwrap_or(6);
    Some(ExternalLedSetting {
        mode: ext_mode_from_code(m),
        color: ext_color_from_code(c),
    })
}

fn enc_ext_req(v: &ExternalLedSetting) -> String {
    format!("{}.{}", ext_mode_code(v.mode), ext_color_code(v.color))
}

fn enc_charging(c: &ChargingConfig) -> String {
    format!(
        "{};{};{}",
        if c.enabled { 1 } else { 0 },
        enc_fan(&c.fan_led),
        enc_ext(&c.external_led)
    )
}

fn enc_notif(n: &NotificationsConfig) -> String {
    let stop = match n.stop_condition.kind {
        NotificationsStopKind::UntilScreenOn => 0,
        NotificationsStopKind::ForSeconds => 1,
    };
    format!(
        "{};{};{};{}",
        if n.enabled { 1 } else { 0 },
        stop,
        n.for_seconds,
        enc_ext_req(&n.external_led)
    )
}

fn enc_profile(fan: &Option<FanLedSetting>, ext: &Option<ExternalLedSetting>) -> String {
    format!("{};{}", enc_fan(fan), enc_ext(ext))
}

fn parse_profile(v: &str) -> (Option<FanLedSetting>, Option<ExternalLedSetting>) {
    let parts: Vec<&str> = v.split(';').collect();
    let fan = parts.first().and_then(|t| parse_fan(t));
    let ext = parts.get(1).and_then(|t| parse_ext(t));
    (fan, ext)
}

fn profile_leds(
    cfg: &UserConfig,
    ty: ProfileType,
) -> (Option<FanLedSetting>, Option<ExternalLedSetting>) {
    for p in cfg.profiles.iter() {
        if p.profile_type == ty {
            return (p.fan_led.clone(), p.external_led.clone());
        }
    }
    (None, None)
}

fn set_profile_leds(
    cfg: &mut UserConfig,
    ty: ProfileType,
    fan: Option<FanLedSetting>,
    ext: Option<ExternalLedSetting>,
) {
    for p in cfg.profiles.iter_mut() {
        if p.profile_type == ty {
            p.fan_led = fan;
            p.external_led = ext;
            return;
        }
    }
}

/// Write defaults from the current config to any prop that is not yet set.
pub fn ensure_defaults(cfg: &UserConfig) {
    if getprop(PROP_SAVER).is_none() {
        setprop(PROP_SAVER, if cfg.battery_saver.enabled { "1" } else { "0" });
    }
    if getprop(PROP_CHARGING).is_none() {
        setprop(PROP_CHARGING, &enc_charging(&cfg.charging));
    }
    if getprop(PROP_NOTIF).is_none() {
        setprop(PROP_NOTIF, &enc_notif(&cfg.notifications));
    }
    if getprop(PROP_NORMAL).is_none() {
        let (fan, ext) = profile_leds(cfg, ProfileType::Normal);
        setprop(PROP_NORMAL, &enc_profile(&fan, &ext));
    }
    if getprop(PROP_GAMING).is_none() {
        let (fan, ext) = profile_leds(cfg, ProfileType::Gaming);
        setprop(PROP_GAMING, &enc_profile(&fan, &ext));
    }
}

/// Read all props and override the color / battery-saver fields of `cfg`.
pub fn read_and_merge(cfg: &mut UserConfig) {
    if let Some(v) = getprop(PROP_SAVER) {
        cfg.battery_saver.enabled = v.trim() == "1";
    }

    if let Some(v) = getprop(PROP_CHARGING) {
        let parts: Vec<&str> = v.split(';').collect();
        if parts.len() >= 3 {
            cfg.charging.enabled = parts[0].trim() == "1";
            cfg.charging.fan_led = parse_fan(parts[1]);
            cfg.charging.external_led = parse_ext(parts[2]);
        }
    }

    if let Some(v) = getprop(PROP_NOTIF) {
        let parts: Vec<&str> = v.split(';').collect();
        if parts.len() >= 4 {
            cfg.notifications.enabled = parts[0].trim() == "1";
            cfg.notifications.stop_condition.kind = if parts[1].trim() == "1" {
                NotificationsStopKind::ForSeconds
            } else {
                NotificationsStopKind::UntilScreenOn
            };
            cfg.notifications.for_seconds = parts[2].trim().parse::<u64>().unwrap_or(10);
            cfg.notifications.external_led = parse_ext(parts[3]).unwrap_or_default();
        }
    }

    if let Some(v) = getprop(PROP_NORMAL) {
        let (fan, ext) = parse_profile(&v);
        set_profile_leds(cfg, ProfileType::Normal, fan, ext);
    }

    if let Some(v) = getprop(PROP_GAMING) {
        let (fan, ext) = parse_profile(&v);
        set_profile_leds(cfg, ProfileType::Gaming, fan, ext);
    }
}

/// Concatenated raw prop snapshot used by the watcher to detect changes.
pub fn read_all_raw() -> String {
    let mut s = String::new();
    for name in [PROP_SAVER, PROP_CHARGING, PROP_NOTIF, PROP_NORMAL, PROP_GAMING] {
        s.push_str(name);
        s.push('=');
        s.push_str(&getprop(name).unwrap_or_default());
        s.push(';');
    }
    s
}
