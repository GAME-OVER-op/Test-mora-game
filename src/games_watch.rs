use crate::{
    games::{apply_updatable_driver_apps, load_from_props},
    state::SharedState,
};
use std::{
    sync::{Arc, RwLock},
    thread,
    time::Duration,
};

/// Poll the `persist.mora.g.*` system properties for the game registry and the
/// per-game trigger coordinates. No files are involved: the app writes the
/// properties directly (e.g. `resetprop -p`), and the daemon reads them here.
///
/// The Android updatable game driver opt-in list is re-applied only when the
/// set of packages actually changes.
pub fn spawn(shared: Arc<RwLock<SharedState>>) {
    thread::spawn(move || {
        let mut last_driver = { shared.read().unwrap().games.driver_string.clone() };

        loop {
            thread::sleep(Duration::from_secs(15));

            let rt = load_from_props();
            let driver = rt.driver_string.clone();
            let changed = driver != last_driver;

            {
                let mut s = shared.write().unwrap();
                s.games = rt;
                s.games_rev = s.games_rev.wrapping_add(1);
                s.last_games_error = None;
            }

            if changed {
                apply_updatable_driver_apps(&driver);
                last_driver = driver;
            }
        }
    });
}
