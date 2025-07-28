package ai.voitta.jetbrains.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Utils {

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public static String getGreeting(User u) {
        return "Hello " + u.getName();
    }

    @SneakyThrows
    public static String getWeather(User u) {
        if (u.getZip() == null || u.getZip().isEmpty()) {
            return "Zip code not available for weather lookup";
        }

        try {
            String geocodeUrl = String.format("https://geocode.maps.co/search?q=%s&api_key=6886ae85e227b908343495kbuebdaf6",
                u.getZip());
            
            HttpRequest geocodeRequest = HttpRequest.newBuilder()
                .uri(URI.create(geocodeUrl))
                .header("User-Agent", "SampleJavaProject/1.0")
                .build();

            HttpResponse<String> geocodeResponse = httpClient.send(geocodeRequest, 
                HttpResponse.BodyHandlers.ofString());

            if (geocodeResponse.statusCode() != 200) {
                return "Unable to geocode zip code";
            }

            JsonNode geocodeData = objectMapper.readTree(geocodeResponse.body());
            if (!geocodeData.isArray() || geocodeData.size() == 0) {
                return "No location found for zip code";
            }

            JsonNode location = geocodeData.get(0);
            double latitude = location.get("lat").asDouble();
            double longitude = location.get("lon").asDouble();

            String pointUrl = String.format("https://api.weather.gov/points/%.4f,%.4f", 
                latitude, longitude);
            
            HttpRequest pointRequest = HttpRequest.newBuilder()
                .uri(URI.create(pointUrl))
                .header("User-Agent", "SampleJavaProject/1.0")
                .build();

            HttpResponse<String> pointResponse = httpClient.send(pointRequest, 
                HttpResponse.BodyHandlers.ofString());

            if (pointResponse.statusCode() != 200) {
                return "Unable to get weather data for location";
            }

            JsonNode pointData = objectMapper.readTree(pointResponse.body());
            String forecastUrl = pointData.at("/properties/forecast").asText();

            if (forecastUrl.isEmpty()) {
                return "Forecast URL not available for location";
            }

            HttpRequest forecastRequest = HttpRequest.newBuilder()
                .uri(URI.create(forecastUrl))
                .header("User-Agent", "SampleJavaProject/1.0")
                .build();

            HttpResponse<String> forecastResponse = httpClient.send(forecastRequest, 
                HttpResponse.BodyHandlers.ofString());

            if (forecastResponse.statusCode() != 200) {
                return "Unable to get forecast data";
            }

            JsonNode forecastData = objectMapper.readTree(forecastResponse.body());
            JsonNode periods = forecastData.at("/properties/periods");

            if (periods.isArray() && periods.size() > 0) {
                JsonNode currentPeriod = periods.get(0);
                String name = currentPeriod.get("name").asText();
                String detailedForecast = currentPeriod.get("detailedForecast").asText();
                return String.format("%s: %s", name, detailedForecast);
            }

            return "No forecast data available";

        } catch (IOException | InterruptedException e) {
            return "Error fetching weather data: " + e.getMessage();
        }
    }
}
