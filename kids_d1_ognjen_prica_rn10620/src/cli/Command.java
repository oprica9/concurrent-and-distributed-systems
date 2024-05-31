package cli;

public interface Command {
    String commandName();

    void execute(String args);
}
