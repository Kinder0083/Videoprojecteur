# Conception — Écran d'accueil « Maison » du vidéoprojecteur HY350 (approche A)

_Document de travail, version 2 — 24/11. Validé par le spike WebView et par la maquette :
https://claude.ai/artifact/S3wCwYBGscwuWZ2EqRNoT5 (version 4)._

## Intention / pourquoi
À l'allumage, le projecteur affiche **la maison d'abord** (voir son état et la piloter), puis donne accès aux applis vidéo.
C'est une **base de départ** : le contenu doit pouvoir évoluer vite, sans reconstruire l'app.

## Objectifs
- Au démarrage, le projecteur ouvre **notre écran d'accueil** à la place de l'accueil Magcubic.
- L'écran principal reprend le **dashboard HA « Maison » (`ma-maison`)** :
  - au centre, la carte **« Flux d'énergie »** (`custom:energy-flow-card`, même configuration que dans `ma-maison`) ;
  - **État de la maison** (lumières allumées, volets ouverts, mode, fenêtres) ;
  - **Raccourcis** : Chauffage, Clim, Arrêt, Ouvrir volets, Fermer volets, Tout éteindre (avec confirmation) ;
  - **Séjour** (`climate.sejour`) avec la consigne.
- **Onglets** : Accueil · **Caméras** · Lumières · Volets · Climat.
  - **Caméras** : nouvel onglet, **pas sur l'écran principal**. Caméras : `camera.ouest`, `camera.salon`, `camera.sud`.
  - Lumières, Volets, Climat : les pages correspondantes du dashboard `ma-maison`.
- **Applis en bas** : Netflix, Prime Video, Disney+, **USB** (lecteur de la clé/disque USB), « + Ajouter », puis **Réglages**.
  Rien d'autre de l'accueil Magcubic n'est utilisé (pas d'entrée HDMI ni de recopie d'écran).
