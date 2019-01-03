package org.dllearner.cli;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.format.CellDateFormatter;
import org.apache.poi.ss.format.CellFormat;
import org.apache.poi.ss.format.CellFormatType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dllearner.statistics.EvaluationData;
import org.dllearner.statistics.ExcelTable;
import org.dllearner.statistics.LearningData;

import com.microsoft.schemas.office.visio.x2012.main.CellType;

public class Evaluation {
	
	public Evaluation() {
//		Map<LearningData, LearningData> pair = new HashMap<LearningData, LearningData>();
//		data.put("Name", pair);
	}

	public static void main(String[] args) throws IOException {
		Evaluation eva = new Evaluation();
		// TODO Auto-generated method stub
		
		String projectPath = "/Users/aris/Documents/repositories/ipr/aml_import/resources/output/ilp2018/4_nestedIE/";
		
		List<DataMatrix> matrices = eva.readTable(projectPath + "statistics.xlsx");
		ExcelTable table = new ExcelTable(projectPath + "statistics.xlsx", 2);
		
		System.out.println("\nmaking comparison...");
		for(DataMatrix matrix : matrices) {
//			System.out.println(matrix);		
//			LearningData compare = matrix.compareData();
//			System.out.println(compare);
//			table.newData(compare.getName(), compare);
			EvaluationData data = matrix.getEvaluation();
//			table.newData(data.getName(), data.getData(label));
		}
		
		System.out.println("saving statistics...");
		table.write(projectPath + "statistics.xlsx");
		System.out.println("evaluation finished");
	}
	
	public List<LearningData> readSheet(XSSFSheet sheet){
				
		Iterator rows = sheet.rowIterator();
		XSSFRow row = (XSSFRow) rows.next();
		String name = row.getCell(0).getStringCellValue();
		List<String> labels = new ArrayList<String>();
		if(name.equals("Name")) {
			for(int i = 1; i < row.getLastCellNum(); i++) {
				String label = row.getCell(i).getStringCellValue();
				labels.add(label);						
			}
		}
		rows.next();
//		System.out.println(labels);

		List<LearningData> dataList = new ArrayList<LearningData>();
		while (rows.hasNext())
		{			
			row = (XSSFRow) rows.next();		
						
			int i = 0;
			Iterator cells = row.cellIterator();			
			LearningData data = new LearningData();
			String s = "";
			while (cells.hasNext())
			{					
				XSSFCell cell = (XSSFCell) cells.next();
//				System.out.println(cell.toString());
				if(i == 0) {
					s = cell.getStringCellValue();
					data.setName(cell.getStringCellValue());	
					i++;
					continue;
				}
				if(cell.getCellTypeEnum() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
					if(!labels.get(i-1).equals(""))
						data.getData().put(labels.get(i-1), (long) cell.getNumericCellValue());
				}
				
				if(cell.getCellTypeEnum() == org.apache.poi.ss.usermodel.CellType.STRING && i == row.getLastCellNum() -1) {
//					System.out.println(s);
					data.setSolution(cell.toString());
				}
				i++;
			}
			dataList.add(data);
		}		
		return dataList;
	} 
	
	
	public List<DataMatrix> readTable(String filename) throws IOException
	{
		InputStream file = new FileInputStream(filename);
		XSSFWorkbook wb = new XSSFWorkbook(file);

		
		XSSFSheet sheetRho = wb.getSheetAt(0);		
		XSSFSheet sheetAML = wb.getSheetAt(1);		
				
				
		List<DataMatrix> matrices = new ArrayList<DataMatrix>();
		System.out.println("\nreading rho...");
		List<LearningData> rho = readSheet(sheetRho);
		System.out.println("\nreading aml...");
		List<LearningData> aml = readSheet(sheetAML);
		
		for(LearningData rd : rho) {
			String name = rd.getName();
			for(LearningData ad : aml) {
				if(ad.getName().equals(name)) {
					if(rd.getSolution() != null) {
//						System.out.println("\n" + name + ": " + rd.getSolution());
						matrices.add(new DataMatrix(rd, ad));
					}
				}
					
			}
		}		
		file.close();
		wb.close();
		return matrices;	
	}

}
