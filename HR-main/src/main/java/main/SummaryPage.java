package main;

// iText PDF 및 ZIP 관련 import
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Apache POI (엑셀) 관련 import
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell; // 오류 해결을 위해 추가
import org.apache.poi.ss.usermodel.CellStyle; // 오류 해결을 위해 추가
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row; // 오류 해결을 위해 추가
import org.apache.poi.ss.usermodel.Sheet; // 오류 해결을 위해 추가
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// 기본 Java 및 Swing 관련 import
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class SummaryPage extends JPanel {
    private final PayrollManager payrollManager;
    private JTable payrollTable;
    private DefaultTableModel tableModel;

    private JComboBox<Integer> yearComboBox;
    private JComboBox<String> monthComboBox;
    private JButton viewButton;
    private JButton deleteButton;
    private JButton exportToExcelButton;
    private JButton exportPayslipPdfButton;
    private JButton exportAllPayslipsZipButton;

    private Integer lastQueriedYear = null;
    private Integer lastQueriedMonth = null;
    private JLabel employeeCountLabel;

    private final String[] columnNames = {
            "ID", "직원명", "총 급여(A)",
            "국민연금", "건강보험", "장기요양", "고용보험", "소득세", "지방소득세",
            "공제 총액(B)", "실지급액(A-B)"
    };
    private final int[] currencyColumnIndicesForSum = {2, 3, 4, 5, 6, 7, 8, 9, 10};
    private java.awt.Font 일반데이터폰트;
    private java.awt.Font 합계행폰트;


    public SummaryPage(PayrollManager payrollManager, PayrollApp payrollApp) {
        this.payrollManager = payrollManager;
        setLayout(new BorderLayout(10, 10));
        setBackground(java.awt.Color.WHITE);

        JPanel topOuterPanel = new JPanel(new BorderLayout());

        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        yearComboBox = new JComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear - 5; i <= currentYear + 1; i++) {
            yearComboBox.addItem(i);
        }
        yearComboBox.setSelectedItem(currentYear);

        String[] months = new String[12];
        for (int i = 0; i < 12; i++) {
            months[i] = String.format("%02d", i + 1);
        }
        monthComboBox = new JComboBox<>(months);
        monthComboBox.setSelectedItem(String.format("%02d", LocalDate.now().getMonthValue()));

        viewButton = new JButton("조회");
        deleteButton = new JButton("선택 행 삭제");
        exportToExcelButton = new JButton("엑셀로 내보내기");
        exportPayslipPdfButton = new JButton("선택직원 명세서(PDF)");
        exportAllPayslipsZipButton = new JButton("명세서 전체출력(ZIP)");

        java.awt.Font baseFontForEnlargement = viewButton.getFont();
        if (baseFontForEnlargement == null) baseFontForEnlargement = new JLabel().getFont();
        float newSize = baseFontForEnlargement.getSize() * 1.5f;
        java.awt.Font enlargedFont = baseFontForEnlargement.deriveFont(newSize);

        JLabel yearLabel = new JLabel("조회 연도:");
        yearLabel.setFont(enlargedFont);
        JLabel monthLabel = new JLabel("조회 월:");
        monthLabel.setFont(enlargedFont);

        yearComboBox.setFont(enlargedFont);
        monthComboBox.setFont(enlargedFont);
        viewButton.setFont(enlargedFont);
        deleteButton.setFont(enlargedFont);
        exportToExcelButton.setFont(enlargedFont);
        exportPayslipPdfButton.setFont(enlargedFont);
        exportAllPayslipsZipButton.setFont(enlargedFont);

        selectionPanel.add(yearLabel);
        selectionPanel.add(yearComboBox);
        selectionPanel.add(monthLabel);
        selectionPanel.add(monthComboBox);
        selectionPanel.add(viewButton);
        selectionPanel.add(deleteButton);
        selectionPanel.add(exportToExcelButton);
        selectionPanel.add(exportPayslipPdfButton);
        selectionPanel.add(exportAllPayslipsZipButton);
        topOuterPanel.add(selectionPanel, BorderLayout.WEST);

        employeeCountLabel = new JLabel("전체 0명", SwingConstants.RIGHT);
        java.awt.Font baseFont = employeeCountLabel.getFont();
        java.awt.Font countFont = baseFont.deriveFont(baseFont.getSize() * 1.3f);
        employeeCountLabel.setFont(countFont.deriveFont(java.awt.Font.BOLD));

        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        countPanel.add(employeeCountLabel);
        topOuterPanel.add(countPanel, BorderLayout.EAST);

        add(topOuterPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        payrollTable = new JTable(tableModel);
        payrollTable.setFillsViewportHeight(true);

        int defaultRowHeight = payrollTable.getRowHeight();
        payrollTable.setRowHeight((int)(defaultRowHeight * 1.5));

        java.awt.Font largerFontBase = baseFont.deriveFont(baseFont.getSize() * 1.3f);
        일반데이터폰트 = largerFontBase.deriveFont(java.awt.Font.PLAIN);
        합계행폰트 = largerFontBase.deriveFont(java.awt.Font.BOLD);

        payrollTable.setFont(일반데이터폰트);

        JTableHeader header = payrollTable.getTableHeader();
        if (header != null) {
            header.setFont(largerFontBase.deriveFont(java.awt.Font.BOLD));
        }

        payrollTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < payrollTable.getColumnCount(); i++) {
            int width = (i == 1) ? 120 : (i==0 ? 40 : 110);
            payrollTable.getColumnModel().getColumn(i).setPreferredWidth(width);
        }

        payrollTable.setDefaultRenderer(Object.class, new SummaryPageTableCellRenderer());

        JScrollPane scrollPane = new JScrollPane(payrollTable);
        add(scrollPane, BorderLayout.CENTER);

        addListeners();

        if(payrollManager != null) {
            try {
                int totalRegisteredEmployees = payrollManager.getAllEmployees().size();
                employeeCountLabel.setText("전체 " + totalRegisteredEmployees + "명");
            } catch (Exception e) {
                System.err.println("SummaryPage 초기화 중 직원 수 로드 오류: " + e.getMessage());
            }
        }
    }

    private void addListeners() {
        viewButton.addActionListener(e -> {
            this.lastQueriedYear = (Integer) yearComboBox.getSelectedItem();
            this.lastQueriedMonth = Integer.parseInt((String) monthComboBox.getSelectedItem());
            displayPayrollList(lastQueriedYear, lastQueriedMonth);
        });
        deleteButton.addActionListener(e -> deleteSelectedPayroll());
        exportToExcelButton.addActionListener(e -> exportTableToExcel());
        exportPayslipPdfButton.addActionListener(e -> exportSinglePayslipAsPdf());
        exportAllPayslipsZipButton.addActionListener(e -> exportPayslipsAsZip());
    }

    private void displayPayrollList(int year, int month) {
        tableModel.setRowCount(0);

        List<Payroll> periodPayrolls = payrollManager.getPayrollsForPeriod(year, month);
        int totalEmployees = payrollManager.getAllEmployees().size();
        employeeCountLabel.setText("조회된 인원: " + periodPayrolls.size() + "명 / 전체 " + totalEmployees + "명");

        if (periodPayrolls.isEmpty()) {
            JOptionPane.showMessageDialog(this, year + "년 " + month + "월에 확정된 급여 내역이 없습니다.", "정보 없음", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        periodPayrolls.sort(Comparator.comparing(p -> p.getEmployee().getName()));
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.KOREA);
        long[] columnTotals = new long[columnNames.length];
        AtomicInteger displayIdCounter = new AtomicInteger(1);

        for (Payroll payroll : periodPayrolls) {
            Object[] rowData = new Object[columnNames.length];
            rowData[0] = displayIdCounter.getAndIncrement();
            rowData[1] = payroll.getEmployee().getName();

            boolean isUnpaidLeave = payroll.getGrossPay() == 0 && payroll.getNetPay() == 0 && payroll.getTotalEmployeeDeduction() == 0;

            if (isUnpaidLeave) {
                for (int i = 2; i < rowData.length; i++) rowData[i] = "-";
                rowData[2] = "무급 휴직";
            } else {
                rowData[2] = currencyFormat.format(payroll.getGrossPay());
                rowData[3] = currencyFormat.format(Math.round(payroll.getNationalPensionEmployee()));
                rowData[4] = currencyFormat.format(Math.round(payroll.getHealthInsuranceEmployee()));
                rowData[5] = currencyFormat.format(Math.round(payroll.getLongTermCareInsuranceEmployee()));
                rowData[6] = currencyFormat.format(Math.round(payroll.getEmploymentInsuranceEmployee()));
                rowData[7] = currencyFormat.format(Math.round(payroll.getIncomeTax()));
                rowData[8] = currencyFormat.format(Math.round(payroll.getLocalIncomeTax()));
                rowData[9] = currencyFormat.format(Math.round(payroll.getTotalEmployeeDeduction()));
                rowData[10] = currencyFormat.format(Math.round(payroll.getNetPay()));
            }
            tableModel.addRow(rowData);

            if (!isUnpaidLeave) {
                columnTotals[2] += payroll.getGrossPay();
                columnTotals[3] += Math.round(payroll.getNationalPensionEmployee());
                columnTotals[4] += Math.round(payroll.getHealthInsuranceEmployee());
                columnTotals[5] += Math.round(payroll.getLongTermCareInsuranceEmployee());
                columnTotals[6] += Math.round(payroll.getEmploymentInsuranceEmployee());
                columnTotals[7] += Math.round(payroll.getIncomeTax());
                columnTotals[8] += Math.round(payroll.getLocalIncomeTax());
                columnTotals[9] += Math.round(payroll.getTotalEmployeeDeduction());
                columnTotals[10] += Math.round(payroll.getNetPay());
            }
        }
        addTotalsRow(columnTotals);
    }

    private void deleteSelectedPayroll() {
        int selectedRowView = payrollTable.getSelectedRow();
        if (selectedRowView == -1) { JOptionPane.showMessageDialog(this, "삭제할 행을 선택하세요.", "알림", JOptionPane.WARNING_MESSAGE); return; }
        int modelRow = payrollTable.convertRowIndexToModel(selectedRowView);
        if (tableModel.getValueAt(modelRow, 0).equals("합계")) { JOptionPane.showMessageDialog(this, "합계 행은 삭제할 수 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE); return; }
        if (lastQueriedYear == null || lastQueriedMonth == null) { JOptionPane.showMessageDialog(this, "먼저 데이터를 조회해주세요.", "알림", JOptionPane.WARNING_MESSAGE); return; }
        String employeeName = (String) tableModel.getValueAt(modelRow, 1);
        Optional<Employee> empOpt = payrollManager.getEmployeeByName(employeeName);
        if (empOpt.isEmpty()) { JOptionPane.showMessageDialog(this, "직원 정보를 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE); return; }

        int confirm = JOptionPane.showConfirmDialog(this, "직원 '" + employeeName + "'의 " + lastQueriedYear + "년 " + lastQueriedMonth + "월 급여 정보를 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (payrollManager.deletePayrollForPeriod(empOpt.get().getId(), lastQueriedYear, lastQueriedMonth)) {
                displayPayrollList(lastQueriedYear, lastQueriedMonth);
            } else { JOptionPane.showMessageDialog(this, "삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void addTotalsRow(long[] totals) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.KOREA);
        Object[] totalsRowData = new Object[columnNames.length];
        totalsRowData[0] = "합계";
        totalsRowData[1] = "";
        for (int index : currencyColumnIndicesForSum) {
            totalsRowData[index] = currencyFormat.format(totals[index]);
        }
        tableModel.addRow(totalsRowData);
    }

    public void refreshTableData() {
        if (this.lastQueriedYear != null && this.lastQueriedMonth != null) {
            displayPayrollList(this.lastQueriedYear, this.lastQueriedMonth);
        }
    }

    private void exportTableToExcel() {
        if (lastQueriedYear == null || lastQueriedMonth == null || tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "내보낼 데이터가 없습니다. 먼저 데이터를 조회해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("엑셀 파일로 저장");
        String defaultFileName = String.format("급여대장_%d년%02d월.xlsx", lastQueriedYear, lastQueriedMonth);
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel 통합 문서 (*.xlsx)", "xlsx"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".xlsx");
            }
            if (fileToSave.exists()) {
                int response = JOptionPane.showConfirmDialog(this, "이미 같은 이름의 파일이 존재합니다. 덮어쓰시겠습니까?", "파일 덮어쓰기 확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response == JOptionPane.NO_OPTION) return;
            }
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                createSummarySheet(workbook);
                createEmployerContributionSheet(workbook);
                try (FileOutputStream fileOut = new FileOutputStream(fileToSave)) {
                    workbook.write(fileOut);
                }
                JOptionPane.showMessageDialog(this, "엑셀 파일이 성공적으로 저장되었습니다:\n" + fileToSave.getAbsolutePath(), "저장 완료", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "엑셀 파일 생성 중 예기치 않은 오류가 발생했습니다: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void createSummarySheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("급여대장");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        DataFormat excelDataFormat = workbook.createDataFormat();
        CellStyle numberDataStyle = createNumberStyle(workbook, dataStyle, excelDataFormat, "#,##0");
        Row headerRow = sheet.createRow(0);
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(tableModel.getColumnName(col));
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < tableModel.getColumnCount(); j++) {
                Cell cell = row.createCell(j);
                Object value = tableModel.getValueAt(i, j);
                if (j >= 2 && value instanceof String && ((String) value).contains("₩")) {
                    try {
                        cell.setCellValue(Double.parseDouble(((String) value).replaceAll("[₩,]", "")));
                        cell.setCellStyle(numberDataStyle);
                    } catch (NumberFormatException e) {
                        cell.setCellValue((String) value);
                        cell.setCellStyle(dataStyle);
                    }
                } else if (value != null) {
                    cell.setCellValue(value.toString());
                    cell.setCellStyle(dataStyle);
                }
            }
        }
        for (int i = 0; i < tableModel.getColumnCount(); i++) sheet.autoSizeColumn(i);
    }

    private void createEmployerContributionSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("사업자 부담분");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        DataFormat excelDataFormat = workbook.createDataFormat();
        CellStyle numberStyle = createNumberStyle(workbook, dataStyle, excelDataFormat, "#,##0");
        String[] headers = {"직원명", "국민연금", "건강보험", "장기요양", "고용보험", "산재보험", "합계"};
        Row headerRow = sheet.createRow(0);
        for(int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        List<Payroll> payrolls = payrollManager.getPayrollsForPeriod(lastQueriedYear, lastQueriedMonth);
        payrolls.sort(Comparator.comparing(p -> p.getEmployee().getName()));
        int rowNum = 1;
        double[] totals = new double[headers.length - 1];
        for(Payroll p : payrolls) {
            if(p.getGrossPay() == 0) continue;
            Row row = sheet.createRow(rowNum++);
            double employerTotal = p.getNationalPensionEmployer() + p.getHealthInsuranceEmployer() + p.getLongTermCareInsuranceEmployer() + p.getEmploymentInsuranceEmployer() + p.getIndustrialAccidentInsuranceEmployer();
            row.createCell(0).setCellValue(p.getEmployee().getName());
            row.getCell(0).setCellStyle(dataStyle);
            createNumericCell(row, 1, p.getNationalPensionEmployer(), numberStyle);
            createNumericCell(row, 2, p.getHealthInsuranceEmployer(), numberStyle);
            createNumericCell(row, 3, p.getLongTermCareInsuranceEmployer(), numberStyle);
            createNumericCell(row, 4, p.getEmploymentInsuranceEmployer(), numberStyle);
            createNumericCell(row, 5, p.getIndustrialAccidentInsuranceEmployer(), numberStyle);
            createNumericCell(row, 6, employerTotal, numberStyle);
            totals[0] += p.getNationalPensionEmployer();
            totals[1] += p.getHealthInsuranceEmployer();
            totals[2] += p.getLongTermCareInsuranceEmployer();
            totals[3] += p.getEmploymentInsuranceEmployer();
            totals[4] += p.getIndustrialAccidentInsuranceEmployer();
            totals[5] += employerTotal;
        }
        Row totalRow = sheet.createRow(rowNum);
        CellStyle totalStyle = createNumberStyle(workbook, dataStyle, excelDataFormat, "#,##0");
        totalStyle.setFont(createBoldFont(workbook));
        totalStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalRow.createCell(0).setCellValue("총계");
        totalRow.getCell(0).setCellStyle(totalStyle);
        for (int i=0; i < totals.length; i++) {
            createNumericCell(totalRow, i + 1, totals[i], totalStyle);
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void exportSinglePayslipAsPdf() {
        int selectedRow = payrollTable.getSelectedRow();
        if (selectedRow < 0 || tableModel.getValueAt(selectedRow, 0).equals("합계")) {
            JOptionPane.showMessageDialog(this, "테이블에서 명세서를 출력할 직원을 선택하세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (lastQueriedYear == null || lastQueriedMonth == null) {
            JOptionPane.showMessageDialog(this, "먼저 데이터를 조회해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String employeeName = (String) tableModel.getValueAt(selectedRow, 1);
        Optional<Employee> empOpt = payrollManager.getEmployeeByName(employeeName);
        if (empOpt.isEmpty()) { return; }
        List<Payroll> payrolls = payrollManager.getPayrollsForPeriod(lastQueriedYear, lastQueriedMonth);
        Optional<Payroll> payrollOpt = payrolls.stream()
                .filter(p -> p.getEmployeeId() == empOpt.get().getId())
                .findFirst();
        if (payrollOpt.isEmpty()) {
            JOptionPane.showMessageDialog(this, "선택한 직원의 해당 월 급여 데이터가 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("급여명세서 PDF로 저장");
        fileChooser.setSelectedFile(new File(String.format("%d년%02d월_%s_급여명세서.pdf", lastQueriedYear, lastQueriedMonth, employeeName)));
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF 문서 (*.pdf)", "pdf"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File pdfFile = fileChooser.getSelectedFile();
            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                ByteArrayOutputStream baos = createPayslipPdfInMemory(payrollOpt.get());
                baos.writeTo(fos);
                JOptionPane.showMessageDialog(this, "급여명세서 PDF 파일이 성공적으로 저장되었습니다.", "저장 완료", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "PDF 파일 생성 중 오류 발생: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void exportPayslipsAsZip() {
        if (lastQueriedYear == null || lastQueriedMonth == null) {
            JOptionPane.showMessageDialog(this, "먼저 데이터를 조회해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Payroll> payrolls = payrollManager.getPayrollsForPeriod(lastQueriedYear, lastQueriedMonth);
        if (payrolls.isEmpty()) {
            JOptionPane.showMessageDialog(this, "내보낼 급여명세서가 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("급여명세서 ZIP 파일로 저장");
        fileChooser.setSelectedFile(new File(String.format("%d년%02d월_급여명세서.zip", lastQueriedYear, lastQueriedMonth)));
        fileChooser.setFileFilter(new FileNameExtensionFilter("ZIP 압축 파일 (*.zip)", "zip"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File zipFile = fileChooser.getSelectedFile();
            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                for (Payroll payroll : payrolls) {
                    if (payroll.getGrossPay() == 0) continue;
                    ByteArrayOutputStream pdfBaos = createPayslipPdfInMemory(payroll);
                    ZipEntry zipEntry = new ZipEntry(String.format("%s_급여명세서.pdf", payroll.getEmployee().getName()));
                    zos.putNextEntry(zipEntry);
                    zos.write(pdfBaos.toByteArray());
                    zos.closeEntry();
                }
                JOptionPane.showMessageDialog(this, "급여명세서 ZIP 파일이 성공적으로 저장되었습니다.", "저장 완료", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "파일 생성 중 오류 발생: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private ByteArrayOutputStream createPayslipPdfInMemory(Payroll payroll) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(50, 50, 50, 50);
        String FONT_PATH = "src/main/resources/fonts/NotoSansKR-Regular.ttf";
        PdfFont koreanFont = PdfFontFactory.createFont(FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        document.setFont(koreanFont).setFontSize(10);
        NumberFormat nf = NumberFormat.getIntegerInstance();

        document.add(new Paragraph(String.format("%d년 %02d월 급여명세서", lastQueriedYear, lastQueriedMonth))
                .setFontSize(20).setBold().setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("\n"));

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2}));
        infoTable.setWidth(UnitValue.createPercentValue(100));
        infoTable.addCell(createCell("성명:", true));
        infoTable.addCell(createCell(payroll.getEmployee().getName(), false));
        infoTable.addCell(createCell("부서:", true));
        infoTable.addCell(createCell(payroll.getEmployee().getDepartment(), false));
        document.add(infoTable);
        document.add(new Paragraph("\n"));

        Table mainTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        mainTable.setWidth(UnitValue.createPercentValue(100));
        mainTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("지급 내역")).setBold().setFontSize(12));
        mainTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("공제 내역")).setBold().setFontSize(12));
        Table earningsTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        addEarningItem(earningsTable, "기본급", payroll.getMonthlyBasicSalary(), nf);
        addEarningItem(earningsTable, "고정연장수당", payroll.getFixedOvertimeAllowance(), nf);
        addEarningItem(earningsTable, "추가 연장 수당", payroll.getAdditionalOvertimePremium(), nf);
        addEarningItem(earningsTable, "상여금", payroll.getBonus(), nf);
        addEarningItem(earningsTable, "기타수당", payroll.getOtherAllowance(), nf);
        addEarningItem(earningsTable, "식대", payroll.getMealAllowance(), nf);
        addEarningItem(earningsTable, "차량유지비", payroll.getVehicleMaintenanceFee(), nf);
        addEarningItem(earningsTable, "연구개발비", payroll.getResearchDevelopmentExpense(), nf);
        addEarningItem(earningsTable, "육아수당", payroll.getChildcareAllowance(), nf);
        earningsTable.addCell(createCell("총 지급액", true));
        earningsTable.addCell(createCell(nf.format(payroll.getGrossPay()), true, TextAlignment.RIGHT));
        mainTable.addCell(new com.itextpdf.layout.element.Cell().add(earningsTable).setBorder(null));
        Table deductionsTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        deductionsTable.addCell(createCell("국민연금", false));
        deductionsTable.addCell(createCell(nf.format(Math.round(payroll.getNationalPensionEmployee())), false, TextAlignment.RIGHT));
        deductionsTable.addCell(createCell("건강보험", false));
        deductionsTable.addCell(createCell(nf.format(Math.round(payroll.getHealthInsuranceEmployee())), false, TextAlignment.RIGHT));
        deductionsTable.addCell(createCell("장기요양보험", false));
        deductionsTable.addCell(createCell(nf.format(Math.round(payroll.getLongTermCareInsuranceEmployee())), false, TextAlignment.RIGHT));
        deductionsTable.addCell(createCell("고용보험", false));
        deductionsTable.addCell(createCell(nf.format(Math.round(payroll.getEmploymentInsuranceEmployee())), false, TextAlignment.RIGHT));
        deductionsTable.addCell(createCell("소득세", false));
        deductionsTable.addCell(createCell(nf.format(Math.round(payroll.getIncomeTax())), false, TextAlignment.RIGHT));
        deductionsTable.addCell(createCell("지방소득세", false));
        deductionsTable.addCell(createCell(nf.format(Math.round(payroll.getLocalIncomeTax())), false, TextAlignment.RIGHT));
        deductionsTable.addCell(createCell("공제 총액", true));
        deductionsTable.addCell(createCell(nf.format(Math.round(payroll.getTotalEmployeeDeduction())), true, TextAlignment.RIGHT));
        mainTable.addCell(new com.itextpdf.layout.element.Cell().add(deductionsTable).setBorder(null));
        document.add(mainTable);
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("실 지급액: " + nf.format(Math.round(payroll.getNetPay())) + " 원")
                .setFontSize(14).setBold().setTextAlignment(TextAlignment.RIGHT));
        document.close();
        return baos;
    }

    private void addEarningItem(Table table, String title, double value, NumberFormat nf) {
        if (value > 0) {
            table.addCell(createCell(title, false));
            table.addCell(createCell(nf.format(value), false, TextAlignment.RIGHT));
        }
    }

    private com.itextpdf.layout.element.Cell createCell(String content, boolean isBold, TextAlignment alignment) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell().add(new Paragraph(content));
        if (isBold) cell.setBold();
        cell.setTextAlignment(alignment);
        cell.setBorder(null);
        return cell;
    }

    private com.itextpdf.layout.element.Cell createCell(String content, boolean isBold) {
        return createCell(content, isBold, TextAlignment.LEFT);
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(createBoldFont(workbook));
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        setBorders(style, BorderStyle.DOTTED);
        return style;
    }

    private CellStyle createNumberStyle(XSSFWorkbook workbook, CellStyle baseStyle, DataFormat format, String formatString) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        style.setDataFormat(format.getFormat(formatString));
        return style;
    }

    private org.apache.poi.ss.usermodel.Font createBoldFont(XSSFWorkbook workbook) {
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        return font;
    }

    private void setBorders(CellStyle style, BorderStyle borderStyle) {
        style.setBorderTop(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
    }

    private void createNumericCell(org.apache.poi.ss.usermodel.Row row, int index, double value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    class SummaryPageTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(일반데이터폰트);
            c.setForeground(table.getForeground());
            c.setBackground(isSelected ? table.getSelectionBackground() : new java.awt.Color(255, 255, 255));
            if (table.getRowCount() > 0 && row < table.getRowCount()) {
                Object idValue = table.getValueAt(row, 0);
                if (idValue != null && idValue.equals("합계")) {
                    c.setFont(합계행폰트);
                    c.setBackground(new java.awt.Color(230, 230, 230));
                    if(isSelected) c.setBackground(table.getSelectionBackground().darker());
                }
            }
            if (column >= 2) { setHorizontalAlignment(JLabel.RIGHT); }
            else { setHorizontalAlignment(JLabel.CENTER); }
            return c;
        }
    }
}