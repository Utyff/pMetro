package com.utyf.pmetro.util;

import android.os.Parcel;
import android.os.Parcelable;

import com.utyf.pmetro.map.Line;
import com.utyf.pmetro.map.MapData;
import com.utyf.pmetro.map.routing.RouteInfo;

import java.util.ArrayList;
import java.util.Locale;

// This class is used to encapsulate all data required to display route selection dialog in a fragment
public class RouteListItem implements Parcelable {
    private String time;
    private RouteImage image;

    private RouteListItem(String time, RouteImage image) {
        this.time = time;
        this.image = image;
    }

    public static RouteListItem createRouteListItem(RouteInfo route, MapData mapData) {
        int minutes = Math.round(route.getTime());
        String time;
        if (minutes <= 60) {
            time = String.format(Locale.US, "%d", minutes);
        } else {
            int hours = minutes / 60;
            minutes %= 60;
            time = String.format(Locale.US, "%d:%02d", hours, minutes);
        }

        RouteDrawInfo drawInfo = getRouteColors(route, mapData);
        RouteImage image = RouteImage.createRouteImage(drawInfo, mapData.map.parameters);
        return new RouteListItem(time, image);
    }

    public String getTime() {
        return time;
    }

    public RouteImage getImage() {
        return image;
    }

    // Returns list of colors of lines as they are traversed in the route
    private static RouteDrawInfo getRouteColors(RouteInfo routes, MapData mapData) {
        // Find continuous sequences of nodes having the same color and save their colors to colorsList
        ArrayList<Integer> colorsList = new ArrayList<>();
        ArrayList<ConnectionType> connectionsList = new ArrayList<>();
        StationsNum prevNode = null;
        boolean isTrainConnectionAdded = false;
        for (StationsNum node: routes.getStations()) {
            // Check if current line differs from previous one
            if (prevNode != null && (prevNode.line != node.line || prevNode.trp != node.trp)) {
                addRouteColor(prevNode, colorsList, mapData);
                connectionsList.add(ConnectionType.TRANSFER);
                isTrainConnectionAdded = false;
            }
            else if (prevNode != null && !isTrainConnectionAdded) {
                addRouteColor(prevNode, colorsList, mapData);
                connectionsList.add(ConnectionType.TRAIN);
                isTrainConnectionAdded = true;
            }
            prevNode = node;
        }
        if (prevNode != null) {
            addRouteColor(prevNode, colorsList, mapData);
            // Connection is not added
            // connectionsList.size() == colorsList.size() - 1
        }
        // Convert ArrayList to array
        int[] colors = new int[colorsList.size()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = colorsList.get(i);
        }
        // Convert ArrayList to array
        ConnectionType[] connections = connectionsList.toArray(new ConnectionType[connectionsList.size()]);
        // Return result
        RouteDrawInfo drawInfo = new RouteDrawInfo();
        drawInfo.stationColors = colors;
        drawInfo.connectionTypes = connections;
        return drawInfo;
    }

    private static void addRouteColor(StationsNum node, ArrayList<Integer> colorsList, MapData mapData) {
        Line line = mapData.map.getLine(node.trp, node.line);
        if (line == null)
            line = mapData.mapMetro.getLine(node.trp, node.line);
        if (line != null)
            colorsList.add(line.parameters.Color);
        else
            colorsList.add(0xff000000); // Cannot get color from the loaded map
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(time);
        out.writeParcelable(image, flags);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public RouteListItem createFromParcel(Parcel in) {
            String time = in.readString();
            RouteImage image = in.readParcelable(RouteImage.class.getClassLoader());
            return new RouteListItem(time, image);
        }

        public RouteListItem[] newArray(int size) {
            return new RouteListItem[size];
        }
    };
}
