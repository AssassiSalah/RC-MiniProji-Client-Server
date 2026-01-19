package com.virus_check;

public enum ApiKeys {
    // Define your API keys as enum constants
    VIRUS_TOTAL_API_KEY("d771c282fb323ad4a933bf849968f505dabd950e9f8af3497f8617e52ee01d07");

    private final String key;

    // Constructor for the enum
    ApiKeys(String key) {
        this.key = key;
    }

    // Method to retrieve the key
    public String getKey() {
        return key;
    }
}
