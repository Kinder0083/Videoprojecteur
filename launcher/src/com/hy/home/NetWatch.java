package com.hy.home;

/**
 * Retour du Wi-Fi après une veille : le projecteur met environ une minute à se reconnecter.
 * Après une perte du réseau, le premier « réseau disponible » demande un rechargement de la page HA
 * (au lieu d'attendre les nouveaux essais de Home Assistant). Le signal reçu au démarrage, réseau
 * déjà présent, ne recharge rien.
 */
public final class NetWatch {
  /** Attente après le retour du réseau, le temps que le DNS réponde. */
  public static final long SETTLE_MS = 1500L;

  private boolean wasLost;

  public synchronized void lost() { wasLost = true; }

  public synchronized boolean offline() { return wasLost; }

  /** @return true si la page doit être rechargée (réseau revenu après une perte). */
  public synchronized boolean available() {
    boolean reload = wasLost;
    wasLost = false;
    return reload;
  }
}
