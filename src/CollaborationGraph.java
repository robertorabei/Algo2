import java.util.*;

public class CollaborationGraph {
    private final Map<Integer, List<Integer>> adj = new HashMap<>();

    public void addFilteredEdge(int u, int v) {
        adj.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
    }

    private int timer = 0;

    public List<List<Integer>> findSCCs(int numNodes) {
        int[] disc = new int[numNodes];
        int[] low = new int[numNodes];
        boolean[] onStack = new boolean[numNodes];
        Stack<Integer> stack = new Stack<>();
        List<List<Integer>> sccs = new ArrayList<>();
        Arrays.fill(disc, -1);

        for (int i = 0; i < numNodes; i++) {
            if (disc[i] == -1 && adj.containsKey(i)) { // On ne visite que si le noeud a des arêtes
                tarjanDFS(i, disc, low, stack, onStack, sccs);
            }
        }
        return sccs;
    }

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
            if (component.size() > 1)
                sccs.add(component);
        }
    }

    public int calculateDiameter(List<Integer> component) {
        int maxDist = 0;
        Set<Integer> nodesInC = new HashSet<>(component);
        for (int startNode : component) {
            maxDist = Math.max(maxDist, bfs(startNode, nodesInC));
        }
        return maxDist;
    }

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
