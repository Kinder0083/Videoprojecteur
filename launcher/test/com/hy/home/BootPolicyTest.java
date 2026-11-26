package com.hy.home;

public final class BootPolicyTest {
  private BootPolicyTest() {}

  public static void run() {
    T.ok("Maison choisi comme accueil : on passe devant Magcubic", BootPolicy.takeOver("com.hy.home"));
    T.ok("accueil d'origine choisi : on ne s'impose pas", !BootPolicy.takeOver("com.htc.magcubicos"));
    T.ok("aucun accueil par défaut (choix demandé) : on ne s'impose pas", !BootPolicy.takeOver("android"));
    T.ok("résolution impossible : on ne s'impose pas", !BootPolicy.takeOver(null));
    T.eq("délai après la fin du démarrage", 3000L, BootPolicy.DELAY_MS);
  }
}
