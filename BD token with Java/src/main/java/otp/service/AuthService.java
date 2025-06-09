package otp.service;

import otp.dao.UserDAO;
import otp.model.User;
import otp.sender.OtpDispatcher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    private final Map<String, String> tokens = new HashMap<>(); // token -> login
    private final Map<String, String> otpTokens = new HashMap<>(); // OTP token -> login

    public boolean register(String login, String password, String role) {
        if (userDAO.userExists(login)) {
            return false; // Пользователь уже существует
        }
        User user = new User();
        user.setLogin(login);
        user.setPassword(hashPassword(password));
        user.setRole(role);
        return userDAO.createUser(user);
    }

    public boolean validateCredentials(String login, String password) {
        User user = userDAO.findByLogin(login);
        if (user == null) return false;
        return user.getPassword().equals(hashPassword(password));
    }

    public User getUserByLogin(String login) {
        return userDAO.findByLogin(login);
    }

    public String generateToken(String login) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, login);
        return token;
    }

    public String getLoginFromToken(String token) {
        return tokens.get(token); // вернёт null, если токена нет
    }

    public String generateOtpToken(String login) {
        String otpToken = UUID.randomUUID().toString();
        otpTokens.put(otpToken, login);
        return otpToken;
    }

    public String verifyOtpToken(String otpToken) {
        return otpTokens.remove(otpToken); // если нет токена, вернёт null
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка хэширования пароля", e);
        }
    }

    public void sendOtpToAllChannels(String code) {
        OtpDispatcher.sendToFile(code);
        OtpDispatcher.sendToTelegram("7550440515:AAFQMO9mDenJoaUDoIn_eg8yDWntP_JbLMc", "1937266059", code);
        OtpDispatcher.sendToEmail("mogila.misha@gmail.com", "Ваш OTP-код", code);
    }
}
