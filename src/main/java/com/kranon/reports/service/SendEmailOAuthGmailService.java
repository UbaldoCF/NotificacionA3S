package com.kranon.reports.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.BCC;


@Slf4j
@Service
public class SendEmailOAuthGmailService {

	private final String EMAIL_APPLICATION_APPS = "SendEmailBackendApps";
	private final String EmailFrom = "kranoncloud@kranon.com";
	private final int EmailPortNotification = 8888;


	
	public boolean sendEmail(
			List<String> vlDestinationEmail,
			List<String> vlDestinationEmailCopy,
			List<String> vlDestinationEmailCopyHide,
			String vsPathCredentials,
			String vsSubject, 
			String vsHtmlTemplate, 
			File voFile) {
		
		Gmail voServiceGmail = null;
		int viContIntent = 1;

		log.info("-------------------------------------------------");
		log.info("[ Creacion de credenciales del correo ]");
		log.info("-------------------------------------------------");
		try {
			NetHttpTransport voHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
			GsonFactory voJsonFactory = GsonFactory.getDefaultInstance();
			voServiceGmail = new Gmail.Builder(voHttpTransport, voJsonFactory,
					GetCredentials(voHttpTransport, voJsonFactory, vsPathCredentials))
					.setApplicationName(EMAIL_APPLICATION_APPS).build();

		} catch (GeneralSecurityException e) {
			log.error("SendEmailServiceAccount()-->ERROR: GeneralSecurityException=["
					+ e.getMessage() + "].");
		} catch (IOException e) {
			log.error("SendEmailServiceAccount()-->ERROR: IOException=[" + e.getMessage() + "].");
		} catch (Exception e) {
			log.error("SendEmailServiceAccount()-->ERROR: Exception=[" + e.getMessage() + "].");
		}

		if (voServiceGmail == null) {
			log.error("SendEmailServiceAccount()-->ERROR: No se genero la conexion con API GMAIL");
			return false;
		}

		do {


			log.info("-------------------------------------------------");
			log.info("[ Intento de envio de correo: {} ]", viContIntent);
			log.info("-------------------------------------------------"); 
			try {

				Properties voProps = new Properties();
				Session voSession = Session.getDefaultInstance(voProps, null);

				MimeMessage voEmail = new MimeMessage(voSession);
				voEmail.setFrom(new InternetAddress(EmailFrom));

				// Configuración de destinatarios
				if (vlDestinationEmail != null && !vlDestinationEmail.isEmpty()) {
					for (String vsToElement : vlDestinationEmail) {
						if (!vsToElement.trim().isEmpty()) {
							voEmail.addRecipient(TO, new InternetAddress(vsToElement.trim()));
						}
					}
				}

				// Configuración de CC (copia)
				if ( vlDestinationEmailCopy != null && ! vlDestinationEmailCopy.isEmpty()) {
					for (String vsCcElement : vlDestinationEmailCopy) {
						if (!vsCcElement.trim().isEmpty()) {
							voEmail.addRecipient(CC, new InternetAddress(vsCcElement.trim()));
						}
					}
				}

				// Configuración de BCC (copia oculta)
				if (vlDestinationEmailCopyHide != null && !vlDestinationEmailCopyHide.isEmpty()) {
					for (String vsConCcElement : vlDestinationEmailCopyHide) {
						if (!vsConCcElement.trim().isEmpty()) {
							voEmail.addRecipient(BCC, new InternetAddress(vsConCcElement.trim()));
						}
					}
				}

				voEmail.setSubject(vsSubject);

				Multipart voMultipart = new MimeMultipart();
				BodyPart voHtmlPart = new MimeBodyPart();

				voHtmlPart.setContent(vsHtmlTemplate, "text/html");
				voHtmlPart.setDisposition(BodyPart.INLINE);
				voMultipart.addBodyPart(voHtmlPart);
				voEmail.setContent(voMultipart);

				log.info("SendEmail()-->ByteArrayOutputStream.");
				ByteArrayOutputStream voBuffer = new ByteArrayOutputStream();
				voEmail.writeTo(voBuffer);
				byte[] rawMessageBytes = voBuffer.toByteArray();
				log.info("SendEmail()-->Encoded Email.");
				String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
				Message voMsg = new Message();
				voMsg.setRaw(encodedEmail);
				log.info("SendEmail()-->Send Email.");
				voMsg = voServiceGmail.users().messages().send("me", voMsg).execute();

				log.info("SendEmail()-->SUCCESS: MessageId=[" + voMsg.getId() + "], StatusSend="
						+ voMsg.toString());
				log.info("------------------------------------------------");
				log.info("El correo fue enviado con exito");
				log.info("------------------------------------------------");
				
				return true;
			} catch (MessagingException e) {
				log.error("SendEmail()-->ERROR: GoogleJsonResponseException=[" + e.getMessage()
						+ "].");
			} catch (GoogleJsonResponseException e) {
				log.error("SendEmail()-->ERROR: GoogleJsonResponseException=[" + e.getMessage()
						+ "].");
			} catch (IOException e) {
				log.error("SendEmail()-->ERROR: IOException=[" + e.getMessage() + "].");
			}
			viContIntent++;

		} while (viContIntent <= 4 );
		
		return false;
	}

	private Credential GetCredentials(NetHttpTransport voHttpTransport, GsonFactory voJsonFactory,
			String vsPathCredentials) {
		log.info("GetCredentials()-->Start Get Credentials.");
		Credential voCredentials = null;
		try {
			
			
			FileInputStream file = new FileInputStream(vsPathCredentials);
			
			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(voJsonFactory, new InputStreamReader(file));
			Set<String> voSet = new HashSet<>();
			voSet.add(GmailScopes.GMAIL_SEND);
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(voHttpTransport, voJsonFactory,
					clientSecrets, voSet).setDataStoreFactory(new FileDataStoreFactory(Paths.get("tokens").toFile()))
					.setAccessType("offline").build();
			LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(EmailPortNotification).build();
			voCredentials = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
		} catch (IOException e) {
			e.printStackTrace();
			log.info("GetCredentials()-->ERROR: IOException=[" + e.getMessage() + "].");
		} catch (Exception e) {
			e.printStackTrace();
			log.info("GetCredentials()-->ERROR: Exception=[" + e.getMessage() + "].");
		}
		log.info("GetCredentials()-->End Get Credentials=[" + voCredentials + "].");
		return voCredentials;
	}

}