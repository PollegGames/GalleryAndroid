# Mises à jour et signature de Galerie

## Identité Android permanente

- application ID : `com.polleg.gallery` ;
- fichier de sauvegarde : `gallery-release.jks` ;
- alias : `gallery` ;
- certificat SHA-256 permanent :
  `2A:11:52:F2:69:66:CF:84:39:D5:0A:E8:91:FD:E1:A7:3B:94:4A:06:54:F0:25:F0:37:AF:02:E5:63:29:B3:C3` ;
- validité du certificat : 23 août 2026 au 30 juillet 2126.

La clé privée et ses mots de passe ne sont jamais présents dans Git. Une copie
de sauvegarde doit être conservée hors du repository. Perdre cette clé empêche
définitivement de publier une mise à jour acceptée par Android pour ce package.

## Secrets GitHub Actions

Les réglages `Settings > Secrets and variables > Actions` du repository doivent
contenir exactement :

- `GALLERY_KEYSTORE_BASE64` : contenu Base64 monoligne du keystore ;
- `GALLERY_KEYSTORE_PASSWORD` : mot de passe du keystore ;
- `GALLERY_KEY_ALIAS` : `gallery` ;
- `GALLERY_KEY_PASSWORD` : mot de passe de la clé.

Les workflows reconstruisent le keystore dans le dossier temporaire du runner.
Les APK `debug` produits par `build-test.yml` et les APK `release` produits par
`release.yml` utilisent la même configuration `gallery`.

## Migration depuis les anciennes APK

L’APK 1.1.1 fourni précédemment est signé par une clé `Android Debug` dont
l’empreinte SHA-256 est :
`DC:58:2E:F1:27:4A:D3:3F:4D:4A:B5:CB:F6:3B:03:97:07:C9:97:13:25:C1:50:BC:13:0E:DA:5A:2F:08:50:C9`.

Cette empreinte diffère de la clé permanente. Android refusera donc probablement
la première installation de `1.1.3-test` par-dessus une ancienne version. Dans
ce cas uniquement :

1. désinstaller l’ancienne Galerie ;
2. installer `Galerie-1.1.3-test.apk` ;
3. installer ensuite une APK de versionCode supérieur signée avec la même clé ;
4. vérifier qu’Android affiche **Mettre à jour**.

Cette désinstallation doit être la dernière.

## Cycle de développement

1. partir de `main`, qui contient la dernière version stable compilable ;
2. créer `test/x.x.x` ;
3. augmenter `versionCode` sans réutiliser une valeur ancienne ;
4. utiliser un `versionName` lisible, par exemple `1.1.4-test2` ;
5. pousser la branche et télécharger l’APK signé depuis GitHub Actions ;
6. valider l’installation et les régressions sur téléphone ;
7. passer au prochain `versionCode`, retirer le suffixe `-test`, puis merger vers `main` ;
8. créer le tag strictement identique, par exemple `v1.1.4` ;
9. `release.yml` crée ou complète la GitHub Release et joint l’APK complet.

Le workflow n’écrit jamais dans le repository. Il effectue uniquement le
checkout, les tests, le lint, l’assemblage, la vérification de signature et la
publication de l’APK.

## Règles non négociables

- ne jamais changer `applicationId` ;
- ne jamais régénérer la clé ou changer l’alias ;
- ne jamais committer le keystore, son Base64 ou ses mots de passe ;
- ne jamais diminuer ou réutiliser un `versionCode` ;
- ne jamais publier comme officielle une APK locale signée avec une clé debug ;
- conserver l’APK comme livrable GitHub principal, même si un AAB est ajouté plus tard.
