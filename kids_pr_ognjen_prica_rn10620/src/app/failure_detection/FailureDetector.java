package app.failure_detection;

import app.AppConfig;
import app.Cancellable;
import app.model.ServentInfo;
import app.mutex.SuzukiKasamiMutex;
import servent.message.ping_pong.CheckSusMessage;
import servent.message.ping_pong.NewTokenHolderMessage;
import servent.message.ping_pong.PingMessage;
import servent.message.ping_pong.RestructureSystemMessage;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FailureDetector implements Runnable, Cancellable {

    public static final int PING_INTERVAL = 1000;
    public static final int WEAK_FAILURE_THRESHOLD = 4000;
    public static final int STRONG_FAILURE_THRESHOLD = 10000;
    private final Map<Integer, Long> lastResponseTimes = new ConcurrentHashMap<>();
    private final AtomicInteger tokenHolder = new AtomicInteger(-1);
    private final AtomicLong myDeclaredTime = new AtomicLong(Long.MAX_VALUE);
    private final Map<Integer, ServentInfo> chordIds = new ConcurrentHashMap<>();
    private final Set<Integer> deadServents = ConcurrentHashMap.newKeySet();
    private volatile boolean working = true;
    private transient boolean shouldBeTokenHolder = false;
    private transient boolean refactored = false;

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
                if (!deadServents.contains(chordId)) {
                    ServentInfo buddy = chordIds.get(chordId);
                    MessageUtil.sendMessage(new PingMessage(
                            AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                            buddy.getIpAddress(), buddy.getListenerPort(),
                            SuzukiKasamiMutex.hasToken()
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

    public void updateTokenHolder(int chordId) {
        tokenHolder.set(chordId);
    }

    public long getMyDeclaredTime() {
        return myDeclaredTime.get();
    }

    public void resetDeclaredTime() {
        myDeclaredTime.set(Long.MAX_VALUE);
    }

    public boolean isRefactored() {
        return refactored;
    }

    public void setRefactored(boolean refactored) {
        this.refactored = refactored;
    }

    public void updateNodeList() {
//        AppConfig.timestampedStandardPrint("Updating my buddies...");
        lastResponseTimes.clear();
        chordIds.clear();
        deadServents.clear();
        setUpBuddies();
    }

    public List<String> getDeadServents() {
        List<String> deadNodeList = new ArrayList<>();
        for (int i : deadServents) {
            ServentInfo deadServent = chordIds.get(i);
            deadNodeList.add(deadServent.getIpAddress() + ":" + deadServent.getListenerPort());
        }
        return deadNodeList;
    }

    public boolean shouldBeTokenHolder() {
        return shouldBeTokenHolder;
    }

    public void setShouldBeTokenHolder(boolean handledNewToken) {
        this.shouldBeTokenHolder = handledNewToken;
    }

    @Override
    public void stop() {
        working = false;
    }

    private void checkForFailures() {
        synchronized (AppConfig.failLock) {
            long currentTime = System.currentTimeMillis();

            if (!refactored) {
                for (Map.Entry<Integer, Long> entry : lastResponseTimes.entrySet()) {
                    int chordId = entry.getKey();
                    long lastResponse = entry.getValue();

                    if (!deadServents.contains(chordId) && currentTime - lastResponse > STRONG_FAILURE_THRESHOLD) {
                        // it's dead fr fr
                        AppConfig.timestampedStandardPrint("Node " + chordId + " is dead." + " Time passed since response: " + (currentTime - lastResponse) / 1000.0 + "s");
                        AppConfig.timestampedStandardPrint("Starting system restructuring...");

                        tellBootstrapNodeIsDead(chordIds.get(chordId));

                        deadServents.add(chordId);

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

                        // The node that died held the token, now we're the token holder (possibly)
                        System.out.println("Current token holder: " + tokenHolder.get() + ", dead node = " + chordId);
                        if (tokenHolder.get() == chordId) {
                            AppConfig.timestampedStandardPrint("I'm the new token holder!");
                            long declaredTime = System.currentTimeMillis();
                            myDeclaredTime.set(declaredTime);

                            SuzukiKasamiMutex.reset();
                            SuzukiKasamiMutex.initToken();

                            for (int i = 0; i < AppConfig.chordState.getSuccessors().length; i++) {
                                ServentInfo succInfo = AppConfig.chordState.getSuccessors()[i];
                                if (succInfo == null) {
                                    break;
                                }
                                int succChordId = succInfo.getChordId();
                                if (myChordId != succChordId && chordId != succChordId) {
                                    MessageUtil.sendMessage(new NewTokenHolderMessage(
                                            AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                                            succInfo.getIpAddress(), succInfo.getListenerPort(),
                                            declaredTime
                                    ));
                                    break;
                                }
                            }
                            tokenHolder.set(-1);
                        }

                        updateNodeList();
                    } else if (!deadServents.contains(chordId) && currentTime - lastResponse > WEAK_FAILURE_THRESHOLD) {
                        // it's sus
                        AppConfig.timestampedStandardPrint("Node " + chordId + " is suspicious." + " Time passed since last response: " + (currentTime - lastResponse) / 1000.0 + "s");

                        ServentInfo checkHelpNode = chordId == AppConfig.chordState.getPredecessor().getChordId()
                                ? AppConfig.chordState.getSuccessors()[0]
                                : AppConfig.chordState.getPredecessor();

                        AppConfig.timestampedStandardPrint("Asking " + checkHelpNode.getChordId() + " to check on " + chordId);

                        MessageUtil.sendMessage(new CheckSusMessage(
                                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                                checkHelpNode.getIpAddress(), checkHelpNode.getListenerPort(),
                                chordIds.get(chordId).getIpAddress(), chordIds.get(chordId).getListenerPort()
                        ));
                    }
                }
            }
        }
    }

    private void tellBootstrapNodeIsDead(ServentInfo serventInfo) {
        AppConfig.timestampedStandardPrint("Telling Bootstrap node " + serventInfo.getChordId() + " is dead.");
        int bsPort = AppConfig.BOOTSTRAP_PORT;
        String bsIpAddress = AppConfig.BOOTSTRAP_IP;

        try {
            Socket bsSocket = new Socket(bsIpAddress, bsPort);

            PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
            bsWriter.write("Dead\n" + serventInfo.getIpAddress() + "\n" + serventInfo.getListenerPort() + "\n");
            bsWriter.flush();

            bsSocket.close();
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint(e);
        }
    }

    private void setUpBuddies() {
        int myChordId = AppConfig.myServentInfo.getChordId();
        ServentInfo succInfo = AppConfig.chordState.getSuccessors()[0];
        ServentInfo predInfo = AppConfig.chordState.getPredecessor();
        System.out.print("My buddies: ");
        addBuddy(myChordId, succInfo);
        addBuddy(myChordId, predInfo);
        System.out.println();
    }

    private void addBuddy(int myChordId, ServentInfo predInfo) {
        if (predInfo == null) {
            return;
        }
        System.out.print(predInfo.getChordId() + " ");
        int predChordId = predInfo.getChordId();
        if (myChordId != predChordId) {
//            AppConfig.timestampedStandardPrint("My buddy is: " + predChordId);
            lastResponseTimes.put(predChordId, System.currentTimeMillis());
            chordIds.put(predChordId, predInfo);
        }
    }
}