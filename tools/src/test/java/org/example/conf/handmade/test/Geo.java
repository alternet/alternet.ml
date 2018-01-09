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

    public double computeDistance(double lat, double lon) {
        double lat1 = this.lat * Math.PI / 180;
        double lon1 = this.lon * Math.PI / 180;
        double lat2 = lat * Math.PI / 180;
        double lon2 = lon * Math.PI / 180;
        double R = 6371d;
        return R * Math.acos(
                  Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1)
                + Math.sin(lat1) * Math.sin(lat2)
        );
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Geo && ((Geo) o).lat == this.lat && ((Geo) o).lon == this.lon;
    }

    @Override
    public int hashCode() {
        return  Double.valueOf(this.lat + this.lon * 31).hashCode();
    }

}
