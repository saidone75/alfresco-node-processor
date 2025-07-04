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

package org.saidone.model.config;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

/**
 * Defines permission settings to be applied to a node.
 */
@Data
public class Permissions {

    private Boolean isInheritanceEnabled;
    private List<Permission> locallySet = new LinkedList<>();

    public void addLocallySet(Permission permission) {
        locallySet.add(permission);
    }

}
