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

import java.util.HashMap;
import java.util.Map;

@Data
public class CollectorConfig {

    private String name;
    private Map<String, Object> args = new HashMap<>();

    public void addArg(String key, Object value) {
        this.args.put(key, value);
    }

    public Object getArg(String key) {
        return args.get(key);
    }

}
