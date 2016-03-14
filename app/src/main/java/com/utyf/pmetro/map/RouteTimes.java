package com.utyf.pmetro.map;

/**
 * Created by Fedor on 12.03.2016.
 */

import com.utyf.pmetro.util.StationsNum;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;
import java.util.HashSet;

public class RouteTimes {
    public AllTimes fromStart,toEnd;
    public class TRPtimes {
        Line[] lines;
        public TRPtimes(int size) {
            lines = new Line[size];
        }
    }

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

    public class Line {
        float[] stns;
        public Line(int size) {
            stns = new float[size];
        }
    }

    public class AllTimes {
        TRPtimes[] trps;

        public AllTimes() {
            trps = new TRPtimes[TRP.trpList.length];
            for (int k = 0; k < TRP.trpList.length; k++) {  // create and erase all arrays
                TRP tt1 = TRP.getTRP(k);
                assert tt1 != null;
                trps[k] = new TRPtimes(tt1.lines.length);
                for (int i = 0; i < tt1.lines.length; i++) {
                    trps[k].lines[i] = new Line(tt1.getLine(i).Stations.length);
                    for (int j = 0; j < trps[k].lines[i].stns.length; j++) {
                        trps[k].lines[i].stns[j] = -1;
                    }
                }
            }
        }

        float getTime(int t, int l, int s) {
            if (t == -1 || l == -1 || s == -1) return -1;
            return trps[t].lines[l].stns[s];
        }

        float getTime(StationsNum stn) {
            if (stn.trp == -1 || stn.line == -1 || stn.stn == -1) return -1;
            return trps[stn.trp].lines[stn.line].stns[stn.stn];
        }

        void setTime(int t, int l, int s, float tm) {
            if (t == -1 || l == -1 || s == -1) return;
            trps[t].lines[l].stns[s] = tm;
        }

        void setTime(StationsNum stn, float tm) {
            if (stn.trp == -1 || stn.line == -1 || stn.stn == -1) return;
            trps[stn.trp].lines[stn.line].stns[stn.stn] = tm;
        }
    }

    public RouteTimes() {
    }

    private void addStationVertices(Graph<Node> graph, int trpIdx, int lnIdx, int stnIdx) {
        for (int platformNum = 0; platformNum < 2; platformNum++) {
            Node platformNode = new Node(trpIdx, lnIdx, stnIdx, platformNum, NodeType.PLATFORM);
            Node trainNode = new Node(trpIdx, lnIdx, stnIdx, platformNum, NodeType.TRAIN);
            graph.addVertex(platformNode);
            graph.addVertex(trainNode);
        }
    }

    private void addStationEdges(Graph<Node> graph, int trpIdx, int lnIdx, int stnIdx) {

        TRP.TRP_line ln = TRP.trpList[trpIdx].lines[lnIdx];

        // Create edges between adjacent stations on each line
        TRP.TRP_Station stn = ln.getStation(stnIdx);
        for (TRP.TRP_Driving drv : stn.drivings) {
            if (drv.bckDR > 0) {
                Node from = new Node(trpIdx, lnIdx, stnIdx, 0, NodeType.TRAIN);
                Node to = new Node(trpIdx, lnIdx, drv.bckStNum, 0, NodeType.TRAIN);
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

        // Create edges for transfers
        TRP.Transfer[] transfers = TRP.getTransfers(trpIdx, lnIdx, stnIdx);
        if (transfers != null) {
            for (TRP.Transfer transfer : transfers) {
                // TODO: 14.03.2016
                // Now it is ignored whether transport is active
//                if (!TRP.isActive(transfer.trp2num))
//                    continue;

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

        // Process each station
        for (int trpIdx = 0; trpIdx < TRP.trpList.length; trpIdx++) {
            TRP trp = TRP.trpList[trpIdx];
            for (int lnIdx = 0; lnIdx < trp.lines.length; lnIdx++) {
                TRP.TRP_line ln = trp.lines[lnIdx];
                for (int stnIdx = 0; stnIdx < ln.Stations.length; stnIdx++) {
                    addStationVertices(graph, trpIdx, lnIdx, stnIdx);
                }
            }
        }

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
        fromStart = new AllTimes();
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

        // Copy shortest path lengths to fromStart
        for (int trpIdx = 0; trpIdx < TRP.trpList.length; trpIdx++) {
            TRP trp = TRP.trpList[trpIdx];
            for (int lnIdx = 0; lnIdx < trp.lines.length; lnIdx++) {
                TRP.TRP_line ln = trp.lines[lnIdx];
                for (int stnIdx = 0; stnIdx < ln.Stations.length; stnIdx++) {
                    double minTime = Double.POSITIVE_INFINITY;
                    for (int platformNum = 0; platformNum < 2; platformNum++) {
                        Node to = new Node(trpIdx, lnIdx, stnIdx, 0, NodeType.PLATFORM);
                        double time = graph.getPathLength(to);
                        minTime = Math.min(minTime, time);
                    }
                    if (minTime == Double.POSITIVE_INFINITY)
                        minTime = -1;
                    fromStart.setTime(trpIdx, lnIdx, stnIdx, (float)minTime);
                }
            }
        }
    }

    public void setEnd(StationsNum end) {
        if (graph == null)
            throw new AssertionError();
        toEnd = new AllTimes();
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
        Node node = endNode;
        HashSet<Node> startNodesSet = new HashSet<>(Arrays.asList(startNodes));
        while (!startNodesSet.contains(node)) {
            // TODO: 14.03.2016
            // Conversion from node to its index in graph happens several times. Maybe it is better
            // to take out this functionality from graph and implement it in RouteTimes.
            route.addNode(new RouteNode(node.trp, node.line, node.stn, (float)graph.getPathLength(node)));
            node = graph.getParent(node);
        }
        return route;
    }
}
