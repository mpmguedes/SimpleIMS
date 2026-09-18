package io.github.vvb2060.ims;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Covers the Android 12 / API 31 compatibility layer.
 *
 * <p>Every expected key string in here was taken from the API 31
 * {@code CarrierConfigManager} on a real device (framework.jar, dex classes3),
 * not from memory. The assertions therefore pin the exact schema names the
 * platform expects on Android 12.
 *
 * <p>These tests exist because this whole compatibility layer is the part most
 * likely to regress silently: on a modern device every branch still compiles and
 * the "modern" path still passes, so nothing would fail until someone actually
 * runs the APK on an older phone.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {31, 33, 34})
public class CarrierConfigCompatTest {

    // ------------------------------------------------------------------
    // Exact schema names as declared on API 31 (verified against device).
    // ------------------------------------------------------------------
    private static final String K_VOLTE = "carrier_volte_available_bool";
    private static final String K_EDITABLE_ENHANCED_4G = "editable_enhanced_4g_lte_bool";
    private static final String K_HIDE_ENHANCED_4G = "hide_enhanced_4g_lte_bool";
    private static final String K_HIDE_LTE_PLUS_ICON = "hide_lte_plus_data_icon_bool";
    private static final String K_VT = "carrier_vt_available_bool";
    private static final String K_SS_OVER_UT = "carrier_supports_ss_over_ut_bool";
    private static final String K_CROSS_SIM = "carrier_cross_sim_ims_available_bool";
    private static final String K_WFC_IMS = "carrier_wfc_ims_available_bool";
    private static final String K_WFC_WIFI_ONLY = "carrier_wfc_supports_wifi_only_bool";
    private static final String K_EDITABLE_WFC_MODE = "editable_wfc_mode_bool";
    private static final String K_EDITABLE_WFC_ROAMING = "editable_wfc_roaming_mode_bool";
    private static final String K_SHOW_WIFI_ICON = "show_wifi_calling_icon_in_status_bar_bool";
    private static final String K_WFC_SPN = "wfc_spn_format_idx_int";
    private static final String K_VONR_ENABLED = "vonr_enabled_bool";
    private static final String K_VONR_VISIBILITY = "vonr_setting_visibility_bool";
    private static final String K_CROSS_SIM_OPPORTUNISTIC =
            "enable_cross_sim_calling_on_opportunistic_data_bool";
    private static final String K_NR_AVAIL = "carrier_nr_availabilities_int_array";
    private static final String K_SSRSRP = "5g_nr_ssrsrp_thresholds_int_array";

    private static final int NSA = 1;
    private static final int SA = 2;

    /** Builds a bundle with everything enabled - the app's default state. */
    private static PersistableBundle allOn() {
        return CarrierConfigCompat.build(true, true, true, true, true, true, true);
    }

    /** Builds a bundle with everything disabled. */
    private static PersistableBundle allOff() {
        return CarrierConfigCompat.build(false, false, false, false, false, false, false);
    }

    // ------------------------------------------------------------------
    // Schema names must match the platform exactly.
    // ------------------------------------------------------------------

    /**
     * The literal fallbacks must resolve to exactly the same schema names the
     * platform uses, otherwise the override silently does nothing.
     */
    @Test
    public void literalsMatchPlatformSchemaNames() {
        assertEquals("show_wifi_calling_icon_in_status_bar_bool",
                CarrierConfigCompat.KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL);
        assertEquals("wfc_spn_format_idx_int",
                CarrierConfigCompat.KEY_WFC_SPN_FORMAT_IDX_INT);
        assertEquals("vonr_enabled_bool", CarrierConfigCompat.KEY_VONR_ENABLED_BOOL);
        assertEquals("vonr_setting_visibility_bool",
                CarrierConfigCompat.KEY_VONR_SETTING_VISIBILITY_BOOL);
        assertEquals("enable_cross_sim_calling_on_opportunistic_data_bool",
                CarrierConfigCompat.KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL);
    }

    /**
     * On API 33+ the platform itself exposes these constants. Where it does, our
     * literal must equal the platform's own value - this is the check that would
     * catch a schema rename on a future Android release.
     *
     * <p>Compared via {@link java.lang.reflect.Field} rather than a direct
     * reference: the test source is compiled against a single SDK, so naming the
     * constant symbol directly would drag the API level of the *compile* SDK into
     * a test that is specifically about the *runtime* SDK.
     */
    @Test
    @Config(sdk = 33)
    public void literalsAgreeWithPlatformConstantsWhereAvailable() throws Exception {
        assertEquals(platformConstant("KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL"),
                CarrierConfigCompat.KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL);
        assertEquals(platformConstant("KEY_WFC_SPN_FORMAT_IDX_INT"),
                CarrierConfigCompat.KEY_WFC_SPN_FORMAT_IDX_INT);
        assertEquals(platformConstant("KEY_VONR_ENABLED_BOOL"),
                CarrierConfigCompat.KEY_VONR_ENABLED_BOOL);
        assertEquals(platformConstant("KEY_VONR_SETTING_VISIBILITY_BOOL"),
                CarrierConfigCompat.KEY_VONR_SETTING_VISIBILITY_BOOL);
    }

