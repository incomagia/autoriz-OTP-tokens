package otp.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import otp.service.AuthService;
import otp.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class AuthController {

    private final AuthService authService = new AuthService();

    public void start(HttpServer server) {
        server.createContext("/register", new RegisterHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/validate", new ValidateHandler());
        server.createContext("/generate-otp", new GenerateOtpHandler());
        server.createContext("/verify-otp", new VerifyOtpHandler());
    }

    static String readRequestBody(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = readRequestBody(exchange.getRequestBody());
            JSONObject json = new JSONObject(body);
            String login = json.optString("login");
            String password = json.optString("password");
            String role = json.optString("role", "user");

            boolean success = authService.register(login, password, role);

            String response = success ? "User registered" : "User already exists";
            int code = success ? 200 : 409;
            exchange.sendResponseHeaders(code, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = readRequestBody(exchange.getRequestBody());
            JSONObject json = new JSONObject(body);
            String login = json.optString("login");
            String password = json.optString("password");

            boolean valid = authService.validateCredentials(login, password);

            JSONObject responseJson = new JSONObject();
            int code;

            if (valid) {
                String token = authService.generateToken(login);
                responseJson.put("message", "Login successful");
                responseJson.put("token", token);
                code = 200;
            } else {
                responseJson.put("message", "Invalid login or password");
                code = 401;
            }

            byte[] responseBytes = responseJson.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    class ValidateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String token = exchange.getRequestHeaders().getFirst("Authorization");
            String login = authService.getLoginFromToken(token);

            JSONObject responseJson = new JSONObject();
            int code;

            if (login != null) {
                responseJson.put("message", "Token is valid");
                responseJson.put("user", login);
                code = 200;
            } else {
                responseJson.put("message", "Invalid or missing token");
                code = 401;
            }

            byte[] responseBytes = responseJson.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    class GenerateOtpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = readRequestBody(exchange.getRequestBody());
            JSONObject json = new JSONObject(body);
            String login = json.optString("login");

            String otpToken = authService.generateOtpToken(login);
            authService.sendOtpToAllChannels(otpToken); // 👉 Теперь рассылка действительно происходит

            JSONObject responseJson = new JSONObject();
            responseJson.put("otp_token", otpToken);

            byte[] responseBytes = responseJson.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }


    class VerifyOtpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = readRequestBody(exchange.getRequestBody());
            JSONObject json = new JSONObject(body);
            String otpToken = json.optString("otp_token");

            String login = authService.verifyOtpToken(otpToken);

            JSONObject responseJson = new JSONObject();
            if (login != null) {
                responseJson.put("message", "OTP token is valid");
                responseJson.put("user", login);
                exchange.sendResponseHeaders(200, responseJson.toString().getBytes().length);
            } else {
                responseJson.put("message", "Invalid OTP token");
                exchange.sendResponseHeaders(401, responseJson.toString().getBytes().length);
            }

            byte[] responseBytes = responseJson.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
