import java.util.*;

/**
 * Structure de données Union-Find optimisée pour 
 * gérer les communautés d'auteurs (Tâche 1 : Graphe non orienté).
 * Elle permet de regrouper efficacement les co-auteurs et de maintenir
 * la taille et le nombre de communautés de manière Online.
 */
public class AuthorUnionFind {
    
    // Tableau stockant le parent de chaque nœud (représentant de la communauté).
    private int[] parent;
    
    // Tableau stockant la taille de la communauté pour chaque nœud racine.
    private int[] size;
    
    // Nombre total de communautés disjointes actives.
    private int count;
    
    // Ensemble contenant tous les identifiants d'auteurs ayant été ajoutés.
    private Set<Integer> activeAuthors;

    /**
     * Constructeur initialisant les structures de base.
     *
     * @param initialCapacity La capacité initiale estimée (nombre d'auteurs).
     * Les tableaux s'agrandiront dynamiquement si nécessaire.
     */
    public AuthorUnionFind(int initialCapacity) {
        parent = new int[initialCapacity];
        size = new int[initialCapacity];
        count = 0;
        activeAuthors = new HashSet<>();
    }

    /**
     * Ajoute un nouvel auteur dans la structure en tant que communauté de taille 1.
     * Si l'auteur existe déjà, la méthode ne fait rien pour éviter les doublons.
     *
     * @param id L'identifiant unique de l'auteur à ajouter.
     */
    public void addAuthor(int id) {
        ensureCapacity(id);
        if (size[id] != 0) return;

        parent[id] = id;
        size[id] = 1;
        activeAuthors.add(id);
        count++; 
    }

    /**
     * Vérifie si les tableaux parent et size sont assez grands pour contenir
     * le nouvel identifiant. Si ce n'est pas le cas, ils sont redimensionnés.
     *
     * @param id L'identifiant qui doit être inséré.
     */
    private void ensureCapacity(int id) {
        if (id >= parent.length) {
            // Double la taille du tableau ou l'ajuste à l'ID + 1 au minimum
            int newSize = Math.max(id + 1, parent.length * 2); 
            parent = Arrays.copyOf(parent, newSize);
            size = Arrays.copyOf(size, newSize);
        }
    }

    /**
     * Trouve le représentant de la communauté à laquelle appartient
     * l'auteur 'i'. Applique l'optimisation de "compression de chemin" pour
     * accélérer les futures recherches.
     *
     * @param i L'identifiant de l'auteur.
     * @return L'identifiant du représentant de sa communauté.
     */
    public int find(int i) {
        if (parent[i] != i) {
            parent[i] = find(parent[i]); 
        }
        return parent[i];
    }

    /**
     * Fusionne les communautés de deux auteurs.
     * Applique l'optimisation "d'union par la taille" pour attacher
     * l'arbre le plus petit sous la racine de l'arbre le plus grand.
     *
     * @param i L'identifiant du premier auteur.
     * @param j L'identifiant du deuxième auteur.
     */
    public void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);

        if (rootI == rootJ) return;

        // Union par la taille : le plus grand absorbe le plus petit
        if (size[rootI] < size[rootJ]) {
            parent[rootI] = rootJ;
            size[rootJ] += size[rootI];
        } else {
            parent[rootJ] = rootI;
            size[rootI] += size[rootJ];
        }

        count--;
    }

    /**
     * Renvoie le nombre actuel de communautés disjointes.
     *
     * @return Le nombre de communautés.
     */
    public int getCount() {
        return count;
    }

    /**
     * Récupère la taille des 'limit' plus grandes communautés.
     *
     * @param limit Le nombre maximum de tailles à renvoyer.
     * @return Une liste contenant les tailles triées par ordre décroissant.
     */
    public List<Integer> getTopCommunitySizes(int limit) {
        List<Integer> sizes = new ArrayList<>();

        // On parcourt tous les auteurs connus
        for (int i : activeAuthors) {
            // Un nœud est racine s'il est son propre parent (représente la communauté complète)
            if (parent[i] == i) {
                sizes.add(size[i]);
            }
        }

        // Tri décroissant pour avoir les plus grandes en premier
        sizes.sort(Collections.reverseOrder());
        return new ArrayList<>(sizes.subList(0, Math.min(limit, sizes.size())));
    }

    /**
     * Génère un histogramme de la distribution des tailles des communautés.
     * Utilisé à la fin du traitement (Offline) pour les statistiques.
     *
     * @return Une Map liant [Taille de la communauté] -> [Nombre de communautés de cette taille].
     */
    public Map<Integer, Integer> getHistogram() {
        Map<Integer, Integer> hist = new TreeMap<>();

        for (int i : activeAuthors) {
            // On ne compte que les racines pour éviter de compter plusieurs fois une même communauté
            if (parent[i] == i) {
                int s = size[i];
                hist.put(s, hist.getOrDefault(s, 0) + 1);
            }
        }

        return hist;
    }
}