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

package org.saidone.processors;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.handler.TrashcanApi;
import org.saidone.model.config.ProcessorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Permanently removes nodes from Alfresco trashcan.
 */
@Component
@Slf4j
public class TrashcanNodeProcessor extends AbstractNodeProcessor {

    @Autowired
    private TrashcanApi trashcanApi;

    /**
     * Deletes the provided deleted-node identifier from Alfresco trashcan.
     *
     * @param nodeId id of the node to remove from trashcan
     * @param config processor configuration
     */
    @Override
    @SneakyThrows
    public void processNode(String nodeId, ProcessorConfig config) {
        nodeId = nodeId.replaceAll(".*/", "");
        log.debug("node id --> {}", nodeId);
        trashcanApi.deleteDeletedNode(nodeId);
    }

}
