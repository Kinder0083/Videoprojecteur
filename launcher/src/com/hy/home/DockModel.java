package com.hy.home;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Contenu de la barre d'applis : 4 applis d'origine (masquables) puis les applis épinglées (CSV de paquets). */
public final class DockModel {
  public static final String[] DEFAULTS = {
    "com.netflix.mediaclient", "com.amazon.amazonvideo.livingroom", "com.disney.disneyplus", "com.hisilicon.explorer"
  };
  private static final String[] LABELS = {"Netflix", "Prime Video", "Disney+", "USB"};

  private DockModel() {}

  /** Libellé des applis fixes, null pour les autres (libellé Android). */
  public static String label(String pkg) {
    for (int i = 0; i < DEFAULTS.length; i++) if (DEFAULTS[i].equals(pkg)) return LABELS[i];
    return null;
  }

  public static boolean isDefault(String pkg) { return label(pkg) != null; }

  private static List<String> parse(String csv) {
    List<String> r = new ArrayList<String>();
    if (csv == null) return r;
    for (String p : csv.split(",")) {
      String s = p.trim();
      if (!s.isEmpty() && !isDefault(s) && !r.contains(s)) r.add(s);
    }
    return r;
  }

  private static String join(List<String> l) {
    StringBuilder b = new StringBuilder();
    for (String s : l) { if (b.length() > 0) b.append(','); b.append(s); }
    return b.toString();
  }

  public static List<String> packages(String pinsCsv) { return packages(pinsCsv, null); }

  /** Applis d'origine non masquées (ordre d'origine), puis les épinglées. */
  public static List<String> packages(String pinsCsv, String hiddenCsv) {
    List<String> hidden = parseDefaults(hiddenCsv);
    List<String> r = new ArrayList<String>();
    for (String p : DEFAULTS) if (!hidden.contains(p)) r.add(p);
    r.addAll(parse(pinsCsv));
    return r;
  }

  private static List<String> parseDefaults(String csv) {
    List<String> r = new ArrayList<String>();
    if (csv == null) return r;
    for (String p : csv.split(",")) {
      String s = p.trim();
      if (isDefault(s) && !r.contains(s)) r.add(s);
    }
    return r;
  }

  /** Masque une appli d'origine (sans effet sur les autres paquets). */
  public static String hide(String hiddenCsv, String pkg) {
    List<String> l = parseDefaults(hiddenCsv);
    if (isDefault(pkg) && !l.contains(pkg)) l.add(pkg);
    return join(l);
  }

  /** Réaffiche une appli d'origine masquée : elle revient à sa place d'origine. */
  public static String unhide(String hiddenCsv, String pkg) {
    List<String> l = parseDefaults(hiddenCsv);
    l.remove(pkg);
    return join(l);
  }

  public static String addPin(String csv, String pkg) {
    List<String> l = parse(csv);
    if (pkg != null && !isDefault(pkg) && !l.contains(pkg)) l.add(pkg);
    return join(l);
  }

  public static String removePin(String csv, String pkg) {
    List<String> l = parse(csv);
    l.remove(pkg);
    return join(l);
  }

  /** Garde l'ordre, retire les paquets absents de l'appareil. */
  public static List<String> visible(List<String> pkgs, Set<String> installed) {
    List<String> r = new ArrayList<String>();
    for (String p : pkgs) if (installed.contains(p)) r.add(p);
    return r;
  }

  /** Ni le launcher Maison ni l'accueil d'origine ne s'épinglent. */
  static final String[] NEVER = {"com.hy.home", "com.htc.magcubicos"};

  /** Applis proposées par « + Ajouter » : lançables, absentes de la barre, triées par paquet. */
  public static List<String> addable(List<String> launchables, List<String> dock) {
    List<String> r = new ArrayList<String>();
    for (String p : launchables) {
      if (dock.contains(p) || r.contains(p) || Arrays.asList(NEVER).contains(p)) continue;
      r.add(p);
    }
    java.util.Collections.sort(r);
    return r;
  }
}
