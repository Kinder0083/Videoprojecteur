package com.hy.home;

public final class IdlePolicyTest {
  private IdlePolicyTest() {}

  public static void run() {
    long t0 = 1_000_000L;
    T.eq("délai de veille", 5L * 60 * 1000, IdlePolicy.TIMEOUT_MS);
    T.ok("actif juste après une touche", !IdlePolicy.asleep(t0, t0 + 1000, IdlePolicy.TIMEOUT_MS));
    T.ok("actif juste avant le délai", !IdlePolicy.asleep(t0, t0 + IdlePolicy.TIMEOUT_MS - 1, IdlePolicy.TIMEOUT_MS));
    T.ok("en veille au délai", IdlePolicy.asleep(t0, t0 + IdlePolicy.TIMEOUT_MS, IdlePolicy.TIMEOUT_MS));
    T.ok("horloge qui recule : reste actif", !IdlePolicy.asleep(t0, t0 - 5000, IdlePolicy.TIMEOUT_MS));
  }
}
