package org.brendan;

import com.nxlight.framework.pal.workflow.common.WorkflowException;
import com.nxlight.framework.pal.workflow.plugin.Plugin;

import org.brendan.random.RandomName;
import org.brendan.scriptures.ScriptureSearch;

public class Brendan extends Plugin {

    public String execute() throws WorkflowException {

        String module = payload.get("module");

        if ("scriptures".equals(module)) {

            ScriptureSearch scriptureSearch =
                    new ScriptureSearch(controller);

            return scriptureSearch.execute(payload);
        }

        if ("random".equals(module)) {

            RandomName randomName =
                    new RandomName(controller);

            return randomName.execute(payload);
        }

        controller.debug("No valid module selected: " + module);

        return "No valid module selected.";
    }
}