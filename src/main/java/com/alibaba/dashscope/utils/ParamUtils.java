package com.alibaba.dashscope.utils;

public class ParamUtils {
    
    /**
     * Check if the model is qwen{n} where n >= 3
     * 
     * @param modelName the model name to check
     * @return true if model is qwen{n} where n >= 3, false otherwise
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
}
