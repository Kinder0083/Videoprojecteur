# SDD ledger — plan: /home/user/Videoprojecteur/docs/superpowers/plans/2026-11-24-launcher-maison.md
Spec: docs/superpowers/specs/2026-11-24-launcher-maison-design.md (copie complète hors dépôt : scratchpad/conception-launcher-maison.md)
Pre-flight:
- T1→T3 : chemins de vues (accueil, cameras) produits par T1 = Tabs.path de T3 → cohérent.
- T2→T10 : input_button.projecteur_accueil_origine produit par T2 = pressé par T10 → cohérent.
- T3→T7/T8 : HomeConfig.normalizeBaseUrl/viewUrl/isInside, Tabs.* → cohérent.
- T4→T8 : Greeting.hello/date/clock, StatusParser.chip/connected ; payload "<1|0>|cond|temp" produit par T6 → cohérent.
- T5→T9 : DockModel.packages/addPin/removePin/visible/label → cohérent.
- T6↔T7 : KioskScript.build("HyBridge",1.25) ; JS appelle HyBridge.onStatus/onPress/log que T7 expose → cohérent.
Task 1: complete (commits 89146a9..064156e, tests: python3 -I -c 'import json,collections;d=json.load(open('ha/projecteur-maison.dashboard.json'));c=collections.Counter(x['type'] for v in d['views'] for s in v['sections'] for x in s['cards']);assert c=={'custom:energy-flow-card':1,'markdown':1,'shortcut':6,'tile':1,'picture-entity':3},c;print('dashboard export OK',dict(c))' → dashboard export OK {'custom:energy-flow-card': 1, 'markdown': 1, 'shortcut': 6, 'tile': 1, 'picture-entity': 3})
Task 2: Ruling: HA a créé input_button.projecteur_revenir_a_l_accueil_d_origine (id dérivé du nom) — renommé en input_button.projecteur_accueil_origine (id du plan, consommé par T6/T10 ; objet neuf, aucun consommateur) — coût si faux : nul.
Task 2: Ruling: garde not_from/not_to unavailable/unknown conservée ; le 1er appui d'un bouton neuf (état unknown) ne déclenche pas — bouton initialisé par un appui de test, état restauré aux redémarrages — coût si faux : un 1er appui « perdu » après recréation du bouton, l'app retombe alors sur openOriginHome (Task 10).
Task 2: complete (commits 064156e..2a6c1a6, tests: sh -c 'test -f ha/README.md && ! grep -rqi 'deauxmo' ha/ docs/ && echo 'HA docs OK, aucune adresse réelle'' → HA docs OK, aucune adresse réelle)
Task 3: complete (commits 2a6c1a6..182732d, tests: tools/run-tests.sh → OK — 1 classes, 18 réussis, 0 échecs)
Task 4: complete (commits 182732d..59db1f0, tests: tools/run-tests.sh → OK — 3 classes, 35 réussis, 0 échecs)
Task 5: complete (commits 59db1f0..a038b04, tests: tools/run-tests.sh → OK — 4 classes, 48 réussis, 0 échecs)
Task 6: complete (commits a038b04..074e9f6, tests: tools/run-tests.sh → kiosk.js : syntaxe JS OK)
Task 7: device checks — APK sha bef1a6d33ac779a9 installé (Success) ; configurer → « adresse HA enregistrée », statut 0|| (page de connexion), pas d'erreur JS ; crash_renderer → « renderer gone, recreated », même PID.
Task 7: Ruling: étape 6b (création du compte HA « Projecteur » + connexion « Rester connecté ») reportée avant la tâche 11 — action utilisateur avec mot de passe, les tâches 8–10 n'en dépendent pas — coût si faux : les vérifs visuelles 8–10 se font sur la page de connexion, revérifiées en tâche 11.
Task 7: complete (commits 074e9f6..5474dd3, tests: tools/run-tests.sh → kiosk.js : syntaxe JS OK)
Task 7: Ruling: étape 6b faite par l'utilisateur (compte non admin « Projecteur », connexion OK) — levée de la réserve précédente.
Task 8: Ruling: retour utilisateur « tout doit tenir à l'écran, pas d'ascenseur » → zoom fixe 1,25 remplacé par un zoom ajusté à la hauteur (borné 0,6–1,25, barre de défilement masquée, réajusté à chaque navigation) — couvert par KioskScriptTest — coût si faux : texte un peu plus petit sur les vues longues ; en dessous de 0,6 la vue défile (barre masquée).
Task 8: device checks — APK 272eff0e165167ba installé ; journal « fit z=0.98 q=1.00 », « kiosk:ok », statut 1|rainy|10.8 (pastilles connecté + météo). Vérif visuelle des onglets demandée à l'utilisateur.
Task 8: Ruling: touche Accueil → launcher d'origine tant que la tâche 11 n'a pas fait de com.hy.home l'accueil par défaut — volontaire : sans dock (tâche 9) l'utilisateur n'aurait plus accès à Netflix — coût si faux : aucun.
Task 8: note: le zoom calculé avant le masquage de l'en-tête n'est pas regrossi après « kiosk:ok » (0,98 au lieu d'un peu plus) — à corriger avec la tâche 9 (refit après kiosk:ok).
Task 8: complete (commits 5474dd3..605ef4e, tests: tools/run-tests.sh → kiosk.js : syntaxe JS OK)
Task 9: Ruling: tâches 9 et 10 livrées dans un même APK — chaque transfert coûte ~45 appels ADB — coût si faux : revue moins granulaire.
Task 9: Ruling: « + Ajouter » et menu Réglages en boîtes de dialogue Material sombres (liste) au lieu d'une grille/panneau sur mesure — fonctionnel d'abord — coût si faux : moins proche de la maquette, à reprendre en finition.
Task 9: device checks (APK 0c3eb254…) — barre affichée : Netflix, Prime Video, Disney+, USB, Ajouter, Réglages ; « Maison connectée » ; fit z=0.72 (la barre prend de la hauteur). Défaut trouvé : aucun élément n'a le focus (projecteur en mode tactile) → la télécommande ne sélectionne rien. Correctif : requestFocusFromTouch sur la 1re tuile au retour + touche directionnelle sans focus → barre (APK fde9ee73…, non encore installé : projecteur « unavailable » dans HA depuis 00:33).
Task 9: device checks (APK fde9ee73…, commit 2c3242c) — focus sur Netflix au démarrage, flèche droite → Prime Video (dumpsys : .F sur la 2e tuile ; uiautomator ne rapporte pas le focus sur cet appareil), gauche + OK → com.netflix.mediaclient au premier plan. Disney+ désactivée puis réactivée : pas de plantage (AndroidRuntime 0), launcher au premier plan, Disney+ réactivée et vérifiée.
Task 9: Ruling: comptage des tuiles avec Disney+ désactivée non relevé (réponse ADB perdue) — couvert par DockModelTest.visible + queryIntentActivities qui exclut les applis désactivées ; pas de nouvel essai pour ne pas risquer de laisser Disney+ désactivée — coût si faux : tuile qui ne lance rien (launchApp reconstruit alors la barre).
Task 9: Ruling: test « HA injoignable » reporté à la tâche 11 — évite de casser la configuration pendant les essais de l'utilisateur — coût si faux : comportement hors-ligne vérifié plus tard.
Task 9: incident: une commande chaînée a expiré et a laissé Disney+ désactivée quelques secondes → réactivée immédiatement ; désormais commandes courtes.
INCIDENT 2026-11-25 (surchauffe 80,4 °C à 18:19, image floue) — enquête (systematic-debugging, phase 1) :
- 17:29 démarrage ; luminosité (bl_duty) revenue à 50 au démarrage (valeur usine) au lieu de 20 ; calibration au démarrage : autofocus caméra puis préréglage → moteur de netteté 392→403.
- 17:30–17:35 écran Magcubic + transfert ADB : CPU 41→50 °C. 17:36 launcher au premier plan : CPU 61→67, puis 68–75 °C stable, ventilateur à 100 % (2520 tr/min) → le launcher (WebView HA animée en continu) ajoute ~+15–20 °C.
- 18:13:49–51 script.projecteur_luminosite lancé 3× (carte HA) → luminosité 90→95→100 ; photos 18:13:35/53, 18:14:32. 18:19 CPU 80,4 °C → extinction.
- Moteur de netteté : 403/404 depuis le 23/11, identique avant/après → le flou ne vient PAS d'un déplacement du moteur ; le préréglage réapplique 403 = valeur déjà en place (d'où « sans effet »). Hypothèse : dérive optique à chaud (à confirmer à froid).
- Aucun code du launcher ne touche moteur/luminosité ; les tests ADB non plus.
- Défaut réel trouvé : HomeActivity.onPause ne met pas la WebView en pause (ni web.onPause ni pauseTimers) → le JS continue quand Netflix est devant.
INCIDENT suite : flou → netteté recalée à 368 par l'utilisateur (préréglage 0 = 403 obsolète). Projecteur non déplacé → dérive du compteur de pas. Pilote : 256+n / 512+n dans motor_ctrl, limites logicielles (motor_limit=1), motor_ctrl_no_limit réservé à root → prise d'origine possible seulement si la butée est atteinte avant le zéro du compteur : test à faire avec l'utilisateur présent.
Point 2 (HA) : input_number.projecteur_luminosite_preferee (20–100, 20) ; script.projecteur_luminosite la mémorise ; calibration au démarrage la réapplique ; sécurité thermique >75 °C 2 min ou >79 °C → bl_duty 20 + alerte.
Point 3 (launcher, commit 3b06600, APK dbfb895c installé) : WebView figée hors écran ; veille après 5 min sans touche (IdlePolicy). Mesure de chauffe à faire.

