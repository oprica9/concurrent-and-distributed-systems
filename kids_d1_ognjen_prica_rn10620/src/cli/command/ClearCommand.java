package cli.command;

import cli.Command;
import matrix_brain.MatrixBrain;

public class ClearCommand implements Command {
    private final MatrixBrain matrixBrain;

    public ClearCommand(MatrixBrain matrixBrain) {
        this.matrixBrain = matrixBrain;
    }

    @Override
    public String commandName() {
        return "clear";
    }

    @Override
    public void execute(String args) {
        String[] splitArgs = args.split(" ");
        if (splitArgs.length != 2) {
            System.out.println("Invalid arguments for clear. Usage: clear <matrix_name | file_name>");
            return;
        }

        if (!splitArgs[1].equals("true") && !splitArgs[1].equals("false")) {
            System.out.println("Invalid arguments for clear. Usage: clear <matrix_name | file_name>");
            return;
        }

        String name = splitArgs[0];
        boolean isFile = Boolean.parseBoolean(splitArgs[1]);

        String mName = name;
        if (isFile) {
            mName = name.substring(0, name.lastIndexOf('.'));
        }

        matrixBrain.clearMatrix(mName);
    }
}
