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

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.handler.TrashcanApi;
import org.saidone.model.config.ProcessorConfig;
import org.springframework.stereotype.Component;

/**
 * Processes deleted nodes from Alfresco trashcan.
 *
 * <p>Supported operations are configured through processor argument {@code op}:</p>
 * <ul>
 *   <li>{@code delete} (default): permanently removes the node from trashcan</li>
 *   <li>{@code restore}: restores the node to its original location</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrashcanNodeProcessor extends AbstractNodeProcessor {

    private final TrashcanApi trashcanApi;

    private static final String OP = "op";
    private static final String OP_DELETE = "delete";
    private static final String OP_RESTORE = "restore";

    /**
     * Executes the configured operation on the provided deleted-node identifier.
     *
     * @param nodeId id of the deleted node in trashcan
     * @param config processor configuration where {@code args.op} is {@code delete} or {@code restore}
     */
    @Override
    @SneakyThrows
    public void processNode(String nodeId, ProcessorConfig config) {
        val op = config.getArgs().getOrDefault(OP, OP_DELETE);
        log.debug("deleting node --> {}", nodeId);
        if (OP_RESTORE.equals(op)) {
            log.info("restoring node --> {}", nodeId);
            trashcanApi.restoreDeletedNode(nodeId, null, null);
        } else if (OP_DELETE.equals(op)) {
            log.info("deleting node --> {}", nodeId);
            trashcanApi.deleteDeletedNode(nodeId);
        } else {
            log.warn("invalid operation --> {}", op);
        }
    }

}