package utils.cli;

import config.AppConfig;

public interface CommandLineParser {
    // Parses the command line arguments, and configures the AppConfig accordingly.
    void parseCliArguments(String args[], AppConfig config);
}
