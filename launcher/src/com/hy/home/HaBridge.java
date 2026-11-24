package com.hy.home;

import android.util.Log;
import android.webkit.JavascriptInterface;

/** Objet « HyBridge » exposé à la page HA. Les appels arrivent sur un fil JS : on les reposte sur le fil UI. */
public final class HaBridge {
  private final HomeActivity a;

  HaBridge(HomeActivity a) { this.a = a; }

  @JavascriptInterface public void onStatus(String payload) { a.ui.post(new StatusTask(a, payload)); }

  @JavascriptInterface public void onPress(String entityId, boolean ok) { a.ui.post(new PressTask(a, entityId, ok)); }

  @JavascriptInterface public void log(String msg) { Log.i(HomeActivity.TAG, "js: " + msg); }

  static final class StatusTask implements Runnable {
    final HomeActivity a; final String payload;
    StatusTask(HomeActivity a, String payload) { this.a = a; this.payload = payload; }
    @Override public void run() { a.onStatus(payload); }
  }

  static final class PressTask implements Runnable {
    final HomeActivity a; final String id; final boolean ok;
    PressTask(HomeActivity a, String id, boolean ok) { this.a = a; this.id = id; this.ok = ok; }
    @Override public void run() { a.onPress(id, ok); }
  }
}
