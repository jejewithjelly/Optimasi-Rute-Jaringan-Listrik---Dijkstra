import java.io.*;
import java.util.*;

public class PathFinder {
    
    // Struktur data Edge
    static class Edge {
        String destination;
        double weight;

        public Edge(String destination, double weight) {
            this.destination = destination;
            this.weight = weight;
        }
    }

    // Struktur data Node untuk PriorityQueue
    static class Node implements Comparable<Node> {
        String id;
        double distance;

        public Node(String id, double distance) {
            this.id = id;
            this.distance = distance;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    // Graph Data
    private Map<String, List<Edge>> adjList = new HashMap<>();
    // Set untuk menyimpan Node yang rusak/padam
    private Set<String> brokenNodes = new HashSet<>();

    public void addEdge(String source, String destination, double weight) {
        adjList.putIfAbsent(source, new ArrayList<>());
        adjList.putIfAbsent(destination, new ArrayList<>());
        adjList.get(source).add(new Edge(destination, weight));
    }

    // Membaca data dari file eksternal
    public void loadGraphFromFile(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            throw new FileNotFoundException("File tidak ditemukan: " + fileName);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Mengabaikan baris yang berisi metadata "" yang mungkin muncul tidak sengaja di data.txt
                if (line.trim().startsWith("[") || line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length == 3) {
                    addEdge(parts[0].trim(), parts[1].trim(), Double.parseDouble(parts[2].trim()));
                }
            }
        }
    }

    // Manajemen Node Rusak
    public void setNodeBroken(String nodeId, boolean isBroken) {
        if (isBroken) {
            brokenNodes.add(nodeId);
        } else {
            brokenNodes.remove(nodeId);
        }
    }

    public boolean isNodeBroken(String nodeId) {
        return brokenNodes.contains(nodeId);
    }
    
    public Map<String, List<Edge>> getGraphMap() {
        return adjList;
    }

    // --- ALGORITMA DIJKSTRA ---
    
    // Overload method agar mudah dipanggil
    public PathResult findShortestPath(String start, String end) {
        return findShortestPath(start, end, false); // Default: Jangan abaikan kerusakan
    }

    // Core Algorithm: Dijkstra Biasa 
    public PathResult findShortestPath(String start, String end, boolean ignoreBroken) {
        // Jika mode normal (tidak ignoreBroken), cek apakah start/end sendiri yang rusak
        if (!ignoreBroken && (brokenNodes.contains(start) || brokenNodes.contains(end))) {
            return new PathResult(new ArrayList<>(), 0.0);
        }

        PriorityQueue<Node> pq = new PriorityQueue<>(); // queue node berdasar jarak terkecil
        Map<String, Double> distances = new HashMap<>(); // menyimpan jarak terpendek ke setiap vertex
        Map<String, String> previous = new HashMap<>(); // menyimpan vertex sebelumnya
        Set<String> visited = new HashSet<>(); // yg sudah dikunjungi akan disimpan

        // Inisialisasi jarak infinity
        for (String v : adjList.keySet()) {
            distances.put(v, Double.MAX_VALUE); // set jarak awal infinity
        }

        distances.put(start, 0.0);
        pq.add(new Node(start, 0.0));

        while (!pq.isEmpty()) {  // loop utama
            Node current = pq.poll(); // ambil dan hapus node dengan jarak terkecil dari queue
            String currentId = current.id;
            
            if (visited.contains(currentId)) continue; // lanjut jika sudah dikunjungi
            visited.add(currentId);
            
            if (adjList.containsKey(currentId)) { // loop untuk semua adjency
                for (Edge edge : adjList.get(currentId)) {
                    
                    // Cek apakah ada node rusak? (Kecuali kita sedang mode diagnosa/ignoreBroken)
                    if (!ignoreBroken && brokenNodes.contains(edge.destination)) {
                        continue; 
                    }

                    if (!visited.contains(edge.destination)) {
                        double newDist = distances.get(currentId) + edge.weight;
                        if (newDist < distances.get(edge.destination)) {
                            distances.put(edge.destination, newDist);
                            previous.put(edge.destination, currentId);
                            pq.add(new Node(edge.destination, newDist));
                        }
                    }
                }
            }
        }

        // Rekonstruksi Jalur (Backtracking dari End ke Start)
        List<String> path = new ArrayList<>();
        
        // Jika jarak ke tujuan masih MAX_VALUE, berarti tidak ada jalur
        if (distances.get(end) == null || distances.get(end) == Double.MAX_VALUE) {
            return new PathResult(path, 0.0);
        }

        String curr = end;
        while (curr != null) {
            path.add(0, curr);
            curr = previous.get(curr);
        }
        
        // Validasi: Jalur harus dimulai dari start (menangani kasus graph terputus total)
        if (!path.isEmpty() && !path.get(0).equals(start)) {
             return new PathResult(new ArrayList<>(), 0.0);
        }

        return new PathResult(path, distances.get(end));
    }

    // --- ALGORITMA MERGE SORT ---
    // Untuk mengurutkan nama vertex secara alfabetis (A-Z)
    public List<String> getSortedVertexNames() {
        List<String> vertices = new ArrayList<>(adjList.keySet());
        mergeSort(vertices, 0, vertices.size() - 1);
        return vertices;
    }

    private void mergeSort(List<String> list, int left, int right) {
        if (left < right) {
            int mid = (left + right) / 2;
            mergeSort(list, left, mid);
            mergeSort(list, mid + 1, right);
            merge(list, left, mid, right);
        }
    }

    // algoritma merge
    private void merge(List<String> list, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;
        List<String> L = new ArrayList<>();
        List<String> R = new ArrayList<>();
        for (int i = 0; i < n1; ++i) L.add(list.get(left + i));
        for (int j = 0; j < n2; ++j) R.add(list.get(mid + 1 + j));

        int i = 0, j = 0, k = left;
        while (i < n1 && j < n2) {
            if (L.get(i).compareTo(R.get(j)) <= 0) { list.set(k, L.get(i)); i++; } 
            else { list.set(k, R.get(j)); j++; }
            k++;
        }
        while (i < n1) { list.set(k, L.get(i)); i++; k++; }
        while (j < n2) { list.set(k, R.get(j)); j++; k++; }
    }

    // Helper class untuk return hasil
    public static class PathResult {
        public List<String> path;
        public double totalCost;

        public PathResult(List<String> path, double totalCost) {
            this.path = path;
            this.totalCost = totalCost;
        }
    }
}