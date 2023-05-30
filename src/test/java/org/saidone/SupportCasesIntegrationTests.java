package org.saidone;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.core.handler.NodesApi;
import org.alfresco.core.model.AssociationBody;
import org.alfresco.core.model.NodeBodyCreate;
import org.alfresco.search.handler.SearchApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.saidone.collectors.NodeCollector;
import org.saidone.model.config.CollectorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@ActiveProfiles("test")

@SpringBootTest
@Slf4j
public class SupportCasesIntegrationTests {

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
    void testListTargetsAssociations() {
        /* create node */
        var nodeId = createNodeInGuestHome();
        var associationBody = new AssociationBody();
        associationBody.setAssocType("cm:references");
        AtomicInteger associationsCreated = new AtomicInteger(0);
        /* create 101 new nodes and associations */
        IntStream.range(0, 101).parallel().forEach(i -> {
            associationBody.setTargetId(createNodeInGuestHome());
            nodesApi.createAssociation(nodeId, associationBody, null);
            associationsCreated.incrementAndGet();
        });
        var targetAssociations = nodesApi.listTargetAssociations(
                nodeId,
                "(assocType='cm:references')",
                null,
                null
        );
        log.info("associations created --> {}", associationsCreated.get());
        /* no way to obtain next page or fetch more than 100 results */
        log.info("count --> {}", Objects.requireNonNull(targetAssociations.getBody()).getList().getPagination().getCount());
        log.info("maxItems --> {}", targetAssociations.getBody().getList().getPagination().getMaxItems());
        /*
        Expected output:
        INFO testing --> testListTargetsAssociations()
        INFO associations created --> 101
        INFO count --> 100
        INFO maxItems --> 100
        From REST API:
        "list": {
        "pagination": {
            "count": 100,
            "hasMoreItems": true,
            "totalItems": 101,
            "skipCount": 0,
            "maxItems": 100
        }
         */
    }

    @SneakyThrows
    private String getGuestHomeNodeId() {
        var collectorConfig = new CollectorConfig();
        collectorConfig.addArg("query", "PATH:'/app:company_home/app:guest_home'");
        (((NodeCollector) context.getBean("queryNodeCollector")).collect(collectorConfig)).get();
        return queue.take();
    }

    private String createNodeInGuestHome() {
        var nodeBodyCreate = new NodeBodyCreate();
        nodeBodyCreate.setNodeType("cm:content");
        nodeBodyCreate.setName(UUID.randomUUID().toString());
        return Objects.requireNonNull(nodesApi.createNode(getGuestHomeNodeId(), nodeBodyCreate, null, null, null, null, null)
                .getBody()).getEntry().getId();
    }

}
