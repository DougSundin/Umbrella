package com.example.whetherornot.data.repository;

import com.example.whetherornot.data.api.WeatherApiService;
import com.example.whetherornot.data.model.WeatherResponse;
import com.example.whetherornot.data.model.ZipCodeResponse;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Repository class for handling weather data operations in Java
 * Follows Repository pattern for data abstraction
 */
public class JavaWeatherRepository {

    private final WeatherApiService apiService;
    private final Gson gson;

    public JavaWeatherRepository() {
        try {
            // Setup HTTP logging interceptor for debugging
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Configure OkHttp client with timeouts
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            // Setup Retrofit
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(WeatherApiService.BASE_URL)
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(WeatherApiService.class);
            gson = new Gson();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize weather repository: " + e.getMessage(), e);
        }
    }

    /**
     * Interface for weather data callbacks
     */
    public interface WeatherDataCallback {
        void onSuccess(WeatherResponse weatherResponse);
        void onError(String errorMessage);
    }

    /**
     * Interface for JSON string callbacks
     */
    public interface JsonDataCallback {
        void onSuccess(String jsonData);
        void onError(String errorMessage);
    }

    /**
     * Interface for coordinates callbacks
     */
    public interface CoordinatesCallback {
        void onSuccess(double latitude, double longitude);
        void onError(String errorMessage);
    }

    /**
     * Interface for location and weather callbacks
     */
    public interface LocationWeatherCallback {
        void onLocationReceived(double latitude, double longitude, String locationName);
        void onWeatherSuccess(String jsonData);
        void onError(String errorMessage);
    }

    /**
     * Fetch weather data from API
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param callback Callback to handle response
     */
    public void getWeatherData(double latitude, double longitude, WeatherDataCallback callback) {
        Call<WeatherResponse> call = apiService.getWeatherDataCall(latitude, longitude, "minutely,alerts",
                WeatherApiService.API_KEY, "imperial");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("API call failed: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Get raw JSON response as string for debugging purposes
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param callback Callback to handle response
     */
    public void getWeatherDataAsJson(double latitude, double longitude, JsonDataCallback callback) {
        Call<WeatherResponse> call = apiService.getWeatherDataCall(latitude, longitude, "minutely,alerts",
                WeatherApiService.API_KEY, "imperial");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = gson.toJson(response.body());
                        callback.onSuccess(jsonString);
                    } catch (Exception e) {
                        callback.onError("JSON conversion error: " + e.getMessage());
                    }
                } else {
                    callback.onError("API call failed: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Get coordinates from zip code using Geocoding API
     * @param zipCode Zip code (e.g., "90210")
     * @param countryCode Country code (default: "US")
     * @param callback Callback to handle response
     */
    public void getCoordinatesFromZip(String zipCode, String countryCode, CoordinatesCallback callback) {
        String zipQuery = zipCode + "," + countryCode;
        Call<ZipCodeResponse> call = apiService.getCoordinatesFromZipCall(zipQuery, WeatherApiService.API_KEY);

        call.enqueue(new Callback<ZipCodeResponse>() {
            @Override
            public void onResponse(Call<ZipCodeResponse> call, Response<ZipCodeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ZipCodeResponse zipResponse = response.body();
                    callback.onSuccess(zipResponse.getLat(), zipResponse.getLon());
                } else {
                    callback.onError("Geocoding API call failed: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ZipCodeResponse> call, Throwable t) {
                callback.onError("Geocoding network error: " + t.getMessage());
            }
        });
    }

    /**
     * Get weather data by zip code (combines geocoding and weather calls)
     * @param zipCode Zip code (e.g., "90210")
     * @param countryCode Country code (default: "US")
     * @param callback Callback to handle response
     */
    public void getWeatherDataByZip(String zipCode, String countryCode, JsonDataCallback callback) {
        android.util.Log.d("JavaWeather", "Getting coordinates for zip code: " + zipCode);
        // First get coordinates from zip code
        getCoordinatesFromZip(zipCode, countryCode, new CoordinatesCallback() {
            @Override
            public void onSuccess(double latitude, double longitude) {
                android.util.Log.d("JavaWeather", "Got coordinates from zip " + zipCode + ": lat=" + latitude + ", lon=" + longitude);
                // Then get weather data using those coordinates
                android.util.Log.d("JavaWeather", "Calling weather API with coordinates: lat=" + latitude + ", lon=" + longitude);
                getWeatherDataAsJson(latitude, longitude, callback);
            }

            @Override
            public void onError(String errorMessage) {
                android.util.Log.e("JavaWeather", "Failed to get coordinates for zip " + zipCode + ": " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * Convenience method for US zip codes
     */
    public void getCoordinatesFromZip(String zipCode, CoordinatesCallback callback) {
        getCoordinatesFromZip(zipCode, "US", callback);
    }

    /**
     * Convenience method for US zip codes
     */
    public void getWeatherDataByZip(String zipCode, JsonDataCallback callback) {
        getWeatherDataByZip(zipCode, "US", callback);
    }

    /**
     * Get current location and fetch weather data
     * @param callback Callback to handle location and weather responses
     * @param context Android context for location services
     */
    public void getCurrentLocationAndWeather(LocationWeatherCallback callback, android.content.Context context) {
        // Use the LocationManager to get current location
        com.example.whetherornot.utils.LocationManager locationManager =
            new com.example.whetherornot.utils.LocationManager(context);

        // Check if location permission is granted
        if (!locationManager.hasLocationPermission()) {
            callback.onError("Location permission not granted");
            return;
        }

        // Get current location in a background thread
        new Thread(() -> {
            try {
                // This is a simplified approach - in a real app you'd want to handle this more elegantly
                // For now, we'll use a blocking approach with timeout
                com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient =
                    com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context);

                com.google.android.gms.tasks.CancellationTokenSource cancellationTokenSource =
                    new com.google.android.gms.tasks.CancellationTokenSource();

                fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.getToken()
                ).addOnSuccessListener(location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        String locationName = String.format("Current Location (%.4f°N, %.4f°W)",
                            latitude, Math.abs(longitude));

                        // Notify about location
                        callback.onLocationReceived(latitude, longitude, locationName);

                        // Now fetch weather data
                        getWeatherDataAsJson(latitude, longitude, new JsonDataCallback() {
                            @Override
                            public void onSuccess(String jsonData) {
                                callback.onWeatherSuccess(jsonData);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                callback.onError(errorMessage);
                            }
                        });
                    } else {
                        callback.onError("Unable to get current location");
                    }
                }).addOnFailureListener(e -> {
                    callback.onError("Location error: " + e.getMessage());
                });

            } catch (SecurityException e) {
                callback.onError("Location permission denied: " + e.getMessage());
            } catch (Exception e) {
                callback.onError("Error getting location: " + e.getMessage());
            }
        }).start();
    }
}
