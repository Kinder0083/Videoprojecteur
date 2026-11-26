package com.hy.home;

public final class NetWatchTest {
  private NetWatchTest() {}

  public static void run() {
    NetWatch n = new NetWatch();
    T.ok("réseau présent au démarrage : pas de rechargement", !n.available());
    T.ok("pas de perte : hors ligne faux", !n.offline());
    n.lost();
    T.ok("perte du Wi-Fi : hors ligne", n.offline());
    T.ok("retour du Wi-Fi après une perte : recharger", n.available());
    T.ok("après le retour : plus hors ligne", !n.offline());
    T.ok("second signal « disponible » : pas de nouveau rechargement", !n.available());
    n.lost(); n.lost();
    T.ok("pertes répétées : un seul rechargement au retour", n.available() && !n.available());
    T.eq("délai avant rechargement (DNS)", 1500L, NetWatch.SETTLE_MS);
  }
}
