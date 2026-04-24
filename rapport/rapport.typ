#set page(
  paper: "a4",
  margin: (x: 2.5cm, y: 2.5cm),
  header: align(right, text(8pt, gray)[INFO-F-203 - Algorithmique 2]),
  footer: [
    #line(length: 100%, stroke: 0.5pt + gray)
  ],
)

#set text(
  font: "New Computer Modern",
  size: 11pt,
  lang: "fr",
)

#set heading(numbering: "1.1.")

#align(center)[
  #block(spacing: 2em)[
    #text(16pt, weight: "bold")[Université Libre de Bruxelles] \
    #text(13pt)[Faculté des Sciences | Département d'Informatique]
  ]

  #v(5em)
  #text(20pt, weight: "bold")[Rapport de Projet : Détection de communautés dans DBLP]
  #v(2em)

  #grid(
    columns: (1fr, 1fr),
    align: center,
    [
      *Étudiant 1* \
      Wilton Da Silva \
      Matricule : 000589652
    ],
    [
      *Étudiant 2* \
      Roberto Rabei \
      Matricule : 000593814
    ],
  )

  #v(5em)
  #text(12pt)[Année académique 2025-2026]
]

#pagebreak()

#outline(indent: auto)
#pagebreak()

= Introduction
Ce projet vise à analyser le réseau de co-publications scientifiques DBLP en extrayant des structures communautaires via deux approches algorithmiques : l'Union-Find pour les relations simples et l'algorithme de Tarjan pour les collaborations fortes.

= Méthodologie et Structures de Données

== Tâche 1 : Co-publications (Graphe non-orienté)
Pour cette tâche, nous devons identifier les composantes connexes dans un flux continu de données.

=== Optimisation (Union-Find)
Nous avons privilégié une structure *Union-Find* (Disjoint Set Union) plutôt qu'une liste d'adjacence pour deux raisons majeures :

- *Efficacité mémoire :* Un graphe complet pour une publication de $k$ auteurs nécessite $k(k-1)/2$ arêtes. L'Union-Find ne nécessite que $k-1$ opérations d'union, évitant la saturation de la RAM.

- *Traitement Online :* Contrairement à un BFS/DFS qui nécessite de reparcourir le graphe ($O(V+E)$) à chaque requête, l'Union-Find maintient les métriques (nombre de composantes, tailles) de manière active durant l'insertion. Les métriques globales sont donc accessibles en $O(1)$ à tout instant.

== Tâche 2 : Relations fortes (Graphe orienté)
Ici, nous isolons les noyaux de collaboration stable en filtrant les arêtes et en utilisant un graphe orienté.

=== Approche Mixte : Online et Offline
1. *Phase Online :* Conformément à l'énoncé, nous comptabilisons uniquement les paires orientées du premier auteur vers ses co-auteurs ($A \to B$). Nous utilisons une table de hachage imbriquée `HashMap<Integer, HashMap<Integer, Integer>>`. Cette approche ciblée maintient une complexité spatiale par publication de $O(k)$ (où $k$ est le nombre d'auteurs). Elle évite une explosion combinatoire en $O(k^2)$ qui saturerait la mémoire de la JVM (OutOfMemoryError) lors du traitement de publications massives (hyper-authorship).
2. *Phase Offline :* Une fois le parsing terminé, nous appliquons le seuil exigé en ne conservant que les arêtes de poids $\ge 6$. Lors de la construction de la liste d'adjacence finale, nous utilisons un ensemble `activeNodes` (Set) pour ne mémoriser que les auteurs ayant survécu à ce filtrage strict.

