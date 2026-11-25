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
    T.ok("réajusté après masquage de l'en-tête", js.contains("log('kiosk:ok');refit();"));
    // animations infinies (carte énergie) : pause + avance par pas, ~1 image/s au lieu de ~40
    T.ok("pas d'animation 1 s", KioskScript.CALM_MS == 1000);
    T.ok("animations listées par racine", js.contains("R[k].getAnimations()"));
    T.ok("shadow roots parcourus", js.contains("function roots(") && js.contains("out.push(s)"));
    T.ok("racines rafraîchies toutes les 10 pas", js.contains("calmK%10===0"));
    T.ok("seulement les boucles infinies", js.contains("iterations===Infinity"));
    T.ok("mise en pause", js.contains(".pause()"));
    T.ok("avance par pas", js.contains("currentTime=(a.currentTime||0)+CALM"));
    T.ok("cadence", js.contains("var CALM=" + KioskScript.CALM_MS + ";") && js.contains("setInterval(calm,CALM)"));
  }
}
