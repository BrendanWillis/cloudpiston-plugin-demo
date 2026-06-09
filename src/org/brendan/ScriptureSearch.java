package org.brendan;

import com.nxlight.framework.pal.workflow.common.CommonController;
import com.nxlight.framework.pal.workflow.common.DataList;
import com.nxlight.framework.pal.workflow.common.PacketDataList;
import com.nxlight.framework.pal.workflow.common.PacketDataRecord;
import com.nxlight.framework.pal.workflow.common.WorkflowException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ScriptureSearch {

    private static final String EPUB_PATH =
            "/scriptures/standard-works-complete-epub-eng.epub";

    private final CommonController controller;

    public ScriptureSearch(CommonController controller) {
        this.controller = controller;
    }

    public DataList searchWord(String searchTerm, int maxResults) throws WorkflowException {

        PacketDataList results = controller.createDataList(
                "scriptureResults",
                new String[]{"reference", "filename", "matchText", "fileOnly"}
        );

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

    public DataList searchProximity(String firstWord, String secondWord, int distance, int maxResults)
            throws WorkflowException {

        PacketDataList results = controller.createDataList(
                "scriptureResults",
                new String[]{"reference", "filename", "matchText", "fileOnly"}
        );

        if (firstWord == null || firstWord.trim().isEmpty()) {
            controller.debug("No first word entered.");
            return results;
        }

        if (secondWord == null || secondWord.trim().isEmpty()) {
            controller.debug("No second word entered.");
            return results;
        }

        String originalFirstWord = firstWord;
        String originalSecondWord = secondWord;

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
}