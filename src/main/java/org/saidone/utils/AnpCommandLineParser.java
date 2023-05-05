package org.saidone.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.util.Strings;

@UtilityClass
@Slf4j
public class AnpCommandLineParser {

    public String parse(String... args) {
        var options = new Options();
        var configOption = Option.builder("c").longOpt("config")
                .argName("config")
                .hasArg()
                .required(true)
                .desc("config file").build();
        options.addOption(configOption);
        CommandLine cmd;
        var parser = new BasicParser();
        var helper = new HelpFormatter();
        var configFileName = Strings.EMPTY;
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("c")) {
                configFileName = cmd.getOptionValue("config");
            }
        } catch (ParseException e) {
            if (log.isTraceEnabled()) e.printStackTrace();
            log.error(e.getMessage());
            helper.printHelp("java -jar anp.jar", options);
            System.exit(0);
        }
        return configFileName;
    }

}
