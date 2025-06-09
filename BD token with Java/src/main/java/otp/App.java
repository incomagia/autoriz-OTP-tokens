package otp;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

import otp.controller.AuthController;
import otp.dao.UserDAO;
import otp.model.User;
import otp.service.AuthService;

public class App {
    public static void main(String[] args) throws IOException {

        UserDAO userDAO = new UserDAO();
        userDAO.init(); // инициализация таблицы users

        // Список заранее заданных пользователей (логин, пароль, роль)
        List<User> initialUsers = List.of(
                new User("admin", AuthService.hashPassword("admin123"), "ADMIN"),
                new User("user1", AuthService.hashPassword("user123"), "USER"),
                new User("user2", AuthService.hashPassword("pass456"), "USER")
        );

        // Вставка в БД, если пользователя ещё нет
        for (User user : initialUsers) {
            if (userDAO.isEmpty(user.getLogin())) {
                userDAO.save(user);
                System.out.println("✅ Добавлен пользователь: " + user.getLogin());
            } else {
                System.out.println("ℹ️ Уже существует: " + user.getLogin());
            }
        }

        // Запускаем HTTP-сервер на порту 8081
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        // Регистрируем маршрут /ping и его обработчик
        server.createContext("/ping", new PingHandler());

        // Регистрируем контроллер авторизации
        AuthController authController = new AuthController();
        authController.start(server);

        // Используем стандартный пул потоков
        server.setExecutor(null);

        // Запускаем сервер
        server.start();
        System.out.println("🚀 Сервер запущен на http://localhost:8081");
    }

    // Встроенный обработчик /ping
    static class PingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Проверка типа запроса — только GET
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Метод не разрешён
                return;
            }

            System.out.println("🔥 /ping был вызван");

            // Ответ
            String response = "pong";
            byte[] responseBytes = response.getBytes("UTF-8");

            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
