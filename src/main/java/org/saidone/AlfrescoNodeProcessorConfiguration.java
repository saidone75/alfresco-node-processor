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

package org.saidone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring configuration that defines common beans used by the application such
 * as the queue and the lists of asynchronous tasks.
 */
@Configuration
public class AlfrescoNodeProcessorConfiguration {

    @Value("${application.queue-size}")
    private int queueSize;

    /**
     * Queue used to pass node identifiers between collectors and processors.
     */
    @Bean
    public LinkedBlockingQueue<String> queue() {
        return new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * Holder for asynchronous collector tasks.
     */
    @Bean
    public LinkedList<CompletableFuture<Void>> nodeCollectors() {
        return new LinkedList<>();
    }

    /**
     * Holder for asynchronous processor tasks.
     */
    @Bean
    public LinkedList<CompletableFuture<Void>> nodeProcessors() {
        return new LinkedList<>();
    }

    /**
     * Counter of processed nodes shared across processors.
     */
    @Bean
    public AtomicInteger processedNodesCounter() {
        return new AtomicInteger(0);
    }

}
