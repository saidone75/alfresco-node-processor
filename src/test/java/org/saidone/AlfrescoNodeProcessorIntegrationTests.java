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

import feign.FeignException;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.handler.NodesApi;
import org.alfresco.core.model.NodeBodyCreate;
import org.alfresco.core.model.NodeBodyUpdate;
import org.alfresco.search.handler.SearchApi;
import org.junit.jupiter.api.*;
import org.saidone.collectors.NodeCollector;
import org.saidone.model.alfresco.ContentModel;
import org.saidone.model.config.CollectorConfig;
import org.saidone.model.config.Permission;
import org.saidone.model.config.Permissions;
import org.saidone.model.config.ProcessorConfig;
import org.saidone.processors.NodeProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@ActiveProfiles("test")

@SpringBootTest
@Slf4j
/**
 * Integration tests for the Alfresco Node Processor.
 */
class AlfrescoNodeProcessorIntegrationTests extends BaseTest {

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

    @Value("${application.test-root-folder}")
    private String testRootFolderPath;

    @MockitoBean
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

    @Test
    @SneakyThrows
    void testNodeTreeCollector() {
        /* create node */
        var nodeId = createNode();
        /* mock config */
        var collectorConfig = new CollectorConfig();
        collectorConfig.addArg("path", "/Guest Home");
        /* use collector to populate queue */
        (((NodeCollector) context.getBean("nodeTreeCollector")).collect(collectorConfig)).get();
        try {
            /* assertions */
            Assertions.assertFalse(queue.isEmpty());
        } finally {
            /* clean up */
            nodesApi.deleteNode(nodeId, true);
        }
    }

    @Test
    @SneakyThrows
    void testMoveNodeProcessor() {
        // create node
        var nodeId = createNode();
        // add node to queue
        queue.add(nodeId);
        // create folder
        var targetParentId = createFolder();
        // mock config
        var processorConfig = new ProcessorConfig();
        processorConfig.addArg("target-parent-id", targetParentId);
        processorConfig.setReadOnly(Boolean.FALSE);
        // process node
        ((NodeProcessor) context.getBean("moveNodeProcessor")).process(processorConfig).get();
        // get node
        var node = Objects.requireNonNull(nodesApi.getNode(nodeId, null, null, null).getBody()).getEntry();
        try {
            // assertions
            Assertions.assertEquals(targetParentId, node.getParentId());
        } finally {
            // clean up
            nodesApi.deleteNode(targetParentId, true);
        }
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("unchecked")
    void testChainingNodeProcessor() {
        // create node
        var nodeId = createNode();
        // add node to queue
        queue.add(nodeId);
        // mock config
        var chainConfig = List.of(
                Map.of("name", "LogNodeNameProcessor"),
                Map.of(
                        "name", "AddAspectsAndSetPropertiesProcessor",
                        "args", Map.of("aspects", List.of(ContentModel.ASP_DUBLINCORE),
                                "properties", Map.of(ContentModel.PROP_PUBLISHER, "saidone", ContentModel.PROP_CONTRIBUTOR, "saidone"),
                                "readOnly", Boolean.FALSE
                        )));
        var processorConfig = new ProcessorConfig();
        processorConfig.addArg("processors", chainConfig);
        processorConfig.setReadOnly(Boolean.FALSE);
        // process node
        ((NodeProcessor) context.getBean("chainingNodeProcessor")).process(processorConfig).get();
        // get properties
        var properties = (Map<String, Object>) Objects.requireNonNull(nodesApi.getNode(nodeId, null, null, null).getBody()).getEntry().getProperties();
        try {
            // assertions
            Assertions.assertEquals("saidone", properties.get(ContentModel.PROP_PUBLISHER));
            Assertions.assertEquals("saidone", properties.get(ContentModel.PROP_CONTRIBUTOR));
            Assertions.assertEquals(1, processedNodesCounter.get());
        } finally {
            // clean up
            nodesApi.deleteNode(nodeId, true);
        }
    }

    @Test
    @SneakyThrows
    void testDownloadNodeProcessor() {
        // create node
        val urlString = "https://freetestdata.com/wp-content/uploads/2021/09/Free_Test_Data_100KB_PDF.pdf";
        val url = (URI.create(urlString).toURL());
        val nodeId = createNode(getTestRootFolderNodeId(), url).getId();
        // add properties
        val properties = new HashMap<String, Object>();
        properties.put(ContentModel.PROP_AUTHOR, "author");
        val nodeBodyUpdate = new NodeBodyUpdate();
        nodeBodyUpdate.setProperties(properties);
        nodesApi.updateNode(nodeId, nodeBodyUpdate, null, null);
        // add node to queue
        queue.add(nodeId);
        // mock config
        val processorConfig = new ProcessorConfig();
        processorConfig.addArg("output-dir", System.getProperty("java.io.tmpdir"));
        // process node
        ((NodeProcessor) context.getBean("downloadNodeProcessor")).process(processorConfig).get();
        // get node
        val node = Objects.requireNonNull(nodesApi.getNode(nodeId, List.of("path"), null, null).getBody()).getEntry();
        val downloadPath = String.format("%s%s%s", File.separator, System.getProperty("java.io.tmpdir"), node.getPath().getName());
        try {
            val fileName = urlString.replaceAll("^.*/", "");
            // check that the downloaded file exists
            @Cleanup("delete") val contentFile = new File(downloadPath, fileName);
            @Cleanup("delete") val metadataFile = new File(downloadPath, String.format("%s.metadata.properties.xml", fileName));
            Assertions.assertTrue(contentFile.exists(), "Downloaded file should exist: " + contentFile.getAbsolutePath());
            Assertions.assertTrue(metadataFile.exists(), "Metadata file should exist: " + metadataFile.getAbsolutePath());
        } finally {
            // clean up
            nodesApi.deleteNode(nodeId, true);
        }
    }

    @SneakyThrows
    @SuppressWarnings("unused")
    private String getGuestHomeNodeId() {
        var collectorConfig = new CollectorConfig();
        collectorConfig.addArg("query", "PATH:'/app:company_home/app:guest_home'");
        (((NodeCollector) context.getBean("queryNodeCollector")).collect(collectorConfig)).get();
        return queue.take();
    }

    private String getTestRootFolderNodeId() {
        return Objects.requireNonNull(nodesApi.getNode("-root-", null, testRootFolderPath, null).getBody()).getEntry().getId();
    }

    private String createNode() {
        return createNode(ContentModel.TYPE_CONTENT);
    }

    private String createFolder() {
        return createNode(ContentModel.TYPE_FOLDER);
    }

    private String createNode(String nodeType) {
        var nodeBodyCreate = new NodeBodyCreate();
        nodeBodyCreate.setNodeType(nodeType);
        nodeBodyCreate.setName(UUID.randomUUID().toString());
        return Objects.requireNonNull(nodesApi.createNode(getTestRootFolderNodeId(), nodeBodyCreate, null, null, null, null, null)
                .getBody()).getEntry().getId();
    }

}
