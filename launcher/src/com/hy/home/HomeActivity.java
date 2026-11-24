package com.hy.home;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Écran d'accueil « Maison » : cadre natif autour d'une WebView Home Assistant. */
public class HomeActivity extends Activity {
  static final String TAG = "hy-home";
  static final String PREFS = "hy_home";
  static final String KEY_URL = "ha_url";
  static final String KEY_PINS = "pins";
  static final int BG = 0xFF0A0E1D;
  static final int FG = 0xFFF2F4FF;
  static final int DIM = 0xFFA3ABCF;
  static final int CARD = 0xFF151C35;
  static final int RING = 0xFF7CF3FF;
  static final double ZOOM = 1.25;
  static final long RETRY_MS = 15000;

  final Handler ui = new Handler(Looper.getMainLooper());
  final RetryTask retry = new RetryTask(this);

  LinearLayout topBar;
  LinearLayout dock;
  FrameLayout webHolder;
  WebView web;
  TextView overlay;
  String base;
  int currentTab = Tabs.HOME;
  boolean pageReady = false;
  boolean loadFailed = false;

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(BG);
    LinearLayout column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    topBar = new LinearLayout(this);
    topBar.setOrientation(LinearLayout.HORIZONTAL);
    topBar.setGravity(Gravity.CENTER_VERTICAL);
    webHolder = new FrameLayout(this);
    dock = new LinearLayout(this);
    dock.setOrientation(LinearLayout.HORIZONTAL);
    dock.setGravity(Gravity.CENTER);
    column.addView(topBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    column.addView(webHolder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    column.addView(dock, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    root.addView(column, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    overlay = new TextView(this);
    overlay.setTextColor(FG);
    overlay.setTextSize(24f);
    overlay.setGravity(Gravity.CENTER);
    overlay.setBackgroundColor(0xE60A0E1D);
    overlay.setPadding(80, 80, 80, 80);
    root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    setContentView(root);
    createWebView();
    readConfig(getIntent());
    reloadHome();
  }

  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    if (intent.getBooleanExtra("crash_renderer", false)) {
      Log.i(TAG, "test : plantage volontaire du moteur WebView");
      web.loadUrl("chrome://crash");
      return;
    }
    if (readConfig(intent)) { currentTab = Tabs.HOME; reloadHome(); return; }
    showTab(Tabs.HOME); // touche Accueil
  }

  @Override public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
          | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
          | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
          | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
  }

  @Override public void onBackPressed() {
    if (web != null && web.canGoBack()) web.goBack(); // un écran d'accueil ne se ferme pas
  }

  SharedPreferences prefs() { return getSharedPreferences(PREFS, MODE_PRIVATE); }

  /** Enregistre l'extra ha_url s'il est valide. Vrai si l'adresse a changé. L'adresse n'est jamais journalisée. */
  boolean readConfig(Intent intent) {
    String before = prefs().getString(KEY_URL, null);
    String raw = intent == null ? null : intent.getStringExtra(KEY_URL);
    if (raw != null) {
      String n = HomeConfig.normalizeBaseUrl(raw);
      if (n == null) Log.w(TAG, "adresse HA refusée (https obligatoire)");
      else { prefs().edit().putString(KEY_URL, n).apply(); Log.i(TAG, "adresse HA enregistrée"); }
    }
    base = prefs().getString(KEY_URL, null);
    return base != null && !base.equals(before);
  }

  void createWebView() {
    web = new WebView(this);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    web.setBackgroundColor(Color.TRANSPARENT);
    CookieManager.getInstance().setAcceptCookie(true);
    web.addJavascriptInterface(new HaBridge(this), "HyBridge");
    web.setWebViewClient(new WebClient(this));
    web.setWebChromeClient(new WebChromeClient());
    webHolder.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
  }

  void showOverlay(String text) { overlay.setText(text); overlay.setVisibility(View.VISIBLE); }

  void hideOverlay() { overlay.setVisibility(View.GONE); }

  /** Recharge complètement l'onglet courant. */
  void reloadHome() {
    ui.removeCallbacks(retry);
    pageReady = false;
    if (base == null) {
      showOverlay("Adresse de Home Assistant non configurée\n\nLance le script HA « Projecteur – configurer le launcher ».");
      return;
    }
    showOverlay("Connexion à la maison…");
    loadFailed = false;
    web.loadUrl(HomeConfig.viewUrl(base, currentTab));
  }

  void showTab(int tab) {
    currentTab = tab;
    navigate(Tabs.path(tab));
  }

  /** Navigation interne à HA (sans recharger la page si elle est prête). */
  void navigate(String path) {
    if (base == null) return;
    if (pageReady) web.evaluateJavascript("window.__hy&&window.__hy.nav('" + path + "')", null);
    else { pageReady = false; web.loadUrl(base + path); }
  }

  // --- appelés par WebClient (fil UI) ---

  void onPageReady() {
    if (loadFailed) return;
    pageReady = true;
    hideOverlay();
    web.evaluateJavascript(KioskScript.build("HyBridge", ZOOM), null);
  }

  void onLoadError(String what) {
    loadFailed = true;
    pageReady = false;
    Log.w(TAG, "chargement impossible : " + what);
    showOverlay("Connexion à la maison…\n\nHome Assistant ne répond pas. Nouvel essai dans 15 s.");
    ui.removeCallbacks(retry);
    ui.postDelayed(retry, RETRY_MS);
  }

  void onRendererGone() {
    webHolder.removeView(web);
    web.destroy();
    createWebView();
    reloadHome();
    Log.i(TAG, "renderer gone, recreated");
  }

  // --- appelés par HaBridge, ramenés sur le fil UI ---

  void onStatus(String payload) { Log.d(TAG, "statut " + payload); }

  void onPress(String entityId, boolean ok) { Log.i(TAG, "appui " + entityId + " → " + ok); }

  /** Relance automatique après une erreur de chargement. */
  static final class RetryTask implements Runnable {
    final HomeActivity a;
    RetryTask(HomeActivity a) { this.a = a; }
    @Override public void run() { a.reloadHome(); }
  }
}
