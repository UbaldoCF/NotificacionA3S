package com.kranon.reports.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kranon.reports.config.PropertiesConfig;
import com.kranon.reports.serviceImp.ChangeFileServiceImp;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChangeFileService implements ChangeFileServiceImp {

	@Setter(onMethod = @__(@Autowired))
	private PropertiesConfig voModel;



	@Override
	public boolean changePropertiesFile(String rutaArchivo, String clave, String nuevoValor) {
	    try {
	        log.info("Inicio de modificacion del archivo: {}", rutaArchivo);
	        log.info("Propiedad: {}", clave);
	        log.info("Valor: {}", nuevoValor);

	        Path path = Path.of(rutaArchivo);
	        Path backup = Path.of(rutaArchivo + ".bak");

	        if (!Files.exists(backup)) {
	            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
	            log.info("Backup generado: {}", backup);
	        }

	        String contenido = Files.readString(path, StandardCharsets.UTF_8);
	        String[] lineas = contenido.split("\\r?\\n", -1);

	        boolean encontrada = false;

	        for (int i = 0; i < lineas.length; i++) {
	            String lineaTrim = lineas[i].trim();

	            if (lineaTrim.startsWith(clave + "=") || lineaTrim.startsWith(clave + " =")) {
	                int idx = lineas[i].indexOf('=');
	                if (idx != -1) {
	                    String izquierda = lineas[i].substring(0, idx).trim();
	                    if (izquierda.equals(clave)) {
	                        lineas[i] = izquierda + "=" + nuevoValor;
	                        encontrada = true;
	                        break;
	                    }
	                }
	            }
	        }

	        if (!encontrada) {
	            log.error("No se encontro la propiedad: {}", clave);
	            return false;
	        }

	        String nuevoContenido = String.join(System.lineSeparator(), lineas);
	        Files.writeString(path, nuevoContenido, StandardCharsets.UTF_8);

	        log.info("Propiedad actualizada con exito");
	        return true;

	    } catch (Exception e) {
	        log.error("[ChangePropertiesFile] No se actualizo el valor: {}", e.getMessage(), e);
	        return false;
	    }
	}
	
	
	
	@Override
	public boolean commentProperties(String rutaArchivo, List<String> propiedades) {
	    try {
	        log.info("Inicio de comentario de propiedades en archivo: {}", rutaArchivo);

	        Path path = Path.of(rutaArchivo);
	        String contenido = Files.readString(path, StandardCharsets.UTF_8);
	        String[] lineas = contenido.split("\\r?\\n", -1);

	        for (int i = 0; i < lineas.length; i++) {
	            String lineaOriginal = lineas[i];
	            String lineaTrim = lineaOriginal.trim();

	            for (String propiedad : propiedades) {
	                if (lineaTrim.startsWith(propiedad + "=") || lineaTrim.startsWith(propiedad + " =")) {
	                    lineas[i] = "#" + lineaOriginal;
	                    log.info("Propiedad comentada: {}", propiedad);
	                    break;
	                }
	            }
	        }

	        String nuevoContenido = String.join(System.lineSeparator(), lineas);
	        Files.writeString(path, nuevoContenido, StandardCharsets.UTF_8);

	        log.info("Propiedades comentadas con exito");
	        return true;

	    } catch (Exception e) {
	        log.error("[commentProperties] Error al comentar propiedades: {}", e.getMessage(), e);
	        return false;
	    }
	}
	
	
	@Override
	public boolean uncommentProperties(String rutaArchivo, List<String> propiedades) {
	    try {
	        log.info("Inicio de descomentado de propiedades en archivo: {}", rutaArchivo);

	        Path path = Path.of(rutaArchivo);
	        String contenido = Files.readString(path, StandardCharsets.UTF_8);
	        String[] lineas = contenido.split("\\r?\\n", -1);

	        for (int i = 0; i < lineas.length; i++) {
	            String lineaOriginal = lineas[i];
	            String lineaTrim = lineaOriginal.trim();

	            if (lineaTrim.startsWith("#")) {
	                String sinComentario = lineaTrim.substring(1).trim();

	                for (String propiedad : propiedades) {
	                    if (sinComentario.startsWith(propiedad + "=") || sinComentario.startsWith(propiedad + " =")) {
	                        int idxHash = lineaOriginal.indexOf('#');
	                        if (idxHash != -1) {
	                            lineas[i] = lineaOriginal.substring(0, idxHash) + lineaOriginal.substring(idxHash + 1);
	                        }
	                        log.info("Propiedad descomentada: {}", propiedad);
	                        break;
	                    }
	                }
	            }
	        }

	        String nuevoContenido = String.join(System.lineSeparator(), lineas);
	        Files.writeString(path, nuevoContenido, StandardCharsets.UTF_8);

	        log.info("Propiedades descomentadas con exito");
	        return true;

	    } catch (Exception e) {
	        log.error("[uncommentProperties] Error al descomentar propiedades: {}", e.getMessage(), e);
	        return false;
	    }
	}
	
}
