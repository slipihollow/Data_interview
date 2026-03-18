# Data Interview

Application Android de recherche pour le suivi passif des interactions smartphone-utilisateur dans le cadre d'une etude academique approuvee par un comite d'ethique (IRB).

L'application enregistre les deverrouillages, l'utilisation des applications et les interactions avec les controles media, puis genere un fichier CSV par periode d'activation, envoye automatiquement via Telegram.

---

## Table des matieres

- [Guide participant](#guide-participant)
  - [Installation](#installation)
  - [Autorisations requises](#autorisations-requises)
  - [Utilisation](#utilisation)
  - [Optimisation batterie par fabricant](#optimisation-batterie-par-fabricant)
- [Guide chercheur](#guide-chercheur)
  - [Configuration Telegram](#configuration-telegram)
  - [Compilation](#compilation)
  - [Format des donnees](#format-des-donnees)
  - [Architecture technique](#architecture-technique)
  - [Structure du projet](#structure-du-projet)

---

# Guide participant

## Installation

### Prerequis

- Smartphone Android 5.0 (Lollipop) ou superieur
- Autoriser l'installation depuis des sources inconnues

### Etapes

1. Telecharger le fichier APK depuis la section [Releases](../../releases) de ce depot
2. Ouvrir le fichier APK sur votre telephone
3. Si demande, autoriser l'installation depuis cette source
4. Appuyer sur **Installer**
5. Ouvrir l'application **Data Interview**

---

## Autorisations requises

L'application necessite plusieurs autorisations systeme pour fonctionner. Un ecran de configuration guide l'utilisateur etape par etape.

| Autorisation | Raison | Comment l'accorder |
|---|---|---|
| **Acces aux statistiques d'utilisation** | Detecter quelles applications sont utilisees | Parametres > Acces aux donnees d'utilisation > Data Interview |
| **Acces aux notifications** | Detecter les controles media (lecture/pause) | Parametres > Acces aux notifications > Data Interview |
| **Notifications** (Android 13+) | Afficher la notification de collecte en cours | Accepter la demande de l'application |
| **Optimisation de la batterie** | Empecher Android d'arreter la collecte en arriere-plan | Accepter la demande de desactivation |

Pour accorder ces autorisations :
1. Ouvrir Data Interview
2. Appuyer sur **Autorisations**
3. Pour chaque autorisation non accordee, appuyer sur **Accorder** et suivre les instructions

---

## Utilisation

### Demarrer une collecte manuelle

1. Ouvrir Data Interview
2. Verifier que toutes les autorisations sont accordees (voir ecran **Autorisations**)
3. Appuyer sur **Demarrer**
4. Une notification permanente confirme : *"Data Interview — Collecte en cours"*
5. Utiliser le telephone normalement

### Arreter la collecte

1. Ouvrir Data Interview
2. Appuyer sur **Arreter**
3. L'application genere le fichier CSV et l'envoie automatiquement via Telegram

### Programmer une collecte

1. Appuyer sur **Programmer une activation**
2. Selectionner la **date et heure de debut**
3. Selectionner la **date et heure de fin**
4. Appuyer sur **Confirmer la programmation**
5. La collecte demarrera et s'arretera automatiquement aux horaires choisis

### Consulter l'historique

1. Appuyer sur **Historique**
2. Chaque activation passee affiche : dates, duree, nombre d'evenements, statut d'envoi
3. Appuyer sur une activation pour visualiser le contenu du CSV

### Apres un redemarrage du telephone

L'application redemarre automatiquement la collecte si elle etait en cours avant le redemarrage. Aucune action n'est necessaire.

---

## Optimisation batterie par fabricant

Certains fabricants arretent agressivement les applications en arriere-plan. Suivez les instructions specifiques a votre telephone :

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

# Guide chercheur

## Configuration Telegram

Les identifiants Telegram sont integres dans l'APK a la compilation. Les participants n'ont rien a configurer — les CSV sont envoyes automatiquement dans votre groupe prive.

### 1. Creer un bot Telegram

1. Ouvrir Telegram et chercher **@BotFather**
2. Envoyer `/newbot` et suivre les instructions
3. Copier le **token du bot** (ex: `123456:ABC-DEF...`)

### 2. Creer un groupe prive

1. Creer un **nouveau groupe** Telegram (seul vous et vos co-chercheurs)
2. Ajouter le bot au groupe
3. Envoyer `/setprivacy` a @BotFather > selectionner votre bot > **Disable** (pour que le bot voie les messages du groupe)

### 3. Obtenir l'ID du chat

1. Envoyer un message dans le groupe
2. Ouvrir dans un navigateur : `https://api.telegram.org/bot<TOKEN>/getUpdates`
3. Reperer la valeur `"chat":{"id": ...}` — c'est votre **ID du chat** (nombre negatif)

### 4. Configurer dans le projet

Modifier le fichier `android/gradle.properties` :

```properties
TELEGRAM_BOT_TOKEN=votre_token_ici
TELEGRAM_CHAT_ID=votre_chat_id_ici
```

Puis recompiler l'APK (voir [Compilation](#compilation)). Les identifiants seront integres dans `BuildConfig` et invisibles pour les participants.

### 5. Verifier

Ouvrir dans un navigateur (remplacer les valeurs) :
```
https://api.telegram.org/bot<TOKEN>/sendMessage?chat_id=<CHAT_ID>&text=Bot+ready
```
Si `"ok": true` apparait et le message s'affiche dans le groupe, c'est pret.

---

## Format des donnees

Chaque activation produit un fichier CSV avec le delimiteur `;` (point-virgule).

### Schema (5 colonnes)

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

### Exemple

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

## Architecture technique

### Composants principaux

```
DataInterviewApp                  Application, cree le canal de notification
  |
  +-- DataInterviewService        Service de premier plan (foreground service)
  |     +-- UnlockReceiver        BroadcastReceiver (ACTION_USER_PRESENT)
  |     +-- AppUsageTracker       Interroge UsageStatsManager toutes les 5s
  |     +-- MediaTracker          MediaSessionManager + callbacks
  |
  +-- ActivationManager           Demarre/arrete le service, genere le CSV
  |     +-- AlarmReceiver         Alarmes pour activations programmees
  |
  +-- TelegramUploader            HTTP POST multipart via OkHttp
  |
  +-- AppDatabase (Room)          Base locale SQLite
        +-- Event                 Evenements de tracking
        +-- Activation            Periodes d'activation
```

### Compatibilite

- **minSdk 21** (Android 5.0 Lollipop) — couvre ~99% des appareils actifs
- Code conditionnel (`Build.VERSION.SDK_INT`) pour les API 23, 26, 29, 31, 33 et 34
- `ContextCompat` et `NotificationCompat` pour la retrocompatibilite

### Technologies

| Composant | Technologie |
|---|---|
| Langage | Kotlin 2.0 |
| Base de donnees | Room 2.6.1 |
| Reseau | OkHttp 4.12 |
| Coroutines | kotlinx-coroutines 1.8 |
| UI | Material Components 1.12 |
| Build | Gradle 8.7, AGP 8.5 |

### Stockage des donnees

- Base de donnees Room locale (`data_interview.db`)
- Fichiers CSV dans le stockage interne de l'application (`files/csv/`)
- Identifiants Telegram integres via `BuildConfig` (configures dans `gradle.properties`)

---

## Compilation

### Prerequis

- Android Studio Hedgehog (2023.1) ou superieur
- JDK 17
- Android SDK 35

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

### Tests

```bash
./gradlew test
```

Les tests unitaires couvrent la generation CSV (5 tests dans `CsvGeneratorTest`).

---

## Structure du projet

```
Data_interview/
  android/                        Projet Android
    app/
      src/main/
        java/com/datainterview/app/
          activation/             Gestion des activations et alarmes
          data/                   Room (entites, DAOs, base), generateur CSV
          service/                Service de premier plan, receivers, trackers
          ui/                     Activites (main, historique, parametres, autorisations)
          upload/                 Envoi Telegram
        res/                      Layouts, strings (francais), themes, drawables
      src/test/                   Tests unitaires
    build.gradle.kts              Configuration Gradle (app)
  problems/PB-001-*/              Documentation du probleme (investigation, decision, plan)
  commit.sh                       Script de workflow git
```

---

## Licence

Projet de recherche academique. Usage reserve aux participants de l'etude.
