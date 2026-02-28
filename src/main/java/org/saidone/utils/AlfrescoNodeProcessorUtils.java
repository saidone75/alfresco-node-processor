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

package org.saidone.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.model.config.Config;

import java.io.File;
import java.nio.file.Files;

/**
 * Utility methods for reading configuration files.
 */
@UtilityClass
@Slf4j
public class AlfrescoNodeProcessorUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Loads the application configuration from the given JSON file.
     *
     * @param configFileName path to the configuration file
     * @return parsed configuration
     */
    public Config loadConfig(String configFileName) {
        Config config;
        try {
            val jsonConfig = Files.readString(new File(configFileName).toPath());
            config = OBJECT_MAPPER.readValue(jsonConfig, Config.class);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            log.error("{}", e.getMessage());
            return null;
        }
        return config;
    }

}
