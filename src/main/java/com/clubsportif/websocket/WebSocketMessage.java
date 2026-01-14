package com.clubsportif.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * WebSocket message protocol for real-time communication.
 * Used for serialization/deserialization between client and server.
 */
public class WebSocketMessage {

    public enum MessageType {
        // Client -> Server
        REGISTER,           // Register client with user info
        
        // Server -> Client(s)
        NEW_MEMBER_REQUEST, // New membership request (notify admins)
        REQUEST_ACCEPTED,   // Request accepted (notify specific member)
        REQUEST_DECLINED,   // Request declined (notify specific member)
        MEMBER_STATUS_CHANGED, // Member status changed (notify admins)
        STATS_UPDATE,       // Dashboard statistics update
        DATA_REFRESH,       // Trigger table reload
        
        // Bidirectional
        PING,               // Keep-alive ping
        PONG                // Keep-alive pong
    }

    private MessageType type;
    private int targetUserId;      // For targeted messages (0 = broadcast)
    private String targetRole;     // ADMIN, MEMBER, or null for all
    private Map<String, Object> payload;
    private long timestamp;

    // Gson instance with LocalDate support
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, 
                (JsonSerializer<LocalDate>) (date, type, context) -> 
                    context.serialize(date.format(DateTimeFormatter.ISO_LOCAL_DATE)))
            .registerTypeAdapter(LocalDate.class,
                (JsonDeserializer<LocalDate>) (json, type, context) -> 
                    LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
            .create();

    public WebSocketMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public WebSocketMessage(MessageType type, Map<String, Object> payload) {
        this();
        this.type = type;
        this.payload = payload;
    }

    // Factory methods for common messages
    public static WebSocketMessage newMemberRequest(int requestId, String memberName, String subscription) {
        return new WebSocketMessage(MessageType.NEW_MEMBER_REQUEST, Map.of(
            "requestId", requestId,
            "memberName", memberName,
            "subscription", subscription
        )).forRole("ADMIN");
    }

    public static WebSocketMessage requestAccepted(int targetUserId, int requestId, String subscription, String endDate) {
        return new WebSocketMessage(MessageType.REQUEST_ACCEPTED, Map.of(
            "requestId", requestId,
            "subscription", subscription,
            "endDate", endDate
        )).forUser(targetUserId);
    }

    public static WebSocketMessage requestDeclined(int targetUserId, int requestId, String reason) {
        return new WebSocketMessage(MessageType.REQUEST_DECLINED, Map.of(
            "requestId", requestId,
            "reason", reason != null ? reason : "Request was declined by administrator"
        )).forUser(targetUserId);
    }

    public static WebSocketMessage memberStatusChanged(int memberId, String newStatus) {
        return new WebSocketMessage(MessageType.MEMBER_STATUS_CHANGED, Map.of(
            "memberId", memberId,
            "newStatus", newStatus
        )).forRole("ADMIN");
    }

    public static WebSocketMessage statsUpdate(int totalMembers, int activeMembers, int dailyRequests) {
        return new WebSocketMessage(MessageType.STATS_UPDATE, Map.of(
            "totalMembers", totalMembers,
            "activeMembers", activeMembers,
            "dailyRequests", dailyRequests
        )).forRole("ADMIN");
    }

    public static WebSocketMessage dataRefresh(String tableType) {
        return new WebSocketMessage(MessageType.DATA_REFRESH, Map.of(
            "tableType", tableType
        ));
    }

    public static WebSocketMessage register(int userId, String role) {
        return new WebSocketMessage(MessageType.REGISTER, Map.of(
            "userId", userId,
            "role", role
        ));
    }

    public static WebSocketMessage ping() {
        return new WebSocketMessage(MessageType.PING, Map.of());
    }

    public static WebSocketMessage pong() {
        return new WebSocketMessage(MessageType.PONG, Map.of());
    }

    // Builder-style methods
    public WebSocketMessage forUser(int userId) {
        this.targetUserId = userId;
        return this;
    }

    public WebSocketMessage forRole(String role) {
        this.targetRole = role;
        return this;
    }

    // Serialization
    public String toJson() {
        return GSON.toJson(this);
    }

    public static WebSocketMessage fromJson(String json) {
        return GSON.fromJson(json, WebSocketMessage.class);
    }

    // Getters and setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public int getTargetUserId() { return targetUserId; }
    public void setTargetUserId(int targetUserId) { this.targetUserId = targetUserId; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @SuppressWarnings("unchecked")
    public <T> T getPayloadValue(String key) {
        if (payload == null) return null;
        return (T) payload.get(key);
    }

    public int getPayloadInt(String key) {
        Object value = getPayloadValue(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    public String getPayloadString(String key) {
        Object value = getPayloadValue(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public String toString() {
        return "WebSocketMessage{" +
                "type=" + type +
                ", targetUserId=" + targetUserId +
                ", targetRole='" + targetRole + '\'' +
                ", payload=" + payload +
                '}';
    }
}
