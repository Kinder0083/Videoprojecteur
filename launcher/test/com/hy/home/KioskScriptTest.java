package com.hy.home;

public final class KioskScriptTest {
  private KioskScriptTest() {}

  public static void run() {
    String js = KioskScript.build("HyBridge", 1.25);
    T.ok("garde", js.startsWith("(function(){if(window.__hyKiosk)return;window.__hyKiosk=1;"));
    String[] must = {"dockedSidebar", "\"always_hidden\"", "selectedTheme", "\"dark\":true", "hui-root",
        "location-changed", "HyBridge.onStatus(", "HyBridge.onPress(", "input_button", "weather.ideaux4",
        "sensor.ideaux4_temperature", "--card-background-color", "#151c35", "zoom", "1.25", "window.__hy={"};
    for (String s : must) T.ok("contient " + s, js.contains(s));
    T.ok("autre pont", KioskScript.build("Autre", 1.0).contains("Autre.onStatus("));
  }
}
