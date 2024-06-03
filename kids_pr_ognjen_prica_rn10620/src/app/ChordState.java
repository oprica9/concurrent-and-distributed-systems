package app;

import app.model.ServentInfo;
import servent.message.dht.AskGetMessage;
import servent.message.dht.PutMessage;
import servent.message.WelcomeMessage;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * This class implements all the logic required for Chord to function.
 * It has a static method <code>chordHash</code> which will calculate our chord ids.
 * It also has a static attribute <code>CHORD_SIZE</code> that tells us what the maximum
 * key is in our system.
 * <p>
 * Other public attributes and methods:
 * <ul>
 *   <li><code>chordLevel</code> - log_2(CHORD_SIZE) - size of <code>successorTable</code></li>
 *   <li><code>successorTable</code> - a map of shortcuts in the system.</li>
 *   <li><code>predecessorInfo</code> - who is our predecessor.</li>
 *   <li><code>valueMap</code> - DHT values stored on this node.</li>
 *   <li><code>init()</code> - should be invoked when we get the WELCOME message.</li>
 *   <li><code>isCollision(int chordId)</code> - checks if a servent with that Chord ID is already active.</li>
 *   <li><code>isKeyMine(int key)</code> - checks if we have a key locally.</li>
 *   <li><code>getNextNodeForKey(int key)</code> - if next node has this key, then return it, otherwise returns the nearest predecessor for this key from my successor table.</li>
 *   <li><code>addNodes(List<ServentInfo> nodes)</code> - updates the successor table.</li>
 *   <li><code>putValue(int key, int value)</code> - stores the value locally or sends it on further in the system.</li>
 *   <li><code>getValue(int key)</code> - gets the value locally, or sends a message to get it from somewhere else.</li>
 * </ul>
 *
 * @author bmilojkovic
 */
public class ChordState {

    public static int CHORD_SIZE;
    private final ServentInfo[] successors;
    // We DO NOT use this to send messages, but only to construct the successor table
    private final List<ServentInfo> allNodes;
    private int chordLevel; // log_2(CHORD_SIZE)
    private ServentInfo predecessorInfo;
    private Map<Integer, Integer> valueMap;

    public ChordState() {
        this.chordLevel = 1;
        int tmp = CHORD_SIZE;
        while (tmp != 2) {
            if (tmp % 2 != 0) { //not a power of 2
                throw new NumberFormatException();
            }
            tmp /= 2;
            this.chordLevel++;
        }

        successors = new ServentInfo[chordLevel];
        for (int i = 0; i < chordLevel; i++) {
            successors[i] = null;
        }

        predecessorInfo = null;
        valueMap = new HashMap<>();
        allNodes = new ArrayList<>();
    }

    public static int chordHash(int value) {
        return 61 * value % CHORD_SIZE;
    }

    public static int chordHash2(String ip, int port) {
        try {
            String nodeIdentifier = ip + ":" + port;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(nodeIdentifier.getBytes(StandardCharsets.UTF_8));

            BigInteger hashValue = new BigInteger(1, hashBytes);

            return hashValue.mod(BigInteger.valueOf(ChordState.CHORD_SIZE)).intValue();

        } catch (NoSuchAlgorithmException e) {
            AppConfig.timestampedErrorPrint("Could not compute Chord ID: " + e.getMessage());
            return -1;
        }
    }

    /**
     * This should be called once after we get <code>WELCOME</code> message.
     * It sets up our initial value map and our first successor, so we can send <code>UPDATE</code>.
     * It also lets bootstrap know that we did not collide.
     */
    public void init(WelcomeMessage welcomeMsg) {
        // Set a temporary pointer to next node, for sending of update message
        successors[0] = new ServentInfo(welcomeMsg.getSenderIpAddress(), welcomeMsg.getSenderPort());
        this.valueMap = welcomeMsg.getValues();

        // Tell bootstrap this node is not a collider
        try {
            Socket bsSocket = new Socket(AppConfig.BOOTSTRAP_IP, AppConfig.BOOTSTRAP_PORT);

            PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
            bsWriter.write("New\n" + AppConfig.myServentInfo.getIpAddress() + "\n" + AppConfig.myServentInfo.getListenerPort() + "\n");

            bsWriter.flush();
            bsSocket.close();
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint(e);
        }
    }

