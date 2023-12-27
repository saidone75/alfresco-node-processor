package org.saidone.collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.saidone.model.config.CollectorConfig;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
@Slf4j
public class NodeListCollector extends AbstractNodeCollector {

    @Override
    public void collectNodes(CollectorConfig config) {
        /* get list of node-id from a file */
        if (Strings.isNotBlank((String) config.getArg("nodeListFile"))) {
            try {
                for (var i : Files.readAllLines(new File((String) config.getArg("nodeListFile")).toPath())) {
                    queue.put(i);
                }
            } catch (InterruptedException | IOException e) {
                log.trace(e.getMessage(), e);
                log.warn(e.getMessage());
            }
        }
    }

}
