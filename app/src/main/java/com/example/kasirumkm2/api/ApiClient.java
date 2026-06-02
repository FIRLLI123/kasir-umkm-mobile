package com.example.kasirumkm2.api;

import android.content.Context;
import android.content.Intent;

import com.example.kasirumkm2.config.Config;
import com.example.kasirumkm2.session.SessionManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    /**
     * Get Retrofit instance with auth interceptor
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            SessionManager sessionManager = new SessionManager(context);

            // Logging interceptor (debug)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Auth interceptor - auto inject Bearer token
            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json");

                    // Add token if available
                    String token = sessionManager.getToken();
                    if (token != null && !token.isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                    }

                    return chain.proceed(requestBuilder.build());
                }
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(Config.TIMEOUT_CONNECT, TimeUnit.SECONDS)
                    .readTimeout(Config.TIMEOUT_READ, TimeUnit.SECONDS)
                    .writeTimeout(Config.TIMEOUT_WRITE, TimeUnit.SECONDS)
                    .addInterceptor(authInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(Config.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Get ApiService singleton
     */
    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            apiService = getClient(context).create(ApiService.class);
        }
        return apiService;
    }

    /**
     * Reset client (call on logout or when BASE_URL changes)
     */
    public static void resetClient() {
        retrofit = null;
        apiService = null;
    }
}
