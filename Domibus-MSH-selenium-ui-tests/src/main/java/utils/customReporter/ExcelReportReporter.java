package utils.customReporter;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.*;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite;
import utils.TestRunData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Catalin Comanici
 * @description:
 * @since 4.1
 */
public class ExcelReportReporter implements IReporter {

	private TestRunData data = new TestRunData();
	private static final String[] headers = {"Type", "Test Suite Name", "Test Case ID", "Test Case Name", "Can be run on Bamboo", "TC is disabled", "Test Result", "Last Execution Started", "Execution time", "JIRA tickets", "Impact", "Comment"};
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static String filename;


	/*Creates the report file, the sheet and writes the headers of the table with style as well*/
	private void onStart() {

		String dateStr = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(Calendar.getInstance().getTime());
		filename = data.getReportsFolder() + "TestRunReport" + dateStr + ".xlsx";

		XSSFWorkbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Run Report");

		Row headerRow = sheet.createRow(0);
		XSSFCellStyle headerStyle = composeCellStyle((XSSFWorkbook) sheet.getWorkbook(), "Header");

		for (int i = 0; i < headers.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellStyle(headerStyle);
			cell.setCellValue(headers[i]);
		}

		try {
			FileOutputStream os = new FileOutputStream(filename);
			workbook.write(os);
			workbook.close();
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {

		onStart();

		//Iterating over each suite included in the test
		for (ISuite suite : suites) {

			//Following code gets the suite name
			String suiteName = suite.getName();
			System.out.println("suiteName = " + suiteName);


			//Getting the results for the said suite
			Map<String, ISuiteResult> suiteResults = suite.getResults();
			System.out.println("suiteResults = " + suiteResults);

			for (ISuiteResult sr : suiteResults.values()) {
				ITestContext tc = sr.getTestContext();
				writeResultsToFile(tc.getPassedTests().getAllResults());
				writeResultsToFile(tc.getFailedTests().getAllResults());
				writeResultsToFile(tc.getSkippedTests().getAllResults());
			}
		}
	}

	private void writeResultsToFile(Set<ITestResult> trs){
		Status[] vals = Status.values();

		for (ITestResult tr : trs) {
			String stat = vals[tr.getStatus()-1].name();
			try {
				writeRowToReportFile(tr, stat);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/* depending on the type of cell returns the desired style. The supported type are "Header", "Fail", "Pass" */
	private XSSFCellStyle composeCellStyle(XSSFWorkbook workbook, String type) {
		XSSFCellStyle style = workbook.createCellStyle();
		XSSFFont font = workbook.createFont();
		font.setBold(true);

		if (StringUtils.equalsIgnoreCase(type, "Pass")) {
			style.setFillBackgroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
			style.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());

		} else if (StringUtils.equalsIgnoreCase(type, "Fail")) {
			style.setFillBackgroundColor(IndexedColors.RED.getIndex());
			style.setFillForegroundColor(IndexedColors.RED.getIndex());
			style.setFont(font);

		} else if (StringUtils.equalsIgnoreCase(type, "Skipped")) {
			style.setFillBackgroundColor(IndexedColors.WHITE.getIndex());
			style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			style.setFont(font);

		} else if (StringUtils.equalsIgnoreCase(type, "Header")) {
			style.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			style.setFont(font);
		}
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		return style;
	}

	/* Generic method to write a row in the report file with the test id, name and result */
	private void writeRowToReportFile(ITestResult iTestResult, String result) throws Exception {

		String qualifiedName = iTestResult.getMethod().getQualifiedName();

		File myFile = new File(filename);
		FileInputStream inputStream = new FileInputStream(myFile);
		XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

		Sheet reportSheet = workbook.getSheetAt(0);
		int rowNum = reportSheet.getLastRowNum() + 1;
		Row currentRow = reportSheet.createRow(rowNum);

		currentRow.createCell(0).setCellValue(iTestResult.getTestContext().getName());
		currentRow.createCell(1).setCellValue(iTestResult.getTestContext().getSuite().getName());
		currentRow.createCell(2).setCellValue(iTestResult.getMethod().getConstructorOrMethod().getMethod().getAnnotation(Test.class).description());
		currentRow.createCell(3).setCellValue(iTestResult.getName());
		currentRow.createCell(4).setCellValue("Yes");
		currentRow.createCell(5).setCellValue(!iTestResult.getMethod().getConstructorOrMethod().getMethod().getAnnotation(Test.class).enabled());
		Cell cell = currentRow.createCell(6);
		cell.setCellValue(result);
		cell.setCellStyle(composeCellStyle(workbook, result));
		currentRow.createCell(7).setCellValue(sdf.format(new Date(iTestResult.getStartMillis())));
		currentRow.createCell(8).setCellValue((iTestResult.getEndMillis() - iTestResult.getStartMillis()) / 1000);
		currentRow.createCell(9).setCellValue("");
		currentRow.createCell(10).setCellValue("");

		if (iTestResult.getThrowable() != null) {
			currentRow.createCell(11).setCellValue(iTestResult.getThrowable().getMessage());
		}


		FileOutputStream os = new FileOutputStream(myFile);
		workbook.write(os);
		os.close();
		workbook.close();
		inputStream.close();

	}




}

enum Status{
	Pass, FAIL, Skipped;
}