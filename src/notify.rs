
use std::{
    fs,
    os::unix::fs::PermissionsExt,
    path::Path,
};

use crate::config::ICON_DST;

const ICON_BYTES: &[u8] = include_bytes!("assets/mora.png");

pub fn ensure_icon_on_disk() {
    let dst = Path::new(ICON_DST);

    let need_write = match fs::metadata(dst) {
        Ok(m) => m.len() != ICON_BYTES.len() as u64,
        Err(_) => true,
    };

    if need_write {
        if let Err(e) = fs::write(dst, ICON_BYTES) {
            println!("NOTIFY: icon write fail ({})", e);
            return;
        }
        let _ = fs::set_permissions(dst, fs::Permissions::from_mode(0o644));
        println!("NOTIFY: icon ready ({})", ICON_DST);
    }
}