## Chauffe du launcher — cause trouvée et corrigée (25/11 soir)
- Mesure page d'origine : ~196 images/5 s, renderer WebView 95 %, com.hy.home 70,6 %, surfaceflinger 21 % → rendu continu ~40 i/s.
- Expérience contrôlée : carte custom:energy-flow-card retirée de projecteur-maison → 0 image/5 s, renderer 2 %. Carte restaurée (hash 756af4ec1837f939).
- Cause : la carte anime une scène SVG complète en CSS (nuages, soleil, flux, pluie, ombres, cellules…) en boucle infinie.
- Ruling: correction côté launcher (KioskScript, CALM_MS=1000) : les animations à itérations infinies sont mises en pause et avancées de 1 s par pas ; transitions et animations finies de HA intactes — la carte et ma-maison ne sont pas modifiées — coût si faux : mouvement saccadé de la carte (1 i/s).
- document.getAnimations() ne voit pas les shadow roots (vérifié dans Chromium) → collecte des shadow roots, rafraîchie tous les 10 pas.
- Mesure après : 7 images/5 s, renderer 10,6 %, com.hy.home 3,6 %, surfaceflinger 1,6 %, log « calm n=9 racines=170 ».
- Limite : jusqu'à 10 s d'animation pleine vitesse après chargement/navigation (avant le rescan des racines).
- Mesure via HA : le lancement en arrière-plan (setsid nohup) est tué à la fin du shell adb → script court mes.sh lancé en avant-plan (<8 s), résultat lu ensuite dans adb_response.
- Contrôle à +5 min (20:41) : CPU 54,6 °C, GPU 50,0 °C, luminosité 20, launcher au premier plan, ~1,3 i/s. Automatisation temporaire projecteur_test_chauffe_launcher_temporaire supprimée.
- Demande utilisateur : animation plus fluide → CALM_MS=500 (2 pas/s). Test KioskScriptTest « pas d'animation 0,5 s » RED→GREEN, suite 91/91. APK 11c128057b90e16b installé 21:26.

