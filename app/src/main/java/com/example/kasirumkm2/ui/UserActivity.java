package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.UserAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.ActivityUserBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.AirinDialog;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserActivity extends AppCompatActivity {

    private ActivityUserBinding binding;
    private ApiService apiService;
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        setupToolbar();
        setupRecyclerView();
        setupFab();
        setupSwipeRefresh();

        loadUsers();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(this, user -> {
            Intent intent = new Intent(this, UserFormActivity.class);
            intent.putExtra("user_id", user.get("id").getAsInt());
            intent.putExtra("user_json", user.toString());
            startActivity(intent);
        });
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> {
            SessionManager sm = new SessionManager(this);
            if ("trial".equalsIgnoreCase(sm.getSubscriptionStatus())) {
                showTrialBlockDialog("Tambah Karyawan");
            } else {
                Intent intent = new Intent(this, UserFormActivity.class);
                startActivity(intent);
            }
        });
    }

    private void showTrialBlockDialog(String featureName) {
        AirinDialog.showConfirm(this,
                "Fitur Premium 💎",
                "Fitur '" + featureName + "' hanya tersedia untuk pelanggan paket Premium dan tidak dapat diakses pada paket Trial.\n\nYuk aktifkan paket premium kamu sekarang!",
                "Aktifkan Premium",
                "Batal",
                () -> {
                    Intent intent = new Intent(this, SubscriptionActivity.class);
                    startActivity(intent);
                },
                null);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.primary));
        binding.swipeRefresh.setOnRefreshListener(this::loadUsers);
    }

    private void loadUsers() {
        binding.progressBar.setVisibility(View.VISIBLE);

        apiService.getUsers().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        JsonArray dataArray = body.getAsJsonArray("data");

                        List<JsonObject> users = new ArrayList<>();
                        for (int i = 0; i < dataArray.size(); i++) {
                            users.add(dataArray.get(i).getAsJsonObject());
                        }

                        adapter.setData(users);
                        toggleEmptyState(users.isEmpty());
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal memuat data user");
                        toggleEmptyState(true);
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    CurrencyHelper.showError(binding.getRoot(), getString(R.string.terjadi_kesalahan));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void toggleEmptyState(boolean empty) {
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvUsers.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void handleUnauthorized() {
        new SessionManager(this).clearSession();
        ApiClient.resetClient();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }
}
