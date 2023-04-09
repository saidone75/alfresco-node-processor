/*
 * Alfresco Node Processor - Do things with nodes
 * Copyright (C) 2023 Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.saidone.processors;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.core.handler.NodesApi;
import org.alfresco.core.model.NodeBodyUpdate;
import org.saidone.model.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AddAspectAndSetPropertiesProcessor extends AbstractNodeProcessor {

    @Autowired
    private NodesApi nodesApi;

    @Override
    public void processNode(String nodeId, Config config) {
        var nodeBodyUpdate = new NodeBodyUpdate();
        nodeBodyUpdate.setAspectNames(config.getAspects());
        nodeBodyUpdate.setProperties(config.getProperties());
        log.debug("updating node {} with {}", nodeId, nodeBodyUpdate);
        if (!config.getReadOnly()) {
            nodesApi.updateNode(nodeId, nodeBodyUpdate, null, null);
        }
    }

}
