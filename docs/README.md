# FlowBerry

FlowBerry est une application Android de suivi du cycle menstruel, des symptômes et des estimations de phases.

L’application est pensée pour rester simple, lisible et locale :

- suivi des règles
- suivi des symptômes
- calendrier du cycle
- estimations des phases
- rappels locaux
- export et import des données

## Confidentialité

FlowBerry fonctionne principalement en local sur l’appareil.

Par défaut :

- pas de compte utilisateur
- pas de backend de synchronisation
- pas de collecte distante visible dans le projet
- données enregistrées localement sur le téléphone

La politique de confidentialité web est disponible ici :

- `privacy-policy.html`

## Fonctionnalités principales

### Calendrier

- affichage des règles déclarées
- affichage des règles estimées
- affichage des phases du cycle
- indication des symptômes liés aux jours sélectionnés

### Symptômes

- ajout de symptômes personnalisés
- types de symptômes par défaut
- intensité et notes
- anticipation de certains symptômes selon l’historique

### Données

- stockage local avec Room
- export de sauvegarde
- import de sauvegarde

### Personnalisation

- français et anglais
- thème
- affichage configurable de certaines phases

## Technique

- Android natif
- Java
- Room
- Material Components
- MaterialCalendarView

## Build

### Debug

```powershell
.\gradlew.bat assembleDebug
```

### Bundle Play Store

```powershell
.\gradlew.bat bundleRelease
```

Le bundle de release est généralement généré ici :

```text
app\build\outputs\bundle\release\app-release.aab
```

## Publication web de la privacy policy

Si tu veux publier la politique de confidentialité sur GitHub Pages :

1. push le repo sur GitHub
2. ouvre `Settings > Pages`
3. choisis :
   - `Deploy from a branch`
   - branche `main`
   - dossier `/docs`

L’URL publique sera ensuite du type :

```text
https://TON-UTILISATEUR.github.io/TON-REPO/privacy-policy.html
```

## Statut

FlowBerry est un outil de suivi personnel.

L’application n’est pas un dispositif médical et ne remplace pas l’avis d’un professionnel de santé.
