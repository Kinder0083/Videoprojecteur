package com.hy.home;

public final class StatusParserTest {
  private StatusParserTest() {}

  public static void run() {
    T.eq("pluie", "🌧️ 10,8 °C · Pluie", StatusParser.chip("1|rainy|10.8"));
    T.eq("soleil", "☀️ 24,5 °C · Ensoleillé", StatusParser.chip("1|sunny|24.5"));
    T.eq("nuit entier", "🌙 6 °C · Nuit claire", StatusParser.chip("1|clear-night|6"));
    T.eq("temp indispo", "☁️ Nuageux", StatusParser.chip("1|cloudy|unavailable"));
    T.eq("condition inconnue", "🌡️ 12 °C", StatusParser.chip("1|bizarre|12"));
    T.eq("déconnecté", "", StatusParser.chip("0||"));
    T.eq("null", "", StatusParser.chip(null));
    T.eq("charabia", "", StatusParser.chip("garbage"));
    T.ok("connecté", StatusParser.connected("1|rainy|10.8"));
    T.ok("pas connecté", !StatusParser.connected("0||"));
  }
}
