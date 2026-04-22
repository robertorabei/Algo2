import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DblpParsingDemo {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("""
                    Usage:
                      java -Xmx2g DblpParsingDemo <dblp.xml|dblp.xml.gz> <dblp.dtd> [--limit=1000000]

                    Exemple:
                      java -Xmx2g DblpParsingDemo dblp.xml.gz dblp.dtd --limit=500000
                    """);
            System.exit(2);
        }

        Path xmlPath = Paths.get(args[0]);
        Path dtdPath = Paths.get(args[1]);

        long limit = Long.MAX_VALUE;
        for (int i = 2; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--limit="))
                limit = Long.parseLong(a.substring("--limit=".length()));
        }

        if (!Files.exists(xmlPath))
            throw new FileNotFoundException("XML introuvable: " + xmlPath);
        if (!Files.exists(dtdPath))
            throw new FileNotFoundException("DTD introuvable: " + dtdPath);

        // --------------------------------------------------------------------
        // IMPORTANT : limites d'expansion d'entités XML
        // --------------------------------------------------------------------
        // DBLP utilise un DTD qui définit beaucoup d'entités.
        // Le parseur XML de Java impose par défaut une limite sur le nombre
        // d'expansions d'entités pour se protéger d'attaques (type "Billion Laughs").
        //
        // Sur DBLP (fichier légitime), on dépasse souvent la limite par défaut (p.ex.
        // 2500),
        // ce qui déclenche une erreur du type:
        // JAXP00010001: The parser has encountered more than "2500" entity
        // expansions...
        //
        // Ici, comme on parse un fichier connu + un DTD local (pas de réseau),
        // on désactive ces limites pour éviter l'erreur.
        //
        // À ne pas faire pour des XML non fiables.
        // --------------------------------------------------------------------
        System.setProperty("jdk.xml.entityExpansionLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0");
        System.setProperty("jdk.xml.maxParameterEntitySizeLimit", "0");

        System.out.println("XML: " + xmlPath);
        System.out.println("DTD: " + dtdPath);
        if (limit != Long.MAX_VALUE)
            System.out.println("Limit: " + limit);

        // --- 2. Structures de données ---
        Map<String, Integer> authorToId = new HashMap<>();
        Map<Integer, String> idToAuthor = new HashMap<>(); // Pour afficher les noms à la fin
        int nextId = 0;

        // Tâche 1 : Union-Find
        AuthorUnionFind uf = new AuthorUnionFind(1024);

        // Tâche 2 : Comptage paires orientées (Online)
        // ID_A -> {ID_B -> Compteur}
        Map<Integer, Map<Integer, Integer>> edgeCounts = new HashMap<>();

        System.out.println("Démarrage du parsing...");

        // --- 3. Boucle de Parsing (Online) ---
        try (DblpPublicationGenerator gen = new DblpPublicationGenerator(xmlPath, dtdPath, 512)) {
            long pubCount = 0;
            while (pubCount < limit) {
                Optional<DblpPublicationGenerator.Publication> opt = gen.nextPublication();
                if (opt.isEmpty())
                    break;

                pubCount++;
                DblpPublicationGenerator.Publication p = opt.get();
                List<String> authors = p.authors;

                if (authors == null || authors.isEmpty())
                    continue;

                // A. Enregistrement des auteurs (Indispensable avant la suite)
                int[] ids = new int[authors.size()];
                for (int i = 0; i < authors.size(); i++) {
                    String name = authors.get(i);
                    if (!authorToId.containsKey(name)) {
                        authorToId.put(name, nextId);
                        idToAuthor.put(nextId, name);
                        uf.addAuthor(nextId);
                        nextId++;
                    }
                    ids[i] = authorToId.get(name);
                }

                // B. Tâche 1 : Union-Find (Non-orienté)
                int firstId = ids[0];
                for (int i = 1; i < ids.length; i++) {
                    uf.union(firstId, ids[i]);
                }

                // C. Tâche 2 : Comptage A -> B (Online)
                if (ids.length >= 2) {
                    Map<Integer, Integer> neighbors = edgeCounts.computeIfAbsent(firstId, k -> new HashMap<>());
                    for (int i = 1; i < ids.length; i++) {
                        int idB = ids[i];
                        neighbors.put(idB, neighbors.getOrDefault(idB, 0) + 1);
                    }
                }

                // Exigence Online : Affichage tous les 100 000
                if (pubCount % 100000 == 0) {
                    System.out.println("\n--- État à " + pubCount + " publications ---");
                    System.out.println("Communautés (UF) : " + uf.getCount());
                    System.out.println("Top 10 tailles : " + uf.getTopCommunitySizes(10));
                }
            }
        }

        System.out.println("\nParsing terminé. Début du traitement Offline...");

        // --- 4. Tâche 2 : Filtrage et Graphe Orienté (Offline) ---
        CollaborationGraph graph = new CollaborationGraph();
        for (var entryA : edgeCounts.entrySet()) {
            int u = entryA.getKey();
            for (var entryB : entryA.getValue().entrySet()) {
                if (entryB.getValue() >= 6) { // SEUIL 6
                    graph.addFilteredEdge(u, entryB.getKey());
                }
            }
        }
        edgeCounts = null; // Libère la mémoire

        // --- 5. Tâche 2 : Calcul des CFC (Communautés Orientées) ---
        List<List<Integer>> sccs = graph.findSCCs(nextId);
        sccs.sort((a, b) -> Integer.compare(b.size(), a.size())); // Trier par taille

        // --- 6. Sorties finales ---

        // Histogramme Tâche 1 (UF)
        saveHistogram(uf.getHistogram(), "histogramme_tache1.txt");

        // Histogramme Tâche 2 (CFC)
        Map<Integer, Integer> sccHist = new TreeMap<>();
        for (List<Integer> scc : sccs)
            sccHist.put(scc.size(), sccHist.getOrDefault(scc.size(), 0) + 1);
        saveHistogram(sccHist, "histogramme_tache2.txt");

        // Top 10 Communautés Orientées
        System.out.println("\nTOP 10 COMMUNAUTÉS ORIENTÉES (Filtrage >= 6) :");
        for (int i = 0; i < Math.min(10, sccs.size()); i++) {
            List<Integer> community = sccs.get(i);
            int diameter = graph.calculateDiameter(community);

            System.out.println("\nRank " + (i + 1) + " | Taille: " + community.size() + " | Diamètre: " + diameter);
            System.out.print("Membres: ");
            for (int id : community)
                System.out.print(idToAuthor.get(id) + ", ");
            System.out.println();
        }
    }

    private static void saveHistogram(Map<Integer, Integer> hist, String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(filename)) {
            writer.println("Taille;Nombre");
            hist.forEach((k, v) -> writer.println(k + ";" + v));
        }
        System.out.println("Histogramme sauvegardé : " + filename);
    }
}
