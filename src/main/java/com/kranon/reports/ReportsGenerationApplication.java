package com.kranon.reports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.kranon.reports.service.FileProcessorService;

/**
 * Author: Ubaldo Cortez Franco Date: 25/04/2024 Description: Creamos un bean de
 * FileProcessor configurable para poder leer los directorios y ejecutarlo en
 * Spring Boot y ejecutamos el bean de EmailService pasandole como parametros
 * una plantilla de Thymeleaf, el correo destino y los reportes generados que se
 * usaran en la plantilla de esa manera hacemos la construccion del reporte que
 * se enviara por correo
 */

@SpringBootApplication
public class ReportsGenerationApplication  {

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(ReportsGenerationApplication.class, args);
		FileProcessorService voService = context.getBean(FileProcessorService.class);
		voService.generarNotificacionLaurete();

	}
}