- **Retour à l'accueil d'origine** : **dans le menu Réglages** (avec confirmation), pas dans la barre d'applis.
- **Autologin** : aucun mot de passe à taper au démarrage (une seule connexion, à l'installation).
- Contenu **modifiable côté HA** (éditer le dashboard suffit pour ajouter, retirer ou déplacer un bloc).

## Non-objectifs
- Remplacer les applis vidéo (on les lance seulement).
- Refaire les cartes HA : on réutilise celles du dashboard « Maison ».
- Toute dépendance cloud payante obligatoire.

## Anti-objectifs (échec même si « ça marche »)
- Un écran d'accueil qui, s'il plante, rend le projecteur inutilisable sans PC.
- Un mot de passe à taper à la télécommande.
- Un bouton « accueil d'origine » qu'on presse par erreur.
- Un écran principal surchargé : il doit rester **aéré** (les caméras vont dans leur onglet).

## Contraintes (vérifiées)
- Android 14 (Allwinner H713), **non rooté**, accès shell (uid 2000) via ADB depuis HA.
  L'accueil par défaut se change sans root (`cmd package set-home-activity`), réversible à distance par ADB/HA.
- **WebView 113** : l'interface HA 2026.11 s'y affiche correctement (spike).
- **Adresse HA** : `https://<domaine-duckdns-de-HA>:8123` — adresse réelle non publiée (dépôt public) — (certificat Let's Encrypt valide, liaison temps réel OK).
  Ne pas utiliser l'IP `192.168.1.135` (certificat invalide pour l'IP, HTTP refusé).
- **RAM limitée** : la WebView a été tuée une fois par le gestionnaire mémoire puis relancée → une seule WebView, rechargement automatique.
- Écran 1920×1080 ; la maquette est pensée en 1280×720 « CSS » (la carte énergie agrandie ×1,5).
- Applis présentes : `com.netflix.mediaclient`, `com.amazon.amazonvideo.livingroom`, `com.disney.disneyplus` ; lecteur USB : paquet à identifier sur le projecteur.
- Signature : même clé que les apps `com.hy.osd` / `com.hy.web` (mises à jour par `pm install -r`).

## Approches considérées
- **A (retenue)** : notre propre écran d'accueil remplace celui de Magcubic. Seule option qui met la maison d'abord au démarrage ; risque couvert par le filet ADB.
- B : app « Maison » par-dessus l'accueil Magcubic. Rejetée : démarrage automatique incertain sans root, moins intégré.
- C : tableau de bord ouvert à la demande. Rejetée : pas « maison au démarrage ».

## Conception (décisions)
- App Android signée **`com.hy.home`**, déclarée comme **écran d'accueil** (HOME).
- **Partie native** (dans l'app) : barre du haut (salutation, date, onglets, heure), **barre d'applis en bas**, **menu Réglages**, et le cadre de sélection à la télécommande.
- **Partie Home Assistant** (WebView, au milieu) : un **tableau de bord dédié « Projecteur maison »** créé dans HA, qui **réutilise les cartes du dashboard Maison** (Flux d'énergie, État, Raccourcis, Séjour) et une vue Caméras (Ouest, Salon, Sud). Les onglets Lumières / Volets / Climat ouvrent les vues du dashboard `ma-maison`.
  → ajouter un bloc = modifier ce tableau de bord dans HA, sans toucher l'app.
- **Menu Réglages** : Recharger l'écran Maison · Réglages du projecteur (netteté, trapèze, luminosité) · Paramètres Android · **Revenir à l'accueil d'origine** (avec confirmation).
- **Connexion** : utilisateur HA dédié **« Projecteur », non administrateur**, créé par toi. Connexion **une seule fois** dans la WebView (page de connexion HA, « Rester connecté » coché) ; la session est ensuite conservée par la WebView (les mises à jour `pm install -r` la gardent).
  Personne d'autre que toi ne voit le mot de passe ; aucun jeton dans l'app, les logs ou un dépôt.
  `trusted_networks` écarté : test du 24/11 à 23:29 → HA voit les requêtes DuckDNS venir de la box (`192.168.1.254`), pas du projecteur.
- **Filet de sécurité** : l'accueil Magcubic reste installé ; commande ADB documentée pour le rétablir à distance ; option du menu Réglages sur place.
- **Style** : sombre (projection), arrondi, aéré, convivial ; le thème HA du tableau dédié reprend les couleurs de la maquette.

## Décisions prises après relecture (mes choix, à vérifier par toi)
- **Dashboard `ma-maison` intact** : on ne le modifie pas (il reste identique sur le téléphone).
  L'en-tête HA est masqué **uniquement dans la WebView du projecteur** (CSS injecté par l'app).
  Les onglets Lumières / Volets / Climat ouvrent **directement** les vues de `ma-maison` → toujours à jour.
  Seuls l'Accueil et les Caméras vivent dans le tableau dédié « Projecteur maison » (copie des 4 cartes de l'accueil).
- **« Revenir à l'accueil d'origine »** = retour **durable** : ouvre l'accueil Magcubic tout de suite **et** demande à HA de le remettre par défaut (script ADB).
  Si HA est injoignable : ouvre au moins l'accueil Magcubic, et l'écran Android « applis par défaut » s'il existe sur ce firmware. Rétablissement à distance par ADB toujours possible.
- **Caméras** : images rafraîchies toutes les ~2 s dans l'onglet (léger pour la mémoire) ; un clic ouvre **une** caméra en direct plein écran.

## Laissé au constructeur
- Masquage de l'en-tête HA dans la WebView (thème, paramètre d'URL ou injection CSS) pour que seuls nos onglets natifs apparaissent.
- Navigation exacte à la télécommande entre la WebView et la barre d'applis.
- Écran affiché pendant le chargement ou si HA est injoignable.
- Le contenu de « + Ajouter » (liste des applis installées).

## Critères de validation
- Au démarrage : notre écran s'affiche, connecté à HA **sans mot de passe**.
- Carte Flux d'énergie, État, Raccourcis et Séjour affichent les **vraies valeurs** et les raccourcis agissent (avec confirmation).
- L'onglet Caméras montre Ouest, Salon, Sud en direct ; les onglets Lumières / Volets / Climat ouvrent les pages du dashboard Maison.
- Netflix, Prime, Disney+ se lancent, et la touche Accueil ramène à notre écran.
- **Retour à l'accueil d'origine testé** : depuis le menu Réglages **et** à distance par ADB.
