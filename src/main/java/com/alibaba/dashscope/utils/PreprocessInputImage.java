package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public final class PreprocessInputImage {

    public static boolean checkAndUploadImage(
            String model, Map<String, String> values, String apiKey)
            throws NoApiKeyException, UploadFileException {
        boolean isUpload = false;

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String v = entry.getValue();
            if (v == null || v.isEmpty()) {
                continue;
            }
            String dstValue = checkAndUploadImage(model, apiKey, v);
            if (!dstValue.equals(v)) {
                isUpload = true;
                entry.setValue(dstValue);
            }
        }
        return isUpload;
    }

    public static String checkAndUploadImage(
            String model, String apiKey, String value)
            throws NoApiKeyException, UploadFileException {
        String dstValue = value;

        if (value.startsWith("http")){
            return dstValue;
        }

        if (value.startsWith(ApiKeywords.FILE_PATH_SCHEMA)) {
            try {
                URI fileURI = new URI(value);
                File f = new File(fileURI);
                if (f.exists()) {
                    String fileUrl = OSSUtils.upload(model, f.getAbsolutePath(), apiKey);
                    if (fileUrl.isEmpty()) {
                        throw new UploadFileException(String.format("Uploading file: %s failed", value));
                    }
                    dstValue = fileUrl;
                } else {
                    throw new UploadFileException(String.format("Local file: %s not exists.", value));
                }
            } catch (URISyntaxException e) {
                throw new UploadFileException(e.getMessage());
            }
        }

        return dstValue;
    }

}
