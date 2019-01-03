package org.dllearner.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelTable {
	
	Row header;
	Sheet sheet;
	Workbook workbook;
	CellStyle dataStyle; 
	CellStyle solutionStyle;
	CellStyle boldStyle; 
	int NrOfRows;
	int NrOfCols;
	
	public ExcelTable() {
		workbook = new XSSFWorkbook();		
		sheet = workbook.createSheet("rho");
		NrOfRows = 0;
		NrOfCols = 0;
		header = sheet.createRow(NrOfRows);	
	}		
	
	public ExcelTable(String filename, int sheetIdx) throws IOException {
		FileInputStream template = new FileInputStream(new File(filename));
		workbook = new XSSFWorkbook(template);
		sheet = workbook.getSheetAt(sheetIdx);
		
		dataStyle = workbook.createCellStyle();
		dataStyle.setAlignment(HorizontalAlignment.CENTER);
		
		boldStyle = workbook.createCellStyle();
		boldStyle.setAlignment(HorizontalAlignment.CENTER);
		XSSFFont font = ((XSSFWorkbook) workbook).createFont();
		font.setFontHeight(12);
		font.setBold(true);
//		font.setFontName("Regular");
		boldStyle.setFont(font);

		solutionStyle = workbook.createCellStyle();
		solutionStyle.setAlignment(HorizontalAlignment.LEFT);
		
		header = sheet.getRow(0);
//		Row secondRow = sheet.getRow(1);
//		
//		if(secondRow != null) {
//			NrOfCols = secondRow.getLastCellNum() + 1;
//		}else
//			NrOfCols = header.getLastCellNum() + 1;
		NrOfRows = sheet.getPhysicalNumberOfRows();
	}
	
//	public void newheader(String name, short index) {
//		Cell headerCell = header.createCell(index);
//		headerCell.setCellValue(name);
//		headerCell.setCellStyle(dataStyle);
//		sheet.autoSizeColumn(index);
//		NrOfCols++;
//	}
//	
//	private void newData(String label, int rowIdx, long data) {
//		if(sheet.getPhysicalNumberOfRows() < rowIdx+1) {
//			System.out.println("error: the requested row does not exist yet!");
//		}
//		
//		Row row = sheet.getRow(rowIdx);
//		Cell dataCell = row.createCell(idx);
//		dataCell.setCellValue(elements.get(field));	
//		dataCell.setCellStyle(dataStyle);		
//		DecimalFormat df = new DecimalFormat("####0.00");
//		
//	}
//	
	public void newData(String name, LearningData data) {

		Row row = sheet.createRow(NrOfRows++);
		Cell nameCell = row.createCell(0);
		nameCell.setCellValue(name);
		nameCell.setCellStyle(dataStyle);
		sheet.autoSizeColumn(0);
		Iterator it = header.cellIterator();
		int idx = 0;
		Map<String, Long> elements = data.getData();
		while(it.hasNext()) {
			String field = it.next().toString();
//				System.out.println("fill data to: " + field);				
			if(elements.containsKey(field)) {			
//				System.out.println("found value " + elements.get(field) + " for field: " + field);
				Cell dataCell = row.createCell(idx);
				dataCell.setCellValue(elements.get(field));	
				dataCell.setCellStyle(dataStyle);		
				DecimalFormat df = new DecimalFormat("####0.00");
//				if(field.equals("#Rules"))
//					dataCell.setCellStyle(boldStyle);
//				else if(field.equals("#Nodes"))
//					dataCell.setCellStyle(boldStyle);				
				if(field.equals("LogTime")) {// && header.getLastCellNum() == NrOfCols-1) {
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getLogPercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("ComputeTime")) {// && header.getLastCellNum() == NrOfCols-1) {
//					dataCell.setCellStyle(boldStyle);
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getComputePercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("RefinementTime")) {// && header.getLastCellNum() == NrOfCols-1) {
//					dataCell.setCellStyle(boldStyle);
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getRefinementPercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("ReasoningTime")) {// && header.getLastCellNum() == NrOfCols-1) {
//					dataCell.setCellStyle(boldStyle);
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getReasoningPercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("TreeTime")) {// && header.getLastCellNum() == NrOfCols-1) {
//					dataCell.setCellStyle(boldStyle);
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getTreePercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("InstCheckTime")) {// && header.getLastCellNum() == NrOfCols-1) {
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getInstCheckPercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("SubsumptionTime")) {// && header.getLastCellNum() == NrOfCols-1) {
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getSubsumptionPercentage())));
					percentage.setCellStyle(dataStyle);
				}
			}
			if(field.equals("Solution")) {
				Cell cell = row.createCell(idx);
				cell.setCellValue(data.getSolution());	
				cell.setCellStyle(solutionStyle);	
				sheet.autoSizeColumn(idx);
			}
				
			idx++;
		}						

		
	}
	
	public void write(String filename) throws IOException {		
		FileOutputStream outputStream = new FileOutputStream(filename);
		workbook.write(outputStream);
		workbook.close();
	}
	
//	public static void main(String[] args) throws Throwable {
//		
//		String path = "/Users/aris/Documents/repositories/ipr/aml_import/resources/output/";
//		String template = "statistics.xlsx";		
//		ExcelTable statistics = new ExcelTable(path + template, 1);
//		
//		LearningData data = new LearningData("dummy3_1");
//		data.setIterations(1425);
//		data.setDepth(11);
//		data.setNrOfNodes(1579);
//		data.setNrOfRules(78693);
//		data.setRunTime(4211);
//		data.setLogTime(124);
//		data.setComputeTime(4087);
//		data.setRefinement(786);
//		data.setReasoning(2240);
//		data.setInstCheck(0);
//		data.setSubsumption(0);
//	
//		statistics.newData(data.getName(), data);
//				
//		statistics.write(path + template);
//		System.out.println("successfully saved " + template + " to disk");
//	}
}

