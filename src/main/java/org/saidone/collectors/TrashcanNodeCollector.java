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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.handler.TrashcanApi;
import org.alfresco.core.model.DeletedNodesPaging;
import org.saidone.model.config.CollectorConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Collects node identifiers from Alfresco trashcan and enqueues them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrashcanNodeCollector extends AbstractNodeCollector {

    private int batchSize = 100;

    private final TrashcanApi trashcanApi;

    /**
     * Lists deleted nodes from Alfresco trashcan in batches and enqueues each
     * returned node identifier.
     *
     * @param config collector configuration
     */
    @Override
    public void collectNodes(CollectorConfig config) {
        if (config.getArg("batch-size") != null) this.batchSize = (int) config.getArg("batch-size");
        var skipCount = 0;
        DeletedNodesPaging deletedNodesPaging;
        do {
            log.debug("skipCount --> {}", skipCount);
            deletedNodesPaging = trashcanApi.listDeletedNodes(skipCount, batchSize, List.of("id")).getBody();
            for (val entry : Objects.requireNonNull(deletedNodesPaging).getList().getEntries()) {
                try {
                    queue.put(entry.getEntry().getId());
                } catch (InterruptedException e) {
                    log.trace(e.getMessage(), e);
                    log.warn(e.getMessage());
                }
            }
            skipCount += batchSize;
        } while (!deletedNodesPaging.getList().getEntries().isEmpty());
    }

}
