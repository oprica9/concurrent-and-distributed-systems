package servent.message.snapshot.li;

import java.io.Serial;
import java.io.Serializable;

public record Tag(int initId, int mkno) implements Serializable {
    @Serial
    private static final long serialVersionUID = -7059762570246439652L;
}
