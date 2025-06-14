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

package org.saidone.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.util.Strings;

/**
 * Simple command line parser responsible for reading the configuration file
 * option.
 */
@UtilityClass
@Slf4j
public class AnpCommandLineParser {

    /**
     * Parses command line arguments.
     *
     * @param args command line arguments
     * @return the provided configuration file name
     */
    public String parse(String... args) {
        var options = new Options();
        var configOption = Option.builder("c").longOpt("config")
                .argName("config")
                .hasArg()
                .required(true)
                .desc("config file").build();
        options.addOption(configOption);
        CommandLine cmd;
        var parser = new DefaultParser();
        var helper = new HelpFormatter();
        var configFileName = Strings.EMPTY;
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("c")) {
                configFileName = cmd.getOptionValue("config");
            }
        } catch (ParseException e) {
            log.trace(e.getMessage(), e);
            log.error(e.getMessage());
            helper.printHelp("java -jar anp.jar", options);
            System.exit(0);
        }
        return configFileName;
    }

}
