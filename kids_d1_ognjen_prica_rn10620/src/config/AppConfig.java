package config;

public record AppConfig(int sysExplorerSleepTime, int maximumFileChunkSize, int maximumRowsSize, String startDir) {
}
