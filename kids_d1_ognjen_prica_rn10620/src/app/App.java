package app;

import cli.CommandLineInterface;
import config.AppConfig;
import config.ConfigReader;

public class App {
    public static void main(String[] args) {
        AppConfig config = ConfigReader.importConfiguration(args[0]);

        CommandLineInterface cli = new CommandLineInterface(config);
        Thread cliThread = new Thread(cli, "command_line_interface");
        cliThread.start();
    }
}