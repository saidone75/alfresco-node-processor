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
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.model.Node;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.util.Strings;
import org.saidone.model.alfresco.ContentModel;
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

    /**
     * Name of the processor configuration argument that defines the output directory.
     */
    private static final String OUTPUT_DIR_ARG = "output-dir";

    /**
     * Extension used for metadata files written next to each node's binary content.
     */
    private static final String METADATA_FILE_SUFFIX = ".metadata.properties.xml";

    /**
     * Preconfigured {@link XmlMapper} instance used to serialize properties to XML.
     */
    private static final XmlMapper XML_MAPPER = XmlMapper.builder()
            .enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    /**
     * Downloads the content and metadata of the node identified by {@code nodeId}.
     *
     * <p>The node is fetched from Alfresco with its properties and path
     * information. A folder mirroring the node's repository path is then created
     * under the configured output directory. The node's metadata and binary
     * content are written into this folder.</p>
     *
     * @param nodeId id of the node to download
     * @param config processor configuration containing the {@code output-dir}
     *               argument
     */
    @Override
    public void processNode(String nodeId, ProcessorConfig config) {
        try {
            val node = getNode(nodeId, List.of("properties", "path"));
            val nodePath = node.getPath().getName();
            val destinationPath = createDestinationPath(getOutputDirectory(config), nodePath);
            saveNodeMetadata(node, destinationPath);
            saveNodeContent(node, destinationPath);
        } catch (Exception e) {
            log.error("Error processing node {}: {}", nodeId, e.getMessage());
            throw new RuntimeException("Failed to process node: " + nodeId, e);
        }
    }

    /**
     * Resolves the output directory from the processor configuration.
     *
     * <p>The directory path is read from the argument named by
     * {@link #OUTPUT_DIR_ARG}. Leading and trailing white spaces are trimmed.</p>
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
     * Creates the directory structure for the node inside the output directory.
     *
     * <p>The returned path points to the folder that will contain the node's
     * binary content and metadata.</p>
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
     * <p>The method collects all node properties and enriches them with a few
     * additional entries such as type, aspects and creation date before
     * serializing them to XML.</p>
     *
     * @param node            the node whose metadata is to be saved
     * @param destinationPath directory where the metadata file is created
     * @throws IOException if the file cannot be written
     */
    private void saveNodeMetadata(Node node, Path destinationPath) throws IOException {
        val properties = new Properties();
        CastUtils.castToMapOfStringSerializable(node.getProperties()).forEach(properties::addEntry);
        // additional properties
        properties.addEntry("type", node.getNodeType());
        properties.addEntry("aspects", String.join(",", node.getAspectNames()));
        properties.addEntry(ContentModel.PROP_CREATED, node.getCreatedAt().toString());
        val xmlPath = destinationPath.resolve(String.format("%s%s", node.getName(), METADATA_FILE_SUFFIX));
        writeStringToFile(xmlPath.toString(), alfPropertiesToXmlString(properties));
        log.debug("Saved node {} properties to {}", node.getId(), xmlPath);
    }

    /**
     * Writes the binary content of the given node to the specified destination path.
     *
     * <p>If the node is a folder, the corresponding directory structure is created
     * without writing any content. Otherwise the node content is downloaded and
     * stored using the node's name.</p>
     *
     * @param node            the node whose content is to be saved
     * @param destinationPath the folder where the content will be stored
     * @throws IOException if an error occurs during writing the file
     */
    private void saveNodeContent(Node node, Path destinationPath) throws IOException {
        if (node.isIsFolder()) {
            Files.createDirectories(Paths.get(String.valueOf(destinationPath), node.getName()));
        } else {
            val nodeContent = getNodeContentBytes(node.getId());
            if (nodeContent.length == 0) {
                return;
            }
            val binPath = destinationPath.resolve(node.getName());
            FileUtils.writeByteArrayToFile(binPath.toFile(), nodeContent);
            log.debug("Saved node {} content to {}", node.getId(), binPath);
        }
    }

    /**
     * Retrieves the binary content of a node using the {@link #nodesApi}.
     *
     * @param nodeId id of the node
     * @return byte array representing the content or an empty array if the
     * content cannot be retrieved
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
     * Serializes Alfresco properties to an XML string using the configured
     * {@link XmlMapper}.
     *
     * @param properties properties to serialize
     * @return XML representation of the properties
     */
    @SneakyThrows
    public static String alfPropertiesToXmlString(Properties properties) {
        return XML_MAPPER.writeValueAsString(properties);
    }

    /**
     * Writes a string to the specified file using {@link StandardCharsets#UTF_8}.
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
