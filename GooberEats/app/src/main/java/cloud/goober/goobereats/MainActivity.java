package cloud.goober.goobereats;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private TextView calSpentView;

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
        new FetchDataTask().execute();
    }

    // AsyncTask to perform network operations on a background thread
    private class FetchDataTask extends AsyncTask<Void, Void, String> {
        private Exception exception;

        @Override
        protected String doInBackground(Void... voids) {
            String storedCalories = null;
            try {
                // Create the URL and connection
                URL url = new URL("http://77.90.6.154:5000/kcount");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

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
                calSpentView.setText(result);
            } else {
                // Handle case where result is null (no data received)
                Toast.makeText(MainActivity.this, "No data received from server", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
