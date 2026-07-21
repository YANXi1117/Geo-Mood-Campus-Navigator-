package com.example.groupproject.model;

public class Building {

    private final String id;
    private final String name;
    private final double lat;
    private final double lng;
    private final float radius;
    private final String intro;
    private final String moodEmoji;

    public Building(String id, String name, double lat, double lng,
                    float radius, String intro, String moodEmoji) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.radius = radius;
        this.intro = intro;
        this.moodEmoji = moodEmoji;
    }

    public String getId()        { return id; }
    public String getName()      { return name; }
    public double getLat()       { return lat; }
    public double getLng()       { return lng; }
    public float getRadius()     { return radius; }
    public String getIntro()     { return intro; }
    public String getMoodEmoji() { return moodEmoji; }
}
