package com.hy.home;

/**
 * Au démarrage, le firmware Magcubic lance son propre accueil de force, sans tenir compte de
 * l'accueil par défaut. Maison ne repasse devant que s'il est bien l'accueil choisi.
 */
public final class BootPolicy {
  /** Attente après la fin du démarrage, le temps que l'accueil Magcubic ait fini de s'ouvrir. */
  public static final long DELAY_MS = 3000L;
  static final String SELF = "com.hy.home";

  private BootPolicy() {}

  /** @param defaultHomePkg paquet qui répond à l'intention HOME par défaut (null si inconnu). */
  public static boolean takeOver(String defaultHomePkg) { return SELF.equals(defaultHomePkg); }
}
