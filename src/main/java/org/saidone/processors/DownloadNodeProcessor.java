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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.handler.VersionsApi;
import org.alfresco.core.model.Node;
import org.alfresco.core.model.Version;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.util.Strings;
import org.saidone.model.alfresco.ContentModel;
import org.saidone.model.config.ProcessorConfig;
import org.saidone.utils.CastUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Downloads content and metadata of a node to the local filesystem.
 * <p>
 * For each processed node a folder matching its path is created under the
 * configured output directory. The node's binary content is saved using its
 * original name while the metadata is stored in an adjacent
 * {@code *.metadata.properties.xml} file. If version history exists, each
 * version is exported with an incremental {@code .vX} suffix.
 * <p>
 * The output format is compatible with Alfresco bulk import.
 */
@Component
@Slf4j
public class DownloadNodeProcessor extends AbstractNodeProcessor {

    @Autowired
    VersionsApi versionsApi;

    /**
     * Name of the processor configuration argument that defines the output directory.
     */
    private static final String OUTPUT_DIR_ARG = "output-dir";

    /**
     * Extension used for metadata files written next to each node's binary content.
     */
    private static final String METADATA_FILE_SUFFIX = ".metadata.properties.xml";

    /**
     * Downloads the content and metadata of the node identified by {@code nodeId}.
     *
     * <p>The node is fetched from Alfresco with its properties and path
     * information. A folder mirroring the node's repository path is then created
     * under the configured output directory. The node's metadata and binary
     * content are written into this folder. When version history is present, all
     * versions except the current one are exported in chronological order and
     * suffixed with {@code .vX} to avoid overwriting the latest content.</p>
     *
     * @param nodeId id of the node to download
     * @param config processor configuration containing the {@code output-dir}
     *               argument
     */
    @Override
    @SneakyThrows
    public void processNode(String nodeId, ProcessorConfig config) {
        try {
            val node = getNode(nodeId, List.of("properties", "path"));
            val nodePath = node.getPath().getName();
            val destinationPath = createDestinationPath(getOutputDirectory(config), nodePath);
            saveNodeMetadata(node, destinationPath);
            saveNodeContent(node, destinationPath);
            var versions = Objects.requireNonNull(versionsApi.listVersionHistory(nodeId, List.of("aspectNames", "properties", "path"), null, 0, 100).getBody()).getList().getEntries();
            if (!versions.isEmpty()) {
                Collections.reverse(versions);
                for (var i = 0; i < versions.size() - 1; i++) {
                    saveNodeMetadata(nodeId, versions.get(i).getEntry(), destinationPath, i + 1);
                    saveNodeContent(nodeId, versions.get(i).getEntry(), destinationPath, i + 1);
                }
            }
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
     * serializing them to XML. The resulting file is placed next to the node's
     * binary content using the node name with the {@link #METADATA_FILE_SUFFIX}
     * suffix.</p>
     *
     * @param node            the node whose metadata is to be saved
     * @param destinationPath directory where the metadata file is created
     * @throws IOException if the file cannot be written
     */
    private void saveNodeMetadata(Node node, Path destinationPath) throws IOException {
        val properties = new Properties();
        CastUtils.castToMapOfObjectObject(node.getProperties(), String.class, Serializable.class)
                .forEach((key, value) -> {
                    if (value instanceof ArrayList) {
                        properties.setProperty(key,
                                String.join(",", CastUtils.castToListOfObjects(value, String.class)));
                    } else {
                        properties.setProperty(key, value.toString());
                    }
                });

        // additional properties
        properties.setProperty("type", node.getNodeType());
        properties.setProperty("aspects", String.join(",", node.getAspectNames()));
        properties.setProperty(ContentModel.PROP_CREATED, node.getCreatedAt().toString());

        val xmlPath = destinationPath.resolve(String.format("%s%s", node.getName(), METADATA_FILE_SUFFIX));
        properties.storeToXML(new FileOutputStream(xmlPath.toString()), null);

        log.debug("Saved node {} properties to {}", node.getId(), xmlPath);
    }

    /**
     * Persists the metadata of a specific version of a node to a metadata file.
     *
     * <p>The metadata are enriched with additional properties (type, aspects,
     * modification date stored under {@link ContentModel#PROP_CREATED}) before
     * being serialized to XML. Each version is stored using an incremental
     * suffix (for example {@code .v1}) so that multiple revisions can coexist in
     * the same folder.</p>
     *
     * @param nodeId          identifier of the node that owns the version
     * @param version         version whose metadata will be written
     * @param destinationPath directory where the metadata file must be created
     * @param versionNumber   sequential number used to distinguish versioned files
     * @throws IOException if the metadata file cannot be written
     */
    private void saveNodeMetadata(String nodeId, Version version, Path destinationPath, Integer versionNumber) throws IOException {
        val properties = new Properties();
        CastUtils.castToMapOfObjectObject(version.getProperties(), String.class, Serializable.class)
                .forEach((key, value) -> {
                    if (value instanceof ArrayList) {
                        properties.setProperty(key,
                                String.join(",", CastUtils.castToListOfObjects(value, String.class)));
                    } else {
                        properties.setProperty(key, value.toString());
                    }
                });

        // additional properties
        properties.setProperty("type", version.getNodeType());
        properties.setProperty("aspects", String.join(",", version.getAspectNames()));
        properties.setProperty(ContentModel.PROP_CREATED, version.getModifiedAt().toString());

        val xmlPath = destinationPath.resolve(String.format("%s%s.v%d", version.getName(), METADATA_FILE_SUFFIX, versionNumber));
        properties.storeToXML(new FileOutputStream(xmlPath.toString()), null);

        log.debug("Saved node {} version {} properties to {}", nodeId, version.getId(), xmlPath);
    }

    /**
     * Writes the binary content of the given node to the specified destination path.
     *
     * <p>If the node is a folder, the corresponding directory structure is created
     * without writing any content. Otherwise the node content is downloaded and
     * stored using the node's name. Empty payloads are ignored to avoid creating
     * zero-byte files.</p>
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
     * Writes the binary content of a specific node version to the destination folder.
     *
     * <p>The file is named after the node followed by the {@code .vX} suffix,
     * where {@code X} corresponds to {@code versionNumber}.</p>
     *
     * @param nodeId          identifier of the node to which the version belongs
     * @param version         version whose binary content has to be saved
     * @param destinationPath folder where the content will be written
     * @param versionNumber   sequential number used to disambiguate versioned content
     * @throws IOException if an error occurs while writing the file
     */
    private void saveNodeContent(String nodeId, Version version, Path destinationPath, Integer versionNumber) throws IOException {
        val nodeContent = getNodeContentBytes(nodeId, version);
        if (nodeContent.length == 0) {
            return;
        }
        val binPath = destinationPath.resolve(String.format("%s.v%d", version.getName(), versionNumber));
        FileUtils.writeByteArrayToFile(binPath.toFile(), nodeContent);
        log.debug("Saved node {} version {} content to {}", nodeId, version.getId(), binPath);
    }

    /**
     * Retrieves the binary content of a node using the {@link #nodesApi}.
     *
     * @param nodeId id of the node
     * @return byte array representing the content or an empty array if the
     * content cannot be retrieved
     */
    private byte[] getNodeContentBytes(String nodeId) {
        return getNodeContentBytes(nodeId, null);
    }

    /**
     * Retrieves the binary content associated with a specific node version.
     *
     * @param nodeId  identifier of the node that owns the version
     * @param version version whose content should be downloaded
     * @return the binary content of the version or an empty array if it cannot be retrieved
     */
    private byte[] getNodeContentBytes(String nodeId, Version version) {
        var nodeContentBody = (Resource) null;
        try {
            nodeContentBody = version == null ?
                    nodesApi.getNodeContent(nodeId, null, null, null).getBody() :
                    versionsApi.getVersionContent(nodeId, version.getId(), null, null, null).getBody();
            if (nodeContentBody == null) {
                log.warn("Node {} content is empty", nodeId);
                return new byte[0];
            }
            return nodeContentBody.getContentAsByteArray();
        } catch (Exception e) {
            if (version == null) {
                log.warn("Could not retrieve content for node {}: {}", nodeId, e.getMessage());
            } else {
                log.warn("Could not retrieve content for node {} and version {}: {}", nodeId, version.getId(), e.getMessage());
            }
            return new byte[0];
        }
    }

}
