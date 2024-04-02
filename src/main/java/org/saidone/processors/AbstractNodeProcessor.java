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

package org.saidone.processors;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.saidone.model.config.ProcessorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractNodeProcessor implements NodeProcessor {

    @Autowired
    LinkedBlockingQueue<String> queue;

    @Autowired
    AtomicInteger processedNodesCounter;

    @Value("${application.consumer-timeout}")
    private long consumerTimeout;

    @SneakyThrows
    public CompletableFuture<Void> process(ProcessorConfig config) {
        return CompletableFuture.runAsync(() -> {
            while (true) {
                String nodeId;
                try {
                    nodeId = queue.poll(consumerTimeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    log.trace(e.getMessage(), e);
                    log.error("{}", e.getMessage());
                    throw new RuntimeException(e);
                }
                if (nodeId == null) break;
                else {
                    /* do things with the node */
                    processNode(nodeId, config);
                    processedNodesCounter.incrementAndGet();
                }
            }
        });
    }

    protected static List<String> castToListOfStrings(List<?> list) {
        return list
                .stream()
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    protected static Map<String, Object> castToMapOfStringObject(Map<?, ?> map) {
        return map
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> (String) e.getKey(),
                        e -> (Object) e.getValue()
                ));
    }

}