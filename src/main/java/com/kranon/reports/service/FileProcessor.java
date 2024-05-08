package com.kranon.reports.service;

import java.io.File;
import java.io.FileFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.kranon.reports.dto.ReportDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Author: Ubaldo Cortez Franco Date: 25/04/2024 Description: Algoritmo de
 * procesamiento de directorios
 */

@Slf4j
public class FileProcessor {

	public final String PATH_ESTADOS_AGENTE = "D:\\appl\\A3S\\estados_agente\\";
	public final String PATH_INBOUND = "D:\\appl\\A3S\\inbound\\";
	public final String PATH_OUTBOUND = "D:\\appl\\A3S\\outbound\\";
	public final String PATH_REDES_SOCIALES = "D:\\appl\\A3S\\redes_sociales\\";

	private final String FOLDER_PROGRAM_FILES = "program";
	private final String FOLDER_PROGRAM_FILES_24_HRS = "program_24hrs";
	private final String FOLDER_PROGRAM_FILES_48_HRS = "program_48hrs";
	private final String FOLDER_PROGRAM_FILES_72_HRS = "program_72hrs";

	private final String PATH_SFTP_ESTADOS_AGENTE = "/ESTADOS_AGENTES/A3S/";
	private final String PATH_SFTP_INBOUND = "/INBOUND/A3S/";
	private final String PATH_SFTP_OUNBOUND = "/OUTBOUND/A3S/";
	private final String PATH_SFTP_REDES = "/SOCIAL/A3S/";

	public static final String AGENT_STATUS_REPORT = "reportesdetalles";
	public static final String INBOUND_REPORT = "inbound";
	public static final String OUTBOUND_REPORT = "outbound";
	public static final String SOCIAL_NETWORKS_REPORT = "sociales";

	@Autowired
	private ConexionSFTPService sftpService;

	List<File> listOutbound = new ArrayList<>();
	List<ReportDTO> listOutboundReport = new ArrayList<>();

	private String report = null;

	public List<ReportDTO> newfactoryFiles(String typeReport) {

		switch (typeReport) {
			case OUTBOUND_REPORT:
				report = OUTBOUND_REPORT;
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				log.info("------------------Reporte outbound ---------------------");
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				return processFiles(PATH_OUTBOUND);
			case INBOUND_REPORT:
				report = INBOUND_REPORT;

				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				log.info("------------------Reporte inbound ---------------------");
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				return processFiles(PATH_INBOUND);

			case AGENT_STATUS_REPORT:
				report = AGENT_STATUS_REPORT;

				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				log.info("------------------Reporte agente ---------------------");
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				return processFiles(PATH_ESTADOS_AGENTE);

			case SOCIAL_NETWORKS_REPORT:
				report = SOCIAL_NETWORKS_REPORT;

				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				log.info("------------------Reporte sociales ---------------------");
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				return processFiles(PATH_REDES_SOCIALES);
		}
		return null;

	}

	private List<ReportDTO> processFiles(String path) {
		List<String> listFolder = createListFolder();

		List<ReportDTO> listReportes = new ArrayList<>();

		Date date = new Date();
		log.info("Fecha actual: {}", date);
		File file = null;
		for (int i = 0; i < listFolder.size(); i++) {
			System.out.println();
			log.info("------------------Ruta: {} ---------------------", path + "" + listFolder.get(i));
			log.info("------------------Carpeta: {} ---------------------", listFolder.get(i));

			System.out.println();

			file = new File(path + "" + listFolder.get(i));

			listReportes.add(createReport(listFolder.get(i), file, date));

		}

		if (report.equals(OUTBOUND_REPORT)) {
			addOutboundReport();
			return listOutboundReport;
		} else {
			return listReportes;

		}

	}

