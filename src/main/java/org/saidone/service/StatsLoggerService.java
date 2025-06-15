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

package org.saidone.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Periodically logs statistics about queued and processed nodes.
 */
@ConditionalOnProperty(prefix = "application.stats-service", name = "enabled", havingValue = "true")
@Service
@Slf4j
public class StatsLoggerService {

    @Autowired
    private LinkedBlockingQueue<String> queue;

    @Autowired
    private AtomicInteger processedNodesCounter;

    @Value("${application.stats-service.print-interval}")
    private int printInterval;

    /**
     * Starts the logger service on application startup.
     * @return future representing the asynchronous logging task
     */
    @PostConstruct
    public CompletableFuture<Void> run() {
        return CompletableFuture.runAsync(() -> {
            /* will terminate on JVM shutdown  */
            while (true) {
                log.debug("queued nodes --> {}", queue.size());
                log.info("processed nodes --> {}", processedNodesCounter.get());
                try {
                    TimeUnit.SECONDS.sleep(printInterval);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

}