package otp.sender;

import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class OtpDispatcher {

    public static void sendToFile(String code) {
        try (FileWriter writer = new FileWriter("OTPcode.txt")) {
            writer.write(code);
            System.out.println("📄 OTP-код записан в файл OTPcode.txt");
        } catch (IOException e) {
            System.err.println("❌ Ошибка при записи в файл: " + e.getMessage());
        }
    }

    public static void sendToTelegram(String token, String chatId, String message) {
        try {
            String urlString = "https://api.telegram.org/bot" + token +
                    "/sendMessage?chat_id=" + chatId +
                    "&text=" + URLEncoder.encode(message, "UTF-8");

            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            System.out.println("📨 Telegram response code: " + responseCode);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке в Telegram: " + e.getMessage());
        }
    }

    public static void sendToEmail(String toEmail, String subject, String body) {
        try {
            Properties props = new Properties();
            props.load(OtpDispatcher.class.getClassLoader().getResourceAsStream("email.properties"));

            final String username = props.getProperty("email.username");
            final String password = props.getProperty("email.password");
            final String fromEmail = props.getProperty("email.from");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);

            System.out.println("📧 Email отправлен: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке Email: " + e.getMessage());
        }
    }
}
