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
import org.saidone.model.config.ProcessorConfig;
import org.springframework.stereotype.Component;

/**
 * Deletes each node passed to the processor.
 */
@Component
@Slf4j
public class DeleteNodeProcessor extends AbstractNodeProcessor {

    /**
     * Deletes the node identified by {@code nodeId}.
     *
     * @param nodeId id of the node to delete
     * @param config processor configuration
     */
    @Override
    public void processNode(String nodeId, ProcessorConfig config) {
        log.debug("deleting node --> {}", nodeId);
        if (!readOnly) {
            nodesApi.deleteNode(nodeId, config.getArg("permanent") != null ? (Boolean) config.getArg("permanent") : false);
        }
    }

}
