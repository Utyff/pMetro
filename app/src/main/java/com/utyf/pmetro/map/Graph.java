package com.utyf.pmetro.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * Created by Fedor on 13.03.2016.
 */

public class Graph<Node> {
    private HashMap<Node, Integer> vertexIndices;
    private ArrayList<Node> vertices;
    private ArrayList<ArrayList<EdgeInfo>> edges;
    private double[] distances;
    private int[] parents;
    private int nVertices;

    class EdgeInfo {
        public int toIdx;
        public double weight;

        public EdgeInfo(int toIdx, double weight) {
            this.toIdx = toIdx;
            this.weight = weight;
        }
    }

    public Graph() {
        vertexIndices = new HashMap<>();
        vertices = new ArrayList<>();
        edges = new ArrayList<>();
        nVertices = 0;
    }

    private int getVertexIndex(Node node) {
        if (!vertexIndices.containsKey(node))
            throw new AssertionError();
        return vertexIndices.get(node);
    }

    public void addVertex(Node node) {
        vertices.add(node);
        vertexIndices.put(node, nVertices);
        edges.add(new ArrayList<EdgeInfo>());
        nVertices++;
    }

    public void addEdge(Node from, Node to, double weight) {
        int fromIdx = getVertexIndex(from);
        int toIdx = getVertexIndex(to);
        edges.get(fromIdx).add(new EdgeInfo(toIdx, weight));
    }

    public void computeShortestPaths(Node[] startVertices) {
        distances = new double[nVertices];
        Arrays.fill(distances, Double.POSITIVE_INFINITY);
        parents = new int[nVertices];

        PriorityQueue<Integer> queue = new PriorityQueue<>(nVertices,
                new Comparator<Integer>() {
                    @Override
                    public int compare(Integer lhs, Integer rhs) {
                        return Double.compare(distances[lhs], distances[rhs]);
                    }
                });
        for (Node node: startVertices) {
            int idx = getVertexIndex(node);
            distances[idx] = 0;
            queue.offer(idx);
        }
        while (!queue.isEmpty()) {
            int fromIdx = queue.poll();
            for (EdgeInfo edgeInfo: edges.get(fromIdx)) {
                int toIdx = edgeInfo.toIdx;
                double newDistance = distances[fromIdx] + edgeInfo.weight;
                if (distances[toIdx] > newDistance) {
                    distances[toIdx] = newDistance;
                    parents[toIdx] = fromIdx;
                    queue.remove(toIdx);
                    queue.offer(toIdx);
                }
            }
        }
    }

    public double getPathLength(Node endVertex) {
        int idx = getVertexIndex(endVertex);
        return distances[idx];
    }

    public Node getParent(Node v) {
        int idx = getVertexIndex(v);
        return vertices.get(parents[idx]);
    }

    public Graph<Node> reversed() {
        Graph<Node> reversedGraph = new Graph<>();

        reversedGraph.nVertices = nVertices;
        reversedGraph.vertexIndices.putAll(vertexIndices);
        reversedGraph.vertices.addAll(vertices);
        for (int fromIdx = 0; fromIdx < nVertices; fromIdx++) {
            reversedGraph.edges.add(new ArrayList<EdgeInfo>());
        }
        for (int fromIdx = 0; fromIdx < nVertices; fromIdx++) {
            ArrayList<EdgeInfo> edgeList = edges.get(fromIdx);
            for (EdgeInfo edge: edgeList) {
                reversedGraph.edges.get(edge.toIdx).add(new EdgeInfo(fromIdx, edge.weight));
            }
        }
        return reversedGraph;
    }
}
