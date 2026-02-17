package cloud.goober.goobereats;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView calSpentView;
    private EditText submitNewCal;
    private EditText submitItemDesc;
    private String currentUserId;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 64;
    private static final String PREFS_NAME = "MyAppPrefs";
    private static final String USER_ID_KEY = "userid";

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

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        calSpentView = findViewById(R.id.calSpentView);

        // Start the AsyncTask to fetch data from the server
        refreshCalorieData();

        submitNewCal = findViewById(R.id.submitNewCal);
        submitItemDesc = findViewById(R.id.newItemDesc);

        Button submitCalButton = findViewById(R.id.submitCalButton);
        submitCalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                persistCurrentUserId();
                submitCalorieData();
                refreshCalorieData();
            }
        });
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
                CharSequence rawInput = userIdField.getText();
                String newId = rawInput != null ? rawInput.toString().trim() : "";
                if (newId.isEmpty()) {
                    userIdField.setError(getString(R.string.dialog_user_id_hint));
                    return;
                }
                storeUserId(newId);
                Toast.makeText(MainActivity.this, "User ID saved", Toast.LENGTH_SHORT).show();
                refreshCalorieData();
                dialog.dismiss();
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

    // AsyncTask to perform network operations on a background thread
    private class FetchDataTask extends AsyncTask<Void, Void, String> {
        private Exception exception;

        @Override
        protected String doInBackground(Void... voids) {
            String storedCalories = null;
            try {
                // Create the URL and connection
                URL url = new URL("https://eats.mattcompton.dev/api/datafor?id=" + getUserID()); // Append userId as query parameter for GET
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

                    // Parse the raw string into a JSONArray and format the display text
                    JSONArray dataArray = new JSONArray(rawjson);
                    storedCalories = formatCalorieEntries(dataArray);

                } else {
                    // Handle non-200 HTTP response codes
                    throw new IOException("HTTP Error: " + responseCode);
                }
            } catch (IOException e) {
                exception = e; // Capture the exception to handle it later
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return storedCalories;
        }

        @Override
        protected void onPostExecute(String result) {
            if (exception != null) {
                // If there was an error, display it in a Toast message
                Toast.makeText(MainActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            } else if (result != null) {
                // If successful, update the TextView with the fetched data
                calSpentView.setText(result);
            } else {
                // Handle case where result is null (no data received)
                Toast.makeText(MainActivity.this, "No data received from server", Toast.LENGTH_SHORT).show();
            }
        }

        // Method to get the userID
        private String getUserID() {
            return getOrCreateUserId();
        }
    }

    private String formatCalorieEntries(JSONArray dataArray) throws JSONException {
        Map<LocalDate, List<CalorieEntry>> entriesByDay = new LinkedHashMap<>();

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.getJSONObject(i);
            String rawDate = item.optString("datestamp", "");
            String recordedAt = item.optString("recorded_at", rawDate);
            int calorieCount = item.optInt("calorie_count");
            String description = item.optString("description", "");

            ParsedTimestamp parsedTimestamp = parseDatestamp(
                    recordedAt != null && !recordedAt.trim().isEmpty() ? recordedAt : rawDate);

            CalorieEntry entry = new CalorieEntry(parsedTimestamp, calorieCount, description);
            entriesByDay
                    .computeIfAbsent(parsedTimestamp.date, unused -> new ArrayList<>())
                    .add(entry);
        }

        if (entriesByDay.isEmpty()) {
            return "";
        }

        DateTimeFormatter dayFormatter =
                DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault());
        DateTimeFormatter timeFormatter =
                DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());

        StringBuilder output = new StringBuilder();

        for (Map.Entry<LocalDate, List<CalorieEntry>> dailyEntry : entriesByDay.entrySet()) {
            if (output.length() > 0) {
                output.append("\n");
            }
            int dailyTotal = 0;
            for (CalorieEntry entry : dailyEntry.getValue()) {
                dailyTotal += entry.calories;
            }

            output.append(dayFormatter.format(dailyEntry.getKey()))
                    .append(" — ")
                    .append(dailyTotal)
                    .append(" kcal")
                    .append("\n");

            for (CalorieEntry entry : dailyEntry.getValue()) {
                output.append(" • ");
                if (entry.timestamp.hasTime) {
                    output.append(timeFormatter.format(entry.timestamp.dateTime));
                } else {
                    output.append(getString(R.string.time_not_available));
                }
                output.append(" — ")
                        .append(entry.calories)
                        .append(" kcal");
                if (entry.description != null && !entry.description.trim().isEmpty()) {
                    output.append(" — ").append(entry.description);
                }
                output.append("\n");
            }
        }

        return output.toString().trim();
    }

    private ParsedTimestamp parseDatestamp(String datestamp) {
        String value = datestamp != null ? datestamp.trim() : "";
        if (value.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            return new ParsedTimestamp(now.toLocalDate(), now, false);
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATS) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value, formatter);
                return new ParsedTimestamp(dateTime.toLocalDate(), dateTime, true);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            Instant instant = Instant.parse(value);
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return new ParsedTimestamp(dateTime.toLocalDate(), dateTime, true);
        } catch (DateTimeParseException ignored) {
        }

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(value, formatter);
                return new ParsedTimestamp(date, date.atStartOfDay(), false);
            } catch (DateTimeParseException ignored) {
            }
        }

        if (value.matches("\\d{8}")) {
            try {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
                return new ParsedTimestamp(date, date.atStartOfDay(), false);
            } catch (DateTimeParseException ignored) {
            }
        }

        LocalDateTime now = LocalDateTime.now();
        return new ParsedTimestamp(now.toLocalDate(), now, false);
    }

    private static final DateTimeFormatter[] DATE_TIME_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    };

    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    private static class ParsedTimestamp {
        private final LocalDate date;
        private final LocalDateTime dateTime;
        private final boolean hasTime;

        private ParsedTimestamp(LocalDate date, LocalDateTime dateTime, boolean hasTime) {
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


    // Method to get user input and make a POST request
    private void submitCalorieData() {
        persistCurrentUserId();
        String userInput = submitNewCal.getText().toString().trim();
        String itemDesc = submitItemDesc.getText().toString().trim();

        if (!userInput.isEmpty()) {
            // Call AsyncTask to perform the POST request
            new PostCalorieTask(userInput, itemDesc).execute();
            submitNewCal.setText("");
            submitItemDesc.setText("");
        } else {
            Toast.makeText(MainActivity.this, "Input cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }

    // AsyncTask to perform POST request in the background
    private class PostCalorieTask extends AsyncTask<Void, Void, String> {
        private String userInput;
        private String itemDesc;
        private Exception exception;

        public PostCalorieTask(String userInput, String itemDesc) {
            this.userInput = userInput;
            this.itemDesc = itemDesc;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Create the URL and connection
                URL url = new URL("https://eats.mattcompton.dev/api/kcount");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                float newcals;

                try {
                    newcals = Float.parseFloat(userInput);
                } catch (NumberFormatException e) {
                    return "That's not a fucking number";
                }

                // Create JSON payload
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("kcal", newcals);
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
            }
        }
    }
}
