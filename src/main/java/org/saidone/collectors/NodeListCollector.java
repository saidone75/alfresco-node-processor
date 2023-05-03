package org.saidone.collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.util.Strings;
import org.saidone.model.config.CollectorConfig;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Slf4j
public class NodeListCollector extends AbstractNodeCollector {

    @Override
    public void collectNodes(CollectorConfig config) {
        /* get list of node-id from a file */
        if (Strings.isNotBlank((String) config.getArg("nodeListFile"))) {
            try {
                for (var i : FileUtils.readLines(new File((String) config.getArg("nodeListFile")))) {
                    queue.put(i);
                }
            } catch (InterruptedException | IOException e) {
                if (log.isTraceEnabled()) e.printStackTrace();
                log.warn(e.getMessage());
            }
        }
    }

}
