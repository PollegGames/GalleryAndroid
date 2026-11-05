# Plan de test sur appareil

Ces contrôles nécessitent un téléphone ou un émulateur doté de médias réels. Les
tests automatisés du projet couvrent les règles métier, mais ne peuvent pas
simuler fidèlement le fournisseur MediaStore d’un constructeur ni l’éjection
physique d’une carte SD.

## 1. Premier affichage et ordre

1. Préparer au moins 14 images et vidéos réparties entre le téléphone et une carte
   SD.
2. S’assurer que plusieurs dates de prise et d’ajout diffèrent.
3. Réinstaller ou effacer les données de l’application.
4. Autoriser l’accès aux images et vidéos.
5. Vérifier que la grille contient 10 éléments.
6. Comparer leur ordre avec la valeur la plus récente entre date de prise et date
   d’ajout.
7. Vérifier qu’une date de modification plus récente ne l’emporte pas lorsqu’une
   date de prise ou d’ajout existe.

## 2. Pagination

1. Avec plus de 60 médias, toucher « Charger 50 de plus ».
2. Vérifier que 60 éléments sont visibles, sans doublon et dans le même ordre.
3. Répéter l’opération et vérifier des lots supplémentaires de 50.

## 3. Dossiers

1. Ouvrir un dossier parent tel que `DCIM`.
2. Vérifier que les médias de `DCIM/Camera`, `DCIM/Screenshots` et des autres
   descendants sont inclus.
3. Vérifier les comptes directs et récursifs dans l’arbre.
4. Épingler un dossier, relancer l’application et vérifier sa présence.
5. Replier le bandeau et vérifier les monogrammes.

## 4. Ouverture et restauration

1. Faire défiler la grille jusqu’à un média identifiable.
2. Ouvrir ce média dans le lecteur interne.
3. Revenir dans l’application et vérifier le même dossier et la même position.
4. Refaire le contrôle après une rotation.
5. Activer « Ne pas conserver les activités » dans les options développeur et
   refaire le contrôle après l’ouverture d’un média.

## 5. Carte SD

1. Depuis « Récents », vérifier que les médias des deux volumes sont réunis.
2. Épingler un dossier de la carte SD.
3. Éjecter proprement la carte pendant que l’application est en arrière-plan.
4. Revenir dans l’application : la carte doit apparaître indisponible et le
   dossier épinglé ne doit pas être sélectionnable.
5. Réinsérer la carte : l’arbre et la grille doivent se reconstruire.

## 6. Changements MediaStore

1. Laisser l’application au premier plan.
2. Prendre une photo ou copier un média avec une autre application.
3. Revenir à Galerie si nécessaire.
4. Vérifier que le nouveau média apparaît sans redémarrage.

## 7. Permissions partielles

1. Sur Android 14 ou plus récent, accorder uniquement une sélection de médias.
2. Vérifier l’avertissement d’accès limité.
3. Vérifier que seuls les médias autorisés sont visibles.
4. Accorder ensuite l’accès complet dans les réglages Android et revenir dans
   l’application.
5. Vérifier la disparition de l’avertissement et l’actualisation de la galerie.

## 8. Déplacement et absence de doublon

1. Sélectionner deux images dans `WhatsApp Images` avec un appui long.
2. Toucher « Déplacer » et vérifier que le sélecteur affiche d’abord les racines
   « Téléphone » et « Carte SD » au lieu d’une liste de chemins complets.
3. Ouvrir « Téléphone », puis `DCIM` ou `Pictures`, et toucher « Déplacer ici ».
4. Autoriser la modification dans la boîte de dialogue Android.
5. Vérifier que les deux images ont disparu de `WhatsApp Images`, qu’elles sont
   présentes dans le dossier choisi et qu’aucun doublon ne subsiste.
6. Refaire l’essai vers un dossier `DCIM` ou `Pictures` de la carte SD, puis dans
   le sens inverse.
