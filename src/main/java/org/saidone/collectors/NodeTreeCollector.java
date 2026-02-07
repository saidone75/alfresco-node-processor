/*
 *  Alfresco Node Processor - Do things with nodes
 *  Copyright (C) 2023-2026 Saidone
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

package org.saidone.collectors;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.handler.NodesApi;
import org.alfresco.core.model.NodeChildAssociationEntry;
import org.alfresco.core.model.NodeChildAssociationPaging;
import org.saidone.model.config.CollectorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Walks a node tree starting from a given root node or path and collects the
 * identifiers of all descendant nodes.
 */
@Component
@Slf4j
public class NodeTreeCollector extends AbstractNodeCollector {

    private int batchSize = 100;

    @Autowired
    private NodesApi nodesApi;

    private void walk(String rootNodeId) {
        val nodeStack = new ArrayDeque<String>();
        nodeStack.push(rootNodeId);
        while (!nodeStack.isEmpty()) {
            val nodeId = nodeStack.pop();
            try {
                processNodeChildren(nodeId, nodeStack);
            } catch (Exception e) {
                log.error("Error processing node {}: {}", nodeId, e.getMessage(), e);
            }
        }
    }

    private void processNodeChildren(String nodeId, Deque<String> nodeStack) throws InterruptedException {
        int skipCount = 0;
        NodeChildAssociationPaging children;
        do {
            children = nodesApi.listNodeChildren(nodeId, skipCount, batchSize, null, null, null, null, null, null).getBody();
            if (children == null || children.getList() == null) {
                break;
            }
            for (val child : children.getList().getEntries().stream().map(NodeChildAssociationEntry::getEntry).toList()) {
                if (child.isIsFolder()) {
                    nodeStack.push(child.getId());
                } else {
                    queue.put(child.getId());
                }
            }
            skipCount += batchSize;
        } while (children.getList().getPagination() != null && children.getList().getPagination().isHasMoreItems());
    }

    /**
     * Traverses the node tree starting from the root defined by
     * {@code node-id} or {@code path} arguments and queues descendant node
     * identifiers.
     *
     * @param config collector configuration
     */
    @Override
    public void collectNodes(CollectorConfig config) {
        if (config.getArg("batch-size") != null) this.batchSize = (int) config.getArg("batch-size");
        var nodeId = (String) config.getArg("node-id");
        // Path resolution if node-id is not provided
        if (nodeId == null && config.getArg("path") != null) {
            try {
                var response = nodesApi.getNode("-root-", null, (String) config.getArg("path"), null);
                if (response != null && response.getBody() != null && response.getBody().getEntry() != null) {
                    nodeId = response.getBody().getEntry().getId();
                } else {
                    log.error("No node found for path: {}", config.getArg("path"));
                }
            } catch (Exception e) {
                log.error("Error resolving path {}: {}", config.getArg("path"), e.getMessage(), e);
            }
        }
        if (nodeId != null) {
            walk(nodeId);
        } else {
            log.error("Root node ID not found");
        }
    }

}