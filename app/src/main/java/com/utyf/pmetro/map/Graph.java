package com.utyf.pmetro.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * Created by Fedor on 13.03.2016.
 *
 * Weighted directed graph and shortest paths finding within it
 */

public class Graph<Node> {
    private HashMap<Node, Integer> vertexIndices;
    private ArrayList<Node> vertices;
    private ArrayList<ArrayList<EdgeInfo>> edges;
    private int nVertices;

    // temporal data
    private int[] parents;
    private PriorityQueue<Integer> queue;

    // output data
    private double[] distances;

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

    // Compute shortest paths using Dijkstra's algorithm
    public void computeShortestPaths(Node[] startVertices) {
        // Reuse allocated memory
        if (distances == null || distances.length < nVertices) {
            distances = new double[nVertices];
            parents = new int[nVertices];
            queue = new PriorityQueue<>(nVertices,
                    new Comparator<Integer>() {
                        @Override
                        public int compare(Integer lhs, Integer rhs) {
                            return Double.compare(distances[lhs], distances[rhs]);
                        }
                    });
        }

        Arrays.fill(distances, Double.POSITIVE_INFINITY);
        for (Node node: startVertices) {
            int idx = getVertexIndex(node);
            distances[idx] = 0;
            queue.offer(idx);
        }
        // Contains value for each vertex, indicating if it has been already extracted from the queue
        boolean isVisited[] = new boolean[nVertices];

        while (!queue.isEmpty()) {
            int fromIdx = queue.poll();
            // This check increases queue size, but allows not to use "decrease key" operation
            if (isVisited[fromIdx])
                continue;
            isVisited[fromIdx] = true;
            for (EdgeInfo edgeInfo: edges.get(fromIdx)) {
                int toIdx = edgeInfo.toIdx;
                double newDistance = distances[fromIdx] + edgeInfo.weight;
                if (distances[toIdx] > newDistance) {
                    distances[toIdx] = newDistance;
                    parents[toIdx] = fromIdx;
                    queue.offer(toIdx);
                }
            }
        }
    }

    // Compute length of path from start vertex to end vertex. Shortest paths must be already
    // precomputed by calling computeShortestPaths.
    public double getPathLength(Node endVertex) {
        int idx = getVertexIndex(endVertex);
        return distances[idx];
    }

    // Get next to last vertex on the shortest path from start vertex to v. Shortest paths must be
    // already precomputed by calling computeShortestPaths.
    public Node getParent(Node v) {
        int idx = getVertexIndex(v);
        return vertices.get(parents[idx]);
    }
}
