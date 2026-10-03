# PERF

Application Android personnelle de suivi des performances de musculation (Samsung Galaxy S25).
Kotlin, Jetpack Compose, Room, 100 % locale, sans compte ni serveur.

## Fonctions

- Exercices : liste avec, pour chacun, le 1RM et le poids pour 5 reps (90 derniers jours, réels ou estimés).
- Fiche exercice : en-tête 1RM / 5 reps, courbe charge (1RM réel + 1RM estimé) ou reps (à une charge choisie), historique modifiable.
- Saisie : date du jour par défaut, modifiable (pas de date future), trois modes (1RM, reps avec charge, reps sans charge).
  Barre : on saisit le total des disques, affichage « 20 + 65 = 85 kg ».
- Journal : toutes les saisies par date, un appui pour corriger une séance passée.
- Poids de corps : pesées datées, courbe avec moyenne glissante sur 7 jours.
- Export ZIP (exercices.csv, performances.csv, poids_corps.csv) et import CSV ou ZIP (menu ⋮ de l'accueil).

## Calculs

- 1RM estimé (Epley) : `charge × (1 + reps / 30)`, pour reps ≥ 2. Poids du corps : calcul sur (PDC + lest), résultat en lest.
- Poids pour 5 reps estimé : `1RM × 30 / 35`, arrondi à l'inférieur au 2,5 kg (barre) ou au 0,5 kg (lest).
- En-tête : meilleure valeur des 90 derniers jours. La valeur réelle est affichée, sauf si l'estimation est plus haute :
  l'estimation est alors affichée (« estimé ») avec la valeur réelle en dessous.
- PDC : dernière pesée à la date de la saisie, sinon la première pesée, sinon 80 kg.

Le code est dans `domain/Calculs.kt`, testé par `CalculsTest` (`./gradlew test`).

## Données pré-intégrées

Au premier lancement (création de la base), l'app charge `app/src/main/assets/seed/*.csv` :
12 exercices et 23 performances (19 au 30 septembre 2026). Pour pré-intégrer des pesées, ajouter
`poids_corps.csv` (`id,date,poids_kg`) dans ce dossier.

Le pré-remplissage ne s'exécute qu'à la création de la base. Pour le rejouer : désinstaller l'app,
ou effacer ses données (Paramètres > Applications > PERF > Stockage).

## Construire

1. Ouvrir le dossier dans Android Studio (Ladybug 2024.2 ou plus récent), laisser Gradle se synchroniser.
2. Brancher le S25 (débogage USB activé) et lancer `app`.

Versions : AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12.01, Room 2.6.1, Gradle 8.11.1, minSdk 26, targetSdk 35.
Android Studio peut proposer des mises à jour de versions : elles sont facultatives.

## Structure

```
app/src/main/java/fr/jeff/perf/
  data/        Entités Room, DAO, base, pré-remplissage, CSV, dépôt (import/export)
  domain/      Calculs (Epley, 5 reps, 90 jours, PDC) et formats d'affichage
  ui/          Thème, navigation, composants (courbe Canvas, cartes, champs), écrans
```