	private void addOutboundReport() {
		Map<String, Integer> folderCount = new LinkedHashMap<>();

		// Agregar las carpetas con un contador inicial de 0
		folderCount.put("program", 0);
		folderCount.put("program_24hrs", 0);
		folderCount.put("program_48hrs", 0);
		folderCount.put("program_72hrs", 0);

		for (ReportDTO f : listOutboundReport) {
			String folderName = f.getNameFolder();
			if (folderCount.containsKey(folderName)) {
				folderCount.put(folderName, folderCount.get(folderName) + 1);
			}
		}

		for (Map.Entry<String, Integer> entry : folderCount.entrySet()) {
			String folderName = entry.getKey();
			int count = entry.getValue();
			switch (folderName) {
				case "program":
					if (count == 0) {
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
					}

					if (count == 1) {
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
					}
					break;
				case "program_24hrs":
					if (count == 0) {
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
					}

					if (count == 1) {
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
					}
					break;
				case "program_48hrs":
					if (count == 0) {
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
					}

					if (count == 1) {
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
					}
					break;
				case "program_72hrs":
					if (count == 0) {
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
					}

					if (count == 1) {
						listOutboundReport.add(new ReportDTO(folderName, "No generado", false, 0.0, false));
					}
					break;
				default:
					break;
			}
		}
	}

	private ReportDTO createReport(String folder, File pathFiles, Date date) {
		ReportDTO reportGenerate = new ReportDTO();
		List<ReportDTO> listReportes = new ArrayList<>();
		File fileGenerate = null;
		/* Filtro que solo buscara archivos .csv */
		FileFilter filtro = new FileFilter() {
			@Override
			public boolean accept(File arch) {
				return arch.isFile() && arch.getName().toLowerCase().endsWith(".csv");
				// return arch.isFile();
			}
		};

		File[] archivos = pathFiles.listFiles(filtro);
		if (archivos == null || archivos.length == 0) {
			log.info("No hay archivos disponibles");

			reportGenerate = generateObject(folder, null);

		} else {
			for (int i = 0; i < archivos.length; i++) {
				File archivo = archivos[i];

				if (archivo.getName().contains("reportesdetalles")) {
					log.info("Arhivos estadosagente");
					log.info("Nombre del archivo encontrado: {}", archivo.getName());
					fileGenerate = validDateFile(folder, date, archivo);
					reportGenerate = generateObject(folder, fileGenerate);
				}

				if (archivo.getName().contains("inbound")) {
					log.info("Arhivos inbound");
					log.info("Nombre del archivo encontrado: {}", archivo.getName());
					fileGenerate = validDateFile(folder, date, archivo);
					reportGenerate = generateObject(folder, fileGenerate);
				}

				if (archivo.getName().contains("outbound")) {
					log.info("Arhivos outbound");
					log.info("Nombre del archivo encontrado: {}", archivo.getName());
					fileGenerate = validDateFile(folder, date, archivo);
					generateObject(folder, fileGenerate);

				}

				if (archivo.getName().contains("social")) {
					log.info("Arhivos redes sociales");
					log.info("Nombre del archivo encontrado: {}", archivo.getName());
					fileGenerate = validDateFile(folder, date, archivo);
					reportGenerate = generateObject(folder, fileGenerate);
				}

			}
		}
		return reportGenerate;
	}

	private ReportDTO generateObject(String folder, File fileGenerate) {
		log.info("Genrando el reporte en: {} ", folder);
		ReportDTO reportGenerate = new ReportDTO();
		reportGenerate.setNameFolder(folder);

		String pathSftp = null;

		if (this.report.equals(OUTBOUND_REPORT)) {
			pathSftp = PATH_SFTP_OUNBOUND;
		}

		if (this.report.equals(SOCIAL_NETWORKS_REPORT)) {
			pathSftp = PATH_SFTP_REDES;
		}

		if (this.report.equals(INBOUND_REPORT)) {
			pathSftp = PATH_SFTP_INBOUND;
		}

		if (this.report.equals(AGENT_STATUS_REPORT)) {
			pathSftp = PATH_SFTP_ESTADOS_AGENTE;
		}

		if (fileGenerate != null) {
			log.info("Nombre archivo a buscar en el sftp: {}", fileGenerate.getName());
			log.info("Ruta a verificar del sftp: {}", pathSftp);
			reportGenerate.setNameReport(fileGenerate.getName());
			reportGenerate.setGenerate(!fileGenerate.getName().isEmpty());
			long fileSizeBytes = fileGenerate.length();
			double fileSizeMegabytes = (double) fileSizeBytes / (1024 * 1024);

			reportGenerate.setSize(redondear(fileSizeMegabytes, 2));

			if (sftpService.isConnected()) {

				reportGenerate.setSftp(sftpService.checkFileExists(pathSftp, fileGenerate.getName()));
			} else {
				reportGenerate.setSftp(false);
			}

			if (this.report.equals(OUTBOUND_REPORT)) {
				listOutboundReport.add(reportGenerate);
			}

		} else {
			reportGenerate.setNameReport("No generado");

		}

		return reportGenerate;
	}

