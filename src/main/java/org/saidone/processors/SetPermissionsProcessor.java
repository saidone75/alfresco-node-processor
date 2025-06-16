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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.core.model.NodeBodyUpdate;
import org.alfresco.core.model.PermissionElement;
import org.alfresco.core.model.PermissionsBody;
import org.saidone.model.config.Permissions;
import org.saidone.model.config.ProcessorConfig;
import org.springframework.stereotype.Component;

/**
 * Applies permission settings to each processed node.
 */
@Component
@Slf4j
public class SetPermissionsProcessor extends AbstractNodeProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sets the permissions defined in the configuration on the given node.
     *
     * @param nodeId id of the node
     * @param config processor configuration
     */
    @Override
    public void processNode(String nodeId, ProcessorConfig config) {
        var permissions = objectMapper.convertValue(config.getArg("permissions"), Permissions.class);
        if (permissions != null) {
            var permissionBody = new PermissionsBody();
            permissionBody.setIsInheritanceEnabled(permissions.getIsInheritanceEnabled());
            permissions.getLocallySet().forEach(p -> {
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
        } else {
            log.warn("permissions not set in config file");
        }
    }

}
