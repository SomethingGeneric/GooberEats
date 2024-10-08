package cloud.goober.goobereats;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Base64;

public class MainActivity extends AppCompatActivity {

    private TextView calSpentView;
    private EditText submitNewCal;
    private EditText submitItemDesc;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 64;

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

        // Initialize the TextView
        calSpentView = findViewById(R.id.calSpentView);

        // Start the AsyncTask to fetch data from the server
        new FetchDataTask().execute(); // TODO: annotated as deprecated?

        submitNewCal = findViewById(R.id.submitNewCal);
        submitItemDesc = findViewById(R.id.newItemDesc);

        findViewById(R.id.submitCalButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCalorieData();
                new FetchDataTask().execute();
            }
        });

        findViewById(R.id.resetButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                emptyUserID();
                Toast.makeText(MainActivity.this, "UID nuked", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public static String generateUserID() {
        StringBuilder sb = new StringBuilder(STRING_LENGTH);
        for (int i = 0; i < STRING_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }

    public void emptyUserID() {
        SharedPreferences sharedPref = this.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove("userid");
        editor.apply();
        editor.commit();
    }

    public String truegetUserID() {
        SharedPreferences sharedPref = this.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String result = sharedPref.getString("userid", null);
        if (result == null) {
            String newID = generateUserID();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("userid", newID);
            editor.apply();
            editor.commit();
            return newID;
        } else {
            return result;
        }
    }

    // AsyncTask to perform network operations on a background thread
    private class FetchDataTask extends AsyncTask<Void, Void, String> {
        private Exception exception;

        @Override
        protected String doInBackground(Void... voids) {
            String storedCalories = null;
            try {
                // Create the URL and connection
                URL url = new URL("http://eats.goober.cloud:5000/api/datafor?id=" + getUserID()); // Append userId as query parameter for GET
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
                    StringBuilder outputStr = new StringBuilder();

                    // Iterate over each JSONObject in the array
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject item = dataArray.getJSONObject(i);

                        // Retrieve each field
                        String datestamp = item.getString("datestamp");
                        int calorieCount = item.getInt("calorie_count");
                        String description = item.getString("description");

                        // Concatenate the fields to the output string
                        outputStr.append(datestamp)
                                .append(", ")
                                .append(calorieCount)
                                .append(", ")
                                .append(description)
                                .append("\n");
                    }

                    // Output the result
                    storedCalories = outputStr.toString();

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
            return truegetUserID();
        }
    }


    // Method to get user input and make a POST request
    private void submitCalorieData() {
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
                URL url = new URL("http://eats.goober.cloud:5000/api/kcount");
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
                jsonBody.put("id", truegetUserID());
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
