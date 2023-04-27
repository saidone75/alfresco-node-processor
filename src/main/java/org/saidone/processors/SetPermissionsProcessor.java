/*
 * Alfresco Node Processor - do things with nodes
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
import org.alfresco.core.model.PermissionElement;
import org.alfresco.core.model.PermissionsBody;
import org.saidone.model.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SetPermissionsProcessor extends AbstractNodeProcessor {

    @Autowired
    private NodesApi nodesApi;

    @Override
    public void processNode(String nodeId, Config config) {
        var permissionBody = new PermissionsBody();
        permissionBody.setIsInheritanceEnabled(config.getPermissions().getIsInheritanceEnabled());
        config.getPermissions().getLocallySet().forEach(p -> {
            var permissionElement = new PermissionElement();
            permissionElement.setAuthorityId(p.getAuthorityId());
            permissionElement.setName(p.getName());
            permissionElement.setAccessStatus(PermissionElement.AccessStatusEnum.valueOf(p.getAccessStatus()));
            permissionBody.addLocallySetItem(permissionElement);
        });
        var nodeBodyUpdate = new NodeBodyUpdate();
        nodeBodyUpdate.setPermissions(permissionBody);
        log.debug("updating node --> {} with --> {}", nodeId, nodeBodyUpdate);
        if (config.getReadOnly() != null && !config.getReadOnly()) {
            nodesApi.updateNode(nodeId, nodeBodyUpdate, null, null);
        }
    }

}
