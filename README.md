# Smart Serveur Web - by Smart Solution

Application Android complète de serveur Web tout-en-un (comme KSWEB) fonctionnant **SANS ROOT** sur Android 7.0+ (API 24 à 34).

## 👨‍💻 Informations Développeur
- **Nom :** Arsène KAPEND A KALEND
- **Société :** Smart Solution
- **Téléphone / WhatsApp / Airtel Money :** +243 977 811 446
- **Site / Plateforme :** [https://smartsolution-jq7ceh5rya-nw.a.run.app](https://smartsolution-jq7ceh5rya-nw.a.run.app)
- **Basé à :** Lubumbashi / Kolwezi, RDC
- **Mission :** Créer des outils numériques qui marchent SANS INTERNET

---

## ⚡ Fonctionnalités principales (SANS ROOT)
1. **Bouton START** : lance instantanément tous les services en Foreground Service avec notification permanente :
   - **Nginx** sur le port `8080`
   - **PHP 8.2 + PHP-FPM**
   - **MariaDB** sur le port `3306` (`root` / `root123`)
   - **phpMyAdmin** sur `http://IP:8080/phpmyadmin`
   - **Serveur FTP** sur le port `2121` (`serveur` / `serveur123`)
2. **Bouton STOP** : stoppe tous les services proprement.
3. **Dossier htdocs** : `/storage/emulated/0/SmartServeurWeb/htdocs` (avec `index.php` généré au premier démarrage).
4. **Boutons intégrés** : Ouvrir phpMyAdmin, Ouvrir htdocs, Voir les logs en direct, Éditeur de code intégré, Page À propos.
5. **Résilience maximale** : Si un binaire externe n'est pas encore présent, le moteur intégré pur Kotlin prend le relais pour garantir 0 crash.

---

## 🚀 Comment compiler sur GitHub et obtenir l'APK

### Étape 1 : Créer votre dépôt sur GitHub
1. Allez sur [github.com/new](https://github.com/new).
2. Nommez le dépôt (ex: `SmartServeurWeb`).
3. Mettez-le en Public ou Privé puis créez-le.

### Étape 2 : Envoyer le code sur GitHub
Dans votre terminal local ou en important les fichiers :
```bash
git init
git add .
git commit -m "Initial commit - Smart Serveur Web v1.0.0"
git branch -M main
git remote add origin https://github.com/VOTRE_NOM/SmartServeurWeb.git
git push -u origin main
```

### Étape 3 : GitHub Actions compile automatiquement
Dès le push, GitHub détecte le fichier `.github/workflows/build.yml` :
1. Il installe Java JDK 17 (Temurin).
2. Il exécute `./gradlew assembleDebug`.
3. Il génère le fichier `app-debug.apk`.

### Étape 4 : Où télécharger l'APK finale ?
1. Rendez-vous sur votre dépôt GitHub.
2. Cliquez sur l'onglet **Actions** en haut.
3. Cliquez sur le workflow en cours ou terminé (**Build Smart Serveur Web APK**).
4. Dans la section **Artifacts** en bas de page, cliquez sur **`SmartServeurWeb-debug-apk`**.
5. Vous obtenez directement l'archive contenant votre fichier **`app-debug.apk`** prêt à installer sur votre téléphone !

---

## 🛠 Compilation en local (optionnel)
Si vous avez Android Studio ou le SDK Android en local :
```bash
chmod +x gradlew
./gradlew assembleDebug
```
L'APK se trouvera dans : `app/build/outputs/apk/debug/app-debug.apk`.
