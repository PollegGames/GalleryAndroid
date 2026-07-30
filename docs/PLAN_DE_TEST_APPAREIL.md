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
2. Ouvrir ce média dans le lecteur natif.
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
