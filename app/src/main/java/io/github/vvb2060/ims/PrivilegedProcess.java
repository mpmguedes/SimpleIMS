package io.github.vvb2060.ims;

import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class PrivilegedProcess extends Instrumentation {

    private static final String PREFS_NAME = "ims_config";

    @Override
    public void onCreate(Bundle arguments) {
        Log.i("PrivilegedProcess", "onCreate called");

        // 等待 Shizuku binder 准备好
        int maxRetries = 50; // 最多等待 5 秒
        for (int i = 0; i < maxRetries; i++) {
            if (rikka.shizuku.Shizuku.pingBinder()) {
                Log.i("PrivilegedProcess", "Shizuku binder is ready");
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }

        String detail;
        try {
            int applied = overrideConfig();
            if (applied == 0) {
                detail = "no subscription was updated";
                Log.w("PrivilegedProcess", "overrideConfig applied nothing");
            } else {
                detail = null;
                Log.i("PrivilegedProcess", "overrideConfig completed successfully ("
                        + applied + " subscription(s))");
            }
        } catch (Exception e) {
            Log.e("PrivilegedProcess", "Failed to override config", e);
            detail = e.getClass().getSimpleName()
                    + (e.getMessage() != null ? ": " + e.getMessage() : "");
        }

        // Record the outcome so the UI can report what really happened instead
        // of always claiming success. Same package, so SharedPreferences is a
        // safe channel - but commit(), not apply(): finish() ends the
        // instrumentation and the OS force-stops this process immediately after,
        // which would discard an async write.
        try {
            getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("last_apply_ok", detail == null)
                    .putString("last_apply_detail", detail)
                    .commit();
        } catch (Throwable t) {
            Log.w("PrivilegedProcess", "Could not record apply result", t);
        }

        finish(0, new Bundle());
    }

    /** @return the number of subscriptions that now carry the config */
    private int overrideConfig() throws Exception {
        Log.i("PrivilegedProcess", "overrideConfig started");
        // 读取用户选择的 SubId
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int selectedSubId = prefs.getInt("selected_subid", Subscriptions.ALL_SIMS);
        Log.i("PrivilegedProcess", "Stored SubId: " + selectedSubId);

        // Same implementation the in-process path uses - see ImsConfigHelper.
        // (Only reached when the UI process was refused, i.e. rare.)
        return ImsConfigHelper.applyWithoutRestart(getContext(), selectedSubId);
    }
}
