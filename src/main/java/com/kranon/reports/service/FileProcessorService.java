package com.kranon.reports.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.SftpSession;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.kranon.reports.config.PropertiesConfig;
import com.kranon.reports.dto.ReportDTO;
import com.kranon.reports.serviceImp.FileProcessorServiceImp;
import com.kranon.reports.serviceImp.ReportBatchServiceImp;
import com.kranon.reports.utils.Utils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Author: Ubaldo Cortez Franco Date: 05/02/2025 Description: Algoritmo de
 * procesamiento de directorios Version 2
 */
@Service
@Slf4j
public class FileProcessorService implements FileProcessorServiceImp {

	private Date voDate = null;

	@Setter(onMethod = @__(@Autowired))
	private PropertiesConfig voModel;

	@Autowired
	ReportBatchServiceImp reportBatchServiceImp;

	@Autowired
	private ConexionSFTPService conexionSFTPService;

	@Autowired
	private SendEmailOAuthGmailService voAuthGmailV2;

	@Autowired
	private SpringTemplateEngine voTemplateEngine;

	private static final String FOLDER_PROGRAM_FILES = "program";
	private static final String FOLDER_PROGRAM_FILES_24_HRS = "program_24hrs";
	private static final String FOLDER_PROGRAM_FILES_48_HRS = "program_48hrs";
	private static final String FOLDER_PROGRAM_FILES_72_HRS = "program_72hrs";

	private static final String CLAVE_ESTADOS_AGENTE = "reportesdetalles";
	private static final String CLAVE_INBOUND = "inbound";
	private static final String CLAVE_OUTBOUND = "outbound";
	private static final String CLAVE_REDES_SOCIALES = "social";

	private static final String VALIDA_CONEXION_SFTP = "validaConexionSFTP";

	private static final String CURRENT_HRS = "hrs";
	private static final String EMAIL_TEMPLATE = "emailtemplate";

	private SftpSession voSessionSFTP;

	@Override
	public void generarNotificacionLaurete() {

		voDate = new Date();

		try {
			// Generar sesion cuando la variable este activa
			if (voModel.isValidarSFTP()) {
				log.info("SESION DEL SFTP ACTIVA");
				try {
					voSessionSFTP = conexionSFTPService.sessionFactory();
					log.info("SFTP SESSION {}", (voSessionSFTP != null ? "OK" : "NULL"));
				} catch (Exception ex) {
					log.error("SFTP SESSION FAIL msg={}", ex.getMessage());
					voSessionSFTP = null;
				}
			} else {
				log.info("SESION DEL SFTP INACTIVA");
				voSessionSFTP = null;
			}

			log.info("[------------Lectura de outbound------------]");
			List<ReportDTO> outbound = safeProcesFile(voModel.getPathLocalOutboundSFTP(), voModel.getPathOutboundSFTP(),
					CLAVE_OUTBOUND);

			// /OUTBOUND/A3S/ | D:\\appl\\A3S\\outbound\\ | inbound

			log.info("[------------Lectura de inbound------------]");
			List<ReportDTO> inbound = safeProcesFile(voModel.getPathLocalInbuoundSFTP(), voModel.getPathInbuoundSFTP(),
					CLAVE_INBOUND);

			log.info("[------------Lectura de sociales------------]");
			List<ReportDTO> social = safeProcesFile(voModel.getPathLocalRedesSFTP(), voModel.getPathRedesSFTP(),
					CLAVE_REDES_SOCIALES);
			log.info("[------------Lectura de estadosagente------------]");
			List<ReportDTO> estadosAgente = safeProcesFile(voModel.getPathLocalAgenteSFTP(),
					voModel.getPathAgenteSFTP(), CLAVE_ESTADOS_AGENTE);

			inbound = validateAndCompleteList(inbound, false);
			outbound = validateAndCompleteList(outbound, true);
			social = validateAndCompleteList(social, false);
			estadosAgente = validateAndCompleteList(estadosAgente, false);

			Context voTemplateContext = new Context();
			voTemplateContext.setVariable(CLAVE_ESTADOS_AGENTE, estadosAgente);
			voTemplateContext.setVariable(CLAVE_INBOUND, inbound);
			voTemplateContext.setVariable(CLAVE_OUTBOUND, outbound);
			voTemplateContext.setVariable(CLAVE_REDES_SOCIALES, social);
			voTemplateContext.setVariable(CURRENT_HRS, Utils.extraerHoras());
			voTemplateContext.setVariable(VALIDA_CONEXION_SFTP, voSessionSFTP != null);

			String vsHtml;
			try {
				vsHtml = voTemplateEngine.process(EMAIL_TEMPLATE, voTemplateContext);
			} catch (Exception ex) {
				log.error("TEMPLATE FAIL msg={}", ex.getMessage());
				vsHtml = "<html><body><h3>ERROR TEMPLATE</h3><p>Revisar logs</p></body></html>";
			}

			String vsSubject = "[" + voModel.getCompany() + "] - " + Utils.dateReport() + " Revision Reportes A3S";

			boolean vbSend = false;
			try {
				vbSend = voAuthGmailV2.sendEmail(voModel.getEmailDestinationReport(),
						voModel.getDestinationEmailCopyReport(), voModel.getDestinationEmailCopyHideReport(),
						voModel.getVsPathCredencialesAuth(), vsSubject, vsHtml, null);
			} catch (Exception ex) {
				log.error("EMAIL FAIL msg={}", ex.getMessage());
				vbSend = false;
			}

			log.info("EMAIL STATUS={}", vbSend ? "OK" : "FAIL");

		} catch (Exception ex) {
			// Nunca debe morir el proceso por una excepcion aqui
			log.error("FATAL CONTROLADO msg={}", ex.getMessage(), ex);
		} finally {
			try {
				if (voSessionSFTP != null) {
					log.info("SFTP CLOSE");
					conexionSFTPService.closeSession(voSessionSFTP);
				}
			} catch (Exception ex) {
				log.warn("SFTP CLOSE FAIL msg={}", ex.getMessage());
			}

			System.exit(0);
		}

	}

