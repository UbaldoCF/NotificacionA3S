package com.kranon.reports.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kranon.reports.config.PropertiesConfig;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ValidateService {

	@Setter(onMethod = @__(@Autowired))
	private PropertiesConfig voModel;
	
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public String validateProperties() {

        if (voModel == null) {
            log.error("La configuracion PropertiesConfig es nula");
            return "La configuracion general no puede ser nula.";
        }

        String error;

        error = validateRequiredString(voModel.getCompany(), "company", "La empresa no puede estar vacia.");
        if (error != null) return error;

        error = validateRequiredString(voModel.getHostSFTP(), "hostSFTP", "El host SFTP no puede estar vacio.");
        if (error != null) return error;

        error = validatePort(voModel.getPortSFTP(), "portSFTP");
        if (error != null) return error;

        error = validateRequiredString(voModel.getUserSFTP(), "userSFTP", "El usuario SFTP no puede estar vacio.");
        if (error != null) return error;

        error = validateRequiredString(voModel.getPasswordSFTP(), "passwordSFTP", "La contraseña SFTP no puede estar vacia.");
        if (error != null) return error;

        error = validateRequiredString(voModel.getPathOutboundSFTP(), "pathOutboundSFTP", "La ruta pathOutboundSFTP no puede estar vacia.");
        if (error != null) return error;

        error = validateRequiredString(voModel.getPathInbuoundSFTP(), "pathInbuoundSFTP", "La ruta pathInbuoundSFTP no puede estar vacia.");
        if (error != null) return error;

        error = validateRequiredString(voModel.getPathAgenteSFTP(), "pathAgenteSFTP", "La ruta pathAgenteSFTP no puede estar vacia.");
        if (error != null) return error;

        error = validateRequiredString(voModel.getPathRedesSFTP(), "pathRedesSFTP", "La ruta pathRedesSFTP no puede estar vacia.");
        if (error != null) return error;

        error = validateRequiredString(voModel.getPathLocalOutboundSFTP(), "pathLocalOutboundSFTP", "La ruta local pathLocalOutboundSFTP no puede estar vacia.");
        if (error != null) return error;

        error = validateRequiredString(voModel.getPathLocalInbuoundSFTP(), "pathLocalInbuoundSFTP", "La ruta local pathLocalInbuoundSFTP no puede estar vacia.");
        if (error != null) return error;

        error = validateRequiredString(voModel.getPathLocalAgenteSFTP(), "pathLocalAgenteSFTP", "La ruta local pathLocalAgenteSFTP no puede estar vacia.");
        if (error != null) return error;

        error = validateRequiredString(voModel.getPathLocalRedesSFTP(), "pathLocalRedesSFTP", "La ruta local pathLocalRedesSFTP no puede estar vacia.");
        if (error != null) return error;

        error = validateRequiredEmailList(
                voModel.getEmailDestinationReport(),
                "emailDestinationReport",
                "Debe existir al menos un correo valido en emailDestinationReport."
        );
        if (error != null) return error;

        error = validateOptionalEmailList(
                voModel.getDestinationEmailCopyReport(),
                "destinationEmailCopyReport"
        );
        if (error != null) return error;

        error = validateOptionalEmailList(
                voModel.getDestinationEmailCopyHideReport(),
                "destinationEmailCopyHideReport"
        );
        if (error != null) return error;

        error = validateRequiredString(
                voModel.getVsPathCredencialesAuth(),
                "vsPathCredencialesAuth",
                "La ruta vsPathCredencialesAuth no puede estar vacia."
        );
        if (error != null) return error;

        log.info("Validacion de propiedades completada correctamente");
        return null;
    }

    private String validateRequiredString(String value, String propertyName, String message) {
        if (value == null || value.trim().isEmpty()) {
            log.error("Propiedad invalida [{}]: {}", propertyName, message);
            return message;
        }
        return null;
    }

    private String validatePort(String portValue, String propertyName) {
        if (portValue == null || portValue.trim().isEmpty()) {
            String message = "El puerto SFTP no puede estar vacio.";
            log.error("Propiedad invalida [{}]: {}", propertyName, message);
            return message;
        }

        try {
            int port = Integer.parseInt(portValue.trim());
            if (port < 1 || port > 65535) {
                String message = "El puerto SFTP no es valido. Debe estar entre 1 y 65535.";
                log.error("Propiedad invalida [{}]: {}", propertyName, message);
                return message;
            }
        } catch (NumberFormatException e) {
            String message = "El puerto SFTP debe ser numerico.";
            log.error("Propiedad invalida [{}]: {}", propertyName, message);
            return message;
        }

        return null;
    }

    private String validateRequiredEmailList(List<String> emails, String propertyName, String emptyMessage) {
        if (emails == null || emails.isEmpty()) {
            log.error("Propiedad invalida [{}]: {}", propertyName, emptyMessage);
            return emptyMessage;
        }

        for (String email : emails) {
            if (email == null || email.trim().isEmpty()) {
                String message = "La propiedad " + propertyName + " contiene un correo vacio.";
                log.error("Propiedad invalida [{}]: {}", propertyName, message);
                return message;
            }

            if (!isValidEmail(email)) {
                String message = "El correo [" + email + "] en la propiedad " + propertyName + " no es valido.";
                log.error("Propiedad invalida [{}]: {}", propertyName, message);
                return message;
            }
        }

        return null;
    }

    private String validateOptionalEmailList(List<String> emails, String propertyName) {
        if (emails == null || emails.isEmpty()) {
            return null;
        }

        for (String email : emails) {
            if (email == null || email.trim().isEmpty()) {
                String message = "La propiedad " + propertyName + " contiene un correo vacio.";
                log.error("Propiedad invalida [{}]: {}", propertyName, message);
                return message;
            }

            if (!isValidEmail(email)) {
                String message = "El correo [" + email + "] en la propiedad " + propertyName + " no es valido.";
                log.error("Propiedad invalida [{}]: {}", propertyName, message);
                return message;
            }
        }

        return null;
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}
