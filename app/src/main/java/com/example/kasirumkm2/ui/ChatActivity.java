package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.ChatAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.ChatMessage;
import com.example.kasirumkm2.session.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etMessage;
    private FloatingActionButton fabSend;
    private ScrollView layoutEmptyState;
    private TextView tvStatusBar;

    private ChatAdapter chatAdapter;
    private ApiService apiService;
    private SessionManager sessionManager;

    private View layoutLimitBanner;
    private TextView tvLimitMessage;
    private View btnUpgradeNow;

    // History max 20 pertukaran (40 message entries)
    private static final int MAX_HISTORY = 20;

    // Keyword yang memicu tombol dev links setelah AI balas
    private static final String DEV_QUESTION_KEYWORD = "bikin aplikasi ini";

    // Last user message, untuk retry jika gagal
    private String lastUserMessage = null;
    // Flag: apakah setelah AI balas perlu tambah tombol dev links
    private boolean pendingShowDevLinks = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);

        bindViews();
        setupRecyclerView();
        setupQuickPrompts();
        setupInput();
        setupToolbarButtons();
        setupDevLinksBehavior();

        checkAndApplyChatLimit();
        refreshChatLimit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndApplyChatLimit();
        refreshChatLimit();
    }

    private void bindViews() {
        rvChat          = findViewById(R.id.rvChat);
        etMessage       = findViewById(R.id.etMessage);
        fabSend         = findViewById(R.id.fabSend);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvStatusBar     = findViewById(R.id.tvStatusBar);

        layoutLimitBanner = findViewById(R.id.layoutLimitBanner);
        tvLimitMessage   = findViewById(R.id.tvLimitMessage);
        btnUpgradeNow     = findViewById(R.id.btnUpgradeNow);

        if (btnUpgradeNow != null) {
            btnUpgradeNow.setOnClickListener(v -> {
                Intent intent = new Intent(ChatActivity.this, SubscriptionActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(this);
        chatAdapter.setOnRetryClickListener(() -> {
            if (lastUserMessage != null) {
                // Hapus bubble error sebelum retry
                List<ChatMessage> messages = chatAdapter.getMessages();
                if (!messages.isEmpty()) {
                    ChatMessage last = messages.get(messages.size() - 1);
                    if (last.getType() == ChatMessage.Type.ERROR) {
                        messages.remove(messages.size() - 1);
                        chatAdapter.notifyItemRemoved(messages.size());
                    }
                }
                sendMessage(lastUserMessage);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);
        rvChat.setHasFixedSize(false);
    }

    private void setupDevLinksBehavior() {
        // Daftarkan listener ke adapter untuk klik tombol di dalam chat
        chatAdapter.setOnDevLinkClickListener(new ChatAdapter.OnDevLinkClickListener() {
            @Override
            public void onPortfolioClick() {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://firlli.vercel.app/"));
                startActivity(intent);
            }

            @Override
            public void onWhatsAppClick() {
                String phone = "6282249495858";
                String message = "Hallo Kak Firlli, mau tanya dong";
                String url = "https://wa.me/" + phone + "?text=" + Uri.encode(message);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });
    }

    private void setupQuickPrompts() {
        View.OnClickListener chipListener = v -> {
            String text = ((TextView) v).getText().toString();
            // Hapus emoji prefix untuk teks yang dikirim
            String cleanText = text.replaceAll("^[^\\w\\s]+\\s+", "").trim();
            // Atau kirim teks as-is supaya natural
            etMessage.setText(cleanText);
            sendMessage(cleanText);
        };

        TextView chip1 = findViewById(R.id.chipPrompt1);
        TextView chip2 = findViewById(R.id.chipPrompt2);
        TextView chip3 = findViewById(R.id.chipPrompt3);
        TextView chip4 = findViewById(R.id.chipPrompt4);
        TextView chip5 = findViewById(R.id.chipPrompt5);

        if (chip1 != null) chip1.setOnClickListener(chipListener);
        if (chip2 != null) chip2.setOnClickListener(chipListener);
        if (chip3 != null) chip3.setOnClickListener(chipListener);
        if (chip4 != null) chip4.setOnClickListener(chipListener);
        // Chip 5 = tentang developer → set flag sebelum kirim
        if (chip5 != null) chip5.setOnClickListener(v -> {
            String text = ((TextView) v).getText().toString()
                    .replaceAll("^[^\\w\\s]+\\s+", "").trim();
            pendingShowDevLinks = true;
            etMessage.setText("");
            sendMessage(text);
        });
    }

    private void setupInput() {
        fabSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                etMessage.setText("");
                sendMessage(text);
            }
        });

        // Allow send via keyboard "Send" action
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                String text = etMessage.getText().toString().trim();
                if (!TextUtils.isEmpty(text)) {
                    etMessage.setText("");
                    sendMessage(text);
                }
                return true;
            }
            return false;
        });
    }

    private void setupToolbarButtons() {
        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnClearChat = findViewById(R.id.btnClearChat);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnClearChat != null) {
            btnClearChat.setOnClickListener(v -> {
                if (chatAdapter.getItemCount() == 0) return;
                new AlertDialog.Builder(this)
                        .setTitle("Hapus Percakapan")
                        .setMessage("Semua pesan akan dihapus. Lanjutkan?")
                        .setPositiveButton("Hapus", (d, w) -> {
                            chatAdapter.clearAll();
                            lastUserMessage = null;
                            showEmptyState(true);
                            updateStatusBar("Siap membantu");
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            });
        }
    }

    // ===== CORE: Send Message =====

    private void sendMessage(String userText) {
        // Double check chat limit locally before sending
        String subStatus = sessionManager.getAiSubStatus();
        int remaining = sessionManager.getAiRemainingToday();
        if ("trial".equalsIgnoreCase(subStatus) && remaining <= 0) {
            checkAndApplyChatLimit();
            return;
        }

        // Dismiss keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        lastUserMessage = userText;

        // 1. Tambah bubble USER
        ChatMessage userMsg = new ChatMessage(userText, ChatMessage.Type.USER);
        chatAdapter.addMessage(userMsg);
        showEmptyState(false);
        scrollToBottom();

        // 2. Tambah bubble LOADING
        ChatMessage loadingMsg = ChatMessage.loading();
        chatAdapter.addMessage(loadingMsg);
        scrollToBottom();

        // 3. Update status bar
        updateStatusBar("Asisten AI sedang mengetik...");

        // 4. Siapkan request body
        JsonObject body = buildRequestBody(userText);

        // 5. Call API
        apiService.chatWithAI(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                chatAdapter.removeLoadingBubble();

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject respBody = response.body();
                        boolean success = respBody.has("success") && respBody.get("success").getAsBoolean();

                        if (success) {
                            sessionManager.updateAiChatLimit(respBody);
                            checkAndApplyChatLimit();

                            JsonObject data = respBody.getAsJsonObject("data");
                            String reply = data.get("reply").getAsString();

                            ChatMessage aiMsg = new ChatMessage(reply, ChatMessage.Type.AI);
                            chatAdapter.addMessage(aiMsg);

                            // Jika pertanyaan tentang developer, tambah tombol link di bawah balasan
                            if (pendingShowDevLinks) {
                                pendingShowDevLinks = false;
                                chatAdapter.addMessage(ChatMessage.devLinks());
                            }
                        } else {
                            String errMsg = respBody.has("message")
                                    ? respBody.get("message").getAsString()
                                    : "Terjadi kesalahan. Silakan coba lagi.";
                            showErrorBubble(errMsg);
                        }
                    } catch (Exception e) {
                        showErrorBubble("Gagal membaca respons AI. Silakan coba lagi.");
                    }
                } else {
                    // HTTP error
                    String errMsg = "Server error (" + response.code() + "). Silakan coba lagi.";
                    if (response.code() == 401) {
                        errMsg = "Sesi login habis. Silakan login ulang.";
                    } else if (response.code() == 422) {
                        errMsg = "Pesan tidak valid. Pastikan pesan tidak kosong.";
                    } else if (response.code() == 429) {
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                JsonObject errorJson = new com.google.gson.JsonParser()
                                        .parse(errorBody).getAsJsonObject();

                                sessionManager.updateAiChatLimit(errorJson);
                                checkAndApplyChatLimit();

                                if (errorJson.has("message")) {
                                    errMsg = errorJson.get("message").getAsString();
                                } else if (errorJson.has("data")) {
                                    JsonObject errData = errorJson.getAsJsonObject("data");
                                    if (errData.has("upgrade_message")) {
                                        errMsg = errData.get("upgrade_message").getAsString();
                                    }
                                }
                            } else {
                                errMsg = "Batas harian chat AI untuk akun trial sudah habis.";
                            }
                        } catch (Exception e) {
                            errMsg = "Batas harian chat AI untuk akun trial sudah habis.";
                        }
                    }
                    showErrorBubble(errMsg);
                }

                scrollToBottom();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                chatAdapter.removeLoadingBubble();
                showErrorBubble("Tidak dapat terhubung ke server. Periksa koneksi internet kamu.");
                checkAndApplyChatLimit();
                scrollToBottom();
            }
        });
    }

    private void checkAndApplyChatLimit() {
        if (sessionManager == null) return;
        String subStatus = sessionManager.getAiSubStatus();
        int remaining = sessionManager.getAiRemainingToday();
        String upgradeMessage = sessionManager.getAiUpgradeMessage();

        if ("trial".equalsIgnoreCase(subStatus)) {
            if (remaining <= 0) {
                etMessage.setEnabled(false);
                fabSend.setEnabled(false);
                if (upgradeMessage == null || upgradeMessage.isEmpty()) {
                    upgradeMessage = "Batas harian chat AI untuk akun trial hanya 3 kali. Yuk lakukan upgrade untuk menikmati akses yang lebih penuh.";
                }
                tvLimitMessage.setText(upgradeMessage);
                layoutLimitBanner.setVisibility(View.VISIBLE);
                updateStatusBar("Batas chat AI tercapai");
            } else {
                etMessage.setEnabled(true);
                fabSend.setEnabled(true);
                layoutLimitBanner.setVisibility(View.GONE);
                updateStatusBar("Online · Sisa " + remaining + " chat AI hari ini");
            }
        } else {
            etMessage.setEnabled(true);
            fabSend.setEnabled(true);
            layoutLimitBanner.setVisibility(View.GONE);
            updateStatusBar("Online");
        }
    }

    private void refreshChatLimit() {
        apiService.getProfile().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        sessionManager.updateAiChatLimit(body);
                        checkAndApplyChatLimit();
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Keep local state on error
            }
        });
    }


    // ===== HELPERS =====

    private JsonObject buildRequestBody(String message) {
        JsonObject body = new JsonObject();
        body.addProperty("message", message);

        // Build history array (max MAX_HISTORY pertukaran, artinya max 2*MAX_HISTORY messages)
        List<ChatMessage> allMessages = chatAdapter.getMessages();
        JsonArray history = new JsonArray();

        // Ambil pesan sebelumnya (exclude loading dan pesan user terakhir yang baru ditambahkan)
        int endIndex = allMessages.size() - 2; // -1 loading, -1 user terbaru
        int startIndex = Math.max(0, endIndex - (MAX_HISTORY * 2));

        for (int i = startIndex; i < endIndex; i++) {
            ChatMessage msg = allMessages.get(i);
            if (msg.getType() == ChatMessage.Type.USER || msg.getType() == ChatMessage.Type.AI) {
                JsonObject histItem = new JsonObject();
                histItem.addProperty("role", msg.getType() == ChatMessage.Type.USER ? "user" : "assistant");
                histItem.addProperty("content", msg.getContent());
                history.add(histItem);
            }
        }

        if (history.size() > 0) {
            body.add("history", history);
        }

        return body;
    }

    private void showErrorBubble(String message) {
        ChatMessage errorMsg = ChatMessage.error("❌ " + message);
        chatAdapter.addMessage(errorMsg);
        updateStatusBar("Gagal · Ketuk pesan merah untuk coba lagi");
    }

    private void showEmptyState(boolean show) {
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvChat.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void scrollToBottom() {
        rvChat.post(() -> {
            int count = chatAdapter.getItemCount();
            if (count > 0) {
                rvChat.smoothScrollToPosition(count - 1);
            }
        });
    }

    private void updateStatusBar(String text) {
        if (tvStatusBar != null) {
            tvStatusBar.setText(text);
        }
    }
}
