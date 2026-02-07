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
import java.util.Objects;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public abstract class BaseTest {

    @Autowired
    protected NodesApi nodesApi;

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
        val fileName = url.toString().replaceAll("^.*/", "");
        val tmpDir = Files.createTempDirectory("anp-");
        val tmpFile = new File(tmpDir.toFile(), fileName);
        tmpDir.toFile().deleteOnExit();
        tmpFile.deleteOnExit();
        FileUtils.copyURLToFile(url, tmpFile);
        return createNode(parentId, tmpFile);
    }

}
