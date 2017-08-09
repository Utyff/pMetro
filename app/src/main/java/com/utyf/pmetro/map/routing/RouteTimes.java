package com.utyf.pmetro.map.routing;

/**
 * Created by Fedor on 12.03.2016.
 *
 * Construct graph for finding of shortest paths using information about lines and transfers
 */

import android.util.Log;

import com.utyf.pmetro.map.TRP;
import com.utyf.pmetro.map.TRP_Collection;
import com.utyf.pmetro.map.TRP_Driving;
import com.utyf.pmetro.map.TRP_Station;
import com.utyf.pmetro.map.TRP_line;
import com.utyf.pmetro.map.Transfer;
import com.utyf.pmetro.util.StationsNum;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;

class RouteTimes {
    private final BitSet activeTransports;
    private final TRP_Collection transports;

    private Graph<Node> graph;
    private Node startNode;
    private Node endNode;
    private ArrayList<Node> blockedNodes;

    private final static int nPlatforms = 1;
    private final static int nTracks = 2;

    // TODO: 30.06.2016 Decrease CHUNK_SIZE after rendering time of bitmap is sped up
    final static int CHUNK_SIZE = 2048;

    private static class Node {
        private enum Type {
            TRAIN, // train has stopped on track and has opened its doors
            PLATFORM, // passenger is on the platform
            ANY_PLATFORM_IN, // passenger starts somewhere on station
            ANY_PLATFORM_OUT // passenger needs to get to any platform on station
        }

        public StationsNum stationsNum;
        public int platform;
        public int track;
        public Type type;

        private Node(int trp, int ln, int stn, int platform, int track, Type type) {
            stationsNum = new StationsNum(trp, ln, stn);
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
            if (!this.stationsNum.isEqual(other.stationsNum)) {
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
            builder.append(stationsNum.trp);
            builder.append(stationsNum.line);
            builder.append(stationsNum.stn);
            builder.append(platform);
            builder.append(type);
            return builder.hashCode();
        }
    }

    public interface Callback {
        void onShortestPathsComputed(StationsNum[] stationNums, float[] stationTimes);
    }

