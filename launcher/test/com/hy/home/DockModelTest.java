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
  }
}
