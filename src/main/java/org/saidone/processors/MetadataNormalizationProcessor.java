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

import java.io.Serializable;
import java.util.*;

@Component
@Slf4j
public class MetadataNormalizationProcessor extends AbstractNodeProcessor {

    private static final String OP = "op";
    private static final String OP_TRIM = "trim";
    private static final String OP_COLLAPSE_WHITESPACE = "collapse-whitespace";
    private static final String OP_CASE = "case";
    private static final String OP_REGEX = "regex";

    @Override
    @SneakyThrows
    public void processNode(String nodeId, ProcessorConfig config) {
        val opMap = parseArgs(config.getArgs());
        val node = Objects.requireNonNull(nodesApi.getNode(nodeId, null, null, null).getBody()).getEntry();
        val actualProperties = CastUtils.castToMapOfObjectObject(node.getProperties(), String.class, Object.class);
        val normalizedProperties = new HashMap<String, Object>();
        opMap.forEach((k, v) -> v.forEach(op -> apply(op.get(OP), k, actualProperties, normalizedProperties)));
        val nodeBodyUpdate = new NodeBodyUpdate();
        nodeBodyUpdate.setProperties(normalizedProperties);
    }

    private LinkedHashMap<String, List<Map<String, Serializable>>> parseArgs(Map<String, Object> args) {
        val opMap = new LinkedHashMap<String, List<Map<String, Serializable>>>();
        CastUtils.castToMapOfObjectObject(args, String.class, List.class).forEach((k, v) -> {
            val op = new ArrayList<Map<String, Serializable>>();
            for (val e : v) {
                op.add(CastUtils.castToMapOfStringSerializable(e));
            }
            opMap.put(k, op);
        });
        return opMap;
    }

    private static void apply(Serializable op, String k, Map<String, Object> actualProperties, HashMap<String, Object> normalizedProperties) {
        val v = normalizedProperties.get(k) != null ? normalizedProperties.get(k) : actualProperties.get(k);
        if (v == null) return;
        switch ((String) op) {
            case OP_TRIM -> normalizedProperties.put(k, trim(v));
            case OP_COLLAPSE_WHITESPACE -> normalizedProperties.put(k, collapseWhitespace(v));
        }
    }

    private static Object trim(Object v) {
        if (v instanceof String) return ((String) v).trim();
        else return v;
    }

    private static Object collapseWhitespace(Object v) {
        if (v instanceof String) return ((String) v).trim();
        else return v;
    }

}
