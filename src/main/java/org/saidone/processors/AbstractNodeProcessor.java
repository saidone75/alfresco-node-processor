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
import org.alfresco.core.handler.NodesApi;
import org.alfresco.core.model.Node;
import org.saidone.component.BaseComponent;
import org.saidone.model.config.ProcessorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base implementation of {@link NodeProcessor} that pulls node identifiers
 * from the shared queue and delegates work to
 * {@link #processNode(String, ProcessorConfig)}.
 * <p>
 * Implementations typically use the {@link #getNode(String)} helpers to fetch
 * metadata and honor the {@link #readOnly} flag to avoid writes when running in
 * dry-run mode.
 */
@Slf4j
public abstract class AbstractNodeProcessor extends BaseComponent implements NodeProcessor {

    @Autowired
    private LinkedBlockingQueue<String> queue;

    @Autowired
    private AtomicInteger processedNodesCounter;

    @Autowired
    protected NodesApi nodesApi;

    @Value("${application.consumer-timeout}")
    private long consumerTimeout;

    @Value("${application.read-only:true}")
    protected boolean readOnly;

    /**
     * Start processing nodes asynchronously by reading identifiers from the
     * queue.
     *
     * @param config processor configuration
     * @return future representing the asynchronous task
     * @throws RuntimeException if the processing thread is interrupted while
     *                          polling the queue
     */
    @SneakyThrows
    public CompletableFuture<Void> process(ProcessorConfig config) {
        return CompletableFuture.runAsync(() -> {
            while (true) {
                String nodeId;
                try {
                    nodeId = queue.poll(consumerTimeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    log.trace(e.getMessage(), e);
                    log.error(e.getMessage());
                    throw new RuntimeException(e);
                }
                if (nodeId == null) break;
                else {
                    /* do things with the node */
                    try {
                        processNode(nodeId, config);
                        processedNodesCounter.incrementAndGet();
                    } catch (Exception e) {
                        log.trace(e.getMessage(), e);
                        log.error(e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Load a node by id without explicitly requesting properties.
     *
     * @param nodeId Alfresco node id
     * @return the node entry
     */
    protected Node getNode(String nodeId) {
        return getNode(nodeId, false);
    }

    /**
     * Load a node by id, optionally requesting properties.
     *
     * @param nodeId            Alfresco node id
     * @param includeProperties whether to request properties in the response
     * @return the node entry
     */
    protected Node getNode(String nodeId, boolean includeProperties) {
        return Objects.requireNonNull(nodesApi.getNode(
                nodeId,
                includeProperties ? List.of("properties") : null,
                null,
                null).getBody()).getEntry();
    }

    /**
     * Load a node by id, requesting specific include parameters.
     *
     * @param nodeId  Alfresco node id
     * @param include list of include flags to pass to the API
     * @return the node entry
     */
    protected Node getNode(String nodeId, List<String> include) {
        return Objects.requireNonNull(nodesApi.getNode(
                nodeId,
                include,
                null,
                null).getBody()).getEntry();
    }

}
