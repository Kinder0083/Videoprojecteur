package com.hy.home;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import java.time.LocalDate;
import java.time.LocalTime;
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
  static final int GLASS = 0x0FFFFFFF;
  static final int GLASS2 = 0x1CFFFFFF;
  static final int LINE = 0x1CFFFFFF;
  static final int OK = 0xFF43E59A;
  static final int BAD = 0xFFFF5C6C;
  static final double ZOOM = 1.25;
  static final long RETRY_MS = 15000;
  static final long CLOCK_MS = 30000;

  final Handler ui = new Handler(Looper.getMainLooper());
  final RetryTask retry = new RetryTask(this);
  final ClockTask clock = new ClockTask(this);

  float scale = 1f;
  TextView helloView, dateView, clockView, wxChip, netChip;
  TextView[] tabViews;
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
    scale = getResources().getDisplayMetrics().widthPixels / 1280f;
    buildTopBar();
    createWebView();
    readConfig(getIntent());
    reloadHome();
  }

  @Override protected void onResume() {
    super.onResume();
    ui.removeCallbacks(clock);
    clock.run();
  }

  @Override protected void onPause() {
    super.onPause();
    ui.removeCallbacks(clock);
  }

  /** Taille de la maquette (écran de 1280 px de large) convertie en pixels réels. */
  float s(float mockPx) { return mockPx * scale; }

  static GradientDrawable pill(int fill, int stroke, float strokeW, float radius) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(fill);
    g.setCornerRadius(radius);
    if (strokeW > 0) g.setStroke(Math.round(strokeW), stroke);
    return g;
  }

  TextView text(float sizeMock, int color, boolean bold) {
    TextView t = new TextView(this);
    t.setTextSize(TypedValue.COMPLEX_UNIT_PX, s(sizeMock));
    t.setTextColor(color);
    t.setSingleLine(true);
    if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
    return t;
  }

  TextView chip() {
    TextView c = text(14.5f, FG, true);
    c.setGravity(Gravity.CENTER_VERTICAL);
    c.setPadding(Math.round(s(15)), 0, Math.round(s(15)), 0);
    c.setBackground(pill(GLASS, LINE, s(1), s(20)));
    c.setCompoundDrawablePadding(Math.round(s(8)));
    return c;
  }

  LinearLayout.LayoutParams wrap(float leftMarginMock, float heightMock) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
        heightMock > 0 ? Math.round(s(heightMock)) : ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.leftMargin = Math.round(s(leftMarginMock));
    return lp;
  }

  /** Barre du haut : salutation et date, onglets, météo, connexion, heure. */
  void buildTopBar() {
    topBar.setPadding(Math.round(s(28)), Math.round(s(20)), Math.round(s(28)), Math.round(s(12)));
    LinearLayout left = new LinearLayout(this);
    left.setOrientation(LinearLayout.VERTICAL);
    helloView = text(30, FG, true);
    dateView = text(14, DIM, false);
    left.addView(helloView);
    left.addView(dateView);
    topBar.addView(left, wrap(0, 0));

    LinearLayout tabs = new LinearLayout(this);
    tabs.setOrientation(LinearLayout.HORIZONTAL);
    int p4 = Math.round(s(4));
    tabs.setPadding(p4, p4, p4, p4);
    tabs.setBackground(pill(GLASS, LINE, s(1), s(24)));
    tabViews = new TextView[Tabs.ORDER.length];
    for (int i = 0; i < Tabs.ORDER.length; i++) {
      int tab = Tabs.ORDER[i];
      TextView t = text(13, DIM, true);
      t.setText(Tabs.label(tab));
      t.setPadding(Math.round(s(13)), Math.round(s(7)), Math.round(s(13)), Math.round(s(7)));
      t.setFocusable(true);
      t.setClickable(true);
      t.setOnClickListener(new TabClick(this, tab));
      tabViews[i] = t;
      tabs.addView(t, wrap(i == 0 ? 0 : 4, 0));
    }
    topBar.addView(tabs, wrap(18, 0));

    View spacer = new View(this);
    topBar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

    wxChip = chip();
    wxChip.setVisibility(View.GONE);
    topBar.addView(wxChip, wrap(12, 40));
    netChip = chip();
    topBar.addView(netChip, wrap(12, 40));
    clockView = text(30, FG, true);
    topBar.addView(clockView, wrap(16, 0));

    styleTabs();
    setConnected(DIM, "Connexion…");
  }

  /** Onglet actif en pastille claire ; l'onglet qui a le focus télécommande est cerclé. */
  void styleTabs() {
    for (int i = 0; i < tabViews.length; i++) {
      boolean on = Tabs.ORDER[i] == currentTab;
      int fill = on ? FG : Color.TRANSPARENT;
      StateListDrawable bg = new StateListDrawable();
      bg.addState(new int[] {android.R.attr.state_focused}, pill(on ? FG : GLASS2, RING, s(2), s(20)));
      bg.addState(new int[0], pill(fill, 0, 0, s(20)));
      tabViews[i].setBackground(bg);
      int[][] states = {{android.R.attr.state_focused}, {}};
      int[] colors = {on ? BG : FG, on ? BG : DIM};
      tabViews[i].setTextColor(new ColorStateList(states, colors));
    }
  }

  void setConnected(int dotColor, String label) {
    GradientDrawable dot = new GradientDrawable();
    dot.setShape(GradientDrawable.OVAL);
    dot.setColor(dotColor);
    int d = Math.round(s(9));
    dot.setSize(d, d);
    dot.setBounds(0, 0, d, d);
    netChip.setCompoundDrawables(dot, null, null, null);
    netChip.setText(label);
  }

  /** Salutation, date et heure. */
  void refreshClock() {
    LocalTime now = LocalTime.now();
    helloView.setText(Greeting.hello(now));
    dateView.setText(capitalize(Greeting.date(LocalDate.now())));
    clockView.setText(Greeting.clock(now));
  }

  static String capitalize(String s) {
    return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
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
    styleTabs();
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
    setConnected(BAD, "Hors ligne");
    wxChip.setVisibility(View.GONE);
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

  void onStatus(String payload) {
    Log.d(TAG, "statut " + payload);
    String c = StatusParser.chip(payload);
    wxChip.setText(c);
    wxChip.setVisibility(c.isEmpty() ? View.GONE : View.VISIBLE);
    boolean ok = StatusParser.connected(payload);
    setConnected(ok ? OK : BAD, ok ? "Maison connectée" : "Hors ligne");
  }

  void onPress(String entityId, boolean ok) { Log.i(TAG, "appui " + entityId + " → " + ok); }

  /** Rafraîchit l'heure toutes les 30 s tant que l'accueil est visible. */
  static final class ClockTask implements Runnable {
    final HomeActivity a;
    ClockTask(HomeActivity a) { this.a = a; }
    @Override public void run() { a.refreshClock(); a.ui.postDelayed(this, CLOCK_MS); }
  }

  static final class TabClick implements View.OnClickListener {
    final HomeActivity a; final int tab;
    TabClick(HomeActivity a, int tab) { this.a = a; this.tab = tab; }
    @Override public void onClick(View v) { a.showTab(tab); }
  }

  /** Relance automatique après une erreur de chargement. */
  static final class RetryTask implements Runnable {
    final HomeActivity a;
    RetryTask(HomeActivity a) { this.a = a; }
    @Override public void run() { a.reloadHome(); }
  }
}
