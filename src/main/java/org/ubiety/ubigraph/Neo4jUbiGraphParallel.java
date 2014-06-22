package org.ubiety.ubigraph;

import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;

public class Neo4jUbiGraphParallel {

    public static final int BATCH = 1000;
    static Executor pool = Executors.newFixedThreadPool(4);

    enum Shape {
        sphere, cone, cube, torus, dodecahedron, icosahedron, octahedron, tetrahedron, none;

        public int styleId() {
            return ordinal() + 1;
        }
    }

    enum Colors {
        red(Color.RED), green(Color.GREEN), blue(Color.BLUE), orange(Color.ORANGE), yellow(Color.YELLOW), white(Color.WHITE);
        private final Color color;

        Colors(Color color) { this.color = color; }
        public String toHexValue() {
            return "#" + Integer.toHexString(color.getRGB() & 0x00FFFFFF);
        }
    }

    static Map<String, Integer> styles = new HashMap<>();

    public static final String ALL_NODES_QUERY =
            "MATCH (n) " +
                    "OPTIONAL MATCH (n)-[r]->(m) " +
                    "RETURN " +
                    " id(n) as start_id, head(labels(n)) as start_label, n as start_properties, " +
                    " id(m) as end_id, head(labels(m)) as end_label, m as end_properties, " +
                    " id(r) as rel_id, type(r) as rel_type, r as rel_properties " +
                    "LIMIT {1}";

    public static void main(String[] args) throws SQLException {
        UbigraphClient graph = new UbigraphClient();
        graph.clear();
        renderNeo4jGraphParallel(graph, "jdbc:neo4j://localhost:7474/", 1000);
    }

    private static void renderNeo4jGraphParallel(final UbigraphClient graph, String url, int limit) throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (PreparedStatement stmt = connection.prepareStatement(ALL_NODES_QUERY)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                final Collection<Tuple<Integer, String>> nodes = new HashSet<>(BATCH);
                final Collection<Tuple<Integer, Integer>> rels = new HashSet<>(BATCH);
                while (rs.next()) {
                    nodes.add(Tuple.of(rs.getInt("start_id"), rs.getString("start_label")));
                    nodes.add(Tuple.of(rs.getInt("end_id"), rs.getString("end_label")));
                    rels.add(Tuple.of(rs.getInt("start_id"), rs.getInt("end_id")));
                    if (rels.size() == BATCH) {
                        submitJob(graph, nodes, rels);
                        nodes.clear();
                        rels.clear();
                    }
                }
                submitJob(graph, nodes, rels);
            }
        }
    }

    private static void submitJob(final UbigraphClient graph, Collection<Tuple<Integer, String>> nodes, Collection<Tuple<Integer, Integer>> rels) {
        final Iterable<Tuple<Integer, String>> nodesCopy = new ArrayList<>(nodes);
        final Iterable<Tuple<Integer, Integer>> relsCopy = new ArrayList<>(rels);
//        pool.execute(new Runnable() {
//            public void run() {
                for (Tuple<Integer, String> node : nodesCopy) {
                    createUbiVertex(graph, node._1, node._2, null);
                }
                for (Tuple<Integer, Integer> rel : relsCopy) {
                    createUbiEdge(graph, rel._1, rel._2);
                }
//            }
//        });
    }

    private static int createUbiEdge(UbigraphClient graph, int from, int to) {
        int id = graph.newEdge(from, to);
        return id;
    }

    private static int createUbiVertex(UbigraphClient graph, int id, String label, Map props) {
        int success = graph.newVertex(id);
        if (success==-1) return success;
        if (label!=null) graph.changeVertexStyle(id, style(graph, label));
        if (props != null && props.containsKey("name") && asList("Player", "Country", "WorldCup").contains(label)) {
            graph.setVertexAttribute(id, "label", (String) props.get("name"));
        }
        return success;
    }

    private static int style(UbigraphClient graph, String label) {
        if (!styles.containsKey(label)) {
            Colors colorByLabel = Colors.values()[styles.size() % Colors.values().length];
            System.out.println("label " + label);
            int styleId = createVertexStyle(graph, Shape.cube, colorByLabel);
            styles.put(label, styleId);
            return styleId;
        } else {
            return styles.get(label);
        }
    }

    private static int createVertexStyle(UbigraphClient graph, Shape shape, Colors color) {
        int style = nextStyleId();
        graph.newVertexStyle(style, 0);
        graph.setVertexStyleAttribute(style, "shape", shape.name());
        graph.setVertexStyleAttribute(style, "size", "0.5");
        graph.setVertexStyleAttribute(style, "color", color.toHexValue());
        System.out.println(nextStyleId() + " style = " + style + " color " + color + " shape " + shape);
        return style;
    }

    private static int nextStyleId() {
        return styles.size() + 1;
    }
}

