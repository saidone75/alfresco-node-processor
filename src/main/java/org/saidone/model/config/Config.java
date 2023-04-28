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

package org.saidone.model.config;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Config {

    private String collector;
    private String processor;
    private String query;
    private String list;
    private Map<String, Object> properties;
    private List<String> aspects;
    private Boolean readOnly;
    private Permissions permissions;

}
