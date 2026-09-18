package io.github.vvb2060.ims;

import android.app.IActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.ServiceManager;
import android.system.Os;
import android.telephony.CarrierConfigManager;
import android.util.Log;

import rikka.shizuku.ShizukuBinderWrapper;

/**
 * Writes the IMS carrier config through Shizuku.
 *
 * <p>Three ways of reaching {@code CarrierConfigManager.overrideConfig} exist,
 * in increasing order of cost to the user:
 *
 * <ol>
 *   <li><b>Shell identity delegated to our own uid</b> ({@link #applyWithDelegatedShellIdentity}).
 *       Cheapest: nothing gets killed. Works where the shell package holds
 *       {@code MODIFY_PHONE_STATE} (verified on Huawei EMUI/HarmonyOS, API 31).
 *   <li><b>{@code carrier_config} binder wrapped in {@link ShizukuBinderWrapper}</b>
 *       ({@link #applyThroughShizukuBinder}). The transaction is re-sent by the
 *       Shizuku process, so the platform sees Shizuku's uid (2000 = shell, or
 *       0 = root) instead of ours. Also kills nothing.
 *   <li><b>Instrumentation</b> ({@link PrivilegedProcess}). Always available, but
 *       {@code startInstrumentation} on our own package makes the system
 *       force-stop the app - which users read as "the app crashed".
 * </ol>
 *
 * {@link #applyWithoutRestart} tries 1 then 2 and only reports failure when both
 * are rejected, so the UI can fall back to 3 as a last resort.
 */
public class ImsConfigHelper {

    private static final String TAG = "ImsConfigHelper";
    private static final String PREFS_NAME = "ims_config";

    /** Bundle key used to detect whether our config is already in place. */
    private static final String KEY_CONFIG_VERSION = "vvb2060_config_version";

    /** Hidden AIDL interface of the {@code carrier_config} system service. */
    private static final String CARRIER_CONFIG_LOADER =
            "com.android.internal.telephony.ICarrierConfigLoader";

    private ImsConfigHelper() {}

    /** Performs the actual {@code overrideConfig} call for one subscription. */
    private interface Writer {
        void write(int subId, PersistableBundle values) throws Exception;
    }

