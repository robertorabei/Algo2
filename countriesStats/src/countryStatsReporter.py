import matplotlib.pyplot as plt
from matplotlib.patches import Circle
import pycountry
from typing import Any


class CountryStatsReporter:
    def __init__(self) -> None:
        # Stocke les données sous forme { "FR": 10, "US": 5 }
        self.data: dict[str, int] = {}

    def updateCountry(self, keyCountry: str) -> None:
        """Incrémente le compteur pour un pays donné."""
        self.data.setdefault(keyCountry, 0)
        self.data[keyCountry] += 1

    def get_full_name(self, code: str) -> str:
        """Convertit un code ISO (ex: 'FR') en nom complet ('France')."""
        if code == "Autres":
            return "Autres"
        country = pycountry.countries.get(alpha_2=code)
        return country.name if country else code

    def generatePieChart(self, rank: int, output_path: str) -> None:
        """Génère un graphique en donut avec légende complète à droite."""
        if not self.data:
            print(f"Aucune donnée de pays pour le Rank {rank}")
            return

        sorted_data = sorted(self.data.items(), key=lambda x: x[1], reverse=True)
        top_n = 11
        main_countries = sorted_data[:top_n]
        others_count = sum(count for _, count in sorted_data[top_n:])

        labels = [self.get_full_name(c[0]) for c in main_countries]
        values = [c[1] for c in main_countries]

        if others_count > 0:
            labels.append("Autres")
            values.append(others_count)

        # Création de la figure
        plt.figure(figsize=(12, 7))
        colors = plt.cm.tab20.colors

        pie_results = plt.pie(
            values, autopct="%1.1f%%", startangle=140, colors=colors, pctdistance=0.82
        )

        wedges = pie_results[0]
        autotexts = pie_results[2]

        plt.setp(autotexts, size=9, weight="bold", color="black")

        # Ajout de la légende
        plt.legend(
            wedges,
            labels,
            title="Pays",
            loc="center left",
            bbox_to_anchor=(1, 0, 0.5, 1),
            fontsize=10,
        )

        # Dessin du cercle central (Donut)
        centre_circle = Circle((0, 0), 0.70, fc="white")
        plt.gca().add_artist(centre_circle)

        plt.title(
            f"Répartition géographique - Communauté Rank {rank}", pad=20, fontsize=14
        )

        # Sauvegarde avec ajustement automatique pour ne pas couper la légende
        save_name = f"{output_path}/rank_{rank}_countries.png"
        plt.savefig(save_name, bbox_inches="tight")
        plt.close()
        print(f"Graphique généré avec succès : {save_name}")

    def printDic(self) -> None:
        for key, value in self.data.items():
            print(f"{key}: {value}")
