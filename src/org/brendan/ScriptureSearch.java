package org.brendan;

import com.nxlight.framework.pal.workflow.common.CommonController;
import com.nxlight.framework.pal.workflow.common.WorkflowException;

import java.io.InputStream;

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
}