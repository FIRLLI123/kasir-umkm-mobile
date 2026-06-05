package com.example.kasirumkm2.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.data.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER      = 1;
    private static final int VIEW_TYPE_AI        = 2;
    private static final int VIEW_TYPE_LOADING   = 3;
    private static final int VIEW_TYPE_DEV_LINKS = 4;

    public interface OnRetryClickListener {
        void onRetry();
    }

    public interface OnDevLinkClickListener {
        void onPortfolioClick();
        void onWhatsAppClick();
    }

    private final Context context;
    private final List<ChatMessage> messages = new ArrayList<>();
    private OnRetryClickListener retryListener;
    private OnDevLinkClickListener devLinkListener;

    public ChatAdapter(Context context) {
        this.context = context;
    }

    public void setOnRetryClickListener(OnRetryClickListener listener) {
        this.retryListener = listener;
    }

    public void setOnDevLinkClickListener(OnDevLinkClickListener listener) {
        this.devLinkListener = listener;
    }

    // ===== DATA OPERATIONS =====

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void removeLoadingBubble() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getType() == ChatMessage.Type.LOADING) {
                messages.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public void clearAll() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    // ===== ADAPTER CORE =====

    @Override
    public int getItemViewType(int position) {
        ChatMessage.Type type = messages.get(position).getType();
        switch (type) {
            case USER:      return VIEW_TYPE_USER;
            case LOADING:   return VIEW_TYPE_LOADING;
            case DEV_LINKS: return VIEW_TYPE_DEV_LINKS;
            default:        return VIEW_TYPE_AI; // AI + ERROR
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case VIEW_TYPE_USER:
                return new UserViewHolder(inflater.inflate(R.layout.item_chat_user, parent, false));
            case VIEW_TYPE_LOADING:
                return new LoadingViewHolder(inflater.inflate(R.layout.item_chat_loading, parent, false));
            case VIEW_TYPE_DEV_LINKS:
                return new DevLinksViewHolder(inflater.inflate(R.layout.item_chat_dev_links, parent, false));
            default:
                return new AiViewHolder(inflater.inflate(R.layout.item_chat_ai, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(message);
        } else if (holder instanceof AiViewHolder) {
            ((AiViewHolder) holder).bind(message);
        } else if (holder instanceof DevLinksViewHolder) {
            ((DevLinksViewHolder) holder).bind(devLinkListener);
        } else if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).startAnimation();
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).stopAnimation();
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ===== VIEW HOLDERS =====

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        UserViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvUserMessage);
            tvTime    = itemView.findViewById(R.id.tvUserTime);
        }

        void bind(ChatMessage msg) {
            tvMessage.setText(msg.getContent());
            tvTime.setText(formatTime(msg.getTimestamp()));
        }
    }

    // ===== DEV LINKS HOLDER =====

    static class DevLinksViewHolder extends RecyclerView.ViewHolder {
        TextView btnPortfolio, btnWhatsApp;

        DevLinksViewHolder(View itemView) {
            super(itemView);
            btnPortfolio = itemView.findViewById(R.id.btnDevPortfolio);
            btnWhatsApp  = itemView.findViewById(R.id.btnDevWhatsApp);
        }

        void bind(OnDevLinkClickListener listener) {
            if (listener == null) return;
            btnPortfolio.setOnClickListener(v -> listener.onPortfolioClick());
            btnWhatsApp.setOnClickListener(v -> listener.onWhatsAppClick());
        }
    }

    // ===== AI HOLDER =====

    class AiViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        View bubbleView;

        AiViewHolder(View itemView) {
            super(itemView);
            tvMessage  = itemView.findViewById(R.id.tvAiMessage);
            tvTime     = itemView.findViewById(R.id.tvAiTime);
            bubbleView = tvMessage;
        }

        void bind(ChatMessage msg) {
            if (msg.getType() == ChatMessage.Type.ERROR) {
                bubbleView.setBackgroundResource(R.drawable.bg_bubble_error);
                String errorText = msg.getContent();
                if (msg.isRetryable()) {
                    errorText += "\n\n🔄 Ketuk untuk kirim ulang";
                    itemView.setOnClickListener(v -> {
                        if (retryListener != null) retryListener.onRetry();
                    });
                    itemView.setClickable(true);
                } else {
                    itemView.setClickable(false);
                }
                tvMessage.setText(errorText);
                tvTime.setText("");
            } else {
                bubbleView.setBackgroundResource(R.drawable.bg_bubble_ai);
                tvMessage.setText(msg.getContent());
                tvTime.setText(formatTime(msg.getTimestamp()));
                itemView.setClickable(false);
            }
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        View dot1, dot2, dot3;
        AnimatorSet animatorSet;

        LoadingViewHolder(View itemView) {
            super(itemView);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }

        void startAnimation() {
            stopAnimation();

            ObjectAnimator anim1 = createBounce(dot1, 0);
            ObjectAnimator anim2 = createBounce(dot2, 160);
            ObjectAnimator anim3 = createBounce(dot3, 320);

            animatorSet = new AnimatorSet();
            animatorSet.playTogether(anim1, anim2, anim3);
            animatorSet.start();
        }

        void stopAnimation() {
            if (animatorSet != null) {
                animatorSet.cancel();
                animatorSet = null;
                if (dot1 != null) dot1.setTranslationY(0);
                if (dot2 != null) dot2.setTranslationY(0);
                if (dot3 != null) dot3.setTranslationY(0);
            }
        }

        private ObjectAnimator createBounce(View view, long delay) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(view, "translationY", 0f, -10f, 0f);
            anim.setDuration(600);
            anim.setStartDelay(delay);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setRepeatMode(ObjectAnimator.RESTART);
            return anim;
        }
    }

    // ===== HELPERS =====

    private static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
