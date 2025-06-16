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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@Slf4j
public class DownloadNodeProcessor extends AbstractNodeProcessor {

    private static final String OUTPUT_DIR_ARG = "output-dir";
    private static final String METADATA_FILE_SUFFIX = ".metadata.properties.xml";

    @Override
    public void processNode(String nodeId, ProcessorConfig config) {
        val outputDirString = getOutputDirectory(config);
        createOutputDirectoryIfNotExists(outputDirString);

        try {
            val node = getNode(nodeId, List.of("properties", "path"));
            val properties = CastUtils.castToMapOfStringSerializable(node.getProperties());
            val nodePath = node.getPath().getName();

            val destinationPath = createDestinationPath(outputDirString, nodePath);

            saveNodeMetadata(nodeId, node.getName(), destinationPath, properties);
            saveNodeContent(nodeId, node.getName(), destinationPath);

        } catch (Exception e) {
            log.error("Error processing node {}: {}", nodeId, e.getMessage());
            throw new RuntimeException("Failed to process node: " + nodeId, e);
        }
    }

    private String getOutputDirectory(ProcessorConfig config) {
        val outputDirString = (String) config.getArg(OUTPUT_DIR_ARG);
        if (outputDirString == null || outputDirString.trim().isEmpty()) {
            throw new IllegalArgumentException("Output directory argument '" + OUTPUT_DIR_ARG + "' is required and cannot be empty");
        }
        return outputDirString.trim();
    }

    private void createOutputDirectoryIfNotExists(String outputDirString) {
        val outputDir = new File(outputDirString);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            log.error("Failed to create output directory: {}", outputDirString);
            throw new RuntimeException("Failed to create output directory: " + outputDirString);
        }
    }

    private Path createDestinationPath(String outputDirString, String nodePath) throws IOException {
        val destinationPath = Paths.get(outputDirString, nodePath);
        Files.createDirectories(destinationPath);
        return destinationPath;
    }

    private void saveNodeMetadata(String nodeId, String nodeName, Path destinationPath,
                                  java.util.Map<String, java.io.Serializable> properties) throws IOException {
        val nodeProperties = new Properties();
        properties.forEach((key, value) -> nodeProperties.addEntry(new Entry(key, value)));

        val xmlPath = destinationPath.resolve(nodeName + METADATA_FILE_SUFFIX);
        writeStringToFile(xmlPath.toString(), alfPropertiesToXmlString(nodeProperties));
        log.debug("Saved node {} properties to {}", nodeId, xmlPath);
    }

    private void saveNodeContent(String nodeId, String nodeName, Path destinationPath) throws IOException {
        val nodeContent = getNodeContentBytes(nodeId);
        val binPath = destinationPath.resolve(nodeName);
        FileUtils.writeByteArrayToFile(binPath.toFile(), nodeContent);
        log.debug("Saved node {} content to {}", nodeId, binPath);
    }

    private byte[] getNodeContentBytes(String nodeId) {
        try {
            val nodeContentBody = nodesApi.getNodeContent(nodeId, null, null, null).getBody();
            if (nodeContentBody == null) {
                log.warn("Node {} content is empty", nodeId);
                return new byte[0];
            }
            return nodeContentBody.getContentAsByteArray();
        } catch (Exception e) {
            log.warn("Could not retrieve content for node {}: {}", nodeId, e.getMessage());
            return new byte[0];
        }
    }

    @SneakyThrows
    public static String alfPropertiesToXmlString(Properties alfProperties) {
        val xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return buildXmlHeaders() + xmlMapper.writeValueAsString(alfProperties);
    }

    public static String buildXmlHeaders() {
        return "<?xml version='1.0' encoding='UTF-8'?>" + System.lineSeparator() +
                "<!DOCTYPE properties SYSTEM 'http://java.sun.com/dtd/properties.dtd'>" + System.lineSeparator();
    }

    public static void writeStringToFile(String path, String content) throws IOException {
        val file = new File(path);
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
    }

}