	// Aqui ajustas a tu formato real

	public String buildExpectedName(LocalDate expectedDate, String vsFolder, String vsClave, boolean vbManual) {

		// Fecha en formato correcto
		// Se requiere: YYYY_MM-DD
		// Ejemplo: 2026_01-05
		String vsDate = expectedDate.toString(); // 2026-01-05
		String vsDateFixed = vsDate.substring(0, 4) + "_" + vsDate.substring(5, 7) + "-" + vsDate.substring(8, 10);

		String vsCompany = (voModel.getCompany() != null && !voModel.getCompany().trim().isEmpty())
				? voModel.getCompany().trim()
				: "COMPANY";

		String vsSuffixManual = vbManual ? "_Manual" : "";

		// Regla folder program
		// No lleva company
		// No lleva 24H 48H 72H
		// Ejemplo: 2026_01-05_outbound.csv
		if (FOLDER_PROGRAM_FILES.equalsIgnoreCase(vsFolder)) {
			return vsDateFixed + "_" + vsClave + ".csv";
		}

		// Regla para folders con horas
		// Ejemplo: 2026_01-04_24H_outbound_UNITEC.csv
		// Ejemplo: 2026_01-04_24H_outbound_UNITEC_Manual.csv
		String vsPrefixHrs = null;

		if (FOLDER_PROGRAM_FILES_24_HRS.equalsIgnoreCase(vsFolder)) {
			vsPrefixHrs = "24H";
		} else if (FOLDER_PROGRAM_FILES_48_HRS.equalsIgnoreCase(vsFolder)) {
			vsPrefixHrs = "48H";
		} else if (FOLDER_PROGRAM_FILES_72_HRS.equalsIgnoreCase(vsFolder)) {
			vsPrefixHrs = "72H";
		} else {
			// Fallback por si llega algo raro
			vsPrefixHrs = "XXH";
		}

		return vsDateFixed + "_" + vsPrefixHrs + "_" + vsClave + "_" + vsCompany + vsSuffixManual + ".csv";
	}

