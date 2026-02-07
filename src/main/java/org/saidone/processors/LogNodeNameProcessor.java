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
import org.saidone.model.config.ProcessorConfig;
import org.springframework.stereotype.Component;

/**
 * Logs the name of each processed node.
 */
@Component
@Slf4j
public class LogNodeNameProcessor extends AbstractNodeProcessor {

    /**
     * Retrieves the node and logs its name.
     *
     * @param nodeId id of the node
     * @param config processor configuration
     */
    @Override
    @SneakyThrows
    public void processNode(String nodeId, ProcessorConfig config) {
        val node = getNode(nodeId);
        log.debug("node name --> {}", node.getName());
    }

}
