package cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandInfo {
    private final String name;
    private final String description;
    private final List<String> arguments;

    public CommandInfo(String name, String description) {
        this.name = name;
        this.description = description;
        arguments = new ArrayList<>();
    }

    public CommandInfo(String name, String description, String... arguments) {
        this.name = name;
        this.description = description;
        this.arguments = new ArrayList<>();
        this.arguments.addAll(Arrays.asList(arguments));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getArguments() {
        return arguments;
    }
}
