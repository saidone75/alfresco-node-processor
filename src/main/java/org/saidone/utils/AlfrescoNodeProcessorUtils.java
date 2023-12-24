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

package org.saidone.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.saidone.model.config.Config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@UtilityClass
@Slf4j
public class AlfrescoNodeProcessorUtils {

    public Config loadConfig(String configFileName) {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(configFileName);
        } catch (FileNotFoundException e) {
            if (log.isTraceEnabled()) e.printStackTrace();
            log.error("{}", e.getMessage());
            System.exit(-1);
        }

        Config config = null;
        try {
            var jsonConfig = IOUtils.toString(fis, "UTF-8");
            var objectMapper = new ObjectMapper();
            config = objectMapper.readValue(jsonConfig, Config.class);
        } catch (IOException e) {
            log.trace(e.getMessage(), e);
            log.error(e.getMessage());
            System.exit(1);
        }
        return config;
    }

}
