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
import android.app.AlertDialog;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;
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
  static final String ORIGIN_BUTTON = "input_button.projecteur_accueil_origine";
  static final int PINK = 0xFFFF8FB1;
  static final long ORIGIN_TIMEOUT_MS = 8000;
  static final int DIALOG_THEME = android.R.style.Theme_Material_Dialog_Alert;
  final UiActions.OriginTimeout originTimeout = new UiActions.OriginTimeout(this);
  final int webId = View.generateViewId();
  final FocusDock focusDock = new FocusDock(this);
  boolean haConnected = false;
  boolean webPaused = false;
  boolean sleeping = false;
  long lastInputMs = 0;
  TextView sleepView;
  boolean pendingOrigin = false;
  View firstTile;
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
    sleepView = new TextView(this);
    sleepView.setTextColor(DIM);
    sleepView.setTextSize(28f);
    sleepView.setGravity(Gravity.CENTER);
    sleepView.setBackgroundColor(0xFF000000);
    sleepView.setVisibility(View.GONE);
    root.addView(sleepView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    setContentView(root);
    scale = getResources().getDisplayMetrics().widthPixels / 1280f;
    buildTopBar();
    createWebView();
    readConfig(getIntent());
    reloadHome();
  }

  @Override protected void onResume() {
    super.onResume();
    buildDock();
    ui.post(focusDock); // la télécommande arrive sur Netflix (après la mise en page)
    lastInputMs = SystemClock.uptimeMillis();
    wake();
    ui.removeCallbacks(clock);
    clock.run();
  }

  /** Hors écran (Netflix devant…) : la page HA est figée, plus aucun calcul ni rendu. */
  @Override protected void onPause() {
    super.onPause();
    ui.removeCallbacks(clock);
    pauseWeb();
  }

  void pauseWeb() {
    if (web != null && !webPaused) { web.onPause(); web.pauseTimers(); webPaused = true; Log.i(TAG, "page figée"); }
  }

  void resumeWeb() {
    if (web != null && webPaused) { web.onResume(); web.resumeTimers(); webPaused = false; Log.i(TAG, "page relancée"); }
  }

  /** Veille de l'écran Maison : page figée et écran sombre avec l'heure, pour limiter la chauffe. */
  void sleep() {
    if (sleeping) return;
    sleeping = true;
    pauseWeb();
    sleepView.setText(Greeting.clock(LocalTime.now()) + "\n\nÉcran Maison en veille\nAppuyez sur une touche");
    sleepView.setVisibility(View.VISIBLE);
    Log.i(TAG, "veille");
  }

  void wake() {
    sleepView.setVisibility(View.GONE);
    resumeWeb();
    if (sleeping) { sleeping = false; Log.i(TAG, "réveil"); }
    burst();
  }

  /** Carte énergie animée quelques secondes (arrivée sur l'écran, réveil, touche), puis figée. */
  void burst() {
    if (web != null && pageReady && !webPaused) web.evaluateJavascript(KioskScript.BURST_CALL, null);
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
    if (!sleeping && IdlePolicy.asleep(lastInputMs, SystemClock.uptimeMillis(), IdlePolicy.TIMEOUT_MS)) sleep();
    if (sleeping) sleepView.setText(Greeting.clock(LocalTime.now()) + "\n\nÉcran Maison en veille\nAppuyez sur une touche");
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
    else focusFirstTile(); // sinon, retour sur la barre d'applis
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
    web.setId(webId);
    webPaused = false; // une WebView neuve tourne : si on était en veille, sleep()/onPause la refigeront
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
    haConnected = false;
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
    haConnected = ok;
    setConnected(ok ? OK : BAD, ok ? "Maison connectée" : "Hors ligne");
  }

  void onPress(String entityId, boolean ok) {
    Log.i(TAG, "appui " + entityId + " → " + ok);
    if (!pendingOrigin || !ORIGIN_BUTTON.equals(entityId)) return;
    pendingOrigin = false;
    ui.removeCallbacks(originTimeout);
    if (ok) AppLauncher.openOriginHome(this); // l'automatisation HA rétablit aussi l'accueil par défaut
    else originFallback("appui refusé par HA");
  }

  // --- barre d'applis ---

  /** Barre centrée en bas : applis visibles, puis « + Ajouter » et « Réglages ». Reconstruite à chaque retour. */
  void buildDock() {
    dock.removeAllViews();
    dock.setPadding(0, Math.round(s(6)), 0, Math.round(s(14)));
    firstTile = null;
    List<String> pkgs = DockModel.visible(DockModel.packages(prefs().getString(KEY_PINS, "")), AppLauncher.launchableSet(this));
    for (String p : pkgs) {
      Drawable banner = AppLauncher.banner(this, p);
      View t = tile(AppLauncher.label(this, p), banner != null ? banner : AppLauncher.icon(this, p), banner != null, null);
      t.setOnClickListener(new UiActions.TileClick(this, UiActions.APP, p));
      if (DockModel.removable(p)) t.setOnLongClickListener(new UiActions.TileLongClick(this, p));
      addTile(t);
    }
    View add = tile("Ajouter", null, false, "+");
    add.setOnClickListener(new UiActions.TileClick(this, UiActions.ADD, null));
    addTile(add);
    View set = tile("Réglages", null, false, "⚙");
    set.setOnClickListener(new UiActions.TileClick(this, UiActions.SETTINGS, null));
    addTile(set);
  }

  void addTile(View t) {
    t.setNextFocusUpId(webId);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.leftMargin = lp.rightMargin = Math.round(s(5));
    dock.addView(t, lp);
    if (firstTile == null) firstTile = t;
  }

  /** Tuile : bannière 16:9 ou icône carrée (ou un symbole), libellé dessous, cadre cyan au focus. */
  View tile(String label, Drawable img, boolean wide, String glyph) {
    LinearLayout col = new LinearLayout(this);
    col.setOrientation(LinearLayout.VERTICAL);
    col.setGravity(Gravity.CENTER_HORIZONTAL);
    int p = Math.round(s(6));
    col.setPadding(p, p, p, Math.round(s(4)));
    col.setFocusable(true);
    col.setClickable(true);
    StateListDrawable bg = new StateListDrawable();
    bg.addState(new int[] {android.R.attr.state_focused}, pill(GLASS2, RING, s(2.5f), s(16)));
    bg.addState(new int[0], pill(Color.TRANSPARENT, 0, 0, s(16)));
    col.setBackground(bg);
    int h = Math.round(s(58));
    int w = wide ? Math.round(s(103)) : h;
    View box;
    if (glyph != null) {
      TextView g = text(26, DIM, false);
      g.setText(glyph);
      g.setGravity(Gravity.CENTER);
      g.setBackground("+".equals(glyph) ? pill(Color.TRANSPARENT, LINE, s(2), s(16)) : pill(GLASS2, 0, 0, s(16)));
      box = g;
    } else {
      ImageView iv = new ImageView(this);
      iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
      if (img != null) iv.setImageDrawable(img);
      iv.setBackground(pill(CARD, 0, 0, s(14)));
      iv.setClipToOutline(true);
      box = iv;
    }
    col.addView(box, new LinearLayout.LayoutParams(w, h));
    TextView name = text(12, DIM, true);
    name.setText(label);
    name.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    nl.topMargin = Math.round(s(5));
    col.addView(name, nl);
    return col;
  }

  void launchApp(String pkg) {
    if (!AppLauncher.launch(this, pkg)) buildDock(); // appli disparue entre-temps
  }

  void showAddDialog() {
    List<String> pkgs = DockModel.addable(AppLauncher.installedLaunchables(this), DockModel.packages(prefs().getString(KEY_PINS, "")));
    AlertDialog.Builder b = new AlertDialog.Builder(this, DIALOG_THEME).setTitle("Ajouter une appli à la barre");
    if (pkgs.isEmpty()) b.setMessage("Aucune autre appli à ajouter.").setPositiveButton("OK", null);
    else {
      String[] labels = new String[pkgs.size()];
      for (int i = 0; i < labels.length; i++) labels[i] = AppLauncher.label(this, pkgs.get(i));
      b.setItems(labels, new UiActions.AddPick(this, pkgs)).setNegativeButton("Annuler", null);
    }
    b.show();
  }

  void pin(String pkg) {
    prefs().edit().putString(KEY_PINS, DockModel.addPin(prefs().getString(KEY_PINS, ""), pkg)).apply();
    buildDock();
  }

  void confirmRemove(String pkg) {
    new AlertDialog.Builder(this, DIALOG_THEME)
        .setTitle("Retirer " + AppLauncher.label(this, pkg) + " de la barre ?")
        .setPositiveButton("Retirer", new UiActions.RemoveConfirm(this, pkg))
        .setNegativeButton("Annuler", null).show();
  }

  void unpin(String pkg) {
    prefs().edit().putString(KEY_PINS, DockModel.removePin(prefs().getString(KEY_PINS, ""), pkg)).apply();
    buildDock();
  }

  // --- menu Réglages ---

  void showSettingsMenu() {
    SpannableString origin = new SpannableString("Revenir à l'accueil d'origine");
    origin.setSpan(new ForegroundColorSpan(PINK), 0, origin.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    CharSequence[] items = {"Recharger l'écran Maison", "Réglages du projecteur (netteté, trapèze, luminosité)",
        "Paramètres Android", origin};
    new AlertDialog.Builder(this, DIALOG_THEME).setTitle("Réglages")
        .setItems(items, new UiActions.MenuPick(this)).show();
  }

  void onMenu(int which) {
    if (which == 0) reloadHome();
    else if (which == 1) navigate(Tabs.PROJECTOR_SETTINGS_PATH);
    else if (which == 2) AppLauncher.openAndroidSettings(this);
    else new AlertDialog.Builder(this, DIALOG_THEME)
        .setTitle("Revenir à l'accueil d'origine du projecteur ?")
        .setMessage("Le launcher Maison reste installé et peut être rétabli.")
        .setPositiveButton("Revenir", new UiActions.OriginConfirm(this))
        .setNegativeButton("Annuler", null).show();
  }

  /** Par HA si la maison est connectée (l'automatisation rétablit l'accueil par défaut), sinon repli local. */
  void goOrigin() {
    if (haConnected && pageReady) {
      pendingOrigin = true;
      ui.removeCallbacks(originTimeout);
      ui.postDelayed(originTimeout, ORIGIN_TIMEOUT_MS);
      web.evaluateJavascript("window.__hy&&window.__hy.press('" + ORIGIN_BUTTON + "')", null);
    } else originFallback("HA déconnecté");
  }

  /** Ouvre Magcubic puis l'écran « Appli d'accueil » pour le choisir par défaut. */
  void originFallback(String why) {
    pendingOrigin = false;
    ui.removeCallbacks(originTimeout);
    Log.i(TAG, "retour à l'origine en local : " + why);
    AppLauncher.openOriginHome(this);
    AppLauncher.openHomeSettings(this);
  }

  /** Sort du mode tactile et place le focus sur la première tuile : sans cela la télécommande ne sélectionne rien. */
  void focusFirstTile() { if (firstTile != null) firstTile.requestFocusFromTouch(); }

  /** Touche directionnelle alors que rien n'a le focus (projecteur resté en mode tactile) → barre d'applis. */
  @Override public boolean dispatchKeyEvent(KeyEvent e) {
    lastInputMs = SystemClock.uptimeMillis();
    if (sleeping) { if (e.getAction() == KeyEvent.ACTION_UP) wake(); return true; } // la touche qui réveille ne fait rien d'autre
    if (e.getAction() == KeyEvent.ACTION_DOWN) burst();
    if (e.getAction() == KeyEvent.ACTION_DOWN && getCurrentFocus() == null && isDpad(e.getKeyCode())) {
      focusFirstTile();
      return true;
    }
    return super.dispatchKeyEvent(e);
  }

  static boolean isDpad(int k) {
    return k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN || k == KeyEvent.KEYCODE_DPAD_LEFT
        || k == KeyEvent.KEYCODE_DPAD_RIGHT || k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER;
  }

  static final class FocusDock implements Runnable {
    final HomeActivity a;
    FocusDock(HomeActivity a) { this.a = a; }
    @Override public void run() { a.focusFirstTile(); }
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent e) {
    if (keyCode == KeyEvent.KEYCODE_MENU) { showSettingsMenu(); return true; }
    return super.onKeyDown(keyCode, e);
  }

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
