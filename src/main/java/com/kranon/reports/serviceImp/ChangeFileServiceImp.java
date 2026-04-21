package com.kranon.reports.serviceImp;

import java.io.IOException;
import java.util.List;

public interface ChangeFileServiceImp {

	

	 boolean changePropertiesFile(String rutaArchivo, String clave, String nuevoValor);
	 boolean commentProperties(String rutaArchivo, List<String> propiedades);
	 
	 boolean uncommentProperties(String rutaArchivo, List<String> propiedades);
}
