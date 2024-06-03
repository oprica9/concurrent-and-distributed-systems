package cli.command;

import app.AppConfig;

import java.util.Arrays;

public class InfoCommand implements CLICommand {

    @Override
    public String commandName() {
        return "info";
    }

    @Override
    public void execute(String args) {
        AppConfig.timestampedStandardPrint("My info: " + AppConfig.myServentInfo + "\nsuccessors: " + Arrays.toString(AppConfig.chordState.getSuccessors()));
    }

}
