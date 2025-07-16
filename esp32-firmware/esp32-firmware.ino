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
static constexpr const char* WIFI_SSID = "myNetwork";
static constexpr const char* WIFI_PASSWORD = "myPassword";

// Adafruit IO credentials (TLS enabled)
#define AIO_SERVER "io.adafruit.com"
#define AIO_SERVERPORT 8883
#define AIO_USERNAME "myUsername"
#define AIO_KEY "myKey"

// Hardware pin configuration
static constexpr int LED_PIN = 3;
static constexpr int WATER_SENSOR_PIN = 4;
static constexpr int SERVO_PIN = 5;
static constexpr int LATCH_INPUT_PIN = 6;
static constexpr int LATCH_OUTPUT_PIN = 7;

// Servo position constants
static constexpr int VALVE_CLOSE_ANGLE = 9;
static constexpr int VALVE_OPEN_ANGLE = 98;

// Sensor timing constants
static constexpr unsigned long SENSOR_READ_INTERVAL_MS = 5000;
static constexpr int WATER_DETECTION_THRESHOLD = 1000;

// Button timing
static constexpr unsigned long SHUTOFF_HOLD_TIME = 2000;

// Hardware objects
Servo water_valve_servo;

// Sensor state variables
int water_sensor_reading = 0;
static unsigned long last_sensor_read_time = 0;
static unsigned long last_button_time = 0;
static bool button_pressed = false;

// io.adafruit.com root CA certificate
static const char ADAFRUIT_IO_ROOT_CA[] PROGMEM = \
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
WiFiClientSecure secure_client;
Adafruit_MQTT_Client mqtt_client(&secure_client, AIO_SERVER, AIO_SERVERPORT, AIO_USERNAME, AIO_KEY);
Adafruit_MQTT_Publish water_sensor_feed = Adafruit_MQTT_Publish(&mqtt_client, AIO_USERNAME "/feeds/water-sensor");
Adafruit_MQTT_Publish valve_ack_feed = Adafruit_MQTT_Publish(&mqtt_client, AIO_USERNAME "/feeds/valve-ack");
Adafruit_MQTT_Subscribe valve_control_feed = Adafruit_MQTT_Subscribe(&mqtt_client, AIO_USERNAME "/feeds/valve-control");

// Functions for hardware control
void move_servo_to_position(Servo& servo, int position, int pin)
{
    servo.attach(pin);
    servo.write(position);
    delay(500);
    servo.detach();
    Serial.println("Servo moved to " + String(position) + " and detached for power savings");

    const char* ack_message = (position == VALVE_OPEN_ANGLE) ? "opened" : "closed";
    if (!valve_ack_feed.publish(ack_message)) {
        Serial.println("Failed to publish ack");
    }
}

void configure_power_management()
{
    esp_pm_config_esp32_t pm_config = {
            .max_freq_mhz = 240,
            .min_freq_mhz = 80,
            .light_sleep_enable = true
    };
    esp_pm_configure(&pm_config);
    esp_wifi_set_ps(WIFI_PS_MAX_MODEM);
    esp_wifi_set_max_tx_power(8);
}

void connect_to_wifi(const char* network_ssid, const char* network_password)
{
    if (WiFi.status() == WL_CONNECTED) {
        return;
    }

    pinMode(LED_BUILTIN, OUTPUT);
    pinMode(LED_PIN, OUTPUT);

    while (WiFi.status() != WL_CONNECTED) {
        WiFi.begin(network_ssid, network_password);
        Serial.println("Connecting to WiFi...");

        for (int cycle = 0; cycle < 5; ++cycle) {
            check_button();
            digitalWrite(LED_BUILTIN, LOW);
            digitalWrite(LED_PIN, LOW);
            delay(200);
            digitalWrite(LED_BUILTIN, HIGH);
            digitalWrite(LED_PIN, HIGH);
            delay(200);
        }
    }

    digitalWrite(LED_BUILTIN, LOW);
    pinMode(LED_BUILTIN, INPUT);

    Serial.print("Connected! IP: ");
    Serial.println(WiFi.localIP());
}