## Essai « carte redessinée » (piste A) — 26/11
- Mesures app d'essai (WebView, carte réelle, données fictives) : orig 47 i/s ~208 % ; orig2 (photons+nuages) 50 i/s ~124 % ; lite2 (mêmes, composités) 67 i/s ~120 % ; static ~0.
- À 12 i/s : orig2@12 ~41 %, lite2@12 ~39 %, orig@12 (carte complète) ~75 %. Coût dominé par la production/composition d'images plein écran, pas par le dessin.
- Ruling: piste A abandonnée (gain ~5 % à fluidité égale) — on garde la carte et on règle CALM_MS=83 (12 pas/s) choisi par l'utilisateur — coût si faux : chauffe ~75 % de charge, à surveiller (repli prévu 8 pas/s si >62 °C).
- Recherche des racines ciblée : seules les racines animées suivies, recherche complète /10 s, chaque seconde les 10 premières s et après navigation. Tests 95/95, vérifié dans Chromium.
- Incident : relance du launcher (am start) à 09:33 pendant que l'utilisateur regardait Netflix → film probablement interrompu. Règle : vérifier topResumedActivity avant tout am start.
- Observation : Netflix seul ~105 % CPU et ~70 °C (avec dex2oat de l'installation).
- 26/11 : 12 i/s permanent mesuré sur le vrai launcher ~125 % (renderer 80 %), 61-62 °C ; gel ~1 s signalé par l'utilisateur ; Netflix moins fluide (dex2oat post-installation à 98 % + page HA gardée en arrière-plan ~180 Mo).
- Ruling (choix utilisateur) : rafale 12 i/s pendant 10 s (chargement, retour écran, réveil, touche, onglet) puis carte figée à mi-parcours (points visibles) ; page HA conservée en arrière-plan — coût si faux : pas de mouvement hors rafale.
- Ruling (mon choix) : recherche des animations limitée à energy-flow-card, plus de scan périodique de toute la page (cause probable du gel) ; PerformanceObserver longtask >150 ms journalisé — coût si faux : une autre carte animée tournerait à pleine vitesse.
- Mesure après (10:19) : 2 images/5 s, renderer 13,8 %, launcher 2,6 % ; longtasks uniquement au chargement de la page (164-343 ms), aucune ensuite ; scan < 30 ms.
- Rafale à durée fixe : 10 s à partir de la première touche ; les touches suivantes ne la prolongent pas tant qu'elle n'est pas finie (demande utilisateur). Une fois figée, la carte n'est vérifiée qu'une fois par seconde (1 tick sur 12) au lieu de 12×/s. APK 0412aa598e28d0de installé. Tests 104/104.
- Validé par l'utilisateur sur l'appareil : la carte se fige bien 10 s après la première touche.
- Ruling : les 4 applis d'origine (Netflix, Prime Video, Disney+, USB) deviennent retirables à la demande de l'utilisateur — préférence « hidden » (CSV) distincte des épinglées ; « + Ajouter » les repropose et les remet à leur place d'origine — coût si faux : aucun (préférence locale, réversible). APK 55d3e2293833e3d7 installé, tests 112/112.
- Validé sur l'appareil par l'utilisateur : retrait/remise des applis d'origine OK ; + Ajouter, appui long, Réglages OK.
- Ruling : calibration au démarrage (HA) — une fois par démarrage via debug.hy.calib au lieu de la garde uptime<300 s (sautée le 26/11 : HA a revu le projecteur 8 min après l'allumage) ; netteté non imposée (load 0 sansfocus) car l'utilisateur confirme une image nette avec la mise au point du fabricant (moteur 400 vs préréglage 355) — coût si faux : netteté à refaire à la main après un allumage ; retour arrière = remettre « load 0 » dans l'action.
- Netteté (26/11 soir, avec l'utilisateur) : montée 392→511 par pas de 15, arrêt du compteur à 511 sans bruit de butée → limite logicielle ; retour 392 net (aucun pas perdu). Redémarrage sans mise au point fabricant : image nette, compteur relu à 400 (comme ce matin) → le compteur est réinitialisé à 400 au démarrage, l'objectif ne bouge pas. Ruling : ne jamais bouger la netteté au démarrage ; mise au point caméra du fabricant désactivée dans la calibration ; préréglage « Appliquer » sans netteté — coût si faux : netteté à refaire à la main via la télécommande ; retour arrière = remettre les commandes d'origine (notes dans l'automatisation).
