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
import org.apache.logging.log4j.util.Strings;
import org.saidone.model.alfresco.bulk.Entry;
import org.saidone.model.alfresco.bulk.Properties;
import org.saidone.model.config.ProcessorConfig;
import org.saidone.utils.CastUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Downloads content and metadata of a node to the local filesystem.
 * <p>
 * For each processed node a folder matching its path is created under the
 * configured output directory. The node's binary content is saved using its
 * original name while the metadata is stored in an adjacent
 * {@code *.metadata.properties.xml} file.
 * <p>
 * The output format is compatible with Alfresco bulk import.
 */
@Component
@Slf4j
public class DownloadNodeProcessor extends AbstractNodeProcessor {

    private static final String OUTPUT_DIR_ARG = "output-dir";
    private static final String METADATA_FILE_SUFFIX = ".metadata.properties.xml";

    /**
     * Downloads a single node.
     *
     * @param nodeId id of the node to download
     * @param config processor configuration containing the {@code output-dir}
     *               argument
     */
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

    /**
     * Resolves the output directory from the processor configuration.
     *
     * @param config processor configuration
     * @return the output directory as a string
     * @throws IllegalArgumentException if the argument is missing or blank
     */
    private String getOutputDirectory(ProcessorConfig config) {
        val outputDirString = (String) config.getArg(OUTPUT_DIR_ARG);
        if (Strings.isBlank(outputDirString)) {
            throw new IllegalArgumentException(String.format("Output directory argument '%s' is required and cannot be empty", OUTPUT_DIR_ARG));
        }
        return outputDirString.trim();
    }

    /**
     * Creates the output directory if it does not already exist.
     *
     * @param outputDirString path of the directory
     * @throws RuntimeException if the directory cannot be created
     */
    private void createOutputDirectoryIfNotExists(String outputDirString) {
        val outputDir = new File(outputDirString);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            log.error("Failed to create output directory: {}", outputDirString);
            throw new RuntimeException("Failed to create output directory: " + outputDirString);
        }
    }

    /**
     * Creates the directory structure for the node inside the output directory.
     *
     * @param outputDirString configured output directory
     * @param nodePath        path of the node relative to the repository
     * @return the created directory
     * @throws IOException if the directories cannot be created
     */
    private Path createDestinationPath(String outputDirString, String nodePath) throws IOException {
        val destinationPath = Paths.get(outputDirString, nodePath);
        Files.createDirectories(destinationPath);
        return destinationPath;
    }

    /**
     * Writes node metadata to a {@code .properties.xml} file.
     *
     * @param nodeId         id of the node
     * @param nodeName       name of the node
     * @param destinationPath directory where the metadata file is created
     * @param properties     map of node properties
     * @throws IOException if the file cannot be written
     */
    private void saveNodeMetadata(String nodeId, String nodeName, Path destinationPath, Map<String, Serializable> properties) throws IOException {
        val nodeProperties = new Properties();
        properties.forEach((key, value) -> nodeProperties.addEntry(new Entry(key, value)));
        val xmlPath = destinationPath.resolve(String.format("%s%s", nodeName, METADATA_FILE_SUFFIX));
        writeStringToFile(xmlPath.toString(), alfPropertiesToXmlString(nodeProperties));
        log.debug("Saved node {} properties to {}", nodeId, xmlPath);
    }

    /**
     * Writes the node binary content to disk.
     *
     * @param nodeId         id of the node
     * @param nodeName       name of the node
     * @param destinationPath folder where the content will be stored
     * @throws IOException if the file cannot be written
     */
    private void saveNodeContent(String nodeId, String nodeName, Path destinationPath) throws IOException {
        val nodeContent = getNodeContentBytes(nodeId);
        if (nodeContent.length == 0) {
            return;
        }
        val binPath = destinationPath.resolve(nodeName);
        FileUtils.writeByteArrayToFile(binPath.toFile(), nodeContent);
        log.debug("Saved node {} content to {}", nodeId, binPath);
    }

    /**
     * Retrieves the binary content of a node.
     *
     * @param nodeId id of the node
     * @return byte array representing the content or an empty array if the
     *         content cannot be retrieved
     */
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

    /**
     * Serializes Alfresco properties to an XML string using {@link XmlMapper}.
     *
     * @param alfProperties properties to serialize
     * @return XML representation of the properties
     */
    @SneakyThrows
    public static String alfPropertiesToXmlString(Properties alfProperties) {
        val xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return buildXmlHeaders() + xmlMapper.writeValueAsString(alfProperties);
    }

    /**
     * Returns the standard XML header used by Alfresco.
     *
     * @return xml header string
     */
    public static String buildXmlHeaders() {
        return "<?xml version='1.0' encoding='UTF-8'?>" + System.lineSeparator() +
                "<!DOCTYPE properties SYSTEM 'http://java.sun.com/dtd/properties.dtd'>" + System.lineSeparator();
    }

    /**
     * Writes a string to the specified file using UTF-8 encoding.
     *
     * @param path    output file path
     * @param content content to write
     * @throws IOException if the file cannot be written
     */
    public static void writeStringToFile(String path, String content) throws IOException {
        val file = new File(path);
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
    }

}
