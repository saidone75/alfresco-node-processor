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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.model.config.ProcessorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

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

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public void processNode(String nodeId, ProcessorConfig config) {

        val processorConfigs = ((List<?>) config.getArg("processors")).stream().map(c -> objectMapper.convertValue(c, ProcessorConfig.class)).toList();

        if (processorConfigs.isEmpty()) {
            log.warn("no processors configured for chaining");
            return;
        }

        for (val processorConfig : processorConfigs) {
            val processorName = StringUtils.uncapitalize(processorConfig.getName());
            NodeProcessor processor;
            try {
                processor = (NodeProcessor) context.getBean(processorName);
                processor.processNode(nodeId, processorConfig);
            } catch (Exception e) {
                throw new Exception(String.format("Processor bean not found: %s", processorConfig.getName()));
            }
        }
    }

}
