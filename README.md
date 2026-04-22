# Détection de communautés DBLP

## 1. Dépendances (Partie Bonus)

Les bibliothèques Python suivantes sont nécessaires pour la récupération des données via l'API OpenAlex et la génération des graphiques de la tâche bonus:

- **pandas** : Pour la manipulation et le tri des données statistiques.
- **matplotlib** : Pour la génération des graphiques en donut illustrant la répartition par pays.
- **aiohttp** : Pour effectuer des requêtes asynchrones vers l'API OpenAlex afin d'identifier les institutions des auteurs.
- **pycountry** : Pour convertir les codes pays ISO Alpha-2 en noms complets.

## 2. Installation

Sur le système Linux, nous recommandons l'utilisation d'un environnement virtuel pour isoler les dépendances :

```bash
# Création et activation de l'environnement virtuel
python3 -m venv .venv
source .venv/bin/activate

# Installation des dépendances
pip install -r requirements.txt

```

## 3. Exécution

### Tache 1 et 2 (java)

Concernant cette partie la compilation et l'exécution est géré par le script **build_n_run.sh**.
Donc il faut s'assurer d'avoir les permissions pour exécuter le script.

```bash
chmod +x build_n_run.sh
./build_n_run.sh
```

### Partie Bonus (python)

Le script python exécute le code java pour capturer son flux de sortie (stdout) pour capturer tout les auteurs des 10 communautés.
Donc il faut s'assurer d'être dans la racine du projet et exécuter.

```bash
python3 countriesStats/src/main.py
```

## Livrables et Sorties

- **Rapport** : Un fichier PDF décrivant les algorithmes, les structures de données et la complexité.
- **Graphiques** : Les histogrammes de la _tache 1_ et la _tache 2_ seront générée en .png dans la racine du projet, et les images .png concernant la partie bonus sont générées dans le dossier bonus_graph/ (il sera créer au lancement du script).
- **Cache** : Pour la partie bonus un fichier author_cache.json est créé pour stocker les résultats d'OpenAlex et limiter les requêtes réseau.
