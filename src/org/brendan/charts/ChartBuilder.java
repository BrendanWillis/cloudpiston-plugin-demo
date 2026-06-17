package org.brendan.charts;

import com.nxlight.framework.pal.workflow.common.CommonController;
import com.nxlight.framework.pal.workflow.common.PacketDataList;
import com.nxlight.framework.pal.workflow.common.DataList;
import com.nxlight.framework.pal.workflow.common.DataRecord;
import com.nxlight.framework.pal.workflow.common.WorkflowException;

public class ChartBuilder {
    // Store the PAL controller so we can write debug messages
    private CommonController controller;

    // constructor
    public ChartBuilder(CommonController controller) {
        this.controller = controller;
    }

    public PacketDataList buildWordUsageChart(String searchWord)
        throws WorkflowException {

        // Write a debug message so we know the method ran
        controller.debug("ChartBuilder started");
        // Show which word was requested
        controller.debug("Search word: " + searchWord);

        // Create a DataList that will eventually hold chart results
        PacketDataList chartData =
                new PacketDataList("chartData");

        // Create one row of test data
        DataRecord row1 = chartData.addRecord();

        // Set the book name
        row1.set("bookName", "Genesis");
        // Set the count
        row1.set("usageCount", 120);

        // Create another row
        DataRecord row2 = chartData.addRecord();

        row2.set("bookName", "Exodus");
        row2.set("usageCount"+ 95);

        // Return the completed DataList
        return chartData;
    }

    private String getBookNameFromFilename(String filename){

        // If the filename is missing, return Unknown so the program does not crash
        if (filename == null) {
            return "Unknown";
        }
        return filename;
    }

}