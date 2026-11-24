# Launcher « Maison » HY350 — plan de construction

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** remplacer l'accueil Magcubic du vidéoprojecteur par un écran « Maison » (Home Assistant d'abord, applis vidéo ensuite), réversible à tout moment.

**Architecture :** une app Android `com.hy.home` déclarée HOME : cadre natif (barre du haut, onglets, barre d'applis, menu Réglages) autour d'une WebView qui affiche un tableau de bord HA dédié (`projecteur-maison`) et les vues du dashboard `ma-maison`. Un script JS injecté masque l'en-tête HA, passe en sombre et parle au natif par un pont `HyBridge`. Côté HA : le tableau dédié, un bouton + une automatisation + des scripts ADB pour changer d'accueil.

**Tech Stack :** Java 8 (javac `--release 8`), Android SDK build-tools r34 (d8, aapt2, zipalign, apksigner), android.jar API 34, WebView 113 du projecteur, Home Assistant 2026.11 (outils MCP `ha_*`), shell ADB via `androidtv.adb_command`.

**Spec :** `docs/superpowers/specs/2026-11-24-launcher-maison-design.md` (version complète non publiée, avec l'adresse HA réelle : copie de travail hors dépôt). Maquette approuvée : https://claude.ai/artifact/S3wCwYBGscwuWZ2EqRNoT5 (version 4, privée).

## Global Constraints

- Java : `javac --release 8` ; **aucune classe anonyme, aucune lambda ni référence de méthode** (le d8 utilisé plante dessus) → classes nommées (`static final class …`) ou `implements` sur l'activité.
- Classes « pures » (`HomeConfig`, `Tabs`, `Greeting`, `StatusParser`, `DockModel`, `KioskScript`) : **aucun import `android.*`** (testées sur la JVM).
- App : paquet `com.hy.home`, activité `.HomeActivity`, `versionCode 1`, `versionName "1.0"`, `minSdkVersion 26`, `targetSdkVersion 28`, `usesCleartextTraffic="false"`, permission `INTERNET` (pas de `QUERY_ALL_PACKAGES` : inutile en `targetSdk` 28).
- Signature : keystore **hors dépôt** (variables `KEYSTORE`, `KS_PASS`, `KS_ALIAS`) ; `*.jks`, `*.apk`, `build/` dans `.gitignore`.
- **Dépôt public** : jamais l'adresse HA réelle, de mot de passe ni de jeton dans le dépôt, les logs ou les commits. L'adresse HA arrive à l'exécution par l'extra `ha_url` (https obligatoire), envoyé par un script HA.
- Préférences Android : fichier `hy_home`, clés `ha_url`, `pins`. Tag de log : `hy-home`.
- Chemins HA : Accueil `/projecteur-maison/accueil` · Caméras `/projecteur-maison/cameras` · Lumières `/ma-maison/lumieres` · Volets `/ma-maison/volets` · Climat `/ma-maison/climat` · Réglages du projecteur `/projecteur-hy350`.
- **Ne jamais modifier le dashboard `ma-maison`.**
- Accueil d'origine : `com.htc.magcubicos/.activity.MainActivity` ; écran de choix d'accueil Android : `com.android.permissioncontroller/.role.ui.HomeSettingsActivity`.
- Barre d'applis (ordre) : Netflix `com.netflix.mediaclient` · Prime Video `com.amazon.amazonvideo.livingroom` · Disney+ `com.disney.disneyplus` · USB `com.hisilicon.explorer` · applis épinglées · « + Ajouter » · « Réglages ». Lancement : intent `LEANBACK_LAUNCHER` d'abord, sinon `LAUNCHER`.
- Couleurs de la maquette : fond `#0a0e1d`, texte `#f2f4ff`, secondaire `#a3abcf`, cartes `#151c35`, sélection `#7cf3ff`. Textes en français.
- Déploiement : via `androidtv.adb_command` sur `media_player.android_tv_192_168_1_35` ; APK gzip + base64 en morceaux de 740 caractères (`tools/apk-chunks.sh`), relus depuis les fichiers (jamais retapés de mémoire), empreinte SHA-256 vérifiée **avant** décodage puis après. Lire les sorties longues via un fichier sur l'appareil (`/data/local/tmp/*.txt`), car la sonde minute de HA écrase `adb_response`.
- Caméras : `camera_view: auto` = rafraîchissement natif de HA (~10 s), moins fréquent que les ~2 s du spec ; aller plus vite demanderait une carte sur mesure → **écart assumé, signalé à l'utilisateur**. Le direct reste disponible au clic.
- Écritures HA (`ha_config_set_*`) : relire la clé du guide des bonnes pratiques (`ha_get_skill_guide`) si elle a expiré.

## Review Focus

1. **HA injoignable au démarrage** (box ou HA redémarre) → écran d'erreur « Connexion à la maison… » avec nouvel essai automatique, et la barre d'applis reste utilisable. Testé en Task 9, Step 6.
2. **WebView tuée par manque de mémoire** → recréée automatiquement, sur le même onglet. Testé en Task 7, Step 7 (extra `crash_renderer`).
3. **Session HA expirée** → la page de connexion HA s'affiche dans la WebView, utilisable à la télécommande, sans erreur du script injecté. Testé en Task 11, Step 1.
4. **Launcher planté ou désinstallé alors qu'il est l'accueil par défaut** → retour à Magcubic à distance par le script HA. Testé en Task 11, Step 5.
5. **Appli de la barre désinstallée** (ex. Disney+) → sa tuile disparaît, sans plantage. Testé en Task 5 (`DockModel.visible`) et Task 9, Step 5.

---

### Task 1: Tableau de bord HA « Projecteur maison »

**Files :**
- Create: `ha/projecteur-maison.dashboard.json` (export de la config créée)

**Interfaces :**
- Produces : dashboard `url_path` `projecteur-maison`, vues `path` `accueil` et `cameras`.

- [ ] **Step 1 : Lire `ma-maison`** avec `ha_config_get_dashboard(url_path="ma-maison")` et repérer, dans `views[0]` : la carte `custom:energy-flow-card`, la carte `markdown` « État de la maison », les 6 cartes `shortcut` (Chauffage, Clim, Arrêt, Ouvrir volets, Fermer volets, Tout éteindre) ; dans `views[3]` : la `tile` `climate.sejour` (nommée « Séjour »).
- [ ] **Step 2 : Créer le tableau** avec `ha_config_set_dashboard(url_path="projecteur-maison", title="Projecteur maison", icon="mdi:projector-screen", show_in_sidebar=False, require_admin=False, config=…)` :
  - vue 1 : `title` « Accueil », `path` `accueil`, `type` `sections`, `max_columns` 2 ; section A = la carte énergie **copiée telle quelle** ; section B = markdown État + 6 raccourcis (`grid_options.columns` 4, **mêmes confirmations**) + tile Séjour (`features: target-temperature`). Pas de titres de section (écran aéré).
  - vue 2 : `title` « Caméras », `path` `cameras`, `type` `sections`, `max_columns` 2 ; `picture-entity` `camera.ouest` (columns 12), `camera.salon` (6), `camera.sud` (6) ; `camera_view: auto`, `show_state: false`, `tap_action: more-info` (le direct s'ouvre au clic).
- [ ] **Step 3 : Vérifier** `ha_config_get_dashboard(url_path="projecteur-maison")` : 2 vues `accueil`, `cameras` ; 1 `custom:energy-flow-card`, 1 `markdown`, 6 `shortcut`, 1 `tile` `climate.sejour`, 3 `picture-entity`. Puis `ha_config_get_dashboard(url_path="ma-maison")` : `config_hash` inchangé (`0ca2a29669e4017c` ou la valeur relue au Step 1).
- [ ] **Step 4 : Exporter** la config dans `ha/projecteur-maison.dashboard.json` (JSON indenté ; contient seulement des entity_id) et committer :

```bash
git add ha/projecteur-maison.dashboard.json
git commit -m "HA : tableau de bord projecteur-maison (accueil + caméras)"
```

### Task 2: HA — changement d'accueil (bouton, automatisation, scripts)

**Files :**
- Create: `ha/README.md` (rôle de chaque objet HA ; adresse HA notée `<adresse-https-de-HA>`)

**Interfaces :**
- Produces : `input_button.projecteur_accueil_origine` (pressé par l'app) ; `script.projecteur_accueil_origine` ; `script.projecteur_accueil_maison` ; `script.projecteur_launcher_configurer` (contient l'adresse réelle — **uniquement dans HA**) ; automatisation « Projecteur – retour à l'accueil d'origine demandé ».

- [ ] **Step 1 : Créer** `input_button.projecteur_accueil_origine` (nom « Projecteur – revenir à l'accueil d'origine », icône `mdi:home-export-outline`).
- [ ] **Step 2 : Créer les scripts** (`mode: single`, action `androidtv.adb_command` sur `media_player.android_tv_192_168_1_35`) :
  - `projecteur_accueil_origine` : `cmd package set-home-activity com.htc.magcubicos/.activity.MainActivity; cmd role get-role-holders android.app.role.HOME | grep -q magcubicos || cmd role add-role-holder --user 0 android.app.role.HOME com.htc.magcubicos; am start -n com.htc.magcubicos/.activity.MainActivity`
  - `projecteur_accueil_maison` : même forme avec `com.hy.home/.HomeActivity` (paquet `com.hy.home`).
  - `projecteur_launcher_configurer` : `am start -n com.hy.home/.HomeActivity --es ha_url "<adresse https réelle de HA>"` (adresse lue dans la copie de travail du spec).
- [ ] **Step 3 : Créer l'automatisation** : déclencheur `state` sur `input_button.projecteur_accueil_origine` (n'importe quel nouvel état, en excluant `unavailable`/`unknown` dans `from_state`), action `script.projecteur_accueil_origine`, `mode: single`. L'app passe par ce bouton parce que l'utilisateur « Projecteur » n'est pas administrateur ; l'automatisation tourne sans utilisateur.
- [ ] **Step 4 : Tester** sans risque : Magcubic est déjà l'accueil. Lancer `script.projecteur_accueil_origine`, puis lire `cmd role get-role-holders android.app.role.HOME` → attendu `com.htc.magcubicos`. Presser `input_button.projecteur_accueil_origine` → la trace de l'automatisation montre le script lancé.
- [ ] **Step 5 : Écrire `ha/README.md`** et committer :

```bash
git add ha/README.md
git commit -m "HA : bouton, automatisation et scripts de changement d'accueil"
```

### Task 3: Banc de test JVM + navigation (`HomeConfig`, `Tabs`)

**Files :**
- Create: `tools/run-tests.sh`, `launcher/test/com/hy/home/T.java`, `launcher/test/com/hy/home/TestRunner.java`
- Create: `launcher/src/com/hy/home/Tabs.java`, `launcher/src/com/hy/home/HomeConfig.java`
- Test: `launcher/test/com/hy/home/HomeConfigTest.java`
- Create: `.gitignore` (`*.jks`, `*.apk`, `build/`)

**Interfaces :**
- Produces : `T.eq(String name, Object expected, Object actual)`, `T.ok(String name, boolean cond)`, `T.failures()` ; `TestRunner.main` appelle `XxxTest.run()` de chaque classe de test et sort avec le code 1 s'il y a des échecs.
- Produces : `Tabs` : `int HOME=0, CAMERAS=1, LIGHTS=2, COVERS=3, CLIMATE=4` ; `int[] ORDER` ; `String label(int)` ; `String path(int)` ; `String PROJECTOR_SETTINGS_PATH = "/projecteur-hy350"`.
- Produces : `HomeConfig.normalizeBaseUrl(String raw) -> String` (null si invalide) ; `HomeConfig.viewUrl(String base, int tab) -> String` ; `HomeConfig.isInside(String base, String url) -> boolean`.

- [ ] **Step 1 : Écrire le test**

```java
// HomeConfigTest.run()
T.eq("trailing slash", "https://ha.example.org:8123", HomeConfig.normalizeBaseUrl("https://ha.example.org:8123/"));
T.eq("trim", "https://ha.example.org:8123", HomeConfig.normalizeBaseUrl("  https://ha.example.org:8123  "));
T.eq("http refusé", null, HomeConfig.normalizeBaseUrl("http://ha.example.org:8123"));
T.eq("vide", null, HomeConfig.normalizeBaseUrl(""));
T.eq("null", null, HomeConfig.normalizeBaseUrl(null));
T.eq("espace", null, HomeConfig.normalizeBaseUrl("https://bad host"));
T.eq("accueil", "/projecteur-maison/accueil", Tabs.path(Tabs.HOME));
T.eq("caméras", "/projecteur-maison/cameras", Tabs.path(Tabs.CAMERAS));
T.eq("lumières", "/ma-maison/lumieres", Tabs.path(Tabs.LIGHTS));
T.eq("volets", "/ma-maison/volets", Tabs.path(Tabs.COVERS));
T.eq("climat", "/ma-maison/climat", Tabs.path(Tabs.CLIMATE));
T.eq("labels", "Accueil,Caméras,Lumières,Volets,Climat", Tabs.label(0)+","+Tabs.label(1)+","+Tabs.label(2)+","+Tabs.label(3)+","+Tabs.label(4));
T.eq("viewUrl", "https://ha.example.org:8123/projecteur-maison/cameras", HomeConfig.viewUrl("https://ha.example.org:8123", Tabs.CAMERAS));
T.ok("inside auth", HomeConfig.isInside("https://ha.example.org:8123", "https://ha.example.org:8123/auth/authorize?x=1"));
T.ok("dehors", !HomeConfig.isInside("https://ha.example.org:8123", "https://evil.org/"));
T.ok("suffixe piège", !HomeConfig.isInside("https://ha.example.org:8123", "https://ha.example.org:8123.evil.org/"));
```

- [ ] **Step 2 : Écrire `tools/run-tests.sh`** : compile `launcher/src/com/hy/home/{HomeConfig,Tabs,Greeting,StatusParser,DockModel,KioskScript}.java` (ceux qui existent) + `launcher/test/**/*.java` avec `javac --release 8 -encoding UTF-8 -d build/test`, puis `java -cp build/test com.hy.home.TestRunner`. Si `KioskScriptDump` existe : `java … KioskScriptDump > build/kiosk.js && node --check build/kiosk.js`.
- [ ] **Step 3 : Lancer** `tools/run-tests.sh` → attendu : échec de compilation (classes absentes).
- [ ] **Step 4 : Implémenter** `Tabs` et `HomeConfig` (`normalizeBaseUrl` : trim, schéma `https://` obligatoire, pas d'espace, slash final retiré ; `isInside` : comparaison sur `base + "/"` ou égalité stricte).
- [ ] **Step 5 : Lancer** `tools/run-tests.sh` → attendu : `OK` et code de sortie 0.
- [ ] **Step 6 : Commit**

```bash
git add .gitignore tools/run-tests.sh launcher/
git commit -m "Launcher : banc de test JVM et navigation (onglets, adresse HA)"
```

### Task 4: Barre du haut — salutation, date, heure, météo (`Greeting`, `StatusParser`)

**Files :**
- Create: `launcher/src/com/hy/home/Greeting.java`, `launcher/src/com/hy/home/StatusParser.java`
- Test: `launcher/test/com/hy/home/GreetingTest.java`, `launcher/test/com/hy/home/StatusParserTest.java`

**Interfaces :**
- Produces : `Greeting.hello(java.time.LocalTime) -> String`, `Greeting.date(java.time.LocalDate) -> String`, `Greeting.clock(java.time.LocalTime) -> String`.
- Produces : `StatusParser.chip(String payload) -> String`, `StatusParser.connected(String payload) -> boolean`. Payload envoyé par le JS : `"<1|0>|<condition>|<température>"`.

- [ ] **Step 1 : Écrire les tests**

```java
T.eq("après-midi", "Bonjour 👋", Greeting.hello(LocalTime.of(14,20)));
T.eq("5h", "Bonjour 👋", Greeting.hello(LocalTime.of(5,0)));
T.eq("17h59", "Bonjour 👋", Greeting.hello(LocalTime.of(17,59)));
T.eq("18h", "Bonsoir 👋", Greeting.hello(LocalTime.of(18,0)));
T.eq("4h59", "Bonsoir 👋", Greeting.hello(LocalTime.of(4,59)));
T.eq("date", "jeudi 1 janvier", Greeting.date(LocalDate.of(2026,1,1)));
T.eq("heure", "09:05", Greeting.clock(LocalTime.of(9,5)));

T.eq("pluie", "🌧️ 10,8 °C · Pluie", StatusParser.chip("1|rainy|10.8"));
T.eq("soleil", "☀️ 24,5 °C · Ensoleillé", StatusParser.chip("1|sunny|24.5"));
T.eq("nuit entier", "🌙 6 °C · Nuit claire", StatusParser.chip("1|clear-night|6"));
T.eq("temp indispo", "☁️ Nuageux", StatusParser.chip("1|cloudy|unavailable"));
T.eq("condition inconnue", "🌡️ 12 °C", StatusParser.chip("1|bizarre|12"));
T.eq("déconnecté", "", StatusParser.chip("0||"));
T.eq("null", "", StatusParser.chip(null));
T.eq("charabia", "", StatusParser.chip("garbage"));
T.ok("connecté", StatusParser.connected("1|rainy|10.8"));
T.ok("pas connecté", !StatusParser.connected("0||"));
```

- [ ] **Step 2 : Lancer** `tools/run-tests.sh` → attendu : échec (classes absentes).
- [ ] **Step 3 : Implémenter.** `hello` : « Bonjour 👋 » de 05:00 à 17:59, sinon « Bonsoir 👋 ». `date` : `DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)`. `clock` : `HH:mm`. `chip` : `<emoji> <temp> °C · <libellé>` ; température à une décimale avec virgule, `,0` supprimé ; sans température : `<emoji> <libellé>` ; condition inconnue : `🌡️ <temp> °C`. Correspondances : sunny ☀️ Ensoleillé · clear-night 🌙 Nuit claire · partlycloudy ⛅ Éclaircies · cloudy ☁️ Nuageux · rainy 🌧️ Pluie · pouring 🌧️ Averses · lightning et lightning-rainy ⛈️ Orage · snowy 🌨️ Neige · snowy-rainy 🌨️ Pluie et neige · hail 🌨️ Grêle · fog 🌫️ Brouillard · windy et windy-variant 💨 Venteux · exceptional ⚠️ Exceptionnel.
- [ ] **Step 4 : Lancer** `tools/run-tests.sh` → attendu : `OK`.
- [ ] **Step 5 : Commit** `git commit -m "Launcher : salutation, date, heure et pastille météo"`

### Task 5: Barre d'applis — modèle (`DockModel`)

**Files :**
- Create: `launcher/src/com/hy/home/DockModel.java`
- Test: `launcher/test/com/hy/home/DockModelTest.java`

**Interfaces :**
- Produces : `DockModel.DEFAULTS` (`String[]` des 4 paquets dans l'ordre des contraintes) ; `DockModel.label(String pkg) -> String` (null hors défauts) ; `packages(String pinsCsv) -> List<String>` ; `addPin(String csv, String pkg) -> String` ; `removePin(String csv, String pkg) -> String` ; `visible(List<String> pkgs, java.util.Set<String> installed) -> List<String>`.

- [ ] **Step 1 : Écrire le test**

```java
List<String> d = Arrays.asList("com.netflix.mediaclient","com.amazon.amazonvideo.livingroom","com.disney.disneyplus","com.hisilicon.explorer");
T.eq("défauts", d, DockModel.packages(""));
T.eq("épinglée", concat(d, "org.videolan.vlc"), DockModel.packages("org.videolan.vlc"));
T.eq("doublon", concat(d, "org.videolan.vlc"), DockModel.packages("com.netflix.mediaclient,org.videolan.vlc"));
T.eq("ajout", "org.videolan.vlc", DockModel.addPin("", "org.videolan.vlc"));
T.eq("ajout idempotent", "org.videolan.vlc", DockModel.addPin("org.videolan.vlc", "org.videolan.vlc"));
T.eq("défaut non épinglé", "a.b", DockModel.addPin("a.b", "com.netflix.mediaclient"));
T.eq("retrait", "c.d", DockModel.removePin("a.b,c.d", "a.b"));
T.eq("appli désinstallée masquée", Arrays.asList("com.netflix.mediaclient","com.hisilicon.explorer"),
     DockModel.visible(d, new HashSet<>(Arrays.asList("com.netflix.mediaclient","com.hisilicon.explorer"))));
T.eq("libellés", "Netflix|Prime Video|Disney+|USB", DockModel.label(d.get(0))+"|"+DockModel.label(d.get(1))+"|"+DockModel.label(d.get(2))+"|"+DockModel.label(d.get(3)));
T.eq("libellé inconnu", null, DockModel.label("org.videolan.vlc"));
```
(`concat` = petite aide locale au test.)

- [ ] **Step 2 : Lancer** `tools/run-tests.sh` → attendu : échec.
- [ ] **Step 3 : Implémenter** `DockModel` (CSV sans espaces, ordre d'épinglage conservé, défauts jamais dans le CSV).
- [ ] **Step 4 : Lancer** `tools/run-tests.sh` → attendu : `OK`.
- [ ] **Step 5 : Commit** `git commit -m "Launcher : modèle de la barre d'applis"`

### Task 6: Script injecté dans la WebView (`KioskScript`)

**Files :**
- Create: `launcher/src/com/hy/home/KioskScript.java`
- Test: `launcher/test/com/hy/home/KioskScriptTest.java`, `launcher/test/com/hy/home/KioskScriptDump.java` (imprime `build("HyBridge", 1.25)`)

**Interfaces :**
- Produces : `KioskScript.build(String bridgeName, double zoom) -> String` (JS auto-exécuté, idempotent).
- Produces, côté page : `window.__hy.nav(path)` (navigation HA sans rechargement : `history.pushState` + événement `location-changed`) ; `window.__hy.press(entityId)` (`hass.callService("input_button","press",{entity_id})`, puis `HyBridge.onPress(entityId, ok)`) ; sondage toutes les 30 s → `HyBridge.onStatus("<1|0>|<état de weather.ideaux4>|<état de sensor.ideaux4_temperature>")`.
- Consumes : objet natif `HyBridge` (Task 7) : `onStatus(String)`, `onPress(String, boolean)`, `log(String)`.

- [ ] **Step 1 : Écrire le test**

```java
String js = KioskScript.build("HyBridge", 1.25);
T.ok("garde", js.startsWith("(function(){if(window.__hyKiosk)return;window.__hyKiosk=1;"));
for (String s : new String[]{"dockedSidebar","\"always_hidden\"","selectedTheme","\"dark\":true","hui-root",
     "location-changed","HyBridge.onStatus(","HyBridge.onPress(","input_button","weather.ideaux4",
     "sensor.ideaux4_temperature","--card-background-color","#151c35","zoom","1.25","window.__hy={"})
  T.ok("contient " + s, js.contains(s));
```

- [ ] **Step 2 : Lancer** `tools/run-tests.sh` → attendu : échec.
- [ ] **Step 3 : Implémenter `build`.** Le JS : (a) si `localStorage.dockedSidebar` ≠ `"always_hidden"` ou si `selectedTheme` n'est pas sombre, les écrire puis `location.reload()` une fois ; (b) masquer l'en-tête de vue en parcourant les shadow roots `home-assistant` → `home-assistant-main` → `ha-panel-lovelace` → `hui-root` → `.header` (réessai toutes les 500 ms, 40 fois au plus, et de nouveau à chaque `location-changed`), puis `HyBridge.log("kiosk:ok")` ; (c) poser `--primary-background-color:#0a0e1d` et `--card-background-color:#151c35` sur `document.documentElement` et `zoom` sur `<html>`, réappliqués à chaque sondage ; (d) n'agir sur l'en-tête que si `hui-root` existe : rien ne doit casser sur la page de connexion.
- [ ] **Step 4 : Lancer** `tools/run-tests.sh` → attendu : `OK` **et** `node --check build/kiosk.js` sans erreur.
- [ ] **Step 5 : Commit** `git commit -m "Launcher : script injecté (mode kiosque, sombre, pont natif)"`

### Task 7: App Android — WebView, configuration, robustesse

**Files :**
- Create: `launcher/AndroidManifest.xml`, `launcher/src/com/hy/home/HomeActivity.java`, `launcher/src/com/hy/home/HaBridge.java`, `launcher/src/com/hy/home/WebClient.java`
- Create: `tools/build-apk.sh` (args : dossier de l'app, APK de sortie ; variables `SDK_BT`, `ANDROID_JAR`, `KEYSTORE`, `KS_PASS`, `KS_ALIAS`) — pipeline : `javac --release 8` → `d8 --min-api 26` → `aapt2 link` → zip `classes.dex` → `zipalign -p 4` → `apksigner sign` → `apksigner verify`
- Create: `tools/apk-chunks.sh` (args : APK ; produit `<apk>.b64` = gzip -9 + base64 -w0, morceaux `chunk_00…` de 740 caractères, et affiche les SHA-256 de l'APK et du `.b64`)

**Interfaces :**
- Consumes : `HomeConfig`, `Tabs`, `KioskScript`.
- Produces : `HomeActivity.showTab(int tab)`, `HomeActivity.navigate(String path)`, `HomeActivity.reloadHome()` ; extras d'intent `ha_url` (enregistré dans les préférences après `normalizeBaseUrl`) et `crash_renderer` (test : charge `chrome://crash`) ; `HaBridge` (`@JavascriptInterface` : `onStatus`, `onPress`, `log`) relaie vers l'activité.

- [ ] **Step 1 : Manifeste** : activité `.HomeActivity`, `launchMode="singleTask"`, `stateNotNeeded="true"`, filtres `MAIN`+`HOME`+`DEFAULT` et `MAIN`+`LAUNCHER`, thème `@android:style/Theme.NoTitleBar.Fullscreen`, `configChanges="orientation|screenSize|keyboardHidden|keyboard|navigation"`.
- [ ] **Step 2 : `HomeActivity`** : fond `#0a0e1d` ; zone centrale = **une seule** WebView (JS, DOM storage, fond transparent, `addJavascriptInterface(bridge, "HyBridge")`) ; sans adresse enregistrée, un message plein écran : « Adresse de Home Assistant non configurée — lance le script HA “Projecteur – configurer le launcher” ». Calque de chargement / d'erreur « Connexion à la maison… » avec nouvel essai automatique toutes les 15 s. `onNewIntent` (touche Accueil) → onglet Accueil. Retour : `goBack` dans HA si possible, sinon rien (un accueil ne se ferme pas).
- [ ] **Step 3 : `WebClient`** (classe nommée) : `shouldOverrideUrlLoading` garde dans la WebView ce qui est `isInside`, bloque le reste ; `onPageFinished` → `evaluateJavascript(KioskScript.build("HyBridge", 1.25))` ; `onReceivedError` (cadre principal) → calque d'erreur ; `onRenderProcessGone` → détruire puis recréer la WebView sur l'onglet courant, journaliser `renderer gone, recreated`, retourner `true`.
- [ ] **Step 4 : Construire** : `tools/build-apk.sh launcher build/hyhome.apk` → attendu : `apksigner verify` OK.
- [ ] **Step 5 : Installer** : `tools/apk-chunks.sh build/hyhome.apk` ; envoyer les morceaux (lots de 4 à 5) dans `/data/local/tmp/hyhome.b64` ; vérifier le SHA-256 du `.b64`, décoder, vérifier celui de l'APK, puis `pm install -r /data/local/tmp/hyhome.apk` → attendu `Success`. L'accueil par défaut n'est **pas** changé dans cette tâche.
- [ ] **Step 6 : Vérifier** : lancer `script.projecteur_launcher_configurer` ; attendu dans `logcat -s hy-home` : URL enregistrée, page de connexion HA affichée, aucune erreur JS.
- [ ] **Step 6b : Actions de l'utilisateur** : (1) créer dans HA l'utilisateur **« Projecteur »** (Paramètres → Personnes → Ajouter, connexion autorisée, **non administrateur**) ; (2) sur le projecteur, s'y connecter en cochant « Rester connecté ». Attendu : Accueil affiché, `kiosk:ok` dans le logcat. La session survit aux `pm install -r` suivants (même signature).
- [ ] **Step 7 : Test mémoire** : `am start -n com.hy.home/.HomeActivity --ez crash_renderer true` → attendu dans le logcat : `renderer gone, recreated`, puis la page rechargée sur l'onglet courant.
- [ ] **Step 8 : Commit** `git commit -m "Launcher : app Android (WebView HA, configuration, reprise après crash)"`

### Task 8: App Android — barre du haut et onglets

**Files :**
- Modify: `launcher/src/com/hy/home/HomeActivity.java`

**Interfaces :**
- Consumes : `Greeting`, `StatusParser`, `Tabs`, `window.__hy.nav`.

- [ ] **Step 1 : Barre du haut** (style de la maquette) : salutation + date à gauche ; onglets Accueil · Caméras · Lumières · Volets · Climat (focusables, l'onglet actif en pastille claire) ; pastille météo ; pastille « Maison connectée » (point vert) ou « Hors ligne » (point rouge) ; heure. Rafraîchissement toutes les 30 s par `Handler` + `Runnable` nommé.
- [ ] **Step 2 : Onglets** : un clic appelle `__hy.nav(Tabs.path(tab))` si la page HA est chargée, sinon `loadUrl(viewUrl(...))`.
- [ ] **Step 3 : Pont** : `onStatus` → `StatusParser.chip/connected` sur le fil UI.
- [ ] **Step 4 : Construire, installer, vérifier** (même procédure que Task 7) : capture `screencap -p` → base64 → contrôle visuel (barre conforme à la maquette ; Caméras affiche Ouest / Salon / Sud, un clic ouvre le direct ; Lumières ouvre la vue `ma-maison/lumieres` sans en-tête HA ; le raccourci « Chauffage » ouvre la confirmation HA → **Annuler**).
- [ ] **Step 5 : Commit** `git commit -m "Launcher : barre du haut, onglets et pastilles d'état"`

### Task 9: App Android — barre d'applis et « + Ajouter »

**Files :**
- Create: `launcher/src/com/hy/home/AppLauncher.java`
- Modify: `launcher/src/com/hy/home/HomeActivity.java`

**Interfaces :**
- Consumes : `DockModel`.
- Produces : `AppLauncher.launch(Context, String pkg) -> boolean` (`getLeanbackLaunchIntentForPackage`, sinon `getLaunchIntentForPackage`) ; `AppLauncher.installedLaunchables(Context) -> List<String>`.

- [ ] **Step 1 : Barre d'applis centrée en bas** : tuiles = bannière TV de l'appli (ou son icône) + libellé `DockModel.label`, sinon libellé Android ; seulement les paquets `visible` ; puis « + Ajouter » et « Réglages ». Le cadre `#7cf3ff` suit la sélection ; gauche/droite dans la barre ; haut → WebView.
- [ ] **Step 2 : « + Ajouter »** : grille des applis lançables non présentes ; OK → `addPin` (enregistré dans `pins`). Appui long sur une tuile épinglée → confirmation « Retirer <appli> de la barre ? » → `removePin`. Les 4 tuiles par défaut ne se retirent pas.
- [ ] **Step 3 : Construire et installer** (procédure de Task 7).
- [ ] **Step 4 : Vérifier les lancements** : Netflix, Prime Video, Disney+, USB s'ouvrent ; logcat `ActivityTaskManager: START` vers le bon paquet.
- [ ] **Step 5 : Appli absente** : `pm disable-user --user 0 com.disney.disneyplus` → la tuile Disney+ disparaît au retour sur le launcher, sans plantage ; puis `pm enable com.disney.disneyplus` → elle revient.
- [ ] **Step 6 : HA injoignable** : `am start -n com.hy.home/.HomeActivity --es ha_url https://192.0.2.1:8123` → calque « Connexion à la maison… », et Netflix se lance encore depuis la barre ; puis relancer `script.projecteur_launcher_configurer` → retour à la normale.
- [ ] **Step 7 : Commit** `git commit -m "Launcher : barre d'applis, épinglage et raccourci USB"`

### Task 10: App Android — menu Réglages

**Files :**
- Modify: `launcher/src/com/hy/home/HomeActivity.java`, `launcher/src/com/hy/home/AppLauncher.java`

**Interfaces :**
- Consumes : `window.__hy.press`, `HaBridge.onPress`, `Tabs.PROJECTOR_SETTINGS_PATH`.
- Produces : `AppLauncher.openOriginHome(Context)` (démarre `com.htc.magcubicos/.activity.MainActivity`) ; `AppLauncher.openHomeSettings(Context)` (`HomeSettingsActivity`).

- [ ] **Step 1 : Menu** (panneau au-dessus de la barre, comme la maquette) : « Recharger l'écran Maison » → `reloadHome()` ; « Réglages du projecteur (netteté, trapèze, luminosité) » → `navigate("/projecteur-hy350")` ; « Paramètres Android » → `Settings.ACTION_SETTINGS` ; « Revenir à l'accueil d'origine » (texte rose) → confirmation « Revenir à l'accueil d'origine du projecteur ? Le launcher Maison reste installé et peut être rétabli. ». Retour ferme le menu.
- [ ] **Step 2 : Retour à l'origine** : si la pastille est « connectée » → `__hy.press("input_button.projecteur_accueil_origine")` ; sur `onPress(ok=true)` → `openOriginHome`. Si HA est déconnecté ou `ok=false` → `openOriginHome` puis `openHomeSettings` (l'utilisateur choisit Magcubic).
- [ ] **Step 3 : Construire, installer, vérifier** que chaque entrée fait ce qui est écrit (le retour à l'origine est testé « pour de vrai » en Task 11).
- [ ] **Step 4 : Commit** `git commit -m "Launcher : menu Réglages et retour à l'accueil d'origine"`

### Task 11: Mise en service

**Files :**
- Create: `README.md` (installation, mise à jour, restauration à distance ; adresse HA en repère)

- [ ] **Step 1 : Session expirée** : depuis HA, supprimer le jeton d'actualisation de l'utilisateur Projecteur → au rechargement, la page de connexion s'affiche, sans erreur JS dans le logcat ; se reconnecter.
- [ ] **Step 2 : Passer en accueil par défaut** : `script.projecteur_accueil_maison` → `cmd role get-role-holders android.app.role.HOME` = `com.hy.home`.
- [ ] **Step 3 : Touche Accueil** : lancer Netflix, appuyer sur Accueil → retour au launcher, onglet Accueil.
- [ ] **Step 4 : Redémarrage du projecteur** → le launcher s'affiche, connecté **sans mot de passe** ; l'automatisation de calibrage au démarrage fonctionne toujours (trapèze, zoom, netteté).
- [ ] **Step 5 : Retours à l'origine** : (a) Réglages → « Revenir à l'accueil d'origine » → titulaire = `com.htc.magcubicos` ; puis `script.projecteur_accueil_maison`. (b) À distance : `script.projecteur_accueil_origine` → Magcubic ; puis de nouveau `script.projecteur_accueil_maison`.
- [ ] **Step 6 : Ménage** : désinstaller l'app du test `com.hy.web` (`pm uninstall com.hy.web`).
- [ ] **Step 7 : `README.md`** puis commit et push :

```bash
git add README.md
git commit -m "Launcher Maison : mise en service et procédure de restauration"
git push -u origin ccr-b5bf406e-tej1rh
```
