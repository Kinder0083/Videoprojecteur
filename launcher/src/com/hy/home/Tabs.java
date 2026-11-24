package com.hy.home;

/** Onglets de la barre du haut et chemins Home Assistant correspondants. */
public final class Tabs {
  public static final int HOME = 0;
  public static final int CAMERAS = 1;
  public static final int LIGHTS = 2;
  public static final int COVERS = 3;
  public static final int CLIMATE = 4;
  public static final int[] ORDER = {HOME, CAMERAS, LIGHTS, COVERS, CLIMATE};

  /** Page « Réglages du projecteur » (dashboard HA existant). */
  public static final String PROJECTOR_SETTINGS_PATH = "/projecteur-hy350";

  private static final String[] LABELS = {"Accueil", "Caméras", "Lumières", "Volets", "Climat"};
  private static final String[] PATHS = {
    "/projecteur-maison/accueil", "/projecteur-maison/cameras",
    "/ma-maison/lumieres", "/ma-maison/volets", "/ma-maison/climat"
  };

  private Tabs() {}

  public static String label(int tab) { return LABELS[tab]; }

  public static String path(int tab) { return PATHS[tab]; }
}
