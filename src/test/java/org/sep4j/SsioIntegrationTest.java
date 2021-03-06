package org.sep4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * the integration test
 * 
 * @author chenjianjx
 * 
 */

public class SsioIntegrationTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Test
	public void saveIfNoErrorTest() throws InvalidFormatException, IOException {
		LinkedHashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		headerMap.put("fake", "Not Real");

		ITRecord record = new ITRecord();

		Collection<ITRecord> records = Arrays.asList(record);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		List<DatumError> datumErrors = new ArrayList<DatumError>();

		// save it
		Ssio.saveIfNoDatumError(headerMap, records, outputStream, null, datumErrors);

		byte[] spreadsheet = outputStream.toByteArray();
		Assert.assertEquals(0, spreadsheet.length);
		Assert.assertEquals(1, datumErrors.size());

	}

	@Test
	public void saveTest_ValidAndInvalid() throws InvalidFormatException, IOException {
		LinkedHashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		headerMap.put("primInt", "Primitive Int");
		headerMap.put("fake", "Not Real");

		ITRecord record = new ITRecord();
		record.setPrimInt(123);

		Collection<ITRecord> records = Arrays.asList(record);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		String datumErrPlaceholder = "!!ERROR!!";
		List<DatumError> datumErrors = new ArrayList<DatumError>();

		// save it
		Ssio.save(headerMap, records, outputStream, datumErrPlaceholder, datumErrors);
		byte[] spreadsheet = outputStream.toByteArray();

		// do a save for human eye check
		FileUtils.writeByteArrayToFile(createFile("saveTest_ValidAndInvalid"), spreadsheet);

		// then parse it
		Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(spreadsheet));

		/*** do assertions ***/
		Sheet sheet = workbook.getSheetAt(0);
		Row headerRow = sheet.getRow(0);
		Row dataRow = sheet.getRow(1);

		Cell cell00 = headerRow.getCell(0);
		Cell cell01 = headerRow.getCell(1);
		Cell cell10 = dataRow.getCell(0);
		Cell cell11 = dataRow.getCell(1);

		// size
		Assert.assertEquals(1, sheet.getLastRowNum());
		Assert.assertEquals(2, headerRow.getLastCellNum()); // note cell num is
															// 1-based
		Assert.assertEquals(2, dataRow.getLastCellNum());

		// types
		Assert.assertEquals(Cell.CELL_TYPE_STRING, cell00.getCellType());
		Assert.assertEquals(Cell.CELL_TYPE_STRING, cell01.getCellType());
		Assert.assertEquals(Cell.CELL_TYPE_STRING, cell10.getCellType());
		Assert.assertEquals(Cell.CELL_TYPE_STRING, cell11.getCellType());

		// texts
		Assert.assertEquals("Primitive Int", cell00.getStringCellValue());
		Assert.assertEquals("Not Real", cell01.getStringCellValue());
		Assert.assertEquals("123", cell10.getStringCellValue());
		Assert.assertEquals("!!ERROR!!", cell11.getStringCellValue());

		// errors
		DatumError datumError = datumErrors.get(0);
		Assert.assertEquals(1, datumErrors.size());
		Assert.assertEquals(0, datumError.getRecordIndex());
		Assert.assertEquals("fake", datumError.getPropName());
		Assert.assertTrue(datumError.getCause().getMessage().contains("no getter method"));

	}

	@Test
	public void saveTest_IngoringErrors() throws InvalidFormatException, IOException {
		LinkedHashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		headerMap.put("fake", "Not Real");

		ITRecord record = new ITRecord();

		Collection<ITRecord> records = Arrays.asList(record);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		// save it
		Ssio.save(headerMap, records, outputStream);
		byte[] spreadsheet = outputStream.toByteArray();

		// do a save for human eye check
		FileUtils.writeByteArrayToFile(createFile("saveTest_IngoringErrors"), spreadsheet);

		// then parse it
		Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(spreadsheet));

		/*** do assertions ***/
		Sheet sheet = workbook.getSheetAt(0);
		Row headerRow = sheet.getRow(0);
		Row dataRow = sheet.getRow(1);

		Cell cell00 = headerRow.getCell(0);
		Cell cell10 = dataRow.getCell(0);

		// size
		Assert.assertEquals(1, sheet.getLastRowNum());
		Assert.assertEquals(1, headerRow.getLastCellNum()); // note cell num is
															// 1-based
		Assert.assertEquals(1, dataRow.getLastCellNum());

		// types
		Assert.assertEquals(Cell.CELL_TYPE_STRING, cell00.getCellType());
		Assert.assertEquals(Cell.CELL_TYPE_STRING, cell10.getCellType());

		// texts
		Assert.assertEquals("Not Real", cell00.getStringCellValue());
		Assert.assertEquals("", cell10.getStringCellValue());

	}

	@Test
	public void saveTest_HeadersOnly() throws InvalidFormatException, IOException {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		// save it
		Ssio.save(ITRecord.getHeaderMap(), null, outputStream);
		byte[] spreadsheet = outputStream.toByteArray();

		// do a save for human eye check
		FileUtils.writeByteArrayToFile(createFile("saveTest_HeadersOnly"), spreadsheet);

		// then parse it
		Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(spreadsheet));

		/*** do assertions ***/
		Sheet sheet = workbook.getSheetAt(0);
		Row headerRow = sheet.getRow(0);

		// size
		Assert.assertEquals(0, sheet.getLastRowNum());
		Assert.assertEquals(ITRecord.getHeaderMap().size(), headerRow.getLastCellNum());

	}

	@Test
	public void saveTest_BigNumber() throws InvalidFormatException, IOException {
		LinkedHashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		headerMap.put("bigInteger", "Big Int");
		headerMap.put("bigDecimal", "Big Decimal");

		String bigIntegerStr = "" + Long.MAX_VALUE;
		String bigDecimalStr = Long.MAX_VALUE + "." + Long.MAX_VALUE;
		ITRecord record = new ITRecord();
		record.setBigInteger(new BigInteger(bigIntegerStr));
		record.setBigDecimal(new BigDecimal(bigDecimalStr));

		Collection<ITRecord> records = Arrays.asList(record);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		List<DatumError> datumErrors = new ArrayList<DatumError>();

		// save it
		Ssio.save(headerMap, records, outputStream, null, datumErrors);
		byte[] spreadsheet = outputStream.toByteArray();

		// do a save for human eye check
		FileUtils.writeByteArrayToFile(createFile("saveTest_BigNumber"), spreadsheet);

		// then parse it
		Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(spreadsheet));

		/*** do assertions ***/
		Sheet sheet = workbook.getSheetAt(0);
		Row dataRow = sheet.getRow(1);

		Cell cell10 = dataRow.getCell(0);
		Cell cell11 = dataRow.getCell(1);

		// texts
		Assert.assertEquals(bigIntegerStr, cell10.getStringCellValue());
		Assert.assertEquals(bigDecimalStr, cell11.getStringCellValue());

		// errors
		Assert.assertEquals(0, datumErrors.size());

	}

	@Test(expected = InvalidHeaderRowException.class)
	public void parseTest_InvalidHeader() throws InvalidFormatException, InvalidHeaderRowException {
		ByteArrayInputStream in = toByteArrayInputStreamAndClose(this.getClass().getResourceAsStream("/parse-test-all-headers-wrong.xlsx"));
		Ssio.parse(ITRecord.getReverseHeaderMap(), in, null, ITRecord.class);
	}

	@Test
	public void parseTest_Excel97() throws InvalidFormatException, InvalidHeaderRowException {
		ByteArrayInputStream in = toByteArrayInputStreamAndClose(this.getClass().getResourceAsStream("/parse-test-excel97.xls"));
		List<ITRecord> list = Ssio.parse(ITRecord.getReverseHeaderMap(), in, null, ITRecord.class);
		Assert.assertEquals((short) 1, list.get(0).getPrimShort());
	}

	@Test
	public void parseTest_DataHalfCorrect() throws InvalidFormatException, InvalidHeaderRowException {
		ByteArrayInputStream in = toByteArrayInputStreamAndClose(this.getClass().getResourceAsStream("/parse-test-data-half-correct.xlsx"));
		List<CellError> cellErrors = new ArrayList<CellError>();
		List<ITRecord> records = Ssio.parse(ITRecord.getReverseHeaderMap(), in, cellErrors, ITRecord.class);

		ITRecord record = records.get(0);
		Assert.assertEquals(1, records.size());
		Assert.assertEquals(123, record.getPrimInt());

		CellError error = cellErrors.get(0);
		Assert.assertEquals(1, cellErrors.size());
		Assert.assertEquals(2, error.getRowIndexOneBased());
		Assert.assertEquals(3, error.getColumnIndexOneBased());
		Assert.assertTrue(error.getCause().getMessage().contains("suitable setter"));
		Assert.assertTrue(error.getCause().getMessage().contains("abc"));
	}

	@Test
	public void parseTest_AllStringCells() throws InvalidFormatException, InvalidHeaderRowException {
		ByteArrayInputStream in = toByteArrayInputStreamAndClose(this.getClass().getResourceAsStream("/parse-test-all-string-cells-input.xlsx"));
		List<CellError> cellErrors = new ArrayList<CellError>();
		List<ITRecord> records = Ssio.parse(ITRecord.getReverseHeaderMap(), in, cellErrors, ITRecord.class);

		// assertions
		ITRecord record = records.get(0);
		Assert.assertEquals(1, records.size());
		Assert.assertEquals(0, cellErrors.size());

		Assert.assertEquals(12, record.getPrimShort());
		Assert.assertEquals(2323, record.getPrimInt());
		Assert.assertEquals(1213l, record.getPrimLong());
		Assert.assertEquals(342.34f, record.getPrimFloat());
		Assert.assertEquals(0.34, record.getPrimDouble());
		Assert.assertEquals(true, record.isPrimBoolean());

		Assert.assertEquals(new Short("23"), record.getObjShort());
		Assert.assertEquals(new Integer(234), record.getObjInt());
		Assert.assertEquals(new Long(982), record.getObjLong());
		Assert.assertEquals(new Float(483.323f), record.getObjFloat());
		Assert.assertEquals(new Double(23903.234), record.getObjDouble());
		Assert.assertEquals(new Boolean(false), record.getObjBoolean());

		Assert.assertEquals(new BigInteger("123456789123456789"), record.getBigInteger());
		Assert.assertEquals(new BigDecimal("123456789.123456789"), record.getBigDecimal());
		Assert.assertEquals("abc", record.getStr());
		Assert.assertEquals("2014-11-29 16:18:47", record.getDateStr());

	}

	@Test
	public void parseTest_FreeTypeCells() throws InvalidFormatException, InvalidHeaderRowException {
		ByteArrayInputStream in = toByteArrayInputStreamAndClose(this.getClass().getResourceAsStream("/parse-test-all-free-type-input.xlsx"));
		List<CellError> cellErrors = new ArrayList<CellError>();
		List<ITRecord> records = Ssio.parse(ITRecord.getReverseHeaderMap(), in, cellErrors, ITRecord.class);

		// assertions
		ITRecord record = records.get(0);
		Assert.assertEquals(1, records.size());
		Assert.assertEquals(0, cellErrors.size());

		Assert.assertEquals(12, record.getPrimShort());
		Assert.assertEquals(2323, record.getPrimInt());
		Assert.assertEquals(1213l, record.getPrimLong());
		// //floats and doubles won't be accurate
		Assert.assertEquals(342, (int) record.getPrimFloat());
		Assert.assertEquals(0, (int) record.getPrimDouble());
		Assert.assertEquals(true, record.isPrimBoolean());

		Assert.assertEquals(new Short("23"), record.getObjShort());
		Assert.assertEquals(new Integer(234), record.getObjInt());
		Assert.assertEquals(new Long(982), record.getObjLong());
		// //floats and doubles won't be accurate
		Assert.assertEquals(483, record.getObjFloat().intValue());
		Assert.assertEquals(23903, record.getObjDouble().intValue());
		Assert.assertEquals(new Boolean(false), record.getObjBoolean());

		Assert.assertEquals(new BigInteger("123456789123456000"), record.getBigInteger());
		Assert.assertEquals(123456789, record.getBigDecimal().intValue());
		Assert.assertEquals("abc", record.getStr());
		Assert.assertEquals("2014-11-29 16:18:47", record.getDateStr());

	}

	// read outside input streams as bytes and then close them, so as to avoid
	// try/finally snippet code in every parsing test
	// method
	private ByteArrayInputStream toByteArrayInputStreamAndClose(InputStream in) {
		try {
			byte[] bytes = IOUtils.toByteArray(in);
			return new ByteArrayInputStream(bytes);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			IOUtils.closeQuietly(in);
		}

	}

	private File createFile(String prefix) {
		File dir = new File(System.getProperty("user.home"), "/temp/sep");
		dir.mkdirs();
		String filename = prefix + System.currentTimeMillis() + ".xlsx";
		File file = new File(dir, filename);
		return file;
	}

	@SuppressWarnings("unused")
	private static class ITRecord {
		private static LinkedHashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		static {
			headerMap.put("primShort", "Primitive Short");
			headerMap.put("primInt", "Primitive Int");
			headerMap.put("primLong", "Primitive Long");
			headerMap.put("primFloat", "Primitive Float");
			headerMap.put("primDouble", "Primitive Double");
			headerMap.put("primBoolean", "Primitive Boolean");

			headerMap.put("objShort", "Object Short");
			headerMap.put("objInt", "Object Int");
			headerMap.put("objLong", "Object Long");
			headerMap.put("objFloat", "Object Float");
			headerMap.put("objDouble", "Object Double");
			headerMap.put("objBoolean", "Object Boolean");

			headerMap.put("bigInteger", "Big Integer");
			headerMap.put("bigDecimal", "Big Decimal");
			headerMap.put("str", "String");
			headerMap.put("dateStr", "Date String");
		}
		private short primShort;
		private int primInt;
		private long primLong;
		private float primFloat;
		private double primDouble;
		private boolean primBoolean;

		private Short objShort;
		private Integer objInt;
		private Long objLong;
		private Float objFloat;
		private Double objDouble;
		private Boolean objBoolean;

		private BigInteger bigInteger;
		private BigDecimal bigDecimal;

		private String str;
		private Date date;

		public static LinkedHashMap<String, String> getHeaderMap() {
			return new LinkedHashMap<String, String>(headerMap);
		}

		public static Map<String, String> getReverseHeaderMap() {
			LinkedHashMap<String, String> map = reverse(headerMap);
			map.remove("Date String");
			map.put("Date", "date");
			return map;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public String getDateStr() {
			if (date == null) {
				return null;
			}
			return DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss");
		}

		public void setDate(String s) {
			if (s == null) {
				return;
			}
			try {
				Date d = DateUtils.parseDate(s, new String[] { "yyyy-MM-dd HH:mm:ss" });
				this.setDate(d);
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}

		}

		public short getPrimShort() {
			return primShort;
		}

		public void setPrimShort(short primShort) {
			this.primShort = primShort;
		}

		public int getPrimInt() {
			return primInt;
		}

		public void setPrimInt(int primInt) {
			this.primInt = primInt;
		}

		public long getPrimLong() {
			return primLong;
		}

		public void setPrimLong(long primLong) {
			this.primLong = primLong;
		}

		public float getPrimFloat() {
			return primFloat;
		}

		public void setPrimFloat(float primFloat) {
			this.primFloat = primFloat;
		}

		public double getPrimDouble() {
			return primDouble;
		}

		public void setPrimDouble(double primDouble) {
			this.primDouble = primDouble;
		}

		public boolean isPrimBoolean() {
			return primBoolean;
		}

		public void setPrimBoolean(boolean primBoolean) {
			this.primBoolean = primBoolean;
		}

		public Short getObjShort() {
			return objShort;
		}

		public void setObjShort(Short objShort) {
			this.objShort = objShort;
		}

		public Integer getObjInt() {
			return objInt;
		}

		public void setObjInt(Integer objInt) {
			this.objInt = objInt;
		}

		public Long getObjLong() {
			return objLong;
		}

		public void setObjLong(Long objLong) {
			this.objLong = objLong;
		}

		public Float getObjFloat() {
			return objFloat;
		}

		public void setObjFloat(Float objFloat) {
			this.objFloat = objFloat;
		}

		public Double getObjDouble() {
			return objDouble;
		}

		public void setObjDouble(Double objDouble) {
			this.objDouble = objDouble;
		}

		public Boolean getObjBoolean() {
			return objBoolean;
		}

		public void setObjBoolean(Boolean objBoolean) {
			this.objBoolean = objBoolean;
		}

		public BigDecimal getBigDecimal() {
			return bigDecimal;
		}

		public void setBigDecimal(BigDecimal bigDecimal) {
			this.bigDecimal = bigDecimal;
		}

		public BigInteger getBigInteger() {
			return bigInteger;
		}

		public void setBigInteger(BigInteger bigInteger) {
			this.bigInteger = bigInteger;
		}

		public String getStr() {
			return str;
		}

		public void setStr(String str) {
			this.str = str;
		}

	}

	private static <K, V> LinkedHashMap<V, K> reverse(Map<K, V> origMap) {
		LinkedHashMap<V, K> newMap = new LinkedHashMap<V, K>();
		if (origMap == null) {
			origMap = new HashMap<K, V>();
		}
		for (Map.Entry<K, V> entry : origMap.entrySet())
			newMap.put(entry.getValue(), entry.getKey());
		return newMap;
	}

}
