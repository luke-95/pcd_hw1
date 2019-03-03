package utils.cli;

import config.AppConfig;
import org.apache.commons.cli.*;

public class ClientCommandLineParser implements CommandLineParser {
    public void parseCliArguments(String[] args, AppConfig config) {
        Options options = new Options();

        Option port = new Option("p", "port", true,  "server port");
        port.setRequired(true);
        options.addOption(port);

        Option ip = new Option("i", "ip", true,  "server ip");
        ip.setRequired(true);
        options.addOption(ip);

        Option url = new Option("url", "use-url", false, "IP address is in URL format");
        url.setRequired(false);
        options.addOption(url);

        Option file = new Option("f", "filename", true,  "file to transfer");
        file.setRequired(true);
        options.addOption(file);

        Option chunk = new Option("c", "chunk", true,  "transfer chunk size");
        chunk.setRequired(false);
        options.addOption(chunk);

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
            config.setIp(cmd.getOptionValue("i"));
            config.setFilename(cmd.getOptionValue("f"));

            if(cmd.hasOption("c")) {
                config.setChunkSize(Integer.parseInt(cmd.getOptionValue("c")));
            }
            if(cmd.hasOption("protocol") && cmd.getOptionValue("protocol").equals("TCP")) {
                config.setUseUDP(false);
            }
            if(cmd.hasOption("stream")) {
                config.setUseStreaming(true);
            }
            if(cmd.hasOption("url")) {
                config.setUseUrl(true);
            }
        }
        catch (ParseException exception) {
            System.out.println(exception.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }
    }
}
