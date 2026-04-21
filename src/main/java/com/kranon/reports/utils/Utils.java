package com.kranon.reports.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Utils {

	private static final String FORMAT_DATE = "yyyy-MM-dd";

	public static double redondear(double numero, int decimales) {
	    try {
	        if (decimales < 0) throw new IllegalArgumentException("Los decimales no pueden ser negativos");

	        BigDecimal bd = new BigDecimal(Double.toString(numero));
	        bd = bd.setScale(decimales, RoundingMode.HALF_UP);
	        return bd.doubleValue();
	    } catch (Exception e) {
	        System.err.println("Error al redondear numero: " + e.getMessage());
	        return 0.0;
	    }
	}
	
	public static boolean validarArchivoCreadoHoy(File voFile, Date voFechaActual) {
		Date voFechaCreacion = null;
		try {
			Path voPath = voFile.toPath();
			BasicFileAttributes voAttributes = Files.readAttributes(voPath, BasicFileAttributes.class);
			voFechaCreacion =  new Date(voAttributes.creationTime().toMillis());
		} catch (IOException e) {
			System.out.println("[ERROR] No se pudo extraer la fecha del archivo: " + voFile.getName() + "]");

			e.printStackTrace();
			return false; // Si hay un error, retornamos falso
		}

		Calendar voCalendarioHoy = Calendar.getInstance();
		voCalendarioHoy.setTime(voFechaActual);
		voCalendarioHoy.set(Calendar.HOUR_OF_DAY, 0);
		voCalendarioHoy.set(Calendar.MINUTE, 0);
		voCalendarioHoy.set(Calendar.SECOND, 0);
		voCalendarioHoy.set(Calendar.MILLISECOND, 0);
		Date voMediaNocheHoy = voCalendarioHoy.getTime();

		voCalendarioHoy.add(Calendar.DAY_OF_MONTH, 1);
		Date voMediaNocheNext = voCalendarioHoy.getTime();

		return !voFechaCreacion.before(voMediaNocheHoy) && voFechaCreacion.before(voMediaNocheNext);
	}


	public static String extraerHoras() {

		long tiempoActualMillis = System.currentTimeMillis();
		Date fechaHoraEjecucion = new Date(tiempoActualMillis);

		SimpleDateFormat formatoFechaHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String fechaHoraFormateada = formatoFechaHora.format(fechaHoraEjecucion);

		return fechaHoraFormateada;
	}
	
	public static double calculateMegabytesFile(File voFile) {

		try {
			return  voFile.length() / (1024.0 * 1024.0);
		} catch (Exception e) {
			log.error("Error al extraer peso de archivo: {}", e.getMessage());
			return 0.0;
		}
	}
	
	public static double roundNumberDecimal(double vdSizeFile, Integer viRound) {
		try {

			BigDecimal vbBd = new BigDecimal(vdSizeFile).setScale(viRound, RoundingMode.HALF_UP);
			double vbSize = vbBd.doubleValue();
			return vbSize;
		} catch (Exception e) {
			log.error("Error al redondear: {}", e.getMessage());
			return 0;
		}
	}

	public static long CSVFileLineCounterApache(File voFile) {
		try (Reader voReader = new FileReader(voFile)) {
			Iterable<CSVRecord> voRecords = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(voReader);
			long viLong = StreamSupport.stream(voRecords.spliterator(), false).count();
			return viLong;
		} catch (IOException e) {
			log.error("Error al leer el archivo CSV: {}", e.getMessage());
			return 0;
		}
	}
	
	public static String getCurrentDayInterval() {

	    ZoneId localZone = ZoneId.of("UTC-6");

	    // Día anterior en zona local
	    LocalDate yesterday = LocalDate.now(localZone).minusDays(1);

	    ZonedDateTime startLocal = yesterday.atStartOfDay(localZone);
	    ZonedDateTime endLocal = yesterday.plusDays(1).atStartOfDay(localZone);

	    ZonedDateTime startUtc = startLocal.withZoneSameInstant(ZoneOffset.UTC);
	    ZonedDateTime endUtc = endLocal.withZoneSameInstant(ZoneOffset.UTC);

	    DateTimeFormatter formatter = DateTimeFormatter
	            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	            .withZone(ZoneOffset.UTC);

	    return formatter.format(startUtc) + "/" + formatter.format(endUtc);
	}
	
	public static String dateReport() {

		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());

		// Restar un día
		cal.add(Calendar.DAY_OF_YEAR, -1);

		// Obtener la fecha resultante
		Date fechaConUnDiaMenos = cal.getTime();

		// Formatear la fecha como desees
		SimpleDateFormat formatoFecha = new SimpleDateFormat(FORMAT_DATE);
		return formatoFecha.format(fechaConUnDiaMenos);
	}
}
