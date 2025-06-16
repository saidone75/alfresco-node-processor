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

package org.saidone.processors;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.saidone.model.alfresco.bulk.Entry;
import org.saidone.model.alfresco.bulk.Properties;
import org.saidone.model.config.ProcessorConfig;
import org.saidone.utils.CastUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@Slf4j
public class DownloadNodeProcessor extends AbstractNodeProcessor {

    @Override
    public void processNode(String nodeId, ProcessorConfig config) {
        // create output dir if not exists
        val outputDirString = (String) config.getArg("output-dir");
        val outputDir = new File(outputDirString);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            log.error("Failed to create output outputDir: {}", outputDirString);
            throw new RuntimeException("Failed to create output outputDir: " + outputDirString);
        }
        try {
            var nodeProperties = new Properties();
            val node = getNode(nodeId, List.of("properties", "path"));
            val properties = CastUtils.castToMapOfStringSerializable(node.getProperties());
            val nodePath = node.getPath().getName();

            // create destination path
            val destinationPath = String.format("%s/%s", outputDirString, nodePath);
            Files.createDirectories(Path.of(destinationPath));

            // populate properties
            properties.forEach((key, value) -> nodeProperties.addEntry(new Entry(key, value)));

            val xmlPath = String.format("%s/%s.metadata.properties.xml", destinationPath, node.getName());
            writeStringToFile(xmlPath, alfPropertiesToXmlString(nodeProperties));
            log.debug("Saved node {} properties to {}", nodeId, xmlPath);

            byte[] nodeContent;
            val nodeContentBody = nodesApi.getNodeContent(nodeId, null, null, null).getBody();
            if (nodeContentBody == null) {
                log.warn("Node {} content is empty", nodeId);
                nodeContent = new byte[]{};
            } else nodeContent = nodeContentBody.getContentAsByteArray();

            // save content to file (bin)
            val binPath = String.format("%s/%s", destinationPath, node.getName());
            FileUtils.writeByteArrayToFile(new File(binPath), nodeContent);
            log.debug("Saved node {} content to {}", nodeId, binPath);
        } catch (Exception e) {
            log.error("Error processing node {}: {}", nodeId, e.getMessage());
            throw new RuntimeException("Failed to process node: " + nodeId, e);
        }
    }

    @SneakyThrows
    public static String alfPropertiesToXmlString(Properties alfProperties) {
        val xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return headers().concat(xmlMapper.writeValueAsString(alfProperties));
    }

    public static String headers() {
        return String.join("", "<?xml version='1.0' encoding='UTF-8'?>",
                System.lineSeparator(),
                "<!DOCTYPE properties SYSTEM 'http://java.sun.com/dtd/properties.dtd'>",
                System.lineSeparator());
    }

    @SneakyThrows
    public static void writeStringToFile(String path, String content) {
        var file = new File(path);
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
    }

}