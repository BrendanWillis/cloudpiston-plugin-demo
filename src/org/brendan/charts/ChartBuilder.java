package org.brendan.charts;

import com.nxlight.framework.pal.workflow.common.*;
import com.nxlight.framework.pal.workflow.common.PacketDataRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.ZipInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;

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

        controller.debug("+++ CHART BUILDER STARTED ===");
        controller.debug("Search word: " + searchWord);

        // Write a debug message so we know the method ran
        controller.debug("ChartBuilder started");

        // Show which word was requested
        controller.debug("Search word: " + searchWord);

        // Store running totals for each book.
        // Key = book name, value = usage count.
        HashMap<String, Integer> bookCounts =
                new HashMap<String, Integer>();

        // Normalize the search word so capitalization and punctuatoin do not affect the search.
        // Example: "Lord" becomes "lord".
        String normalizedSearchWord = normalizeForSearch(searchWord);
        
        //open the EPUB file from the plugin resources.
        InputStream stream = getClass().getResourceAsStream(EPUB_PATH);

        // If java cannot find the EPUB, log the problem and return empty chart data.
        if(stream == null) {
            controller.debug("EPUB NOT FOUND:" + EPUB_PATH);
        } else {

            try {
                // EPUB files are zip files internally.
                // ZipInputStream lets us read each file inside the EPUB one at a time.
                ZipInputStream zip = new ZipInputStream (stream);

                ZipEntry entry;

                // Loop through every file inside the EPUB.
                while ((entry = zip.getNextEntry()) != null) {

                    // We only want scripture chapter files.
                    // Ski anything that is not XHTML.
                    if (!entry.getName().endsWith(".xhtml")) {
                        continue;
                    }

                    if (!entry.getName().contains("/Text/")) {
                        continue;
                    }

                    // For now, print each XHTML file so we know the EPUB loop works.
//                    controller.debug("chartBuilder XHTML file: " + entry.getName());

                    // Read the XHTML file into a String
                    String fileText = readZipEntry(zip);

                    // Remove HTML tags so we can only search scripture text.
                    String plainText = cleanHtml(fileText);

                    // Normalize the scripture text for searching.
                    String searchableText = normalizeForSearch(plainText);

                    // Count how many times the search word appears in this file.
                    int matches = countMatches(searchableText, normalizedSearchWord);

                    // If there are no matches, do not add anything to the chart data.
                    if (matches == 0) {
                        continue;
                    }

                    // Get the book name from the filename.
                    String bookName = getBookNameFromFilename(entry.getName());

                    // Add this file's mathmatchesces to the running total for that book.
                    addToBookCount(bookCounts, bookName, matches);
                }
            } catch (IOException e) {
                throw new WorkflowException("Error building chart data from EPUB", e);
            }
        }

        // Print the HashMap so we can verify the totals.
        controller.debug(bookCounts.toString());

        // Create a DataList that will eventually hold chart results.
        PacketDataList chartData = controller.createDataList(
                "chartData",
                new String[]{"bookName", "usageCount"}
        );

        // Convert the HashMap into DataList rows.
        for (String bookName : bookCounts.keySet()) {

            // Create a new row in the DataList.
            PacketDataRecord row = chartData.insertRecord();

            // Store book name
            row.setDataValue("bookName", bookName);
            // Store usage count.
            row.setDataValue("usageCount", String.valueOf(bookCounts.get(bookName)));
        }

        controller.debug("Chart Data Record Count: "
        +chartData.getRecordCount());

        controller.debug("=== CHART BUILDER FINISHED ===");

        // Return the completed DataList.
        return chartData;
    }


    private void addToBookCount(HashMap<String, Integer> bookCounts,
                                String bookName,
                                int matches) {
        //if this book is already in teh Hashmap,
        // we need to add the new matches to the existing total.
        if (bookCounts.containsKey(bookName)) {
            // get the current total for this gbook.
            int currentCount = bookCounts.get(bookName);

            //add the new matches to the old total.
            int newCount = currentCount + matches;

            // store the updated toal back into the HashMap.
            bookCounts.put(bookName, newCount);
        }

        // if this book is not in the hashmap yet,
        // this is the first time we have found matches for it.
        else {
            // create a new entry for this book using the match count we just found.
            bookCounts.put(bookName, matches);
        }
    }

    private String getBookNameFromFilename(String filename){

        // If the filename is missing, return Unknown so the program does not crash
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
            name = filename.substring(slash +1);
        }
        // Remove the file extension.
        // Example:
        // 06897_000_isa_040.xhtml
        // becomes:
        // 06897_000_isa_040
        name = name.replace(".xhtml","");

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
        if(parts.length >= 1) {
            return getReadableBookName(parts[0]);
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
        if ("od".equals(code)) return "Official Declaration";

        // =====================
        // PEARL OF GREAT PRICE
        // =====================
        if ("moses".equals(code)) return "Moses";
        if ("abr".equals(code)) return "Abraham";
        if ("js-m".equals(code)) return "Joseph Smith—Matthew";
        if ("a-of-f".equals(code)) return "Articles of Faith";

        return code.replace("-", " ");
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

    private String normalizeForSearch(String text) {
        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int countMatches(String text, String searchTerm) {
        // If either valueis missing, there is nothing to count.
        if (text == null || searchTerm == null || searchTerm.length() ==0){
            return 0;
        }

        // split the clearned text into individual words.
        String[] words = text.split(" ");

        // This will store how many matchas we find.
        int count = 0;

        // Look at each word one at a time.
        for (String word : words) {

            // If teh current word exactly matches the search term, count it.
            if (word.equals(searchTerm)) {
                count++;
            }
        }

        // Return the total number of matches found.
        return count;
    }


}