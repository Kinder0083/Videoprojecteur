package com.hy.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Lancement des applis et accès à l'accueil d'origine (côté Android, non testable sur la JVM). */
public final class AppLauncher {
  static final ComponentName ORIGIN_HOME =
      new ComponentName("com.htc.magcubicos", "com.htc.magcubicos.activity.MainActivity");
  static final ComponentName HOME_SETTINGS =
      new ComponentName("com.android.permissioncontroller", "com.android.permissioncontroller.role.ui.HomeSettingsActivity");

  private AppLauncher() {}

  /** Intent de lancement TV (Leanback) en priorité, sinon classique ; null si absente ou désactivée. */
  static Intent launchIntent(Context c, String pkg) {
    PackageManager pm = c.getPackageManager();
    Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
    if (i == null) i = pm.getLaunchIntentForPackage(pkg);
    return i;
  }

  public static boolean launch(Context c, String pkg) {
    Intent i = launchIntent(c, pkg);
    if (i == null) { Log.w(HomeActivity.TAG, "appli introuvable : " + pkg); return false; }
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    try { c.startActivity(i); return true; }
    catch (RuntimeException e) { Log.w(HomeActivity.TAG, "lancement impossible : " + pkg); return false; }
  }

  /** Paquets ayant une entrée de lancement (TV ou classique). */
  public static List<String> installedLaunchables(Context c) {
    PackageManager pm = c.getPackageManager();
    Set<String> seen = new HashSet<String>();
    List<String> r = new ArrayList<String>();
    String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
    for (String cat : cats) {
      Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
      for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
        String p = ri.activityInfo.packageName;
        if (seen.add(p)) r.add(p);
      }
    }
    return r;
  }

  static Set<String> launchableSet(Context c) { return new HashSet<String>(installedLaunchables(c)); }

  static String label(Context c, String pkg) {
    String l = DockModel.label(pkg);
    if (l != null) return l;
    try {
      PackageManager pm = c.getPackageManager();
      return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
    } catch (PackageManager.NameNotFoundException e) { return pkg; }
  }

  /** Bannière TV (16:9) si l'appli en a une. */
  static Drawable banner(Context c, String pkg) {
    try { return c.getPackageManager().getApplicationBanner(pkg); }
    catch (PackageManager.NameNotFoundException e) { return null; }
  }

  static Drawable icon(Context c, String pkg) {
    try { return c.getPackageManager().getApplicationIcon(pkg); }
    catch (PackageManager.NameNotFoundException e) { return null; }
  }

  /** Ouvre l'accueil Magcubic (ne change pas l'accueil par défaut). */
  public static boolean openOriginHome(Context c) {
    Intent i = new Intent(Intent.ACTION_MAIN).setComponent(ORIGIN_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try { c.startActivity(i); return true; }
    catch (RuntimeException e) { Log.w(HomeActivity.TAG, "accueil d'origine introuvable"); return false; }
  }

  /** Écran Android « Appli d'accueil » pour choisir Magcubic à la main. */
  public static void openHomeSettings(Context c) {
    Intent i = new Intent().setComponent(HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try { c.startActivity(i); return; } catch (RuntimeException e) { Log.w(HomeActivity.TAG, "HomeSettings indisponible"); }
    try { c.startActivity(new Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
    catch (RuntimeException e) { Log.w(HomeActivity.TAG, "réglage d'accueil indisponible"); }
  }

  public static void openAndroidSettings(Context c) {
    try { c.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
    catch (RuntimeException e) { Log.w(HomeActivity.TAG, "paramètres indisponibles"); }
  }
}
