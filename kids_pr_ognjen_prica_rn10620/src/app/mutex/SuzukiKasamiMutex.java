package app.mutex;

import app.AppConfig;
import app.model.ServentInfo;
import servent.message.mutex.TokenReplyMessage;
import servent.message.mutex.TokenRequestMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SuzukiKasamiMutex {

    private static final ReentrantLock localLock = new ReentrantLock();
    public static final AtomicBoolean inCS = new AtomicBoolean(false);
    private static final ReentrantLock mutex = new ReentrantLock();
    private static final Condition welcomeResponseReceived = mutex.newCondition();
    private static boolean waitingForWelcomeResponse = false;

    // Distributed
    /**
     * RNi[j] denotes the largest sequence number received in a REQUEST message so far from site Sj
     */
    public static Map<Integer, Integer> RN = new ConcurrentHashMap<>();
    public static Token TOKEN = null;

    public static void initialize() {

    }

    public static void initToken() {
        TOKEN = new Token();
    }

    public static void lock() {
        localLock.lock();
        inCS.set(true);

        // If requesting site Si does not have the token, then it increments its
        // sequence number, RNi[i], and sends a REQUEST(i, sn) message to all
        // other sites. (“sn” is the updated value of RNi[i].)
        if (TOKEN == null) {
            incrementMyRN();

            int i = AppConfig.myServentInfo.getChordId();
            int sn = RN.get(i);

            for (ServentInfo info : AppConfig.chordState.getAllNodes()) {
                MessageUtil.sendMessage(new TokenRequestMessage(
                        AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                        info.getIpAddress(), info.getListenerPort(),
                        i, sn
                ));
            }

            while (TOKEN == null) {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void unlock() {
        // Update LN[i] to RNi[i]
        updateLN();

        // For every site Sj whose id is not in the token queue, it appends its id
        // to the token queue if RNi[j] = LN[j] + 1
        for (ServentInfo info : AppConfig.chordState.getAllNodes()) {
            int j = info.getChordId();
            if (j != AppConfig.myServentInfo.getChordId()   // Exclude myself
                    && !TOKEN.getQ().contains(j)            // Not already in the queue
                    && getRN(j) == getLN(j) + 1) {          // Check the request condition
                TOKEN.getQ().add(j);                        // Add to the token queue
            }
        }

        // If the token queue is nonempty after the above update, Si deletes the
        // top site id from the token queue and sends the token to the site indicated
        // by the id
        if (!TOKEN.getQ().isEmpty()) {
            Integer nextSiteId = TOKEN.getQ().poll(); // Get (but don't remove)
            if (nextSiteId != null) { // Check if the poll returned a valid value

                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(nextSiteId);

                MessageUtil.sendMessage(new TokenReplyMessage(
                        AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                        nextNode.getIpAddress(), nextNode.getListenerPort(),
                        TOKEN, nextSiteId
                ));
                releaseToken(); // Release the token locally
            }
        }
        localLock.unlock();
        inCS.set(false);
    }

    public static void awaitWelcomeResponse() throws InterruptedException {
        mutex.lock();
        try {
            waitingForWelcomeResponse = true;
            while (waitingForWelcomeResponse) {
                welcomeResponseReceived.await();
            }
        } finally {
            mutex.unlock();
        }
    }

    public static void signalWelcomeResponseReceived() {
        mutex.lock();
        try {
            waitingForWelcomeResponse = false;
            welcomeResponseReceived.signalAll();
        } finally {
            mutex.unlock();
        }
    }

    public static boolean isInCS() {
        return inCS.get();
    }

    public static void setRN(int j, int n) {
        int currValue = RN.getOrDefault(j, 0);
        int newVal = Math.max(currValue, n);
        RN.put(j, newVal);
    }

    public static boolean hasToken() {
        return TOKEN != null;
    }

    public static int getLN(int i) {
        return TOKEN.getLN().getOrDefault(i, 0);
    }

    public static void setToken(Token token) {
        TOKEN = token;
    }

    private static void updateLN() {
        int i = AppConfig.myServentInfo.getChordId();
        TOKEN.getLN().put(i, getRN(i));
    }

    private static Integer getRN(int i) {
        return RN.getOrDefault(i, 0);
    }

    private static void incrementMyRN() {
        int myId = AppConfig.myServentInfo.getChordId();
        int currValue = RN.getOrDefault(myId, 0);
        int newVal = currValue + 1;
        RN.put(myId, newVal);
    }

    public static void releaseToken() {
        TOKEN = null;
    }
}