7. Vérifier que les répertoires privés `Android/media` ne sont jamais proposés
   comme destination et que `Movies` n’est proposé que pour une sélection
   composée uniquement de vidéos.
8. Refuser l’autorisation Android et vérifier que ni la source ni la destination
   n’ont changé.

## 9. Lecteur interne et vidéo

1. Ouvrir successivement une photo, une vidéo, puis une autre photo.
2. Vérifier le swipe horizontal et le retour à la même position dans la galerie.
3. Sur la vidéo, vérifier lecture/pause, recul de 5 secondes, avance de 15 secondes,
   volume et barre de progression.
4. Toucher la vidéo pour afficher ou masquer les contrôles et vérifier leur
   masquage automatique pendant la lecture.
5. Vérifier Partager, Déplacer et Supprimer depuis le lecteur.

## 10. Zoom photo

1. Tester une photo portrait, paysage et une photo de grande définition.
2. Pincer jusqu’à 5× puis dézoomer jusqu’à 1×.
3. À plus de 1×, déplacer la photo dans les quatre directions et vérifier qu’elle
   ne peut pas être perdue hors écran.
4. Vérifier le double toucher 1× → 2,5× puis 2,5× → 1×.
5. À 1×, vérifier que le swipe change de photo.
6. À plus de 1×, vérifier que le mouvement déplace d’abord la photo sans changer
   de page.
7. Passer à la photo suivante après un zoom et vérifier le reset à 1×, offset zéro.
8. Vérifier qu’un toucher simple continue à afficher ou masquer les actions.

## 11. Ouverture depuis Android

1. Dans Téléchargements, ouvrir un JPEG avec **Ouvrir avec > Galerie**.
2. Vérifier l’affichage, le nom du fichier et le partage.
3. Dans un gestionnaire de fichiers, refaire le test avec un PNG.
4. Vérifier qu’une URI externe non MediaStore garde Move et Supprimer désactivés.
5. Vérifier qu’aucune permission de stockage global ni chemin physique n’est demandé.
6. Vérifier que les vidéos ne proposent pas Galerie dans cette version.

## 12. Mise à jour signée

1. Si nécessaire, désinstaller une dernière fois l’ancienne APK signée en debug.
2. Installer `Galerie-1.1.3-test.apk`.
3. Installer une seconde APK avec le même certificat et un `versionCode` supérieur.
4. Vérifier qu’Android affiche **Mettre à jour**, sans demander de désinstallation.
5. Lancer Galerie et refaire les contrôles essentiels : permissions, récents,
   dossiers, carte SD, partage, déplacement, suppression et viewer.

## 1.1.4 : défilement et édition

- Bibliothèques de 0, 12, 50, 51 et 125 médias : vérifier 50 au départ au maximum,
  le chargement automatique et l’arrêt au dernier média, sans doublon ni saut.
- Défiler rapidement, changer de dossier pendant le chargement, revenir en arrière.
  Simuler une indisponibilité du stockage : les médias restent affichés, Réessayer fonctionne.
- Photo portrait avec orientation EXIF, paysage, panorama, PNG transparent, GIF :
  comparer l’aperçu avec l’export pour rotation/miroir/cadrages/filtres/réglages.
- Tester les cadrages aux quatre bords et le zoom maximal ; réinitialiser et comparer l’original.
- Ouvrir la copie dans un autre lecteur ; vérifier dimensions, transparence et original intact.
- Vidéos MP4 H.264, HEVC, portrait, avec/sans audio : couper hors image clé,
  vérifier durée, première/dernière image, rotation et silence effectif de la copie.
- Rotation de l’appareil pendant réglages et export ; passage en arrière-plan : l’aperçu s’arrête.
- Annuler les modifications, export avec stockage presque plein et format non pris en charge :
  vérifier message d’erreur et absence de copie partielle visible.
- Vérifier le bouton Modifier sur écran étroit et avec grande taille de police.

Ces tests appareil doivent être réalisés sur téléphone ; les tests unitaires ne valident pas les codecs matériels.
