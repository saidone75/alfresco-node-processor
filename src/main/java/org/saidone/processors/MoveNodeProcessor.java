/*
 *  Alfresco Node Processor - Do things with nodes
 *  Copyright (C) 2023-2025 Saidone
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.saidone.processors;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.model.NodeBodyMove;
import org.apache.logging.log4j.util.Strings;
import org.saidone.model.config.ProcessorConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Moves nodes to the configured target parent node.
 */
@Component
@Slf4j
public class MoveNodeProcessor extends AbstractNodeProcessor {

    private static String targetParentId = null;
    private static String targetPath = null;

    /**
     * Moves the node to the target parent defined in the configuration.
     *
     * @param nodeId id of the node to move
     * @param config processor configuration
     */
    @Override
    public void processNode(String nodeId, ProcessorConfig config) {
        val moveBody = new NodeBodyMove();
        if (Strings.isBlank(targetParentId)) {
            if (config.getArg("target-parent-id") != null) {
                targetParentId = (String) config.getArg("target-parent-id");
                targetPath = getNode(targetParentId, List.of("path")).getPath().getName();
            }
            if (config.getArg("target-path") != null) {
                targetPath = (String) config.getArg("target-path");
                targetParentId = Objects.requireNonNull(nodesApi.getNode("-root-", null, targetPath, null).getBody()).getEntry().getId();
            }
        }
        val mode = (String) config.getArg("mode");
        if (Strings.isNotBlank(mode)) {

        }
        log.debug("moving node --> {} to --> {}", nodeId, moveBody.getTargetParentId());
        moveBody.setTargetParentId(targetParentId);
        if (config.getReadOnly() != null && !config.getReadOnly()) {
            nodesApi.moveNode(nodeId, moveBody, null, null);
        }
    }

}
