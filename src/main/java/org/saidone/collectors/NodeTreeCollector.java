/*
 * Alfresco Node Processor - Do things with nodes
 * Copyright (C) 2023-2024 Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.saidone.collectors;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.core.handler.NodesApi;
import org.alfresco.core.model.NodeChildAssociationEntry;
import org.alfresco.core.model.NodeChildAssociationPaging;
import org.saidone.model.config.CollectorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
public class NodeTreeCollector extends AbstractNodeCollector {

    private int batchSize = 100;

    @Autowired
    private NodesApi nodesApi;

    @SneakyThrows
    private void walk(String nodeId) {
        int skipCount = 0;
        NodeChildAssociationPaging children;
        do {
            children = nodesApi.listNodeChildren(nodeId, skipCount, batchSize, null, null, null, null, null, null).getBody();
            for (NodeChildAssociationEntry entry : children.getList().getEntries()) {
                var child = entry.getEntry();
                queue.put(child.getId());
                if (Boolean.TRUE.equals(child.getIsFolder())) {
                    walk(child.getId());
                }
            }
            skipCount += batchSize;
        } while (children.getList().getPagination().isHasMoreItems());
    }

    @Override
    public void collectNodes(CollectorConfig config) {
        if (config.getArg("list-batch-size") != null) this.batchSize = (int) config.getArg("list-batch-size");

        String nodeId = (String) config.getArg("nodeId");
        if (nodeId == null && config.getArg("path") != null) {
            try {
                nodeId = Objects.requireNonNull(nodesApi.getNode("-root-", null, (String) config.getArg("path"), null).getBody()).getEntry().getId();
            } catch (Exception e) {
                log.trace(e.getMessage(), e);
                log.warn(e.getMessage());
            }
        }
        if (nodeId != null) {
            walk(nodeId);
        }
    }
}