	private File validDateFile(String folder, Date fechaActual, File file) {
		Date medianocheActual = getMedianoche(fechaActual);
		log.info("Medianoche del día actual: {}", medianocheActual);

		switch (folder) {
			case FOLDER_PROGRAM_FILES:
				log.info("Validamos fecha de creación del archivo en: {}", FOLDER_PROGRAM_FILES);
				if (isWithinTimeRange(file, medianocheActual, 0, 0)) {
					log.info("El archivo {} fue creado hoy en el folder: {}", file.getName(), folder);
					log.info("--------------------------------------------------------------------------");
					return file;
				}
				break;
			case FOLDER_PROGRAM_FILES_24_HRS:
				log.info("Validamos creación de archivos en las últimas 24 horas en: {}", FOLDER_PROGRAM_FILES_24_HRS);
				if (isWithinTimeRange(file, medianocheActual, -24, 0)) {
					log.info("El archivo {} fue creado en las últimas 24 horas en el folder: {}", file.getName(),
							folder);
					log.info("--------------------------------------------------------------------------");
					return file;
				}
				break;
			case FOLDER_PROGRAM_FILES_48_HRS:
				log.info("Validamos creación de archivos en las últimas 48 horas en: {}", FOLDER_PROGRAM_FILES_48_HRS);
				if (isWithinTimeRange(file, medianocheActual, -48, 0)) {
					log.info("El archivo {} fue creado en las últimas 48 horas en el folder: {}", file.getName(),
							folder);
					log.info("--------------------------------------------------------------------------");
					return file;
				}
				break;
			case FOLDER_PROGRAM_FILES_72_HRS:
				log.info("Validamos creación de archivos en las últimas 72 horas en: {}", FOLDER_PROGRAM_FILES_72_HRS);
				if (isWithinTimeRange(file, medianocheActual, -72, 0)) {
					log.info("El archivo {} fue creado en las últimas 72 horas en el folder: {}", file.getName(),
							folder);
					log.info("--------------------------------------------------------------------------");
					return file;
				}
				break;
		}

		return null;
	}

	private boolean isWithinTimeRange(File file, Date medianoche, int horasAntes, int horasDespues) {
		Date fechaCreacion = new Date(file.lastModified());
		Calendar calArchivo = Calendar.getInstance();
		calArchivo.setTime(fechaCreacion);
		calArchivo.add(Calendar.HOUR_OF_DAY, horasDespues);

		Calendar calInicioRango = Calendar.getInstance();
		calInicioRango.setTime(medianoche);
		calInicioRango.add(Calendar.HOUR_OF_DAY, horasAntes);

		// Convertir los objetos Calendar a Date
		Date fechaArchivo = calArchivo.getTime();
		Date fechaInicioRango = calInicioRango.getTime();

		// Comparar las fechas como objetos Date
		return !fechaCreacion.before(fechaInicioRango) && !fechaCreacion.after(fechaArchivo);
	}

	private Date getMedianoche(Date fecha) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(fecha);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	private ArrayList<String> createListFolder() {
		ArrayList<String> list = new ArrayList<String>();
		list.add(FOLDER_PROGRAM_FILES);
		list.add(FOLDER_PROGRAM_FILES_24_HRS);
		list.add(FOLDER_PROGRAM_FILES_48_HRS);
		list.add(FOLDER_PROGRAM_FILES_72_HRS);
		return list;

	}

	public double redondear(double Num, int Decimales) {
		int aux = 1;
		for (int i = 0; i < Decimales; i++) {
			aux = aux * 10;
		}
		Num = Num * aux;
		Num = (double) Math.round(Num);
		Num = Num / aux;
		return (Num);
	}

}
