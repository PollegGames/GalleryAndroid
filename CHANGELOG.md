## 1.1.4

- 50 médias au départ et chargement automatique à proximité du bas de la grille.
- Position conservée pendant le chargement, arrêt en fin de liste et bouton Réessayer uniquement en cas d’erreur.
- Éditeur photo intégré : recadrage, rotation, miroir, réglages et cinq styles couleur.
- Éditeur vidéo intégré : découpage temporel, rotation et suppression du son.
- Aperçu et export dans une nouvelle copie, sans modification des originaux.

# Historique des versions

## 1.1.3-test

- sources 1.1.2 intégrées définitivement sans scripts de génération pendant le build ;
- workflows génériques de test et de release, sans modification automatique des sources ;
- signature permanente commune aux APK de test et release ;
- zoom photo 1× à 5×, pan borné et double toucher 1×/2,5× ;
- priorité au pan lorsque l’image est zoomée et au pager lorsqu’elle est à 1× ;
- prise en charge de `ACTION_VIEW image/*` avec affichage direct des URI `content://` ;
- partage conservé pour les images externes et actions Move/Delete désactivées si non sûres.

## 1.1.2

- partage simple et multiple ;
- lecteur interne pour images et vidéos ;
- commandes vidéo avec masquage automatique ;
- actions Partager, Déplacer et Supprimer dans le lecteur ;
- nouvelle icône de l’application.

## 1.1.1

- explorateur de destination plein écran, organisé par stockage puis par dossier ;
- carte SD proposée comme destination lorsqu’elle est disponible ;
- déplacement des médias provenant de répertoires protégés tels que WhatsApp Images ;
- déplacement transactionnel entre le téléphone et la carte SD, sans laisser de copie ;
- filtrage des destinations incompatibles avec les règles MediaStore ;
- annulation de la destination si le transfert ou la suppression de la source échoue ;
- message d’erreur détaillé lorsqu’aucun média n’a pu être déplacé.

## 1.1.0

- navigation latérale adaptative sur téléphone et tablette ;
- noms de dossiers sur deux lignes et indicateur de défilement ;
- sélection multiple par appui long ;
- suppression confirmée par Android ;
- déplacement sans copie, limité au même volume MediaStore ;
- sélecteur de dossier de destination ;
- icône recentrée pour les lanceurs ronds ;
- tests de sélection et de mutation MediaStore.

## 1.0.0

- première version publique de la galerie MediaStore.
