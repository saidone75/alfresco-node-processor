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

import org.saidone.model.config.CollectorConfig;

import java.util.concurrent.CompletableFuture;

/**
 * Contract for components able to collect node identifiers and push them into
 * the processing queue.
 */
public interface NodeCollector {

    /**
     * Start collecting nodes asynchronously.
     *
     * @param config collector configuration
     * @return future representing the asynchronous task
     */
    CompletableFuture<Void> collect(CollectorConfig config);

    /**
     * Implementation specific node collection logic.
     *
     * @param config collector configuration
     */
    void collectNodes(CollectorConfig config);

}
