#include "BluetoothSerial.h"
#include "WiFi.h"
#include "ESPAsyncWebServer.h"
#include "ESP32Servo.h"
#include "esp_wifi.h"
#include "esp_pm.h"
#include "AsyncEventSource.h"
 
const char* ssid = "myNetwork";
const char* password = "myPassword";
AsyncWebServer server(80);
AsyncEventSource events("/events"); // SSE endpoint
Servo myservo;
static constexpr int waterSensorPin = 4;
static constexpr int servoPin = 5;
static constexpr int closeAngle = 9;
static constexpr int openAngle = 98;

bool triggered = false;
int data = 0;
int last_data = 0;
static unsigned long lastSensorRead = 0;
const unsigned long SENSOR_INTERVAL = 5000;

const auto moveServoTo = [](Servo& servo, int position, int pin) -> void {
  servo.attach(pin);
  servo.write(position);
  delay(500);
  servo.detach();
  Serial.println("Servo moved to " + String(position) + " and detached for power savings");
};

const auto configurePowerManagement = []() -> void {
  esp_pm_config_esp32_t pm_config = {
    .max_freq_mhz = 240,
    .min_freq_mhz = 80,
    .light_sleep_enable = true
  };
  esp_pm_configure(&pm_config);
  esp_wifi_set_ps(WIFI_PS_MAX_MODEM);
  esp_wifi_set_max_tx_power(8);
};

const auto connectToWiFi = [](const char* network, const char* pass) -> void {
  WiFi.begin(network, pass);
  
  while (WiFi.status() != WL_CONNECTED) {
    pinMode(LED_BUILTIN, OUTPUT);
    digitalWrite(LED_BUILTIN, LOW);
    delay(200);
    digitalWrite(LED_BUILTIN, HIGH);
    delay(200);
    Serial.println("Connecting to WiFi...");
  }
  
  Serial.print("Connected! IP: ");
  Serial.println(WiFi.localIP());
};

// Pure function to send sensor event
const auto sendSensorEvent = [](AsyncEventSource& eventSource, int reading, bool triggered) -> void {
  String json = "{\"reading\":" + String(reading) + ",\"triggered\":" + (triggered ? "true" : "false") + "}";
  eventSource.send(json.c_str(), "sensor", millis());
};

const auto configureServerRoutes = [](AsyncWebServer& srv, Servo& servo, int pin, AsyncEventSource& eventSource) -> void {
  srv.on("/open", HTTP_POST, [&servo, pin](AsyncWebServerRequest *request) {
    Serial.println("Received open request - waking from light sleep");
    moveServoTo(servo, openAngle, pin);
    request->send(200, "text/plain", "Valve opened");
    Serial.println("Returning to light sleep with servo detached");
  });
  
  srv.on("/close", HTTP_POST, [&servo, pin](AsyncWebServerRequest *request) {
    Serial.println("Received close request - waking from light sleep");
    moveServoTo(servo, closeAngle, pin);
    request->send(200, "text/plain", "Valve closed");
    Serial.println("Returning to light sleep with servo detached");
  });
  
  // Add event source handler
  srv.addHandler(&eventSource);
  
  // CORS headers for SSE
  DefaultHeaders::Instance().addHeader("Access-Control-Allow-Origin", "*");
  DefaultHeaders::Instance().addHeader("Access-Control-Allow-Headers", "Content-Type");
};

void setup() {
  Serial.begin(115200);
  Serial.println("Starting ESP32 with power management...");
  
  configurePowerManagement();
  btStop();
  connectToWiFi(ssid, password);
  
  Serial.println("Servo configured for pin " + String(servoPin) + " (attach-on-demand)");
  
  configureServerRoutes(server, myservo, servoPin, events);
  
  server.begin();
  Serial.println("HTTP server started with SSE support");
  Serial.println("Device will enter light sleep when idle...");
}

void loop() {
  if (millis() - lastSensorRead >= SENSOR_INTERVAL) {
    data = analogRead(waterSensorPin);
    triggered = (data >= 1000 && last_data < 1000);
    sendSensorEvent(events, data, triggered);
    if (triggered) {
      moveServoTo(myservo, closeAngle, servoPin);
    }
    
    last_data = data;
    lastSensorRead = millis();
  }
}