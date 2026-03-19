import java.util.Arrays;

public class AuthorUnionFind {
    private int[] parent;
    private int[] rank;
    private int count; 

    public AuthorUnionFind(int initialCapacity) {
        parent = new int[initialCapacity];
        rank = new int[initialCapacity];
        count = 0; 
    }

    public void addAuthor(int id) {
        ensureCapacity(id);
        parent[id] = id;
        rank[id] = 0;
        count++; 
    }

    // Cette méthode double la capacité des tableaux si nécessaire
    private void ensureCapacity(int id) {
        if (id >= parent.length) {
            int newSize = Math.max(id + 1, parent.length * 2);
            int[] oldParent = parent;
            parent = Arrays.copyOf(parent, newSize);
            rank = Arrays.copyOf(rank, newSize);
            for (int i = oldParent.length; i < newSize; i++) {
                parent[i] = i;
            }
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
            if (rank[rootI] < rank[rootJ]) parent[rootI] = rootJ;
            else if (rank[rootI] > rank[rootJ]) parent[rootJ] = rootI;
            else {
                parent[rootI] = rootJ;
                rank[rootJ]++;
            }
            count--; 
        }
    }

    public int getCount() { return count; }
}