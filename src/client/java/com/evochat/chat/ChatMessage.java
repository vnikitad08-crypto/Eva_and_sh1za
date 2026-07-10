package com.evochat.chat;

public class ChatMessage {

    public final String text;
    public final long timestamp;
    // Animation: opacity fade-in on arrival
    public float alpha;

    public ChatMessage(String text) {
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.alpha = 0f;
    }
}
