package com.alibaba.dashscope.utils;

public class ParamUtils {

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private ParamUtils() {
        // Utility class should not be instantiated
    }

    /**
     * Check if the model is qwen{n} where n is greater than or equal to 3
     * 
     * @param modelName the model name to check
     * @return true if model is qwen{n} where n is greater than or equal to 3,
     *         false otherwise
     */
    public static boolean isQwenVersionThreeOrHigher(String modelName) {
        if (modelName == null) {
            return false;
        }

        String lowerModelName = modelName.toLowerCase();
        if (!lowerModelName.startsWith("qwen")) {
            return false;
        }

        String remaining = lowerModelName.substring(4);
        try {
            // Extract the number after "qwen"
            StringBuilder numberStr = new StringBuilder();
            for (char c : remaining.toCharArray()) {
                if (Character.isDigit(c)) {
                    numberStr.append(c);
                } else {
                    break;
                }
            }
            if (numberStr.length() > 0) {
                int version = Integer.parseInt(numberStr.toString());
                return version >= 3;
            }
        } catch (NumberFormatException e) {
            // If parsing fails, use default behavior
        }

        return false;
    }

    /**
     * Check if the increment_output parameter should be modified for the given
     * model
     * 
     * @param modelName the model name to check
     * @return false if model contains "tts", "omni", or "qwen-deep-research",
     *         true otherwise
     */
    public static boolean shouldModifyIncrementalOutput(String modelName) {
        if (modelName == null) {
            return true;
        }

        String lowerModelName = modelName.toLowerCase();

        // Return false if model contains any of the specified strings
        if (lowerModelName.contains("tts") ||
            lowerModelName.contains("omni") ||
            lowerModelName.contains("qwen-deep-research")) {
            return false;
        }

        return true;
    }
}
