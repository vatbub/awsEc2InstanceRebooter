package com.github.vatbub.awsEc2InstanceRebooter;

import com.amazonaws.regions.Regions;
import com.github.vatbub.common.core.Common;
import com.github.vatbub.common.core.logging.FOKLogger;
import org.apache.commons.cli.*;

import java.util.logging.Level;

public class Main {
    public static void main(String[] args){
        Common.setAppName("awsEc2InstanceRebooter");
        Options cliOptions = new Options();

        Option commandOption = new Option("c", "command", true, "The command to execute. Can either be start, stop or reboot");
        Option instanceIDOption = new Option("i", "instanceId", true, "The id of the instance to interact with");
        Option awsRegionOption = new Option("r", "region", true, "The aws region the instance is located in");
        Option awsKeyOption = new Option("k", "awsKey", true, "The aws key obtained from the IAM");
        Option awsSecretOption = new Option("s", "awsSecret", true, "The aws secret obtained from the IAM");

        commandOption.setRequired(true);
        instanceIDOption.setRequired(true);
        awsRegionOption.setRequired(true);
        awsKeyOption.setRequired(true);
        awsSecretOption.setRequired(true);

        cliOptions.addOption(commandOption);
        cliOptions.addOption(instanceIDOption);
        cliOptions.addOption(awsRegionOption);
        cliOptions.addOption(awsKeyOption);
        cliOptions.addOption(awsSecretOption);

        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine commandLine = parser.parse( cliOptions, args );
            Command command = Command.valueOf(commandLine.getOptionValue("c"));
            switch(command){
                case start:
                    AWSEC2Rebooter.startInstance(commandLine.getOptionValue("i"), Regions.valueOf(commandLine.getOptionValue("r")), commandLine.getOptionValue("k"), commandLine.getOptionValue("s"));
                    break;
                case stop:
                    AWSEC2Rebooter.stopInstance(commandLine.getOptionValue("i"), Regions.valueOf(commandLine.getOptionValue("r")), commandLine.getOptionValue("k"), commandLine.getOptionValue("s"));
                    break;
                case reboot:
                    AWSEC2Rebooter.rebootInstance(commandLine.getOptionValue("i"), Regions.valueOf(commandLine.getOptionValue("r")), commandLine.getOptionValue("k"), commandLine.getOptionValue("s"));
                    break;
            }
        }
        catch( ParseException e ) {
            // oops, something went wrong
            FOKLogger.log(Main.class.getName(), Level.SEVERE, "Could not parse command line arguments", e);
            printHelpMessage(cliOptions);
        }
    }

    public static void printHelpMessage(Options cliOptions){
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( Common.getPathAndNameOfCurrentJar(), cliOptions );
    }

    private enum Command {
        start, stop, reboot
    }
}
