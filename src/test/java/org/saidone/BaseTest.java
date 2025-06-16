package org.saidone;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.handler.NodesApi;
import org.alfresco.core.model.Node;
import org.alfresco.core.model.NodeBodyCreate;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.TestInstance;
import org.saidone.model.alfresco.ContentModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Objects;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public abstract class BaseTest {

    @Autowired
    protected NodesApi nodesApi;

    private static final HashMap<String, Integer> names = new HashMap<>();
    private static final HashMap<String, File> downloadedFiles = new HashMap<>();

    @SneakyThrows
    public Node createNode(String parentId, File file) {
        val nodeBodyCreate = new NodeBodyCreate();
        nodeBodyCreate.setName(file.getName());
        nodeBodyCreate.setNodeType(ContentModel.TYPE_CONTENT);
        val node = Objects.requireNonNull(nodesApi.createNode(parentId, nodeBodyCreate, true, null, null, null, null).getBody()).getEntry();
        nodesApi.updateNodeContent(node.getId(), Files.readAllBytes(file.toPath()), null, null, null, null, null);
        log.debug("Node {} created with name: {}", node.getId(), node.getName());
        return node;
    }

    @SneakyThrows
    public Node createNode(String parentId, URL url) {
            val extension = url.getPath().replaceAll("^.*\\.(.*)$", "$1");
            val tmpFile = File.createTempFile("anp-", String.format(".%s", extension));
            tmpFile.deleteOnExit();
            FileUtils.copyURLToFile(url, tmpFile);
        return createNode(parentId, tmpFile);
    }

}