    public boolean isCollision(int chordId) {
        if (chordId == AppConfig.myServentInfo.getChordId()) {
            AppConfig.timestampedStandardPrint("Collision happens here 1");
            return true;
        }

        for (ServentInfo serventInfo : allNodes) {
            if (serventInfo.getChordId() == chordId) {
                AppConfig.timestampedStandardPrint("Collision happens here 2");
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if we are the owner of the specified key.
     */
    public boolean isKeyMine(int key) {
        if (predecessorInfo == null) {
            return true;
        }

        int predecessorChordId = predecessorInfo.getChordId();
        int myChordId = AppConfig.myServentInfo.getChordId();

        if (predecessorChordId < myChordId) { // No overflow
            return key <= myChordId && key > predecessorChordId;
        } else { // Overflow
            return key <= myChordId || key > predecessorChordId;
        }
    }

    public int getNextNodePort() {
        return isFirstAndOnlyNode()
                ? AppConfig.myServentInfo.getListenerPort()
                : successors[0].getListenerPort();
    }

    public String getNextNodeIpAddress() {
        return isFirstAndOnlyNode()
                ? AppConfig.myServentInfo.getIpAddress()
                : successors[0].getIpAddress();
    }

    /**
     * Main chord operation - find the nearest node to hop to, to find a specific key.
     * We have to take a value that is smaller than required to make sure we don't overshoot.
     * We can only be certain we have found the required node when it is our first next node.
     */
    public ServentInfo getNextNodeForKey(int key) {
        if (isKeyMine(key)) {
            return AppConfig.myServentInfo;
        }

        // Normally we start the search from our first successor
        int startInd = 0;

        // If the key is smaller than us, and we are not the owner,
        // then all nodes up to CHORD_SIZE will never be the owner,
        // so we start the search from the first item in our table after CHORD_SIZE
        // we know that such a node must exist, because otherwise we would own this key
        if (key < AppConfig.myServentInfo.getChordId()) {
            int skip = 1;
            while (successors[skip].getChordId() > successors[startInd].getChordId()) {
                startInd++;
                skip++;
            }
        }

        int previousId = successors[startInd].getChordId();

        for (int i = startInd + 1; i < successors.length; i++) {
            if (successors[i] == null) {
                AppConfig.timestampedErrorPrint("Couldn't find successor for " + key);
                break;
            }

            int successorId = successors[i].getChordId();

            if (successorId >= key) {
                return successors[i - 1];
            }
            if (key > previousId && successorId < previousId) { //overflow
                return successors[i - 1];
            }
            previousId = successorId;
        }
        // If we have only one node in all slots in the table, we might get here
        // then we can return any item
        return successors[0];
    }

    /**
     * This method constructs an ordered list of all nodes. They are ordered by chordId, starting from this node.
     * Once the list is created, we invoke <code>updateSuccessorTable()</code> to do the rest of the work.
     */
    public void addNodes(List<ServentInfo> newNodes) {
        boolean changed = allNodes.addAll(newNodes);
        if (changed) {
            updateNodes();
        }
    }

    /**
     * This method constructs an ordered list of all nodes. They are ordered by chordId, starting from this node.
     * Once the list is created, we invoke <code>updateSuccessorTable()</code> to do the rest of the work.
     */
    public void removeNodes(List<ServentInfo> newNodes) {
        AppConfig.timestampedStandardPrint("BEFORE: " + allNodes);
        AppConfig.timestampedStandardPrint("REMOVING: " + newNodes);
        boolean changed = allNodes.removeAll(newNodes);
        AppConfig.timestampedStandardPrint("CHANGED " + changed);
        AppConfig.timestampedStandardPrint("AFTER: " + allNodes);
        if (changed) {
            updateNodes();
        }
    }

    private void updateNodes() {
        allNodes.sort(new Comparator<ServentInfo>() {

            @Override
            public int compare(ServentInfo o1, ServentInfo o2) {
                return o1.getChordId() - o2.getChordId();
            }

        });

        List<ServentInfo> newList = new ArrayList<>();
        List<ServentInfo> newList2 = new ArrayList<>();

        int myId = AppConfig.myServentInfo.getChordId();
        for (ServentInfo serventInfo : allNodes) {
            if (serventInfo.getChordId() < myId) {
                newList2.add(serventInfo);
            } else {
                newList.add(serventInfo);
            }
        }

        allNodes.clear();
        allNodes.addAll(newList);
        allNodes.addAll(newList2);
        if (newList2.size() > 0) {
            predecessorInfo = newList2.get(newList2.size() - 1);
        } else {
            if (newList.isEmpty()) {
                predecessorInfo = null; // If we're the last node standing, we ain't got no predecessor
            } else {
                predecessorInfo = newList.get(newList.size() - 1);
            }
        }

        if (!allNodes.isEmpty()) {
            updateSuccessorTable();
        } else {
            successors[0] = null;
        }
    }

    /**
     * The Chord put operation. Stores locally if key is ours, otherwise sends it on.
     */
    public void putValue(int key, int value) {
        if (isKeyMine(key)) {
            valueMap.put(key, value);
        } else {
            ServentInfo nextNode = getNextNodeForKey(key);
            PutMessage pm = new PutMessage(
                    AppConfig.myServentInfo.getIpAddress(),
                    AppConfig.myServentInfo.getListenerPort(),
                    nextNode.getIpAddress(),
                    nextNode.getListenerPort(),
                    key,
                    value
            );
            MessageUtil.sendMessage(pm);
        }
    }

    /**
     * The chord get operation. Gets the value locally if key is ours, otherwise asks someone else to give us the value.
     *
     * @return <ul>
     * <li>The value, if we have it</li>
     * <li>-1 if we own the key, but there is nothing there</li>
     * <li>-2 if we asked someone else</li>
     * </ul>
     */
    public int getValue(int key) {
        if (isKeyMine(key)) {
            return valueMap.getOrDefault(key, -1);
        }

        ServentInfo nextNode = getNextNodeForKey(key);
        AskGetMessage agm = new AskGetMessage(
                AppConfig.myServentInfo.getIpAddress(),
                AppConfig.myServentInfo.getListenerPort(),
                nextNode.getIpAddress(),
                nextNode.getListenerPort(),
                String.valueOf(key)
        );
        MessageUtil.sendMessage(agm);

        return -2;
    }

    public int getChordLevel() {
        return chordLevel;
    }

    public ServentInfo[] getSuccessors() {
        return successors;
    }

    public ServentInfo getPredecessor() {
        return predecessorInfo;
    }

    public void setPredecessor(ServentInfo newNodeInfo) {
        this.predecessorInfo = newNodeInfo;
    }

    public Map<Integer, Integer> getValueMap() {
        return valueMap;
    }

    public void setValueMap(Map<Integer, Integer> valueMap) {
        this.valueMap = valueMap;
    }

    private void updateSuccessorTable() {
        // First node after me has to be successorTable[0]

        int currentNodeIndex = 0;
        ServentInfo currentNode = allNodes.get(currentNodeIndex);
        successors[0] = currentNode;

        int currentIncrement = 2;

        ServentInfo previousNode = AppConfig.myServentInfo;

        // i is successorTable index
        for (int i = 1; i < chordLevel; i++, currentIncrement *= 2) {
            // We are looking for the node that has larger chordId than this
            int currentValue = (AppConfig.myServentInfo.getChordId() + currentIncrement) % CHORD_SIZE;

            int currentId = currentNode.getChordId();
            int previousId = previousNode.getChordId();

            // This loop needs to skip all nodes that have smaller chordId than currentValue
            while (true) {
                if (currentValue > currentId) {
                    // Before skipping, check for overflow
                    if (currentId > previousId || currentValue < previousId) {
                        // Try same value with the next node
                        previousId = currentId;
                        currentNodeIndex = (currentNodeIndex + 1) % allNodes.size();
                        currentNode = allNodes.get(currentNodeIndex);
                        currentId = currentNode.getChordId();
                    } else {
                        successors[i] = currentNode;
                        break;
                    }
                } else { // Node id is larger
                    ServentInfo nextNode = allNodes.get((currentNodeIndex + 1) % allNodes.size());
                    int nextNodeId = nextNode.getChordId();
                    // Check for overflow
                    if (nextNodeId < currentId && currentValue <= nextNodeId) {
                        // Try same value with the next node
                        previousId = currentId;
                        currentNodeIndex = (currentNodeIndex + 1) % allNodes.size();
                        currentNode = allNodes.get(currentNodeIndex);
                        currentId = currentNode.getChordId();
                    } else {
                        successors[i] = currentNode;
                        break;
                    }
                }
            }
        }
    }

    private boolean isFirstAndOnlyNode() {
        return successors[0] == null && allNodes.size() <= 1;
    }

}