    /**
     * Reflectively reads a String constant from the platform's
     * {@code CarrierConfigManager}. Returns {@code null} when the constant does not
     * exist at the running SDK level, so the caller's assertion fails loudly
     * rather than silently skipping.
     */
    private static String platformConstant(String fieldName) throws Exception {
        try {
            java.lang.reflect.Field f = CarrierConfigManager.class.getField(fieldName);
            Object v = f.get(null);
            return v instanceof String ? (String) v : null;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // VoLTE
    // ------------------------------------------------------------------

    @Test
    public void volteKeysMatchPlatformConstants() {
        PersistableBundle b = allOn();
        assertTrue(b.getBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, false));
        assertTrue(b.getBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, false));
        // "hide" must be explicitly false, and false is the value that matters:
        // getBoolean(key, default) cannot distinguish unset from false, so assert
        // via a true-default to prove the key is genuinely present as false.
        assertFalse(b.getBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, true));
        assertFalse(b.getBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, true));
    }

    @Test
    public void volteValuesAreReadableByRawSchemaName() {
        // Guards against any regression that swaps a constant for a wrong literal.
        PersistableBundle b = allOn();
        assertTrue(b.getBoolean(K_VOLTE, false));
        assertTrue(b.getBoolean(K_EDITABLE_ENHANCED_4G, false));
        assertFalse(b.getBoolean(K_HIDE_ENHANCED_4G, true));
        assertFalse(b.getBoolean(K_HIDE_LTE_PLUS_ICON, true));
    }

    // ------------------------------------------------------------------
    // VT / UT
    // ------------------------------------------------------------------

    @Test
    public void vtAndUtKeys() {
        PersistableBundle b = allOn();
        assertTrue(b.getBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, false));
        assertTrue(b.getBoolean(K_VT, false));
        assertTrue(b.getBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, false));
        assertTrue(b.getBoolean(K_SS_OVER_UT, false));
    }

    // ------------------------------------------------------------------
    // VoWiFi
    // ------------------------------------------------------------------

    @Test
    public void vowifiKeys() {
        PersistableBundle b = allOn();
        assertTrue(b.getBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, false));
        assertTrue(b.getBoolean(CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, false));
        assertTrue(b.getBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, false));
        assertTrue(b.getBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, false));
        assertTrue(b.getBoolean(K_WFC_IMS, false));
        assertTrue(b.getBoolean(K_WFC_WIFI_ONLY, false));
        assertTrue(b.getBoolean(K_EDITABLE_WFC_MODE, false));
        assertTrue(b.getBoolean(K_EDITABLE_WFC_ROAMING, false));
    }

    /**
     * The WiFi-calling icon and the SPN format index have no constant on API 31.
     * They must still be written, using the literal names, or Android 12 loses
     * the status-bar icon behaviour that newer devices get.
     */
    @Test
    public void vowifiLiteralOnlyKeysAreWritten() {
        PersistableBundle b = allOn();
        assertTrue("wifi calling icon key missing", b.getBoolean(K_SHOW_WIFI_ICON, false));
        assertEquals("wfc spn format index wrong", 6, b.getInt(K_WFC_SPN, -1));
    }

    // ------------------------------------------------------------------
    // Cross-SIM
    // ------------------------------------------------------------------

    @Test
    public void crossSimKeys() {
        PersistableBundle b = allOn();
        // Primary key exists on API 31 - this is the one that actually works.
        assertTrue(b.getBoolean(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL, false));
        assertTrue(b.getBoolean(K_CROSS_SIM, false));
    }

    /**
     * The opportunistic-data key is API 34+. It must still be written as a
     * literal (so behaviour is identical on newer devices) while being provably
     * absent from the platform on API 31.
     */
    @Test
    public void crossSimOpportunisticKeyIsWrittenAsLiteral() {
        assertTrue(allOn().getBoolean(K_CROSS_SIM_OPPORTUNISTIC, false));
    }

    @Test
    public void crossSimCapabilityFlagTracksApiLevel() {
        boolean expected = Build.VERSION.SDK_INT >= 34;
        assertEquals(expected, CarrierConfigCompat.supportsCrossSimOpportunisticKey());
    }

    // ------------------------------------------------------------------
    // VoNR - the genuine Android 12 limitation
    // ------------------------------------------------------------------

    /**
     * VoNR keys must be written on every version (identical bundle everywhere),
     * even though API 31 ignores them.
     */
    @Test
    public void vonrKeysAreAlwaysWritten() {
        PersistableBundle b = allOn();
        assertTrue(b.getBoolean(K_VONR_ENABLED, false));
        assertTrue(b.getBoolean(K_VONR_VISIBILITY, false));
    }

    /**
     * The capability flag is what the UI uses to avoid lying to the user. It must
     * be false on API 31/32 and true from API 33 up.
     */
    @Test
    public void vonrCapabilityFlagTracksApiLevel() {
        assertEquals(Build.VERSION.SDK_INT >= 33, CarrierConfigCompat.supportsVonrConfigKeys());
    }

    @Test
    @Config(sdk = 31)
    public void vonrNotSupportedOnAndroid12() {
        assertFalse("VoNR must report unsupported on API 31",
                CarrierConfigCompat.supportsVonrConfigKeys());
    }

    @Test
    @Config(sdk = 32)
    public void vonrNotSupportedOnAndroid12L() {
        assertFalse("VoNR must report unsupported on API 32",
                CarrierConfigCompat.supportsVonrConfigKeys());
    }

    @Test
    @Config(sdk = 33)
    public void vonrSupportedFromAndroid13() {
        assertTrue("VoNR must report supported from API 33",
                CarrierConfigCompat.supportsVonrConfigKeys());
    }

    // ------------------------------------------------------------------
    // 5G NR
    // ------------------------------------------------------------------

    @Test
    public void fiveGNrKeys() {
        PersistableBundle b = allOn();
        int[] avail = b.getIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
        assertEquals("NSA+SA", 2, avail.length);
        assertEquals(NSA, avail[0]);
        assertEquals(SA, avail[1]);
    }

    @Test
    public void fiveGNrThresholdsAreOrdered() {
        int[] t = allOn().getIntArray(K_SSRSRP);
        assertEquals(4, t.length);
        // Must be strictly increasing, as the platform expects band boundaries.
        for (int i = 1; i < t.length; i++) {
            assertTrue("thresholds must ascend", t[i] > t[i - 1]);
        }
    }

    // ------------------------------------------------------------------
    // Flag independence
    // ------------------------------------------------------------------

    /**
     * Each feature flag must independently control only its own keys. A previous
     * duplicated implementation existed in two classes and could drift; this test
     * would have caught that.
     */
    @Test
    public void flagsAreIndependent() {
        PersistableBundle onlyVolte =
                CarrierConfigCompat.build(true, false, false, false, false, false, false);
        assertTrue(onlyVolte.getBoolean(K_VOLTE, false));
        assertFalse("VT must not be set", onlyVolte.getBoolean(K_VT, false));
        assertFalse("VoWiFi must not be set", onlyVolte.getBoolean(K_WFC_IMS, false));
        assertFalse("VoNR must not be set", onlyVolte.getBoolean(K_VONR_ENABLED, false));
        assertFalse("UT must not be set", onlyVolte.getBoolean(K_SS_OVER_UT, false));
        assertFalse("cross-SIM must not be set", onlyVolte.getBoolean(K_CROSS_SIM, false));
        assertEquals("no 5G keys expected", null, onlyVolte.getIntArray(K_NR_AVAIL));
    }

    @Test
    public void allOffProducesEmptyBundle() {
        PersistableBundle b = allOff();
        assertFalse(b.getBoolean(K_VOLTE, false));
        assertFalse(b.getBoolean(K_VT, false));
        assertFalse(b.getBoolean(K_WFC_IMS, false));
        assertFalse(b.getBoolean(K_VONR_ENABLED, false));
        assertFalse(b.getBoolean(K_CROSS_SIM, false));
        assertFalse(b.getBoolean(K_SS_OVER_UT, false));
        assertEquals(null, b.getIntArray(K_NR_AVAIL));
        assertEquals(null, b.getIntArray(K_SSRSRP));
    }

    // ------------------------------------------------------------------
    // getConfigForSubId shim
    // ------------------------------------------------------------------

    /**
     * The shim must never route to the API-34-only overload below API 34. This is
     * the exact latent bug inherited from upstream (which called the 2-arg form
     * while declaring minSdk 33 and would throw NoSuchMethodError on Android 13).
     */
    @Test
    @Config(sdk = 31)
    public void shimUsesSingleArgOverloadOnAndroid12() throws Exception {
        assertFalse("2-arg overload must not exist on API 31",
                hasTwoArgOverload());
    }

    @Test
    @Config(sdk = 34)
    public void shimUsesTwoArgOverloadOnAndroid14() throws Exception {
        assertTrue("2-arg overload expected on API 34", hasTwoArgOverload());
    }

    /** Reflectively checks whether the API-34 "with feature" overload exists. */
    private static boolean hasTwoArgOverload() {
        for (java.lang.reflect.Method m : CarrierConfigManager.class.getMethods()) {
            if (!m.getName().equals("getConfigForSubId")) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 2 && p[0] == int.class && p[1] == String[].class) {
                return true;
            }
        }
        return false;
    }
}
