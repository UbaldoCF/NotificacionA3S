package com.kranon.reports.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.stereotype.Service;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.kranon.reports.config.PropertiesConfig;
import com.kranon.reports.serviceImp.ConexionSFTPServiceImp;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConexionSFTPService implements ConexionSFTPServiceImp {

	@Setter(onMethod = @__(@Autowired))
	private PropertiesConfig voModel;

	@Override
	public SftpSession sessionFactory() {
		try {
		
				log.info("Conexion por SFTP habilitada");
				DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
				factory.setHost(voModel.getHostSFTP());
				factory.setPort(Integer.parseInt(voModel.getPortSFTP()));
				factory.setUser(voModel.getUserSFTP());
				factory.setPassword(voModel.getPasswordSFTP());
				factory.setAllowUnknownKeys(true);
				log.info("Exito generando conexion por SFTP [conexionSFTP] ");
				return factory.getSession();

		} catch (Exception e) {
			log.error("Error SFTP [conexionSFTP] : " + e.getMessage());
			return null;
		}

	}

	@Override
	public boolean checkFileExists(String directoryPath, String fileName, Session<LsEntry> session) {
		try {
			LsEntry[] files = session.list(directoryPath);
			for (LsEntry file : files) {
				if (file.getFilename().equals(fileName)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			log.error("Error verificando archivo en SFTP: " + e.getMessage());
			return false;
		}
	}

	// Método para cerrar la sesion al finalizar la app
	@Override
	public void closeSession(Session<LsEntry> sessionFactory) {
		if (sessionFactory != null && sessionFactory.isOpen()) {
			sessionFactory.close();
			log.info("Sesion SFTP cerrada correctamente al finalizar la aplicacion");
		}
	}

	@Override
	public boolean uploadFile(File fileToSend, String remotePath, Session<LsEntry> session) {
		try (InputStream inputStream = new FileInputStream(fileToSend)) {
			session.write(inputStream, remotePath + "/" + fileToSend.getName());
			log.info("Archivo [" + fileToSend.getName() + "] enviado correctamente a: " + remotePath);
			return true;
		} catch (Exception e) {
			log.error("Error al subir archivo al SFTP: " + e.getMessage());
			return false;
		}
	}

}
