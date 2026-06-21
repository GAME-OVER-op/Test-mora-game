package com.mora.gamespace

/**
 * Mirrors the GameSpace game list into a single system property the mora-perf daemon reads:
 *
 *     persist.mora.games = "pkg1,pkg2,..."
 *
 * Why a single list property (and not only the per-game persist.mora.g.<pkg> props):
 * the per-game props are written by MoraTriggers ONLY once the user configures triggers,
 * and an app cannot delete a system property. So those props alone cannot express "this
 * game was added without triggers" or "this game was removed". This list is rewritten in
 * full on every change, so both additions and removals propagate to the daemon, which
 * treats a package as a registered game if it appears here OR has a per-game trigger prop.
 *
 * Per-game trigger coordinates still live in persist.mora.g.<pkg> via MoraTriggers.
 *
 * Writing persist.mora.* needs the privileged SELinux context granted by the permissions
 * bootstrap (service.sh + sepolicy.rule), exactly like the other privileged props this app
 * touches; the call is best-effort and simply returns false if the policy is not installed.
 */
object MoraGames {

    const val PROP = "persist.mora.games"

    /** @return true if the system-property mirror succeeded (SELinux may still reject it). */
    fun register(pkgs: List<String>): Boolean {
        val csv = pkgs.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(",")
        return setProp(PROP, csv)
    }

    private fun setProp(name: String, value: String): Boolean {
        return try {
            val cls = Class.forName("android.os.SystemProperties")
            val m = cls.getMethod("set", String::class.java, String::class.java)
            m.invoke(null, name, value)
            true
        } catch (e: Throwable) {
            false
        }
    }
}
