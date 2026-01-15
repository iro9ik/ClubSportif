package com.clubsportif.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket server endpoint for real-time club communication.
 * Handles connections, message routing, and session management.
 */
@ServerEndpoint("/club")
public class ClubServerEndpoint {

    // Thread-safe session storage
    private static final Set<Session> allSessions = new CopyOnWriteArraySet<>();
    private static final Map<Integer, Session> userSessions = new ConcurrentHashMap<>();
    private static final Map<Session, Integer> sessionUserIds = new ConcurrentHashMap<>();
    private static final Map<Session, String> sessionRoles = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        allSessions.add(session);
        System.out.println("[WebSocket] New connection opened: " + session.getId());
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        allSessions.remove(session);
        
        Integer userId = sessionUserIds.remove(session);
        if (userId != null) {
            userSessions.remove(userId);
        }
        sessionRoles.remove(session);
        
        System.out.println("[WebSocket] Connection closed: " + session.getId() + 
                " - Reason: " + closeReason.getReasonPhrase());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("[WebSocket] Error on session " + session.getId() + ": " + throwable.getMessage());
        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(String messageJson, Session session) {
        try {
            WebSocketMessage message = WebSocketMessage.fromJson(messageJson);
            
            switch (message.getType()) {
                case REGISTER -> handleRegister(session, message);
                case PING -> sendToSession(session, WebSocketMessage.pong());
                default -> System.out.println("[WebSocket] Received message: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("[WebSocket] Error processing message: " + e.getMessage());
        }
    }

    /**
     * Register a client with user ID and role for targeted messaging.
     */
    private void handleRegister(Session session, WebSocketMessage message) {
        int userId = message.getPayloadInt("userId");
        String role = message.getPayloadString("role");
        
        if (userId > 0) {
            userSessions.put(userId, session);
            sessionUserIds.put(session, userId);
        }
        if (role != null) {
            sessionRoles.put(session, role);
        }
        
        System.out.println("[WebSocket] Registered: userId=" + userId + ", role=" + role);
    }


    /**
     * Broadcast message to all connected clients.
     */
    public static void broadcast(WebSocketMessage message) {
        String json = message.toJson();
        for (Session session : allSessions) {
            sendAsync(session, json);
        }
    }

    /**
     * Send message to a specific user by ID.
     */
    public static void sendToUser(int userId, WebSocketMessage message) {
        Session session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            sendAsync(session, message.toJson());
        }
    }

    /**
     * Send message to all users with a specific role.
     */
    public static void sendToRole(String role, WebSocketMessage message) {
        String json = message.toJson();
        for (Map.Entry<Session, String> entry : sessionRoles.entrySet()) {
            if (role.equals(entry.getValue())) {
                sendAsync(entry.getKey(), json);
            }
        }
    }

    /**
     * Smart send - routes based on message target settings.
     */
    public static void send(WebSocketMessage message) {
        if (message.getTargetUserId() > 0) {
            sendToUser(message.getTargetUserId(), message);
        } else if (message.getTargetRole() != null) {
            sendToRole(message.getTargetRole(), message);
        } else {
            broadcast(message);
        }
    }

    /**
     * Notify all admins about a new membership request.
     */
    public static void notifyNewRequest(int requestId, String memberName, String subscription) {
        send(WebSocketMessage.newMemberRequest(requestId, memberName, subscription));
    }

    /**
     * Notify member that their request was accepted.
     */
    public static void notifyRequestAccepted(int userId, int requestId, String subscription, String endDate) {
        send(WebSocketMessage.requestAccepted(userId, requestId, subscription, endDate));
    }

    /**
     * Notify member that their request was declined.
     */
    public static void notifyRequestDeclined(int userId, int requestId, String reason) {
        send(WebSocketMessage.requestDeclined(userId, requestId, reason));
    }

    /**
     * Notify admins about stats update.
     */
    public static void notifyStatsUpdate(int totalMembers, int activeMembers, int dailyRequests) {
        send(WebSocketMessage.statsUpdate(totalMembers, activeMembers, dailyRequests));
    }

    /**
     * Trigger data refresh on all clients.
     */
    public static void notifyDataRefresh(String tableType) {
        broadcast(WebSocketMessage.dataRefresh(tableType));
    }

    // ==================== Helper methods ====================

    private static void sendToSession(Session session, WebSocketMessage message) {
        if (session != null && session.isOpen()) {
            sendAsync(session, message.toJson());
        }
    }

    private static void sendAsync(Session session, String json) {
        try {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(json);
            }
        } catch (Exception e) {
            System.err.println("[WebSocket] Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Get count of connected sessions.
     */
    public static int getConnectionCount() {
        return allSessions.size();
    }

    /**
     * Get count of admin connections.
     */
    public static int getAdminConnectionCount() {
        return (int) sessionRoles.values().stream()
                .filter("ADMIN"::equals)
                .count();
    }
}
