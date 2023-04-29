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
import org.saidone.collectors.NodeCollector;
import org.saidone.model.config.*;
import org.saidone.processors.NodeProcessor;
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
        var logNodeNameProcessorFuture = ((NodeProcessor) context.getBean("logNodeNameProcessor")).process(new ProcessorConfig());
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
        var processorConfig = new ProcessorConfig();
        processorConfig.setReadOnly(Boolean.FALSE);
        processorConfig.setAspects(List.of("cm:dublincore"));
        processorConfig.setProperties(Map.of(
                "cm:publisher", "saidone",
                "cm:contributor", "saidone"
        ));
        /* process node */
        var addAspectsAndSetPropertiesProcessorFuture = ((NodeProcessor) context.getBean("addAspectsAndSetPropertiesProcessor")).process(processorConfig);
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
        var processorConfig = new ProcessorConfig();
        processorConfig.setReadOnly(Boolean.FALSE);
        /* process node */
        var deleteNodeProcessorFuture = ((NodeProcessor) context.getBean("deleteNodeProcessor")).process(processorConfig);
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

    @Test
    @SneakyThrows
    @SuppressWarnings("unchecked")
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
        processorConfig.setPermissions(permissions);
        processorConfig.setReadOnly(Boolean.FALSE);
        /* process node */
        var setPermissionsProcessorFuture = ((NodeProcessor) context.getBean("setPermissionsProcessor")).process(processorConfig);
        setPermissionsProcessorFuture.get();
        /* check permissions for node */
        var nodePermissions = Objects.requireNonNull(nodesApi.getNode(nodeId, List.of("permissions"), null, null).getBody()).getEntry().getPermissions();
        var nodeLocallySet = nodePermissions.getLocallySet().get(0);
        Assertions.assertEquals(nodeLocallySet.getAuthorityId(), permission.getAuthorityId());
        Assertions.assertEquals(nodeLocallySet.getName(), permission.getName());
        Assertions.assertEquals(nodeLocallySet.getAccessStatus().toString(), permission.getAccessStatus());
        /* clean up */
        nodesApi.deleteNode(nodeId, true);

        log.info("nodes processed --> {}", processedNodesCounter.get());
        Assertions.assertEquals(1, processedNodesCounter.get());
    }

    @SneakyThrows
    private String getGuestHomeNodeId() {
        var collectorConfig = new CollectorConfig();
        collectorConfig.setQuery("PATH:'/app:company_home/app:guest_home'");
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
