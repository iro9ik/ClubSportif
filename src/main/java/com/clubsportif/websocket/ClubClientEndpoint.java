package com.clubsportif.websocket;

import jakarta.websocket.*;

/**
 * WebSocket client endpoint implementation.
 * Must be public class for Tyrus reflection access via module system.
 */
@ClientEndpoint
public class ClubClientEndpoint {

    private final WebSocketClientService service;

    public ClubClientEndpoint(WebSocketClientService service) {
        this.service = service;
    }

    @OnOpen
    public void onOpen(Session session) {
        service.handleOpen(session);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        service.handleClose(session, closeReason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        service.handleError(session, throwable);
    }

    @OnMessage
    public void onMessage(String messageJson) {
        service.handleMessage(messageJson);
    }
}
