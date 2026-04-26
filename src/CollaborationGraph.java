import java.util.*;

/**
 * Représente le graphe orienté des collaborations entre auteurs.
 * Cette classe permet de construire le graphe filtré, de détecter les
 * CFC via l'algorithme de Tarjan, et de calculer le diamètre de ces composantes.
 */
public class CollaborationGraph {
    
    //Liste d'adjacence représentant le graphe orienté
    private final Map<Integer, List<Integer>> adj = new HashMap<>();
    
    // Ensemble des nœuds ayant au moins une arête après le filtrage. 
    private final Set<Integer> activeNodes = new HashSet<>();

    /**
     * Ajoute une arête orientée et filtrée dans le graphe (de u vers v).
     * Marque également les nœuds u et v comme actifs pour limiter
     * la recherche de communautés aux seuls auteurs pertinents.
     *
     * @param u L'identifiant de l'auteur source.
     * @param v L'identifiant de l'auteur destination.
     */
    public void addFilteredEdge(int u, int v) {
        adj.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
        activeNodes.add(u);
        activeNodes.add(v);
    }

    // Compteur global de temps de découverte utilisé par l'algorithme de Tarjan.
    private int timer = 0;

    /**
     * Trouve toutes les CFC du graphe en utilisant l'algorithme de Tarjan.
     *  L'exploration est optimisée en ne visitant que les nœuds actifs.
     *
     * @param numNodes Le nombre total d'auteurs répertoriés dans le système.
     * @return Une liste de communautés, où chaque communauté est une liste d'identifiants d'auteurs.
     */
    public List<List<Integer>> findSCCs(int numNodes) {
        int[] disc = new int[numNodes];
        int[] low = new int[numNodes];
        boolean[] onStack = new boolean[numNodes];
        Stack<Integer> stack = new Stack<>();
        List<List<Integer>> sccs = new ArrayList<>();
        Arrays.fill(disc, -1);
        timer = 0;

        for (int i : activeNodes) {
            if (disc[i] == -1) {
                tarjanDFS(i, disc, low, stack, onStack, sccs);
            }
        }
        return sccs;
    }

    /**
     * DFS récursif utilisé par l'algorithme de Tarjan.
     *
     * @param u       Le nœud actuellement visité.
     * @param disc    Tableau stockant les temps de découverte des nœuds.
     * @param low     Tableau stockant la plus petite valeur de découverte accessible depuis le nœud.
     * @param stack   La pile gardant la trace des nœuds de la CFC en cours d'exploration.
     * @param onStack Tableau booléen indiquant si un nœud est actuellement présent dans la pile.
     * @param sccs    La liste globale où les nouvelles CFC découvertes sont ajoutées.
     */
    private void tarjanDFS(int u, int[] disc, int[] low, Stack<Integer> stack, boolean[] onStack,
            List<List<Integer>> sccs) {
        disc[u] = low[u] = timer++;
        stack.push(u);
        onStack[u] = true;

        List<Integer> neighbors = adj.get(u);
        if (neighbors != null) {
            for (int v : neighbors) {
                if (disc[v] == -1) {
                    tarjanDFS(v, disc, low, stack, onStack, sccs);
                    low[u] = Math.min(low[u], low[v]);
                } else if (onStack[v]) {
                    low[u] = Math.min(low[u], disc[v]);
                }
            }
        }

        if (low[u] == disc[u]) {
            List<Integer> component = new ArrayList<>();
            while (true) {
                int node = stack.pop();
                onStack[node] = false;
                component.add(node);
                if (u == node)
                    break;
            }
            sccs.add(component);
        }
    }

    /**
     * Calcule le diamètre d'une communauté donnée.
     * Le diamètre correspond à la distance maximale du plus court chemin
     * entre n'importe quelle paire de nœuds au sein de cette communauté.
     * Le calcul s'effectue strictement dans le sous-graphe induit par la communauté.
     *
     * @param component La liste des identifiants des nœuds formant la communauté.
     * @return Le diamètre de la communauté (exprimé en nombre d'arêtes).
     */
    public int calculateDiameter(List<Integer> component) {
        int maxDist = 0;
        Set<Integer> nodesInC = new HashSet<>(component);
        // Lance un BFS depuis chaque nœud de la composante pour trouver la distance max
        for (int startNode : component) {
            maxDist = Math.max(maxDist, bfs(startNode, nodesInC));
        }
        return maxDist;
    }

    /**
     * BFS pour trouver la distance maximale depuis un nœud de départ,
     * en se restreignant exclusivement aux nœuds de la communauté spécifiée.
     *
     * @param start    Le nœud de départ du BFS.
     * @param nodesInC L'ensemble des nœuds appartenant à la communauté (utilisé pour le filtrage).
     * @return La distance du chemin le plus long (le plus court chemin) trouvé depuis le nœud de départ.
     */
    private int bfs(int start, Set<Integer> nodesInC) {
        Queue<Integer> q = new LinkedList<>();
        Map<Integer, Integer> dist = new HashMap<>();
        q.add(start);
        dist.put(start, 0);
        int max = 0;
        
        while (!q.isEmpty()) {
            int u = q.poll();
            max = Math.max(max, dist.get(u));
            
            List<Integer> neighbors = adj.get(u);
            if (neighbors != null) {
                for (int v : neighbors) {
                    // On n'explore le voisin que s'il fait partie de la communauté et n'a pas encore été visité
                    if (nodesInC.contains(v) && !dist.containsKey(v)) {
                        dist.put(v, dist.get(u) + 1);
                        q.add(v);
                    }
                }
            }
        }
        return max;
    }
}