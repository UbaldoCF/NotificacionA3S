package com.kranon.reports.service;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.mail.MessagingException;

@Slf4j
@Service
public class EmailService {

	private String emailCc = "jrodriguez@kranon.com";

	@Value("${spring.mail.username}")
	private String formEmail;

	private final String FORMAT_DATE = "yyyy-MM-dd";

	private final JavaMailSender javaMailSender;
	private final SpringTemplateEngine templateEngine;

	@Autowired
	public EmailService(JavaMailSender javaMailSender, SpringTemplateEngine templateEngine) {
		this.javaMailSender = javaMailSender;
		this.templateEngine = templateEngine;
	}

	@Async
	public void sendEmail(String to, String templateName, Context context) {

		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		log.info("Construccion del correo electronico");
		log.info("Enviando correo a:  {} ", to);
		try {
			helper.setTo(to);
			helper.setCc(emailCc);
			helper.setSubject("[UNITEC] - " + dateReport() + " Revision Reportes A3S");
			helper.setFrom(formEmail);
			helper.setPriority(1);
			String htmlContent = templateEngine.process(templateName, context);
			helper.setText(htmlContent, true);
		} catch (MessagingException e) {
			e.printStackTrace();
			log.error("Ocurrio un error al enviar el correo");

			log.error("{}", e.getMessage());
		}

		javaMailSender.send(message);
	}

	private String dateReport() {

		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());

		// Restar un día
		cal.add(Calendar.DAY_OF_YEAR, -1);

		// Obtener la fecha resultante
		Date fechaConUnDiaMenos = cal.getTime();

		// Formatear la fecha como desees
		SimpleDateFormat formatoFecha = new SimpleDateFormat(FORMAT_DATE);
		return formatoFecha.format(fechaConUnDiaMenos);
	}

}
