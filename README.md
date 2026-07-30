# Galerie Android

Application Android native en Kotlin et Jetpack Compose qui affiche les images et
vidéos de MediaStore, sur le téléphone comme sur les cartes SD.

## Fonctionnalités

- 10 médias les plus récents au premier affichage, puis chargement par lots de 50 ;
- tri par la date la plus récente entre la prise de vue et l’ajout, avec la date de
  modification utilisée uniquement en dernier recours ;
- images et vidéos réunies dans une grille, avec durée et aperçu des vidéos ;
- arbre récursif des dossiers, comptes cumulés et sélection d’un dossier parent ;
- distinction entre stockage interne et cartes SD, y compris une carte devenue
  indisponible ;
- dossiers épinglés et bandeau repliable avec monogrammes persistants ;
- ouverture dans le lecteur Android natif ;
- restauration du dossier, de l’historique, du nombre d’éléments et de la position ;
- actualisation au retour dans l’application et observation de MediaStore/du
  montage des volumes uniquement quand l’écran est actif ;
- accès complet ou limité aux photos et vidéos sur les versions Android récentes.

L’application n’utilise ni `MANAGE_EXTERNAL_STORAGE`, ni chemin physique, ni base
Room, ni service en arrière-plan.

## Installer l’APK de démonstration

Télécharger [`Galerie-1.0.0-demo.apk`](Galerie-1.0.0-demo.apk), puis l’installer
avec ADB :

```bash
adb install -r Galerie-1.0.0-demo.apk
```

Cet APK `release` est signé avec une clé Android de développement. Il convient aux
tests et à une installation manuelle, mais pas à une publication sur Google Play.

## Ouvrir et compiler le projet

Pré-requis :

- Android Studio compatible avec AGP 8.13.2 ;
- JDK 17 ;
- Android SDK Platform 36.

Le projet est volontairement composé d’une seule activité et d’un seul module
Gradle `app`.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

L’APK de débogage généré se trouve ensuite dans :

```text
app/build/outputs/apk/debug/app-debug.apk
```

Installation avec ADB :

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Le projet compile avec `minSdk 29`, `compileSdk 36` et `targetSdk 36`. Android 17
(API 37) est encore en phase bêta au moment de cette version du projet ; API 36
reste donc le choix stable. Le passage à API 37 pourra se faire lorsque le SDK et
la chaîne Gradle correspondante seront stables.

## Organisation

```text
com.polleg.gallery/
├── MainActivity.kt
├── GalleryApplication.kt
├── AppContainer.kt
└── gallery/
    ├── ui/             Compose, état, actions, effets et ViewModel
    ├── application/    Queries, commands, handlers et ports
    ├── domain/         Modèles et règles pures
    ├── data/
    │   ├── mediastore/ Requêtes, dossiers et observation de MediaStore
    │   └── preferences/JSON DataStore
    └── platform/       Permissions, volumes et lecteur Android
```

Le flux est unidirectionnel :

```text
Compose → GalleryAction → GalleryViewModel → handlers → repositories
Compose ← GalleryUiState ← GalleryViewModel
Android ← GalleryEffect
```

## Validation

Les tests unitaires couvrent notamment :

- les règles de date ;
- la fusion et le classement des 10 médias récents entre téléphone et carte SD ;
- l’arbre récursif et les comptes cumulés ;
- les monogrammes ;
- la sérialisation des emplacements restaurés.

Commandes de validation du projet :

```text
testDebugUnitTest
assembleDebug
assembleRelease
lintDebug
lintVitalRelease
```

Les scénarios qui nécessitent du matériel réel sont détaillés dans
[`docs/PLAN_DE_TEST_APPAREIL.md`](docs/PLAN_DE_TEST_APPAREIL.md).
