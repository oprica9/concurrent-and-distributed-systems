package app.failure_detection;

import app.AppConfig;
import app.Cancellable;
import app.model.ServentInfo;
import servent.message.ping_pong.CheckSusNodeMessage;
import servent.message.ping_pong.PingMessage;
import servent.message.ping_pong.RestructureSystemMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FailureDetector implements Runnable, Cancellable {

    private volatile boolean working = true;
    private final int PING_INTERVAL = 1000; // Ping every 1 second
    private final int WEAK_FAILURE_THRESHOLD = 4000;
    private final int STRONG_FAILURE_THRESHOLD = 10000;

    private final Map<Integer, Long> lastResponseTimes = new ConcurrentHashMap<>();
    private final Map<Integer, ServentInfo> chordIds = new ConcurrentHashMap<>();
    private final Set<Integer> deadNodes = ConcurrentHashMap.newKeySet();

    public FailureDetector() {
    }

    @Override
    public void run() {
        while (working) {
            try {
                Thread.sleep(PING_INTERVAL);
            } catch (InterruptedException e) {
                AppConfig.timestampedErrorPrint("Failure detector interrupted.");
            }

            // Send PING messages to buddies
            for (int chordId : chordIds.keySet()) {
                if (!deadNodes.contains(chordId)) {
                    ServentInfo buddy = chordIds.get(chordId);
                    MessageUtil.sendMessage(new PingMessage(
                            AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                            buddy.getIpAddress(), buddy.getListenerPort()
                    ));
                }
            }

            // Small delay before checking for responses
            try {
                Thread.sleep(WEAK_FAILURE_THRESHOLD / 2);
            } catch (InterruptedException e) {
                AppConfig.timestampedErrorPrint("Failure detector interrupted.");
            }

            // Check for failures
            checkForFailures();
        }
    }

    public void updateLastResponseTime(int chordId) {
        if (lastResponseTimes.containsKey(chordId)) {
            lastResponseTimes.put(chordId, System.currentTimeMillis());
        }
    }

    public void updateNodeList() {
//        AppConfig.timestampedStandardPrint("Updating my buddies...");
        lastResponseTimes.clear();
        chordIds.clear();
        deadNodes.clear();
        setUpBuddies();
    }

    public String getDeadNodes() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i : deadNodes) {
            ServentInfo deadServent = chordIds.get(i);
            stringBuilder.append(deadServent.getIpAddress()).append(":").append(deadServent.getListenerPort());
            stringBuilder.append(",");
        }
        if (!stringBuilder.isEmpty()) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();

    }

    @Override
    public void stop() {
        working = false;
    }

    private void checkForFailures() {
        synchronized (AppConfig.failLock) {
            long currentTime = System.currentTimeMillis();

            for (Map.Entry<Integer, Long> entry : lastResponseTimes.entrySet()) {
                int chordId = entry.getKey();
                long lastResponse = entry.getValue();

                if (!deadNodes.contains(chordId) && currentTime - lastResponse > STRONG_FAILURE_THRESHOLD) {
                    // it's dead fr fr
                    AppConfig.timestampedStandardPrint("Node " + chordId + " is dead." + " Time passed since response: " + (currentTime - lastResponse) / 1000.0 + "s");
                    AppConfig.timestampedStandardPrint("Starting system restructuring...");
                    deadNodes.add(chordId);

                    List<ServentInfo> died = new ArrayList<>();
                    died.add(chordIds.get(chordId));

                    AppConfig.chordState.removeNodes(died);

                    int myChordId = AppConfig.myServentInfo.getChordId();
                    for (int i = 0; i < AppConfig.chordState.getSuccessors().length; i++) {
                        ServentInfo succInfo = AppConfig.chordState.getSuccessors()[i];
                        if (succInfo == null) {
                            break;
                        }
                        int succChordId = succInfo.getChordId();
                        if (myChordId != succChordId && chordId != succChordId) {
                            MessageUtil.sendMessage(new RestructureSystemMessage(
                                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                                    succInfo.getIpAddress(), succInfo.getListenerPort(),
                                    chordIds.get(chordId).getIpAddress(), chordIds.get(chordId).getListenerPort()
                            ));
                            break;
                        }
                    }

                    updateNodeList();
                } else if (!deadNodes.contains(chordId) && currentTime - lastResponse > WEAK_FAILURE_THRESHOLD) {
                    // it's sus
                    AppConfig.timestampedStandardPrint("Node " + chordId + " is suspicious." + " Time passed since last response: " + (currentTime - lastResponse) / 1000.0 + "s");

                    ServentInfo checkHelpNode = chordId == AppConfig.chordState.getPredecessor().getChordId()
                            ? AppConfig.chordState.getSuccessors()[0]
                            : AppConfig.chordState.getPredecessor();

                    AppConfig.timestampedStandardPrint("Asking " + checkHelpNode.getChordId() + " to check on " + chordId);

                    MessageUtil.sendMessage(new CheckSusNodeMessage(
                            AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                            checkHelpNode.getIpAddress(), checkHelpNode.getListenerPort(),
                            chordIds.get(chordId).getIpAddress(), chordIds.get(chordId).getListenerPort()
                    ));
                }
            }
        }
    }

    private void setUpBuddies() {
        int myChordId = AppConfig.myServentInfo.getChordId();
        ServentInfo succInfo = AppConfig.chordState.getSuccessors()[0];
        ServentInfo predInfo = AppConfig.chordState.getPredecessor();
        addBuddy(myChordId, succInfo);
        addBuddy(myChordId, predInfo);
    }

    private void addBuddy(int myChordId, ServentInfo predInfo) {
        if (predInfo == null) {
            return;
        }

        int predChordId = predInfo.getChordId();
        if (myChordId != predChordId) {
//            AppConfig.timestampedStandardPrint("My buddy is: " + predChordId);
            lastResponseTimes.put(predChordId, System.currentTimeMillis());
            chordIds.put(predChordId, predInfo);
        }
    }
}