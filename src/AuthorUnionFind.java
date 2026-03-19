import java.util.*;
import java.io.*;

public class AuthorUnionFind {
    private int[] parent;
    private int[] rank;
    private int[] size; // <--- Nouveau : stocke la taille de la communauté
    private int count;

    public AuthorUnionFind(int initialCapacity) {
        parent = new int[initialCapacity];
        rank = new int[initialCapacity];
        size = new int[initialCapacity];
        count = 0;
    }

    public void addAuthor(int id) {
        ensureCapacity(id);
        parent[id] = id;
        size[id] = 1; // <--- Chaque nouvel auteur est une communauté de taille 1
        count++;
    }

    private void ensureCapacity(int id) {
        if (id >= parent.length) {
            int newSize = Math.max(id + 1, parent.length * 2);
            parent = Arrays.copyOf(parent, newSize);
            rank = Arrays.copyOf(rank, newSize);
            size = Arrays.copyOf(size, newSize);
        }
    }

    public int find(int i) {
        if (parent[i] == i) return i;
        return parent[i] = find(parent[i]);
    }

    public void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);
        if (rootI != rootJ) {
            if (rank[rootI] < rank[rootJ]) {
                parent[rootI] = rootJ;
                size[rootJ] += size[rootI]; // Fusion des tailles
            } else if (rank[rootI] > rank[rootJ]) {
                parent[rootJ] = rootI;
                size[rootI] += size[rootJ];
            } else {
                parent[rootI] = rootJ;
                size[rootJ] += size[rootI];
                rank[rootJ]++;
            }
            count--;
        }
    }

    public int getCount() { return count; }

    public List<Integer> getTopCommunitySizes(int limit) {
        List<Integer> sizes = new ArrayList<>();
        for (int i = 0; i < parent.length; i++) {
            if (parent[i] == i && size[i] > 0) { // Si c'est une racine
                sizes.add(size[i]);
            }
        }
        sizes.sort(Collections.reverseOrder());
        return sizes.subList(0, Math.min(limit, sizes.size()));
    }

    // Pour l'histogramme final
    public Map<Integer, Integer> getHistogram() {
        Map<Integer, Integer> hist = new TreeMap<>();
        for (int i = 0; i < parent.length; i++) {
            if (parent[i] == i && size[i] > 0) {
                int s = size[i];
                hist.put(s, hist.getOrDefault(s, 0) + 1);
            }
        }
        return hist;
    }
}