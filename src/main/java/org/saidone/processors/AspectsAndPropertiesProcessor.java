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

package org.saidone.processors;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.model.NodeBodyUpdate;
import org.saidone.model.config.ProcessorConfig;
import org.saidone.utils.CastUtils;
import org.springframework.stereotype.Component;

/**
 * Applies aspect and property updates to nodes loaded from Alfresco.
 * <p>
 * The processor expects the following optional configuration arguments:
 * <ul>
 *     <li>{@code aspects}: list of aspect QNames to append to the node aspect set.</li>
 *     <li>{@code !aspects}: list of aspect QNames to remove from the node aspect set.</li>
 *     <li>{@code properties}: map of property QNames to values that must be sent in the update payload.
 *     Supplying a {@code null} value clears the corresponding property in Alfresco.</li>
 * </ul>
 * Missing arguments are treated as empty collections/maps. When {@link #readOnly} is enabled,
 * the processor still computes and logs the update payload but does not invoke Alfresco update APIs.
 */
@Component
@Slf4j
public class AspectsAndPropertiesProcessor extends AbstractNodeProcessor {

    /**
     * Builds and optionally sends a {@link NodeBodyUpdate} for the target node.
     * <p>
     * Processing flow:
     * <ol>
     *     <li>Load the current node and start from its current aspect list.</li>
     *     <li>Add all values configured in {@code aspects}.</li>
     *     <li>Remove all values configured in {@code !aspects}.</li>
     *     <li>Set {@code properties} as the properties map in the update request.</li>
     * </ol>
     * The resulting payload is always logged at debug level. The remote update call is skipped
     * when {@link #readOnly} is {@code true}.
     *
     * @param nodeId Alfresco identifier of the node to update.
     * @param config processor configuration containing aspect/property instructions.
     */
    @Override
    @SneakyThrows
    public void processNode(String nodeId, ProcessorConfig config) {
        val node = getNode(nodeId);
        val aspectNames = node.getAspectNames();
        aspectNames.addAll(CastUtils.castToListOfObjects(config.getArg("aspects"), String.class));
        aspectNames.removeAll(CastUtils.castToListOfObjects(config.getArg("!aspects"), String.class));
        val nodeBodyUpdate = new NodeBodyUpdate();
        nodeBodyUpdate.setAspectNames(aspectNames);
        nodeBodyUpdate.setProperties(CastUtils.castToMapOfObjectObject(config.getArg("properties"), String.class, Object.class));
        log.debug("updating node --> {} with --> {}", nodeId, nodeBodyUpdate);
        if (!readOnly) {
            nodesApi.updateNode(nodeId, nodeBodyUpdate, null, null);
        }
    }

}
