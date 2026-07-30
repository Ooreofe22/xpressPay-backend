package com.payxpress.biller;

public final class BillerPaths {

    private BillerPaths() {}

    public static String fulfilPath(String categoryName, String subCategory) {
        return switch (categoryName.toUpperCase()) {
            case "AIRTIME" -> "airtime/fulfil";
            case "DATA" -> "data/fulfil";
            case "ELECTRICITY" -> "electricity/fulfil";
            case "CABLE TV", "CABLETV" -> "cable/fulfil"; // NOT cable-tv — confirmed from real collection export
            case "EDUCATION" -> educationFulfilPath(subCategory);
            default -> throw new IllegalArgumentException("Unsupported category: " + categoryName);
        };
    }

    public static String validatePath(String categoryName) {
        return switch (categoryName.toUpperCase()) {
            case "ELECTRICITY" -> "electricity/validate";
            case "CABLE TV", "CABLETV" -> "cable/validate";
            default -> throw new IllegalArgumentException(
                    "No validate step for category: " + categoryName + " (airtime/data don't need one)");
        };
    }

    private static String educationFulfilPath(String subCategory) {
        return switch (subCategory.toUpperCase()) {
            case "WAEC_PIN" -> "education/waec/pin/fulfil";
            default -> throw new IllegalArgumentException("Unsupported education product: " + subCategory);
        };
    }
}
