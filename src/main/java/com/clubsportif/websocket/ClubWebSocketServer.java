package com.clubsportif.websocket;

import org.glassfish.tyrus.server.Server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standalone WebSocket server using Tyrus.
 * Runs on port 8025 without Spring Boot.
 */
public class ClubWebSocketServer {

    private static final String HOST = "localhost";
    private static final int PORT = 8025;
    private static final String ROOT_PATH = "/";
    
    private static Server server;
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static Thread serverThread;

    /**
     * Start the WebSocket server in a background thread.
     */
    public static void start() {
        if (running.get()) {
            System.out.println("[WebSocket Server] Already running on port " + PORT);
            return;
        }

        serverThread = new Thread(() -> {
            try {
                // Configure server properties for module access
                Map<String, Object> properties = new HashMap<>();
                
                server = new Server(HOST, PORT, ROOT_PATH, properties, ClubServerEndpoint.class);
                server.start();
                running.set(true);
                System.out.println("[WebSocket Server] Started on ws://" + HOST + ":" + PORT + "/club");
                
                // Keep thread alive
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                System.out.println("[WebSocket Server] Interrupted, shutting down...");
            } catch (Exception e) {
                System.err.println("[WebSocket Server] Failed to start: " + e.getMessage());
                e.printStackTrace();
            }
        }, "WebSocket-Server-Thread");
        
        serverThread.setDaemon(true);
        serverThread.start();
        
        // Wait briefly to ensure server starts
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stop the WebSocket server.
     */
    public static void stop() {
        if (!running.get()) {
            return;
        }

        try {
            if (server != null) {
                server.stop();
                server = null;
            }
            
            if (serverThread != null) {
                serverThread.interrupt();
                serverThread = null;
            }
            
            running.set(false);
            System.out.println("[WebSocket Server] Stopped");
        } catch (Exception e) {
            System.err.println("[WebSocket Server] Error stopping: " + e.getMessage());
        }
    }

    /**
     * Check if server is running.
     */
    public static boolean isRunning() {
        return running.get();
    }

    /**
     * Get the WebSocket URL.
     */
    public static String getWebSocketUrl() {
        return "ws://" + HOST + ":" + PORT + "/club";
    }

    /**
     * Get the port number.
     */
    public static int getPort() {
        return PORT;
    }
}

