package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public final class PreprocessInputImage {

    /**
     * Check and upload multiple images with certificate reuse support.
     *
     * @param model Model name
     * @param values Map of image values
     * @param apiKey API key
     * @param certificate Optional upload certificate for reuse
     * @return CheckAndUploadImageResult containing upload status and cert
     * @throws NoApiKeyException If API key is missing
     * @throws UploadFileException If upload fails
     */
    public static CheckAndUploadImageResult checkAndUploadImages(
            String model, Map<String, String> values, String apiKey,
            OSSUploadCertificate certificate)
            throws NoApiKeyException, UploadFileException {
        boolean isUpload = false;
        OSSUploadCertificate cert = certificate;

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String v = entry.getValue();
            if (v == null || v.isEmpty()) {
                continue;
            }
            CheckAndUploadOneImageResult result =
                checkAndUploadOneImage(model, apiKey, v, cert);
            if (!result.getFileUrl().equals(v)) {
                isUpload = true;
                entry.setValue(result.getFileUrl());
            }
            cert = result.getCertificate();
        }
        return new CheckAndUploadImageResult(isUpload, cert);
    }

    /**
     * Check and upload multiple images without certificate reuse (legacy).
     *
     * @param model Model name
     * @param values Map of image values
     * @param apiKey API key
     * @return true if any file was uploaded
     * @throws NoApiKeyException If API key is missing
     * @throws UploadFileException If upload fails
     */
    public static boolean checkAndUploadImage(
            String model, Map<String, String> values, String apiKey)
            throws NoApiKeyException, UploadFileException {
        CheckAndUploadImageResult result = checkAndUploadImages(model,
            values, apiKey, null);
        return result.isUpload();
    }

    /**
     * Check and upload one image with certificate reuse support.
     *
     * @param model Model name
     * @param apiKey API key
     * @param value Image file path
     * @param certificate Optional upload certificate for reuse
     * @return CheckAndUploadOneImageResult containing file URL and cert
     * @throws NoApiKeyException If API key is missing
     * @throws UploadFileException If upload fails
     */
    public static CheckAndUploadOneImageResult checkAndUploadOneImage(
            String model, String apiKey, String value,
            OSSUploadCertificate certificate)
            throws NoApiKeyException, UploadFileException {
        String dstValue = value;
        OSSUploadCertificate cert = certificate;

        if (value.startsWith("http")){
            return new CheckAndUploadOneImageResult(dstValue, cert);
        }

        if (value.startsWith(ApiKeywords.FILE_PATH_SCHEMA)) {
            try {
                URI fileURI = new URI(value);
                File f = new File(fileURI);
                if (f.exists()) {
                    UploadResult result = OSSUtils.uploadWithCertificate(
                        model, f.getAbsolutePath(), apiKey, cert);
                    if (result.getOssUrl().isEmpty()) {
                        throw new UploadFileException(String.format(
                            "Uploading file: %s failed", value));
                    }
                    dstValue = result.getOssUrl();
                    cert = result.getCertificate();
                } else {
                    throw new UploadFileException(String.format(
                        "Local file: %s not exists.", value));
                }
            } catch (URISyntaxException e) {
                throw new UploadFileException(e.getMessage());
            }
        }

        return new CheckAndUploadOneImageResult(dstValue, cert);
    }

    /**
     * Check and upload one image without certificate reuse (legacy).
     *
     * @param model Model name
     * @param apiKey API key
     * @param value Image file path
     * @return File URL
     * @throws NoApiKeyException If API key is missing
     * @throws UploadFileException If upload fails
     */
    public static String checkAndUploadImage(
            String model, String apiKey, String value)
            throws NoApiKeyException, UploadFileException {
        CheckAndUploadOneImageResult result = checkAndUploadOneImage(model,
            apiKey, value, null);
        return result.getFileUrl();
    }

    /**
     * Result of check and upload image operation.
     */
    public static class CheckAndUploadImageResult {
        private boolean upload;
        private OSSUploadCertificate certificate;

        public CheckAndUploadImageResult(boolean upload,
                OSSUploadCertificate certificate) {
            this.upload = upload;
            this.certificate = certificate;
        }

        public boolean isUpload() {
            return upload;
        }

        public OSSUploadCertificate getCertificate() {
            return certificate;
        }
    }

    /**
     * Result of check and upload one image operation.
     */
    public static class CheckAndUploadOneImageResult {
        private String fileUrl;
        private OSSUploadCertificate certificate;

        public CheckAndUploadOneImageResult(String fileUrl,
                OSSUploadCertificate certificate) {
            this.fileUrl = fileUrl;
            this.certificate = certificate;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public OSSUploadCertificate getCertificate() {
            return certificate;
        }
    }

}
