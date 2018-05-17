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
	CellStyle headerStyle;
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
		
		
		headerStyle = workbook.createCellStyle();
//		headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
//		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		XSSFFont font = ((XSSFWorkbook) workbook).createFont();
		font.setFontName("Arial");
		font.setFontHeightInPoints((short) 16);
		font.setBold(true);
		headerStyle.setFont(font);
//		headerStyle.setShrinkToFit(true);
	}	
	
	
	public ExcelTable(String filename, int idx) throws IOException {
		FileInputStream template = new FileInputStream(new File(filename));
		workbook = new XSSFWorkbook(template);
		sheet = workbook.getSheetAt(idx);
		
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
		
//		Map<Integer, List<String>> data = new HashMap<>();
		header = sheet.getRow(0);			
		for(Cell cell : header) {
			if(!cell.toString().isEmpty()) {
				NrOfCols++;
			}
		}
		NrOfRows = sheet.getPhysicalNumberOfRows();
	}
	
	public void newheader(String name, short index) {
		Cell headerCell = header.createCell(index);
		headerCell.setCellValue(name);
		headerCell.setCellStyle(headerStyle);
		sheet.autoSizeColumn(index);
		NrOfCols++;
	}
	
	public void newData(String name, LearningData data) {
		
//		if(data.getSize() != NrOfCols) 
//			System.err.println("Dimension of data different to dimension of table!");

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
				Cell dataCell = row.createCell(idx);
				dataCell.setCellValue(elements.get(field));	
				dataCell.setCellStyle(dataStyle);		
				DecimalFormat df = new DecimalFormat("####0.00");
//				if(field.equals("#Rules"))
//					dataCell.setCellStyle(boldStyle);
//				else if(field.equals("#Nodes"))
//					dataCell.setCellStyle(boldStyle);
				if(field.equals("LogTime")) {
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getLogPercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("ComputeTime")) {
//					dataCell.setCellStyle(boldStyle);
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getComputePercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("RefinementTime")) {
//					dataCell.setCellStyle(boldStyle);
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getRefinementPercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("ReasoningTime")) {
//					dataCell.setCellStyle(boldStyle);
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getReasoningPercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("TreeTime")) {
//					dataCell.setCellStyle(boldStyle);
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getTreePercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("InstCheckTime")) {
					Cell percentage = row.createCell(idx+1);
					percentage.setCellValue(Double.parseDouble(df.format(data.getInstCheckPercentage())));
					percentage.setCellStyle(dataStyle);
				}else if(field.equals("SubsumptionTime")) {
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
	
	public static void main(String[] args) throws Throwable {
		
		String path = "/Users/aris/Documents/repositories/ipr/aml_import/resources/output/";
		String template = "statistics.xlsx";
		String output = "rho.xlsx";
		
		
		ExcelTable statistics = new ExcelTable(path + template, 1);
		
		LearningData data = new LearningData("dummy3_1");
		data.setIterations(1425);
		data.setDepth(11);
		data.setNrOfNodes(1579);
		data.setNrOfRules(78693);
		data.setRunTime(4211);
		data.setLogTime(124);
		data.setComputeTime(4087);
		data.setRefinement(786);
		data.setReasoning(2240);
		data.setInstCheck(0);
		data.setSubsumption(0);
	
		statistics.newData(data.getName(), data);
				
		statistics.write(path + template);
		System.out.println("successfully saved " + template + " to disk");
	}
}

