package com.danny.taskmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MailNotificationService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendBase;

    public MailNotificationService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.frontend-base-url}") String frontendBase) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendBase = frontendBase.trim();
    }

    public void sendProjectInvitation(String toEmail, String projectName, String token) {
        String base = frontendBase.endsWith("/") ? frontendBase.substring(0, frontendBase.length() - 1) : frontendBase;
        String link = base + "/?invite=" + token;
        String subject = "Einladung zum Projekt „" + projectName + "“";
        String body = "Sie wurden zu einem Projekt eingeladen.\n\nProjekt: " + projectName + "\n\nLink: " + link + "\n\n"
                + "Der Link ist begrenzt gültig.";

        if (mailSender == null) {
            log.info("Kein Mail-Server (spring.mail.host). Einladung an {} — Link: {}", toEmail, link);
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}
