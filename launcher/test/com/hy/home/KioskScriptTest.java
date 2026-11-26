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
    T.ok("12 images par seconde", KioskScript.CALM_MS == 83);
    T.ok("racines complètes toutes les 10 s", KioskScript.RESCAN_STEPS == 120);
    T.ok("animations listées par racine", js.contains("R[k].getAnimations()"));
    T.ok("shadow roots parcourus", js.contains("function roots(") && js.contains("out.push(s)"));
    T.ok("plus de recherche périodique de toute la page", js.contains("var SCAN=" + KioskScript.RESCAN_STEPS + ";") && !js.contains("calmK%SCAN===0"));
    // rafale de 10 s à 12 i/s puis carte figée à mi-parcours
    T.ok("rafale de 10 s", KioskScript.BURST_MS == 10000 && js.contains("var BURST=10000;"));
    T.ok("rafale appelable par l'app", js.contains("burst:function(ms){burst(ms);}"));
    T.ok("appel de rafale côté app", KioskScript.BURST_CALL.equals("window.__hy&&window.__hy.burst&&window.__hy.burst(10000)"));
    T.ok("rafale au chargement", js.contains("log('kiosk:ok');refit();burst();"));
    T.ok("figée à mi-parcours", js.contains("a.currentTime=snap(t)") && js.contains("0.5*d"));
    T.ok("recherche limitée à la carte énergie", js.contains("deep(document,'energy-flow-card',0)"));
    T.ok("blocages mesurés", js.contains("PerformanceObserver") && js.contains("'longtask'"));
    T.ok("recherche rapprochée au démarrage", js.contains("(calmK<SCAN&&calmK%12===0)"));
    T.ok("recherche relancée après navigation", js.contains("addEventListener('location-changed',function(){calmK=0;animUntil="));
    T.ok("seules les racines animées sont suivies", js.contains("if(inf(l[i])){R.push(all[k]);break;}"));
    T.ok("seulement les boucles infinies", js.contains("iterations===Infinity"));
    T.ok("mise en pause", js.contains(".pause()"));
    T.ok("avance par pas", js.contains("currentTime=(a.currentTime||0)+CALM"));
    T.ok("cadence", js.contains("var CALM=" + KioskScript.CALM_MS + ";") && js.contains("setInterval(calm,CALM)"));
  }
}
