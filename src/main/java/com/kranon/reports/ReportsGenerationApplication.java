package com.kranon.reports;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.thymeleaf.context.Context;

import com.kranon.reports.dto.ReportDTO;
import com.kranon.reports.service.ConexionSFTPService;
import com.kranon.reports.service.EmailService;
import com.kranon.reports.service.FileProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * Author: Ubaldo Cortez Franco Date: 25/04/2024 Description: Creamos un bean de
 * FileProcessor configurable para poder leer los directorios y ejecutarlo en
 * Spring Boot y ejecutamos el bean de EmailService pasandole como parametros
 * una plantilla de Thymeleaf, el correo destino y los reportes generados que se
 * usaran en la plantilla de esa manera hacemos la construccion del reporte que
 * se enviara por correo
 */

@Slf4j
@SpringBootApplication
@EnableAsync // Hablitamos la lectura de hilos
public class ReportsGenerationApplication {

	private static final String CURRENT_HRS = "hrs";

	private static final String DESTINATION_MAIL = "sistemas.automatizados@kranon.com";
	private static final String EMAIL_TEMPLATE = "emailtemplate";

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(ReportsGenerationApplication.class, args);

		FileProcessor fileProcessor = context.getBean(FileProcessor.class);

		List<ReportDTO> inbound = fileProcessor.newfactoryFiles(FileProcessor.INBOUND_REPORT);

		List<ReportDTO> outbound = fileProcessor.newfactoryFiles(FileProcessor.OUTBOUND_REPORT);

		List<ReportDTO> social = fileProcessor.newfactoryFiles(FileProcessor.SOCIAL_NETWORKS_REPORT);

		List<ReportDTO> estadosAgente = fileProcessor.newfactoryFiles(FileProcessor.AGENT_STATUS_REPORT);

		System.out.println();
		System.out.println();
		System.out.println();

		log.info("----- Reporte generado de estados agente ------");
		showReport(estadosAgente);
		log.info("----- Reporte generado de inbound -------");
		showReport(inbound);
		log.info("----- Reporte generado de outbound ------");
		showReport(outbound);
		log.info("----- Reporte generado de redes sociales ------");
		showReport(social);

		System.out.println();
		System.out.println();
		System.out.println();

		EmailService emailSender = context.getBean(EmailService.class);

		Context templateContext = new Context();

		templateContext.setVariable(FileProcessor.AGENT_STATUS_REPORT, estadosAgente);
		templateContext.setVariable(FileProcessor.INBOUND_REPORT, inbound);
		templateContext.setVariable(FileProcessor.OUTBOUND_REPORT, outbound);
		templateContext.setVariable(FileProcessor.SOCIAL_NETWORKS_REPORT, social);
		templateContext.setVariable(CURRENT_HRS, hrs());

		log.info("Fecha de ejecucion del programa: " + hrs());

		emailSender.sendEmail(DESTINATION_MAIL, EMAIL_TEMPLATE, templateContext);

	}

	public static void showReport(List<ReportDTO> list) {
		for (int i = 0; i < list.size(); i++) {
			log.info("{}", list.get(i));
		}
		System.out.println();
		System.out.println();
	}

	private static String hrs() {

		long tiempoActualMillis = System.currentTimeMillis();
		Date fechaHoraEjecucion = new Date(tiempoActualMillis);

		SimpleDateFormat formatoFechaHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String fechaHoraFormateada = formatoFechaHora.format(fechaHoraEjecucion);

		return fechaHoraFormateada;
	}

}
