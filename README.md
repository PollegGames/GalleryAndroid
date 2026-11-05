# Galerie Android

Application Android native en Kotlin et Jetpack Compose qui affiche les images et
vidéos de MediaStore, sur le téléphone comme sur les cartes SD.

## Fonctionnalités

- 50 médias les plus récents au premier affichage, puis chargement automatique par lots de 50 à l’approche du bas ;
- tri par la date la plus récente entre la prise de vue et l’ajout, avec la date de
  modification utilisée uniquement en dernier recours ;
- images et vidéos réunies dans une grille, avec durée et aperçu des vidéos ;
- arbre récursif des dossiers, comptes cumulés et sélection d’un dossier parent ;
- distinction entre stockage interne et cartes SD, y compris une carte devenue
  indisponible ;
- dossiers épinglés et bandeau repliable avec monogrammes persistants ;
- lecteur interne d’images et vidéos avec swipe horizontal ;
- zoom photo de 1× à 5×, déplacement borné et double toucher à 2,5× ;
- ouverture des images reçues par `ACTION_VIEW image/*` depuis Fichiers,
  Téléchargements ou une autre application ;
- partage simple et multiple avec aperçu fourni via `ClipData` ;
- restauration du dossier, de l’historique, du nombre d’éléments et de la position ;
- actualisation au retour dans l’application et observation de MediaStore/du
  montage des volumes uniquement quand l’écran est actif ;
- accès complet ou limité aux photos et vidéos sur les versions Android récentes.
- navigation latérale adaptative, superposée sur téléphone et permanente sur tablette ;
- sélection multiple des médias par appui long ;
- suppression avec confirmation Android ;
- explorateur de destination hiérarchique, depuis les racines Téléphone et Carte SD ;
- déplacement sans doublon, y compris depuis WhatsApp Images et entre deux stockages.

L’application n’utilise ni `MANAGE_EXTERNAL_STORAGE`, ni chemin physique, ni base
Room, ni service en arrière-plan.

## Installer ou mettre à jour l’APK

**[Télécharger Galerie 1.1.4 (APK)](https://github.com/PollegGames/GalleryAndroid/releases/download/v1.1.4/Galerie-1.1.4.apk)**

Télécharger la dernière version depuis la page
[`Releases`](https://github.com/PollegGames/GalleryAndroid/releases/latest).

Les versions officielles de test et release à partir de `1.1.3-test` utilisent
la même clé permanente. Leur `applicationId` reste `com.polleg.gallery` et leur
`versionCode` augmente à chaque APK : Android affiche donc **Mettre à jour**.

Les anciens APK 1.1.0 à 1.1.2 utilisaient des clés de développement. La première
installation de `1.1.3-test` peut demander une dernière désinstallation. Les
données locales de Galerie seront alors réinitialisées. Après cette migration,
il ne faut plus désinstaller l’application pour les versions suivantes.

Pour une version de test : ouvrir l’exécution correspondante dans GitHub
Actions, télécharger l’artifact `Galerie-x.x.x-test`, puis ouvrir l’APK sur le
téléphone. Pour une version finale, utiliser uniquement la page Releases.

La procédure durable, les empreintes de certificat et la récupération de la clé
sont documentées dans
[`docs/MISES_A_JOUR_ET_SIGNATURE.md`](docs/MISES_A_JOUR_ET_SIGNATURE.md).

## Ouvrir et compiler le projet

Pré-requis :

- Android Studio compatible avec AGP 8.13.2 ;
- JDK 17 ;
- Android SDK Platform 36.

Le projet contient un seul module Gradle `app`. `MainActivity` affiche la
galerie et `ViewerActivity` affiche un média interne ou une URI externe.

```bash
./gradlew testDebugUnitTest lint assembleDebug
```

L’APK de débogage généré se trouve ensuite dans :

```text
app/build/outputs/apk/debug/app-debug.apk
```

Une compilation locale sans variables de signature utilise la clé debug locale
et ne doit pas être distribuée comme mise à jour officielle. Les workflows
GitHub reconstruisent la clé permanente depuis les secrets et signent aussi bien
les APK de test que les releases.

Installation locale avec ADB, uniquement pour le développement :

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
├── ViewerActivity.kt
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
- la sélection multiple et la priorité du bouton Retour ;
- le cycle de suppression confirmé et l’actualisation ;
- les destinations autorisées selon le type de média ;
- le déplacement direct, la sortie de `Android/media` et le passage téléphone/carte SD ;
- les résultats partiels et les erreurs MediaStore détaillées.
- les limites du zoom et le filtrage des intents externes.

Commandes de validation du projet :

```text
./gradlew testDebugUnitTest lint assembleDebug
./gradlew testDebugUnitTest lint assembleRelease
```

Les scénarios qui nécessitent du matériel réel sont détaillés dans
[`docs/PLAN_DE_TEST_APPAREIL.md`](docs/PLAN_DE_TEST_APPAREIL.md).

## Retouche (1.1.4)

Ouvrir une photo ou une vidéo, puis toucher **Modifier** dans la barre du lecteur.

- Photos : rotation, miroir, recadrage (original, carré, 4:3, 3:4, 16:9, 9:16),
  zoom et déplacement du cadrage, luminosité, contraste, saturation, filtres
  naturel/noir et blanc/chaud/froid/sépia, comparaison avec l’original et réinitialisation.
- Vidéos : choix du début et de la fin, lecture de l’extrait, rotation et retrait du son.
- **Enregistrer une copie** conserve toujours le média original. Les copies sont dans
  `Pictures/Galerie` (JPEG ou PNG transparent) et `Movies/Galerie` (MP4 H.264/AAC).
- Photos exportées jusqu’à 4096 pixels sur le plus grand côté, avec une limite
  adaptée à la mémoire du téléphone. Les images animées sont exportées en image fixe.
  Les métadonnées EXIF (notamment GPS et date originale) ne sont pas recopiées.
- Les réglages et l’export survivent à la rotation de l’appareil. Rester dans
  l’éditeur pendant l’export vidéo ; une erreur de codec ou d’espace libre est affichée.

L’export vidéo utilise [Media3 Transformer](https://developer.android.com/media/media3/transformer/getting-started).
La validation sur téléphone des formats, codecs et aperçus reste nécessaire.
