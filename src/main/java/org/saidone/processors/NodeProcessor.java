/*
 * Alfresco Node Processor - Do things with nodes
 * Copyright (C) 2023 Saidone
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

package org.saidone.processors;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.saidone.model.config.Config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class NodeProcessor {

    @SneakyThrows
    public CompletableFuture<Void> process(BlockingQueue<String> queue, Config config) {
        return CompletableFuture.runAsync(() -> {
            while (true) {
                String nodeId;
                try {
                    nodeId = queue.poll(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    if (log.isTraceEnabled()) e.printStackTrace();
                    log.error("{}", e.getMessage());
                    throw new RuntimeException(e);
                }
                if (nodeId == null) break;
                else {
                    /* do things with node */
                    processNodes(nodeId, config);
                }
            }
        });
    }

    abstract void processNodes(String nodeId, Config config);

}
