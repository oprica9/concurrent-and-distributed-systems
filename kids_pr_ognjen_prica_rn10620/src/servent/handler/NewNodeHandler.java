package servent.handler;

import app.AppConfig;
import app.FileManager;
import app.model.ServentInfo;
import app.model.StoredFileInfo;
import app.mutex.SuzukiKasamiMutex;
import servent.message.*;
import servent.message.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class NewNodeHandler implements MessageHandler {

    private final Message clientMessage;
    private final FileManager fileManager;

    public NewNodeHandler(Message clientMessage, FileManager fileManager) {
        this.clientMessage = clientMessage;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.NEW_NODE) {
            AppConfig.timestampedErrorPrint("NEW_NODE handler got something that is not new node message.");
        }

        String newNodeIp = clientMessage.getSenderIpAddress();
        int newNodePort = clientMessage.getSenderPort();
        ServentInfo newNodeInfo = new ServentInfo(newNodeIp, newNodePort);

        // Check if the new node collides with another existing node.
        if (AppConfig.chordState.isCollision(newNodeInfo.getChordId())) {
            Message sry = new SorryMessage(
                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                    newNodeInfo.getIpAddress(), newNodeInfo.getListenerPort()
            );
            MessageUtil.sendMessage(sry);
            return;
        }

        System.out.println("LOCK FOR: " + newNodeInfo.getChordId() + ", My token: " + SuzukiKasamiMutex.TOKEN);
        SuzukiKasamiMutex.lock();
        System.out.println("CRITICAL SECTION FOR: " + newNodeInfo.getChordId());

        // Check if he is my predecessor
        boolean isMyPred = AppConfig.chordState.isKeyMine(newNodeInfo.getChordId());
        if (isMyPred) {

            // If he is my predecessor, prepare and send a welcome message
            ServentInfo hisPred = AppConfig.chordState.getPredecessor();
            if (hisPred == null) {
                hisPred = AppConfig.myServentInfo;
            }

            AppConfig.chordState.setPredecessor(newNodeInfo);

            // Set his values
            Map<Integer, Integer> myValues = AppConfig.chordState.getValueMap();
            Map<Integer, Integer> hisValues = getHisValues(hisPred, newNodeInfo, myValues);

            // Remove HIS values from MY map
            for (Integer key : hisValues.keySet()) {
                myValues.remove(key);
            }

            AppConfig.chordState.setValueMap(myValues);

            // Set his files
//            AppConfig.timestampedStandardPrint("Refactoring my file map...\n" + fileManager.getFiles());
            Map<String, StoredFileInfo> myFiles = fileManager.getFiles();
            Map<String, StoredFileInfo> hisFiles = getHisFiles(hisPred, newNodeInfo, myFiles);
//            AppConfig.timestampedStandardPrint("His file map before:\n" + hisFiles);

            for (String key : hisFiles.keySet()) {
                myFiles.remove(key);
            }

            fileManager.setFiles(myFiles);
//            AppConfig.timestampedStandardPrint("My new file map:\n" + fileManager.getFiles());
//            AppConfig.timestampedStandardPrint("His file map after:\n" + hisFiles);

            // Send the info
            WelcomeMessage wm = new WelcomeMessage(
                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                    newNodeInfo.getIpAddress(), newNodeInfo.getListenerPort(),
                    hisValues,
                    hisFiles
            );
            MessageUtil.sendMessage(wm);

            try {
                SuzukiKasamiMutex.awaitWelcomeResponse(); // Use a condition variable
            } catch (InterruptedException e) {
                AppConfig.timestampedErrorPrint("Interrupted while waiting for Welcome response.");
                Thread.currentThread().interrupt(); // Preserve the interruption status
            }

        } else {
            // If he is not my predecessor, let someone else take care of it
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(newNodeInfo.getChordId());
            NewNodeMessage nnm = new NewNodeMessage(
                    newNodeInfo.getIpAddress(), newNodeInfo.getListenerPort(),
                    nextNode.getIpAddress(), nextNode.getListenerPort()
            );
            MessageUtil.sendMessage(nnm);
        }
        SuzukiKasamiMutex.unlock();
    }

    private Map<String, StoredFileInfo> getHisFiles(ServentInfo hisPred, ServentInfo newNodeInfo, Map<String, StoredFileInfo> myFiles) {
        Map<String, StoredFileInfo> hisFiles = new HashMap<>();

        int myId = AppConfig.myServentInfo.getChordId();
        int hisPredId = hisPred.getChordId();
        int newNodeId = newNodeInfo.getChordId();

        for (Entry<String, StoredFileInfo> fileEntry : myFiles.entrySet()) {
            StoredFileInfo info = fileEntry.getValue();
            int hashValue = FileManager.fileHash(fileEntry.getKey());

            if (hisPredId == myId) {
                // I am first and he is second
                if (myId < newNodeId) {
                    if (hashValue <= newNodeId && hashValue > myId) {
                        hisFiles.put(fileEntry.getKey(), info);
                    }
                } else {
                    if (hashValue <= newNodeId || hashValue > myId) {
                        hisFiles.put(fileEntry.getKey(), info);
                    }
                }
            }
            if (hisPredId < myId) {
                // My old predecessor was before me
                if (hashValue <= newNodeId) {
                    hisFiles.put(fileEntry.getKey(), info);
                }
            } else {
                // My old predecessor was after me
                if (hisPredId > newNodeId) { //new node overflow
                    if (hashValue <= newNodeId || hashValue > hisPredId) {
                        hisFiles.put(fileEntry.getKey(), info);
                    }
                } else {
                    // No new node overflow
                    if (hashValue <= newNodeId && hashValue > hisPredId) {
                        hisFiles.put(fileEntry.getKey(), info);
                    }

                }
            }
        }
        return hisFiles;
    }

    private static Map<Integer, Integer> getHisValues(ServentInfo hisPred, ServentInfo
            newNodeInfo, Map<Integer, Integer> myValues) {
        Map<Integer, Integer> hisValues = new HashMap<>();

        int myId = AppConfig.myServentInfo.getChordId();
        int hisPredId = hisPred.getChordId();
        int newNodeId = newNodeInfo.getChordId();

        for (Entry<Integer, Integer> valueEntry : myValues.entrySet()) {
            if (hisPredId == myId) {
                // I am first and he is second
                if (myId < newNodeId) {
                    if (valueEntry.getKey() <= newNodeId && valueEntry.getKey() > myId) {
                        hisValues.put(valueEntry.getKey(), valueEntry.getValue());
                    }
                } else {
                    if (valueEntry.getKey() <= newNodeId || valueEntry.getKey() > myId) {
                        hisValues.put(valueEntry.getKey(), valueEntry.getValue());
                    }
                }
            }
            if (hisPredId < myId) {
                // My old predecessor was before me
                if (valueEntry.getKey() <= newNodeId) {
                    hisValues.put(valueEntry.getKey(), valueEntry.getValue());
                }
            } else {
                // My old predecessor was after me
                if (hisPredId > newNodeId) { //new node overflow
                    if (valueEntry.getKey() <= newNodeId || valueEntry.getKey() > hisPredId) {
                        hisValues.put(valueEntry.getKey(), valueEntry.getValue());
                    }
                } else {
                    // No new node overflow
                    if (valueEntry.getKey() <= newNodeId && valueEntry.getKey() > hisPredId) {
                        hisValues.put(valueEntry.getKey(), valueEntry.getValue());
                    }
                }
            }
        }
        return hisValues;
    }
}
