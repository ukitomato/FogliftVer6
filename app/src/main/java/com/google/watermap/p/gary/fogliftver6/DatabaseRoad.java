package com.google.watermap.p.gary.fogliftver6;

import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

public class DatabaseRoad {

    private String name;
    private String kind;
    private long level;
    private double startLatitude;
    private double startLongitude;
    private double endLatitude;
    private double endLongitude;

    private LatLng start;
    private LatLng end;

    private String uri;
    private long id;
    private String information;

    private boolean drink;
    private boolean gargle;
    private boolean ice;

    public DatabaseRoad() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public DatabaseRoad(String name, String kind, long level, double startLatitude, double startLongitude,double endLatitude, double endLongitude, long id, String uri, String information) {
        this.name = name;
        this.kind = kind;
        this.level = level;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        start = new LatLng(startLatitude, startLongitude);
        this.endLatitude = endLatitude;
        this.endLongitude = endLongitude;
        end = new LatLng(endLatitude, endLongitude);
        this.uri = uri;
        this.id = id;
        this.information = information;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public long getLevel() {
        return level;
    }

    public LatLng getStart() {
        return start;
    }

    public LatLng getEnd() {
        return end;
    }

    public float getMakerColor() {
        switch ((int) level) {
            case 5:
                return BitmapDescriptorFactory.HUE_RED;
            case 4:
            case 3:
            case 2:
                return BitmapDescriptorFactory.HUE_YELLOW;
            case 1:
                return BitmapDescriptorFactory.HUE_GREEN;
        }
        return BitmapDescriptorFactory.HUE_VIOLET;
    }

    public int getLevelColor() {
        switch ((int) level) {
            case 5:
                return Color.argb(100, 204, 0, 0);
            case 4:
                return Color.argb(100, 255, 183, 76);
            case 3:
                return Color.argb(100, 255, 255, 0);
            case 2:
                return Color.argb(100, 103, 228, 126);
            case 1:
                return Color.argb(100, 12, 0, 204);
        }
        return Color.WHITE;
    }


    public String getImageURI() {
        return uri;
    }

    public long getId() {
        return id;
    }

    public int getImageDrawableID() {
        switch ((int) id) {
            case 5:
                return R.drawable.tsukubauniversity;
            case 4:
                return R.drawable.ministop;
            case 3:
                return R.drawable.seveneleven;
            case 2:
                return R.drawable.lawson;
            case 1:
                return R.drawable.familymart;
        }
        return R.drawable.ic_icon;
    }

    public String getUri() {
        return uri;
    }

    public String getInformation() {
        return information;
    }


    public void setDiarrheaInfo(boolean drink, boolean gargle, boolean ice) {
        this.drink = drink;
        this.gargle = gargle;
        this.ice = ice;
    }

    public boolean canDrink() {
        return drink;
    }

    public boolean canGargle() {
        return gargle;
    }

    public boolean canIce() {
        return ice;
    }
}
