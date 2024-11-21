package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherApp {

    private static final String BASE_URL = "https://api.weather.yandex.ru/v2/forecast";
    private static final String API_KEY = "91fc8880-11a5-4050-8493-08e57b5cbcdf";

    public static void main(String[] args) {
        try {
            // Параметры для запроса
            double lat = 55.76; // Москва
            double lon = 37.62; // Москва
            int limit = 3; // Прогноз на 3 дня

            // Получаем данные о погоде
            JSONObject weatherData = fetchWeatherData(lat, lon, limit);

            // Проверяем, что данные получены
            if (weatherData == null) {
                System.err.println("Не удалось получить данные о погоде. Проверьте параметры или API.");
                return; // Завершаем выполнение
            }

            // Выводим полные данные о погоде
            System.out.println("Полные данные о погоде:");
            System.out.println(weatherData.toString(4)); // Красивый вывод JSON с отступами

            // Выводим текущую температуру
            int currentTemp = weatherData.getJSONObject("fact").getInt("temp");
            System.out.println("Текущая температура: " + currentTemp + "°C");

            // Получаем среднюю температуру на заданный период
            double averageTemp = calculateAverageTemperature(weatherData, limit);
            System.out.println("Средняя температура за " + limit + " дня(ей): " + averageTemp + "°C");

        } catch (Exception e) {
            System.err.println("Произошла ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static JSONObject fetchWeatherData(double lat, double lon, int limit) throws IOException, InterruptedException {
        // Проверяем границы параметров
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Широта (lat) должна быть в диапазоне от -90 до 90.");
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Долгота (lon) должна быть в диапазоне от -180 до 180.");
        }
        if (limit > 7 || limit < 1) {
            throw new IllegalArgumentException("Параметр 'limit' должен быть в диапазоне от 1 до 7.");
        }

        // Формируем URL
        String url = String.format(Locale.US, "%s?lat=%.2f&lon=%.2f&limit=%d", BASE_URL, lat, lon, limit);
        System.out.println("Запрос к API: " + url);

        // Создаём запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Yandex-Weather-Key", API_KEY)
                .GET()
                .build();

        // Отправляем запрос и получаем ответ
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем статус ответа
        if (response.statusCode() != 200) {
            System.err.println("Ошибка: API вернул статус " + response.statusCode());
            System.err.println("Ответ сервера: " + response.body());
            return null; // Возвращаем null в случае ошибки
        }

        // Преобразуем строку ответа в JSON
        return new JSONObject(response.body());
    }

    private static double calculateAverageTemperature(JSONObject weatherData, int limit) {
        double totalTemp = 0;
        int count = 0;

        // Если данные для прогноза доступны, то получаем температуру для каждого дня
        JSONArray days = weatherData.getJSONArray("forecasts");

        for (int i = 0; i < limit; i++) {
            // Извлекаем данные для каждого дня
            JSONObject dayData = days.getJSONObject(i);
            JSONObject parts = dayData.getJSONObject("parts");

            // Проверяем, что есть объект "day" и извлекаем температуру
            if (parts.has("day")) {
                JSONObject dayTempObj = parts.getJSONObject("day");

                // Пытаемся извлечь среднюю температуру (temp_avg)
                if (dayTempObj.has("temp_avg")) {
                    double dayTemp = dayTempObj.getDouble("temp_avg");
                    totalTemp += dayTemp;
                    count++;
                } else {
                    System.err.println("Не удалось найти среднюю температуру 'temp_avg' для дня " + i);
                }
            } else {
                System.err.println("Не удалось найти объект 'day' для дня " + i);
            }
        }

        // Вычисляем среднюю температуру
        return count > 0 ? totalTemp / count : 0.0;
    }
}