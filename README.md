# Data Interview

Application Android conçue comme un outil de collecte de données pour le suivi passif des interactions smartphone-utilisateur dans le cadre d'une thèse en psychologie sociale de l'Université de Lausanne.

Elle enregistre les déverrouillages, l'ouverture et fermeture des applications, ainsi que les interactions avec les controles media, puis génère un fichier CSV chiffré par periode d'activation, envoyé automatiquement à l'équipe de recherche via Telegram.

> **Note :** Ce projet est en cours de développement. L'application n'est destinée à aucune utilisation en dehors de tests internes pour le moment.


---

## Table des matières

### destinée aux participant.e.s

- [Installation](#installation)
- [Autorisations requises](#autorisations-requises)
- [Utilisation](#utilisation)
- [Optimisation batterie par fabricant](#optimisation-batterie-par-fabricant)
- [Format des donnees collectees](#format-des-donnees-collectees)
- [Confidentialite et securite](#confidentialite-et-securite)

### Déstinée aux Chercheur.euse.s

- [Architecture technique](#architecture-technique)
- [Pipeline de collecte](#pipeline-de-collecte)
- [Base de donnees locale](#base-de-donnees-locale)
- [Generation du CSV](#generation-du-csv)
- [Chiffrement des donnees](#chiffrement-des-donnees)
- [Envoi via Telegram](#envoi-via-telegram)
- [Dechiffrement et analyse](#dechiffrement-et-analyse)
- [Ecrans de l'application](#ecrans-de-lapplication)
- [Compilation](#compilation)
- [Tests](#tests)

---

# Guide de participation

Tout ce qu'il faut savoir pour installer, configurer et utiliser l'application.

---

## Installation

### Prérequis

- Smartphone Android 5.0 (Lollipop) ou superieur
- Autoriser l'installation depuis des sources inconnues (l'application n'est pas sur le Play Store)

### Étapes

1. Télécharger le fichier APK depuis la section [Releases](../../releases) de ce dépôt
2. Ouvrir le fichier APK sur votre téléphone
3. Si demande, autoriser l'installation depuis cette source
4. Appuyer sur **Installer**
5. Ouvrir l'application **Data Interview**

---

## Autorisations requises

L'application nécessite plusieurs autorisations système pour fonctionner correctement. Sans ces autorisations, la collecte sera incomplète ou impossible. Un écran de configuration intégré guide l'utilisateur étape par étape.

| Autorisation | Pourquoi est-ce nécessaire | Comment l'accorder |
|---|---|---|
| **Accès aux statistiques d'utilisation** | Permet de détecter quelles applications sont ouvertes et fermées | Paramètres > Accès aux données d'utilisation > Data Interview |
| **Accès aux notifications** | Permet de détecter les contrôles media (lecture, pause, suivant, précèdent) sur l'écran de verrouillage | Paramètres > Accès aux notifications > Data Interview |
| **Paramètres restreints** (Android 13+ uniquement) | Nécessaire pour les applications installées hors Play Store afin de débloquer les autorisations ci-dessus | Paramètres > Applications > Data Interview > Autoriser les paramètres restreints |
| **Notifications** (Android 13+ uniquement) | Permet d'afficher la notification permanente indiquant que la collecte est en cours | Accepter la demande de l'application |
| **Optimisation de la batterie** | Empêche Android d'arrêter la collecte en arrière-plan pour économiser la batterie | Accepter la demande de désactivation |

### Comment faire

1. Ouvrir Data Interview
2. Appuyer sur le bouton **Autorisations**
3. L'écran affiche chaque autorisation avec son statut (accordée ou non)
4. Pour chaque autorisation non accordée, appuyer sur **Accorder** et suivre les instructions a l'écran
5. Revenir dans l'application — le statut se met à jour automatiquement

> **Important :** Si une autorisation semble déjà accordée mais ne fonctionne pas, essayez de la révoquer puis de la réaccorder.


---

## Utilisation

### Demarrer une collecte manuelle

1. Ouvrir Data Interview
2. Vérifier que toutes les autorisations sont accordées (indicateur vert sur l'écran **Autorisations**)
3. Appuyer sur **Démarrer**
4. Une notification permanente apparait : *"Data Interview — Collecte en cours"*
5. **Utiliser le téléphone normalement** — l'application fonctionne en arrière-plan de manière silencieuse

> La notification permanente est normale et nécessaire : elle empêche Android de fermer l'application. Ne la supprimez pas.

### Arreter la collecte

1. Ouvrir Data Interview
2. Appuyer sur **Arrêter**
3. L'application génère le fichier de données, le chiffre, et l'envoie automatiquement au chercheur via Telegram
4. L'écran principal indique *"Collecte arrêtée"*

### Programmer une collecte

Si vous souhaitez que la collecte démarre et s'arrête automatiquement a des horaires précis :

1. Appuyer sur **Programmer une activation**
2. Sélectionner la **date et heure de début**
3. Sélectionner la **date et heure de fin**
4. Appuyer sur **Confirmer la programmation**
5. La collecte démarrera et s'arrêtera automatiquement aux horaires choisis — aucune intervention nécessaire

### Consulter l'historique

1. Appuyer sur **Historique**
2. Chaque collecte passée affiche :
   - Dates de début et de fin
   - Durée totale
   - Nombre d'évènements enregistres
   - Statut d'envoi : **Envoyé**, **En attente** ou **Echec**
3. Appuyer sur une collecte pour visualiser le détail des donnees sous forme de tableau

### Apres un redémarrage du téléphone

L'application redémarre automatiquement la collecte si elle était en cours avant le redémarrage. **Aucune action n'est nécessaire.**

### En cas de problème

- **La collecte semble s'arrêter toute seule** : vérifiez les réglages d'optimisation de batterie (section suivante)
- **Le statut d'envoi affiche "Echec"** : vérifiez que le téléphone est connecté à Internet. Le fichier reste stocké localement et peut être renvoyé
- **Aucun évènements n'est enregistré** : vérifiez que toutes les autorisations sont accordées

---

## Optimisation batterie par fabricant

Certains fabricants arrêtent agressivement les applications en arrière-plan. **Cette étape est cruciale** pour garantir une collecte continue. Suivez les instructions spécifiques à votre téléphone :
### Samsung

1. Parametres > Entretien de l'appareil > Batterie
2. Appuyer sur Data Interview
3. Selectionner **Sans restriction**
4. Desactiver **Mettre en veille l'application**

### Xiaomi / Redmi / POCO

1. Parametres > Applications > Gerer les applications > Data Interview
2. Appuyer sur **Economie de batterie** > **Aucune restriction**
3. Parametres > Applications > Autostart > Activer pour Data Interview
4. Dans Securite > Autostart, activer Data Interview

### Huawei / Honor

1. Parametres > Applications > Lancement d'applications
2. Desactiver la gestion automatique pour Data Interview
3. Activer manuellement : Lancement auto, Lancement secondaire, Execution en arriere-plan

### OnePlus / Oppo / Realme

1. Parametres > Batterie > Optimisation de la batterie
2. Selectionner Data Interview > **Ne pas optimiser**

### Autres fabricants

Consultez [dontkillmyapp.com](https://dontkillmyapp.com) pour des instructions specifiques a votre modele.

---

## Format des donnees collectees

Chaque collecte produit un fichier CSV (valeurs separees par des points-virgules `;`).

### Ce qui est enregistre

| Donnee | Description |
|---|---|
| **Deverrouillages** | Chaque fois que vous deverrouillez votre telephone, l'heure est enregistree |
| **Applications** | Chaque application ouverte, avec l'heure d'ouverture et l'heure de fermeture |
| **Controles media** | Interactions avec les controles media (lecture, pause, suivant, precedent) sur l'ecran de verrouillage |

### Ce qui n'est PAS enregistre

- Le contenu des applications (messages, photos, pages web...)
- Les notifications recues
- La localisation GPS
- Les contacts ou l'historique d'appels
- Les mots de passe ou donnees de saisie

### Schema du CSV (5 colonnes)

| Colonne | Description | Exemple |
|---|---|---|
| `type_interaction` | Type d'evenement | `deverrouillage`, `application`, `widget` |
| `heure` | Heure de l'evenement (HH:MM) | `14:30` |
| `nom_app_widget` | Nom de l'application ou du widget | `Spotify`, `Chrome` |
| `heure_fermeture` | Heure de fermeture (HH:MM, applications uniquement) | `14:45` |
| `emplacement_widget` | Emplacement du widget | `ecran_verrouillage` |

### Types d'interaction

- **`deverrouillage`** — le telephone a ete deverrouille. Seule la colonne `heure` est renseignee.
- **`application`** — une application a ete ouverte. `nom_app_widget` contient le nom, `heure_fermeture` l'heure de fermeture.
- **`widget`** — interaction avec les controles media (lecture, pause, suivant, precedent). `nom_app_widget` contient le nom de l'application et l'action, `emplacement_widget` indique `ecran_verrouillage`.

### Exemple de donnees

```csv
type_interaction;heure;nom_app_widget;heure_fermeture;emplacement_widget
deverrouillage;08:30;;;
application;08:30;Chrome;08:45;
application;08:45;Spotify;09:12;
widget;08:50;Spotify (lecture);;ecran_verrouillage
widget;08:55;Spotify (suivant);;ecran_verrouillage
deverrouillage;10:15;;;
application;10:15;Instagram;10:32;
```

---

## Confidentialite et securite

- **Chiffrement** : les donnees sont chiffrees sur votre telephone avant tout envoi. Le chercheur est le seul a pouvoir les dechiffrer avec sa cle privee.
- **Pas de donnees personnelles sensibles** : l'application ne lit pas vos messages, photos, contacts ou mots de passe. Seuls les noms d'applications et les horaires sont enregistres.
- **Stockage local** : les donnees sont stockees dans l'espace prive de l'application, inaccessible aux autres applications.
- **Transmission securisee** : le fichier chiffre est transmis via Telegram. Meme en cas d'interception, les donnees restent illisibles sans la cle de dechiffrement.
- **Desinstallation** : supprimer l'application supprime toutes les donnees locales.

---

# Guide chercheur

Documentation technique detaillee de l'architecture, du fonctionnement interne et des outils d'analyse.

---

## Architecture technique

### Vue d'ensemble des composants

```
DataInterviewApp                  Classe Application — cree le canal de notification au lancement
  |
  +-- DataInterviewService        Foreground service — orchestre les 3 capteurs ci-dessous
  |     +-- UnlockReceiver        BroadcastReceiver ecoute ACTION_USER_PRESENT / SCREEN_ON / SCREEN_OFF
  |     +-- AppUsageTracker       Interroge UsageStatsManager toutes les 5 secondes via Handler
  |     +-- MediaTracker          Ecoute les callbacks MediaController via MediaSessionManager
  |
  +-- ActivationManager           Demarre/arrete le service, genere le CSV, lance le chiffrement et l'envoi
  |     +-- AlarmReceiver         Recoit les alarmes exactes pour les activations programmees
  |
  +-- CsvGenerator                Transforme les Event en fichier CSV brut
  +-- CsvEncryptor                Chiffrement hybride RSA-2048 + AES-256-GCM
  +-- TelegramUploader            Envoi du fichier .enc via HTTP POST multipart (OkHttp)
  |
  +-- AppDatabase (Room)          Base SQLite locale
  |     +-- Event                 Table des evenements (un row par interaction)
  |     +-- Activation            Table des periodes de collecte
  |
  +-- BootReceiver                Relance le service apres un redemarrage du telephone
  +-- MediaNotificationListener   Service stub requis par MediaSessionManager.getActiveSessions()
```

### Technologies

| Composant | Technologie |
|---|---|
| Langage | Kotlin 2.0 |
| Base de donnees | Room 2.6.1 (SQLite) |
| Reseau | OkHttp 4.12 |
| JSON | Gson 2.10.1 |
| Coroutines | kotlinx-coroutines 1.8 |
| UI | Material Components 1.12 |
| Build | Gradle 8.7, AGP 8.5, JDK 17 |
| SDK | compileSdk 35, minSdk 21, targetSdk 35 |

### Compatibilite

- **minSdk 21** (Android 5.0 Lollipop) — couvre ~99% des appareils actifs
- Code conditionnel (`Build.VERSION.SDK_INT`) pour les API 23, 26, 29, 31, 33 et 34
- `ContextCompat` et `NotificationCompat` pour la retrocompatibilite

---

## Pipeline de collecte

### Flux de bout en bout

```
Interaction utilisateur (deverrouillage / ouverture d'app / controle media)
        |
        +--- UnlockReceiver --------+
        |    (BroadcastReceiver)    |
        |                           |
        +--- AppUsageTracker -------+--- Event (entite Room) ---> Base SQLite
        |    (polling 5s)           |
        |                           |
        +--- MediaTracker ----------+
             (callbacks)
                                              |
                             Arret de la collecte (manuel ou programme)
                                              |
                                              v
                                    ActivationManager.stopActivation()
                                              |
                                 +------------+------------+
                                 |                         |
                            Arret du service        Requete des Event
                            (stopForeground)        par activationId
                                                          |
                                                          v
                                                    CsvGenerator.generate()
                                                    -> fichier .csv brut
                                                          |
                                                          v
                                                    CsvEncryptor.encrypt()
                                                    -> fichier .enc chiffre
                                                    -> suppression du .csv brut
                                                          |
                                                          v
                                                    TelegramUploader.upload()
                                                    -> POST multipart vers API Telegram
                                                          |
                                                          v
                                                    Mise a jour de l'Activation
                                                    (uploadStatus = success/failed)
```

### Capteur 1 : UnlockReceiver

- **Type** : `BroadcastReceiver` enregistre dynamiquement par le service
- **Evenements ecoutes** : `ACTION_USER_PRESENT` (deverrouillage), `ACTION_SCREEN_ON`, `ACTION_SCREEN_OFF`
- **Donnee produite** : un `Event` de type `deverrouillage` avec l'heure au format HH:MM
- **Insertion** : via coroutine dans la base Room

### Capteur 2 : AppUsageTracker

- **Mecanisme** : `Handler` sur le main looper, execute un `Runnable` toutes les 5 secondes
- **Source de donnees** : `UsageStatsManager.queryEvents()` avec une fenetre de 10 secondes en arriere
- **Detection** :
  - API 29+ : evenements `ACTIVITY_RESUMED` (ouverture) et `ACTIVITY_PAUSED` (fermeture)
  - API < 29 : evenements `MOVE_TO_FOREGROUND` et `MOVE_TO_BACKGROUND`
- **Resolution du nom** : le nom du package est converti en nom lisible via `PackageManager.getApplicationLabel()`. En cas d'echec, le nom du package est conserve tel quel.
- **Donnee produite** : un `Event` de type `application` avec `nom_app_widget` (nom de l'app), `heure` (ouverture) et `heure_fermeture` (fermeture)

### Capteur 3 : MediaTracker

- **Prerequis** : l'autorisation `NotificationListenerService` doit etre accordee. Un service stub (`MediaNotificationListenerService`) existe uniquement pour satisfaire l'exigence systeme de `MediaSessionManager.getActiveSessions()`.
- **Mecanisme** : enregistre un `MediaController.Callback` sur chaque session media active
- **Evenements detectes** : changement de `PlaybackState` — lecture (`STATE_PLAYING`), pause (`STATE_PAUSED`), suivant (`ACTION_SKIP_TO_NEXT`), precedent (`ACTION_SKIP_TO_PREVIOUS`)
- **Donnee produite** : un `Event` de type `widget` avec `nom_app_widget` = "NomApp (action)" et `emplacement_widget` = `ecran_verrouillage`
- **Robustesse** : si l'initialisation echoue (permission manquante), le tracker retourne `null` et le service continue sans suivi media

### Cycle de vie d'une activation

#### Activation manuelle

1. L'utilisateur appuie sur **Demarrer** → `ActivationManager.startActivation()`
2. Creation d'un enregistrement `Activation` en base (status = `active`)
3. L'ID de l'activation est sauvegarde dans `SharedPreferences` pour la persistance
4. Demarrage de `DataInterviewService` avec l'ID en extra de l'Intent
5. Le service enregistre les 3 capteurs et commence la collecte
6. L'utilisateur appuie sur **Arreter** → `ActivationManager.stopActivation()`
7. Le service est arrete, le CSV est genere, chiffre et envoye
8. L'enregistrement `Activation` est mis a jour avec le statut final

#### Activation programmee

1. L'utilisateur choisit une date/heure de debut et de fin
2. `ActivationManager.scheduleActivation(startTime, endTime)` cree une `Activation` avec status `scheduled`
3. Deux alarmes exactes sont programmees via `AlarmManager.setExactAndAllowWhileIdle()`
4. `AlarmReceiver` recoit les broadcasts et appelle `startActivation()` ou `stopActivation()`
5. Le reste du pipeline est identique a une activation manuelle

#### Redemarrage du telephone

1. `BootReceiver` ecoute `ACTION_BOOT_COMPLETED` et `ACTION_LOCKED_BOOT_COMPLETED`
2. Verifie dans `SharedPreferences` s'il existe un ID d'activation active
3. Si oui, relance `DataInterviewService` avec cet ID — la collecte reprend automatiquement

---

## Base de donnees locale

### Schema Room (SQLite)

#### Table `events`

| Colonne | Type | Description |
|---|---|---|
| `id` | INTEGER (PK, auto-increment) | Identifiant unique |
| `activationId` | INTEGER (FK) | Reference vers la table `activations` |
| `interactionType` | TEXT | `deverrouillage`, `application` ou `widget` |
| `time` | TEXT | Heure de l'evenement au format HH:MM |
| `appOrWidgetName` | TEXT (nullable) | Nom de l'application ou du widget |
| `closeTime` | TEXT (nullable) | Heure de fermeture HH:MM (applications uniquement) |
| `widgetLocation` | TEXT (nullable) | Emplacement du widget (ex: `ecran_verrouillage`) |
| `timestampMillis` | INTEGER | Timestamp en millisecondes pour le tri chronologique |

#### Table `activations`

| Colonne | Type | Description |
|---|---|---|
| `id` | INTEGER (PK, auto-increment) | Identifiant unique |
| `startTime` | INTEGER | Debut en epoch millisecondes |
| `endTime` | INTEGER (nullable) | Fin en epoch millisecondes (`null` si en cours) |
| `scheduledStart` | INTEGER (nullable) | Debut programme (activations programmees) |
| `scheduledEnd` | INTEGER (nullable) | Fin programmee (activations programmees) |
| `status` | TEXT | `active`, `completed` ou `scheduled` |
| `csvFilePath` | TEXT (nullable) | Chemin vers le fichier `.enc` genere |
| `uploadStatus` | TEXT (nullable) | `pending`, `success`, `failed: <erreur>` ou `encryption_failed` |
| `eventCount` | INTEGER | Nombre d'evenements a la fin de la collecte |

### Acces aux donnees (DAO)

- `EventDao` : insertion unitaire, requete par `activationId` (tries par `timestampMillis`), comptage par activation, suppression par activation
- `ActivationDao` : insertion, mise a jour, requete par ID, liste ordonnee par `startTime` DESC, requete des activations actives ou programmees

---

## Generation du CSV

### Fonctionnement (`CsvGenerator`)

1. Recoit la liste d'`Event` et l'ID de l'activation
2. Cree le fichier dans `context.filesDir/csv/` avec le nom `data_interview_{activationId}_{timestamp}.csv`
3. Ecrit l'en-tete : `type_interaction;heure;nom_app_widget;heure_fermeture;emplacement_widget`
4. Ecrit une ligne par evenement, les champs vides restent vides (pas de `null`)
5. Retourne le `File` pour la suite du pipeline

### Correspondance Event → CSV

| Champ Event | Colonne CSV |
|---|---|
| `interactionType` | `type_interaction` |
| `time` | `heure` |
| `appOrWidgetName` | `nom_app_widget` |
| `closeTime` | `heure_fermeture` |
| `widgetLocation` | `emplacement_widget` |

---

## Chiffrement des donnees

Les donnees sont chiffrees **sur le telephone** avant tout envoi reseau. Le chiffrement utilise un schema hybride standard.

### Schema de chiffrement hybride

```
CSV brut
   |
   v
[1] Generation d'une cle AES-256 aleatoire (KeyGenerator, 256 bits)
[2] Generation d'un IV aleatoire (SecureRandom, 12 octets)
[3] Chiffrement du CSV avec AES-256-GCM (tag d'authentification de 128 bits)
[4] Chiffrement de la cle AES avec RSA-2048-OAEP (SHA-256, MGF1-SHA-256)
   |
   v
Fichier .enc (binaire)
```

### Format du fichier `.enc`

| Offset | Taille | Contenu |
|---|---|---|
| 0–3 | 4 octets | Longueur de la cle AES chiffree (entier big-endian, typiquement 256) |
| 4–259 | 256 octets | Cle AES chiffree par RSA-OAEP |
| 260–271 | 12 octets | IV (nonce) pour AES-GCM |
| 272–fin | variable | Texte chiffre AES-256-GCM + tag d'authentification (16 octets) |

### Gestion des cles

- La **cle publique RSA-2048** est embarquee dans l'APK via `BuildConfig`. Elle permet uniquement de chiffrer — il est impossible de dechiffrer avec.
- La **cle privee** correspondante n'est **jamais presente sur le telephone**. Elle est conservee uniquement sur la machine du chercheur.
- Chaque fichier utilise une cle AES et un IV uniques generes aleatoirement, garantissant qu'aucun fichier ne partage le meme materiel cryptographique.

### Gestion des erreurs de chiffrement

Si le chiffrement echoue (cle publique invalide, erreur systeme...) :
1. Le fichier CSV brut est supprime immediatement (pas d'envoi en clair)
2. L'activation est marquee `encryption_failed`
3. Aucun envoi Telegram n'est tente
4. L'erreur est journalisee dans les logs Android

---

## Envoi via Telegram

### Fonctionnement (`TelegramUploader`)

- **Client HTTP** : OkHttp 4.12 avec timeouts de 30s (connexion/lecture) et 60s (ecriture)
- **Endpoint** : `POST https://api.telegram.org/bot{token}/sendDocument`
- **Format** : requete multipart avec le fichier `.enc` en `application/octet-stream`
- **Parametres** : `chat_id` (conversation cible) et `document` (fichier chiffre)

### Identifiants Telegram

Les identifiants du bot Telegram (token et chat ID) sont injectes a la compilation via `BuildConfig` depuis `local.properties` (fichier non commite dans le depot). Ils ne sont pas accessibles en clair dans le code source.

En cas d'absence des identifiants de compilation, l'ecran **Parametres** de l'application permet une saisie manuelle (stockee dans `SharedPreferences`).

### Statuts d'envoi

| Statut | Signification |
|---|---|
| `pending` | Fichier chiffre, envoi en cours |
| `success` | Fichier recu par Telegram |
| `failed: <message>` | Erreur reseau ou API (le message d'erreur est conserve) |
| `encryption_failed` | Le chiffrement a echoue — aucun envoi tente |

---

## Dechiffrement et analyse

### Script `decrypt_gui.py`

Le script Python `decrypt_gui.py` permet au chercheur de dechiffrer les fichiers `.enc` recus via Telegram et de generer un rapport d'analyse. Il fonctionne en mode GUI (glisser-deposer) ou en ligne de commande.

#### Prerequis

```bash
pip install cryptography tkinterdnd2
```

#### Utilisation

```bash
# Mode GUI (glisser-deposer ou bouton Browse)
python decrypt_gui.py

# Mode ligne de commande
python decrypt_gui.py --input fichier.enc
```

- `--input` : chemin vers le fichier `.enc` a dechiffrer (mode CLI, sans GUI)
- **Sortie** : deux fichiers au meme emplacement que le `.enc` :
  - `fichier.csv` — pour Excel (delimiteur `;`, BOM UTF-8, rapport en en-tete)
  - `fichier_R.csv` — pour R (delimiteur `,`, pas de commentaires, donnees uniquement)

```r
# Chargement dans R
df <- read.csv("fichier_R.csv")
```

#### Etapes du dechiffrement

1. Chargement de la cle privee RSA (embarquee dans le script ou via fichier PEM)
2. Lecture du fichier `.enc` selon le format binaire decrit ci-dessus (longueur de cle, cle chiffree, IV, texte chiffre)
3. Dechiffrement de la cle AES avec RSA-OAEP (SHA-256, MGF1-SHA-256)
4. Dechiffrement du CSV avec AES-256-GCM
5. Enrichissement du CSV avec des metriques calculees (voir ci-dessous)
6. Ecriture du fichier Excel CSV (`;`, BOM UTF-8, avec rapport) et du fichier R CSV (`,`, donnees uniquement)

#### Rapport de sobriete numerique

Le script ajoute en en-tete du CSV un rapport d'analyse commente (lignes prefixees par `#`) contenant :

**Metriques generales :**
- Temps d'ecran total (somme des durees d'utilisation, hors launcher)
- Premiere et derniere utilisation de la journee

**Comportement de deverrouillage :**
- Nombre total de deverrouillages
- Temps moyen entre deux deverrouillages
- Nombre de consultations rapides (sessions <= 30 secondes)

**Patterns de sessions :**
- Nombre total de sessions (une session = du deverrouillage jusqu'au suivant)
- Duree moyenne par session
- Nombre moyen d'applications differentes par session

**Reseaux sociaux :**
- Temps total passe sur les reseaux sociaux et pourcentage du temps d'ecran
- Detail par application sociale (nombre d'ouvertures, duree)
- Applications reconnues : Instagram, Facebook, Messenger, Twitter/X, TikTok, Snapchat, Reddit, LinkedIn, Telegram, WhatsApp, Discord, Pinterest, Tumblr, BeReal

**Usage par application :**
- Liste complete de toutes les applications utilisees, triees par temps d'utilisation decroissant
- Pour chaque application : nom lisible, nombre d'ouvertures, duree totale, nom du package

#### Colonne ajoutee au CSV

Le script ajoute une 6e colonne `duree_secondes` qui calcule automatiquement la duree en secondes entre `heure` et `heure_fermeture` pour chaque ligne d'application.

#### Noms lisibles

Le script convertit les noms de packages Android (ex: `com.instagram.android`) en noms lisibles (ex: `Instagram`) grace a un dictionnaire integre. Les packages inconnus sont convertis automatiquement en extrayant le dernier segment du nom (ex: `com.example.myapp` → `Myapp`).

### Exemple de sortie

```csv
# === DIGITAL SOBRIETY REPORT ===
#
# Total screen time: 45m30s
# First use: 08:30 | Last use: 17:45
#
# --- Unlock behavior ---
# Unlock count: 12
# Avg time between unlocks: 42m15s
# Quick checks (<=30s): 3/12 sessions
#
# --- Session patterns ---
# Total sessions: 12
# Avg session duration: 3m47s
# Avg app switches/session: 2.3
#
# --- Social media ---
# Social media time: 18m00s (40% of screen time)
#   Instagram: 5x, 10m00s  (com.instagram.android)
#   WhatsApp: 3x, 8m00s  (com.whatsapp)
#
# --- App usage (by time) ---
# Instagram;5x;10m00s  (com.instagram.android)
# Chrome;4x;9m30s  (com.android.chrome)
# WhatsApp;3x;8m00s  (com.whatsapp)
#
type_interaction;heure;nom_app_widget;heure_fermeture;emplacement_widget;duree_secondes
deverrouillage;08:30;;;;
application;08:30;Chrome;08:45;;900
widget;08:40;Spotify (lecture);;ecran_verrouillage;
```

---

## Ecrans de l'application

### MainActivity (ecran principal)

- Affiche le statut de collecte : **"Collecte en cours"** (vert) ou **"Collecte arretee"** (rouge)
- Bouton **Demarrer / Arreter** pour le controle manuel
- Section **Programmer une activation** avec selecteurs de date et heure
- Boutons de navigation vers **Historique** et **Autorisations**
- Met a jour l'interface a chaque retour au premier plan (`onResume`)

### PermissionsActivity

- `RecyclerView` listant les autorisations requises
- Chaque item affiche le nom, l'explication et un bouton **Accorder**
- Verification dynamique du statut de chaque autorisation
- Gere 5 types : Usage Stats, Notification Listener, Restricted Settings (API 33+), POST_NOTIFICATIONS (API 33+), Battery Optimization (API 23+)

### HistoryActivity

- `RecyclerView` des activations passees, triees par date decroissante
- Chaque carte affiche : dates, duree, nombre d'evenements, indicateur de statut d'envoi
- Clic sur une carte ouvre le `CsvViewerActivity`

### CsvViewerActivity

- Lit le fichier CSV et le rend sous forme de `TableLayout` Android
- En-tete en gras, une ligne par evenement
- Menu d'options pour partager le fichier via `FileProvider` (`Intent.ACTION_SEND`)

### SettingsActivity

- Champs de saisie manuelle pour les identifiants Telegram (token et chat ID)
- Utilise uniquement si les identifiants ne sont pas fournis a la compilation
- Stockage dans `SharedPreferences` (contexte `data_interview`)

---

## Compilation

### Prerequis

- Android Studio Hedgehog (2023.1) ou superieur
- JDK 17
- Android SDK 35

### Configuration des identifiants

Creer un fichier `android/local.properties` (non commite) avec :

```properties
TELEGRAM_BOT_TOKEN=votre_token_ici
TELEGRAM_CHAT_ID=votre_chat_id_ici
```

Ces valeurs sont injectees dans `BuildConfig` a la compilation via `app/build.gradle.kts`.

La cle publique de chiffrement est deja presente dans `gradle.properties` (ce fichier est commite car la cle publique ne permet que le chiffrement).

### Etapes

1. Cloner le depot :
   ```bash
   git clone https://github.com/slipihollow/Data_interview.git
   cd Data_interview/android
   ```

2. Ouvrir le dossier `android/` dans Android Studio

3. Synchroniser Gradle (automatique a l'ouverture)

4. Compiler le APK :
   - **Debug** : Build > Build Bundle(s) / APK(s) > Build APK(s)
   - **Release** : Build > Generate Signed Bundle / APK, puis suivre l'assistant de signature

5. Le APK se trouve dans `app/build/outputs/apk/`

### Structure du build

Les champs `BuildConfig` generes automatiquement :

| Champ | Source | Description |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | `local.properties` | Token du bot Telegram |
| `TELEGRAM_CHAT_ID` | `local.properties` | ID de la conversation Telegram |
| `ENCRYPTION_PUBLIC_KEY` | `gradle.properties` | Cle publique RSA-2048 (Base64, format X.509 DER) |

### Proguard / R8

Le build release active R8 (minification) avec les regles definies dans `proguard-rules.pro`.

---

## Tests

```bash
cd android && ./gradlew test
```

### Tests unitaires

#### CsvGeneratorTest (5 tests)

- Validation du format de l'en-tete CSV
- Format correct d'une ligne de deverrouillage
- Format correct d'une ligne d'application (avec heure de fermeture)
- Format correct d'une ligne de widget (avec emplacement)
- Plusieurs evenements produisent plusieurs lignes

#### CsvEncryptorTest (5 tests)

- Aller-retour chiffrement/dechiffrement avec une paire de cles de test
- Le fichier CSV brut est supprime apres chiffrement
- Structure du fichier chiffre (longueur de cle, IV, texte chiffre)
- Une cle publique invalide leve une exception
- Conformite du format (RSA-2048 produit une cle chiffree de 256 octets)

---

## Stockage des donnees

| Donnee | Emplacement | Persistance |
|---|---|---|
| Evenements et activations | Base Room `data_interview.db` (stockage prive de l'app) | Jusqu'a desinstallation |
| Fichiers `.enc` chiffres | `context.filesDir/csv/` (stockage prive de l'app) | Jusqu'a desinstallation |
| ID d'activation en cours | `SharedPreferences` (cle `data_interview`) | Jusqu'a desinstallation |
| Identifiants Telegram manuels | `SharedPreferences` (cle `data_interview`) | Jusqu'a desinstallation |

---

## Autorisations Android (Manifest)

| Autorisation | Usage |
|---|---|
| `INTERNET` | Envoi des fichiers chiffres via Telegram |
| `WAKE_LOCK` | Maintenir le telephone actif pendant le service |
| `FOREGROUND_SERVICE` | Executer le service de collecte en premier plan |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Requis par API 34+ pour les foreground services a usage special |
| `PACKAGE_USAGE_STATS` | Interroger l'historique d'utilisation des applications |
| `RECEIVE_BOOT_COMPLETED` | Relancer le service apres un redemarrage |
| `POST_NOTIFICATIONS` | Afficher la notification de collecte (API 33+) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Demander la desactivation de l'optimisation batterie |
| `SCHEDULE_EXACT_ALARM` | Programmer des alarmes exactes pour les activations (API 31+) |

---

## Licence

Projet de recherche academique. Usage reserve aux tests internes en attendant l'approbation IRB.
