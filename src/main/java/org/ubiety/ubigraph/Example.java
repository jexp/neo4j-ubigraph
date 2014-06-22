package org.ubiety.ubigraph;

import org.ubiety.ubigraph.UbigraphClient;

import java.util.Random;

public class Example {

    public static void main(String[] args) {
        UbigraphClient graph = new UbigraphClient();

        graph.clear();
        int N = 100;

        int style = createStyle(graph);

        createNodes(graph, N,style);
//        createRandomConnections(graph, N);
        createClusteredConnections(graph, N);
    }

    private static int createStyle(UbigraphClient graph) {
        int style = graph.newVertexStyle(0);
        graph.setVertexStyleAttribute(style, "shape", "sphere");
        graph.setVertexStyleAttribute(style, "size", "0.2");
        graph.setVertexStyleAttribute(style, "color", "#335500");
        return style;
    }

    private static void createNodes(UbigraphClient graph, int n, int style) {
        for (int i = 0; i < n; ++i) {
            graph.newVertex(i);
            graph.changeVertexStyle(i,style);


//            graph.setVertexAttribute(i, "shape", "sphere");
//            graph.setVertexAttribute(i, "size", "0.2");
////            graph.setVertexAttribute(i, "label", String.valueOf(i));
//            graph.setVertexAttribute(i, "color", "#335500");
        }
    }

    private static void createRandomConnections(UbigraphClient graph, int n) {
        Random random = new Random();
        for (int i = 0; i < n; ++i) {
            int from = random.nextInt(n);
            int to = random.nextInt(n);
            graph.newEdge(from, to);
        }
    }

    private static void createClusteredConnections(UbigraphClient graph, int n) {
        Random random = new Random();
        for (int from = 0; from < n; ++from) {
            for (int to = 0; to < n; ++to) {
                double rand = random.nextDouble();
                boolean inCluster = to % 10 == from % 10;
                if (rand < 0.1 && inCluster || rand < 0.001) {
                    graph.newEdge(from, to);
                }
            }
        }
    }
}

