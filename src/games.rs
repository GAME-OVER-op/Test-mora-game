use std::{
    collections::{HashMap, HashSet},
    process::{Command, Stdio},
};

/// Persistent system-property prefix for the game registry.
///
/// One property per registered game:
///   persist.mora.g.<package> = <left>;<right>
/// where each side is `enabled.x.y` (a disabled side may be `0` or `0.0.0`).
///
/// The set of these properties IS the game list -- there are no files. The app
/// writes the properties directly (e.g. `resetprop -p`), and the daemon only
/// reads them.
pub const PROP_PREFIX: &str = "persist.mora.g.";

/// Authoritative game-list property written by the app as a single
/// comma-separated package list: `persist.mora.games = "pkg1,pkg2,..."`.
///
/// The per-game `persist.mora.g.<pkg>` props only exist once the user configures
/// triggers, and the app cannot delete a system property, so those props cannot
/// express "this game was removed". This list property is rewritten in full on
/// every change, so additions AND removals propagate. A package counts as a
/// registered game if it appears here OR has a `persist.mora.g.<pkg>` entry.
pub const LIST_PROP: &str = "persist.mora.games";

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub struct TriggerSideConfig {
    pub enabled: bool,
    /// Display pixel coordinate captured by the overlay, in the orientation
    /// described by `rot`/`dw`/`dh` below.
    pub x: i32,
    pub y: i32,
    /// Display rotation at capture time (Surface.ROTATION_*: 0..3). The daemon
    /// uses this plus dw/dh to map the display point into the panel-fixed raw
    /// touch axes. Legacy values (no rotation info) are stored as 0.
    pub rot: i32,
    /// Display width/height (px) of the capture surface in that orientation.
    /// 0 means "unknown" (legacy) -> daemon falls back to fb0 portrait scaling.
    pub dw: i32,
    pub dh: i32,
}

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub struct TriggersConfig {
    pub enabled: bool,
    pub left: TriggerSideConfig,
    pub right: TriggerSideConfig,
}

/// Split-charge config kept only so the daemon's (dormant) split-charge
/// controller still has a type to work with. It is no longer configurable.
#[derive(Clone, Copy, Debug)]
pub struct SplitChargeConfig {
    pub enabled: bool,
    pub stop_battery_percent: u8,
}

impl Default for SplitChargeConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            stop_battery_percent: 20,
        }
    }
}

#[derive(Clone, Debug)]
pub struct GamesRuntime {
    pub pkg_set: HashSet<String>,
    /// Every registered game package gets the updatable game driver.
    pub driver_pkgs: Vec<String>,
    pub driver_string: String,
    pub triggers: HashMap<String, TriggersConfig>,
}

impl GamesRuntime {
    pub fn empty() -> Self {
        Self {
            pkg_set: HashSet::new(),
            driver_pkgs: Vec::new(),
            driver_string: String::new(),
            triggers: HashMap::new(),
        }
    }

    pub fn is_game(&self, pkg: &str) -> bool {
        self.pkg_set.contains(pkg)
    }

    pub fn triggers_for(&self, pkg: &str) -> Option<TriggersConfig> {
        self.triggers.get(pkg).copied()
    }
}

fn sanitize_pkg(s: &str) -> String {
    let s = s.trim();
    s.trim_matches(|c: char| !(c.is_ascii_alphanumeric() || c == '.' || c == '_' || c == '-'))
        .to_string()
}

/// Parse one side string. Two formats are accepted:
///   * legacy:  `enabled.x.y`             (rot/dw/dh unknown -> 0)
///   * current: `enabled.x.y.rot.dw.dh`   (display px + capture frame)
/// A disabled side may be `0`, `0.0.0`, or empty.
fn parse_side(s: &str) -> TriggerSideConfig {
    let s = s.trim();
    if s.is_empty() {
        return TriggerSideConfig::default();
    }
    let mut it = s.split('.');
    let enabled = it.next().map(|v| v.trim() == "1").unwrap_or(false);
    if !enabled {
        return TriggerSideConfig::default();
    }
    let x = it.next().and_then(|v| v.trim().parse::<i32>().ok()).unwrap_or(0);
    let y = it.next().and_then(|v| v.trim().parse::<i32>().ok()).unwrap_or(0);
    let rot = it.next().and_then(|v| v.trim().parse::<i32>().ok()).unwrap_or(0);
    let dw = it.next().and_then(|v| v.trim().parse::<i32>().ok()).unwrap_or(0);
    let dh = it.next().and_then(|v| v.trim().parse::<i32>().ok()).unwrap_or(0);
    TriggerSideConfig {
        enabled: true,
        x: x.max(0),
        y: y.max(0),
        rot: ((rot % 4) + 4) % 4,
        dw: dw.max(0),
        dh: dh.max(0),
    }
}

