package com.hy.home;

import android.content.DialogInterface;
import android.view.View;
import java.util.List;

/** Écouteurs nommés de la barre d'applis et du menu (d8 refuse les classes anonymes). */
final class UiActions {
  private UiActions() {}

  static final int APP = 0, ADD = 1, SETTINGS = 2;

  /** Appui OK sur une tuile. */
  static final class TileClick implements View.OnClickListener {
    final HomeActivity a; final int kind; final String pkg;
    TileClick(HomeActivity a, int kind, String pkg) { this.a = a; this.kind = kind; this.pkg = pkg; }
    @Override public void onClick(View v) {
      if (kind == APP) a.launchApp(pkg);
      else if (kind == ADD) a.showAddDialog();
      else a.showSettingsMenu();
    }
  }

  /** Appui long sur une tuile épinglée : proposer de la retirer. */
  static final class TileLongClick implements View.OnLongClickListener {
    final HomeActivity a; final String pkg;
    TileLongClick(HomeActivity a, String pkg) { this.a = a; this.pkg = pkg; }
    @Override public boolean onLongClick(View v) {
      if (!DockModel.removable(pkg)) return false;
      a.confirmRemove(pkg);
      return true;
    }
  }

  static final class AddPick implements DialogInterface.OnClickListener {
    final HomeActivity a; final List<String> pkgs;
    AddPick(HomeActivity a, List<String> pkgs) { this.a = a; this.pkgs = pkgs; }
    @Override public void onClick(DialogInterface d, int which) { a.pin(pkgs.get(which)); }
  }

  static final class RemoveConfirm implements DialogInterface.OnClickListener {
    final HomeActivity a; final String pkg;
    RemoveConfirm(HomeActivity a, String pkg) { this.a = a; this.pkg = pkg; }
    @Override public void onClick(DialogInterface d, int which) { a.unpin(pkg); }
  }

  static final class MenuPick implements DialogInterface.OnClickListener {
    final HomeActivity a;
    MenuPick(HomeActivity a) { this.a = a; }
    @Override public void onClick(DialogInterface d, int which) { a.onMenu(which); }
  }

  static final class OriginConfirm implements DialogInterface.OnClickListener {
    final HomeActivity a;
    OriginConfirm(HomeActivity a) { this.a = a; }
    @Override public void onClick(DialogInterface d, int which) { a.goOrigin(); }
  }

  /** HA n'a pas répondu à temps à l'appui du bouton : on passe au repli local. */
  static final class OriginTimeout implements Runnable {
    final HomeActivity a;
    OriginTimeout(HomeActivity a) { this.a = a; }
    @Override public void run() { a.originFallback("pas de réponse de HA"); }
  }
}
