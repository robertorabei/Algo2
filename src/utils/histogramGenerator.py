import pandas as pd
import matplotlib.pyplot as plt
import os


def generate_binned_histogram(filename, title_prefix, color, output_png):
    if not os.path.exists(filename):
        print(f"Fichier {filename} non trouvé.")
        return

    #  Lecture des données
    df = pd.read_csv(filename, sep=";")

    # Définition des tranches de tailles
    # On définit des paliers pour capturer les petites et les très grandes communautés
    bins = [0, 1, 2, 5, 10, 50, 100, 1000, float("inf")]
    labels = ["1", "2", "3-5", "6-10", "11-50", "51-100", "101-1k", ">1k"]

    # Création de la colonne Categorie
    df["Categorie"] = pd.cut(df["Taille"], bins=bins, labels=labels)

    # Agrégation des données
    # On somme le "Nombre" de communautés pour chaque tranche
    df_grouped = df.groupby("Categorie", observed=True).agg({"Nombre": "sum"})

    # Préparation des données pour Matplotlib
    cat_labels = [str(x) for x in df_grouped.index]
    counts = df_grouped["Nombre"].values

    # Création du graphique
    plt.figure(figsize=(10, 6))
    bars = plt.bar(cat_labels, counts, color=color, edgecolor="black", alpha=0.8)

    # Configuration des axes
    plt.title(f"{title_prefix} - Distribution des tailles")
    plt.xlabel("Tranches de taille (Nombre d'auteurs)")
    plt.ylabel("Nombre de communautés")

    # Ajout des valeurs au-dessus des barres pour la précision du rapport
    for bar in bars:
        height = bar.get_height()
        if height > 0:
            plt.text(
                bar.get_x() + bar.get_width() / 2.0,
                height,
                f"{int(height)}",
                ha="center",
                va="bottom",
                fontsize=9,
            )

    plt.grid(axis="y", linestyle="--", alpha=0.4)
    plt.tight_layout()

    # Sauvegarde
    plt.savefig(output_png)
    plt.close()
    print(f"Histogramme généré : {output_png}")


# --- Exécution ---

# Tâche 1 : Graphe non orienté
generate_binned_histogram(
    "histogramme_tache1.csv",
    "Tâche 1 (Co-publication)",
    "skyblue",
    "histogramme_t1_categories.png",
)

# Tâche 2 : Graphe orienté filtré
generate_binned_histogram(
    "histogramme_tache2.csv",
    "Tâche 2 (Orienté Filtré)",
    "salmon",
    "histogramme_t2_categories.png",
)
