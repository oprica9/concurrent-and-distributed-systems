package cli.command;

import cli.Command;
import matrix_brain.MatrixBrain;

public class SaveCommand implements Command {

    private final MatrixBrain matrixBrain;

    public SaveCommand(MatrixBrain matrixBrain) {
        this.matrixBrain = matrixBrain;
    }

    @Override
    public String commandName() {
        return "save";
    }

    @Override
    public void execute(String args) {
        String[] splitArgs = args.split(" ");
        if (splitArgs.length != 4) {
            System.out.println("Invalid arguments for save. Usage: save -name <matrix_name> -file <file_name>");
            return;
        }

        if (!splitArgs[0].equals("-name") || !splitArgs[2].equals("-file")) {
            System.out.println("Invalid arguments for save. Usage: save -name <matrix_name> -file <file_name>");
            return;
        }

        String matrixName = splitArgs[1];
        String fileName = splitArgs[3];

        matrixBrain.saveMatrixToFile(matrixName, fileName);
    }
}
