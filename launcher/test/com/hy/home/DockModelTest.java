package com.hy.home;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public final class DockModelTest {
  private DockModelTest() {}

  private static List<String> concat(List<String> a, String b) {
    List<String> r = new ArrayList<String>(a);
    r.add(b);
    return r;
  }

  public static void run() {
    List<String> d = Arrays.asList("com.netflix.mediaclient", "com.amazon.amazonvideo.livingroom",
        "com.disney.disneyplus", "com.hisilicon.explorer");
    T.eq("défauts", d, DockModel.packages(""));
    T.eq("défauts (null)", d, DockModel.packages(null));
    T.eq("épinglée", concat(d, "org.videolan.vlc"), DockModel.packages("org.videolan.vlc"));
    T.eq("doublon", concat(d, "org.videolan.vlc"), DockModel.packages("com.netflix.mediaclient,org.videolan.vlc"));
    T.eq("ajout", "org.videolan.vlc", DockModel.addPin("", "org.videolan.vlc"));
    T.eq("ajout idempotent", "org.videolan.vlc", DockModel.addPin("org.videolan.vlc", "org.videolan.vlc"));
    T.eq("ajout en fin", "a.b,c.d", DockModel.addPin("a.b", "c.d"));
    T.eq("défaut non épinglé", "a.b", DockModel.addPin("a.b", "com.netflix.mediaclient"));
    T.eq("retrait", "c.d", DockModel.removePin("a.b,c.d", "a.b"));
    T.eq("appli désinstallée masquée", Arrays.asList("com.netflix.mediaclient", "com.hisilicon.explorer"),
        DockModel.visible(d, new HashSet<String>(Arrays.asList("com.netflix.mediaclient", "com.hisilicon.explorer"))));
    T.eq("libellés", "Netflix|Prime Video|Disney+|USB",
        DockModel.label(d.get(0)) + "|" + DockModel.label(d.get(1)) + "|" + DockModel.label(d.get(2)) + "|" + DockModel.label(d.get(3)));
    T.eq("libellé inconnu", null, DockModel.label("org.videolan.vlc"));
    T.eq("défauts exposés", d, Arrays.asList(DockModel.DEFAULTS));
      // « + Ajouter » : applis lançables absentes de la barre, sans le launcher lui-même ni l'accueil d'origine, triées
    java.util.List<String> launchables = Arrays.asList("org.videolan.vlc", "com.netflix.mediaclient", "com.hy.home",
        "com.htc.magcubicos", "com.google.android.youtube.tv", "com.spotify.tv.android");
    java.util.List<String> dock = DockModel.packages("com.spotify.tv.android");
    T.eq("ajoutables", Arrays.asList("com.google.android.youtube.tv", "org.videolan.vlc"),
        DockModel.addable(launchables, dock));
    T.eq("ajoutables vide", new java.util.ArrayList<String>(), DockModel.addable(new java.util.ArrayList<String>(), dock));
    // Les 4 applis d'origine se retirent aussi (liste « masquées ») et reviennent à leur place via « + Ajouter »
    String nf = "com.netflix.mediaclient", dp = "com.disney.disneyplus";
    T.ok("défaut reconnu", DockModel.isDefault(nf) && !DockModel.isDefault("org.videolan.vlc"));
    T.eq("masquage", nf, DockModel.hide("", nf));
    T.eq("masquage idempotent", nf, DockModel.hide(nf, nf));
    T.eq("seuls les défauts se masquent", "", DockModel.hide("", "org.videolan.vlc"));
    T.eq("réaffichage", dp, DockModel.unhide(nf + "," + dp, nf));
    T.eq("défaut masqué absent", Arrays.asList("com.amazon.amazonvideo.livingroom", "com.disney.disneyplus",
        "com.hisilicon.explorer", "org.videolan.vlc"), DockModel.packages("org.videolan.vlc", nf));
    T.eq("tout masqué", Arrays.asList("org.videolan.vlc"),
        DockModel.packages("org.videolan.vlc", "com.netflix.mediaclient,com.amazon.amazonvideo.livingroom,com.disney.disneyplus,com.hisilicon.explorer"));
    T.eq("masquées null", d, DockModel.packages(null, null));
    T.eq("réaffiché à sa place", d, DockModel.packages("", DockModel.unhide(nf, nf)));
    T.eq("défaut masqué ajoutable", Arrays.asList("com.google.android.youtube.tv", nf, "org.videolan.vlc"),
        DockModel.addable(launchables, DockModel.packages("com.spotify.tv.android", nf)));
}
}
