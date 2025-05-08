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

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.saidone.model.config.CollectorConfig;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
@Slf4j
public class NodeListCollector extends AbstractNodeCollector {

    @Override
    public void collectNodes(CollectorConfig config) {
        /* get list of node-id from a file */
        if (Strings.isNotBlank((String) config.getArg("nodeListFile"))) {
            try {
                for (var i : Files.readAllLines(new File((String) config.getArg("nodeListFile")).toPath())) {
                    queue.put(i);
                }
            } catch (InterruptedException | IOException e) {
                log.trace(e.getMessage(), e);
                log.warn(e.getMessage());
            }
        }
    }

}