// MQTT connection function
void connect_to_mqtt(Adafruit_MQTT_Client& mqtt_client_ref)
{
    if (mqtt_client_ref.connected()) {
        return;
    }

    Serial.println("Connecting to Adafruit IO MQTT...");
    int8_t connection_result;

    while ((connection_result = mqtt_client_ref.connect()) != 0) {
        check_button();
        connect_to_wifi(WIFI_SSID, WIFI_PASSWORD);
        Serial.println(mqtt_client_ref.connectErrorString(connection_result));
        Serial.println("Retrying MQTT connection in 5 seconds...");
        mqtt_client_ref.disconnect();
        delay(5000);
    }

    Serial.println("MQTT Connected!");
}

void check_button()
{
    if (digitalRead(LATCH_INPUT_PIN) == LOW && !button_pressed) {
        button_pressed = true;
        last_button_time = millis();
    } else if (digitalRead(LATCH_INPUT_PIN) == HIGH) {
        button_pressed = false;
    }
    if (millis() - last_button_time >= SHUTOFF_HOLD_TIME && button_pressed) {
        digitalWrite(LATCH_OUTPUT_PIN, LOW);
        digitalWrite(LED_PIN, LOW);
        pinMode(LED_PIN, INPUT);
    }
}

// Sensor data publishing
void publish_sensor_data(Adafruit_MQTT_Publish& feed, int reading)
{
    char json_buffer[32];

    int json_length = snprintf(json_buffer, sizeof(json_buffer), "{\"reading\":%d}", reading);

    if (json_length < 0 || json_length >= sizeof(json_buffer)) {
        Serial.println("JSON buffer overflow!");
        return;
    }

    if (!feed.publish(json_buffer)) {
        Serial.println("Failed to publish sensor data");
    } else {
        Serial.print("Published: ");
        Serial.println(json_buffer);
    }
}

// Handle valve control commands
void handle_valve_command(const char* command, Servo& servo, int servo_pin)
{
    if (strcasecmp(command, "open") == 0) {
        move_servo_to_position(servo, VALVE_OPEN_ANGLE, servo_pin);
    }
    else if (strcasecmp(command, "close") == 0) {
        move_servo_to_position(servo, VALVE_CLOSE_ANGLE, servo_pin);
    }
}

void setup()
{
    pinMode(LATCH_OUTPUT_PIN, OUTPUT);
    digitalWrite(LATCH_OUTPUT_PIN, HIGH);

    pinMode(LATCH_INPUT_PIN, INPUT_PULLUP);

    Serial.begin(115200);
    Serial.println("Starting ESP32 with Adafruit IO MQTT over TLS...");

    configure_power_management();
    btStop();
    connect_to_wifi(WIFI_SSID, WIFI_PASSWORD);

    secure_client.setCACert(ADAFRUIT_IO_ROOT_CA);

    // Subscribe to valve control feed
    mqtt_client.subscribe(&valve_control_feed);

    connect_to_mqtt(mqtt_client);

    Serial.println("Servo configured for pin " + String(SERVO_PIN) + " (attach-on-demand)");
    Serial.println("MQTT TLS setup complete. Device will enter light sleep when idle...");

    publish_sensor_data(water_sensor_feed, water_sensor_reading);
}

void loop()
{
    check_button();

    // Maintain MQTT connection
    connect_to_mqtt(mqtt_client);

    // Process MQTT messages
    Adafruit_MQTT_Subscribe* subscription;
    while ((subscription = mqtt_client.readSubscription(100))) {
        if (subscription == &valve_control_feed) {
            handle_valve_command(
                    reinterpret_cast<char*>(valve_control_feed.lastread),
                    water_valve_servo,
                    SERVO_PIN
            );
        }
    }

    // Read and publish sensor data
    if (millis() - last_sensor_read_time >= SENSOR_READ_INTERVAL_MS) {
        water_sensor_reading = analogRead(WATER_SENSOR_PIN);

        // Auto-close valve if water detected
        if (water_sensor_reading >= WATER_DETECTION_THRESHOLD) {
            move_servo_to_position(water_valve_servo, VALVE_CLOSE_ANGLE, SERVO_PIN);
            publish_sensor_data(water_sensor_feed, water_sensor_reading);
            delay(500);
            digitalWrite(LATCH_OUTPUT_PIN, LOW);
        }

        last_sensor_read_time = millis();
    }

    // Ping to keep connection alive
    if (!mqtt_client.ping()) {
        mqtt_client.disconnect();
    }
}