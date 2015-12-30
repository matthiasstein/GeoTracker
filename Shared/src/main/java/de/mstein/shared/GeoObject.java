package de.mstein.shared;

import android.content.res.TypedArray;

import com.google.android.gms.wearable.DataMap;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Mattes on 31.10.2015.
 */
public class GeoObject implements Serializable {
    double lat;
    double lon;
    String name;
    String type;
    String description;
    long date;

    public GeoObject(double lat, double lon, String name, String type, String description) {
        this.lat = lat;
        this.lon = lon;
        this.name = name;
        this.type = type;
        this.description = description;
        this.date = new Date().getTime();
    }

    public GeoObject(double lat, double lon, String name, String type, String description, long date) {
        this.lat = lat;
        this.lon = lon;
        this.name = name;
        this.type = type;
        this.description = description;
        this.date = date;
    }

    public GeoObject(DataMap map) {
        this(
                map.getDouble("lat"),
                map.getDouble("lon"),
                map.getString("name"),
                map.getString("type"),
                map.getString("description"),
                map.getLong("date")
        );
    }

    public DataMap putToDataMap(DataMap map) {
        map.putDouble("lat", lat);
        map.putDouble("lon", lon);
        map.putString("name", name);
        map.putString("type", type);
        map.putString("description", description);
        map.putLong("date", date);
        return map;
    }

    @Override
    public String toString() {
        return name + " - " + type;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getType() {
        return type;
    }

    public Long getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
