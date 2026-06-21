//! Physical 2-position side slider -> launch / close GameSpace.
//!
//! The RedMagic / Nubia side slider is a *momentary* key device: each flip
//! emits a single KEY pulse (down + up) on /dev/input/eventN and reports NO
//! persistent state. One physical direction emits KEY_RED (0x18e), the other
//! KEY_GREEN (0x18f) -- confirmed from `getevent` on event0.
//!
//! Default mapping:  RED  = ON  -> start the GameSpace activity
//!                   GREEN = OFF -> force-stop the app (closes UI + overlay)
//!
//! Tunables (system properties, read live on each flip):
//!   persist.mora.slider.enable    1/0   master on/off            (default 1)
//!   persist.mora.slider.invert    1/0   swap which side means ON (default 0)
//!   persist.mora.slider.statepath PATH  optional sysfs/proc node that reports
//!                                        the current physical position at boot
//!   persist.mora.slider.stateon   STR   value of that node meaning ON (def "1")
//!
//! Boot state: a momentary KEY device cannot be read back, so at boot we take
//! NO action (GameSpace stays closed) until the first flip -- UNLESS a state
//! node is configured via persist.mora.slider.statepath, in which case we read
//! the current position once at startup and sync accordingly.

use std::{
    fs,
    io::{self, Read},
    path::{Path, PathBuf},
    process::Command,
    thread,
    time::{Duration, Instant},
};

const EV_KEY: u16 = 0x01;
const KEY_RED: u16 = 0x18e; // 398
const KEY_GREEN: u16 = 0x18f; // 399

const PKG: &str = "com.mora.gamespace";
const ACTIVITY: &str = "com.mora.gamespace/com.mora.gamespace.MainActivity";

#[repr(C)]
#[derive(Clone, Copy)]
struct TimeVal {
    tv_sec: i64,
    tv_usec: i64,
}

#[repr(C)]
#[derive(Clone, Copy)]
struct InputEvent {
    time: TimeVal,
    type_: u16,
    code: u16,
    value: i32,
}

fn getprop(name: &str) -> Option<String> {
    let out = Command::new("getprop").arg(name).output().ok()?;
    let s = String::from_utf8_lossy(&out.stdout).trim().to_string();
    if s.is_empty() {
        None
    } else {
        Some(s)
    }
}

fn prop_bool(name: &str, default: bool) -> bool {
    match getprop(name).as_deref() {
        Some("1") | Some("true") | Some("on") => true,
        Some("0") | Some("false") | Some("off") => false,
        _ => default,
    }
}

fn read_to_string(path: &Path) -> io::Result<String> {
    let mut s = String::new();
    fs::File::open(path)?.read_to_string(&mut s)?;
    Ok(s)
}

fn parse_caps_hex_words_u64(s: &str) -> Vec<u64> {
    let mut words: Vec<u64> = Vec::new();
    for p in s.split_whitespace() {
        if let Ok(v) = u64::from_str_radix(p.trim(), 16) {
            words.push(v);
        }
    }
    words.reverse(); // word0 = lowest bits
    words
}

fn cap_has(words: &[u64], bit: u32) -> bool {
    let wi = (bit / 64) as usize;
    let bi = (bit % 64) as u64;
    wi < words.len() && (words[wi] & (1u64 << bi)) != 0
}

/// Find the input device that exposes BOTH KEY_RED and KEY_GREEN (the slider).
fn find_slider_dev() -> Option<PathBuf> {
    let dir = Path::new("/sys/class/input");
    for ent in fs::read_dir(dir).ok()? {
        let ent = match ent {
            Ok(e) => e,
            Err(_) => continue,
        };
        let ev = ent.file_name().to_string_lossy().to_string();
        if !ev.starts_with("event") {
            continue;
        }
        let keyfile = dir.join(&ev).join("device").join("capabilities").join("key");
        let caps = read_to_string(&keyfile).unwrap_or_default();
        let words = parse_caps_hex_words_u64(&caps);
        if cap_has(&words, KEY_RED as u32) && cap_has(&words, KEY_GREEN as u32) {
            return Some(PathBuf::from("/dev/input").join(&ev));
        }
    }
    None
}

fn read_input_event(f: &mut fs::File) -> io::Result<InputEvent> {
    let mut buf = [0u8; std::mem::size_of::<InputEvent>()];
    f.read_exact(&mut buf)?;
    Ok(unsafe { std::ptr::read_unaligned(buf.as_ptr() as *const InputEvent) })
}

fn launch_gamespace() {
    println!("SLIDER: ON -> start {}", ACTIVITY);
    let _ = Command::new("am")
        .args(["start", "--user", "0", "-n", ACTIVITY])
        .status();
}

fn close_gamespace() {
    println!("SLIDER: OFF -> force-stop {}", PKG);
    let _ = Command::new("am")
        .args(["force-stop", "--user", "0", PKG])
        .status();
}

/// Optional boot-position read from a sysfs/proc node, if configured.
/// Returns Some(true)=ON, Some(false)=OFF, None=unknown/unavailable.
fn read_boot_state() -> Option<bool> {
    let path = getprop("persist.mora.slider.statepath")?;
    let on_val = getprop("persist.mora.slider.stateon").unwrap_or_else(|| "1".to_string());
    let raw = read_to_string(Path::new(&path)).ok()?;
    Some(raw.trim() == on_val.trim())
}

pub fn spawn() {
    thread::spawn(move || {
        if !prop_bool("persist.mora.slider.enable", true) {
            println!("SLIDER: disabled (persist.mora.slider.enable=0)");
            return;
        }

        // Optional boot-position sync. Only works if a state node is configured;
        // a bare momentary KEY device cannot be read back.
        match read_boot_state() {
            Some(true) => {
                println!("SLIDER: boot state = ON");
                launch_gamespace();
            }
            Some(false) => println!("SLIDER: boot state = OFF"),
            None => println!(
                "SLIDER: boot state unknown (momentary key) -> no action until first flip"
            ),
        }

        // Input nodes can appear slightly after boot; wait for the slider.
        let dev = loop {
            if let Some(p) = find_slider_dev() {
                break p;
            }
            println!("SLIDER: device (KEY_RED+KEY_GREEN) not found yet; retry in 5s");
            thread::sleep(Duration::from_secs(5));
        };
        println!("SLIDER: listen {}", dev.display());

        let mut f = match fs::File::open(&dev) {
            Ok(x) => x,
            Err(e) => {
                eprintln!("SLIDER: open {} failed: {}", dev.display(), e);
                return;
            }
        };

        let mut last = Instant::now() - Duration::from_secs(1);
        loop {
            let ev = match read_input_event(&mut f) {
                Ok(e) => e,
                Err(e) => {
                    eprintln!("SLIDER: read error: {} -- reopening", e);
                    thread::sleep(Duration::from_millis(500));
                    match fs::File::open(&dev) {
                        Ok(x) => {
                            f = x;
                            continue;
                        }
                        Err(_) => return,
                    }
                }
            };

            // React only to key-down pulses for our two codes.
            if ev.type_ != EV_KEY || ev.value != 1 {
                continue;
            }
            if ev.code != KEY_RED && ev.code != KEY_GREEN {
                continue;
            }

            // Debounce bouncy/repeat pulses.
            if last.elapsed() < Duration::from_millis(250) {
                continue;
            }
            last = Instant::now();

            let invert = prop_bool("persist.mora.slider.invert", false);
            let on = if invert {
                ev.code == KEY_GREEN
            } else {
                ev.code == KEY_RED
            };

            if on {
                launch_gamespace();
            } else {
                close_gamespace();
            }
        }
    });
}
