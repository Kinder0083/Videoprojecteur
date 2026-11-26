package com.hy.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/** Fin du démarrage : si Maison est l'accueil choisi, il repasse devant l'accueil Magcubic imposé par le firmware. */
public final class BootReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c, Intent intent) {
    if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
    Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
    ResolveInfo ri = c.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
    String pkg = ri != null && ri.activityInfo != null ? ri.activityInfo.packageName : null;
    if (!BootPolicy.takeOver(pkg)) { Log.i(HomeActivity.TAG, "démarrage : accueil par défaut = " + pkg + ", on ne s'impose pas"); return; }
    PendingResult pr = goAsync();
    new Handler(Looper.getMainLooper()).postDelayed(new Start(c.getApplicationContext(), pr), BootPolicy.DELAY_MS);
  }

  static final class Start implements Runnable {
    final Context c; final PendingResult pr;
    Start(Context c, PendingResult pr) { this.c = c; this.pr = pr; }
    @Override public void run() {
      try {
        c.startActivity(new Intent(c, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Log.i(HomeActivity.TAG, "démarrage : Maison repasse devant");
      } catch (RuntimeException e) {
        Log.w(HomeActivity.TAG, "démarrage : lancement refusé : " + e);
      } finally { pr.finish(); }
    }
  }
}
