package org.example.conf.handmade.test;

public class Geo {

    double lat;
    double lon;

    public Geo(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public static Geo parse(String latlon) {
        String[] parts = latlon.split("\\s*,\\s*");
        double lat = Double.parseDouble(parts[0]);
        double lon = Double.parseDouble(parts[1]);
        return new Geo(lat, lon);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Geo && ((Geo) o).lat == this.lat && ((Geo) o).lon == this.lon;
    }

}
