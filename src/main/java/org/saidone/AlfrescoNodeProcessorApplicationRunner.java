/*
 * Alfresco Node Processor - do things with nodes
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

package org.saidone;

import lombok.extern.slf4j.Slf4j;
import org.saidone.collectors.NodeCollector;
import org.saidone.processors.NodeProcessor;
import org.saidone.utils.AlfrescoNodeProcessorUtils;
import org.saidone.utils.AnpCommandLineParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Component
@Slf4j
public class AlfrescoNodeProcessorApplicationRunner implements CommandLineRunner {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private LinkedList<CompletableFuture<Void>> nodeCollectors;

    @Autowired
    private LinkedList<CompletableFuture<Void>> nodeProcessors;

    @Autowired
    private AtomicInteger processedNodesCounter;

    @Value("${application.consumer-threads}")
    private int consumerThreads;

    @Override
    public void run(String... args) {

        /* get start time for metrics */
        var startTimeMillis = System.currentTimeMillis();

        /* parse CLI arguments */
        var configFileName = AnpCommandLineParser.parse(args);

        /* load and parse config file */
        var config = AlfrescoNodeProcessorUtils.loadConfig(configFileName);

        /* log mode */
        if (config.getProcessor().getReadOnly() != null && !config.getProcessor().getReadOnly())
            log.warn("READ-WRITE mode");
        else {
            config.getProcessor().setReadOnly(Boolean.TRUE);
            log.warn("READ-ONLY mode");
        }

        /* producer(s) */
        var collector = (NodeCollector) context.getBean(StringUtils.uncapitalize(config.getCollector().getName()));
        nodeCollectors.add(collector.collect(config.getCollector()));

        /* consumer(s) */
        var processor = (NodeProcessor) context.getBean(StringUtils.uncapitalize(config.getProcessor().getName()));
        IntStream.range(0, consumerThreads).forEach(i -> nodeProcessors.add(processor.process(config.getProcessor())));

        /* wait for all threads to complete */
        try {
            CompletableFuture.allOf(nodeCollectors.toArray(new CompletableFuture[0])).get();
            CompletableFuture.allOf(nodeProcessors.toArray(new CompletableFuture[0])).get();
        } catch (ExecutionException | InterruptedException e) {
            log.trace(e.getMessage(), e);
            log.error(e.getMessage());
            System.exit(1);
        }

        log.info("{} nodes processed", processedNodesCounter.get());
        log.debug("total time --> {}", String.format("%.02f", (System.currentTimeMillis() - startTimeMillis) / 1000f));
        System.exit(0);
    }

}