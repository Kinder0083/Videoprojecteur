package com.hy.home;

public final class HomeConfigTest {
  private HomeConfigTest() {}

  public static void run() {
    T.eq("trailing slash", "https://ha.example.org:8123", HomeConfig.normalizeBaseUrl("https://ha.example.org:8123/"));
    T.eq("trim", "https://ha.example.org:8123", HomeConfig.normalizeBaseUrl("  https://ha.example.org:8123  "));
    T.eq("http refusé", null, HomeConfig.normalizeBaseUrl("http://ha.example.org:8123"));
    T.eq("vide", null, HomeConfig.normalizeBaseUrl(""));
    T.eq("null", null, HomeConfig.normalizeBaseUrl(null));
    T.eq("espace", null, HomeConfig.normalizeBaseUrl("https://bad host"));
    T.eq("accueil", "/projecteur-maison/accueil", Tabs.path(Tabs.HOME));
    T.eq("caméras", "/projecteur-maison/cameras", Tabs.path(Tabs.CAMERAS));
    T.eq("lumières", "/ma-maison/lumieres", Tabs.path(Tabs.LIGHTS));
    T.eq("volets", "/ma-maison/volets", Tabs.path(Tabs.COVERS));
    T.eq("climat", "/ma-maison/climat", Tabs.path(Tabs.CLIMATE));
    T.eq("labels", "Accueil,Caméras,Lumières,Volets,Climat",
        Tabs.label(0) + "," + Tabs.label(1) + "," + Tabs.label(2) + "," + Tabs.label(3) + "," + Tabs.label(4));
    T.eq("ordre", 5, Tabs.ORDER.length);
    T.eq("réglages projecteur", "/projecteur-hy350", Tabs.PROJECTOR_SETTINGS_PATH);
    T.eq("viewUrl", "https://ha.example.org:8123/projecteur-maison/cameras",
        HomeConfig.viewUrl("https://ha.example.org:8123", Tabs.CAMERAS));
    T.ok("inside auth", HomeConfig.isInside("https://ha.example.org:8123", "https://ha.example.org:8123/auth/authorize?x=1"));
    T.ok("dehors", !HomeConfig.isInside("https://ha.example.org:8123", "https://evil.org/"));
    T.ok("suffixe piège", !HomeConfig.isInside("https://ha.example.org:8123", "https://ha.example.org:8123.evil.org/"));
  }
}
