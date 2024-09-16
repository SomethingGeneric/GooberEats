package cloud.goober.goobereats;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

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

        findViewById(R.id.submitCalButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCalorieData();
                new FetchDataTask().execute();
            }
        });

    }

    public static String generateUserID() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes).substring(0, 64);
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
                URL url = new URL("http://77.90.6.154:5000/kcount?id=" + getUserID()); // Append userId as query parameter for GET
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
                    storedCalories = content.toString();
                } else {
                    // Handle non-200 HTTP response codes
                    throw new IOException("HTTP Error: " + responseCode);
                }
            } catch (IOException e) {
                exception = e; // Capture the exception to handle it later
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
                calSpentView.setText("Today's calories: " + result);
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

        if (!userInput.isEmpty()) {
            // Call AsyncTask to perform the POST request
            new PostCalorieTask(userInput).execute();
            submitNewCal.setText("");
        } else {
            Toast.makeText(MainActivity.this, "Input cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }

    // AsyncTask to perform POST request in the background
    private class PostCalorieTask extends AsyncTask<Void, Void, String> {
        private String userInput;
        private Exception exception;

        public PostCalorieTask(String userInput) {
            this.userInput = userInput;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Create the URL and connection
                URL url = new URL("http://77.90.6.154:5000/kcount");
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

                // Write the JSON data to the request body
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Get response code
                int responseCode = connection.getResponseCode();
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
                // Log.e("PostCalorieTask", "Error occurred", exception);
            } else if (result != null) {
                // Show success message
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