=== Analyse des Composantes Fortement Connexes (Tarjan)
Pour extraire ces noyaux, nous utilisons le concept de *CFC* : un sous-ensemble où chaque nœud peut atteindre tous les autres (réciprocité). Nous avons implémenté l'*algorithme de Tarjan*, optimal car il identifie toutes les CFC en un seul parcours DFS ($O(V+E)$) grâce aux concepts d'index de découverte et de `low-link`. Optimisation majeure : Pour garantir des performances optimales et un décompte exact, l'algorithme n'initie sa recherche que sur l'ensemble des activeNodes. Cela nous évite d'exécuter l'algorithme sur l'entièreté des ~4 millions d'auteurs isolés de DBLP et fausser les statistiques. Enfin, chaque composante identifiée, y compris les auteurs de taille 1 ayant survécu au filtrage, est dûment comptabilisée.

= Résultats et Analyse

== Statistiques du jeu de données
- *Nombre d'auteurs identifiés :* 7 995 940
- *Nombre de publications traitées :* 4 031 750

== Comparaison des structures communautaires
#figure(
  grid(
    columns: (1fr, 1fr),
    gutter: 1em,
    image("histogramme_t1_categories.png", width: 100%), image("histogramme_t2_categories.png", width: 100%),
  ),
  caption: [Comparaison des distributions : Tâche 1 (gauche) et Tâche 2 (droite).],
)

La Tâche 1 est dominée par une composante géante due à la permissivité du lien. En revanche, la Tâche 2 fragmente le réseau en noyaux denses, illustrant la transition d'un réseau de "connaissance" vers un réseau de "collaboration stable".

== Analyse des communautés orientées (Top 5)
Nous avons calculé le diamètre des plus grandes CFC pour évaluer leur cohésion interne.

#table(
  columns: (1fr, 1fr, 1fr),
  inset: 7pt,
  stroke: 0.5pt + gray,
  fill: (x, y) => if y == 0 { silver } else { white },
  [*Rang*], [*Taille*], [*Diamètre*],
  [1], [133], [30],
  [2], [73], [35],
  [3], [38], [13],
  [4], [34], [7],
  [5], [33], [6],
)

- *Modèle "Petit Monde" (Rang 5) :* Un diamètre très faible (6) pour 33 membres indique un noyau très dense, typique d'un laboratoire de recherche où les membres sont presque tous co-auteurs directs.
- *Modèle "Réseau étendu" (Rang 1) :* Un diamètre de 30 suggère une structure plus linéaire, comme une chaîne de collaborations successives s'étendant sur plusieurs institutions.

== Analyse géographique (Bonus)

#figure(
  grid(
    columns: (1fr, 1fr),
    gutter: 1em,
    image("bonus_graph/rank_1_countries.png", width: 100%), image("bonus_graph/rank_5_countries.png", width: 100%),
  ),
  caption: [Distribution par pays pour les communautés de Rang 1 et Rang 5.],
)


En enrichissant nos données via l'API *OpenAlex*, nous avons analysé l'origine des auteurs du Top 10. Les résultats montrent une forte corrélation entre la structure du graphe et la géographie : les noyaux les plus denses (diamètre faible) sont souvent localisés dans un même pays, tandis que les réseaux étendus (diamètre élevé) reflètent des collaborations transcontinentales.

= Conclusion

Ce projet nous a permis de confronter les théories des graphes à une volumétrie réelle. La gestion du flux (Online) a imposé des choix de structures de données performantes (`HashMap`, `Union-Find`). L'analyse finale prouve que le filtrage par le poids des arêtes et la forte connexité sont des outils puissants pour extraire du sens d'un réseau social complexe.

= Mentions relatives à l'IA

- *Aide à l'interprétation sémantique :* L'IA nous a aidés à faire le pont entre les métriques algorithmiques (diamètre, taille de CFC) et la réalité du monde académique.
Elle a permis de valider que nos "noyaux denses" correspondaient logiquement à des laboratoires spécialisés et nos "chaînes étendues" à des réseaux de collaborations interdisciplinaires ou historiques.

- *Structure du rapport :*  La structure du rapport a été optimisée par l'IA pour garantir que nos explications s'intègrent parfaitement dans le cadre de la limite de pages.
