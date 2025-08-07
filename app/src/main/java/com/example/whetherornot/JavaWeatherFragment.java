package com.example.whetherornot;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.whetherornot.data.repository.JavaWeatherRepository;

/**
 * Java-based Fragment for weather functionality
 * This fragment will contain the Java implementation of the weather app
 * using traditional Android Views and XML layouts
 */
public class JavaWeatherFragment extends Fragment {

    private static final String TAG = "JavaWeatherFragment";

    private TextView titleTextView;
    private TextView locationTextView;
    private Button fetchDataButton;
    private ProgressBar loadingProgressBar;
    private TextView errorTextView;
    private TextView jsonLabelTextView;
    private TextView jsonContentTextView;
    private ScrollView jsonScrollView;

    private JavaWeatherRepository repository;

    // Test coordinates: Duluth, MN
    private static final double LATITUDE = 46.8384;
    private static final double LONGITUDE = -92.1800;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Initializing JavaWeatherFragment");
        try {
            repository = new JavaWeatherRepository();
            Log.d(TAG, "onCreate: Repository initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Failed to initialize repository", e);
            throw e;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Inflating layout");
        try {
            View view = inflater.inflate(R.layout.fragment_java_weather, container, false);
            Log.d(TAG, "onCreateView: Layout inflated successfully");
            return view;
        } catch (Exception e) {
            Log.e(TAG, "onCreateView: Failed to inflate layout", e);
            throw e;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Starting view setup");

        try {
            // Initialize views
            initializeViews(view);
            Log.d(TAG, "onViewCreated: Views initialized");

            // Setup initial content
            setupInitialContent();
            Log.d(TAG, "onViewCreated: Initial content setup");

            // Setup click listeners
            setupClickListeners();
            Log.d(TAG, "onViewCreated: Click listeners setup complete");
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated: Error during view setup", e);
            throw e;
        }
    }

    /**
     * Initialize all view references
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        Log.d(TAG, "initializeViews: Finding views by ID");
        try {
            titleTextView = view.findViewById(R.id.java_title_text);
            Log.d(TAG, "Found titleTextView: " + (titleTextView != null));

            locationTextView = view.findViewById(R.id.java_location_text);
            Log.d(TAG, "Found locationTextView: " + (locationTextView != null));

            fetchDataButton = view.findViewById(R.id.fetch_data_button);
            Log.d(TAG, "Found fetchDataButton: " + (fetchDataButton != null));

            loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
            Log.d(TAG, "Found loadingProgressBar: " + (loadingProgressBar != null));

            errorTextView = view.findViewById(R.id.error_text_view);
            Log.d(TAG, "Found errorTextView: " + (errorTextView != null));

            jsonLabelTextView = view.findViewById(R.id.json_label_text);
            Log.d(TAG, "Found jsonLabelTextView: " + (jsonLabelTextView != null));

            jsonContentTextView = view.findViewById(R.id.json_content_text);
            Log.d(TAG, "Found jsonContentTextView: " + (jsonContentTextView != null));

            jsonScrollView = view.findViewById(R.id.json_scroll_view);
            Log.d(TAG, "Found jsonScrollView: " + (jsonScrollView != null));

        } catch (Exception e) {
            Log.e(TAG, "initializeViews: Error finding views", e);
            throw e;
        }
    }

    /**
     * Setup the initial content for the Java weather tab
     */
    private void setupInitialContent() {
        if (titleTextView != null) {
            titleTextView.setText("Java Weather Implementation");
        }

        if (locationTextView != null) {
            locationTextView.setText(String.format("Location: Duluth, MN (%.4f°N, %.4f°W)",
                    LATITUDE, Math.abs(LONGITUDE)));
        }

        // Initially hide loading, error, and JSON views
        hideAllResponseViews();
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        Log.d(TAG, "setupClickListeners: Setting up button click listener");
        try {
            if (fetchDataButton != null) {
                fetchDataButton.setOnClickListener(v -> {
                    Log.d(TAG, "Button clicked: Starting weather data fetch");
                    fetchWeatherData();
                });
                Log.d(TAG, "setupClickListeners: Click listener set successfully");
            } else {
                Log.e(TAG, "setupClickListeners: fetchDataButton is null!");
            }
        } catch (Exception e) {
            Log.e(TAG, "setupClickListeners: Error setting up click listeners", e);
            throw e;
        }
    }

    /**
     * Fetch weather data from the API
     */
    private void fetchWeatherData() {
        Log.d(TAG, "fetchWeatherData: Starting API call");
        try {
            // Show loading state
            showLoadingState();
            Log.d(TAG, "fetchWeatherData: Loading state shown");

            Log.d(TAG, "fetchWeatherData: Making API call with lat=" + LATITUDE + ", lon=" + LONGITUDE);
            repository.getWeatherDataAsJson(LATITUDE, LONGITUDE, new JavaWeatherRepository.JsonDataCallback() {
                @Override
                public void onSuccess(String jsonData) {
                    Log.d(TAG, "API Success: Received data length: " + (jsonData != null ? jsonData.length() : "null"));
                    // Update UI on main thread
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                Log.d(TAG, "Updating UI with success state");
                                showSuccessState(jsonData);
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating UI with success state", e);
                                showErrorState("UI update error: " + e.getMessage());
                            }
                        });
                    } else {
                        Log.w(TAG, "Activity is null or fragment not added, skipping UI update");
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "API Error: " + errorMessage);
                    // Update UI on main thread
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                Log.d(TAG, "Updating UI with error state");
                                showErrorState(errorMessage);
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating UI with error state", e);
                                showErrorState("Error handling failed: " + e.getMessage());
                            }
                        });
                    } else {
                        Log.w(TAG, "Activity is null or fragment not added, skipping error UI update");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "fetchWeatherData: Critical error during API call setup", e);
            showErrorState("Failed to initiate API call: " + e.getMessage());
        }
    }

    /**
     * Show loading state in UI
     */
    private void showLoadingState() {
        hideAllResponseViews();
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
        if (fetchDataButton != null) {
            fetchDataButton.setEnabled(false);
        }
    }

    /**
     * Show success state with JSON data
     */
    private void showSuccessState(String jsonData) {
        hideAllResponseViews();

        if (jsonLabelTextView != null) {
            jsonLabelTextView.setVisibility(View.VISIBLE);
        }

        if (jsonContentTextView != null) {
            jsonContentTextView.setText(jsonData);
            jsonContentTextView.setVisibility(View.VISIBLE);
        }

        if (jsonScrollView != null) {
            jsonScrollView.setVisibility(View.VISIBLE);
        }

        if (fetchDataButton != null) {
            fetchDataButton.setEnabled(true);
        }
    }

    /**
     * Show error state with error message
     */
    private void showErrorState(String errorMessage) {
        hideAllResponseViews();

        if (errorTextView != null) {
            errorTextView.setText("Error: " + errorMessage);
            errorTextView.setVisibility(View.VISIBLE);
        }

        if (fetchDataButton != null) {
            fetchDataButton.setEnabled(true);
        }
    }

    /**
     * Hide all response-related views
     */
    private void hideAllResponseViews() {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.GONE);
        }
        if (errorTextView != null) {
            errorTextView.setVisibility(View.GONE);
        }
        if (jsonLabelTextView != null) {
            jsonLabelTextView.setVisibility(View.GONE);
        }
        if (jsonContentTextView != null) {
            jsonContentTextView.setVisibility(View.GONE);
        }
        if (jsonScrollView != null) {
            jsonScrollView.setVisibility(View.GONE);
        }
    }

    /**
     * Static factory method to create a new instance of this fragment
     * @return A new instance of JavaWeatherFragment
     */
    public static JavaWeatherFragment newInstance() {
        JavaWeatherFragment fragment = new JavaWeatherFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
}
