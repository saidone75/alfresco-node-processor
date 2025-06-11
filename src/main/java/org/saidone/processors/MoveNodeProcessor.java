package org.saidone.processors;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.core.model.NodeBodyMove;
import org.saidone.model.config.ProcessorConfig;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MoveNodeProcessor extends AbstractNodeProcessor {

    @Override
    public void processNode(String nodeId, ProcessorConfig config) {
        var moveBody = new NodeBodyMove();
        if (config.getArg("targetParentId") != null) {
            moveBody.setTargetParentId((String) config.getArg("targetParentId"));
        }
        if (config.getArg("targetPath") != null) {
            moveBody.setTargetPath((String) config.getArg("targetPath"));
        }
        log.debug("moving node --> {} to --> {}", nodeId, moveBody);
        if (config.getReadOnly() != null && !config.getReadOnly()) {
            nodesApi.moveNode(nodeId, moveBody, null, null);
        }
    }
}
