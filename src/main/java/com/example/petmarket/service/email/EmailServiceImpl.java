package com.example.petmarket.service.email;

import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.OrderItem;
import com.example.petmarket.enums.DeliveryMethod;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailSender, AccountEmailSender, OrderEmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8088}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            log.info("Wysyłanie wiadomości email do: {}", to);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Email został wysłany pomyślnie.");
        } catch (MailException e) {
            log.error("Błąd wysyłania emaila do {}: {}", to, e.getMessage());
            log.error("SimpleMessage Pełny błąd SMTP:", e);
            throw new RuntimeException("Nie udało się wysłać wiadomości email", e);
        }
    }

    @Override
    public void sendActivationEmail(String to, String activationCode) {
        String activationLink = baseUrl + "/activate?code=" + activationCode;

        String subject = "Aktywacja konta - PetMarket";
        String htmlContent = String.format(
                """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #2d5a3f;">Witamy w PetMarket!</h2>
                        <p>Dziękujemy za rejestrację w naszym sklepie.</p>
                        <p>Aby aktywować swoje konto, kliknij poniższy przycisk:</p>
                        <p style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background-color: #2d5a3f; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">
                                Aktywuj konto
                            </a>
                        </p>
                        <p style="font-size: 12px; color: #666;">
                            Jeśli przycisk nie działa, skopiuj i wklej ten link w przeglądarkę:<br>
                            <a href="%s">%s</a>
                        </p>
                        <p>Jeśli nie rejestrowałeś się w naszym serwisie, zignoruj tę wiadomość.</p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                        <p style="font-size: 12px; color: #999;">Pozdrawiamy,<br>Zespół PetMarket</p>
                    </div>
                </body>
                </html>
                """,
                activationLink, activationLink, activationLink);

        sendHtmlMessage(to, subject, htmlContent);
    }

    private void sendHtmlMessage(String to, String subject, String htmlContent) {
        try {
            log.info("Wysyłanie wiadomości HTML email do: {}", to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email HTML został wysłany pomyślnie.");
        } catch (MessagingException | MailException e) {
            log.error("Błąd wysyłania emaila HTML do {}: {}", to, e.getMessage());
            log.error("Pełny błąd SMTP:", e);
            throw new RuntimeException("Nie udało się wysłać wiadomości email", e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;

        String subject = "Reset hasła - PetMarket";
        String htmlContent = String.format(
                """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #2d5a3f;">Reset hasła</h2>
                        <p>Otrzymaliśmy prośbę o reset hasła dla Twojego konta.</p>
                        <p>Aby zresetować hasło, kliknij poniższy przycisk:</p>
                        <p style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background-color: #2d5a3f; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">
                                Zresetuj hasło
                            </a>
                        </p>
                        <p style="font-size: 12px; color: #666;">
                            Jeśli przycisk nie działa, skopiuj i wklej ten link w przeglądarkę:<br>
                            <a href="%s">%s</a>
                        </p>
                        <p><strong>Link jest ważny przez 24 godziny.</strong></p>
                        <p>Jeśli nie prosiłeś o reset hasła, zignoruj tę wiadomość.</p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                        <p style="font-size: 12px; color: #999;">Pozdrawiamy,<br>Zespół PetMarket</p>
                    </div>
                </body>
                </html>
                """,
                resetLink, resetLink, resetLink);

        sendHtmlMessage(to, subject, htmlContent);
        log.info("Wysłano email resetowania hasła do: {}", to);
    }

    @Override
    public void sendShippingNotification(Order order) {
        String customerEmail = order.getCustomerEmail();
        String customerName = order.getCustomerName();
        String trackingNumber = order.getTrackingNumber();
        String carrierName = order.getDeliveryMethod() != null
                ? order.getDeliveryMethod().getDescription()
                : "Kurier";

        String subject = "Twoje zamówienie #" + order.getOrderNumber() + " zostało wysłane!";

        String trackingInfo = "";
        if (order.getDeliveryMethod() == DeliveryMethod.LOCKER) {
            trackingInfo = "Możesz śledzić przesyłkę na stronie: https://inpost.pl/sledzenie-przesylek?number=" + trackingNumber;
        } else if (order.getDeliveryMethod() == DeliveryMethod.COURIER) {
            trackingInfo = "Numer przesyłki do śledzenia: " + trackingNumber;
        }

        String text = String.format(
                "Cześć %s!\n\n" +
                        "Mamy świetne wieści - Twoje zamówienie #%s zostało właśnie wysłane!\n\n" +
                        "Szczegóły przesyłki:\n" +
                        "- Przewoźnik: %s\n" +
                        "- Numer przesyłki: %s\n\n" +
                        "%s\n\n" +
                        "Dziękujemy za zakupy w naszym sklepie!\n\n" +
                        "Pozdrawiamy,\n" +
                        "Zespół PetMarket",
                customerName != null ? customerName : "Kliencie",
                order.getOrderNumber(),
                carrierName,
                trackingNumber,
                trackingInfo);

        sendSimpleMessage(customerEmail, subject, text);
        log.info("Wysłano powiadomienie o wysyłce do {} dla zamówienia {}", customerEmail, order.getOrderNumber());
    }

    @Override
    public void sendOrderConfirmationEmail(Order order) {
        String to = order.getCustomerEmail();
        if (to == null || to.isEmpty()) {
            log.warn("Brak adresu email dla zamówienia {}", order.getOrderNumber());
            return;
        }
        String subject = "Potwierdzenie zamówienia #" + order.getOrderNumber() + " - PetMarket";
        StringBuilder itemsList = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String productName = item.getProduct() != null ? item.getProduct().getName() : "Produkt";
                BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                int quantity = item.getQuantity();
                BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(quantity));
                itemsList.append(String.format("- %s x%d: %.2f zł\n",
                        productName, quantity, itemTotal));
            }
        }
        StringBuilder text = new StringBuilder();
        text.append("Dziękujemy za złożenie zamówienia w PetMarket!\n\n");
        text.append("Numer zamówienia: ").append(order.getOrderNumber()).append("\n\n");
        text.append("=== ZAMÓWIONE PRODUKTY ===\n");
        text.append(itemsList);
        text.append("\n");
        if (order.getSubtotal() != null) {
            text.append("Wartość produktów: ").append(String.format("%.2f zł", order.getSubtotal())).append("\n");
        }
        if (order.getDeliveryCost() != null && order.getDeliveryCost().compareTo(BigDecimal.ZERO) > 0) {
            text.append("Koszt dostawy: ").append(String.format("%.2f zł", order.getDeliveryCost())).append("\n");
        }
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            text.append("Rabat: -").append(String.format("%.2f zł", order.getDiscountAmount())).append("\n");
        }
        text.append("\n");
        text.append("SUMA: ").append(String.format("%.2f zł", order.getTotalAmount())).append("\n\n");
        if (order.getDeliveryMethod() != DeliveryMethod.PICKUP) {
            text.append("Zostaniesz powiadomiony, gdy przesyłka zostanie wysłana.\n\n");
        }
        text.append("Dziękujemy za zakupy!\n");
        text.append("Zespół PetMarket");
        try {
            sendSimpleMessage(to, subject, text.toString());
            log.info("Wysłano potwierdzenie zamówienia {} na adres {}", order.getOrderNumber(), to);
        } catch (Exception e) {
            log.error("Nie udało się wysłać potwierdzenia zamówienia {} na adres {}: {}",
                    order.getOrderNumber(), to, e.getMessage());
        }
    }
}