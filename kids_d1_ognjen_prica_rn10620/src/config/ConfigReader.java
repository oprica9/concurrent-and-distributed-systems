package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {

    private final static String APP_PROPERTIES = "app.properties";

    public static AppConfig importConfiguration(String configName) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configName + File.separator + APP_PROPERTIES));

        } catch (IOException e) {
            System.out.println("Couldn't open properties file. Exiting...");
            System.exit(0);
        }

        int sysExplorerSleepTime = -1;
        int maximumFileChunkSize = -1;
        int maximumRowsSize = -1;
        try {
            sysExplorerSleepTime = Integer.parseInt(properties.getProperty("sys_explorer_sleep_time"));
            maximumFileChunkSize = Integer.parseInt(properties.getProperty("maximum_file_chunk_size"));
            maximumRowsSize = Integer.parseInt(properties.getProperty("maximum_rows_size"));
        } catch (NumberFormatException e) {
            System.out.println("Problem parsing properties. Exiting...");
            System.exit(0);
        }

        String startDir = properties.getProperty("start_dir");

        return new AppConfig(sysExplorerSleepTime, maximumFileChunkSize, maximumRowsSize, startDir);
    }

}
