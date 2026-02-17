package cloud.goober.goobereats;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView calorieRecyclerView;
    private CalorieAdapter calorieAdapter;
    private final List<DisplayItem> displayItems = new ArrayList<>();
    private final List<QuickEntryOption> quickEntryOptions = new ArrayList<>();
    private String currentUserId;
    private String currentApiBase;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 64;
    private static final String PREFS_NAME = "MyAppPrefs";
    private static final String USER_ID_KEY = "userid";
    private static final String API_BASE_KEY = "api_base_url";
    private static final String DEFAULT_API_BASE = "https://eats.mattcompton.dev";
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ENTRY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Set up insets for the view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        currentUserId = getOrCreateUserId();
        currentApiBase = getApiBaseUrl();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        calorieRecyclerView = findViewById(R.id.calorieRecyclerView);
        calorieRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        calorieAdapter = new CalorieAdapter(displayItems);
        calorieRecyclerView.setAdapter(calorieAdapter);

        ExtendedFloatingActionButton aiEntryFab = findViewById(R.id.aiEntryFab);
        aiEntryFab.setOnClickListener(v -> {
            persistCurrentUserId();
            showAiEntryDialog();
        });

        ExtendedFloatingActionButton quickEntryFab = findViewById(R.id.quickEntryFab);
        quickEntryFab.setOnClickListener(v -> {
            persistCurrentUserId();
            showQuickEntryDialog();
        });

        FloatingActionButton addEntryFab = findViewById(R.id.addEntryFab);
        addEntryFab.setOnClickListener(v -> {
            persistCurrentUserId();
            showNewEntryDialog();
        });

        // Start the AsyncTask to fetch data from the server
        refreshCalorieData();
    }

    private void refreshCalorieData() {
        new FetchDataTask().execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        persistCurrentUserId();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_user_id) {
            showUserIdDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showUserIdDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_id, null);
        TextInputEditText userIdField = dialogView.findViewById(R.id.dialogUserIdInput);
        userIdField.setText(getOrCreateUserId());
        TextInputLayout apiBaseLayout = dialogView.findViewById(R.id.dialogApiBaseLayout);
        TextInputEditText apiBaseInput = dialogView.findViewById(R.id.dialogApiBaseInput);
        if (apiBaseInput != null) {
            apiBaseInput.setText(getApiBaseUrl());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_user_id_title)
                .setView(dialogView)
                .setPositiveButton(R.string.dialog_user_id_save, null)
                .setNegativeButton(R.string.dialog_user_id_cancel, (d, which) -> {
                })
                .create();

        Button randomButton = dialogView.findViewById(R.id.dialogRandomButton);
        randomButton.setOnClickListener(v -> userIdField.setText(generateUserID()));

        dialog.setOnShowListener(dlg -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                if (apiBaseLayout != null) {
                    apiBaseLayout.setError(null);
                }
                CharSequence rawInput = userIdField.getText();
                String newId = rawInput != null ? rawInput.toString().trim() : "";
                if (newId.isEmpty()) {
                    userIdField.setError(getString(R.string.dialog_user_id_hint));
                    return;
                }
                String newBase = apiBaseInput != null && apiBaseInput.getText() != null
                        ? apiBaseInput.getText().toString().trim()
                        : "";
                if (TextUtils.isEmpty(newBase)) {
                    if (apiBaseLayout != null) {
                        apiBaseLayout.setError(getString(R.string.dialog_api_base_hint));
                    }
                    return;
                }
                String normalizedBase = normalizeBaseUrl(newBase);
                if (TextUtils.isEmpty(normalizedBase)) {
                    if (apiBaseLayout != null) {
                        apiBaseLayout.setError(getString(R.string.dialog_api_base_hint));
                    }
                    return;
                }
                storeUserId(newId);
                storeApiBase(normalizedBase);
                Toast.makeText(MainActivity.this, "User ID saved", Toast.LENGTH_SHORT).show();
                refreshCalorieData();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showAiEntryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_entry, null);
        TextInputLayout descriptionLayout = dialogView.findViewById(R.id.dialogAiDescriptionLayout);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.dialogAiDescriptionInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_ai_entry_title)
                .setView(dialogView)
                .setPositiveButton(R.string.dialog_ai_entry_submit, null)
                .setNegativeButton(R.string.dialog_ai_entry_cancel, (d, which) -> {
                })
                .create();

        dialog.setOnShowListener(dlg -> {
            Button submitButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submitButton.setOnClickListener(v -> {
                if (descriptionLayout != null) {
                    descriptionLayout.setError(null);
                }

                String description = "";
                if (descriptionInput != null && descriptionInput.getText() != null) {
                    description = descriptionInput.getText().toString().trim();
                }

                if (TextUtils.isEmpty(description)) {
                    if (descriptionLayout != null) {
                        descriptionLayout.setError(getString(R.string.dialog_ai_entry_hint));
                    }
                    return;
                }

                dialog.dismiss();
                new EstimateCaloriesTask(description).execute();
            });
        });

        dialog.show();
    }

    private void showQuickEntryDialog() {
        if (quickEntryOptions.isEmpty()) {
            Toast.makeText(this, R.string.quick_entry_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] items = new CharSequence[quickEntryOptions.size()];
        for (int i = 0; i < quickEntryOptions.size(); i++) {
            QuickEntryOption option = quickEntryOptions.get(i);
            items[i] = getString(R.string.quick_entry_item_format, option.description, option.calories);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.quick_entry_dialog_title)
                .setItems(items, (dialog, which) -> {
                    if (which >= 0 && which < quickEntryOptions.size()) {
                        QuickEntryOption option = quickEntryOptions.get(which);
                        submitCalorieData(option.calories, option.description);
                    }
                })
                .setNegativeButton(R.string.dialog_new_entry_cancel, null)
                .show();
    }

    private void showNewEntryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_entry, null);
        TextInputLayout caloriesLayout = dialogView.findViewById(R.id.dialogCaloriesLayout);
        TextInputEditText caloriesInput = dialogView.findViewById(R.id.dialogCaloriesInput);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.dialogDescriptionInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_new_entry_title)
                .setView(dialogView)
                .setPositiveButton(R.string.dialog_new_entry_save, null)
                .setNegativeButton(R.string.dialog_new_entry_cancel, (d, which) -> {
                })
                .create();

        dialog.setOnShowListener(dlg -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                if (caloriesLayout != null) {
                    caloriesLayout.setError(null);
                }

                String caloriesText = "";
                if (caloriesInput != null && caloriesInput.getText() != null) {
                    caloriesText = caloriesInput.getText().toString().trim();
                }

                if (TextUtils.isEmpty(caloriesText)) {
                    if (caloriesLayout != null) {
                        caloriesLayout.setError(getString(R.string.error_invalid_calories));
                    }
                    return;
                }

                float caloriesValue;
                try {
                    caloriesValue = Float.parseFloat(caloriesText);
                } catch (NumberFormatException e) {
                    if (caloriesLayout != null) {
                        caloriesLayout.setError(getString(R.string.error_invalid_calories));
                    }
                    return;
                }

                if (caloriesValue <= 0) {
                    if (caloriesLayout != null) {
                        caloriesLayout.setError(getString(R.string.error_invalid_calories));
                    }
                    return;
                }

                String description = "";
                if (descriptionInput != null && descriptionInput.getText() != null) {
                    description = descriptionInput.getText().toString().trim();
                }

                dialog.dismiss();
                submitCalorieData(caloriesValue, description);
            });
        });

        dialog.show();
    }

    public static String generateUserID() {
        StringBuilder sb = new StringBuilder(STRING_LENGTH);
        for (int i = 0; i < STRING_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }

    private void persistCurrentUserId() {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            currentUserId = generateUserID();
        }
        storeUserId(currentUserId);
    }

    private void storeUserId(String userId) {
        SharedPreferences sharedPref = this.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sharedPref.edit().putString(USER_ID_KEY, userId).apply();
        currentUserId = userId;
    }

    private void storeApiBase(String apiBase) {
        SharedPreferences sharedPref = this.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sharedPref.edit().putString(API_BASE_KEY, apiBase).apply();
        currentApiBase = apiBase;
    }

    private String getStoredUserId() {
        SharedPreferences sharedPref = this.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPref.getString(USER_ID_KEY, null);
    }

    private String getOrCreateUserId() {
        if (currentUserId != null && !currentUserId.trim().isEmpty()) {
            return currentUserId;
        }
        String stored = getStoredUserId();
        if (stored == null || stored.trim().isEmpty()) {
            String newId = generateUserID();
            storeUserId(newId);
            return newId;
        }
        currentUserId = stored;
        return currentUserId;
    }

    private String getStoredApiBase() {
        SharedPreferences sharedPref = this.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPref.getString(API_BASE_KEY, null);
    }

    private String getApiBaseUrl() {
        if (currentApiBase != null && !currentApiBase.trim().isEmpty()) {
            return currentApiBase;
        }
        String stored = getStoredApiBase();
        if (stored == null || stored.trim().isEmpty()) {
            storeApiBase(DEFAULT_API_BASE);
            return DEFAULT_API_BASE;
        }
        currentApiBase = stored;
        return currentApiBase;
    }

    private String normalizeBaseUrl(String base) {
        if (base == null) {
            return "";
        }
        String trimmed = base.trim();
        while (trimmed.endsWith("/") && trimmed.length() > 1) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String buildApiUrl(String path) {
        String base = getApiBaseUrl();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    // AsyncTask to perform network operations on a background thread
    private class FetchDataTask extends AsyncTask<Void, Void, List<CalorieEntry>> {
        private Exception exception;

        @Override
        protected List<CalorieEntry> doInBackground(Void... voids) {
            List<CalorieEntry> entries = Collections.emptyList();
            try {
                // Create the URL and connection
                URL url = new URL(buildApiUrl("/api/datafor") + "?id=" + getUserID()); // Append userId as query parameter for GET
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");

                // Check if response is HTTP OK (200)
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the input stream
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder content = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    String rawjson = content.toString();

                    // Parse the raw string into a JSONArray
                    JSONArray dataArray = new JSONArray(rawjson);
                    entries = parseCalorieEntries(dataArray);

                } else {
                    // Handle non-200 HTTP response codes
                    throw new IOException("HTTP Error: " + responseCode);
                }
            } catch (IOException e) {
                exception = e; // Capture the exception to handle it later
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return entries;
        }

        @Override
        protected void onPostExecute(List<CalorieEntry> result) {
            if (exception != null) {
                // If there was an error, display it in a Toast message
                Toast.makeText(MainActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            } else if (result != null) {
                updateCalorieDisplay(result);
            } else {
                Toast.makeText(MainActivity.this, "No data received from server", Toast.LENGTH_SHORT).show();
            }
        }

        // Method to get the userID
        private String getUserID() {
            return getOrCreateUserId();
        }
    }

    private List<CalorieEntry> parseCalorieEntries(JSONArray dataArray) throws JSONException {
        List<CalorieEntry> entries = new ArrayList<>();

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.getJSONObject(i);
            String rawDate = item.optString("datestamp", "");
            String recordedAt = item.optString("recorded_at", rawDate);
            int calorieCount = item.optInt("calorie_count");
            String description = item.optString("description", "");

            String timestampSource = !TextUtils.isEmpty(recordedAt) ? recordedAt : rawDate;
            ParsedTimestamp parsedTimestamp = parseDatestamp(timestampSource);
            entries.add(new CalorieEntry(parsedTimestamp, calorieCount, description));
        }

        return entries;
    }

    private void updateCalorieDisplay(List<CalorieEntry> entries) {
        displayItems.clear();
        quickEntryOptions.clear();

        if (entries == null || entries.isEmpty()) {
            calorieAdapter.notifyDataSetChanged();
            return;
        }

        Map<LocalDate, List<CalorieEntry>> entriesByDay = new LinkedHashMap<>();
        LinkedHashMap<String, QuickEntryOption> quickMap = new LinkedHashMap<>();
        for (CalorieEntry entry : entries) {
            entriesByDay
                    .computeIfAbsent(entry.timestamp.date, unused -> new ArrayList<>())
                    .add(entry);
            if (!TextUtils.isEmpty(entry.description)) {
                quickMap.putIfAbsent(
                        entry.description,
                        new QuickEntryOption(entry.description, entry.calories));
            }
        }

        DateTimeFormatter dayFormatter =
                DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault());
        DateTimeFormatter timeFormatter =
                DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());

        for (Map.Entry<LocalDate, List<CalorieEntry>> dayEntry : entriesByDay.entrySet()) {
            int dailyTotal = 0;
            for (CalorieEntry entry : dayEntry.getValue()) {
                dailyTotal += entry.calories;
            }
            displayItems.add(new DayHeaderItem(dayFormatter.format(dayEntry.getKey()), dailyTotal));

            for (CalorieEntry entry : dayEntry.getValue()) {
                String timeLabel = entry.timestamp.hasTime
                        ? timeFormatter.format(entry.timestamp.dateTime)
                        : getString(R.string.time_not_available);
                displayItems.add(new CalorieEntryItem(timeLabel, entry.calories, entry.description));
            }
        }

        calorieAdapter.notifyDataSetChanged();
        int limit = 15;
        int count = 0;
        for (QuickEntryOption option : quickMap.values()) {
            quickEntryOptions.add(option);
            count++;
            if (count >= limit) {
                break;
            }
        }
    }

    private ParsedTimestamp parseDatestamp(String datestamp) {
        String value = datestamp != null ? datestamp.trim() : "";
        ZoneId deviceZone = ZoneId.systemDefault();
        if (value.isEmpty()) {
            ZonedDateTime now = ZonedDateTime.now(deviceZone);
            return new ParsedTimestamp(now.toLocalDate(), now, false);
        }

        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(value);
            ZonedDateTime zonedDateTime = offsetDateTime.atZoneSameInstant(deviceZone);
            return new ParsedTimestamp(zonedDateTime.toLocalDate(), zonedDateTime, true);
        } catch (DateTimeParseException ignored) {
        }

        try {
            Instant instant = Instant.parse(value);
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, deviceZone);
            return new ParsedTimestamp(zonedDateTime.toLocalDate(), zonedDateTime, true);
        } catch (DateTimeParseException ignored) {
        }

        for (DateTimeFormatter formatter : LOCAL_DATE_TIME_FORMATS) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value, formatter);
                return new ParsedTimestamp(dateTime.toLocalDate(), dateTime.atZone(deviceZone), true);
            } catch (DateTimeParseException ignored) {
            }
        }

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(value, formatter);
                return new ParsedTimestamp(date, date.atStartOfDay(deviceZone), false);
            } catch (DateTimeParseException ignored) {
            }
        }

        if (value.matches("\\d{8}")) {
            try {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
                return new ParsedTimestamp(date, date.atStartOfDay(deviceZone), false);
            } catch (DateTimeParseException ignored) {
            }
        }

        ZonedDateTime now = ZonedDateTime.now(deviceZone);
        return new ParsedTimestamp(now.toLocalDate(), now, false);
    }

    private static final DateTimeFormatter[] LOCAL_DATE_TIME_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    };

    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    private static class ParsedTimestamp {
        private final LocalDate date;
        private final ZonedDateTime dateTime;
        private final boolean hasTime;

        private ParsedTimestamp(LocalDate date, ZonedDateTime dateTime, boolean hasTime) {
            this.date = date;
            this.dateTime = dateTime;
            this.hasTime = hasTime;
        }
    }

    private static class CalorieEntry {
        private final ParsedTimestamp timestamp;
        private final int calories;
        private final String description;

        private CalorieEntry(ParsedTimestamp timestamp, int calories, String description) {
            this.timestamp = timestamp;
            this.calories = calories;
            this.description = description;
        }
    }

    private interface DisplayItem {
        int getViewType();
    }

    private static class DayHeaderItem implements DisplayItem {
        private final String dayLabel;
        private final int totalCalories;

        private DayHeaderItem(String dayLabel, int totalCalories) {
            this.dayLabel = dayLabel;
            this.totalCalories = totalCalories;
        }

        @Override
        public int getViewType() {
            return VIEW_TYPE_HEADER;
        }
    }

    private static class CalorieEntryItem implements DisplayItem {
        private final String timeLabel;
        private final int calories;
        private final String description;

        private CalorieEntryItem(String timeLabel, int calories, String description) {
            this.timeLabel = timeLabel;
            this.calories = calories;
            this.description = description;
        }

        @Override
        public int getViewType() {
            return VIEW_TYPE_ENTRY;
        }
    }

    private static class CalorieAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<DisplayItem> items;

        private CalorieAdapter(List<DisplayItem> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).getViewType();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_HEADER) {
                View view = inflater.inflate(R.layout.item_day_header, parent, false);
                return new DayHeaderViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_calorie_entry, parent, false);
                return new EntryViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            DisplayItem item = items.get(position);
            if (holder instanceof DayHeaderViewHolder && item instanceof DayHeaderItem) {
                ((DayHeaderViewHolder) holder).bind((DayHeaderItem) item);
            } else if (holder instanceof EntryViewHolder && item instanceof CalorieEntryItem) {
                ((EntryViewHolder) holder).bind((CalorieEntryItem) item);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private static class DayHeaderViewHolder extends RecyclerView.ViewHolder {
            private final TextView titleView;

            private DayHeaderViewHolder(View itemView) {
                super(itemView);
                this.titleView = (TextView) itemView;
            }

            private void bind(DayHeaderItem item) {
                titleView.setText(titleView.getContext().getString(
                        R.string.day_total_format,
                        item.dayLabel,
                        item.totalCalories));
            }
        }

        private static class EntryViewHolder extends RecyclerView.ViewHolder {
            private final TextView timeView;
            private final TextView caloriesView;
            private final TextView descriptionView;

            private EntryViewHolder(View itemView) {
                super(itemView);
                timeView = itemView.findViewById(R.id.entryTime);
                caloriesView = itemView.findViewById(R.id.entryCalories);
                descriptionView = itemView.findViewById(R.id.entryDescription);
            }

            private void bind(CalorieEntryItem item) {
                timeView.setText(item.timeLabel);
                caloriesView.setText(caloriesView.getContext().getString(
                        R.string.entry_calories_format,
                        item.calories));
                if (TextUtils.isEmpty(item.description)) {
                    descriptionView.setVisibility(View.GONE);
                } else {
                    descriptionView.setVisibility(View.VISIBLE);
                    descriptionView.setText(item.description);
                }
            }
        }
    }


    private static class QuickEntryOption {
        private final String description;
        private final int calories;

        private QuickEntryOption(String description, int calories) {
            this.description = description;
            this.calories = calories;
        }
    }

    private class EstimateCaloriesTask extends AsyncTask<Void, Void, Integer> {
        private final String description;
        private Exception exception;
        private String errorMessage;

        private EstimateCaloriesTask(String description) {
            this.description = description;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                URL url = new URL(buildApiUrl("/api/estimate"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("description", description);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                StringBuilder content = new StringBuilder();

                InputStream responseStream = responseCode == HttpURLConnection.HTTP_OK
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                if (responseStream != null) {
                    try (BufferedReader reader =
                                 new BufferedReader(new InputStreamReader(responseStream))) {
                        String inputLine;
                        while ((inputLine = reader.readLine()) != null) {
                            content.append(inputLine);
                        }
                    }
                }
                connection.disconnect();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    if (content.length() > 0) {
                        try {
                            JSONObject errorJson = new JSONObject(content.toString());
                            errorMessage = errorJson.optString("error", null);
                        } catch (JSONException ignore) {
                            errorMessage = content.toString();
                        }
                    }
                    throw new IOException("HTTP Error: " + responseCode);
                }

                JSONObject result = new JSONObject(content.toString());
                return result.optInt("estimated_calories", -1);
            } catch (IOException | JSONException e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (exception != null) {
                if (!TextUtils.isEmpty(errorMessage)) {
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.error_ai_with_reason, exception.getMessage()),
                        Toast.LENGTH_SHORT).show();
                Log.e("EstimateCaloriesTask", "Error estimating calories", exception);
                return;
            }

            if (result == null || result <= 0) {
                Toast.makeText(
                        MainActivity.this,
                        R.string.error_ai_generic,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(MainActivity.this, getString(R.string.toast_ai_result, result), Toast.LENGTH_SHORT).show();
            submitCalorieData(result, description);
        }
    }

    // Method to get user input and make a POST request
    private void submitCalorieData(float calories, String itemDesc) {
        persistCurrentUserId();
        new PostCalorieTask(calories, itemDesc).execute();
    }

    // AsyncTask to perform POST request in the background
    private class PostCalorieTask extends AsyncTask<Void, Void, String> {
        private final float calorieValue;
        private final String itemDesc;
        private Exception exception;

        public PostCalorieTask(float calorieValue, String itemDesc) {
            this.calorieValue = calorieValue;
            this.itemDesc = itemDesc;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Create the URL and connection
                URL url = new URL(buildApiUrl("/api/kcount"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                // Create JSON payload
                int normalizedCalories = Math.round(calorieValue);

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("kcal", normalizedCalories);
                jsonBody.put("id", getOrCreateUserId());
                jsonBody.put("desc", itemDesc);

                // Write the JSON data to the request body
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Get response code
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    return "Success!";
                } else {
                    return "Error: " + responseCode;
                }

            } catch (IOException | JSONException e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (exception != null) {
                Toast.makeText(MainActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PostCalorieTask", "GE Error occurred", exception);
            } else if (result != null) {
                // Show success message
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                refreshCalorieData();
            }
        }
    }
}
