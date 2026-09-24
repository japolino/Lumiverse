package chat.lumiverse.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.*;
import android.widget.*;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private WebView web;
    private String origin = "";
    private String pageScript;
    private boolean volumePaging;
    private boolean loaded;
    private final VolumePagingKeys pagingKeys = new VolumePagingKeys();

    // Lumiverse requires JavaScript; native bridges and file access stay disabled.
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try (var input = getAssets().open("page.js")) {
            var bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            pageScript = bytes.toString(StandardCharsets.UTF_8.name());
        } catch (Exception error) { throw new IllegalStateException(error); }
        var prefs = getPreferences(MODE_PRIVATE);
        origin = prefs.getString("origin", "");
        volumePaging = prefs.getBoolean("volumePaging", false);
        var layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        var bar = new LinearLayout(this);
        addButton(bar, "Server", this::configure);
        addButton(bar, "Up", () -> page(-1));
        addButton(bar, "Down", () -> page(1));
        var toggle = new Switch(this);
        toggle.setText(R.string.volume_paging);
        toggle.setChecked(volumePaging);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            volumePaging = checked;
            prefs.edit().putBoolean("volumePaging", checked).apply();
        });
        layout.addView(bar);
        layout.addView(toggle);
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.getSettings().setSupportZoom(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri url = request.getUrl();
                if (sameOrigin(url)) return false;
                if (request.hasGesture() && ("https".equals(url.getScheme()) || "http".equals(url.getScheme()))) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, url)); } catch (android.content.ActivityNotFoundException ignored) { }
                }
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) { loaded = false; }
            @Override public void onPageFinished(WebView view, String url) { loaded = sameOrigin(Uri.parse(url)); }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    loaded = false;
                    Toast.makeText(MainActivity.this, "Could not load server. Check the address and connection.", Toast.LENGTH_LONG).show();
                }
            }
        });
        layout.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);
        layout.requestApplyInsets();
        if (!origin.isEmpty()) web.loadUrl(origin); else configure();
    }

    private void addButton(LinearLayout bar, String label, Runnable action) {
        var button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> action.run());
        bar.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private boolean sameOrigin(Uri url) {
        Uri selected = Uri.parse(origin);
        return "https".equals(url.getScheme()) && selected.getHost() != null
            && selected.getHost().equalsIgnoreCase(url.getHost())
            && (selected.getPort() == -1 ? 443 : selected.getPort()) == (url.getPort() == -1 ? 443 : url.getPort());
    }

    private void configure() {
        var input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://your-lumiverse-server");
        input.setText(origin);
        var dialog = new AlertDialog.Builder(this).setTitle("Lumiverse server")
            .setMessage("Enter your server's HTTPS origin. Sign in inside the app. Volume paging is optional and replaces volume controls while this app is open.")
            .setView(input).setNegativeButton("Cancel", null).setPositiveButton("Connect", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            Uri url = Uri.parse(input.getText().toString().trim());
            if (!"https".equals(url.getScheme()) || url.getHost() == null || url.getUserInfo() != null
                || (url.getPath() != null && !url.getPath().isEmpty() && !url.getPath().equals("/"))
                || url.getQuery() != null || url.getFragment() != null) {
                input.setError("Enter an HTTPS origin without a path, credentials, query or fragment."); return;
            }
            origin = url.buildUpon().path("").build().toString();
            getPreferences(MODE_PRIVATE).edit().putString("origin", origin).apply();
            loaded = false;
            web.loadUrl(origin);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void page(int direction) {
        if (loaded && sameOrigin(Uri.parse(web.getUrl() == null ? "" : web.getUrl())))
            web.evaluateJavascript(pageScript.replace("__DIRECTION__", Integer.toString(direction)), null);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int decision = pagingKeys.handle(event.getKeyCode(), event.getAction(), event.getRepeatCount(),
            volumePaging && loaded && web.hasWindowFocus());
        if (decision == VolumePagingKeys.UP) page(-1);
        if (decision == VolumePagingKeys.DOWN) page(1);
        if (decision != VolumePagingKeys.PASS) return true;
        return super.dispatchKeyEvent(event);
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
    @Override protected void onPause() { pagingKeys.reset(); web.onPause(); CookieManager.getInstance().flush(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }
}
