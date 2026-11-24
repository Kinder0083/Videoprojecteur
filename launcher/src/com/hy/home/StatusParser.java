package com.hy.home;

import java.util.HashMap;
import java.util.Map;

/** Traduit l'état envoyé par la page HA (« 1|condition|température ») en pastille météo. */
public final class StatusParser {
  private static final Map<String, String[]> COND = new HashMap<String, String[]>();

  static {
    put("sunny", "☀️", "Ensoleillé");
    put("clear-night", "🌙", "Nuit claire");
    put("partlycloudy", "⛅", "Éclaircies");
    put("cloudy", "☁️", "Nuageux");
    put("rainy", "🌧️", "Pluie");
    put("pouring", "🌧️", "Averses");
    put("lightning", "⛈️", "Orage");
    put("lightning-rainy", "⛈️", "Orage");
    put("snowy", "🌨️", "Neige");
    put("snowy-rainy", "🌨️", "Pluie et neige");
    put("hail", "🌨️", "Grêle");
    put("fog", "🌫️", "Brouillard");
    put("windy", "💨", "Venteux");
    put("windy-variant", "💨", "Venteux");
    put("exceptional", "⚠️", "Exceptionnel");
  }

  private StatusParser() {}

  private static void put(String k, String emoji, String label) { COND.put(k, new String[] {emoji, label}); }

  public static boolean connected(String payload) {
    String[] p = split(payload);
    return p != null && "1".equals(p[0]);
  }

  public static String chip(String payload) {
    String[] p = split(payload);
    if (p == null || !"1".equals(p[0])) return "";
    String temp = formatTemp(p[2]);
    String[] c = COND.get(p[1]);
    if (c == null) return temp == null ? "" : "🌡️ " + temp + " °C";
    return temp == null ? c[0] + " " + c[1] : c[0] + " " + temp + " °C · " + c[1];
  }

  private static String[] split(String payload) {
    if (payload == null) return null;
    String[] p = payload.split("\\|", -1);
    return p.length == 3 ? p : null;
  }

  /** Une décimale avec virgule, « ,0 » supprimé ; null si non numérique. */
  private static String formatTemp(String raw) {
    double v;
    try { v = Double.parseDouble(raw.trim()); } catch (NumberFormatException e) { return null; }
    if (Double.isNaN(v) || Double.isInfinite(v)) return null;
    long tenths = Math.round(v * 10);
    String s = (tenths % 10 == 0) ? String.valueOf(tenths / 10) : String.valueOf(tenths / 10.0);
    return s.replace('.', ',');
  }
}
