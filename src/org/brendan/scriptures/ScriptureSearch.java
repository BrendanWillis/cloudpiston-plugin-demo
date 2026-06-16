package org.brendan.scriptures;

import com.nxlight.framework.pal.workflow.common.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Base64;

public class ScriptureSearch {

    private static final String EPUB_PATH =
            "/scriptures/standard-works-complete-epub-eng.epub";

    private final CommonController controller;

    public ScriptureSearch(CommonController controller) {
        this.controller = controller;
    }

    // MOVED FROM Brendan.java
    public String execute(Payload payload) throws WorkflowException {

        Data data = payload.getData();

        String searchWord = data.get("scriptureSearchWord");
        String maxResultsText = data.get("scriptureMaxResults");
        String secondWord = data.get("scriptureSecondWord");
        String distanceText = data.get("scriptureDistance");

        int maxResults = 10;

        try {
            maxResults = Integer.parseInt(maxResultsText);
        } catch (Exception e) {
            maxResults = 10;
        }

        int distance = 0;

        try {
            distance = Integer.parseInt(distanceText);
        } catch (Exception e) {
            distance = 0;
        }

        if (searchWord == null || searchWord.trim().length() == 0) {
            return "No scripture search word entered.";
        }

        long javaStart = System.currentTimeMillis();

        PacketDataList scriptureFiles = controller.createDataList(
                "scriptureFiles",
                new String[]{"filename", "fileOnly", "reference", "payloadKey"}
        );

        DataList scriptureResults;

        boolean isProximitySearch =
                secondWord != null && secondWord.trim().length() > 0;

        if (isProximitySearch) {
            scriptureResults =
                    searchProximity(searchWord, secondWord, distance, maxResults, payload, scriptureFiles);
        } else {
            scriptureResults =
                    searchWord(searchWord, maxResults, payload, scriptureFiles);
        }

        long javaEnd = System.currentTimeMillis();
        long scriptureSearchTime = javaEnd - javaStart;

        payload.addDataList(scriptureResults);
        payload.addDataList(scriptureFiles);

        controller.debug("ScriptureFiles count = "
                + scriptureFiles.getRecordCount());

        payload.set("scriptureResultCount",
                String.valueOf(scriptureResults.getRecordCount()));

        payload.set("scriptureFileCount",
                String.valueOf(scriptureFiles.getRecordCount()));

        payload.set("scriptureSearchWord", searchWord);
        payload.set("scriptureSecondWord", secondWord);
        payload.set("scriptureDistance", String.valueOf(distance));
        payload.set("scriptureMaxResults", String.valueOf(maxResults));
        payload.set("isProximitySearch", isProximitySearch ? "Yes" : "No");

        payload.set("javaScriptureSearchTime", scriptureSearchTime + " ms");

        controller.debug("Java scripture search time: " + scriptureSearchTime + " ms");

        return "Scripture search completed.";
    }

    public DataList searchWord(String searchTerm, int maxResults, Payload payload, PacketDataList scriptureFiles)
            throws WorkflowException {

        PacketDataList results = controller.createDataList(
                "scriptureResults",
                new String[]{
                        "reference",
                        "filename",
                        "matchText",
                        "fileOnly",
                        "imageName",
                        "imageMimeType",
                        "imageBase64",
                        "imagePayloadKey"
                });

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            controller.debug("No search term entered.");
            return results;
        }

        String originalSearchTerm = searchTerm;
        searchTerm = normalizeForSearch(searchTerm);

        InputStream stream = getClass().getResourceAsStream(EPUB_PATH);

        if (stream == null) {
            controller.debug("EPUB NOT FOUND: " + EPUB_PATH);
            return results;
        }

        int count = 0;

        try {
            ZipInputStream zip = new ZipInputStream(stream);
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {

                if (!entry.getName().endsWith(".xhtml")) {
                    continue;
                }

                if (!entry.getName().contains("/Text/")) {
                    continue;
                }

                String fileText = readZipEntry(zip);
                String plainText = cleanHtml(fileText);
                String searchableText = normalizeForSearch(plainText);

                if (searchableText.contains(searchTerm)) {

                    PacketDataRecord record = results.insertRecord();

                    record.setDataValue("reference", makeReference(entry.getName(), plainText));
                    record.setDataValue("filename", entry.getName());
                    record.setDataValue("matchText", makeSnippet(plainText, originalSearchTerm));
                    record.setDataValue("fileOnly", getFileOnly(entry.getName()));

                    addFileToPayload(payload, scriptureFiles, entry.getName(), plainText, fileText);
                    addImageToRecord(payload, record, entry.getName(), fileText);

                    count++;

                    if (count >= maxResults) {
                        break;
                    }
                }
            }

        } catch (IOException e) {
            throw new WorkflowException("Error searching EPUB", e);
        }

