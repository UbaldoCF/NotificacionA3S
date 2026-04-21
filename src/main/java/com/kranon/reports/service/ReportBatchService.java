package com.kranon.reports.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kranon.reports.config.PropertiesConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.kranon.reports.serviceImp.ChangeFileServiceImp;
import com.kranon.reports.serviceImp.ReportBatchServiceImp;
import com.kranon.reports.utils.Utils;

@Slf4j
@Service
public class ReportBatchService implements ReportBatchServiceImp {

    private static final String PROPERTIES_KEY = "intervalo";

    private static final String FOLDER_PROGRAM_FILES = "program";
    private static final String FOLDER_PROGRAM_FILES_24_HRS = "program_24hrs";
    private static final String FOLDER_PROGRAM_FILES_48_HRS = "program_48hrs";
    private static final String FOLDER_PROGRAM_FILES_72_HRS = "program_72hrs";

    @Setter(onMethod = @__(@Autowired))
    private PropertiesConfig voModel;

    private final List<String> propsAComentar = Arrays.asList(
            "SFTPHost",
            "SFTPUser",
            "SFTPPassword",
            "SFTPPort",
            "SFTPPath"
    );

    @Autowired
    private ChangeFileServiceImp changeFileServiceImp;

    @Override
    public void runReportGeneration(String vsPath, String key, LocalDate expectedDate, String vsFolder) {

        log.info("Dia generado: {}", Utils.getCurrentDayInterval());

        List<String> bloquesTiempo = dividirDiaTresPartes(Utils.getCurrentDayInterval());

        Path rutaBase = Paths.get(vsPath, vsFolder);

        String pathConfig = rutaBase.resolve(voModel.getA3SConfigName()).toString();
        String pathHashMap = rutaBase.resolve("hashMapData.txt").toString();
        String pathBat = rutaBase.resolve("program_" + key + ".bat").toString();

        log.info("Lectura de reporte: {}", key);
        log.info("Archivo de configuracion: {}", pathConfig);
        log.info("Archivo hashMap: {}", pathHashMap);
        log.info("Archivo bat: {}", pathBat);
        log.info("Folder: {}", vsFolder);
        log.info("ExpectedDate: {}", expectedDate);

        String nombreFinalNormal = buildExpectedName(expectedDate, vsFolder, key, false);
        String nombreFinalManual = buildExpectedName(expectedDate, vsFolder, key, true);

        List<Path> parcialesNormales = new ArrayList<>();
        List<Path> parcialesManuales = new ArrayList<>();

        Path carpetaTmp = rutaBase.resolve("tmp_merge" + System.currentTimeMillis());

        try {
            Files.createDirectories(carpetaTmp);

            // comentar una sola vez antes de iniciar las 3 ejecuciones
            changeFileServiceImp.commentProperties(pathConfig, propsAComentar);

            for (int i = 0; i < bloquesTiempo.size(); i++) {
                String timeValue = bloquesTiempo.get(i);

                log.info("Procesando bloque {} con intervalo={}", i + 1, timeValue);

                boolean updated = changeFileServiceImp.changePropertiesFile(pathConfig, PROPERTIES_KEY, timeValue);
                if (!updated) {
                    throw new RuntimeException("No se pudo actualizar la propiedad [" + PROPERTIES_KEY + "]");
                }

                if (!voModel.isA3SActivateBat()) {
                    log.info("[----------Proceso de unificacion desactivado----------]");
                    return;
                }

                log.info("[----------Proceso de unificacion activo----------]");
                log.info("Inicio de borrado de hashmap");
                deleteFile(pathHashMap);

                log.info("Inicio de ejecucion del BAT");
                int exitCode = ejecutarBat(pathBat);

                if (exitCode != 0) {
                    throw new RuntimeException("El BAT termino con codigo de error: " + exitCode);
                }

                // ===== Caso outbound: normal + manual =====
                if ("outbound".equalsIgnoreCase(key)) {

                    Path csvNormal = obtenerArchivoPorNombre(rutaBase.toString(), nombreFinalNormal);
                    if (csvNormal == null) {
                        throw new RuntimeException("No se encontro CSV normal: " + nombreFinalNormal);
                    }

                    Path copiaNormal = carpetaTmp.resolve("parte_" + (i + 1) + "_" + nombreFinalNormal);
                    Files.copy(csvNormal, copiaNormal, StandardCopyOption.REPLACE_EXISTING);
                    parcialesNormales.add(copiaNormal);
                    log.info("CSV normal detectado y copiado: {}", copiaNormal);

                    Path csvManual = obtenerArchivoPorNombre(rutaBase.toString(), nombreFinalManual);
                    if (csvManual == null) {
                        throw new RuntimeException("No se encontro CSV manual: " + nombreFinalManual);
                    }

                    Path copiaManual = carpetaTmp.resolve("parte_" + (i + 1) + "_" + nombreFinalManual);
                    Files.copy(csvManual, copiaManual, StandardCopyOption.REPLACE_EXISTING);
                    parcialesManuales.add(copiaManual);
                    log.info("CSV manual detectado y copiado: {}", copiaManual);

                } else {
                    // ===== Resto: solo un archivo =====
                    Path csvNormal = obtenerArchivoPorNombre(rutaBase.toString(), nombreFinalNormal);
                    if (csvNormal == null) {
                        throw new RuntimeException("No se encontro CSV generado: " + nombreFinalNormal);
                    }

                    Path copiaNormal = carpetaTmp.resolve("parte_" + (i + 1) + "_" + nombreFinalNormal);
                    Files.copy(csvNormal, copiaNormal, StandardCopyOption.REPLACE_EXISTING);
                    parcialesNormales.add(copiaNormal);
                    log.info("CSV detectado y copiado: {}", copiaNormal);
                }
            }

            // ===== Unificacion final =====
            Path archivoFinalNormal = rutaBase.resolve(nombreFinalNormal);
            unirCsvs(parcialesNormales, archivoFinalNormal);
            log.info("Reporte final normal generado en: {}", archivoFinalNormal);

            if ("outbound".equalsIgnoreCase(key)) {
                Path archivoFinalManual = rutaBase.resolve(nombreFinalManual);
                unirCsvs(parcialesManuales, archivoFinalManual);
                log.info("Reporte final manual generado en: {}", archivoFinalManual);
            }

        } catch (Exception e) {
            log.error("[runReportGeneration] Error en unificacion de CSVs: {}", e.getMessage(), e);
        } finally {
            try {
                changeFileServiceImp.uncommentProperties(pathConfig, propsAComentar);
            } catch (Exception e) {
                log.error("No se pudieron descomentar propiedades: {}", e.getMessage(), e);
            }

            try {
              //  limpiarTemporales(carpetaTmp);
            } catch (Exception e) {
                log.warn("No se pudo limpiar la carpeta temporal {}: {}", carpetaTmp, e.getMessage());
            }
        }
    }

