package io.github.vvb2060.ims;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "ims_config";
    private static final String TAG = "IMS_MainActivity";
    private static final int REQ_PHONE_STATE = 1001;

    private TextView tvAndroidVersion;
    private TextView tvShizukuStatus;
    private TextView tvPersistentWarning;
    private TextView tvSimInfo;
    private Button btnSelectSim;
    private Button btnSwitchLanguage;
    private Switch switchVoLTE;
    private Switch switchVoWiFi;
    private Switch switchVT;
    private Switch switchVoNR;
    private Switch switchCrossSIM;
    private Switch switchUT;
    private Switch switch5GNR;
    private Button btnApply;

    private SharedPreferences prefs;
    /**
     * Sentinel meaning "apply to every active subscription". Real subscription
     * ids are always positive, so a negative sentinel cannot collide with one.
     * (Upstream used -1 for this, which is also negative; the constant simply
     * makes the intent explicit.)
     */
    private static final int ALL_SIMS = -1;
    /** Selected *subscription id*, not a SIM slot index. Resolved at runtime. */
    private int selectedSubId = ALL_SIMS;

    private final Shizuku.OnBinderReceivedListener binderListener = this::updateShizukuStatus;
    private final Shizuku.OnBinderDeadListener binderDeadListener = this::updateShizukuStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 应用保存的语言设置
        String language = LocaleHelper.getLanguage(this);
        LocaleHelper.updateResources(this, language);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();
        loadPreferences();
        ensurePhoneStatePermission();
        updateSimInfo();
        updateAndroidVersionInfo();
        updateShizukuStatus();
        reportPendingResult();

        Shizuku.addBinderReceivedListener(binderListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
    }

    /**
     * READ_PHONE_STATE is a *dangerous* permission, so declaring it in the
     * manifest is not enough on Android 12 - the user has to grant it at
     * runtime. Without it the subscription list can come back empty and SIM
     * detection silently fails.
     */
    private void ensurePhoneStatePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) return;
        requestPermissions(new String[]{android.Manifest.permission.READ_PHONE_STATE},
                REQ_PHONE_STATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PHONE_STATE) {
            // Whether granted or denied, refresh: Subscriptions falls back to
            // other resolution strategies, so the UI shows reality either way.
            updateSimInfo();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeBinderReceivedListener(binderListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
    }

    private void initViews() {
        tvAndroidVersion = findViewById(R.id.tv_android_version);
        tvShizukuStatus = findViewById(R.id.tv_shizuku_status);
        tvPersistentWarning = findViewById(R.id.tv_persistent_warning);
        tvSimInfo = findViewById(R.id.tv_sim_info);
        btnSelectSim = findViewById(R.id.btn_select_sim);
        btnSwitchLanguage = findViewById(R.id.btn_switch_language);
        // Label shows the language you will switch TO, written in that language.
        btnSwitchLanguage.setText(LocaleHelper.nextLanguageLabel(this));

        // Find switches from included layouts
        switchVoLTE = findViewById(R.id.item_volte).findViewById(R.id.feature_switch);
        switchVoWiFi = findViewById(R.id.item_vowifi).findViewById(R.id.feature_switch);
        switchVT = findViewById(R.id.item_vt).findViewById(R.id.feature_switch);
        switchVoNR = findViewById(R.id.item_vonr).findViewById(R.id.feature_switch);
        switchCrossSIM = findViewById(R.id.item_cross_sim).findViewById(R.id.feature_switch);
        switchUT = findViewById(R.id.item_ut).findViewById(R.id.feature_switch);
        switch5GNR = findViewById(R.id.item_5g_nr).findViewById(R.id.feature_switch);

        // Set feature titles and descriptions
        ((TextView) findViewById(R.id.item_volte).findViewById(R.id.feature_title))
            .setText(R.string.volte);
        ((TextView) findViewById(R.id.item_volte).findViewById(R.id.feature_desc))
            .setText(R.string.volte_desc);

        ((TextView) findViewById(R.id.item_vowifi).findViewById(R.id.feature_title))
            .setText(R.string.vowifi);
        ((TextView) findViewById(R.id.item_vowifi).findViewById(R.id.feature_desc))
            .setText(R.string.vowifi_desc);

        ((TextView) findViewById(R.id.item_vt).findViewById(R.id.feature_title))
            .setText(R.string.vt);
        ((TextView) findViewById(R.id.item_vt).findViewById(R.id.feature_desc))
            .setText(R.string.vt_desc);

        ((TextView) findViewById(R.id.item_vonr).findViewById(R.id.feature_title))
            .setText(R.string.vonr);
        ((TextView) findViewById(R.id.item_vonr).findViewById(R.id.feature_desc))
            .setText(R.string.vonr_desc);

        ((TextView) findViewById(R.id.item_cross_sim).findViewById(R.id.feature_title))
            .setText(R.string.cross_sim);
        ((TextView) findViewById(R.id.item_cross_sim).findViewById(R.id.feature_desc))
            .setText(R.string.cross_sim_desc);

        ((TextView) findViewById(R.id.item_ut).findViewById(R.id.feature_title))
            .setText(R.string.ut);
        ((TextView) findViewById(R.id.item_ut).findViewById(R.id.feature_desc))
            .setText(R.string.ut_desc);

        ((TextView) findViewById(R.id.item_5g_nr).findViewById(R.id.feature_title))
            .setText(R.string._5g_nr);
        ((TextView) findViewById(R.id.item_5g_nr).findViewById(R.id.feature_desc))
            .setText(R.string._5g_nr_desc);

        btnApply = findViewById(R.id.btn_apply);
        btnApply.setOnClickListener(v -> applyConfiguration());

        btnSelectSim.setOnClickListener(v -> showSimSelectionDialog());

        findViewById(R.id.btn_about).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));

        btnSwitchLanguage.setOnClickListener(v -> {
            LocaleHelper.toggleLanguage(this);
            recreate(); // 重新创建 Activity 以应用新语言
        });
    }

    private void showSimSelectionDialog() {
        // Resolve the *real* subscription ids rather than assuming subId == slot+1.
        // See Subscriptions: on the test device the ids are 10 and 11, not 1 and 2.
        int[] subIds = Subscriptions.activeSubIds(this);

        String[] items = {
            getString(R.string.sim_1),
            getString(R.string.sim_2),
            getString(R.string.apply_to_all_sims)
        };

        int selectedIndex = 2; // default: all SIMs
        if (selectedSubId == ALL_SIMS) {
            selectedIndex = 2;
        } else if (subIds.length > 0 && selectedSubId == subIds[0]) {
            selectedIndex = 0;
        } else if (subIds.length > 1 && selectedSubId == subIds[1]) {
            selectedIndex = 1;
        }

        new AlertDialog.Builder(this)
            .setTitle(R.string.select_sim)
            .setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
                if (which == 0 && subIds.length > 0) {
                    selectedSubId = subIds[0];
                } else if (which == 1 && subIds.length > 1) {
                    selectedSubId = subIds[1];
                } else if (which == 1) {
                    // SIM 2 chosen but only one SIM present - keep the existing
                    // selection instead of storing an id that cannot work.
                    Toast.makeText(this, R.string.sim_2_unavailable, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                } else {
                    selectedSubId = ALL_SIMS;
                }
                updateSimInfo();
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void updateSimInfo() {
        if (selectedSubId == ALL_SIMS) {
            tvSimInfo.setText(R.string.apply_to_all_sims);
            btnApply.setText(R.string.apply_to_all);
            return;
        }

        int slot = Subscriptions.slotForSubId(this, selectedSubId);
        if (slot == 0) {
            tvSimInfo.setText(R.string.sim_1);
            btnApply.setText(R.string.apply_to_sim_1);
        } else if (slot == 1) {
            tvSimInfo.setText(R.string.sim_2);
            btnApply.setText(R.string.apply_to_sim_2);
        } else {
            // Could not map to a slot - show the raw subscription id rather than
            // silently labelling it "SIM 1" and misleading the user.
            tvSimInfo.setText(getString(R.string.sim_subid_format, selectedSubId));
            btnApply.setText(R.string.apply_config);
        }
    }

    private void loadPreferences() {
        switchVoLTE.setChecked(prefs.getBoolean("volte", true));
        switchVoWiFi.setChecked(prefs.getBoolean("vowifi", true));
        switchVT.setChecked(prefs.getBoolean("vt", true));
        switchVoNR.setChecked(prefs.getBoolean("vonr", true));
        switchCrossSIM.setChecked(prefs.getBoolean("cross_sim", true));
        switchUT.setChecked(prefs.getBoolean("ut", true));
        switch5GNR.setChecked(prefs.getBoolean("5g_nr", true));
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("volte", switchVoLTE.isChecked());
        editor.putBoolean("vowifi", switchVoWiFi.isChecked());
        editor.putBoolean("vt", switchVT.isChecked());
        editor.putBoolean("vonr", switchVoNR.isChecked());
        editor.putBoolean("cross_sim", switchCrossSIM.isChecked());
        editor.putBoolean("ut", switchUT.isChecked());
        editor.putBoolean("5g_nr", switch5GNR.isChecked());
        // commit(), not apply(): starting the instrumentation kills this process
        // within milliseconds, and apply() only schedules an async disk write
        // that would be lost.
        editor.commit();
    }

    private void updateAndroidVersionInfo() {
        String version = String.format(getString(R.string.android_version),
                "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        tvAndroidVersion.setText(version);

        // Check if it's QPR2 Beta 3 or higher (API 36+)
        if (Build.VERSION.SDK_INT >= 36) {
            tvPersistentWarning.setVisibility(View.VISIBLE);
        } else {
            tvPersistentWarning.setVisibility(View.GONE);
        }

        // Android 12/13 (API 31-32) has no VoNR toggle in CarrierConfig at all -
        // KEY_VONR_ENABLED_BOOL only exists from API 33. Rather than silently
        // accepting a switch that cannot do anything, mark the row as
        // unavailable and explain why. The switch state is preserved so the
        // same preferences keep working on a newer device.
        if (!CarrierConfigCompat.supportsVonrConfigKeys()) {
            switchVoNR.setEnabled(false);
            switchVoNR.setAlpha(0.5f);
            ((TextView) findViewById(R.id.item_vonr).findViewById(R.id.feature_desc))
                    .setText(R.string.vonr_unsupported_desc);
        }
    }

    private void updateShizukuStatus() {
        runOnUiThread(() -> {
            String statusText;
            int statusColor;

            if (!Shizuku.pingBinder()) {
                statusText = String.format(getString(R.string.shizuku_status),
                        getString(R.string.shizuku_not_running));
                statusColor = 0xFFFF0000;
                btnApply.setEnabled(false);
            } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                statusText = String.format(getString(R.string.shizuku_status),
                        getString(R.string.shizuku_no_permission));
                statusColor = 0xFFFF9800;
                btnApply.setEnabled(false);
                requestShizukuPermission();
            } else {
                statusText = String.format(getString(R.string.shizuku_status),
                        getString(R.string.shizuku_ready));
                statusColor = 0xFF4CAF50;
                btnApply.setEnabled(true);
            }

            tvShizukuStatus.setText(statusText);
            tvShizukuStatus.setTextColor(statusColor);
        });
    }

    private void requestShizukuPermission() {
        if (Shizuku.isPreV11()) {
            Toast.makeText(this, R.string.update_shizuku, Toast.LENGTH_LONG).show();
            return;
        }
        Shizuku.requestPermission(0);
    }

    private void applyConfiguration() {
        savePreferences();

        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show();
            return;
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.shizuku_no_permission_msg, Toast.LENGTH_LONG).show();
            requestShizukuPermission();
            return;
        }

        // 保存选中的 SubId 供 PrivilegedProcess 使用
        // commit(), not apply(): the fallback path kills this process.
        prefs.edit().putInt("selected_subid", selectedSubId).commit();

        btnApply.setEnabled(false);
        Toast.makeText(this, R.string.applying_config, Toast.LENGTH_SHORT).show();

        // Prefer writing from this process: it costs nothing and the app stays
        // alive. Only if the platform rejects both in-process mechanisms do we
        // fall back to instrumentation, which force-stops the package.
        final int selection = selectedSubId;
        new Thread(() -> {
            int applied;
            try {
                applied = ImsConfigHelper.applyWithoutRestart(getApplicationContext(), selection);
            } catch (Throwable t) {
                Log.w(TAG, "In-process apply rejected; falling back to instrumentation", t);
                runOnUiThread(() -> applyViaInstrumentation());
                return;
            }
            Log.i(TAG, "In-process apply succeeded for " + applied + " subscription(s)");
            runOnUiThread(() -> {
                btnApply.setEnabled(true);
                prefs.edit().putBoolean("last_apply_ok", true)
                        .putString("last_apply_detail", null).commit();
                showResultDialog();
            });
        }, "ims-apply").start();
    }

    /**
     * Last resort: hand the work to the instrumentation process.
     *
     * <p>{@code startInstrumentation} on our own package makes the system
     * force-stop it ("Force stopping io.github.turboims.pixel ... finished
     * inst"). That is platform behaviour, not a crash, but it looks like one -
     * so say what is about to happen and show the outcome on the next launch.
     */
    private void applyViaInstrumentation() {
        Toast.makeText(this, R.string.applying_config_restart, Toast.LENGTH_LONG).show();
        // commit() for the same reason as in savePreferences().
        prefs.edit().putBoolean("pending_result", true).commit();
        ShizukuProvider.startInstrument(this);
    }

    /**
     * The instrumentation path ends with the platform force-stopping the app,
     * so the result cannot be shown in the same run. PrivilegedProcess records
     * the outcome; show it on the next launch instead of leaving the user
     * guessing whether "the app closed" meant success or failure.
     */
    private void reportPendingResult() {
        if (!prefs.getBoolean("pending_result", false)) return;
        prefs.edit().putBoolean("pending_result", false).apply();

        boolean ok = prefs.getBoolean("last_apply_ok", true);
        String detail = prefs.getString("last_apply_detail", null);
        if (ok) {
            showResultDialog();
        } else {
            showFailureDialog(detail != null && !detail.isEmpty() ? detail : "unknown error");
        }
    }

    private void showResultDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.config_applied)
            .setMessage(R.string.config_success_message)
            .setPositiveButton(R.string.go_to_network_settings, (dialog, which) -> {
                // 跳转到网络设置页面
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open network settings", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.later, null)
            .show();
    }

    private void showFailureDialog(String reason) {
        Log.e(TAG, "Apply reported failure: " + reason);
        new AlertDialog.Builder(this)
            .setTitle(R.string.config_failed_title)
            .setMessage(getString(R.string.config_failed_detail, reason))
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        String language = LocaleHelper.getLanguage(newBase);
        LocaleHelper.updateResources(newBase, language);
        super.attachBaseContext(newBase);
    }
}
