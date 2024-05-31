package cli.command;

import cli.Command;
import exceptions.MatrixDoesntExistException;
import matrix_brain.MatrixBrain;
import model.MatrixInfo;

import java.util.ArrayList;
import java.util.List;

public class InfoCommand implements Command {

    private final MatrixBrain matrixBrain;

    public InfoCommand(MatrixBrain matrixBrain) {
        this.matrixBrain = matrixBrain;
    }

    @Override
    public String commandName() {
        return "info";
    }

    @Override
    public void execute(String args) {
        String[] splitArgs = args.split(" ");
        if (splitArgs.length < 1 || splitArgs.length > 3) {
            System.out.println("Invalid arguments for info. Usage: info [<matrix_name> | -all] [-asc | -desc] [[-s | -e] <number>]");
            return;
        }

        boolean allMatrices = false;
        String matrixName = null;
        String sortOrder = null;
        int showFirstN = -1;
        int showLastN = -1;

        for (int i = 0; i < splitArgs.length; i++) {
            if (matrixName != null) {
                throw new IllegalArgumentException("You can either use a matrix name on its own or -all with arguments.");
            }
            switch (splitArgs[i]) {
                case "-all" -> allMatrices = setAll(allMatrices);
                case "-asc", "-desc" -> sortOrder = getSortOrder(sortOrder, splitArgs[i]);
                case "-s" -> {
                    showFirstN = setFirstOrLast(showFirstN, showLastN, i, splitArgs);
                    i++;
                }
                case "-e" -> {
                    showLastN = setFirstOrLast(showFirstN, showLastN, i, splitArgs);
                    i++;
                }
                default -> matrixName = getMatrixName(allMatrices, splitArgs[i]);
            }
        }

        if (!allMatrices && sortOrder == null && showFirstN == -1 && showLastN == -1) {
            try {
                MatrixInfo matrixInfo = matrixBrain.getInfo(matrixName);
                System.out.println(matrixInfo);
            } catch (MatrixDoesntExistException e) {
                System.out.println(e.getMessage());
            }
        } else {
            List<MatrixInfo> matrixInfos = new ArrayList<>();
            if (allMatrices) {
                matrixInfos = matrixBrain.getAllInfo(sortOrder);
            } else if (showFirstN != -1) {
                matrixInfos = matrixBrain.getFirstNInfo(showFirstN, sortOrder);
            } else if (showLastN != -1) {
                matrixInfos = matrixBrain.getLastNInfo(showLastN, sortOrder);
            }

            for (MatrixInfo matrixInfo : matrixInfos) {
                System.out.println(matrixInfo);
            }
        }
    }

    private Integer setFirstOrLast(int showFirstN, int showLastN, int i, String[] args) {
        if (showFirstN != -1 || showLastN != -1) {
            throw new IllegalArgumentException("The -s and -e options cannot be used together or multiple times.");
        }
        if (i + 1 >= args.length) {
            throw new IllegalArgumentException(String.format("'%s' requires a number after.", args[i]));
        }

        try {
            return Integer.parseInt(args[++i]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("'%s' is not a number.", args[i]));
        }
    }

    private String getMatrixName(boolean allMatrices, String matrixName) {
        if (allMatrices) {
            throw new IllegalArgumentException("You can either use a matrix name on its own or -all with arguments.");
        }

        return matrixName;
    }

    private String getSortOrder(String sortOrder, String args) {
        if (sortOrder != null) {
            throw new IllegalArgumentException("Sort order already specified.");
        }

        sortOrder = args.substring(1);
        return sortOrder;
    }

    private boolean setAll(boolean all) {
        if (all) {
            throw new IllegalArgumentException("'-all' already specified.");
        }
        return true;
    }

}
