package com.kranon.reports.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor

@SuperBuilder
public class ReportDTO {

	
	
	private String nameFolder;
	private String nameReport;
	private boolean generate;
	private double size;
	private boolean sftp;
	
	public ReportDTO(String nameFolder, String nameReport, boolean generate, double size, boolean sftp) {
		super();
		this.nameFolder = nameFolder;
		this.nameReport = nameReport;
		this.generate = generate;
		this.size = size;
		this.sftp = sftp;
	}
	
	
	
}
