package utils.cli;

import config.AppConfig;
import org.apache.commons.cli.*;

public class ServerCommandLineParser implements CommandLineParser{
    public void parseCliArguments(String[] args, AppConfig config) {
        Options options = new Options();

        Option port = new Option("p", "port", true,  "server port");
        port.setRequired(true);
        options.addOption(port);

        Option protocol = new Option("protocol", "protocol", true,  "Valid inputs: UDP, TCP. Default: UDP");
        protocol.setRequired(false);
        options.addOption(protocol);

        Option stopAndWait = new Option("stream", "stream", false,  "Set this flag to set the transfer mode to streaming. Default: stop and wait");
        stopAndWait.setRequired(false);
        options.addOption(stopAndWait);

        org.apache.commons.cli.CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse( options, args);

            config.setPort(Integer.parseInt(cmd.getOptionValue("p")));

            if(cmd.hasOption("protocol") && cmd.getOptionValue("protocol").equals("TCP")) {
                config.setUseUDP(false);
            }
            if(cmd.hasOption("stream")) {
                config.setUseStreaming(true);
            }
        }
        catch (ParseException exception) {
            System.out.println(exception.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

    }
}
