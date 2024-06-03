package cli.command;

import app.AppConfig;
import app.failure_detection.FailureDetector;
import cli.CLIParser;
import servent.SimpleServentListener;

public class StopCommand implements CLICommand {

    private final CLIParser parser;
    private final SimpleServentListener listener;
    private final FailureDetector failureDetector;

    public StopCommand(CLIParser parser, SimpleServentListener listener, FailureDetector failureDetector) {
        this.parser = parser;
        this.listener = listener;
        this.failureDetector = failureDetector;
    }

    @Override
    public String commandName() {
        return "stop";
    }

    @Override
    public void execute(String args) {
        AppConfig.timestampedStandardPrint("Stopping...");
        parser.stop();
        listener.stop();
        failureDetector.stop();
    }

}