	// Wrapper para que procesFile nunca mate el flujo
	private List<ReportDTO> safeProcesFile(String vsPathLocal, String vsPathSftp, String vsClave) {
		try {
			return procesFile(vsPathLocal, vsPathSftp, vsClave);
		} catch (Exception ex) {
			log.error("PROCESFILE FAIL clave={} msg={}", vsClave, ex.getMessage(), ex);

			// Regresar lista con 4 folders pero todo en cero
			List<ReportDTO> vl = new ArrayList<>();
			for (String f : createListFolder()) {
				vl.add(buildZeroReport(f, "ERROR PROCESO " + vsClave));
			}
			return vl;
		}
	}

	// Me ordena la lista de reportes para el envio de informacion en la
	// notificacion
	private List<ReportDTO> validateAndCompleteList(List<ReportDTO> vlListReports, boolean vbSpecialList) {

		List<String> vlOrderedFolders = createListFolder();

		Map<String, List<ReportDTO>> voMapGrupoFolders = vlListReports.stream()
				.collect(Collectors.groupingBy(ReportDTO::getNameFolder));

		List<ReportDTO> vlListaOrdenada = new ArrayList<>();

		for (String vsFolder : vlOrderedFolders) {

			List<ReportDTO> vlReportes = voMapGrupoFolders.getOrDefault(vsFolder, new ArrayList<>());

			int viEsperados = vbSpecialList ? 2 : 1;

			while (vlReportes.size() < viEsperados) {
				vlReportes.add(new ReportDTO(vsFolder, "NO GENERADO", false, 0.0, false));
			}

			if (vlReportes.size() > viEsperados) {
				vlReportes = vlReportes.subList(0, viEsperados);
			}

			vlListaOrdenada.addAll(vlReportes);
		}

		return vlListaOrdenada;
	}

	private void generarReporteUnificado(String totalGenesys, String totalLineasRepote, String vsPathLocal,
			LocalDate expectedDate, String vsFolder) {

		for (String vsClaveEsperada : voModel.getA3SReadPaths()) {

			log.info("--------------Inicio de unificacion de reportes--------------");
			log.info("Lectura activada para: {}", vsClaveEsperada);
		
			reportBatchServiceImp.runReportGeneration(vsPathLocal, vsClaveEsperada, expectedDate, vsFolder);

		}
	}

	// Lee folders y selecciona reportes
	private List<ReportDTO> procesFile(String vsPathLocal, String vsPathSftp, String vsClaveEsperada) {

		log.info("SCAN clave={} local={} sftp={}", vsClaveEsperada, vsPathLocal, vsPathSftp);

		List<ReportDTO> vlListReport = new ArrayList<>();
		boolean vbOutbound = CLAVE_OUTBOUND.equalsIgnoreCase(vsClaveEsperada);

		for (String vsFolder : createListFolder()) {

			LocalDate expectedDate = resolveExpectedDateByFolder(vsFolder, voDate);

			if (vbOutbound) {
				// Outbound normal
				vlListReport.add(procesOne(vsPathLocal, vsPathSftp, vsFolder, expectedDate, vsClaveEsperada, false));
				// Outbound Manual
				vlListReport.add(procesOne(vsPathLocal, vsPathSftp, vsFolder, expectedDate, vsClaveEsperada, true));
			} else {
				vlListReport.add(procesOne(vsPathLocal, vsPathSftp, vsFolder, expectedDate, vsClaveEsperada, false));
			}
		}

		return vlListReport;
	}

	private ReportDTO procesOne(String vsPathLocal, String vsPathSftp, String vsFolder, LocalDate expectedDate,
			String vsClave, boolean vbManual) {

		String tipo = vbManual ? "Manual" : "Normal";
		log.info("FOLDER {} clave={} tipo={} date={}", vsFolder, vsClave, tipo, expectedDate);

		try {

			File dir = new File(vsPathLocal + vsFolder);

			if (!dir.exists() || !dir.isDirectory()) {
				log.warn("NO DIR {}", dir.getAbsolutePath());
				return buildZeroReport(vsFolder, "NO DIR " + tipo);
			}

			File[] files = dir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".csv"));

