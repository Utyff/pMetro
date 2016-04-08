package com.utyf.pmetro.map;

/**
 * Created by Fedor on 12.03.2016.
 *
 * Construct graph for finding of shortest paths using information about lines and transfers
 */

import android.util.Log;

import com.utyf.pmetro.util.StationsNum;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;

public class RouteTimes {
    private Graph<Node> graph;
    private Node startNode;
    private Node endNode;

    final static int nPlatforms = 1;
    final static int nTracks = 2;

    private static class Node extends StationsNum {
        private enum Type {
            TRAIN, // train has stopped on track and has opened its doors
            PLATFORM, // passenger is on the platform
            ANY_PLATFORM_IN, // passenger starts somewhere on station
            ANY_PLATFORM_OUT // passenger needs to get to any platform on station
        }

        public int platform;
        public int track;
        public Type type;

        private Node(int trp, int ln, int stn, int platform, int track, Type type) {
            super(trp, ln, stn);
            this.platform = platform;
            this.track = track;
            this.type = type;
        }

        public static Node createTrainNode(int trp, int ln, int stn, int track) {
            return new Node(trp, ln, stn, -1, track, Type.TRAIN);
        }

        public static Node createPlatformNode(int trp, int ln, int stn, int platform) {
            return new Node(trp, ln, stn, platform, -1, Type.PLATFORM);
        }

        public static Node createAnyPlatformInNode(int trp, int ln, int stn) {
            return new Node(trp, ln, stn, -1, -1, Type.ANY_PLATFORM_IN);
        }

        public static Node createAnyPlatformOutNode(int trp, int ln, int stn) {
            return new Node(trp, ln, stn, -1, -1, Type.ANY_PLATFORM_OUT);
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
            if (this.track != other.track) {
                return false;
            }
            //noinspection RedundantIfStatement
            if (this.type != other.type) {
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
            builder.append(this.type);
            return builder.hashCode();
        }
    }

    public RouteTimes() {
    }

    private void addStationVertices(Graph<Node> graph, int trpIdx, int lnIdx, int stnIdx) {
        int nPlatforms = 1;
        int nTracks = 2;
        for (int platformNum = 0; platformNum < nPlatforms; platformNum++) {
            Node platformNode = Node.createPlatformNode(trpIdx, lnIdx, stnIdx, platformNum);
            graph.addNode(platformNode);
        }
        for (int trackNum = 0; trackNum < nTracks; trackNum++) {
            Node trainNode = Node.createTrainNode(trpIdx, lnIdx, stnIdx, trackNum);
            graph.addNode(trainNode);
        }
        Node anyPlatformInNode = Node.createAnyPlatformInNode(trpIdx, lnIdx, stnIdx);
        graph.addNode(anyPlatformInNode);
        Node anyPlatformOutNode = Node.createAnyPlatformOutNode(trpIdx, lnIdx, stnIdx);
        graph.addNode(anyPlatformOutNode);
    }

