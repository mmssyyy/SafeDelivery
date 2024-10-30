package com.example.safedelivery;

public class RewardItem {
    private String name;
    private int pointCost;
    private String description;
    private int iconResId;

    public RewardItem(String name, int pointCost, String description, int iconResId) {
        this.name = name;
        this.pointCost = pointCost;
        this.description = description;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public int getPointCost() {
        return pointCost;
    }

    public String getDescription() {
        return description;
    }

    public int getIconResId() {
        return iconResId;
    }
}