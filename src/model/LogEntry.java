package model;

public class LogEntry {

    private long timestamp;
    private String userId;
    private int action;

    public LogEntry(long timestamp, String userId, int action) {
        this.timestamp = timestamp;
        this.userId = userId;
        this.action = action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public int getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                ", action=" + action +
                '}';
    }
}