package org.brendan.charts;

import com.nxlight.framework.pal.workflow.common.CommonController;
import com.nxlight.framework.pal.workflow.common.DataList;
import com.nxlight.framework.pal.workflow.common.PacketDataList;
import com.nxlight.framework.pal.workflow.common.PacketDataRecord;
import com.nxlight.framework.pal.workflow.common.WorkflowException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ChartBuilder {

    // Store the PAL controller so we can write debug messages
    private CommonController controller;

    private static final String EPUB_PATH =
            "/scriptures/standard-works-complete-epub-eng.epub";

    // constructor
    public ChartBuilder(CommonController controller) {
        this.controller = controller;
    }

    public DataList buildWordUsageChart(String searchWord)
            throws WorkflowException {

        controller.debug("=== CHART BUILDER STARTED ===");
        controller.debug("Search word: " + searchWord);

        // Create a DataList that will eventually hold chart results.
        // This is created early so we can safely return an empty list if something is wrong.
        PacketDataList chartData = controller.createDataList(
                "chartData",
                new String[]{"bookName", "usageCount"}
        );

        // Normalize the search word so capitalization and punctuation do not affect the search.
        // Example: "Lord" becomes "lord".
        String normalizedSearchWord = normalizeForSearch(searchWord);

        // If the user did not enter a real search word, stop before reading the EPUB.
        if (normalizedSearchWord == null || normalizedSearchWord.length() == 0) {
            controller.debug("No valid search word was entered.");
            return chartData;
        }

        // Store running totals for each book.
        // Key = book name, value = usage count.
        HashMap<String, Integer> bookCounts =
                new HashMap<String, Integer>();

        // Open the EPUB file from the plugin resources.
        InputStream stream = getClass().getResourceAsStream(EPUB_PATH);

        // If Java cannot find the EPUB, log the problem and return empty chart data.
        if (stream == null) {
            controller.debug("EPUB NOT FOUND: " + EPUB_PATH);
            return chartData;
        }

        try {
            // EPUB files are zip files internally.
            // ZipInputStream lets us read each file inside the EPUB one at a time.
            ZipInputStream zip = new ZipInputStream(stream);

            ZipEntry entry;

            // Loop through every file inside the EPUB.
            while ((entry = zip.getNextEntry()) != null) {

                String fileName = entry.getName();

                // We only want scripture chapter files.
                // Skip anything that is not XHTML.
                if (!fileName.endsWith(".xhtml")) {
                    continue;
                }

                // Only read files inside the Text folder.
                if (!fileName.contains("/Text/")) {
                    continue;
                }

                // Only read the standard scripture files that use this filename pattern.
                if (!fileName.contains("06897_000_")) {
                    continue;
                }

                // Skip intro/table of contents/title page files so the chart focuses on scripture text.
                if (fileName.contains("introduction") ||
                        fileName.contains("toc") ||
                        fileName.contains("title-page")) {
                    continue;
                }

                // Read the XHTML file into a String.
                String fileText = readZipEntry(zip);

                // TEMP DEBUG: inspect Genesis 3 XHTML structure.
                if (fileName.contains("06897_000_gen_003.xhtml")) {
                    controller.debug("RAW GENESIS 3 XHTML:");
                    controller.debug(fileText.substring(0, Math.min(fileText.length(), 3000)));
                }

                // Remove HTML tags so we can only search scripture text.
                String plainText = cleanHtml(fileText);

                // Normalize the scripture text for searching.
                String searchableText = normalizeForSearch(plainText);

                // Count how many times the search word appears in this file.
                int matches = countMatches(searchableText, normalizedSearchWord);

                // Get the book name from the filename.
                String bookName = getBookNameFromFilename(fileName);

                // If there are no matches, do not add anything to the chart data.
                if (matches == 0) {
                    continue;
                }

                // Debug why Genesis contains this search word.
                if ("Genesis".equals(bookName)) {

                    controller.debug(
                            "========== GENESIS DEBUG =========="
                                    + "\nFile: " + fileName
                                    + "\nMatches: " + matches
                                    + "\nSearch Word: " + normalizedSearchWord
                                    + "\nSnippet:"
                                    + "\n" + makeSnippet(searchableText, normalizedSearchWord)
                    );
                }
                String summaryDebug = cleanHtml(extractChapterSummary(fileText));

                if (fileName.contains("06897_000_gen_003.xhtml")) {
                    controller.debug("GENESIS 3 SUMMARY ONLY:\n" + summaryDebug);
                }

                String verseDebug = cleanHtml(extractVerseText(fileText));

                if (fileName.contains("06897_000_gen_003.xhtml")) {
                    controller.debug("GENESIS 3 VERSES ONLY:\n" + verseDebug.substring(0, Math.min(verseDebug.length(), 500)));
                }

                // Add this file's matches to the running total for that book.
                addToBookCount(bookCounts, bookName, matches);

                // Debug each file that has matches so we can verify the search is working.
                controller.debug(
                        "File: " + fileName
                                + "\nBook: " + bookName
                                + "\nMatches: " + matches
                                + "\nSnippet:\n" + makeSnippet(searchableText, normalizedSearchWord)
                );
            }

            // Close the zip stream after we finish reading the EPUB.
            zip.close();

        } catch (IOException e) {
            throw new WorkflowException("Error building chart data from EPUB", e);
        }

        // Get the books in scripture order so the chart does not display randomly.
        List<String> canonicalBooks =
                getCanonicalBookOrder();

        // Convert the HashMap into DataList rows.
        for (String bookName : canonicalBooks) {

            if (!bookCounts.containsKey(bookName)) {
                continue;
            }

            // Create a new row in the DataList.
            PacketDataRecord row = chartData.insertRecord();

            // Store book name.
            row.setDataValue("bookName", bookName);

            // Store usage count.
            row.setDataValue("usageCount", String.valueOf(bookCounts.get(bookName)));
        }

        controller.debug("Chart Data Record Count: " + chartData.getRecordCount());
        controller.debug("Book Counts: " + bookCounts);
        controller.debug("=== CHART BUILDER FINISHED ===");

        // Return the completed DataList.
        return chartData;
    }

    private String makeSnippet(String text, String searchWord) {

        // If either value is missing, we cannot create a snippet.
        if (text == null || searchWord == null) {
            return "No snippet found.";
        }

        int index = text.indexOf(searchWord);

        if (index == -1) {
            return "No snippet found.";
        }

        int start = Math.max(0, index - 60);
        int end = Math.min(text.length(), index + searchWord.length() + 60);

        return "..." + text.substring(start, end) + "...";
    }

    private void addToBookCount(HashMap<String, Integer> bookCounts,
                                String bookName,
                                int matches) {
        // If this book is already in the HashMap,
        // we need to add the new matches to the existing total.
        if (bookCounts.containsKey(bookName)) {

            // Get the current total for this book.
            int currentCount = bookCounts.get(bookName);

            // Add the new matches to the old total.
            int newCount = currentCount + matches;

            // Store the updated total back into the HashMap.
            bookCounts.put(bookName, newCount);
        }

        // If this book is not in the HashMap yet,
        // this is the first time we have found matches for it.
        else {

            // Create a new entry for this book using the match count we just found.
            bookCounts.put(bookName, matches);
        }
    }

    private String getBookNameFromFilename(String filename) {

        // If the filename is missing, return Unknown so the program does not crash.
        if (filename == null) {
            return "Unknown";
        }

        // Remove the folder path.
        // Example:
        // OEBPS/Text/bible/06897_000_isa_040.xhtml
        // becomes:
        // 06897_000_isa_040.xhtml
        int slash = filename.lastIndexOf("/");
        String name = filename;

        if (slash >= 0) {
            name = filename.substring(slash + 1);
        }

        // Remove the file extension.
        // Example:
        // 06897_000_isa_040.xhtml
        // becomes:
        // 06897_000_isa_040
        name = name.replace(".xhtml", "");

        // Remove the EPUB prefix.
        // Example:
        // 06897_000_isa_040
        // becomes:
        // isa_040
        name = name.replace("06897_000_", "");

        // Split the remaining filename into parts.
        // Example:
        // isa_040 becomes ["isa", "040"]
        String[] parts = name.split("_");

        // The first part is the book code.
        if (parts.length >= 1) {
            String bookCode = parts[0];
            String bookName = getReadableBookName(bookCode);

            // Debug only when we find a code that is probably not mapped yet.
            if (bookName.equals(bookCode.replace("-", " "))) {
                try {
                    controller.debug("Unmapped book code: " + bookCode);
                    controller.debug("Filename: " + filename);
                } catch (WorkflowException e) {
                    // Ignore debug errors so they do not break compilation.
                }
            }

            return bookName;
        }

        return name;
    }

    private String getReadableBookName(String code) {

        // =====================
        // OLD TESTAMENT
        // =====================
        if ("gen".equals(code)) return "Genesis";
        if ("ex".equals(code)) return "Exodus";
        if ("lev".equals(code)) return "Leviticus";
        if ("num".equals(code)) return "Numbers";
        if ("deut".equals(code)) return "Deuteronomy";
        if ("josh".equals(code)) return "Joshua";
        if ("judg".equals(code)) return "Judges";
        if ("ruth".equals(code)) return "Ruth";
        if ("1-sam".equals(code)) return "1 Samuel";
        if ("2-sam".equals(code)) return "2 Samuel";
        if ("1-kgs".equals(code)) return "1 Kings";
        if ("2-kgs".equals(code)) return "2 Kings";
        if ("1-chr".equals(code)) return "1 Chronicles";
        if ("2-chr".equals(code)) return "2 Chronicles";
        if ("ezra".equals(code)) return "Ezra";
        if ("neh".equals(code)) return "Nehemiah";
        if ("esth".equals(code)) return "Esther";
        if ("job".equals(code)) return "Job";
        if ("ps".equals(code)) return "Psalms";
        if ("prov".equals(code)) return "Proverbs";
        if ("eccl".equals(code)) return "Ecclesiastes";
        if ("song".equals(code)) return "Song of Solomon";
        if ("isa".equals(code)) return "Isaiah";
        if ("jer".equals(code)) return "Jeremiah";
        if ("lam".equals(code)) return "Lamentations";
        if ("ezek".equals(code)) return "Ezekiel";
        if ("dan".equals(code)) return "Daniel";
        if ("hosea".equals(code)) return "Hosea";
        if ("joel".equals(code)) return "Joel";
        if ("amos".equals(code)) return "Amos";
        if ("obad".equals(code)) return "Obadiah";
        if ("jonah".equals(code)) return "Jonah";
        if ("micah".equals(code)) return "Micah";
        if ("nahum".equals(code)) return "Nahum";
        if ("hab".equals(code)) return "Habakkuk";
        if ("zeph".equals(code)) return "Zephaniah";
        if ("hag".equals(code)) return "Haggai";
        if ("zech".equals(code)) return "Zechariah";
        if ("mal".equals(code)) return "Malachi";

        // =====================
        // NEW TESTAMENT
        // =====================
        if ("matt".equals(code)) return "Matthew";
        if ("mark".equals(code)) return "Mark";
        if ("luke".equals(code)) return "Luke";
        if ("john".equals(code)) return "John";
        if ("acts".equals(code)) return "Acts";
        if ("rom".equals(code)) return "Romans";
        if ("1-cor".equals(code)) return "1 Corinthians";
        if ("2-cor".equals(code)) return "2 Corinthians";
        if ("gal".equals(code)) return "Galatians";
        if ("eph".equals(code)) return "Ephesians";
        if ("philip".equals(code)) return "Philippians";
        if ("col".equals(code)) return "Colossians";
        if ("1-thes".equals(code)) return "1 Thessalonians";
        if ("2-thes".equals(code)) return "2 Thessalonians";
        if ("1-tim".equals(code)) return "1 Timothy";
        if ("2-tim".equals(code)) return "2 Timothy";
        if ("titus".equals(code)) return "Titus";
        if ("philem".equals(code)) return "Philemon";
        if ("heb".equals(code)) return "Hebrews";
        if ("james".equals(code)) return "James";
        if ("1-pet".equals(code)) return "1 Peter";
        if ("2-pet".equals(code)) return "2 Peter";
        if ("1-jn".equals(code)) return "1 John";
        if ("2-jn".equals(code)) return "2 John";
        if ("3-jn".equals(code)) return "3 John";
        if ("jude".equals(code)) return "Jude";
        if ("rev".equals(code)) return "Revelation";

        // =====================
        // BOOK OF MORMON
        // =====================
        if ("bofm".equals(code)) return "Book of Mormon Title Page";
        if ("1-ne".equals(code)) return "1 Nephi";
        if ("2-ne".equals(code)) return "2 Nephi";
        if ("jacob".equals(code)) return "Jacob";
        if ("enos".equals(code)) return "Enos";
        if ("jarom".equals(code)) return "Jarom";
        if ("omni".equals(code)) return "Omni";
        if ("w-of-m".equals(code)) return "Words of Mormon";
        if ("mosiah".equals(code)) return "Mosiah";
        if ("alma".equals(code)) return "Alma";
        if ("hel".equals(code)) return "Helaman";
        if ("3-ne".equals(code)) return "3 Nephi";
        if ("4-ne".equals(code)) return "4 Nephi";
        if ("morm".equals(code)) return "Mormon";
        if ("ether".equals(code)) return "Ether";
        if ("moro".equals(code)) return "Moroni";

        // =====================
        // DOCTRINE AND COVENANTS
        // =====================
        if ("dc".equals(code)) return "Doctrine and Covenants";

        // Some EPUB files may use this code for D&C section files.
        if ("dc-testament".equals(code)) return "Doctrine and Covenants";

        if ("od".equals(code)) return "Official Declaration";

        // =====================
        // PEARL OF GREAT PRICE
        // =====================

        // Some EPUB files may use this code for the Pearl of Great Price wrapper.
        if ("pgp".equals(code)) return "Pearl of Great Price";

        if ("moses".equals(code)) return "Moses";
        if ("abr".equals(code)) return "Abraham";
        if ("js-m".equals(code)) return "Joseph Smith—Matthew";
        if ("a-of-f".equals(code)) return "Articles of Faith";

        return code.replace("-", " ");
    }

    private List<String> getCanonicalBookOrder() {

        List<String> books = new ArrayList<String>();

        // Old Testament
        books.add("Genesis");
        books.add("Exodus");
        books.add("Leviticus");
        books.add("Numbers");
        books.add("Deuteronomy");
        books.add("Joshua");
        books.add("Judges");
        books.add("Ruth");
        books.add("1 Samuel");
        books.add("2 Samuel");
        books.add("1 Kings");
        books.add("2 Kings");
        books.add("1 Chronicles");
        books.add("2 Chronicles");
        books.add("Ezra");
        books.add("Nehemiah");
        books.add("Esther");
        books.add("Job");
        books.add("Psalms");
        books.add("Proverbs");
        books.add("Ecclesiastes");
        books.add("Song of Solomon");
        books.add("Isaiah");
        books.add("Jeremiah");
        books.add("Lamentations");
        books.add("Ezekiel");
        books.add("Daniel");
        books.add("Hosea");
        books.add("Joel");
        books.add("Amos");
        books.add("Obadiah");
        books.add("Jonah");
        books.add("Micah");
        books.add("Nahum");
        books.add("Habakkuk");
        books.add("Zephaniah");
        books.add("Haggai");
        books.add("Zechariah");
        books.add("Malachi");

        // New Testament
        books.add("Matthew");
        books.add("Mark");
        books.add("Luke");
        books.add("John");
        books.add("Acts");
        books.add("Romans");
        books.add("1 Corinthians");
        books.add("2 Corinthians");
        books.add("Galatians");
        books.add("Ephesians");
        books.add("Philippians");
        books.add("Colossians");
        books.add("1 Thessalonians");
        books.add("2 Thessalonians");
        books.add("1 Timothy");
        books.add("2 Timothy");
        books.add("Titus");
        books.add("Philemon");
        books.add("Hebrews");
        books.add("James");
        books.add("1 Peter");
        books.add("2 Peter");
        books.add("1 John");
        books.add("2 John");
        books.add("3 John");
        books.add("Jude");
        books.add("Revelation");

        // Book of Mormon
        books.add("Book of Mormon Title Page");
        books.add("1 Nephi");
        books.add("2 Nephi");
        books.add("Jacob");
        books.add("Enos");
        books.add("Jarom");
        books.add("Omni");
        books.add("Words of Mormon");
        books.add("Mosiah");
        books.add("Alma");
        books.add("Helaman");
        books.add("3 Nephi");
        books.add("4 Nephi");
        books.add("Mormon");
        books.add("Ether");
        books.add("Moroni");

        // Doctrine and Covenants
        books.add("Doctrine and Covenants");
        books.add("Official Declaration");

        // Pearl of Great Price
        books.add("Pearl of Great Price");
        books.add("Moses");
        books.add("Abraham");
        books.add("Joseph Smith—Matthew");
        books.add("Articles of Faith");

        return books;
    }

    private String readZipEntry(ZipInputStream zip) throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int length;

        while ((length = zip.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }

        byte[] bytes = output.toByteArray();

        // Check for UTF-16 little endian encoding.
        if (bytes.length >= 2) {

            if ((bytes[0] == (byte) 0xFF) && (bytes[1] == (byte) 0xFE)) {
                return new String(bytes, StandardCharsets.UTF_16LE);
            }

            // Check for UTF-16 big endian encoding.
            if ((bytes[0] == (byte) 0xFE) && (bytes[1] == (byte) 0xFF)) {
                return new String(bytes, StandardCharsets.UTF_16BE);
            }
        }

        // Default to UTF-8, which is what most XHTML files use.
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String extractChapterSummary(String html) {
        // If the HTML is missing, return an empty string so the program does not crash.
        if (html == null) {
            return "";
        }

        // In this EPUB, the chapter summary is stored inside the <h3> tag.
        int start = html.indexOf("<h3>");
        int end = html.indexOf("</h3>");

        // If this file does not have a chapter summary, return an empty string.
        if (start == -1 || end == -1 || end <= start) {
            return "";
        }

        // Move start past the opening <h3> tag.
        start = start + "<h3>".length();

        // Return only the summary text.
        return html.substring(start, end);
    }

    private String extractVerseText(String html) {

        // If the HTML is missing, return an empty string so the program does not crash.
        if (html == null) {
            return "";
        }

        // In this EPUB, verses start after the chapter summary's closing </h3> tag.
        int summaryEnd = html.indexOf("</h3>");

        // If there is no summary, return the full HTML so older or different files still work.
        if (summaryEnd == -1) {
            return html;
        }

        // Move past the closing </h3> tag so only verse text remains.
        return html.substring(summaryEnd + "</h3>".length());
    }

    private String cleanHtml(String html) {

        // If the HTML is missing, return an empty string so the program does not crash.
        if (html == null) {
            return "";
        }

        return html
                .replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeForSearch(String text) {

        // If the text is missing, return an empty string so the program does not crash.
        if (text == null) {
            return "";
        }

        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int countMatches(String text, String searchTerm) {

        // If either value is missing, there is nothing to count.
        if (text == null || searchTerm == null || searchTerm.length() == 0) {
            return 0;
        }

        // Split the cleaned text into individual words.
        String[] words = text.split(" ");

        // This will store how many matches we find.
        int count = 0;

        // Look at each word one at a time.
        for (String word : words) {

            // If the current word exactly matches the search term, count it.
            if (word.equals(searchTerm)) {
                count++;
            }
        }

        // Return the total number of matches found.
        return count;
    }
}