			if (files == null || files.length == 0) {
				log.warn("NO CSV folder={} tipo={}", vsFolder, tipo);
				return handleNoLocalTrySftp(vsPathSftp, vsFolder, expectedDate, vsClave, vbManual);
			}

			// Buscar el mejor archivo local
			File best = null;
			long bestLast = -1;

			for (File f : files) {

				String name = f.getName();

				if (!name.contains(vsClave)) {
					continue;
				}

				boolean hasManual = name.contains("Manual");

				if (vbManual && !hasManual) {
					continue;
				}
				if (!vbManual && hasManual) {
					continue;
				}

				// Regla principal
				// debe coincidir fecha en nombre con la esperada
				LocalDate dateInName = extractDateFromFilename(name);
				if (dateInName == null) {
					continue;
				}
				if (!expectedDate.equals(dateInName)) {
					continue;
				}

				if (f.lastModified() > bestLast) {
					best = f;
					bestLast = f.lastModified();
				}
			}

			if (best == null) {
				log.warn("NO MATCH local folder={} tipo={} date={}", vsFolder, tipo, expectedDate);
				return handleNoLocalTrySftp(vsPathSftp, vsFolder, expectedDate, vsClave, vbManual);
			}

			log.info("LOCAL OK folder={} tipo={} file={}", vsFolder, tipo, best.getName());

