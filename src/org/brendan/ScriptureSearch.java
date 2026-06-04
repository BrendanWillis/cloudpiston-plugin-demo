package org.brendan;

import com.nxlight.framework.pal.workflow.common.CommonController;
import com.nxlight.framework.pal.workflow.common.WorkflowException;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ScriptureSearch {

    private static final String EPUB_PATH =
            "/scriptures/standard-works-complete-epub-eng.epub";

    private CommonController controller;

    public ScriptureSearch(CommonController controller) {
        this.controller = controller;
    }

    public void testResource() throws WorkflowException {

        InputStream stream =
                getClass().getResourceAsStream(EPUB_PATH);

        if (stream == null) {
            controller.debug("EPUB NOT FOUND: " + EPUB_PATH);
        } else {
            controller.debug("EPUB FOUND: " + EPUB_PATH);
        }
    }
    public void listXhtmlFiles() throws WorkflowException {

        InputStream stream =
                getClass().getResourceAsStream(EPUB_PATH);

        if (stream == null) {
            controller.debug("EPUB NOT FOUND: " + EPUB_PATH);
            return;
        }

        try {
            ZipInputStream zipStream =
                    new ZipInputStream(stream);

            ZipEntry entry;

            while ((entry = zipStream.getNextEntry()) != null) {

                String fileName = entry.getName();

                if (fileName.endsWith(".xhtml")) {
                    controller.debug("XHTML FOUND: " + fileName);

                    String content = readZipEntry(zipStream);

                    int previewLength = Math.min(content.length(), 300);

                    controller.debug(
                            "XHTML PREVIEW: " +
                                    content.substring(0, previewLength)
                    );

                    break;
                }
            }

            zipStream.close();

        } catch (IOException e) {
            throw new WorkflowException("Error reading EPUB file", e);
        }
    }
    private String readZipEntry(InputStream inputStream) throws IOException {

        byte[] buffer = new byte[1024];
        int length;

        StringBuilder builder = new StringBuilder();

        while ((length = inputStream.read(buffer)) != -1) {
            builder.append(new String(buffer, 0, length, "UTF-8"));
        }

        return builder.toString();
    }
    public void searchWord(String searchTerm, int maxResults)
            throws WorkflowException {

        InputStream stream =
                getClass().getResourceAsStream(EPUB_PATH);

        if (stream == null) {
            controller.debug("EPUB NOT FOUND: " + EPUB_PATH);
            return;
        }

        try {
            ZipInputStream zipStream =
                    new ZipInputStream(stream);

            ZipEntry entry;
            int matches = 0;

            String lowerSearchTerm = searchTerm.toLowerCase();

            while ((entry = zipStream.getNextEntry()) != null) {

                String fileName = entry.getName();

                if (fileName.endsWith(".xhtml")) {

                    String content = readZipEntry(zipStream);
                    String lowerContent = content.toLowerCase();

                    if (lowerContent.contains(lowerSearchTerm)) {
                        controller.debug("MATCH FOUND: " + fileName);
                        matches++;
                    }

                    if (matches >= maxResults) {
                        break;
                    }
                }
            }

            zipStream.close();

            controller.debug("Total matches found: " + matches);

        } catch (IOException e) {
            throw new WorkflowException("Error searching EPUB file", e);
        }
    }
}