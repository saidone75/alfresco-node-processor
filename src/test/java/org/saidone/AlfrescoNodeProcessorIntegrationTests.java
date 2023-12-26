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
import org.junit.jupiter.api.*;
import org.saidone.collectors.NodeCollector;
import org.saidone.model.config.*;
import org.saidone.processors.NodeProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Files;
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
    NodesApi nodesApi;

    @Autowired
    SearchApi searchApi;

    @MockBean
    AlfrescoNodeProcessorApplicationRunner alfrescoNodeProcessorApplicationRunner;

    @BeforeEach
    public void printName(TestInfo testInfo) {
        log.info("testing --> {}", testInfo.getDisplayName());
    }

    @BeforeEach
    public void resetProcessedNodesCounter() {
        processedNodesCounter.set(0);
    }

    @AfterEach
    public void emptyQueue() {
        queue.clear();
    }

    @Test
    @SneakyThrows
    void testLogNodeNameProcessor() {
        /* create node */
        var nodeId = createNode();
        /* add node to queue */
        queue.add(nodeId);
        /* process node */
        ((NodeProcessor) context.getBean("logNodeNameProcessor")).process(new ProcessorConfig()).get();
        try {
            /* assertions */
            Assertions.assertEquals(1, processedNodesCounter.get());
        } finally {
            /* clean up */
            nodesApi.deleteNode(nodeId, true);
        }
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
        var processorConfig = new ProcessorConfig();
        processorConfig.setReadOnly(Boolean.FALSE);
        processorConfig.addArg("aspects", List.of("cm:dublincore"));
        processorConfig.addArg("properties",
                Map.of(
                        "cm:publisher", "saidone",
                        "cm:contributor", "saidone"));
        /* process node */
        ((NodeProcessor) context.getBean("addAspectsAndSetPropertiesProcessor")).process(processorConfig).get();
        /* get properties */
        var properties = (Map<String, Object>) Objects.requireNonNull(nodesApi.getNode(nodeId, null, null, null).getBody()).getEntry().getProperties();
        try {
            /* assertions */
            Assertions.assertEquals("saidone", properties.get("cm:publisher"));
            Assertions.assertEquals("saidone", properties.get("cm:contributor"));
            Assertions.assertEquals(1, processedNodesCounter.get());
        } finally {
            /* clean up */
            nodesApi.deleteNode(nodeId, true);
        }
    }

    @Test
    @SneakyThrows
    void testDeleteNodeProcessor() {
        /* create node */
        var nodeId = createNode();
        /* add node to queue */
        queue.add(nodeId);
        /* mock config */
        var processorConfig = new ProcessorConfig();
        processorConfig.setReadOnly(Boolean.FALSE);
        /* process node */
        ((NodeProcessor) context.getBean("deleteNodeProcessor")).process(processorConfig).get();
        /* check if node has been deleted */
        Integer status = null;
        try {
            nodesApi.getNode(nodeId, null, null, null).getStatusCode();
        } catch (FeignException e) {
            status = e.status();
        }
        /* assertions */
        Assertions.assertEquals(404, status);
        Assertions.assertEquals(1, processedNodesCounter.get());
    }

    @Test
    @SneakyThrows
    void testSetPermissionsProcessor() {
        /* create node */
        var nodeId = createNode();
        /* add node to queue */
        queue.add(nodeId);
        /* mock config */
        var processorConfig = new ProcessorConfig();
        var permission = new Permission();
        permission.setAuthorityId("GROUP_EVERYONE");
        permission.setName("Collaborator");
        permission.setAccessStatus("ALLOWED");
        var permissions = new Permissions();
        permissions.addLocallySet(permission);
        permissions.setIsInheritanceEnabled(false);
        processorConfig.addArg("permissions", permissions);
        processorConfig.setReadOnly(Boolean.FALSE);
        /* process node */
        ((NodeProcessor) context.getBean("setPermissionsProcessor")).process(processorConfig).get();
        /* check permissions for node */
        var actualPermission = Objects.requireNonNull(nodesApi.getNode(nodeId, List.of("permissions"), null, null).getBody()).getEntry().getPermissions().getLocallySet().get(0);
        try {
            /* assertions */
            Assertions.assertEquals(actualPermission.getAuthorityId(), permission.getAuthorityId());
            Assertions.assertEquals(actualPermission.getName(), permission.getName());
            Assertions.assertEquals(actualPermission.getAccessStatus().toString(), permission.getAccessStatus());
            Assertions.assertEquals(1, processedNodesCounter.get());
        } finally {
            /* clean up */
            nodesApi.deleteNode(nodeId, true);
        }
    }

    @Test
    @SneakyThrows
    void testNodeListCollector() {
        /* create node */
        var nodeId = createNode();
        /* write node-id to a temp file */
        var file = File.createTempFile("nodeList-", ".txt");
        Files.writeString(file.toPath(), nodeId);
        /* mock config */
        var collectorConfig = new CollectorConfig();
        collectorConfig.addArg("nodeListFile", file.getAbsolutePath());
        /* use collector to populate queue */
        (((NodeCollector) context.getBean("nodeListCollector")).collect(collectorConfig)).get();
        try {
            /* assertions */
            Assertions.assertEquals(1, queue.size());
            Assertions.assertEquals(nodeId, queue.peek());
        } finally {
            /* clean up */
            nodesApi.deleteNode(nodeId, true);
            Files.delete(file.toPath());
        }
    }

    @SneakyThrows
    private String getGuestHomeNodeId() {
        var collectorConfig = new CollectorConfig();
        collectorConfig.addArg("query", "PATH:'/app:company_home/app:guest_home'");
        (((NodeCollector) context.getBean("queryNodeCollector")).collect(collectorConfig)).get();
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