        controller.debug("Scripture search found " + count + " result(s).");

        return results;
    }

    public DataList searchProximity(String firstWord, String secondWord, int distance, int maxResults,
                                    Payload payload, PacketDataList scriptureFiles)
            throws WorkflowException {

        PacketDataList results = controller.createDataList(
                "scriptureResults",
                new String[]{
                        "reference",
                        "filename",
                        "matchText",
                        "fileOnly",
                        "imageName",
                        "imageMimeType",
                        "imageBase64",
                        "imagePayloadKey"
                });

        if (firstWord == null || firstWord.trim().isEmpty()) {
            controller.debug("No first word entered.");
            return results;
        }

        if (secondWord == null || secondWord.trim().isEmpty()) {
            controller.debug("No second word entered.");
            return results;
        }

        String originalFirstWord = firstWord;

        firstWord = normalizeForSearch(firstWord);
        secondWord = normalizeForSearch(secondWord);

        InputStream stream = getClass().getResourceAsStream(EPUB_PATH);

        if (stream == null) {
            controller.debug("EPUB NOT FOUND: " + EPUB_PATH);
            return results;
        }

        int count = 0;

        try {
            ZipInputStream zip = new ZipInputStream(stream);
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {

                if (!entry.getName().endsWith(".xhtml")) {
                    continue;
                }

                if (!entry.getName().contains("/Text/")) {
                    continue;
                }

                String fileText = readZipEntry(zip);
                String plainText = cleanHtml(fileText);
                String searchableText = normalizeForSearch(plainText);

                if (wordsAreClose(searchableText, firstWord, secondWord, distance)) {

                    PacketDataRecord record = results.insertRecord();

                    record.setDataValue("reference", makeReference(entry.getName(), plainText));
                    record.setDataValue("filename", entry.getName());
                    record.setDataValue("matchText", makeSnippet(plainText, originalFirstWord));
                    record.setDataValue("fileOnly", getFileOnly(entry.getName()));

                    addFileToPayload(payload, scriptureFiles, entry.getName(), plainText, fileText);
                    addImageToRecord(payload, record, entry.getName(), fileText);

                    count++;

                    if (count >= maxResults) {
                        break;
                    }
                }
            }

        } catch (IOException e) {
            throw new WorkflowException("Error searching EPUB", e);
        }

        controller.debug("Proximity search found " + count + " result(s).");

        return results;
    }

    private String readZipEntry(ZipInputStream zip) throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int length;

        while ((length = zip.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }

        byte[] bytes = output.toByteArray();

        if (bytes.length >= 2) {

            if ((bytes[0] == (byte) 0xFF) && (bytes[1] == (byte) 0xFE)) {
                return new String(bytes, StandardCharsets.UTF_16LE);
            }

            if ((bytes[0] == (byte) 0xFE) && (bytes[1] == (byte) 0xFF)) {
                return new String(bytes, StandardCharsets.UTF_16BE);
            }
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String cleanHtml(String html) {

        return html
                .replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String makeSnippet(String text, String searchTerm) {

        String lowerText = text.toLowerCase();
        int index = lowerText.indexOf(searchTerm.toLowerCase());

        if (index < 0) {
            return text.substring(0, Math.min(200, text.length()));
        }

        int start = Math.max(0, index - 80);
        int end = Math.min(text.length(), index + searchTerm.length() + 120);

        return "..." + text.substring(start, end) + "...";
    }

    private String makeReference(String filename, String plainText) {

        String name = filename;

        int slash = name.lastIndexOf("/");
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }

        name = name.replace(".xhtml", "");
        name = name.replace("06897_000_", "");

        String[] parts = name.split("_");

        if (parts.length >= 2) {
            String bookCode = parts[0];
            String chapter = removeLeadingZeros(parts[1]);

            return getBookName(bookCode) + " " + chapter;
        }

        return name.replace("_", " ").replace("-", " ");
    }

    private String removeLeadingZeros(String text) {

        while (text.length() > 1 && text.startsWith("0")) {
            text = text.substring(1);
        }

        return text;
    }

    private String getBookName(String code) {

        if ("1-kgs".equals(code)) {
            return "1 Kings";
        }

        if ("1-chr".equals(code)) {
            return "1 Chronicles";
        }

        if ("2-chr".equals(code)) {
            return "2 Chronicles";
        }

        if ("song".equals(code)) {
            return "Song of Solomon";
        }

        return code.replace("-", " ");
    }

    private String normalizeForSearch(String text) {
        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean wordsAreClose(String text, String firstWord, String secondWord, int distance) {

        String[] words = text.split(" ");

        for (int i = 0; i < words.length; i++) {

            if (words[i].equals(firstWord)) {

                int start = Math.max(0, i - distance);
                int end = Math.min(words.length - 1, i + distance);

                for (int j = start; j <= end; j++) {
                    if (words[j].equals(secondWord)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    private String getFileOnly(String filename) {

        int slash = filename.lastIndexOf("/");

        if (slash >= 0) {
            return filename.substring(slash + 1);
        }

        return filename;
    }

    private void addImageToRecord(Payload payload, PacketDataRecord record, String xhtmlFilename, String fileText)
            throws WorkflowException {

//        controller.debug("IMAGE CHECK RUNNING FOR: " + xhtmlFilename);

        String imagePath = findFirstImagePath(fileText);

        if (imagePath == null) {
//            controller.debug("No image tag found in: " + xhtmlFilename);
            return;
        }

        String resolvedImagePath = resolveImagePath(xhtmlFilename, imagePath);

        byte[] imageBytes = readFileFromEpub(resolvedImagePath);

        if (imageBytes == null) {
            controller.debug("Image not found in EPUB: " + resolvedImagePath);
            return;
        }

        String imageBase64 =
                Base64.getEncoder().encodeToString(imageBytes);

        String imagePayloadKey = "image_" + makeSafeKey(resolvedImagePath);

        record.setDataValue("imageName", resolvedImagePath);
        record.setDataValue("imageMimeType", getMimeType(resolvedImagePath));
        record.setDataValue("imageBase64", imageBase64);
        record.setDataValue("imagePayloadKey", imagePayloadKey);

        payload.set(imagePayloadKey, imageBase64);

        controller.debug("Added payload image: " + imagePayloadKey);
    }

    private String findFirstImagePath(String html) {

        String lowerHtml = html.toLowerCase();

        int imgIndex = lowerHtml.indexOf("<img");

        if (imgIndex < 0) {
            return null;
        }

        int srcIndex = lowerHtml.indexOf("src=", imgIndex);

        if (srcIndex < 0) {
            return null;
        }

        int quoteStart = srcIndex + 4;

        while (quoteStart < html.length() &&
                (html.charAt(quoteStart) == ' ' || html.charAt(quoteStart) == '\t')) {
            quoteStart++;
        }

        char quote = html.charAt(quoteStart);

        if (quote != '"' && quote != '\'') {
            return null;
        }

        int pathStart = quoteStart + 1;
        int pathEnd = html.indexOf(quote, pathStart);

        if (pathEnd < 0) {
            return null;
        }

        return html.substring(pathStart, pathEnd);
    }

    private String resolveImagePath(String xhtmlFilename, String imagePath) {

        if (imagePath.startsWith("/")) {
            return imagePath.substring(1);
        }

        int slash = xhtmlFilename.lastIndexOf("/");

        String folder = "";

        if (slash >= 0) {
            folder = xhtmlFilename.substring(0, slash + 1);
        }

        String combined = folder + imagePath;

        while (combined.contains("../")) {

            int parentIndex = combined.indexOf("../");

            String beforeParent = combined.substring(0, parentIndex);

            if (beforeParent.endsWith("/")) {
                beforeParent = beforeParent.substring(0, beforeParent.length() - 1);
            }

            int previousSlash = beforeParent.lastIndexOf("/");

            if (previousSlash >= 0) {
                combined = beforeParent.substring(0, previousSlash + 1)
                        + combined.substring(parentIndex + 3);
            } else {
                combined = combined.substring(parentIndex + 3);
            }
        }

        return combined;
    }

    private byte[] readFileFromEpub(String targetPath) throws WorkflowException {

        InputStream stream = getClass().getResourceAsStream(EPUB_PATH);

        if (stream == null) {
            controller.debug("EPUB NOT FOUND: " + EPUB_PATH);
            return null;
        }

        try {
            ZipInputStream zip = new ZipInputStream(stream);
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {

                if (entry.getName().equals(targetPath)) {
                    return readZipEntryBytes(zip);
                }
            }

        } catch (IOException e) {
            throw new WorkflowException("Error reading image from EPUB", e);
        }

        return null;
    }

    private byte[] readZipEntryBytes(ZipInputStream zip) throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int length;

        while ((length = zip.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }

        return output.toByteArray();
    }

    private String getMimeType(String filename) {

        String lower = filename.toLowerCase();

        if (lower.endsWith(".png")) {
            return "image/png";
        }

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        if (lower.endsWith(".gif")) {
            return "image/gif";
        }

        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }

        return "image/png";
    }
    public void listImageFiles() throws WorkflowException {

        InputStream stream = getClass().getResourceAsStream(EPUB_PATH);

        if (stream == null) {
            controller.debug("EPUB NOT FOUND: " + EPUB_PATH);
            return;
        }

        try {
            ZipInputStream zip = new ZipInputStream(stream);
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {

                String name = entry.getName().toLowerCase();

                if (name.endsWith(".png") ||
                        name.endsWith(".jpg") ||
                        name.endsWith(".jpeg") ||
                        name.endsWith(".gif") ||
                        name.endsWith(".svg")) {

                    controller.debug("EPUB IMAGE FILE: " + entry.getName());
                }
            }

        } catch (IOException e) {
            throw new WorkflowException("Error listing EPUB images", e);
        }
    }

//    public void inspectEpubForImages() throws WorkflowException {
//
//        InputStream stream = getClass().getResourceAsStream(EPUB_PATH);
//
//        if (stream == null) {
//            controller.debug("EPUB NOT FOUND: " + EPUB_PATH);
//            return;
//        }
//
//        int imageFileCount = 0;
//        int xhtmlWithImgTagCount = 0;
//        int xhtmlWithImageReferenceCount = 0;
//
//        try {
//            ZipInputStream zip = new ZipInputStream(stream);
//            ZipEntry entry;
//
//            while ((entry = zip.getNextEntry()) != null) {
//
//                String entryName = entry.getName();
//                String lowerName = entryName.toLowerCase();
//
//                if (isImageFile(lowerName)) {
//                    imageFileCount++;
//                    controller.debug("EPUB IMAGE FILE: " + entryName);
//                    continue;
//                }
//
//                if (lowerName.endsWith(".xhtml") || lowerName.endsWith(".html")) {
//
//                    String fileText = readZipEntry(zip);
//                    String lowerText = fileText.toLowerCase();
//
//                    if (lowerText.contains("<img")) {
//                        xhtmlWithImgTagCount++;
//                        controller.debug("XHTML HAS IMG TAG: " + entryName);
//                    }
//
//                    if (lowerText.contains(".jpg") ||
//                            lowerText.contains(".jpeg") ||
//                            lowerText.contains(".png") ||
//                            lowerText.contains(".gif") ||
//                            lowerText.contains(".svg")) {
//
//                        xhtmlWithImageReferenceCount++;
//                        controller.debug("XHTML HAS IMAGE TEXT REFERENCE: " + entryName);
//                    }
//                }
//            }
//
//        } catch (IOException e) {
//            throw new WorkflowException("Error inspecting EPUB images", e);
//        }
//
//        controller.debug("EPUB INSPECTION SUMMARY - image files: " + imageFileCount);
//        controller.debug("EPUB INSPECTION SUMMARY - XHTML files with <img>: " + xhtmlWithImgTagCount);
//        controller.debug("EPUB INSPECTION SUMMARY - XHTML files mentioning image extensions: " + xhtmlWithImageReferenceCount);
//    }

    private boolean isImageFile(String lowerName) {

        return lowerName.endsWith(".png") ||
                lowerName.endsWith(".jpg") ||
                lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".gif") ||
                lowerName.endsWith(".svg");
    }
private void addFileToPayload(
        Payload payload,
        PacketDataList scriptureFiles,
        String filename,
        String plainText,
        String fileText) throws WorkflowException {

    String payloadKey = "file_" + makeSafeKey(filename);

    PacketDataRecord fileRecord = scriptureFiles.insertRecord();
    fileRecord.setDataValue("filename", filename);
    fileRecord.setDataValue("fileOnly", getFileOnly(filename));
    fileRecord.setDataValue("reference", makeReference(filename, plainText));
    fileRecord.setDataValue("payloadKey", payloadKey);

    String base64File =
            Base64.getEncoder().encodeToString(
                    fileText.getBytes(StandardCharsets.UTF_8)
            );

    payload.set(payloadKey, base64File);

    controller.debug("Added payload file: " + payloadKey);}

private String makeSafeKey(String text) {
    return text.replaceAll("[^a-zA-Z0-9]", "_");
}

}