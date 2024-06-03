package app.model;

import app.ChordState;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * This is an immutable class that holds all the information for a servent.
 *
 * @author bmilojkovic
 */
public class ServentInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 5304170042791281555L;
    private final String ipAddress;
    private final int listenerPort;
    private final int chordId;
    private final int weakFailureConsistency;
    private final int strongFailureConsistency;

    public ServentInfo(String ipAddress, int listenerPort) {
        this.ipAddress = ipAddress;
        this.listenerPort = listenerPort;
        this.chordId = ChordState.chordHash2(ipAddress, listenerPort);
        this.weakFailureConsistency = -1;
        this.strongFailureConsistency = -1;
    }

    public ServentInfo(String ipAddress, int listenerPort, int weakFailureConsistency, int strongFailureConsistency) {
        this.ipAddress = ipAddress;
        this.listenerPort = listenerPort;
        this.chordId = ChordState.chordHash2(ipAddress, listenerPort);
        this.weakFailureConsistency = weakFailureConsistency;
        this.strongFailureConsistency = strongFailureConsistency;
    }


    public String getIpAddress() {
        return ipAddress;
    }

    public int getListenerPort() {
        return listenerPort;
    }

    public int getChordId() {
        return chordId;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        sb.append("Chord ID: ").append(chordId).append(", ");
        sb.append("IP: ").append(ipAddress).append(", ");
        sb.append("Port: ").append(listenerPort);

        // Optional: Only add failure consistency if they are not the default -1
        if (weakFailureConsistency != -1) {
            sb.append(", Weak Fail: ").append(weakFailureConsistency);
        }
        if (strongFailureConsistency != -1) {
            sb.append(", Strong Fail: ").append(strongFailureConsistency);
        }

        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ServentInfo info = (ServentInfo) object;
        return listenerPort == info.listenerPort && chordId == info.chordId && Objects.equals(ipAddress, info.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, listenerPort, chordId);
    }
}
