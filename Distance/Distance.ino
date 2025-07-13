#include <WiFi.h>            // CC3200 WiFi library
#include "Ultrasonic.h"      // Ultrasonic sensor library

// Pin configuration
#define BUZZER_PIN      39   // Digital output for buzzer
#define TRIG_ECHO_PIN   24   // Shared trigger/echo pin for ultrasonic sensor (Grove style)
#define LED_PIN         3    // Optional LED for visual alert

// WiFi Access Point credentials
char ssid[] = "DistanceMeter";      // WiFi hotspot SSID
char password[] = "12345678";       // WiFi hotspot password (min 8 characters for WPA2)

// Server and sensor objects
WiFiServer server(80);              // HTTP server on port 80
Ultrasonic ultrasonic(TRIG_ECHO_PIN); // Ultrasonic sensor using shared TRIG/ECHO pin

long rangeInCm = 0;  // Variable to hold measured distance in cm

void setup() {
  // Initialize output devices
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(LED_PIN, OUTPUT);

  // Begin serial communication for debugging
  Serial.begin(9600);

  // Start WiFi Access Point mode
  WiFi.beginNetwork(ssid, password);   // Energia-style API for AP mode
  while (WiFi.localIP() == INADDR_NONE) {
    delay(1000);
    Serial.println("Waiting for AP IP...");
  }

  // Print IP address once ready
  Serial.print("AP IP: ");
  Serial.println(WiFi.localIP());

  // Start HTTP server
  server.begin();
}

void loop() {
  // 1. Measure distance from ultrasonic sensor
  rangeInCm = ultrasonic.MeasureInCentimeters();
  Serial.print("Distance: ");
  Serial.println(rangeInCm);

  // 2. Buzzer and LED alert for short distance (< 20 cm)
  if (rangeInCm < 20) {
    digitalWrite(BUZZER_PIN, HIGH);   // Turn on buzzer
    digitalWrite(LED_PIN, HIGH);      // Flash LED
    delay(200);
    digitalWrite(LED_PIN, LOW);
    delay(200);
  } else {
    digitalWrite(BUZZER_PIN, LOW);    // Turn off buzzer
    digitalWrite(LED_PIN, LOW);       // Turn off LED
    delay(1000);                       // Reduce polling frequency
  }

  // 3. Handle HTTP client request
  WiFiClient client = server.available();  // Check for incoming client
  if (client) {
    Serial.println("Client connected");

    // Read the HTTP request
    String req = client.readStringUntil('\r');
    client.flush(); // Clear any remaining data

    // 4. Respond to /sensor endpoint with JSON data
    if (req.indexOf("/sensor") != -1) {
      String json = "{ \"distance\": " + String(rangeInCm) + " }";

      // Send HTTP response
      client.println("HTTP/1.1 200 OK");
      client.println("Content-Type: application/json");
      client.println("Connection: close");
      client.println();  // End of headers
      client.println(json);  // Body with JSON data
    } else {
      // 404 Not Found for any other endpoint
      client.println("HTTP/1.1 404 Not Found");
      client.println("Connection: close");
      client.println();
    }

    // Close connection
    client.stop();
    Serial.println("Client disconnected");
  }
}
