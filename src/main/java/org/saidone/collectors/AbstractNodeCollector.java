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

package org.saidone.collectors;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.saidone.model.config.CollectorConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Base implementation of {@link NodeCollector} providing queue injection and a
 * default asynchronous execution of {@link #collectNodes(CollectorConfig)}.
 */
@Slf4j
public abstract class AbstractNodeCollector implements NodeCollector {

    @Autowired
    LinkedBlockingQueue<String> queue;

    @SneakyThrows
    /**
     * Collects nodes asynchronously by delegating to {@link #collectNodes(CollectorConfig)}.
     *
     * @param config collector configuration
     * @return future representing the asynchronous task
     */
    public CompletableFuture<Void> collect(CollectorConfig config) {
        return CompletableFuture.runAsync(() -> collectNodes(config));
    }

}