    private void addStationEdges(Graph<Node> graph, int trpIdx, int lnIdx, int stnIdx) {
        TRP.TRP_line ln = TRP.trpList[trpIdx].lines[lnIdx];

        // Create edges between adjacent stations on each line
        TRP.TRP_Station stn = ln.getStation(stnIdx);
        for (TRP.TRP_Driving drv : stn.drivings) {
            if (drv.bckDR > 0) {
                Node from = Node.createTrainNode(trpIdx, lnIdx, stnIdx, 1);
                Node to = Node.createTrainNode(trpIdx, lnIdx, drv.bckStNum, 1);
                if (drv.bckDR < 0)
                    throw new AssertionError();
                graph.addEdge(from, to, drv.bckDR);
            }
            if (drv.frwDR > 0) {
                Node from = Node.createTrainNode(trpIdx, lnIdx, stnIdx, 0);
                Node to = Node.createTrainNode(trpIdx, lnIdx, drv.frwStNum, 0);
                if (drv.frwDR < 0)
                    throw new AssertionError();
                graph.addEdge(from, to, drv.frwDR);
            }
        }

        // Create edges for waiting, getting on and off a train
        float delay = ln.delays.get();
        if (delay < 0)
            throw new AssertionError();
        for (int trackNum = 0; trackNum < nTracks; trackNum++) {
            // Passenger is waiting for a train, then gets in
            {
                Node from = Node.createPlatformNode(trpIdx, lnIdx, stnIdx, 0);
                Node to = Node.createTrainNode(trpIdx, lnIdx, stnIdx, trackNum);
                graph.addEdge(from, to, delay);
            }

            // Passenger is getting off the train, no need to wait
            {
                Node from = Node.createTrainNode(trpIdx, lnIdx, stnIdx, trackNum);
                Node to = Node.createPlatformNode(trpIdx, lnIdx, stnIdx, 0);
                graph.addEdge(from, to, 0);
            }
        }

        // It is possible to move between platforms. Time depends on station geometry and is
        // neglected now.
        for (int fromPlatform = 0; fromPlatform < nPlatforms; fromPlatform++) {
            for (int toPlatform = 0; toPlatform < nPlatforms; toPlatform++) {
                if (fromPlatform != toPlatform) {
                    Node from = Node.createPlatformNode(trpIdx, lnIdx, stnIdx, fromPlatform);
                    Node to = Node.createPlatformNode(trpIdx, lnIdx, stnIdx, toPlatform);
                    graph.addEdge(from, to, 0);
                }
            }
        }

        // Create edges for starting and ending route on any platform for each station
        Node anyPlatformsInNode = Node.createAnyPlatformInNode(trpIdx, lnIdx, stnIdx);
        Node anyPlatformsOutNode = Node.createAnyPlatformOutNode(trpIdx, lnIdx, stnIdx);
        for (int platformNum = 0; platformNum < nPlatforms; platformNum++) {
            Node node = Node.createPlatformNode(trpIdx, lnIdx, stnIdx, platformNum);
            graph.addEdge(anyPlatformsInNode, node, 0);
            graph.addEdge(node, anyPlatformsOutNode, 0);
        }

        // Create edges for transfers
        TRP.Transfer[] transfers = TRP.getTransfers(trpIdx, lnIdx, stnIdx);
        if (transfers != null) {
            for (TRP.Transfer transfer : transfers) {
                if (!TRP.isActive(transfer.trp1num) || !TRP.isActive(transfer.trp2num))
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

                for (int fromPlatformNum = 0; fromPlatformNum < nPlatforms; fromPlatformNum++) {
                    for (int toPlatformNum = 0; toPlatformNum < nPlatforms; toPlatformNum++) {
                        Node from = Node.createPlatformNode(fromNum.trp, fromNum.line, fromNum.stn, fromPlatformNum);
                        Node to = Node.createPlatformNode(toNum.trp, toNum.line, toNum.stn, toPlatformNum);
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
        // It is assumed that each station has exactly two tracks. One or several tracks
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
        if (!TRP.isActive(start.trp)) {
            Log.e("RouteTimes", "Transport of start station is not active!");
            return;
        }
        startNode = Node.createAnyPlatformInNode(start.trp, start.line, start.stn);
        graph.computeShortestPaths(startNode);
    }

    public void setEnd(StationsNum end) {
        if (graph == null)
            throw new AssertionError();
        if (!TRP.isActive(end.trp)) {
            Log.e("RouteTimes", "Transport of end station is not active!");
            return;
        }
        endNode = Node.createAnyPlatformOutNode(end.trp, end.line, end.stn);
    }

    // Convert list of nodes to route. Multiple nodes can possibly correspond to single node in route.
    private Route createRoute(ArrayList<Node> path) {
        Route route = new Route();
        Node lastNode = null;
        for (Node node: path) {
            if (lastNode == null || lastNode.trp != node.trp || lastNode.line != node.line || lastNode.stn != node.stn)
                route.addNode(new RouteNode(node.trp, node.line, node.stn));
            lastNode = node;
        }
        return route;
    }

    // Must be called after setStart and setEnd. Route must exist.
    public Route getRoute() {
        return createRoute(graph.getPath(endNode));
    }

    // In general it is much easier to find the shortest path than alternative paths, so finding
    // alternative paths is a separate function
    public Route[] getAlternativeRoutes(int maxCount, float maxTimeDelta) {
        ArrayList<ArrayList<Node>> paths = graph.getAlternativePaths(endNode, maxTimeDelta);
        ArrayList<Route> routes = new ArrayList<>(paths.size());
        for (ArrayList<Node> path: paths) {
            routes.add(createRoute(path));
        }
        return routes.toArray(new Route[0]);
    }

    public float getTime(int trp, int line, int stn) {
        Node node = Node.createAnyPlatformOutNode(trp, line, stn);
        double time = graph.getPathLength(node);
        if (time == Double.POSITIVE_INFINITY)
            return -1;
        else
            return (float)graph.getPathLength(node);
    }

    public float getTime(StationsNum num) {
        return getTime(num.trp, num.line, num.stn);
    }
}
