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
import org.apache.logging.log4j.util.Strings;
import org.saidone.model.config.CollectorConfig;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Collects node identifiers from a text file where each line contains a node
 * id.
 */
@Component
@Slf4j
public class NodeListCollector extends AbstractNodeCollector {

    public static final String NODE_LIST_ARG = "node-list-file";

    /**
     * Reads node identifiers from the file specified by the
     * {@code node-list-file} argument and enqueues them for processing.
     *
     * @param config collector configuration
     */
    @Override
    public void collectNodes(CollectorConfig config) {
        if (Strings.isNotBlank((String) config.getArg(NODE_LIST_ARG))) {
            try {
                for (val i : Files.readAllLines(new File((String) config.getArg(NODE_LIST_ARG)).toPath())) {
                    queue.put(i);
                }
            } catch (InterruptedException | IOException e) {
                log.trace(e.getMessage(), e);
                log.warn(e.getMessage());
            }
        }
    }

}
