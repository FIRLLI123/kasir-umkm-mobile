package com.example.kasirumkm2.data;

public class ChatMessage {

    public enum Type {
        USER,      // pesan dari user
        AI,        // balasan dari AI
        LOADING,   // bubble loading saat AI berpikir
        ERROR,     // pesan error + retry
        DEV_LINKS  // tombol portfolio & WA di bawah balasan tentang developer
    }

    private String content;
    private Type type;
    private long timestamp;
    private boolean isRetryable; // apakah tampilkan tombol retry

    public ChatMessage(String content, Type type) {
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.isRetryable = false;
    }

    // Konstruktor khusus untuk bubble LOADING (tidak butuh konten)
    public static ChatMessage loading() {
        return new ChatMessage("", Type.LOADING);
    }

    // Konstruktor khusus untuk error yang bisa di-retry
    public static ChatMessage error(String message) {
        ChatMessage msg = new ChatMessage(message, Type.ERROR);
        msg.isRetryable = true;
        return msg;
    }

    // Konstruktor khusus untuk tombol dev links (portfolio + WA)
    public static ChatMessage devLinks() {
        return new ChatMessage("", Type.DEV_LINKS);
    }

    // ============ GETTERS & SETTERS ============

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public long getTimestamp() { return timestamp; }

    public boolean isRetryable() { return isRetryable; }
    public void setRetryable(boolean retryable) { isRetryable = retryable; }
}
