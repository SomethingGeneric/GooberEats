/**
 * Example integration code for Android app to use calorie estimation API
 * This demonstrates how the Android app could integrate with the new /api/estimate endpoint
 */

// AsyncTask example to call calorie estimation API
private class EstimateCaloriesTask extends AsyncTask<Void, Void, Integer> {
    private String foodDescription;
    private Exception exception;

    public EstimateCaloriesTask(String foodDescription) {
        this.foodDescription = foodDescription;
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            URL url = new URL("http://eats.goober.cloud:5000/api/estimate");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Create JSON payload
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("description", foodDescription);

            // Write the JSON data to the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Check response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder content = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                // Parse JSON response
                JSONObject response = new JSONObject(content.toString());
                return response.getInt("estimated_calories");
            } else {
                throw new IOException("HTTP Error: " + responseCode);
            }
        } catch (IOException | JSONException e) {
            exception = e;
            return null;
        }
    }

    @Override
    protected void onPostExecute(Integer estimatedCalories) {
        if (exception != null) {
            Toast.makeText(MainActivity.this, "Estimation failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        } else if (estimatedCalories != null) {
            // Auto-fill the calories field with the estimate
            submitNewCal.setText(String.valueOf(estimatedCalories));
            Toast.makeText(MainActivity.this, "Estimated: " + estimatedCalories + " calories", Toast.LENGTH_SHORT).show();
        }
    }
}

// Usage example:
// new EstimateCaloriesTask("apple").execute();