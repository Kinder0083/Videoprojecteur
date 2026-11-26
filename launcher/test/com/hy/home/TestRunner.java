package com.hy.home;

import java.lang.reflect.Method;

/** Lance XxxTest.run() pour chaque classe de test présente ; code 1 si un test échoue. */
public final class TestRunner {
  private static final String[] TESTS = {
    "HomeConfigTest", "GreetingTest", "StatusParserTest", "DockModelTest", "KioskScriptTest", "IdlePolicyTest", "NetWatchTest", "BootPolicyTest"
  };

  private TestRunner() {}

  public static void main(String[] args) throws Exception {
    int ran = 0;
    for (String n : TESTS) {
      Class<?> c;
      try { c = Class.forName("com.hy.home." + n); } catch (ClassNotFoundException e) { continue; }
      Method m = c.getMethod("run");
      m.invoke(null);
      ran++;
    }
    System.out.println((T.failures() == 0 ? "OK" : "ECHEC") + " — " + ran + " classes, "
        + T.passes() + " réussis, " + T.failures() + " échecs");
    System.exit(T.failures() == 0 ? 0 : 1);
  }
}
