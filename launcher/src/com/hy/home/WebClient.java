package com.hy.home;

import android.util.Log;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Garde la navigation dans Home Assistant, signale les erreurs et la mort du moteur de rendu. */
public final class WebClient extends WebViewClient {
  private final HomeActivity a;

  WebClient(HomeActivity a) { this.a = a; }

  @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
    String url = r.getUrl().toString();
    if (HomeConfig.isInside(a.base, url)) return false;
    Log.w(HomeActivity.TAG, "navigation hors de Home Assistant bloquée");
    return true;
  }

  @Override public void onPageFinished(WebView v, String url) { a.onPageReady(); }

  @Override public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
    if (req != null && req.isForMainFrame()) a.onLoadError(err.getErrorCode() + " " + err.getDescription());
  }

  @Override public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
    a.onRendererGone();
    return true;
  }
}
