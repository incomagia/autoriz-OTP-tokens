package otp.dao;

import otp.model.User;

import java.sql.*;
import java.util.Optional;

public class UserDAO {
    private final String url = "jdbc:sqlite:otp.db";

    public void init() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                login TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL
            );
        """;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("✅ Таблица users инициализирована");

        } catch (SQLException e) {
            System.err.println("Ошибка при инициализации таблицы users: " + e.getMessage());
        }
    }

    public boolean userExists(String login) {
        String sql = "SELECT 1 FROM users WHERE login = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("Ошибка при проверке пользователя: " + e.getMessage());
            return false;
        }
    }

    public boolean createUser(User user) {
        String sql = "INSERT INTO users (login, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getLogin());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole());
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Ошибка при создании пользователя: " + e.getMessage());
            return false;
        }
    }

    public User findByLogin(String login) {
        String sql = "SELECT * FROM users WHERE login = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("password"),
                        rs.getString("role")
                );
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при поиске пользователя: " + e.getMessage());
        }

        return null;
    }

    public void save(User user) {
        if (!userExists(user.getLogin())) {
            createUser(user);
        }
    }

    public Optional<User> findByLoginOptional(String login) {
        User user = findByLogin(login);
        return Optional.ofNullable(user);
    }

    public boolean isEmpty(String login) {
        return findByLogin(login) == null;
    }
}
