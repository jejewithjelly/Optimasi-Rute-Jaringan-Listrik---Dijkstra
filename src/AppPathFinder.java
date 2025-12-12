import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppPathFinder extends JFrame {

    private PathFinder pathFinder;
    private GraphPanel graphPanel;
    private JComboBox<String> startCombo;
    private JComboBox<String> endCombo;
    private JComboBox<String> brokenCombo;
    private JTextArea infoArea;
    private Map<String, Point> nodeLocations = new HashMap<>();

    private static final double ARUS_I = 600.0;       // Ampere (I)
    private static final double RESISTANSI_R = 0.0671; // Ohm/km (r)
    private static final double FASA_CONST = 3.0;     // Konstanta 3 Fasa

    public AppPathFinder() {
        pathFinder = new PathFinder();
        
        // Load data
        try {
            pathFinder.loadGraphFromFile("data.txt");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Gagal membaca file data.txt! Pastikan file ada di folder project.");
        }
        
        initCoordinates(); 

        setTitle("Simulasi Jalur Listrik & Power Loss");
        
        // Full Screen
        setExtendedState(JFrame.MAXIMIZED_BOTH); 
        setSize(1300, 750); 
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- 1. PANEL KONTROL (ATAS) ---
        JPanel topPanel = new JPanel(new GridLayout(2, 1)); 
        
        // Baris 1: Rute
        JPanel routePanel = new JPanel();
        List<String> sortedVertices = pathFinder.getSortedVertexNames();
        startCombo = new JComboBox<>(sortedVertices.toArray(new String[0]));
        endCombo = new JComboBox<>(sortedVertices.toArray(new String[0]));
        JButton btnFind = new JButton("Cari Rute & Hitung Power Loss");
        
        routePanel.add(new JLabel("Start:"));
        routePanel.add(startCombo);
        routePanel.add(new JLabel("End:"));
        routePanel.add(endCombo);
        routePanel.add(btnFind);
        
        // Baris 2: Jika ada vertex padam (dan bisa dihidupkan kembali)
        JPanel brokenPanel = new JPanel();
        brokenCombo = new JComboBox<>(sortedVertices.toArray(new String[0]));
        JButton btnToggleBroken = new JButton("Matikan / Hidupkan Gardu");
        
        brokenPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        brokenPanel.add(new JLabel("Simulasi Kerusakan (Pilih Gardu):"));
        brokenPanel.add(brokenCombo);
        brokenPanel.add(btnToggleBroken);

        topPanel.add(routePanel);
        topPanel.add(brokenPanel);
        add(topPanel, BorderLayout.NORTH);

        // PANEL VISUALISASI GRAF (TENGAH) 
        graphPanel = new GraphPanel();
        graphPanel.setPreferredSize(new Dimension(1200,800));

        JScrollPane scrollPaneGraph = new JScrollPane(graphPanel);
        scrollPaneGraph.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneGraph.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        add(scrollPaneGraph, BorderLayout.CENTER);

        // PANEL INFO (KANAN)
        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(new Font("SansSerif", Font.PLAIN, 12)); 
        infoArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(infoArea);
        JPanel sidePanel = new JPanel(new BorderLayout());
        
        // Lebar panel samping diperbesar sedikit agar muat rumus
        sidePanel.setPreferredSize(new Dimension(350, 0));
        sidePanel.setBorder(BorderFactory.createTitledBorder("Hasil Perhitungan"));
        sidePanel.add(scrollPane, BorderLayout.CENTER);

        add(sidePanel, BorderLayout.EAST);

        // ACTION LISTENERS 
        
        // Tombol Cari Rute
        btnFind.addActionListener(e -> {
            String start = (String) startCombo.getSelectedItem();
            String end = (String) endCombo.getSelectedItem();
            
            if(start.equals(end)) {
                JOptionPane.showMessageDialog(this, "Titik awal dan akhir sama!");
                return;
            }

            infoArea.setText(""); // Reset log setiap pencarian baru
            infoArea.append("Mencari: " + start + "\n      -> " + end + "\n\n");

            // 1. Cek Normal
            PathFinder.PathResult result = pathFinder.findShortestPath(start, end, false);
            
            if (result.path.isEmpty()) {
                // 2. Cek Diagnostik
                PathFinder.PathResult theoreticalResult = pathFinder.findShortestPath(start, end, true);
                
                infoArea.append("STATUS: GAGAL\n");
                if (theoreticalResult.path.isEmpty()) {
                    infoArea.append("Penyebab: Tidak ada jalur koneksi.\n");
                } else {
                    infoArea.append("Penyebab: Jalur terputus akibat\n");
                    infoArea.append("adanya Gardu/Node yang PADAM.\n");
                }
                
            } else {
                infoArea.append("STATUS: RUTE DITEMUKAN\n");
                infoArea.append("----------------------------------\n");
                
                // --- PERHITUNGAN POWER LOSS (Sistem 3 Fasa) ---
                double L = result.totalCost; // Panjang Saluran (km)
                
                // 1. Hitung Tahanan Total (Rtotal = L * r)
                double Rtotal = L * RESISTANSI_R;
                
                // 2. Hitung Power Loss (Plosses = 3 * I^2 * Rtotal
                double Plosses = FASA_CONST * Math.pow(ARUS_I, 2) * Rtotal;
                
                // --- TAMPILKAN HASIL ---
                infoArea.append("[PARAMETER TEKNIS]\n");
                infoArea.append("Arus (I)      : " + ARUS_I + " A\n");
                infoArea.append("Resistansi (r): " + RESISTANSI_R + " Ohm/km\n");
                infoArea.append("Fasa          : 3\n\n");
                
                infoArea.append("[HASIL PERHITUNGAN]\n");
                infoArea.append("Jarak Total (L): " + String.format("%.3f", L) + " km\n");
                infoArea.append("Tahanan (Rtot) : " + String.format("%.4f", Rtotal) + " Ohm\n");
                infoArea.append("----------------------------------\n");
                infoArea.append("POWER LOSS (Plosses):\n");
                infoArea.append("= 3 * I^2 * Rtotal\n");
                infoArea.append("= " + String.format("%,.2f", Plosses) + " Watt\n");
                infoArea.append("= " + String.format("%,.2f", Plosses/1000) + " kW\n");
                infoArea.append("----------------------------------\n");
                
                infoArea.append("\nDetail Jalur (" + result.path.size() + " steps):\n");
                for (int i = 0; i < result.path.size(); i++) {
                    String p = result.path.get(i);
                    if (i == result.path.size() - 1) infoArea.append(" └ " + p + "\n");
                    else if (i == 0) infoArea.append(" ┌ " + p + "\n");
                    else infoArea.append(" ├ " + p + "\n");
                }
            }
            
            infoArea.setCaretPosition(0);
            graphPanel.animatePath(result.path);
        });

        // Tombol Toggle Kerusakan
        btnToggleBroken.addActionListener(e -> {
            String selectedNode = (String) brokenCombo.getSelectedItem();
            boolean isCurrentlyBroken = pathFinder.isNodeBroken(selectedNode);
            
            pathFinder.setNodeBroken(selectedNode, !isCurrentlyBroken);
            
            String status = !isCurrentlyBroken ? "PADAM" : "AKTIF";
            infoArea.append("\n[UPDATE STATUS]\n");
            infoArea.append(selectedNode + "\nStatus: " + status + "\n");
            
            graphPanel.repaint();
        });
    }

    private void initCoordinates() {
        // --- Koordinat Visual Node ---
        nodeLocations.put("PLN (V1)", new Point(30, 450));
        nodeLocations.put("Patung Wisnu (V2)", new Point(130, 400));
        nodeLocations.put("Lampu Merah Girimulyo (V3)", new Point(210, 300));
        nodeLocations.put("Bahagia Bersama Kopi (V4)", new Point(260, 550));
        nodeLocations.put("Tugu Adipura (V5)", new Point(320, 150));
        nodeLocations.put("Jalan Raya Solo (V6)", new Point(370, 300));
        nodeLocations.put("Pertigaan Jalan Kapten Piere Tendean Ki Mangun Sarkoro (V9)", new Point(460, 50));
        nodeLocations.put("Simpang Nusukan (V8)", new Point(500, 250));
        nodeLocations.put("Perempatan Jalan Raya Solo - Jalan Kapten Piere Tendean (V7)", new Point(540, 350));
        nodeLocations.put("Perempatan Ngemplak (V10)", new Point(630, 380));
        nodeLocations.put("Pertigaan Jalan A.Yani - Jalan Tentara Pelajar (V11)", new Point(710, 380));
        nodeLocations.put("Lampu Merah Dr Oen (V14)", new Point(790, 380));
        nodeLocations.put("Pertigaan Patung Semar (V12)", new Point(730, 500));
        nodeLocations.put("Panggung (V13)", new Point(810, 500));
        nodeLocations.put("Tugu Tembengan (V15)", new Point(880, 420));
        nodeLocations.put("Gerbang Depan UNS (V16)", new Point(930, 550));
    }

    class GraphPanel extends JPanel {
        private List<String> currentPath = null;
        private int animationStep = 0;
        private Timer timer;

        public GraphPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
            
            timer = new Timer(500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (currentPath != null && animationStep < currentPath.size()) {
                        animationStep++;
                        repaint();
                    } else {
                        timer.stop();
                    }
                }
            });
        }

        // Jalur muncul bertahap
        public void animatePath(List<String> path) {
            this.currentPath = path;
            this.animationStep = 0;
            timer.start();
            repaint();
        }

        // visialisasi
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Loop Background (Edges)
            g2.setStroke(new BasicStroke(1));
            Map<String, List<PathFinder.Edge>> graph = pathFinder.getGraphMap();
            
            for (String src : graph.keySet()) {
                Point p1 = nodeLocations.get(src);
                for (PathFinder.Edge edge : graph.get(src)) {
                    Point p2 = nodeLocations.get(edge.destination);
                    if (p1 != null && p2 != null) {
                        
                        // Warna abu-abu jika ada node yang rusak
                        if (pathFinder.isNodeBroken(src) || pathFinder.isNodeBroken(edge.destination)) {
                             g2.setColor(new Color(230, 230, 230)); 
                        } else {
                             g2.setColor(Color.LIGHT_GRAY);
                        }
                        
                        g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                        drawArrow(g2, p1.x, p1.y, p2.x, p2.y);
                        drawWeight(g2, p1, p2, edge.weight);
                    }
                }
            }

            // 2. Loop Animasi Jalur
            if (currentPath != null && currentPath.size() > 0) {
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(3));
                for (int i = 0; i < animationStep - 1; i++) {
                    if (i + 1 < currentPath.size()) {
                        String u = currentPath.get(i);
                        String v = currentPath.get(i+1);
                        Point p1 = nodeLocations.get(u);
                        Point p2 = nodeLocations.get(v);
                        if(p1 != null && p2 != null) {
                            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                            drawArrow(g2, p1.x, p1.y, p2.x, p2.y);
                        }
                    }
                }
            }

            // 3. Loop Nodes
            for (String node : nodeLocations.keySet()) {
                Point p = nodeLocations.get(node);
                if (p == null) continue;

                if (pathFinder.isNodeBroken(node)) {
                    g2.setColor(Color.DARK_GRAY);
                } else if (currentPath != null && currentPath.contains(node)) {
                    int indexInPath = currentPath.indexOf(node);
                    if (indexInPath < animationStep) g2.setColor(Color.ORANGE);
                    else g2.setColor(Color.BLUE);
                } else {
                    g2.setColor(Color.BLUE);
                }

                g2.fillOval(p.x - 15, p.y - 15, 30, 30);
                
                g2.setColor(Color.BLACK);
                String label = node.length() > 10 ? node.substring(0, 10) + "..." : node;
                g2.drawString(label, p.x - 20, p.y + 25);
                
                if (pathFinder.isNodeBroken(node)) {
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawLine(p.x-5, p.y-5, p.x+5, p.y+5);
                    g2.drawLine(p.x+5, p.y-5, p.x-5, p.y+5);
                }
            }
        }
        
        private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
            int midX = (x1 + x2) / 2;
            int midY = (y1 + y2) / 2;
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowSize = 8;
            g2.fillPolygon(new int[] {
                midX, 
                (int) (midX - arrowSize * Math.cos(angle - Math.PI / 6)), 
                (int) (midX - arrowSize * Math.cos(angle + Math.PI / 6))
            }, new int[] {
                midY, 
                (int) (midY - arrowSize * Math.sin(angle - Math.PI / 6)), 
                (int) (midY - arrowSize * Math.sin(angle + Math.PI / 6))
            }, 3);
        }

        private void drawWeight(Graphics2D g2, Point p1, Point p2, double weight) {
            int midX = (p1.x + p2.x) / 2;
            int midY = (p1.y + p2.y) / 2;
            int offsetX = midX;
            int offsetY = midY - 10;

            String label = String.valueOf(weight);
            
            Font originalFont = g2.getFont();
            g2.setFont(new Font("SansSerif", Font.BOLD, 10)); 
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(label);
            int textH = fm.getHeight();
            
            g2.setColor(new Color(255, 255, 255, 240)); 
            g2.fillRect(offsetX - textW/2 - 2, offsetY - textH + 3, textW + 4, textH);
            g2.setColor(new Color(0, 100, 0));
            g2.drawString(label, offsetX - textW/2, offsetY);
            g2.setFont(originalFont);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppPathFinder().setVisible(true));
    }
}