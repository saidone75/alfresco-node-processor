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

import lombok.Locked;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.model.NodeBodyMove;
import org.alfresco.core.model.NodeChildAssociationEntry;
import org.alfresco.core.model.NodeChildAssociationPaging;
import org.apache.logging.log4j.util.Strings;
import org.saidone.model.config.ProcessorConfig;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Moves nodes to the configured target parent node.
 */
@Component
@Slf4j
public class MoveNodeProcessor extends AbstractNodeProcessor {

    private static String targetParentId = null;

    private static final ConcurrentHashMap<String, AtomicInteger> movedNodes = new ConcurrentHashMap<>();
    private static boolean childrenRead = false;

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
            if (config.getArg("target-parent") != null) {
                targetParentId = (String) config.getArg("target-parent");
                if (!targetParentId.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
                    targetParentId = Objects.requireNonNull(nodesApi.getNode("-root-", null, targetParentId, null).getBody()).getEntry().getId();
                }
            } else {
                log.warn("target-parent must be set");
                return;
            }
        }

        if (!childrenRead) getChildren(targetParentId);

        val node = getNode(nodeId);
        var nodeName = node.getName();
        if (!movedNodes.containsKey(nodeName)) {
            movedNodes.put(nodeName, new AtomicInteger());
        } else {
            do {
                val name = nodeName.substring(0, nodeName.lastIndexOf("."));
                val extension = nodeName.substring(nodeName.lastIndexOf(".") + 1);
                nodeName = String.format("%s (%d).%s", name, movedNodes.get(nodeName).incrementAndGet(), extension);
            } while (movedNodes.containsKey(nodeName));
        }
        moveBody.setName(nodeName);
        moveBody.setTargetParentId(targetParentId);
        log.debug("moving node --> {} to --> {} as --> {}", nodeId, moveBody.getTargetParentId(), nodeName);
        if (config.getReadOnly() != null && !config.getReadOnly()) {
            nodesApi.moveNode(nodeId, moveBody, null, null);
        }
    }

    @Locked.Write
    private void getChildren(String nodeId) {
        if (childrenRead) return;
        int skipCount = 0;
        int batchSize = 100;
        var children = (NodeChildAssociationPaging) null;
        do {
            children = nodesApi.listNodeChildren(nodeId, skipCount, batchSize, null, null, null, null, null, null).getBody();
            if (children == null || children.getList() == null) {
                break;
            }
            for (val child : children.getList().getEntries().stream().map(NodeChildAssociationEntry::getEntry).toList()) {
                movedNodes.put(child.getName(), new AtomicInteger());
            }
            skipCount += batchSize;
        } while (children.getList().getPagination() != null && children.getList().getPagination().isHasMoreItems());
        childrenRead = true;
    }

}
