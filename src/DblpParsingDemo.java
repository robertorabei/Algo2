import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Usage:
 *   java -Xmx2g DblpParsingDemo <dblp.xml|dblp.xml.gz> <dblp.dtd> [--limit=1000000]
 */
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
            if (a.startsWith("--limit=")) limit = Long.parseLong(a.substring("--limit=".length()));
        }

        if (!Files.exists(xmlPath)) throw new FileNotFoundException("XML introuvable: " + xmlPath);
        if (!Files.exists(dtdPath)) throw new FileNotFoundException("DTD introuvable: " + dtdPath);

        // --------------------------------------------------------------------
        // IMPORTANT : limites d'expansion d'entités XML
        // --------------------------------------------------------------------
        // DBLP utilise un DTD qui définit beaucoup d'entités.
        // Le parseur XML de Java impose par défaut une limite sur le nombre
        // d'expansions d'entités pour se protéger d'attaques (type "Billion Laughs").
        //
        // Sur DBLP (fichier légitime), on dépasse souvent la limite par défaut (p.ex. 2500),
        // ce qui déclenche une erreur du type:
        //   JAXP00010001: The parser has encountered more than "2500" entity expansions...
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
        if (limit != Long.MAX_VALUE) System.out.println("Limit: " + limit);

        long pubCount = 0;
        Map<String, Integer> authorToId = new HashMap<>();
        int nextId = 0;
        AuthorUnionFind uf = new AuthorUnionFind(100000);

        try (DblpPublicationGenerator gen = new DblpPublicationGenerator(xmlPath, dtdPath, 256)) {
            while (pubCount < limit) {
                Optional<DblpPublicationGenerator.Publication> opt = gen.nextPublication();
                if (opt.isEmpty()) break; 

                pubCount++;
                DblpPublicationGenerator.Publication p = opt.get();

                List<String> authors = p.authors;
                // On ignore les publications sans auteurs
                if (authors == null || authors.isEmpty()) {
                    continue;
                }

                for (String name : authors) {
                    if (!authorToId.containsKey(name)) {
                        int id = nextId++;
                        authorToId.put(name, id);
                        uf.addAuthor(id); 
                    }
                }

                int firstId = authorToId.get(authors.get(0));
                for (int i = 1; i < authors.size(); i++) {
                    uf.union(firstId, authorToId.get(authors.get(i)));
                }
            }

            System.out.println("Nombre d'auteurs uniques : " + authorToId.size());
            System.out.println("Nombre de communautés isolées : " + uf.getCount());
        }
    }
}