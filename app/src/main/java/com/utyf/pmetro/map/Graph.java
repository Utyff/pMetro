package com.utyf.pmetro.map;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Fedor on 13.03.2016.
 *
 * Wrapper for graph to convert nodes to its indices and vice versa
 */

public class Graph<Node> {
    private HashMap<Node, Integer> nodeIndices;
    private ArrayList<Node> nodes;
    private BaseGraph baseGraph;

    public Graph() {
        nodeIndices = new HashMap<>();
        nodes = new ArrayList<>();
        baseGraph = new BaseGraph();
    }

    private int getNodeIndex(Node node) {
        if (!nodeIndices.containsKey(node))
            throw new AssertionError();
        return nodeIndices.get(node);
    }

    public void addNode(Node node) {
        int idx = nodes.size();
        nodes.add(node);
        nodeIndices.put(node, idx);
        baseGraph.addVertex();
    }

    public void addEdge(Node from, Node to, double weight) {
        int fromIdx = getNodeIndex(from);
        int toIdx = getNodeIndex(to);
        baseGraph.addEdge(fromIdx, toIdx, weight);
    }

    // Compute shortest paths using Dijkstra's algorithm
    public void computeShortestPaths(Node startNode) {
        baseGraph.computeShortestPaths(getNodeIndex(startNode));
    }

    // Compute length of path from start vertex to end vertex. Shortest paths must be already
    // precomputed by calling computeShortestPaths.
    public double getPathLength(Node endNode) {
        int idx = getNodeIndex(endNode);
        return baseGraph.getPathLength(idx);
    }

    public ArrayList<Node> getPath(Node endNode) {
        int endIdx = getNodeIndex(endNode);
        ArrayList<Integer> path = baseGraph.getPath(endIdx);
        ArrayList<Node> nodePath = new ArrayList<>(path.size());
        for (int idx: path)
            nodePath.add(nodes.get(idx));
        return nodePath;
    }
}
