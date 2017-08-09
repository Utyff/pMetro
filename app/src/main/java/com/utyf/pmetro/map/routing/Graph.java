package com.utyf.pmetro.map.routing;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Fedor on 13.03.2016.
 *
 * Wrapper for graph to convert nodes to its indices and vice versa
 */

class Graph<Node> {
    private HashMap<Node, Integer> nodeIndices;
    private ArrayList<Node> nodes;
    private BaseGraph baseGraph;

    public class Path {
        double length;
        ArrayList<Node> nodes;
    }

    public interface Callback<Node> {
        void onShortestPathsComputed(ArrayList<Node> nodes1, double[] nodeTimes);
    }

    public Graph() {
        nodeIndices = new HashMap<>();
        nodes = new ArrayList<>();
        baseGraph = new BaseGraph();
    }

    private int getNodeIndex(Node node) {
        if (!nodeIndices.containsKey(node))
            throw new IllegalArgumentException("Cannot find node: " + node.toString());
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
    public void computeShortestPaths(Node startNode, ArrayList<Node> blockedNodes, int chunkSize, Callback<Node> callback) {
        ArrayList<Integer> blockedVertices = new ArrayList<>(blockedNodes.size());
        for (Node node: blockedNodes)
            blockedVertices.add(getNodeIndex(node));

        baseGraph.computeShortestPaths(getNodeIndex(startNode), blockedVertices, chunkSize, new ShortestPathsComputed(callback));
    }

    private class ShortestPathsComputed implements BaseGraph.Callback {
        private Callback<Node> callback;

        public ShortestPathsComputed(Callback<Node> callback) {
            this.callback = callback;
        }

        @Override
        public void onShortestPathsComputed(int[] vertices, double[] vertexTimes) {
            ArrayList<Node> nodes1 = new ArrayList<>(vertices.length);
            for (int vertex: vertices) {
                nodes1.add(nodes.get(vertex));
            }
            callback.onShortestPathsComputed(nodes1, vertexTimes);
        }
    }

    // Compute length of path from start vertex to end vertex. Shortest paths must be already
    // precomputed by calling computeShortestPaths.
    public double getPathLength(Node endNode) {
        int idx = getNodeIndex(endNode);
        return baseGraph.getPathLength(idx);
    }

    public Path getPath(Node endNode) {
        int endIdx = getNodeIndex(endNode);
        BaseGraph.Path path = baseGraph.getPath(endIdx);
        ArrayList<Node> nodeList = new ArrayList<>(path.vertices.size());
        for (int idx: path.vertices)
            nodeList.add(nodes.get(idx));
        Path nodePath = new Path();
        nodePath.nodes = nodeList;
        nodePath.length = path.length;
        return nodePath;
    }

    public ArrayList<Path> getAlternativePaths(Node endNode, double lengthThreshold) {
        int endIdx = getNodeIndex(endNode);
        ArrayList<BaseGraph.Path> paths = baseGraph.getAlternativePaths(endIdx, lengthThreshold);
        ArrayList<Path> nodePaths = new ArrayList<>(paths.size());
        for (BaseGraph.Path path: paths) {
            ArrayList<Node> nodeList = new ArrayList<>(path.vertices.size());
            for (int idx : path.vertices)
                nodeList.add(nodes.get(idx));
            Path nodePath = new Path();
            nodePath.nodes = nodeList;
            nodePath.length = path.length;
            nodePaths.add(nodePath);
        }
        return nodePaths;
    }
}
