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
import org.alfresco.core.model.NodeBodyUpdate;
import org.saidone.model.config.ProcessorConfig;
import org.saidone.utils.CastUtils;
import org.springframework.stereotype.Component;

/**
 * Adds configured aspects and properties to each processed node.
 * <p>
 * Expected configuration arguments:
 * <ul>
 *     <li>{@code aspects}: list of aspect QNames to apply</li>
 *     <li>{@code properties}: map of property QNames to values</li>
 * </ul>
 * Updates are skipped when {@link #readOnly} is {@code true}.
 */
@Component
@Slf4j
public class AddAspectsAndSetPropertiesProcessor extends AbstractNodeProcessor {

    /**
     * Adds configured aspects and properties to the given node.
     *
     * @param nodeId id of the node
     * @param config processor configuration
     */
    @Override
    @SneakyThrows
    public void processNode(String nodeId, ProcessorConfig config) {
        val node = getNode(nodeId);
        val aspectNames = node.getAspectNames();
        aspectNames.addAll(CastUtils.castToListOfObjects(config.getArg("aspects"), String.class));
        val nodeBodyUpdate = new NodeBodyUpdate();
        nodeBodyUpdate.setAspectNames(aspectNames);
        nodeBodyUpdate.setProperties(CastUtils.castToMapOfObjectObject(config.getArg("properties"), String.class, Object.class));
        log.debug("updating node --> {} with --> {}", nodeId, nodeBodyUpdate);
        if (!readOnly) {
            nodesApi.updateNode(nodeId, nodeBodyUpdate, null, null);
        }
    }

}
