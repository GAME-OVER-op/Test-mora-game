use crate::{led_props, state::SharedState};
use std::{
    sync::{Arc, RwLock},
    thread,
    time::Duration,
};

/// Poll the `persist.perf.led.*` and `persist.perf.saver.status` properties.
/// The Android app writes them directly; the daemon merges them into the live
/// config so LED / profile / battery-saver behaviour follows the app UI.
///
/// Only the color + battery-saver fields are overridden, so this never fights
/// with the rest of the running config.
pub fn spawn(shared: Arc<RwLock<SharedState>>) {
    thread::spawn(move || {
        let mut last = led_props::read_all_raw();
        loop {
            thread::sleep(Duration::from_secs(15));

            let raw = led_props::read_all_raw();
            if raw == last {
                continue;
            }
            last = raw;

            let mut cfg = { shared.read().unwrap().config.clone() };
            led_props::read_and_merge(&mut cfg);
            let _ = cfg.validate_and_normalize();

            let mut s = shared.write().unwrap();
            s.config = cfg;
            s.config_rev = s.config_rev.wrapping_add(1);
        }
    });
}