/// Parse a property value `<left>;<right>` into a triggers config. The global
/// enable is derived: it is on when at least one side is enabled.
fn parse_triggers_value(v: &str) -> TriggersConfig {
    let v = v.trim();
    let (l, r) = v.split_once(';').unwrap_or((v, ""));
    let left = parse_side(l);
    let right = parse_side(r);
    TriggersConfig {
        enabled: left.enabled || right.enabled,
        left,
        right,
    }
}

/// Parse a single `getprop` listing line of the form `[key]: [value]`.
fn parse_getprop_line(line: &str) -> Option<(String, String)> {
    let line = line.trim();
    let rest = line.strip_prefix('[')?;
    let (key, rest) = rest.split_once("]:")?;
    let val = rest.trim_start();
    let val = val.strip_prefix('[')?;
    let val = val.strip_suffix(']')?;
    Some((key.trim().to_string(), val.to_string()))
}

fn run_getprop_all() -> String {
    if let Ok(o) = Command::new("getprop").stderr(Stdio::null()).output() {
        if o.status.success() {
            return String::from_utf8_lossy(&o.stdout).into_owned();
        }
    }
    // Fallback via shell.
    Command::new("/system/bin/sh")
        .args(["-c", "getprop"])
        .stderr(Stdio::null())
        .output()
        .map(|o| String::from_utf8_lossy(&o.stdout).into_owned())
        .unwrap_or_default()
}

/// Build the runtime from the `persist.mora.g.*` properties. Never fails; an
/// absent or empty registry simply yields an empty runtime.
pub fn load_from_props() -> GamesRuntime {
    let all = run_getprop_all();

    let mut pkg_set: HashSet<String> = HashSet::new();
    let mut triggers: HashMap<String, TriggersConfig> = HashMap::new();
    let mut driver_pkgs: Vec<String> = Vec::new();
    let mut list_val = String::new();

    for line in all.lines() {
        let (key, val) = match parse_getprop_line(line) {
            Some(kv) => kv,
            None => continue,
        };
        // Authoritative game list (single comma-separated property). Note this
        // key does NOT start with PROP_PREFIX ("persist.mora.g.") because it has
        // no trailing dot, so it is matched explicitly here.
        if key == LIST_PROP {
            list_val = val;
            continue;
        }
        let pkg_raw = match key.strip_prefix(PROP_PREFIX) {
            Some(p) => p,
            None => continue,
        };
        let pkg = sanitize_pkg(pkg_raw);
        if pkg.is_empty() {
            continue;
        }
        let cfg = parse_triggers_value(&val);
        pkg_set.insert(pkg.clone());
        triggers.insert(pkg.clone(), cfg);
        driver_pkgs.push(pkg);
    }

    // Merge in packages from the authoritative comma-separated list. These are
    // registered games even when they have no trigger property yet (no triggers
    // configured) -- this is what makes game mode / the in-game cooler baseline /
    // the per-game performance mode engage for a freshly added game.
    for raw in list_val.split(',') {
        let pkg = sanitize_pkg(raw);
        if pkg.is_empty() {
            continue;
        }
        if pkg_set.insert(pkg.clone()) {
            driver_pkgs.push(pkg);
        }
    }

    driver_pkgs.sort();
    driver_pkgs.dedup();
    let driver_string = driver_pkgs.join(",");

    GamesRuntime {
        pkg_set,
        driver_pkgs,
        driver_string,
        triggers,
    }
}

fn escape_for_double_quotes(s: &str) -> String {
    s.replace('\\', "\\\\").replace('"', "\\\"")
}

/// Apply Android updatable game driver opt-in list.
/// Uses `settings put global updatable_driver_production_opt_in_apps "<csv>"`.
pub fn apply_updatable_driver_apps(csv: &str) {
    // Direct call first (daemon often runs as root).
    let st = Command::new("settings")
        .args([
            "put",
            "global",
            "updatable_driver_production_opt_in_apps",
            csv,
        ])
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .status();

    if st.is_err() || !st.as_ref().ok().map(|x| x.success()).unwrap_or(false) {
        // Fallback via shell.
        let q = escape_for_double_quotes(csv);
        let cmd = format!(
            "settings put global updatable_driver_production_opt_in_apps \"{}\"",
            q
        );
        let _ = Command::new("/system/bin/sh")
            .args(["-c", &cmd])
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .status();
    }

    println!(
        "GAMES: updatable_driver_production_opt_in_apps = {}",
        if csv.is_empty() { "\"\"" } else { csv }
    );
}