    /**
     * Applies the saved preferences to the selected subscription(s) without
     * starting an instrumentation, i.e. without the app being force-stopped.
     *
     * @param selected a real subscription id, or {@link Subscriptions#ALL_SIMS}
     * @return the number of subscriptions that now carry the requested config
     *         (subscriptions that were already up to date count as satisfied)
     * @throws Exception when every in-process mechanism was rejected by the
     *         platform; the caller should then fall back to instrumentation
     */
    public static int applyWithoutRestart(Context context, int selected) throws Exception {
        Throwable firstFailure = null;
        try {
            int n = applyWithDelegatedShellIdentity(context, selected);
            Log.i(TAG, "MECHANISM_RESULT delegated-shell-identity OK (" + n + ")");
            return n;
        } catch (Throwable t) {
            Log.w(TAG, "MECHANISM_RESULT delegated-shell-identity REJECTED: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            firstFailure = t;
        }

        try {
            int n = applyThroughShizukuBinder(context, selected);
            Log.i(TAG, "MECHANISM_RESULT shizuku-binder OK (" + n + ")");
            return n;
        } catch (Throwable t) {
            Log.w(TAG, "MECHANISM_RESULT shizuku-binder REJECTED: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            if (firstFailure != null && firstFailure != t) {
                t.addSuppressed(firstFailure);
            }
            throw t instanceof Exception ? (Exception) t : new RuntimeException(t);
        }
    }

    /**
     * Mechanism 1: borrow the shell identity, then write through the normal
     * {@link CarrierConfigManager} of this process.
     */
    public static int applyWithDelegatedShellIdentity(Context context, int selected)
            throws Exception {
        Log.i(TAG, "Applying (delegated shell identity), selection=" + selected);

        var binder = ServiceManager.getService(Context.ACTIVITY_SERVICE);
        if (binder == null) throw new IllegalStateException("activity service unavailable");
        var am = IActivityManager.Stub.asInterface(new ShizukuBinderWrapper(binder));

        am.startDelegateShellPermissionIdentity(Os.getuid(), null);
        try {
            var cm = context.getSystemService(CarrierConfigManager.class);
            return applyToSubscriptions(context, cm, selected, (subId, values) -> {
                try {
                    cm.getClass().getMethod("overrideConfig", int.class, PersistableBundle.class)
                            .invoke(cm, subId, values);
                    Log.i(TAG, "Applied config to subscription: " + subId);
                } catch (NoSuchMethodException e) {
                    // Older/newer platforms take a third "persistent" argument.
                    cm.getClass().getMethod("overrideConfig", int.class, PersistableBundle.class,
                            boolean.class).invoke(cm, subId, values, false);
                    Log.i(TAG, "Applied config (non-persistent) to subscription: " + subId);
                }
            });
        } finally {
            am.stopDelegateShellPermissionIdentity();
        }
    }

    /**
     * Mechanism 2: send the write through Shizuku's own process, so the platform
     * performs the permission check against Shizuku's uid rather than ours.
     */
    public static int applyThroughShizukuBinder(Context context, int selected) throws Exception {
        Log.i(TAG, "Applying (carrier_config through Shizuku), selection=" + selected);

        IBinder raw = ServiceManager.getService(Context.CARRIER_CONFIG_SERVICE);
        if (raw == null) throw new IllegalStateException("carrier_config service unavailable");

        Object loader = Class.forName(CARRIER_CONFIG_LOADER + "$Stub")
                .getMethod("asInterface", IBinder.class)
                .invoke(null, new ShizukuBinderWrapper(raw));
        if (loader == null) throw new IllegalStateException("cannot reach ICarrierConfigLoader");

        var cm = context.getSystemService(CarrierConfigManager.class);
        return applyToSubscriptions(context, cm, selected, (subId, values) -> {
            try {
                loader.getClass().getMethod("overrideConfig", int.class, PersistableBundle.class)
                        .invoke(loader, subId, values);
                Log.i(TAG, "Applied config to subscription: " + subId);
            } catch (NoSuchMethodException e) {
                loader.getClass().getMethod("overrideConfig", int.class, PersistableBundle.class,
                        boolean.class).invoke(loader, subId, values, false);
                Log.i(TAG, "Applied config (non-persistent) to subscription: " + subId);
            }
        });
    }

    /** Applies to a single, already-known subscription id (instrumentation path). */
    public static void applyConfig(Context context, int subId) throws Exception {
        applyWithDelegatedShellIdentity(context, subId);
    }

    /**
     * Shared loop: resolve targets, skip subscriptions that already match, write
     * the rest, and count what ended up correct.
     */
    private static int applyToSubscriptions(Context context, CarrierConfigManager cm,
            int selected, Writer writer) throws Exception {
        var values = readConfig(context);
        values.putInt(KEY_CONFIG_VERSION, BuildConfig.VERSION_CODE);

        int[] subIds = Subscriptions.resolveTargets(context, selected);
        if (subIds.length == 0) {
            throw new IllegalStateException("no active subscription");
        }

        int satisfied = 0;
        for (int subId : subIds) {
            try {
                if (matches(CarrierConfigCompat.getConfigForSubId(cm, subId), values)) {
                    // Already carrying exactly these values - success, not failure.
                    Log.i(TAG, "Config already up-to-date for subscription: " + subId);
                    satisfied++;
                    continue;
                }
                writer.write(subId, values);
                satisfied++;
            } catch (Throwable t) {
                // One bad subscription must not abort the others - but the cause
                // has to stay visible, otherwise a platform rejection looks like
                // a silent no-op.
                Log.e(TAG, "OVERRIDE_RESULT subId=" + subId + " FAILED: "
                        + t.getClass().getName() + ": " + t.getMessage(), t);
                if (satisfied == 0 && subId == subIds[0]) {
                    throw asException(t);
                }
            }
        }
        Log.i(TAG, "OVERRIDE_RESULT satisfied=" + satisfied + "/" + subIds.length);
        if (satisfied == 0) {
            throw new IllegalStateException("overrideConfig failed for every subscription");
        }
        return satisfied;
    }

    private static Exception asException(Throwable t) {
        if (t instanceof Exception) return (Exception) t;
        return new RuntimeException(t);
    }

    /**
     * Whether the subscription already carries exactly the values we want.
     *
     * <p>Upstream only compared {@code vvb2060_config_version} against the
     * version code. That meant the config was written once per app version and
     * then never again - toggling a switch in the UI and pressing Apply did
     * nothing at all. Comparing the actual values fixes that while still
     * avoiding pointless writes.
     *
     * <p>Only keys present in {@code values} are compared, so this is a subset
     * check: a feature switched OFF simply stops being forced and its previous
     * value is left alone. That matches upstream and is deliberate - see the
     * "force-on flags" note in the audit document.
     */
    private static boolean matches(PersistableBundle existing, PersistableBundle values) {
        if (existing == null) return false;
        if (existing.getInt(KEY_CONFIG_VERSION, 0) != BuildConfig.VERSION_CODE) return false;
        for (String key : values.keySet()) {
            if (KEY_CONFIG_VERSION.equals(key)) continue;
            Object want = values.get(key);
            if (want == null) continue;
            // Objects.deepEquals, not equals(): int[]/String[] members compare
            // by identity with equals(), which would always report "differs".
            if (!java.util.Objects.deepEquals(want, existing.get(key))) {
                Log.i(TAG, "Key differs, rewriting: " + key
                        + " want=" + want + " have=" + existing.get(key));
                return false;
            }
        }
        return true;
    }

    private static PersistableBundle readConfig(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enableVoLTE = prefs.getBoolean("volte", true);
        boolean enableVoWiFi = prefs.getBoolean("vowifi", true);
        boolean enableVT = prefs.getBoolean("vt", true);
        boolean enableVoNR = prefs.getBoolean("vonr", true);
        boolean enableCrossSIM = prefs.getBoolean("cross_sim", true);
        boolean enableUT = prefs.getBoolean("ut", true);
        boolean enable5GNR = prefs.getBoolean("5g_nr", true);

        return CarrierConfigCompat.build(enableVoLTE, enableVoWiFi, enableVT, enableVoNR,
                enableCrossSIM, enableUT, enable5GNR);
    }
}
