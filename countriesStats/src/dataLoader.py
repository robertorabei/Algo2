import asyncio
import aiohttp
import re
import time
import json
import subprocess
from pathlib import Path

from countryStatsReporter import CountryStatsReporter

# --- Configuration ---
CACHE_FILE = Path("author_cache.json")
MAILTO = "wilton.da.silva@ulb.be"
MAX_CONCURRENT_REQUESTS = 10  # Limite pour ne pas être banni de OpenAlex
OUTPUT_GRAPH_DIR = "bonus_graph/"


def cleanAuthorName(name):
    """Supprime les ID DBLP (ex: Wilton Da Silva 0007 -> Wilton Da Silva)"""
    return re.sub(r"\s+\d+$", "", name).strip()


def load_cache():
    """Charge les données auteurs déjà récupérées précédemment"""
    if CACHE_FILE.exists():
        try:
            with open(CACHE_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return {}
    return {}


def save_cache(cache_data):
    """Sauvegarde le dictionnaire d'auteurs dans un fichier JSON"""
    with open(CACHE_FILE, "w", encoding="utf-8") as f:
        json.dump(cache_data, f, indent=4, ensure_ascii=False)


async def fetchAuthorCountries(session, author_name, semaphore):
    """Effectue la requête API OpenAlex avec gestion de quota et de type"""
    url = "https://api.openalex.org/authors"
    # Utilisation de search au lieu de filter pour plus de souplesse sur les noms
    params = {"search": author_name, "mailto": MAILTO}

    async with semaphore:
        try:
            async with session.get(url, params=params, timeout=15) as response:
                if response.status == 200:
                    data = await response.json()
                    results = data.get("results", [])

                    if results:
                        author_data = results[0]
                        institutions = author_data.get("last_known_institutions")
                        if institutions is None:
                            institutions = []

                        countries = [
                            inst.get("country_code")
                            for inst in institutions
                            if inst and inst.get("country_code")
                        ]

                        if not countries and author_data.get("affiliations"):
                            for aff in author_data["affiliations"]:
                                inst = aff.get("institution", {})
                                code = inst.get("country_code")
                                if code:
                                    countries.append(code)

                        return author_name, list(
                            set(countries)
                        )  # set() pour éviter les doublons

                elif response.status == 429:
                    print(f"Quota dépassé pour {author_name}")

                return author_name, []
        except Exception as e:
            print(f"Erreur pour {author_name}: {e}")
            return author_name, []


class DataLoader:
    def __init__(self, script_name):
        base_path = Path(__file__).resolve().parent
        self.cmd_path = (base_path / ".." / ".." / script_name).resolve()
        self.buffer = ""
        self.data = []  # Liste d'objets CountryStatsReporter
        self.cache = load_cache()

    def loadOutputStream(self) -> None:
        """Lance le programme Java et récupère son flux de sortie"""
        if not self.cmd_path.exists():
            print(f"Erreur : Le fichier {self.cmd_path} est introuvable.")
            return

        print("Lancement du programme Java...")
        process = subprocess.Popen(
            ["sh", str(self.cmd_path)],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )

        if process.stdout:
            for line in process.stdout:
                self.buffer += line

        process.wait()
        print("Données Java récupérées. Début de l'analyse géographique...")

        # Lancement de la partie asynchrone
        asyncio.run(self.countryStatsParserAsync())

    async def countryStatsParserAsync(self):
        """Parse le buffer, récupère les pays manquants et génère les graphs"""

        # Extraction des rangs et membres par Regex
        pattern = r"Rank (\d+).*?\nMembres: (.*?)(?=\nRank|\Z)"
        matches = re.findall(pattern, self.buffer, re.DOTALL)

        all_ranks_info = []
        authors_to_query = set()

        for rank_str, members in matches:
            rank_idx = int(rank_str)
            # On initialise les reporters pour chaque rank
            while len(self.data) < rank_idx:
                self.data.append(CountryStatsReporter())

            # Nettoyage des noms d'auteurs
            author_list = [cleanAuthorName(a) for a in members.split(",") if a.strip()]
            all_ranks_info.append((rank_idx, author_list))

            # On identifie les auteurs qui ne sont pas encore dans notre CACHE
            for auth in author_list:
                if auth not in self.cache:
                    authors_to_query.add(auth)

        # Récupération des données manquantes via l'API
        if authors_to_query:
            print(
                f"🔍 {len(authors_to_query)} nouveaux auteurs à identifier sur OpenAlex..."
            )
            semaphore = asyncio.Semaphore(MAX_CONCURRENT_REQUESTS)

            async with aiohttp.ClientSession() as session:
                tasks = [
                    fetchAuthorCountries(session, auth, semaphore)
                    for auth in authors_to_query
                ]
                results = await asyncio.gather(*tasks)

                # Mise à jour du cache
                for name, countries in results:
                    self.cache[name] = countries

                # Sauvegarde sur le disque pour la prochaine fois
                save_cache(self.cache)
        else:
            print("INFO: Tous les auteurs sont déjà connus en cache local.")

        #  Remplissage des statistiques et Génération des visuels
        print("Génération des statistiques...")

        Path(OUTPUT_GRAPH_DIR).mkdir(parents=True, exist_ok=True)
        for rank, authors in all_ranks_info:
            reporter = self.data[rank - 1]
            for auth in authors:
                countries = self.cache.get(auth, [])
                # print(f"  -> Auteur: {auth} | Pays trouvés: {countries}")

                for code in countries:
                    reporter.updateCountry(code)

            reporter.generatePieChart(rank, OUTPUT_GRAPH_DIR)

        print(f"Analyse terminée. Les graphiques sont dans : {OUTPUT_GRAPH_DIR}")
