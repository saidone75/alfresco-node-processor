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

package org.saidone;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.collectors.NodeCollector;
import org.saidone.component.BaseComponent;
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

/**
 * Runs the application from the command line. It loads the configuration,
 * starts node collectors and processors and waits for them to complete.
 */
@Component
@Slf4j
public class AlfrescoNodeProcessorApplicationRunner extends BaseComponent implements CommandLineRunner {

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

    @Value("${application.read-only:true}")
    private boolean readOnly;

    /**
     * Executes the collectors and processors defined by the configuration.
     *
     * @param args command line arguments
     */
    @Override
    public void run(String... args) {

        // get start time for metrics
        val startTimeMillis = System.currentTimeMillis();

        // parse CLI arguments
        val configFileName = AnpCommandLineParser.parse(args);

        // load and parse config file
        val config = AlfrescoNodeProcessorUtils.loadConfig(configFileName);
        if (config == null) super.shutDown(1);

        // log mode
        if (readOnly) {
            log.warn("READ-ONLY mode");
        } else {
            log.warn("READ-WRITE mode");
        }

        // producer(s)
        val collector = (NodeCollector) context.getBean(StringUtils.uncapitalize(config.getCollector().getName()));
        nodeCollectors.add(collector.collect(config.getCollector()));

        // consumer(s)
        val processor = (NodeProcessor) context.getBean(StringUtils.uncapitalize(config.getProcessor().getName()));
        IntStream.range(0, consumerThreads).forEach(i -> nodeProcessors.add(processor.process(config.getProcessor())));

        // wait for all threads to complete
        try {
            CompletableFuture.allOf(nodeCollectors.toArray(new CompletableFuture[0])).get();
            CompletableFuture.allOf(nodeProcessors.toArray(new CompletableFuture[0])).get();
        } catch (ExecutionException | InterruptedException e) {
            log.trace(e.getMessage(), e);
            log.error(e.getMessage());
            super.shutDown(1);
        }

        log.info("{} nodes processed", processedNodesCounter.get());
        log.debug("total time --> {}", String.format("%.02f", (System.currentTimeMillis() - startTimeMillis) / 1000f));
        super.shutDown(0);
    }

}