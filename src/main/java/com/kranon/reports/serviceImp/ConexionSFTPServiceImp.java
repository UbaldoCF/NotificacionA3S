package com.kranon.reports.serviceImp;

import java.io.File;

import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.sftp.session.SftpSession;

import com.jcraft.jsch.ChannelSftp.LsEntry;

public interface ConexionSFTPServiceImp {

	public SftpSession sessionFactory();
	
	public boolean checkFileExists(String directoryPath, String fileName, Session<LsEntry> session) ;
	
	public void closeSession(Session<LsEntry> sessionFactory);
	
	public boolean uploadFile(File fileToSend, String remotePath, Session<LsEntry> session);
	
}
