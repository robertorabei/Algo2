import java.util.*;

public class AuthorUnionFind {
    private int[] parent;
    private int[] size;
    private int count;
    private Set<Integer> activeAuthors;

    public AuthorUnionFind(int initialCapacity) {
        parent = new int[initialCapacity];
        size = new int[initialCapacity];
        count = 0;
        activeAuthors = new HashSet<>();
    }

    public void addAuthor(int id) {
        ensureCapacity(id);
        // évite doublons 
        if (size[id] != 0) return;

        parent[id] = id;
        size[id] = 1;
        activeAuthors.add(id);
        count++;
    }

    private void ensureCapacity(int id) {
        if (id >= parent.length) {
            int newSize = Math.max(id + 1, parent.length * 2); 
            parent = Arrays.copyOf(parent, newSize);
            size = Arrays.copyOf(size, newSize);
        }
    }

    public int find(int i) {
        if (parent[i] != i) {
            parent[i] = find(parent[i]); // path compression
        }
        return parent[i];
    }

    public void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);

        if (rootI == rootJ) return;

        // union par taille
        if (size[rootI] < size[rootJ]) {
            parent[rootI] = rootJ;
            size[rootJ] += size[rootI];
        } else {
            parent[rootJ] = rootI;
            size[rootI] += size[rootJ];
        }

        count--;
    }

    public int getCount() {
        return count;
    }

    public List<Integer> getTopCommunitySizes(int limit) {
        List<Integer> sizes = new ArrayList<>();

        for (int i : activeAuthors) {
            if (parent[i] == i) {
                sizes.add(size[i]);
            }
        }

        sizes.sort(Collections.reverseOrder());
        return new ArrayList<>(sizes.subList(0, Math.min(limit, sizes.size())));
    }

    public Map<Integer, Integer> getHistogram() {
        Map<Integer, Integer> hist = new TreeMap<>();

        for (int i : activeAuthors) {
            if (parent[i] == i) {
                int s = size[i];
                hist.put(s, hist.getOrDefault(s, 0) + 1);
            }
        }

        return hist;
    }
}