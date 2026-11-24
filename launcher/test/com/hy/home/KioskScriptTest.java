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
    // tout tient à l'écran : zoom ajusté à la hauteur, borné, sans barre de défilement
    T.ok("ajustement hauteur", js.contains("function fit("));
    T.ok("zoom maximal", js.contains("var ZMAX=1.25"));
    T.ok("zoom minimal", js.contains(",ZMIN=" + KioskScript.MIN_ZOOM + ","));
    T.ok("plus de barre", js.contains("::-webkit-scrollbar{display:none"));
    T.ok("ajusté après navigation", js.contains("addEventListener('location-changed',refit)"));
    T.ok("ajusté au redimensionnement", js.contains("addEventListener('resize',refit)"));
    T.ok("journal fit", js.contains("log('fit "));
  }
}