			return generateObject(vsFolder, best, vsPathSftp);

		} catch (Exception ex) {
			log.error("FOLDER FAIL folder={} tipo={} msg={}", vsFolder, tipo, ex.getMessage(), ex);
			return buildZeroReport(vsFolder, "ERROR FOLDER " + tipo);
		}
	}

	// Si no hay local o no hizo match entonces probar con nombre esperado en SFTP
	private ReportDTO handleNoLocalTrySftp(String vsPathSftp, String vsFolder, LocalDate expectedDate, String vsClave,
			boolean vbManual) {

		String tipo = vbManual ? "Manual" : "Normal";

		if (voSessionSFTP == null || !voSessionSFTP.isOpen()) {
			return buildZeroReport(vsFolder, "NO LOCAL SFTP" + tipo);
		}

		// Nombre esperado

		String expectedName = buildExpectedName(expectedDate, vsFolder, vsClave, vbManual);

		boolean vbExists = false;

		if (voSessionSFTP != null) {
			try {
				vbExists = conexionSFTPService.checkFileExists(vsPathSftp, expectedName, voSessionSFTP);
			} catch (Exception ex) {
				log.warn("SFTP CHECK FAIL folder={} tipo={} msg={}", vsFolder, tipo, ex.getMessage());
				vbExists = false;
			}
		}

		if (vbExists) {
			log.warn("NO LOCAL EN SFTP OK folder={} tipo={} file={}", vsFolder, tipo, expectedName);
			return buildZeroReport(vsFolder, "NO LOCAL EN SFTP OK " + expectedName);
		}

		log.warn("NO LOCAL NO SFTP folder={} tipo={} file={}", vsFolder, tipo, expectedName);
		return buildZeroReport(vsFolder, "NO LOCAL NO SFTP " + expectedName);
	}

	private ReportDTO buildZeroReport(String vsFolder, String vsMsg) {

		ReportDTO voReport = new ReportDTO();
		voReport.setNameFolder(vsFolder);
		voReport.setNameReport(vsMsg);

		voReport.setSize(0.0);
		voReport.setVsNumLienas(0);
		voReport.setGenerate(false);

		boolean vbSftpOk = voSessionSFTP != null && voSessionSFTP.isOpen();
		voReport.setSftp(vbSftpOk);
		voReport.setValidaSubidaSFTP(false);

		return voReport;
	}

	private LocalDate resolveExpectedDateByFolder(String vsFolder, Date voDateActual) {

		LocalDate base = voDateActual.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		int daysBack = 1;

		if (FOLDER_PROGRAM_FILES_24_HRS.equalsIgnoreCase(vsFolder))
			daysBack = 2;
		else if (FOLDER_PROGRAM_FILES_48_HRS.equalsIgnoreCase(vsFolder))
			daysBack = 3;
		else if (FOLDER_PROGRAM_FILES_72_HRS.equalsIgnoreCase(vsFolder))
			daysBack = 4;

		return base.minusDays(daysBack);
	}

	private LocalDate extractDateFromFilename(String vsName) {

		if (vsName == null)
			return null;

		Matcher m1 = Pattern.compile("(\\d{4})_(\\d{2})-(\\d{2})").matcher(vsName);
		if (m1.find())
			return parseDateParts(m1.group(1), m1.group(2), m1.group(3));

		Matcher m2 = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})").matcher(vsName);
		if (m2.find())
			return parseDateParts(m2.group(1), m2.group(2), m2.group(3));

		Matcher m3 = Pattern.compile("(\\d{4})_(\\d{2})_(\\d{2})").matcher(vsName);
		if (m3.find())
			return parseDateParts(m3.group(1), m3.group(2), m3.group(3));

		return null;
	}

	private LocalDate parseDateParts(String y, String m, String d) {
		try {
			return LocalDate.of(Integer.parseInt(y), Integer.parseInt(m), Integer.parseInt(d));
		} catch (Exception ex) {
			return null;
		}
	}

	private ReportDTO generateObject(String vsFolder, File voFileGenerate, String vsPathSFTP) {

		ReportDTO voReportGenerate = new ReportDTO();

		// Tamaño de archivo
		double vdSizeFile = 0.0;
		// Numero de lineas
		long vlCounterLine = 0;

		boolean vbArchivoGenerado = false;
		boolean vbGenerateSFTP = false;
		boolean vbSubidaSFTP = false;

		try {

			if (voFileGenerate != null && voFileGenerate.exists() && voFileGenerate.isFile()) {

				vdSizeFile = Utils.calculateMegabytesFile(voFileGenerate);

				if (vdSizeFile > 0) {
					try (BufferedReader voBr = new BufferedReader(new FileReader(voFileGenerate))) {
						String vsData = voBr.readLine();
						if (vsData != null) {
							vlCounterLine = Utils.CSVFileLineCounterApache(voFileGenerate);
							vbArchivoGenerado = true;
						}
					}
				}

				if (voSessionSFTP != null && voSessionSFTP.isOpen()) {

					vbGenerateSFTP = conexionSFTPService.checkFileExists(vsPathSFTP, voFileGenerate.getName(),
							voSessionSFTP);

					if (voModel.isUploadSFTP()) {
						if (vbArchivoGenerado && !vbGenerateSFTP) {

							int viIntento = 1;

							while (viIntento <= 3) {
								if (conexionSFTPService.uploadFile(voFileGenerate, vsPathSFTP, voSessionSFTP)) {
									vbGenerateSFTP = true;
									vbSubidaSFTP = true;
									break;
								}
								viIntento++;
							}
						}
					}
				}

			}

		} catch (Exception e) {
			log.error("generateObject fail msg={}", e.getMessage(), e);
		}

		voReportGenerate.setNameFolder(vsFolder);
		voReportGenerate.setNameReport(voFileGenerate != null ? voFileGenerate.getName() : "NO GENERADO");

		voReportGenerate.setSize(Utils.roundNumberDecimal(vdSizeFile, 2));
		voReportGenerate.setVsNumLienas(vlCounterLine);
		voReportGenerate.setGenerate(vbArchivoGenerado);
		voReportGenerate.setSftp(vbGenerateSFTP);
		voReportGenerate.setValidaSubidaSFTP(vbSubidaSFTP);

		return voReportGenerate;
	}

	private ArrayList<String> createListFolder() {
		ArrayList<String> vlList = new ArrayList<>();
		vlList.add(FOLDER_PROGRAM_FILES);
		vlList.add(FOLDER_PROGRAM_FILES_24_HRS);
		vlList.add(FOLDER_PROGRAM_FILES_48_HRS);
		vlList.add(FOLDER_PROGRAM_FILES_72_HRS);
		return vlList;
	}
}
