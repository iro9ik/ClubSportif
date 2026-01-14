package com.clubsportif.websocket;

import jakarta.websocket.*;
import javafx.application.Platform;
import org.glassfish.tyrus.client.ClientManager;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * WebSocket client service for JavaFX controllers.
 * Handles connection, auto-reconnection, and message dispatching.
 */
public class WebSocketClientService {

    private static final String WS_URL = "ws://localhost:8025/club";
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long INITIAL_RECONNECT_DELAY_MS = 1000;
    private static final long MAX_RECONNECT_DELAY_MS = 30000;

    private Session session;
    private final ClientManager client;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean shouldReconnect = new AtomicBoolean(true);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "WebSocket-Client-Scheduler");
        t.setDaemon(true);
        return t;
    });

    private int userId;
    private String userRole;
    
    // Message handlers
    private Consumer<WebSocketMessage> onNewMemberRequest;
    private Consumer<WebSocketMessage> onRequestAccepted;
    private Consumer<WebSocketMessage> onRequestDeclined;
    private Consumer<WebSocketMessage> onMemberStatusChanged;
    private Consumer<WebSocketMessage> onStatsUpdate;
    private Consumer<WebSocketMessage> onDataRefresh;
    private Consumer<Boolean> onConnectionStateChanged;

    public WebSocketClientService() {
        this.client = ClientManager.createClient();
    }

    /**
     * Connect to WebSocket server with user credentials.
     */
    public void connect(int userId, String role) {
        this.userId = userId;
        this.userRole = role;
        this.shouldReconnect.set(true);
        this.reconnectAttempts.set(0);
        
        doConnect();
    }

    private void doConnect() {
        if (connected.get()) {
            return;
        }

        try {
            // Use Configurator to pass this service to the endpoint
            ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                .configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void beforeRequest(java.util.Map<String, java.util.List<String>> headers) {
                        // Can add custom headers here if needed
                    }
                })
                .build();
            config.getUserProperties().put("service", this);
            
            session = client.connectToServer(new ClubClientEndpoint(this), new URI(WS_URL));
        } catch (Exception e) {
            System.err.println("[WebSocket Client] Connection failed: " + e.getMessage());
            scheduleReconnect();
        }
    }

    /**
     * Schedule reconnection with exponential backoff.
     */
    private void scheduleReconnect() {
        if (!shouldReconnect.get()) {
            return;
        }

        int attempts = reconnectAttempts.incrementAndGet();
        if (attempts > MAX_RECONNECT_ATTEMPTS) {
            System.err.println("[WebSocket Client] Max reconnection attempts reached");
            return;
        }

        long delay = Math.min(
            INITIAL_RECONNECT_DELAY_MS * (1L << (attempts - 1)),
            MAX_RECONNECT_DELAY_MS
        );

        System.out.println("[WebSocket Client] Reconnecting in " + delay + "ms (attempt " + attempts + ")");
        
        scheduler.schedule(this::doConnect, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Disconnect from server.
     */
    public void disconnect() {
        shouldReconnect.set(false);
        
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            System.err.println("[WebSocket Client] Error disconnecting: " + e.getMessage());
        }
        
        session = null;
        connected.set(false);
    }

    /**
     * Send a message to the server.
     */
    public void send(WebSocketMessage message) {
        if (session != null && session.isOpen()) {
            try {
                session.getAsyncRemote().sendText(message.toJson());
            } catch (Exception e) {
                System.err.println("[WebSocket Client] Failed to send: " + e.getMessage());
            }
        }
    }

    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Shutdown the client completely.
     */
    public void shutdown() {
        disconnect();
        scheduler.shutdown();
    }

    // ==================== Handler Setters ====================

    public void setOnNewMemberRequest(Consumer<WebSocketMessage> handler) {
        this.onNewMemberRequest = handler;
    }

    public void setOnRequestAccepted(Consumer<WebSocketMessage> handler) {
        this.onRequestAccepted = handler;
    }

    public void setOnRequestDeclined(Consumer<WebSocketMessage> handler) {
        this.onRequestDeclined = handler;
    }

    public void setOnMemberStatusChanged(Consumer<WebSocketMessage> handler) {
        this.onMemberStatusChanged = handler;
    }

    public void setOnStatsUpdate(Consumer<WebSocketMessage> handler) {
        this.onStatsUpdate = handler;
    }

    public void setOnDataRefresh(Consumer<WebSocketMessage> handler) {
        this.onDataRefresh = handler;
    }

    public void setOnConnectionStateChanged(Consumer<Boolean> handler) {
        this.onConnectionStateChanged = handler;
    }

    // ==================== Endpoint Callbacks (called by ClubClientEndpoint) ====================

    void handleOpen(Session session) {
        connected.set(true);
        reconnectAttempts.set(0);
        System.out.println("[WebSocket Client] Connected to server");

        // Register with server
        send(WebSocketMessage.register(userId, userRole));

        // Notify connection state change on JavaFX thread
        if (onConnectionStateChanged != null) {
            Platform.runLater(() -> onConnectionStateChanged.accept(true));
        }
    }

    void handleClose(Session session, CloseReason closeReason) {
        connected.set(false);
        System.out.println("[WebSocket Client] Connection closed: " + closeReason.getReasonPhrase());

        if (onConnectionStateChanged != null) {
            Platform.runLater(() -> onConnectionStateChanged.accept(false));
        }

        scheduleReconnect();
    }

    void handleError(Session session, Throwable throwable) {
        System.err.println("[WebSocket Client] Error: " + throwable.getMessage());
    }

    void handleMessage(String messageJson) {
        try {
            WebSocketMessage message = WebSocketMessage.fromJson(messageJson);
            dispatchMessage(message);
        } catch (Exception e) {
            System.err.println("[WebSocket Client] Failed to parse message: " + e.getMessage());
        }
    }

    /**
     * Dispatch message to appropriate handler on JavaFX thread.
     */
    private void dispatchMessage(WebSocketMessage message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case NEW_MEMBER_REQUEST -> {
                    if (onNewMemberRequest != null) onNewMemberRequest.accept(message);
                }
                case REQUEST_ACCEPTED -> {
                    if (onRequestAccepted != null) onRequestAccepted.accept(message);
                }
                case REQUEST_DECLINED -> {
                    if (onRequestDeclined != null) onRequestDeclined.accept(message);
                }
                case MEMBER_STATUS_CHANGED -> {
                    if (onMemberStatusChanged != null) onMemberStatusChanged.accept(message);
                }
                case STATS_UPDATE -> {
                    if (onStatsUpdate != null) onStatsUpdate.accept(message);
                }
                case DATA_REFRESH -> {
                    if (onDataRefresh != null) onDataRefresh.accept(message);
                }
                case PONG -> {
                    // Keep-alive response, no action needed
                }
                default -> System.out.println("[WebSocket Client] Unhandled message type: " + message.getType());
            }
        });
    }
}
