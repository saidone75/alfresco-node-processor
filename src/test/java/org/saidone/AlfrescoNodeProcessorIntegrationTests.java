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

import feign.FeignException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.core.handler.NodesApi;
import org.alfresco.core.model.NodeBodyCreate;
import org.alfresco.search.handler.SearchApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.saidone.model.config.Config;
import org.saidone.processors.NodeProcessor;
import org.saidone.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@ActiveProfiles("test")

@SpringBootTest
@Slf4j
class AlfrescoNodeProcessorIntegrationTests {

    @Autowired
    ApplicationContext context;

    @Autowired
    LinkedBlockingQueue<String> queue;

    @Autowired
    AtomicInteger processedNodesCounter;

    @Autowired
    SearchService searchService;

    @Autowired
    NodesApi nodesApi;

    @Autowired
    SearchApi searchApi;

    @MockBean
    AlfrescoNodeProcessorApplicationRunner alfrescoNodeProcessorApplicationRunner;

    @BeforeEach
    public void resetProcessedNodesCounter() {
        processedNodesCounter.set(0);
    }

    @Test
    @SneakyThrows
    void testLogNodeNameProcessor() {
        /* create node */
        var nodeId = createNode();
        /* add node to queue */
        queue.add(nodeId);
        /* process node */
        var logNodeNameProcessorFuture = ((NodeProcessor) context.getBean("logNodeNameProcessor")).process(new Config());
        logNodeNameProcessorFuture.get();
        /* clean up */
        nodesApi.deleteNode(nodeId, true);

        log.info("nodes processed --> {}", processedNodesCounter.get());
        Assertions.assertEquals(1, processedNodesCounter.get());
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("unchecked")
    void testAddAspectsAndSetPropertiesProcessor() {
        /* create node */
        var nodeId = createNode();
        /* add node to queue */
        queue.add(nodeId);
        /* mock config */
        var config = new Config();
        config.setReadOnly(Boolean.FALSE);
        config.setAspects(List.of("cm:dublincore"));
        config.setProperties(Map.of(
                "cm:publisher", "saidone",
                "cm:contributor", "saidone"
        ));
        /* process node */
        var addAspectsAndSetPropertiesProcessorFuture = ((NodeProcessor) context.getBean("addAspectsAndSetPropertiesProcessor")).process(config);
        addAspectsAndSetPropertiesProcessorFuture.get();
        /* get properties */
        var properties = (Map<String, Object>) Objects.requireNonNull(nodesApi.getNode(nodeId, null, null, null).getBody()).getEntry().getProperties();
        Assertions.assertEquals("saidone", properties.get("cm:publisher"));
        Assertions.assertEquals("saidone", properties.get("cm:contributor"));
        /* clean up */
        nodesApi.deleteNode(nodeId, true);

        log.info("nodes processed --> {}", processedNodesCounter.get());
        Assertions.assertEquals(1, processedNodesCounter.get());
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("unchecked")
    void testDeleteNodeProcessor() {
        /* create node */
        var nodeId = createNode();
        /* add node to queue */
        queue.add(nodeId);
        /* mock config */
        var config = new Config();
        config.setReadOnly(Boolean.FALSE);
        /* process node */
        var deleteNodeProcessorFuture = ((NodeProcessor) context.getBean("deleteNodeProcessor")).process(config);
        deleteNodeProcessorFuture.get();
        /* check if node has been deleted */
        Integer status = null;
        try {
            nodesApi.getNode(nodeId, null, null, null).getStatusCode();
        } catch (FeignException e) {
            status = e.status();
        }
        Assertions.assertEquals(404, status);

        log.info("nodes processed --> {}", processedNodesCounter.get());
        Assertions.assertEquals(1, processedNodesCounter.get());
    }

    @SneakyThrows
    private String getGuestHomeNodeId() {
        searchService.doQuery("PATH:'/app:company_home/app:guest_home'");
        return queue.take();
    }

    private String createNode() {
        var nodeBodyCreate = new NodeBodyCreate();
        nodeBodyCreate.setNodeType("cm:content");
        nodeBodyCreate.setName(UUID.randomUUID().toString());
        return Objects.requireNonNull(nodesApi.createNode(getGuestHomeNodeId(), nodeBodyCreate, null, null, null, null, null)
                .getBody()).getEntry().getId();
    }

}