    public List<String> dividirDiaTresPartes(String rango) {
        String[] partes = rango.split("/");
        Instant inicio = Instant.parse(partes[0]);
        Instant fin = Instant.parse(partes[1]);

        long totalSeconds = Duration.between(inicio, fin).getSeconds();
        long bloque = totalSeconds / 3;

        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC);

        List<String> resultado = new ArrayList<>();
        Instant current = inicio;

        for (int i = 0; i < 3; i++) {
            Instant siguiente = (i == 2) ? fin : current.plusSeconds(bloque);
            resultado.add(formatter.format(current) + "/" + formatter.format(siguiente));
            current = siguiente;
        }

        return resultado;
    }

    private int ejecutarBat(String rutaBat) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", rutaBat);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[BAT] {}", line);
            }
        }

        int exitCode = process.waitFor();
        log.info("BAT finalizado con codigo: {}", exitCode);
        return exitCode;
    }

    private Path obtenerArchivoPorNombre(String carpetaSalida, String nombreArchivo) {
        try {
            Path path = Paths.get(carpetaSalida, nombreArchivo);
            return Files.exists(path) ? path : null;
        } catch (Exception e) {
            log.error("[obtenerArchivoPorNombre] {}", e.getMessage(), e);
            return null;
        }
    }

    private boolean deleteFile(String rutaArchivo) {
        try {
            Path path = Paths.get(rutaArchivo);

            log.info("Nombre del archivo que se eliminara: {}", path.getFileName());

            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Archivo residual eliminado: {}", rutaArchivo);
                return true;
            } else {
                log.info("No existe archivo residual: {}", rutaArchivo);
                return false;
            }

        } catch (Exception e) {
            log.error("[deleteFile]: {}", e.getMessage(), e);
            return false;
        }
    }

    private void unirCsvs(List<Path> archivosCsv, Path archivoFinal) throws IOException {
        if (archivosCsv == null || archivosCsv.isEmpty()) {
            throw new IllegalArgumentException("No hay archivos CSV para unir.");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(archivoFinal, StandardCharsets.UTF_8)) {
            boolean primeraCabecera = true;

            for (Path csv : archivosCsv) {
                try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
                    String line;
                    boolean esPrimeraLinea = true;

                    while ((line = reader.readLine()) != null) {
                        if (esPrimeraLinea) {
                            if (primeraCabecera) {
                                writer.write(line);
                                writer.newLine();
                                primeraCabecera = false;
                            }
                            esPrimeraLinea = false;
                            continue;
                        }

                        writer.write(line);
                        writer.newLine();
                    }
                }
            }
        }
    }

    private void limpiarTemporales(Path carpetaTmp) throws IOException {
        if (carpetaTmp == null || !Files.exists(carpetaTmp)) {
            return;
        }

        try (var walk = Files.walk(carpetaTmp)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        log.warn("No se pudo eliminar temporal: {}", path);
                    }
                });
        }
    }

    private String buildExpectedName(LocalDate expectedDate, String vsFolder, String vsClave, boolean vbManual) {

        String vsDate = expectedDate.toString();
        String vsDateFixed = vsDate.substring(0, 4) + "_" + vsDate.substring(5, 7) + "-" + vsDate.substring(8, 10);

        String vsCompany = (voModel.getCompany() != null && !voModel.getCompany().trim().isEmpty())
                ? voModel.getCompany().trim()
                : "COMPANY";

        String vsSuffixManual = vbManual ? "_Manual" : "";

        if (FOLDER_PROGRAM_FILES.equalsIgnoreCase(vsFolder)) {
            return vsDateFixed + "_" + vsClave + vsSuffixManual + ".csv";
        }

        String vsPrefixHrs;

        if (FOLDER_PROGRAM_FILES_24_HRS.equalsIgnoreCase(vsFolder)) {
            vsPrefixHrs = "24H";
        } else if (FOLDER_PROGRAM_FILES_48_HRS.equalsIgnoreCase(vsFolder)) {
            vsPrefixHrs = "48H";
        } else if (FOLDER_PROGRAM_FILES_72_HRS.equalsIgnoreCase(vsFolder)) {
            vsPrefixHrs = "72H";
        } else {
            vsPrefixHrs = "XXH";
        }

        return vsDateFixed + "" + vsPrefixHrs + "" + vsClave + "_" + vsCompany + vsSuffixManual + ".csv";
    }
}