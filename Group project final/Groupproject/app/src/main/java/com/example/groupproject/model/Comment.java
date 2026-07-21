package com.example.groupproject.model;

public class Comment {
    private final String buildingId;
    private final String emoji;
    private final String text;

    public Comment(String buildingId, String emoji, String text) {
        this.buildingId = buildingId;
        this.emoji = emoji;
        this.text = text;
    }

    public String getBuildingId() { return buildingId; }
    public String getEmoji()      { return emoji; }
    public String getText()       { return text; }
}
