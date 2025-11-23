package model;

public class LogEntry {

    private long timestamp;
    private String userId;
    private String sessionId;
    private String actionType;
    private String targetResource;
    private int severityLevel;
    private long bytesTransferred;

    public LogEntry(long timestamp, String userId, String sessionId,
                    String actionType, String targetResource,
                    int severityLevel, long bytesTransferred) {

        this.timestamp = timestamp;
        this.userId = userId;
        this.sessionId = sessionId;
        this.actionType = actionType;
        this.targetResource = targetResource;
        this.severityLevel = severityLevel;
        this.bytesTransferred = bytesTransferred;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getTargetResource() {
        return targetResource;
    }

    public int getSeverityLevel() {
        return severityLevel;
    }

    public long getBytesTransferred() {
        return bytesTransferred;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", actionType='" + actionType + '\'' +
                ", targetResource='" + targetResource + '\'' +
                ", severityLevel=" + severityLevel +
                ", bytesTransferred=" + bytesTransferred +
                '}';
    }
}
