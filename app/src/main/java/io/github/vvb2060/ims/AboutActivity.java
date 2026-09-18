package io.github.vvb2060.ims;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.Locale;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Same locale handling as MainActivity so the translated strings show up.
        String language = LocaleHelper.getLanguage(this);
        LocaleHelper.updateResources(this, language);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Button close = findViewById(R.id.btn_about_close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        String language = LocaleHelper.getLanguage(newBase);
        LocaleHelper.updateResources(newBase, language);
        super.attachBaseContext(newBase);
    }
}