    public RouteTimes(TRP_Collection transports, BitSet activeTransports) {
        this.transports = transports;
        this.activeTransports = activeTransports;
        this.blockedNodes = new ArrayList<>();
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
        TRP_line ln = transports.getTRP(trpIdx).lines[lnIdx];

        // Create edges between adjacent stations on each line
        TRP_Station stn = ln.getStation(stnIdx);
        for (TRP_Driving drv : stn.drivings) {
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
        Transfer[] transfers = transports.getTransfers(trpIdx, lnIdx, stnIdx);
        if (transfers != null) {
            for (Transfer transfer : transfers) {
                if (!isActive(transfer.trp1num) || !isActive(transfer.trp2num))
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
        for (int trpIdx = 0; trpIdx < transports.getSize(); trpIdx++) {
            TRP trp = transports.getTRP(trpIdx);
            for (int lnIdx = 0; lnIdx < trp.lines.length; lnIdx++) {
                TRP_line ln = trp.lines[lnIdx];
                for (int stnIdx = 0; stnIdx < ln.Stations.length; stnIdx++) {
                    addStationVertices(graph, trpIdx, lnIdx, stnIdx);
                }
            }
        }

        // Process each station and add edges to graph
        for (int trpIdx = 0; trpIdx < transports.getSize(); trpIdx++) {
            TRP trp = transports.getTRP(trpIdx);
            for (int lnIdx = 0; lnIdx < trp.lines.length; lnIdx++) {
                TRP_line ln = trp.lines[lnIdx];
                for (int stnIdx = 0; stnIdx < ln.Stations.length; stnIdx++) {
                    addStationEdges(graph, trpIdx, lnIdx, stnIdx);
                }
            }
        }
    }

    public void setStart(StationsNum start) {
        if (graph == null)
            throw new AssertionError();
        if (!isActive(start.trp)) {
            Log.e("RouteTimes", "Transport of start station is not active!");
            return;
        }
        startNode = Node.createAnyPlatformInNode(start.trp, start.line, start.stn);
    }

    public void setEnd(StationsNum end) {
        if (graph == null)
            throw new AssertionError();
        if (!isActive(end.trp)) {
            Log.e("RouteTimes", "Transport of end station is not active!");
            return;
        }
        endNode = Node.createAnyPlatformOutNode(end.trp, end.line, end.stn);
    }

    public void setBlocked(List<StationsNum> blockedStations) {
        if (graph == null)
            throw new AssertionError();

        blockedNodes.clear();
        for (StationsNum station : blockedStations) {
            int nPlatforms = 1;
            int nTracks = 2;
            for (int platformNum = 0; platformNum < nPlatforms; platformNum++) {
                Node platformNode = Node.createPlatformNode(station.trp, station.line, station.stn, platformNum);
                blockedNodes.add(platformNode);
            }
            for (int trackNum = 0; trackNum < nTracks; trackNum++) {
                Node trainNode = Node.createTrainNode(station.trp, station.line, station.stn, trackNum);
                blockedNodes.add(trainNode);
            }
        }
    }

    public void computeShortestPaths(Callback callback) {
        if (startNode == null)
            throw new AssertionError("Start node is not set");
        graph.computeShortestPaths(startNode, blockedNodes, CHUNK_SIZE, new ShortestPathsComputed(callback));
    }

    private class ShortestPathsComputed implements Graph.Callback<Node> {
        private Callback callback;

        public ShortestPathsComputed(Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onShortestPathsComputed(ArrayList<Node> nodes, double[] nodeTimes) {
            ArrayList<StationsNum> stationNums = new ArrayList<>(nodeTimes.length);
            float[] stationTimes = new float[nodeTimes.length];
            for (int i = 0; i < nodeTimes.length; i++) {
                Node node = nodes.get(i);
                if (node.type == Node.Type.ANY_PLATFORM_OUT) {
                    stationNums.add(node.stationsNum);
                    stationTimes[stationNums.size() - 1] = (float)nodeTimes[i];
                }
            }

            callback.onShortestPathsComputed(stationNums.toArray(new StationsNum[0]), Arrays.copyOf(stationTimes, stationNums.size()));
        }
    }

    // Convert list of nodes to route. Multiple nodes can possibly correspond to single node in route.
    private RouteInfo createRoute(Graph<Node>.Path path) {
        ArrayList<StationsNum> stationsNums = new ArrayList<>(path.nodes.size());
        Node lastNode = null;
        for (Node node: path.nodes) {
            if (lastNode == null || !lastNode.stationsNum.isEqual(node.stationsNum))
                stationsNums.add(node.stationsNum);
            lastNode = node;
        }
        double time = path.length;
        StationsNum[] stations = stationsNums.toArray(new StationsNum[stationsNums.size()]);
        return new RouteInfo(stations, (float)time);
    }

    // Must be called after setStart and setEnd. Route must exist.
    public RouteInfo getRoute() {
        return createRoute(graph.getPath(endNode));
    }

    private ArrayList<Graph<Node>.Path> removeNonOptimalTransfers(ArrayList<Graph<Node>.Path> paths) {
        // Subpaths containing only nodes that have TRAIN type
        HashSet<ArrayList<Node>> subpaths = new HashSet<>();
        ArrayList<Graph<Node>.Path> filteredPaths = new ArrayList<>();
        for (Graph<Node>.Path path: paths) {
            ArrayList<Node> subpath = new ArrayList<>();
            for (Node node: path.nodes) {
                if (node.type == Node.Type.TRAIN) {
                    subpath.add(node);
                }
            }
            if (!subpaths.contains(subpath)) {
                // A new path is encountered, train nodes of which differ from train nodes of all
                // previously seen paths
                subpaths.add(subpath);
                filteredPaths.add(path);
            }
        }
        return filteredPaths;
    }

    // TODO: 26.10.2016 Use maxCount!
    // In general it is much easier to find the shortest path than alternative paths, so finding
    // alternative paths is a separate function
    public RouteInfo[] getAlternativeRoutes(int maxCount, float maxTimeDelta) {
        ArrayList<Graph<Node>.Path> paths = graph.getAlternativePaths(endNode, maxTimeDelta);

        // Remove paths that differ only in footpath between two stations
        ArrayList<Graph<Node>.Path> allPaths = new ArrayList<>();
        allPaths.add(graph.getPath(endNode));
        allPaths.addAll(paths);
        ArrayList<Graph<Node>.Path> filteredPaths = removeNonOptimalTransfers(allPaths);
        filteredPaths.remove(0);

        ArrayList<RouteInfo> routes = new ArrayList<>(filteredPaths.size());
        for (Graph<Node>.Path path: filteredPaths) {
            routes.add(createRoute(path));
        }
        return routes.toArray(new RouteInfo[routes.size()]);
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

    private boolean isActive(int transport) {
        return activeTransports.get(transport);
    }
}
