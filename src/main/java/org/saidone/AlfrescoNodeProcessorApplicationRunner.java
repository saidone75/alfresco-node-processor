/*
 * Alfresco Node Processor - do things with nodes
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

package org.saidone;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.saidone.model.config.Config;
import org.saidone.processors.NodeProcessor;
import org.saidone.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
@Slf4j
public class AlfrescoNodeProcessorApplicationRunner implements ApplicationRunner {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SearchService searchService;

    @Value("${application.consumer-threads}")
    private int consumerThreads;

    @Value("${application.queue-size}")
    private int queueSize;

    private boolean running = true;
    private LinkedBlockingQueue<String> queue;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        /* check for arguments */
        if (args.getNonOptionArgs().size() < 1) {
            log.error("Config file not specified");
            System.exit(-1);
        }
        var configFileName = args.getNonOptionArgs().get(0);

        /* load and parse config file */
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(configFileName);
        } catch (FileNotFoundException e) {
            log.error("{}", e.getMessage());
            System.exit(-1);
        }
        String jsonConfig = IOUtils.toString(fis, "UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        Config config = objectMapper.readValue(jsonConfig, Config.class);

        if (config.getReadOnly() != null && !config.getReadOnly()) log.warn("READ-WRITE mode");
        else {
            config.setReadOnly(Boolean.TRUE);
            log.warn("READ-ONLY mode");
        }

        /* collect nodes */
        queue = new LinkedBlockingQueue<>(queueSize);

        /* queue size logger */
        var progressLogger = new ProgressLogger();
        progressLogger.start();

        /* do query */
        searchService.submitQuery(config.getQuery(), queue);

        /* consumers */
        var nodeProcessors = new LinkedList<CompletableFuture<Void>>();
        IntStream.range(0, consumerThreads).forEach(i -> nodeProcessors.add(((NodeProcessor) context.getBean(StringUtils.uncapitalize(config.getProcessor()))).process(queue, config)));
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(nodeProcessors.toArray(new CompletableFuture[0]));

        /* wait for all threads to complete */
        allFutures.get();

        this.running = false;
        System.exit(0);
    }

    public class ProgressLogger extends Thread {
        public void run() {
            while (running) {
                log.info("queued nodes --> {}", queue.size());
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
