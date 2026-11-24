package com.hy.home;

/** Imprime le script injecté, pour « node --check » dans tools/run-tests.sh. */
public final class KioskScriptDump {
  private KioskScriptDump() {}

  public static void main(String[] args) { System.out.print(KioskScript.build("HyBridge", 1.25)); }
}
