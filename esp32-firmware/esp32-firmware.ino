/*
    Copyright (C) 2025  Derrick Choi

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#include "WiFi.h"
#include "WiFiClientSecure.h"
#include "ESP32Servo.h"
#include "esp_wifi.h"
#include "esp_pm.h"
#include "Adafruit_MQTT.h"
#include "Adafruit_MQTT_Client.h"

// WiFi credentials
static constexpr const char* kSsid = "myNetwork";
static constexpr const char* kPassword = "myPassword";

// Adafruit IO credentials (TLS enabled)
#define AIO_SERVER "io.adafruit.com"
#define AIO_SERVERPORT 8883
#define AIO_USERNAME "myUsername"
#define AIO_KEY "myKey"

// Hardware configuration
Servo myServo;
static constexpr int kLedPin = 3;
static constexpr int kOffPin = 6;
static constexpr int kWaterSensorPin = 4;
static constexpr int kServoPin = 5;
static constexpr int kCloseAngle = 9;
static constexpr int kOpenAngle = 98;

// Sensor state
int data = 0;
static unsigned long lastSensorRead = 0;
static constexpr unsigned long kSensorInterval = 5000;

// io.adafruit.com root CA
static const char kAdafruitIoRootCa[] PROGMEM = \
      "-----BEGIN CERTIFICATE-----\n"
      "MIIEjTCCA3WgAwIBAgIQDQd4KhM/xvmlcpbhMf/ReTANBgkqhkiG9w0BAQsFADBh\n"
      "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n"
      "d3cuZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBH\n"
      "MjAeFw0xNzExMDIxMjIzMzdaFw0yNzExMDIxMjIzMzdaMGAxCzAJBgNVBAYTAlVT\n"
      "MRUwEwYDVQQKEwxEaWdpQ2VydCBJbmMxGTAXBgNVBAsTEHd3dy5kaWdpY2VydC5j\n"
      "b20xHzAdBgNVBAMTFkdlb1RydXN0IFRMUyBSU0EgQ0EgRzEwggEiMA0GCSqGSIb3\n"
      "DQEBAQUAA4IBDwAwggEKAoIBAQC+F+jsvikKy/65LWEx/TMkCDIuWegh1Ngwvm4Q\n"
      "yISgP7oU5d79eoySG3vOhC3w/3jEMuipoH1fBtp7m0tTpsYbAhch4XA7rfuD6whU\n"
      "gajeErLVxoiWMPkC/DnUvbgi74BJmdBiuGHQSd7LwsuXpTEGG9fYXcbTVN5SATYq\n"
      "DfbexbYxTMwVJWoVb6lrBEgM3gBBqiiAiy800xu1Nq07JdCIQkBsNpFtZbIZhsDS\n"
      "fzlGWP4wEmBQ3O67c+ZXkFr2DcrXBEtHam80Gp2SNhou2U5U7UesDL/xgLK6/0d7\n"
      "6TnEVMSUVJkZ8VeZr+IUIlvoLrtjLbqugb0T3OYXW+CQU0kBAgMBAAGjggFAMIIB\n"
      "PDAdBgNVHQ4EFgQUlE/UXYvkpOKmgP792PkA76O+AlcwHwYDVR0jBBgwFoAUTiJU\n"
      "IBiV5uNu5g/6+rkS7QYXjzkwDgYDVR0PAQH/BAQDAgGGMB0GA1UdJQQWMBQGCCsG\n"
      "AQUFBwMBBggrBgEFBQcDAjASBgNVHRMBAf8ECDAGAQH/AgEAMDQGCCsGAQUFBwEB\n"
      "BCgwJjAkBggrBgEFBQcwAYYYaHR0cDovL29jc3AuZGlnaWNlcnQuY29tMEIGA1Ud\n"
      "HwQ7MDkwN6A1oDOGMWh0dHA6Ly9jcmwzLmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydEds\n"
      "b2JhbFJvb3RHMi5jcmwwPQYDVR0gBDYwNDAyBgRVHSAAMCowKAYIKwYBBQUHAgEW\n"
      "HGh0dHBzOi8vd3d3LmRpZ2ljZXJ0LmNvbS9DUFMwDQYJKoZIhvcNAQELBQADggEB\n"
      "AIIcBDqC6cWpyGUSXAjjAcYwsK4iiGF7KweG97i1RJz1kwZhRoo6orU1JtBYnjzB\n"
      "c4+/sXmnHJk3mlPyL1xuIAt9sMeC7+vreRIF5wFBC0MCN5sbHwhNN1JzKbifNeP5\n"
      "ozpZdQFmkCo+neBiKR6HqIA+LMTMCMMuv2khGGuPHmtDze4GmEGZtYLyF8EQpa5Y\n"
      "jPuV6k2Cr/N3XxFpT3hRpt/3usU/Zb9wfKPtWpoznZ4/44c1p9rzFcZYrWkj3A+7\n"
      "TNBJE0GmP2fhXhP1D/XVfIW/h0yCJGEiV9Glm/uGOa3DXHlmbAcxSyCRraG+ZBkA\n"
      "7h4SeM6Y8l/7MBRpPCz6l8Y=\n"
      "-----END CERTIFICATE-----\n";

// MQTT setup with TLS
WiFiClientSecure client;
Adafruit_MQTT_Client mqtt(&client, AIO_SERVER, AIO_SERVERPORT, AIO_USERNAME, AIO_KEY);
Adafruit_MQTT_Publish waterSensorFeed = Adafruit_MQTT_Publish(&mqtt, AIO_USERNAME "/feeds/water-sensor");
Adafruit_MQTT_Subscribe valveControlFeed = Adafruit_MQTT_Subscribe(&mqtt, AIO_USERNAME "/feeds/valve-control");

// Functions for hardware control
void moveServoTo(Servo& servo, int position, int pin) {
    servo.attach(pin);
    servo.write(position);
    delay(500);
    servo.detach();
    Serial.println("Servo moved to " + String(position) + " and detached for power savings");
};

void configurePowerManagement() {
    esp_pm_config_esp32_t pm_config = {
            .max_freq_mhz = 240,
            .min_freq_mhz = 80,
            .light_sleep_enable = true
    };
    esp_pm_configure(&pm_config);
    esp_wifi_set_ps(WIFI_PS_MAX_MODEM);
    esp_wifi_set_max_tx_power(8);
};

void connectToWiFi(const char* network, const char* pass) {
    if (WiFi.status() == WL_CONNECTED) return;

    pinMode(LED_BUILTIN, OUTPUT);
    pinMode(kLedPin, OUTPUT);

    while (WiFi.status() != WL_CONNECTED) {
        WiFi.begin(kSsid, kPassword);
        Serial.println("Connecting to WiFi...");

        for (int cycle = 0; cycle < 5; cycle++) {
            digitalWrite(LED_BUILTIN, LOW);
            digitalWrite(kLedPin, HIGH);
            delay(200);
            digitalWrite(LED_BUILTIN, HIGH);
            digitalWrite(kLedPin, LOW);
            delay(200);
        }
    }

    digitalWrite(LED_BUILTIN, LOW);
    pinMode(LED_BUILTIN, INPUT);

    Serial.print("Connected! IP: ");
    Serial.println(WiFi.localIP());
};

// MQTT connection function
void connectToMQTT(Adafruit_MQTT_Client& mqttClient) {
    if (mqttClient.connected()) return;

    Serial.println("Connecting to Adafruit IO MQTT...");
    int8_t ret;

    while ((ret = mqttClient.connect()) != 0) {
        connectToWiFi(kSsid, kPassword);
        Serial.println(mqttClient.connectErrorString(ret));
        Serial.println("Retrying MQTT connection in 5 seconds...");
        mqttClient.disconnect();
        delay(5000);
    }

    Serial.println("MQTT Connected!");
};

// Sensor data publishing
void publishSensorData(Adafruit_MQTT_Publish& feed, int reading) {
    char json[32];

    int len = snprintf(json, sizeof(json), "{\"reading\":%d}", reading);

    if (len < 0 || len >= sizeof(json)) {
        Serial.println("JSON buffer overflow!");
        return;
    }

    if (!feed.publish(json)) {
        Serial.println("Failed to publish sensor data");
    } else {
        Serial.print("Published: ");
        Serial.println(json);
    }
}

// Handle valve control commands
void handleValveCommand(const char* command, Servo& servo, int pin) {
    if (strcasecmp(command, "open") == 0) {
        moveServoTo(servo, kOpenAngle, pin);
    }
    else if (strcasecmp(command, "close") == 0) {
        moveServoTo(servo, kCloseAngle, pin);
    }
};

void setup() {
    Serial.begin(115200);
    Serial.println("Starting ESP32 with Adafruit IO MQTT over TLS...");

    configurePowerManagement();
    btStop();
    connectToWiFi(kSsid, kPassword);

    client.setCACert(kAdafruitIoRootCa);

    // Subscribe to valve control feed
    mqtt.subscribe(&valveControlFeed);

    connectToMQTT(mqtt);

    Serial.println("Servo configured for pin " + String(kServoPin) + " (attach-on-demand)");
    Serial.println("MQTT TLS setup complete. Device will enter light sleep when idle...");

    publishSensorData(waterSensorFeed, data);
}

void loop() {
    // Maintain MQTT connection
    connectToMQTT(mqtt);

    // Process MQTT messages
    Adafruit_MQTT_Subscribe *subscription;
    while ((subscription = mqtt.readSubscription(100))) {
        if (subscription == &valveControlFeed) {
            handleValveCommand((char *)valveControlFeed.lastread, myServo, kServoPin);
        }
    }

    // Read and publish sensor data
    if (millis() - lastSensorRead >= kSensorInterval) {
        data = analogRead(kWaterSensorPin);

        // Auto-close valve if water detected
        if (data >= 1000) {
            moveServoTo(myServo, kCloseAngle, kServoPin);
            publishSensorData(waterSensorFeed, data);
            delay(500);
            pinMode(kOffPin, OUTPUT);
            digitalWrite(kOffPin, HIGH);
        }

        lastSensorRead = millis();
    }

    // Ping to keep connection alive
    if (!mqtt.ping()) {
        mqtt.disconnect();
    }
}