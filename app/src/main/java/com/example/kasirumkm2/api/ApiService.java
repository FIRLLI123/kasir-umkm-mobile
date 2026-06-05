package com.example.kasirumkm2.api;

import com.example.kasirumkm2.data.LoginRequest;
import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface ApiService {

    // ======================== AUTH ========================

    @POST("login")
    Call<JsonObject> login(@Body LoginRequest loginRequest);

    @GET("profile")
    Call<JsonObject> getProfile();

    @POST("logout")
    Call<JsonObject> logout();

    // ======================== CUSTOMER GROUP ========================

    @GET("customer-groups")
    Call<JsonObject> getCustomerGroups();

    // ======================== CUSTOMERS ========================

    @GET("customers")
    Call<JsonObject> getCustomers();

    @GET("customers")
    Call<JsonObject> getCustomersFiltered(@QueryMap Map<String, String> params);

    @GET("customers/{id}")
    Call<JsonObject> getCustomerDetail(@Path("id") int id);

    @POST("customers")
    Call<JsonObject> createCustomer(@Body JsonObject body);

    @PUT("customers/{id}")
    Call<JsonObject> updateCustomer(@Path("id") int id, @Body JsonObject body);

    @DELETE("customers/{id}")
    Call<JsonObject> deleteCustomer(@Path("id") int id);

    // ======================== PRODUCTS ========================

    @GET("products")
    Call<JsonObject> getProducts();

    @GET("products")
    Call<JsonObject> getProductsFiltered(@QueryMap Map<String, String> params);

    @GET("products/{id}")
    Call<JsonObject> getProductDetail(@Path("id") int id);

    @POST("products")
    Call<JsonObject> createProduct(@Body JsonObject body);

    @PUT("products/{id}")
    Call<JsonObject> updateProduct(@Path("id") int id, @Body JsonObject body);

    @DELETE("products/{id}")
    Call<JsonObject> deleteProduct(@Path("id") int id);

    // ======================== PAYMENT METHODS ========================

    @GET("payment-methods")
    Call<JsonObject> getPaymentMethods();

    // ======================== SALES ========================

    @POST("sales")
    Call<JsonObject> createSale(@Body JsonObject body);

    @GET("sales")
    Call<JsonObject> getSales();

    @GET("sales")
    Call<JsonObject> getSalesFiltered(@QueryMap Map<String, String> filters);

    @GET("sales/{id}")
    Call<JsonObject> getSaleDetail(@Path("id") int id);

    @GET("sales/{id}/receipt")
    Call<JsonObject> getReceipt(@Path("id") int id);

    @POST("sales/{id}/void")
    Call<JsonObject> voidSale(@Path("id") int id, @Body JsonObject body);

    // ======================== REPORTS ========================

    @GET("reports/daily")
    Call<JsonObject> getReportDaily();

    @GET("reports/daily")
    Call<JsonObject> getReportDailyByDate(@QueryMap Map<String, String> params);

    @GET("reports/weekly")
    Call<JsonObject> getReportWeekly();

    @GET("reports/weekly")
    Call<JsonObject> getReportWeeklyFiltered(@QueryMap Map<String, String> params);

    @GET("reports/monthly")
    Call<JsonObject> getReportMonthly();

    @GET("reports/monthly")
    Call<JsonObject> getReportMonthlyFiltered(@QueryMap Map<String, String> params);

    @GET("reports/products")
    Call<JsonObject> getReportProducts(@QueryMap Map<String, String> params);

    @GET("reports/customers")
    Call<JsonObject> getReportCustomers(@QueryMap Map<String, String> params);

    @GET("reports/payment-methods")
    Call<JsonObject> getReportPaymentMethods(@QueryMap Map<String, String> params);

    // ======================== STOCKS ========================

    @GET("stocks")
    Call<JsonObject> getStocks(@QueryMap Map<String, String> params);

    @GET("stocks/{productId}/history")
    Call<JsonObject> getStockHistory(@Path("productId") int productId, @QueryMap Map<String, String> params);

    @POST("stocks/adjustments")
    Call<JsonObject> createStockAdjustment(@Body JsonObject body);

    // ======================== APP SETTINGS ========================

    @GET("app-settings")
    Call<JsonObject> getAppSettings();

    // ======================== COMPANIES ========================

    @GET("companies")
    Call<JsonObject> getCompanies();

    @POST("companies")
    Call<JsonObject> createCompany(@Body JsonObject body);

    @PUT("companies/{id}")
    Call<JsonObject> updateCompany(@Path("id") int id, @Body JsonObject body);

    @DELETE("companies/{id}")
    Call<JsonObject> deleteCompany(@Path("id") int id);

    // ======================== USERS ========================

    @GET("users")
    Call<JsonObject> getUsers();

    @POST("users")
    Call<JsonObject> createUser(@Body JsonObject body);

    @PUT("users/{id}")
    Call<JsonObject> updateUser(@Path("id") int id, @Body JsonObject body);

    @DELETE("users/{id}")
    Call<JsonObject> deleteUser(@Path("id") int id);

    // ======================== AI CHAT ========================

    @POST("ai/chat")
    Call<JsonObject> chatWithAI(@Body JsonObject body);
}
