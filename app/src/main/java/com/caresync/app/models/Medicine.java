package com.caresync.app.models;

public class Medicine {
    private String name;
    private boolean morning; // Breakfast
    private boolean afternoon; // Lunch
    private boolean night; // Dinner
    
    // "Before Meal" or "After Meal"
    private String morningTiming; 
    private String afternoonTiming;
    private String nightTiming;

    private String morningTime;
    private String afternoonTime;
    private String nightTime;

    public Medicine() {}

    public Medicine(String name, boolean morning, boolean afternoon, boolean night) {
        this.name = name;
        this.morning = morning;
        this.afternoon = afternoon;
        this.night = night;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isMorning() { return morning; }
    public void setMorning(boolean morning) { this.morning = morning; }

    public boolean isAfternoon() { return afternoon; }
    public void setAfternoon(boolean afternoon) { this.afternoon = afternoon; }

    public boolean isNight() { return night; }
    public void setNight(boolean night) { this.night = night; }

    public String getMorningTiming() { return morningTiming; }
    public void setMorningTiming(String morningTiming) { this.morningTiming = morningTiming; }

    public String getAfternoonTiming() { return afternoonTiming; }
    public void setAfternoonTiming(String afternoonTiming) { this.afternoonTiming = afternoonTiming; }

    public String getNightTiming() { return nightTiming; }
    public void setNightTiming(String nightTiming) { this.nightTiming = nightTiming; }

    public String getMorningTime() { return morningTime; }
    public void setMorningTime(String morningTime) { this.morningTime = morningTime; }

    public String getAfternoonTime() { return afternoonTime; }
    public void setAfternoonTime(String afternoonTime) { this.afternoonTime = afternoonTime; }

    public String getNightTime() { return nightTime; }
    public void setNightTime(String nightTime) { this.nightTime = nightTime; }

    public String getScheduleText() {
        StringBuilder sb = new StringBuilder();
        if (morning) sb.append("Morning (").append(morningTiming != null ? morningTiming : "After Meal").append(")");
        if (afternoon) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Afternoon (").append(afternoonTiming != null ? afternoonTiming : "After Meal").append(")");
        }
        if (night) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Night (").append(nightTiming != null ? nightTiming : "After Meal").append(")");
        }
        return sb.length() > 0 ? sb.toString() : "—";
    }
}
