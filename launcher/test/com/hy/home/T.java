package com.hy.home;

import java.util.Objects;

/** Mini-banc de test sans dépendance (pas de JUnit). */
public final class T {
  private static int passes = 0;
  private static int fails = 0;

  private T() {}

  public static void eq(String name, Object expected, Object actual) {
    if (Objects.equals(expected, actual)) { passes++; }
    else { fails++; System.out.println("FAIL " + name + " : attendu <" + expected + "> obtenu <" + actual + ">"); }
  }

  public static void ok(String name, boolean cond) {
    if (cond) { passes++; } else { fails++; System.out.println("FAIL " + name); }
  }

  public static int failures() { return fails; }

  public static int passes() { return passes; }
}
