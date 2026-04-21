package com.kranon.reports.serviceImp;

import java.time.LocalDate;

public interface ReportBatchServiceImp {

	 public void runReportGeneration(String vsPath, String key, LocalDate expectedDate, String vsFolder);
}
