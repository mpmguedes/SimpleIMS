package io.github.vvb2060.ims;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the subscription ids ("subId") that actually exist on the device.
 *
 * <p>A subId is NOT a SIM slot index. The platform assigns subIds itself and
 * they are neither small nor stable: on the Android 12 test device the two SIMs
 * are subId 10 and 11, not 1 and 2. Upstream TurboIMS assumed subId == slot+1,
 * which makes the platform fail its subId-&gt;phoneId lookup inside
 * {@code CarrierConfigManager.overrideConfig} and throw
 * {@code IllegalArgumentException: Invalid phoneId 2147483647 for subId 1}.
 *
 * <p>This class is the single place that knowledge lives, so MainActivity (UI)
 * and PrivilegedProcess (the privileged write) cannot drift apart.
 *
 * <p>Three resolution strategies are tried in order, because which one works
 * depends on what the calling identity is allowed to see:
 * <ol>
 *   <li>{@code SubscriptionManager.getActiveSubscriptionInfoList()} - public
 *       since API 23. Needs READ_PHONE_STATE, but gives slot index too.</li>
 *   <li>{@code SubscriptionManager.getActiveSubscriptionIdList()} - hidden
 *       ({@code @hide}), so reached by reflection. The app exempts hidden API
 *       access via LSPass in ShizukuProvider, so this is legal here.</li>
 *   <li>{@code SubscriptionManager.getDefaultSubscriptionId()} - public since
 *       API 24 and needs no permission. Yields one usable id rather than none.</li>
 * </ol>
 */
final class Subscriptions {

    private static final String TAG = "Subscriptions";

    /** Sentinel for "every active subscription". Real subIds are positive. */
    static final int ALL_SIMS = -1;

    /** Returned when a subId could not be mapped to a SIM slot. */
    static final int SLOT_UNKNOWN = -1;

    private Subscriptions() {}

    /**
     * Subscription ids of all active subscriptions, ordered by SIM slot index
     * so that index 0 in the result is SIM 1. Empty when nothing can be read.
     */
    @SuppressWarnings("MissingPermission")
    static int[] activeSubIds(Context context) {
        if (context == null) return new int[0];

        // Strategy 1: public API, gives both subId and slot index.
        try {
            SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
            if (sm != null) {
                List<SubscriptionInfo> infos = sm.getActiveSubscriptionInfoList();
                if (infos != null && !infos.isEmpty()) {
                    List<SubscriptionInfo> sorted = new ArrayList<>(infos);
                    sorted.sort((a, b) -> Integer.compare(safeSlot(a), safeSlot(b)));
                    int[] ids = new int[sorted.size()];
                    for (int i = 0; i < sorted.size(); i++) {
                        ids[i] = sorted.get(i).getSubscriptionId();
                    }
                    Log.i(TAG, "Resolved subIds via getActiveSubscriptionInfoList(): "
                            + java.util.Arrays.toString(ids));
                    return ids;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "getActiveSubscriptionInfoList() unavailable", t);
        }

        // Strategy 2: hidden getActiveSubscriptionIdList() via reflection.
        try {
            SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
            if (sm != null) {
                int[] ids = (int[]) SubscriptionManager.class
                        .getMethod("getActiveSubscriptionIdList")
                        .invoke(sm);
                if (ids != null && ids.length > 0) {
                    Log.i(TAG, "Resolved subIds via hidden getActiveSubscriptionIdList(): "
                            + java.util.Arrays.toString(ids));
                    return ids;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "hidden getActiveSubscriptionIdList() unavailable", t);
        }

        // Strategy 3: at least one id we know is valid, no permission needed.
        try {
            int def = SubscriptionManager.getDefaultSubscriptionId();
            if (def != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                Log.i(TAG, "Falling back to default subId: " + def);
                return new int[]{def};
            }
        } catch (Throwable t) {
            Log.w(TAG, "getDefaultSubscriptionId() unavailable", t);
        }

        Log.w(TAG, "No active subscription id could be resolved");
        return new int[0];
    }

    /** The subId currently in the given SIM slot (0-based), or -1. */
    static int subIdForSlot(Context context, int slot) {
        int[] ids = activeSubIds(context);
        if (slot >= 0 && slot < ids.length) {
            return ids[slot];
        }
        return -1;
    }

    /** The SIM slot index (0-based) holding the given subId, or {@link #SLOT_UNKNOWN}. */
    @SuppressWarnings("MissingPermission")
    static int slotForSubId(Context context, int subId) {
        if (context == null || subId < 0) return SLOT_UNKNOWN;
        try {
            SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
            if (sm != null) {
                List<SubscriptionInfo> infos = sm.getActiveSubscriptionInfoList();
                if (infos != null) {
                    for (SubscriptionInfo info : infos) {
                        if (info != null && info.getSubscriptionId() == subId) {
                            return info.getSimSlotIndex();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Unable to map subId " + subId + " to a slot", t);
        }
        return SLOT_UNKNOWN;
    }

    /**
     * Picks the subIds that should be written for a selection.
     *
     * @param selected a real subId, or {@link #ALL_SIMS} for every active one
     * @return never null; empty when there is nothing safe to write to
     */
    static int[] resolveTargets(Context context, int selected) {
        int[] active = activeSubIds(context);
        if (active.length == 0) {
            Log.w(TAG, "No active subscriptions; nothing to write");
            return new int[0];
        }
        if (selected == ALL_SIMS) {
            return active;
        }
        for (int id : active) {
            if (id == selected) return new int[]{selected};
        }
        // The stored subId is stale (SIM swapped / eSIM profile changed).
        // Writing it would throw, so fall back to everything active instead of
        // failing - and make that visible in the log rather than silent.
        Log.w(TAG, "Stored subId " + selected + " is not active; applying to all: "
                + java.util.Arrays.toString(active));
        return active;
    }

    private static int safeSlot(SubscriptionInfo info) {
        if (info == null) return Integer.MAX_VALUE;
        try {
            int slot = info.getSimSlotIndex();
            return slot < 0 ? Integer.MAX_VALUE : slot;
        } catch (Throwable t) {
            return Integer.MAX_VALUE;
        }
    }
}
