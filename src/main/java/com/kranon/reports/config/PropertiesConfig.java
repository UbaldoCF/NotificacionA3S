package com.kranon.reports.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Configuration
@ConfigurationProperties(prefix = "reportproperties")
public class PropertiesConfig {

	private String company;
	private String hostSFTP;
	private String portSFTP;
	private String userSFTP;
	private String passwordSFTP;
	
	private String pathOutboundSFTP;
	private String pathInbuoundSFTP;
	private String pathAgenteSFTP;
	private String pathRedesSFTP;
	
	private String pathLocalOutboundSFTP;
	private String pathLocalInbuoundSFTP;
	private String pathLocalAgenteSFTP;
	private String pathLocalRedesSFTP;

	private List< String> destinationEmailCopyReport;
	private List< String> emailDestinationReport;
	private List< String> destinationEmailCopyHideReport;
	
	private boolean validarSFTP;
	private boolean uploadSFTP;
			
	private String vsPathCredencialesAuth;

	private boolean A3SActivateBat;
	private String A3SConfigName;
	private List<String> A3SReadPaths;
	

}
