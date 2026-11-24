package com.hy.home;

/** Adresse de Home Assistant : validation et construction des URL. */
public final class HomeConfig {
  private HomeConfig() {}

  /** Adresse https sans slash final, ou null si absente ou invalide (http refusé). */
  public static String normalizeBaseUrl(String raw) {
    if (raw == null) return null;
    String s = raw.trim();
    if (!s.startsWith("https://") || s.length() <= "https://".length()) return null;
    for (int i = 0; i < s.length(); i++) {
      if (Character.isWhitespace(s.charAt(i))) return null;
    }
    while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
    return s;
  }

  public static String viewUrl(String base, int tab) { return base + Tabs.path(tab); }

  /** Vrai si l'URL appartient à l'instance HA (même origine, pas un domaine qui la prolonge). */
  public static boolean isInside(String base, String url) {
    if (base == null || url == null) return false;
    return url.equals(base) || url.startsWith(base + "/") || url.startsWith(base + "?") || url.startsWith(base + "#");
  }
}
