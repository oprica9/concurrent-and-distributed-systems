package cli.command;

import app.AppConfig;
import app.model.ServentInfo;

public class SuccessorInfoCommand implements CLICommand {

    @Override
    public String commandName() {
        return "successor_info";
    }

    @Override
    public void execute(String args) {
        ServentInfo[] successorTable = AppConfig.chordState.getSuccessors();

        int num = 0;
        for (ServentInfo serventInfo : successorTable) {
            System.out.println(num + ": " + serventInfo);
            num++;
        }

    }

}
