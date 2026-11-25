package com.hy.home;

/** Mise en veille de l'écran Maison : sans touche pendant TIMEOUT_MS, la page HA est figée pour limiter la chauffe. */
public final class IdlePolicy {
  public static final long TIMEOUT_MS = 5L * 60 * 1000;

  private IdlePolicy() {}

  public static boolean asleep(long lastInputMs, long nowMs, long timeoutMs) {
    long idle = nowMs - lastInputMs;
    return idle >= timeoutMs;
  }
}
