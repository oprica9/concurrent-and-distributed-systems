package app.friend_manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FriendManager {

    private final Map<Integer, String> friends = new ConcurrentHashMap<>();
    private final Map<Integer, String> friendRequests = new ConcurrentHashMap<>();

    public void addFriend(int requesterHash, String ip, int port) {
        friendRequests.remove(requesterHash);
        friends.put(requesterHash, getAddress(ip, port));
    }

    public boolean isFriend(int requesterHash) {
        return friends.containsKey(requesterHash);
    }

    public void addFriendRequest(int requesterHash, String senderIpAddress, int senderPort) {
        friendRequests.put(requesterHash, getAddress(senderIpAddress, senderPort));
    }

    public boolean haveRequest(int requesterHash) {
        return friendRequests.containsKey(requesterHash);
    }

    public void printFriends() {
        StringBuilder builder = new StringBuilder();
        builder.append("Friends:\n");
        printMap(builder, friends);
    }

    public void printFriendRequests() {
        StringBuilder builder = new StringBuilder();
        builder.append("Friend requests:\n");
        printMap(builder, friendRequests);
    }

    public String getAddress(String ip, int port) {
        return ip + ":" + port;
    }

    private void printMap(StringBuilder builder, Map<Integer, String> map) {
        for (Map.Entry<Integer, String> friendEntry : map.entrySet()) {
            builder.append("\t");
            builder.append(friendEntry.getValue());
            builder.append("\n");
        }
        builder.deleteCharAt(builder.length() - 1);
        System.out.println(builder);
    }
}
