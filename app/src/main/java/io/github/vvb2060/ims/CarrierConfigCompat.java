package io.github.vvb2060.ims;

import android.os.Build;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;

/**
 * Carrier-config keys that are not present (as compile-time constants) on older
 * Android releases.
 *
 * <p>Two distinct problems are handled here, and they need different solutions:
 *
 * <ol>
 *   <li><b>Constant missing from the compile SDK.</b> A key that exists in the
 *       carrier config schema but has no {@code CarrierConfigManager.KEY_*}
 *       field on API 31. The string value is identical on every Android version,
 *       so we simply use the literal. No version check is needed - writing an
 *       unknown key into the bundle is harmless on platforms that ignore it.</li>
 *
 *   <li><b>Constant not available on the running platform at all.</b>
 *       {@code KEY_VONR_ENABLED_BOOL} and {@code KEY_VONR_SETTING_VISIBILITY_BOOL}
 *       were introduced in Android 13 (API 33). On API 31 the platform has no
 *       notion of a VoNR toggle in CarrierConfig, so writing these keys cannot
 *       enable anything. We still write them (a literal string, so it is safe)
 *       so that the same bundle remains meaningful if the config is restored on
 *       a newer device, but callers should report the feature as
 *       "not supported by this platform" rather than "applied".</li>
 * </ol>
 *
 * <p>Keeping these in one place means {@link ImsConfigHelper} and
 * {@link PrivilegedProcess} cannot drift apart.
 */
final class CarrierConfigCompat {

    private CarrierConfigCompat() {
    }

    // ---------------------------------------------------------------------
    // Keys whose *constant field* is unavailable on API 31 but whose schema
    // name is unchanged. Safe to use unconditionally as literals.
    // ---------------------------------------------------------------------

    /**
     * {@code CarrierConfigManager.KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL}.
     * Field added in API 33; schema name unchanged.
     */
    static final String KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL =
            "show_wifi_calling_icon_in_status_bar_bool";

    /**
     * {@code CarrierConfigManager.KEY_WFC_SPN_FORMAT_IDX_INT}.
     * Field added in API 33; schema name unchanged.
     */
    static final String KEY_WFC_SPN_FORMAT_IDX_INT = "wfc_spn_format_idx_int";

    // ---------------------------------------------------------------------
    // Keys that do not exist on the API 31 platform at all.
    // ---------------------------------------------------------------------

    /** Added in API 33 (Android 13). No effect on API 31. */
    static final String KEY_VONR_ENABLED_BOOL = "vonr_enabled_bool";

    /** Added in API 33 (Android 13). No effect on API 31. */
    static final String KEY_VONR_SETTING_VISIBILITY_BOOL = "vonr_setting_visibility_bool";

    /**
     * {@code CarrierConfigManager.KEY_CROSS_SIM_SPN_FORMAT_INT} era key.
     * Added in API 34 (Android 14); schema name unchanged where supported.
     */
    static final String KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL =
            "enable_cross_sim_calling_on_opportunistic_data_bool";

    /**
     * True when the running platform actually understands the VoNR carrier
     * config keys. On API 31 the VoNR toggle is not part of CarrierConfig, so
     * the UI must not claim VoNR was enabled.
     */
    static boolean supportsVonrConfigKeys() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU; // 33
    }

    /**
     * True when the platform understands the cross-SIM opportunistic-data key.
     * Present from Android 14 (API 34).
     */
    static boolean supportsCrossSimOpportunisticKey() {
        return Build.VERSION.SDK_INT >= 34;
    }

    /**
     * Reads the carrier config for a subscription across Android versions.
     *
     * <p>The "with feature" overload is {@code getConfigForSubId(int, String...)}
     * and was added in <b>Android 14 (API 34)</b>. It does not exist on API 31,
     * where only {@code getConfigForSubId(int)} is declared - so a direct call
     * throws {@link NoSuchMethodError} at runtime. Both forms return the same
     * bundle for our purposes, so the version check is purely about which
     * overload is callable.
     *
     * <p>The lint "missing permission" findings here are expected: this class
     * performs the privileged work while the *Shizuku shell identity* holds
     * NETWORK_SETTINGS, not the app process. The app declares READ_PHONE_STATE
     * in the manifest for the read path; the delegated shell identity supplies
     * the privileged write. Suppressed deliberately rather than by accident.
     */
    @SuppressWarnings("MissingPermission")
    static PersistableBundle getConfigForSubId(CarrierConfigManager cm, int subId) {
        if (Build.VERSION.SDK_INT >= 34) {
            return cm.getConfigForSubId(subId, "vvb2060_config_version");
        }
        // API 31 / 32 / 33: single-argument overload only.
        return cm.getConfigForSubId(subId);
    }

    // ---------------------------------------------------------------------
    // Bundle builders - only differ by which optional keys are included.
    // ---------------------------------------------------------------------

    private static void putVolte(PersistableBundle b, boolean enable) {
        if (!enable) return;
        b.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true);
        b.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true);
        b.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false);
        b.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false);
    }

    private static void putVt(PersistableBundle b, boolean enable) {
        if (!enable) return;
        b.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true);
    }

    private static void putUt(PersistableBundle b, boolean enable) {
        if (!enable) return;
        b.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true);
    }

    private static void putCrossSim(PersistableBundle b, boolean enable) {
        if (!enable) return;
        b.putBoolean(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL, true);
        // Key only honoured from Android 14; literal string is safe on API 31.
        b.putBoolean(KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL, true);
    }

    private static void putVowifi(PersistableBundle b, boolean enable) {
        if (!enable) return;
        b.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true);
        b.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true);
        b.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true);
        b.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true);
        b.putBoolean(KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL, true);
        b.putInt(KEY_WFC_SPN_FORMAT_IDX_INT, 6);
    }

    private static void putVonr(PersistableBundle b, boolean enable) {
        if (!enable) return;
        // These two keys only exist from Android 13 onwards. Writing them on
        // API 31 is a no-op - the platform never reads them - but it keeps the
        // bundle identical across versions. The UI reports VoNR separately.
        b.putBoolean(KEY_VONR_ENABLED_BOOL, true);
        b.putBoolean(KEY_VONR_SETTING_VISIBILITY_BOOL, true);
    }

    private static void put5gNr(PersistableBundle b, boolean enable) {
        if (!enable) return;
        b.putIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                new int[]{CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA,
                        CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA});
        b.putIntArray(CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,
                // Boundaries: [-140 dBm, -44 dBm]
                new int[]{
                        -128, /* SIGNAL_STRENGTH_POOR */
                        -118, /* SIGNAL_STRENGTH_MODERATE */
                        -108, /* SIGNAL_STRENGTH_GOOD */
                        -98,  /* SIGNAL_STRENGTH_GREAT */
                });
    }

    /**
     * Builds the full carrier-config bundle. Identical output to the original
     * implementation, with the two version-sensitive keys handled via the
     * literals above instead of constants that do not compile on API 31.
     */
    static PersistableBundle build(boolean enableVoLTE, boolean enableVoWiFi,
                                   boolean enableVT, boolean enableVoNR,
                                   boolean enableCrossSIM, boolean enableUT,
                                   boolean enable5GNR) {
        var bundle = new PersistableBundle();
        putVolte(bundle, enableVoLTE);
        putVt(bundle, enableVT);
        putUt(bundle, enableUT);
        putCrossSim(bundle, enableCrossSIM);
        putVowifi(bundle, enableVoWiFi);
        putVonr(bundle, enableVoNR);
        put5gNr(bundle, enable5GNR);
        return bundle;
    }
}
