/*
 * Alfresco Node Processor - Do things with nodes
 * Copyright (C) 2023-2024 Saidone
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
import org.saidone.model.config.ProcessorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * A processor that delegates node processing to a chain of other processors.
 * <p>
 * The list of processors is provided via the {@code processors} argument in the
 * configuration. Each element of the list must define at least a
 * {@code name}. Optional {@code args} and {@code readOnly} can be specified per
 * processor.
 */
@Component
@Slf4j
public class ChainingNodeProcessor extends AbstractNodeProcessor {

    @Autowired
    private ApplicationContext context;

    @Override
    @SuppressWarnings("unchecked")
    public void processNode(String nodeId, ProcessorConfig config) {
        var processors = (List<?>) config.getArg("processors");
        if (processors == null || processors.isEmpty()) {
            log.warn("no processors configured for chaining");
            return;
        }
        for (Object obj : processors) {
            if (!(obj instanceof Map)) {
                log.warn("invalid processor definition: {}", obj);
                continue;
            }
            Map<String, Object> map = (Map<String, Object>) obj;
            var subConfig = new ProcessorConfig();
            subConfig.setName((String) map.get("name"));
            // inherit readOnly if not explicitly set
            if (map.get("readOnly") != null) {
                subConfig.setReadOnly((Boolean) map.get("readOnly"));
            } else {
                subConfig.setReadOnly(config.getReadOnly());
            }
            Object argsObj = map.get("args");
            if (argsObj instanceof Map<?, ?> args) {
                args.forEach((k, v) -> subConfig.addArg((String) k, v));
            }
            if (subConfig.getName() == null) {
                log.warn("processor name missing in chain element: {}", map);
                continue;
            }
            var beanName = StringUtils.uncapitalize(subConfig.getName());
            NodeProcessor processor;
            try {
                processor = (NodeProcessor) context.getBean(beanName);
            } catch (Exception e) {
                log.warn("processor bean not found: {}", subConfig.getName());
                continue;
            }
            processor.processNode(nodeId, subConfig);
        }
    }
}
