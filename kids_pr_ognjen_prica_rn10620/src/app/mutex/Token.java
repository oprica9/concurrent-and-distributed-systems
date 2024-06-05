package app.mutex;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Token implements Serializable {

    @Serial
    private static final long serialVersionUID = -73350957468027273L;

    /**
     * Queue of requesting sites
     */
    private final Queue<Integer> Q;
    /**
     * LN[j] is the sequence number of the request which site Sj executed most recently.
     * <p>
     * After executing its CS, a site Si updates
     * <p>
     * LN[i] := RNi[i]
     * <p>
     * to indicate that its request corresponding to sequence number RNi[i] has been executed.
     * <p>
     * Token array LN[1, ..., N] permits a site to
     * determine if a site has an outstanding request for the CS.
     */
    private final Map<Integer, Integer> LN;

    public Token() {
        Q = new ConcurrentLinkedQueue<>();
        LN = new ConcurrentHashMap<>();
    }

    private Token(Queue<Integer> Q, Map<Integer, Integer> LN) {
        this.Q = Q;
        this.LN = LN;
    }

    public Queue<Integer> getQ() {
        return Q;
    }

    public Map<Integer, Integer> getLN() {
        return LN;
    }

    public Token copy() {
        return new Token(new ConcurrentLinkedQueue<>(Q), new ConcurrentHashMap<>(LN));
    }
}
