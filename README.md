# 5 Rois

Le jeu de cartes **5 Rois** pour Android, à jouer seul contre 1 à 3 adversaires contrôlés par l'ordinateur.
Écrit en Kotlin avec Jetpack Compose.

## Règles (version de l'app)

- Deux paquets mélangés : 5 couleurs (♠ ♥ ♣ ♦ ★) de 3 à Roi, plus 6 jokers (116 cartes).
- 11 manches. On reçoit 3 cartes à la manche 1, 4 à la manche 2, ... 13 à la manche 11.
- **Cartes sauvages** : les jokers et les cartes du rang de la manche (3 à la manche 1, 4 à la 2, ..., Valet à la 9, Dame à la 10, Roi à la 11). Elles remplacent n'importe quelle carte.
- À son tour : on pioche (pioche ou dessus de la défausse), puis on défausse une carte.
- **Combinaisons** : brelan / carré (3 cartes ou plus de même valeur) ou suite (3 cartes ou plus qui se suivent, même couleur, sans passer du Roi au 3).
- **Sortir** : quand toutes les cartes sauf celle défaussée sont dans des combinaisons. Les autres jouent un dernier tour.
- **Points** : les cartes hors combinaison comptent leur valeur (3 à 10), Valet 11, Dame 12, Roi 13, joker 50. Une carte sauvage restante compte sa valeur. Le moins de points gagne après 11 manches.

L'app regroupe automatiquement votre main en combinaisons (cartes soulignées de la même couleur) et indique les points restants. Le bouton « Groupes » coupe cette aide.

## Construire l'APK

Avec Android Studio : ouvrir le dossier, laisser Gradle se synchroniser, puis lancer l'app sur un téléphone ou un émulateur.

En ligne de commande (JDK 17 et Android SDK requis) :

```
./gradlew assembleDebug
```

L'APK est dans `app/build/outputs/apk/debug/`.

Le workflow GitHub Actions `.github/workflows/build.yml` construit l'APK à chaque push ; il se télécharge dans l'onglet *Actions* du dépôt (artefact `cinq-rois-debug-apk`).

## Structure

- `app/src/main/java/fr/cinqrois/game/` : moteur de jeu pur Kotlin, sans dépendance Android.
  - `Card.kt` : cartes et paquet.
  - `HandSolver.kt` : cherche le meilleur rangement d'une main (brelans, suites, sauvages) pour minimiser les points.
  - `Game.kt` : manches, tours, sortie, scores.
  - `Ai.kt` : les adversaires.
- `app/src/main/java/fr/cinqrois/ui/` : écrans Compose et `GameViewModel`.

## Versions

Android Gradle Plugin 8.13.0, Kotlin 2.2.20, Compose BOM 2025.09.00, compileSdk 36, minSdk 26. Elles peuvent être mises à jour dans `build.gradle.kts` et `app/build.gradle.kts`.
