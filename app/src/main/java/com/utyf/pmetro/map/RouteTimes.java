package com.utyf.pmetro.map;

/**
 * Created by Fedor on 12.03.2016.
 */

import com.utyf.pmetro.util.StationsNum;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;

public class RouteTimes {
    private Graph<Node> graph;
    private Node[] startNodes;
    private Node[] endNodes;

    private enum NodeType {
        TRAIN, // train has stopped at platform and has opened its doors
        PLATFORM // passenger is on the platform
    }

    private class Node extends StationsNum {
        public int platform;
        public NodeType nodeType;

        public Node(int trp, int ln, int stn, int platform, NodeType nodeType) {
            super(trp, ln, stn);
            this.platform = platform;
            this.nodeType = nodeType;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!Node.class.isAssignableFrom(obj.getClass())) {
                return false;
            }
            final Node other = (Node) obj;
            if (this.trp != other.trp) {
                return false;
            }
            if (this.line != other.line) {
                return false;
            }
            if (this.stn != other.stn) {
                return false;
            }
            if (this.platform != other.platform) {
                return false;
            }
            //noinspection RedundantIfStatement
            if (this.nodeType != other.nodeType) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            HashCodeBuilder builder = new HashCodeBuilder();
            builder.append(this.trp);
            builder.append(this.line);
            builder.append(this.stn);
            builder.append(this.platform);
            builder.append(this.nodeType);
            return builder.hashCode();
        }
    }

    public RouteTimes() {
    }

    private void addStationVertices(Graph<Node> graph, int trpIdx, int lnIdx, int stnIdx) {
        for (int platformNum = 0; platformNum < 2; platformNum++) {
            Node platformNode = new Node(trpIdx, lnIdx, stnIdx, platformNum, NodeType.PLATFORM);
            Node trainNode = new Node(trpIdx, lnIdx, stnIdx, platformNum, NodeType.TRAIN);
            graph.addNode(platformNode);
            graph.addNode(trainNode);
        }
    }

    private void addStationEdges(Graph<Node> graph, int trpIdx, int lnIdx, int stnIdx) {

        TRP.TRP_line ln = TRP.trpList[trpIdx].lines[lnIdx];

        // Create edges between adjacent stations on each line
        TRP.TRP_Station stn = ln.getStation(stnIdx);
        for (TRP.TRP_Driving drv : stn.drivings) {
            if (drv.bckDR > 0) {
                Node from = new Node(trpIdx, lnIdx, stnIdx, 1, NodeType.TRAIN);
                Node to = new Node(trpIdx, lnIdx, drv.bckStNum, 1, NodeType.TRAIN);
                if (drv.bckDR < 0)
                    throw new AssertionError();
                graph.addEdge(from, to, drv.bckDR);
            }
            if (drv.frwDR > 0) {
                Node from = new Node(trpIdx, lnIdx, stnIdx, 0, NodeType.TRAIN);
                Node to = new Node(trpIdx, lnIdx, drv.frwStNum, 0, NodeType.TRAIN);
                if (drv.frwDR < 0)
                    throw new AssertionError();
                graph.addEdge(from, to, drv.frwDR);
            }
        }

        // Create edges for waiting, getting on and off a train
        float delay = ln.delays.get();
        if (delay < 0)
            throw new AssertionError();
        for (int platformNum = 0; platformNum < 2; platformNum++) {
            // Passenger is waiting for a train, then gets in
            {
                Node from = new Node(trpIdx, lnIdx, stnIdx, platformNum, NodeType.PLATFORM);
                Node to = new Node(trpIdx, lnIdx, stnIdx, platformNum, NodeType.TRAIN);
                graph.addEdge(from, to, delay);
            }

            // Passenger is getting off the train, no need to wait
            {
                Node from = new Node(trpIdx, lnIdx, stnIdx, platformNum, NodeType.TRAIN);
                Node to = new Node(trpIdx, lnIdx, stnIdx, platformNum, NodeType.PLATFORM);
                graph.addEdge(from, to, 0);
            }
        }

        // It is possible to move between platforms. Time dependes on station geometry and is
        // neglected now.
        {
            Node from = new Node(trpIdx, lnIdx, stnIdx, 0, NodeType.PLATFORM);
            Node to = new Node(trpIdx, lnIdx, stnIdx, 1, NodeType.PLATFORM);
            graph.addEdge(from, to, 0);
        }
        {
            Node from = new Node(trpIdx, lnIdx, stnIdx, 1, NodeType.PLATFORM);
            Node to = new Node(trpIdx, lnIdx, stnIdx, 0, NodeType.PLATFORM);
            graph.addEdge(from, to, 0);
        }

        // Create edges for transfers
        TRP.Transfer[] transfers = TRP.getTransfers(trpIdx, lnIdx, stnIdx);
        if (transfers != null) {
            for (TRP.Transfer transfer : transfers) {
                if (!TRP.isActive(transfer.trp2num))
                    continue;

                // Find destination of transfer
                StationsNum transferNum1 = new StationsNum(transfer.trp1num, transfer.line1num, transfer.st1num);
                StationsNum transferNum2 = new StationsNum(transfer.trp2num, transfer.line2num, transfer.st2num);
                StationsNum fromNum = new StationsNum(trpIdx, lnIdx, stnIdx);
                StationsNum toNum;
                if (transferNum1.isEqual(fromNum))
                    toNum = transferNum2;
                else if (transferNum2.isEqual(fromNum))
                    toNum = transferNum1;
                else
                    throw new AssertionError();

                for (int fromPlatformNum = 0; fromPlatformNum < 2; fromPlatformNum++) {
                    for (int toPlatformNum = 0; toPlatformNum < 2; toPlatformNum++) {
                        Node from = new Node(fromNum.trp, fromNum.line, fromNum.stn, fromPlatformNum, NodeType.PLATFORM);
                        Node to = new Node(toNum.trp, toNum.line, toNum.stn, toPlatformNum, NodeType.PLATFORM);
                        if (transfer.time < 0)
                            throw new AssertionError();
                        graph.addEdge(from, to, transfer.time);
                    }
                }
            }
        }
    }

    public void createGraph() {
        // TODO: 13.03.2016
        // It is assumed that each station has exactly two platforms. One or several platforms
        // should be supported
        graph = new Graph<>();

        // Process each station and add vertices to graph
        for (int trpIdx = 0; trpIdx < TRP.trpList.length; trpIdx++) {
            TRP trp = TRP.trpList[trpIdx];
            for (int lnIdx = 0; lnIdx < trp.lines.length; lnIdx++) {
                TRP.TRP_line ln = trp.lines[lnIdx];
                for (int stnIdx = 0; stnIdx < ln.Stations.length; stnIdx++) {
                    addStationVertices(graph, trpIdx, lnIdx, stnIdx);
                }
            }
        }

        // Process each station and add edges to graph
        for (int trpIdx = 0; trpIdx < TRP.trpList.length; trpIdx++) {
            TRP trp = TRP.trpList[trpIdx];
            for (int lnIdx = 0; lnIdx < trp.lines.length; lnIdx++) {
                TRP.TRP_line ln = trp.lines[lnIdx];
                for (int stnIdx = 0; stnIdx < ln.Stations.length; stnIdx++) {
                    addStationEdges(graph, trpIdx, lnIdx, stnIdx);
                }
            }
        }
    }

    public synchronized void setStart(StationsNum start) {
        if (graph == null)
            throw new AssertionError();
        if (!TRP.isActive(start.trp) )
            return;  // if start station transport not active
        // TODO: 13.03.2016
        // Replace with ArrayList<Node>
        startNodes = new Node[2];
        for (int platformNum = 0; platformNum < 2; platformNum++) {
            Node from = new Node(start.trp, start.line, start.stn, platformNum, NodeType.PLATFORM);
            startNodes[platformNum] = from;
        }
        graph.computeShortestPaths(startNodes);
    }

    public void setEnd(StationsNum end) {
        if (graph == null)
            throw new AssertionError();
        if (!TRP.isActive(end.trp) )
            return;  // if start station transport not active
        // TODO: 13.03.2016
        // Replace with ArrayList<Node>
        endNodes = new Node[2];
        for (int platformNum = 0; platformNum < 2; platformNum++) {
            Node to = new Node(end.trp, end.line, end.stn, platformNum, NodeType.PLATFORM);
            endNodes[platformNum] = to;
        }
    }

    // Must be called after setStart and setEnd. Route must exist.
    public Route getRoute() {
        Route route = new Route();

        double minTime = Double.POSITIVE_INFINITY;
        Node endNode = null;
        for (Node node: endNodes) {
            double time = graph.getPathLength(node);
            if (minTime > time) {
                minTime = time;
                endNode = node;
            }
            minTime = Math.min(minTime, time);
        }
        ArrayList<Node> path = graph.getPath(endNode);
        // Convert list of nodes to route. Multiple nodes can possibly correspond to single node in route.
        Node lastNode = null;
        for (Node node: path) {
            if (lastNode == null || lastNode.trp != node.trp || lastNode.line != node.line || lastNode.stn != node.stn)
                route.addNode(new RouteNode(node.trp, node.line, node.stn));
            lastNode = node;
        }
        return route;
    }

    public float getTime(int trp, int line, int stn) {
        double minTime = Double.POSITIVE_INFINITY;
        for (int platformNum = 0; platformNum < 2; platformNum++) {
            Node to = new Node(trp, line, stn, platformNum, NodeType.PLATFORM);
            double time = graph.getPathLength(to);
            minTime = Math.min(minTime, time);
        }
        if (minTime == Double.POSITIVE_INFINITY)
            minTime = -1;
        return (float)minTime;
    }

    public float getTime(StationsNum num) {
        return getTime(num.trp, num.line, num.stn);
    }
}
