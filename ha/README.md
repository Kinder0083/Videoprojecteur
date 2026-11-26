# Objets Home Assistant du launcher « Maison »

Tous créés via l'API de Home Assistant (pas de YAML à éditer).

| Objet | Rôle |
|---|---|
| Tableau de bord `projecteur-maison` (vues `accueil`, `cameras`) | Ce que la WebView du launcher affiche sur l'Accueil et l'onglet Caméras. Export : `projecteur-maison.dashboard.json`. Le dashboard `ma-maison` n'est **pas** modifié : les onglets Lumières / Volets / Climat l'ouvrent directement. |
| `input_button.projecteur_accueil_origine` | Pressé par le launcher (menu Réglages → « Revenir à l'accueil d'origine »). |
| Automatisation « Projecteur – retour à l'accueil d'origine demandé » | Sur appui du bouton → `script.projecteur_accueil_origine`. Elle tourne sans utilisateur : marche même si le launcher est connecté avec le compte non administrateur « Projecteur ». Garde `not_from`/`not_to` `unavailable`/`unknown` : un redémarrage de HA ne déclenche rien (le tout premier appui d'un bouton neuf ne fait qu'initialiser son état). |
| `script.projecteur_accueil_origine` | **Filet de sécurité** : remet l'accueil Magcubic (`com.htc.magcubicos/.activity.MainActivity`) par défaut et l'ouvre, via ADB. À lancer à distance si le launcher pose problème. |
| `script.projecteur_accueil_maison` | Fait du launcher (`com.hy.home/.HomeActivity`) l'accueil par défaut et l'ouvre. |
| Automatisation « Projecteur – Calibration au démarrage » (modifiée) | Au retour du projecteur : luminosité préférée ; mise au point caméra seulement dans les 4 premières minutes ; puis **une fois par démarrage** (marque `debug.hy.calib`, propriété non persistante effacée à chaque redémarrage) trapèze et zoom du préréglage 0 via `hyscreen.sh load 0 sansfocus`. La netteté n'est plus imposée : la mise au point du fabricant au démarrage donne une image nette, alors qu'une position mémorisée glisse avec le compteur du moteur. Avant : garde « uptime < 300 s », sautée quand HA revoyait le projecteur 8 min après l'allumage. |
| `script.projecteur_launcher_configurer` | Envoie au launcher l'adresse HTTPS de HA (`--es ha_url "<adresse-https-de-HA>"`). L'adresse réelle n'existe que dans ce script, **jamais dans ce dépôt public**. |

Commandes ADB utilisées (Android 14, sans root, shell) :

```sh
cmd package set-home-activity <paquet>/<activité>
cmd role get-role-holders android.app.role.HOME
cmd role add-role-holder --user 0 android.app.role.HOME <paquet>   # si le rôle n'a pas suivi
```
