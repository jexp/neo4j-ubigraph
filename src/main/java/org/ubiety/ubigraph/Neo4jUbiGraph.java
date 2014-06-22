package org.ubiety.ubigraph;

import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;

public class Neo4jUbiGraph {

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
                    " id(n) as start_id, labels(n) as start_labels, n as start_properties, " +
                    " id(m) as end_id, labels(m) as end_labels, m as end_properties, " +
                    " id(r) as rel_id, type(r) as rel_type, r as rel_properties " +
                    "LIMIT {1}";


    public static void main(String[] args) throws SQLException {
        UbigraphClient graph = new UbigraphClient();
        graph.clear();
        renderNeo4jGraph(graph, "jdbc:neo4j://localhost:7474/", 1000);
    }

    private static void renderNeo4jGraph(UbigraphClient graph, String url, int limit) throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (PreparedStatement stmt = connection.prepareStatement(ALL_NODES_QUERY)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    createUbiVertex(rs, "start", graph, false);
                    createUbiVertex(rs, "end", graph, false);
                    createUbiEdge(rs, "start_id", "end_id", "rel", graph);
                }
            }
        }
    }

    private static int createUbiEdge(ResultSet rs, String startId, String endId, String relPrefix, UbigraphClient graph) throws SQLException {
        int from = rs.getInt(startId);
        int to = rs.getInt(endId);
        return createUbiEdge(graph, from, to);
    }

    private static int createUbiEdge(UbigraphClient graph, int from, int to) {
        int id = graph.newEdge(from, to);
        return id;
    }

    private static int createUbiVertex(ResultSet rs, String prefix, UbigraphClient graph, boolean renderProps) throws SQLException {
        int id = rs.getInt(prefix + "_id");
        String label = label((Collection<String>) rs.getObject(prefix + "_labels"));
        Map props = renderProps ? (Map) rs.getObject(prefix + "_properties") : null;

        return createUbiVertex(graph, id, label, props);
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

    private static String label(Collection<String> labels) {
        return labels == null || labels.isEmpty() ? "" : labels.iterator().next();
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

