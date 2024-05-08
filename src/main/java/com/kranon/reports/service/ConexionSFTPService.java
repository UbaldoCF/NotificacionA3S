package com.kranon.reports.service;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.ChannelSftp.LsEntry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ConexionSFTPService {

	private final Session<LsEntry> session;

	@Autowired
	public ConexionSFTPService(SessionFactory<LsEntry> sessionFactory) {
		this.session = sessionFactory.getSession();
	}

	public boolean isConnected() {
		return session.isOpen();
	}

	public boolean checkFileExists(String directoryPath, String filename) {
		log.info("--------------------------------El pathsftp es {} -------------------------------", directoryPath);
		try {
			LsEntry[] files = session.list(directoryPath);
			for (LsEntry file : files) {
				if (file.getFilename().equals(filename)) {
					log.info("El archivo {} existe en el servidor", filename);
					return true;
				}
			}
			log.info("El archivo {} no existe en el servidor", filename);
			return false;
		} catch (Exception e) {
			log.error("Ocurrió un error al verificar la existencia del archivo: {}", e.getMessage());
			return false;
		}
	}

	// Método para cerrar la sesión al finalizar la aplicación
	@PreDestroy
	public void closeSession() {
		if (session != null && session.isOpen()) {
			session.close();
			log.info("Sesión SFTP cerrada correctamente al finalizar la aplicación");
		}
	}

}
