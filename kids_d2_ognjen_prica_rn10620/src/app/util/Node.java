package app.util;

import java.util.*;

public record Node(Integer id, List<Node> children) {
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(50);
        print(buffer, "", "");
        return buffer.toString();
    }

    private void print(StringBuilder buffer, String prefix, String childrenPrefix) {
        buffer.append(prefix);
        buffer.append(id);
        buffer.append('\n');
        for (Iterator<Node> it = children.iterator(); it.hasNext(); ) {
            Node next = it.next();
            if (it.hasNext()) {
                next.print(buffer, childrenPrefix + "+-- ", childrenPrefix + "|   ");
            } else {
                next.print(buffer, childrenPrefix + "+-- ", childrenPrefix + "    ");
            }
        }
    }

}
