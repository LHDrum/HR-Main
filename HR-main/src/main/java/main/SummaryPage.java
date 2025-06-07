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
import java.math.BigDecimal;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Apache POI (엑셀) 관련 import
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
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
        payrollTable.setRowHeight((int) (defaultRowHeight * 1.5));

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
            int width = (i == 1) ? 120 : (i == 0 ? 40 : 110);
            payrollTable.getColumnModel().getColumn(i).setPreferredWidth(width);
        }

        payrollTable.setDefaultRenderer(Object.class, new SummaryPageTableCellRenderer());

        JScrollPane scrollPane = new JScrollPane(payrollTable);
        add(scrollPane, BorderLayout.CENTER);

        addListeners();

        if (payrollManager != null) {
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

        BigDecimal[] columnTotals = new BigDecimal[columnNames.length];
        for (int i = 0; i < columnTotals.length; i++) {
            columnTotals[i] = BigDecimal.ZERO;
        }

        AtomicInteger displayIdCounter = new AtomicInteger(1);

        for (Payroll payroll : periodPayrolls) {
            Object[] rowData = new Object[columnNames.length];
            rowData[0] = displayIdCounter.getAndIncrement();
            rowData[1] = payroll.getEmployee().getName();

            boolean isUnpaidLeave = payroll.getGrossPay().compareTo(BigDecimal.ZERO) == 0 &&
                    payroll.getNetPay().compareTo(BigDecimal.ZERO) == 0 &&
                    payroll.getTotalEmployeeDeduction().compareTo(BigDecimal.ZERO) == 0;

            if (isUnpaidLeave) {
                rowData[2] = "무급 휴직";
                for (int i = 3; i < rowData.length; i++) rowData[i] = "-";
            } else {
                DecimalFormat df = new DecimalFormat("#,###");
                rowData[2] = df.format(payroll.getGrossPay());
                rowData[3] = df.format(payroll.getNationalPensionEmployee());
                rowData[4] = df.format(payroll.getHealthInsuranceEmployee());
                rowData[5] = df.format(payroll.getLongTermCareInsuranceEmployee());
                rowData[6] = df.format(payroll.getEmploymentInsuranceEmployee());
                rowData[7] = df.format(payroll.getIncomeTax());
                rowData[8] = df.format(payroll.getLocalIncomeTax());
                rowData[9] = df.format(payroll.getTotalEmployeeDeduction());
                rowData[10] = df.format(payroll.getNetPay());

                columnTotals[2] = columnTotals[2].add(payroll.getGrossPay());
                columnTotals[3] = columnTotals[3].add(payroll.getNationalPensionEmployee());
                columnTotals[4] = columnTotals[4].add(payroll.getHealthInsuranceEmployee());
                columnTotals[5] = columnTotals[5].add(payroll.getLongTermCareInsuranceEmployee());
                columnTotals[6] = columnTotals[6].add(payroll.getEmploymentInsuranceEmployee());
                columnTotals[7] = columnTotals[7].add(payroll.getIncomeTax());
                columnTotals[8] = columnTotals[8].add(payroll.getLocalIncomeTax());
                columnTotals[9] = columnTotals[9].add(payroll.getTotalEmployeeDeduction());
                columnTotals[10] = columnTotals[10].add(payroll.getNetPay());
            }
            tableModel.addRow(rowData);
        }
        addTotalsRow(columnTotals);
    }

    private void deleteSelectedPayroll() {
        int selectedRowView = payrollTable.getSelectedRow();
        if (selectedRowView == -1) {
            JOptionPane.showMessageDialog(this, "삭제할 행을 선택하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = payrollTable.convertRowIndexToModel(selectedRowView);
        if (tableModel.getValueAt(modelRow, 0).equals("합계")) {
            JOptionPane.showMessageDialog(this, "합계 행은 삭제할 수 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (lastQueriedYear == null || lastQueriedMonth == null) {
            JOptionPane.showMessageDialog(this, "먼저 데이터를 조회해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String employeeName = (String) tableModel.getValueAt(modelRow, 1);
        Optional<Employee> empOpt = payrollManager.getEmployeeByName(employeeName);
        if (empOpt.isEmpty()) {
            JOptionPane.showMessageDialog(this, "직원 정보를 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "직원 '" + employeeName + "'의 " + lastQueriedYear + "년 " + lastQueriedMonth + "월 급여 정보를 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (payrollManager.deletePayrollForPeriod(empOpt.get().getId(), lastQueriedYear, lastQueriedMonth)) {
                displayPayrollList(lastQueriedYear, lastQueriedMonth);
            } else {
                JOptionPane.showMessageDialog(this, "삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addTotalsRow(BigDecimal[] totals) {
        DecimalFormat df = new DecimalFormat("#,###");
        Object[] totalsRowData = new Object[columnNames.length];
        totalsRowData[0] = "합계";
        totalsRowData[1] = "";
        for (int index : currencyColumnIndicesForSum) {
            totalsRowData[index] = df.format(totals[index]);
        }
        tableModel.addRow(totalsRowData);
    }

    public void refreshTableData() {
        if (this.lastQueriedYear != null && this.lastQueriedMonth != null) {
            displayPayrollList(this.lastQueriedYear, this.lastQueriedMonth);
        }
    }

    private void exportTableToExcel() {
        if (lastQueriedYear == null || lastQueriedMonth == null || tableModel.getRowCount() <= 1) {
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
                List<Payroll> payrolls = payrollManager.getPayrollsForPeriod(lastQueriedYear, lastQueriedMonth);
                payrolls.sort(Comparator.comparing(p -> p.getEmployee().getName()));

                createDetailedPayrollLedgerSheet(workbook, payrolls); // 상세 급여대장 시트 생성
                createEmployerContributionSheet(workbook, payrolls);  // 사업자 부담분 시트 생성

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

    private void createDetailedPayrollLedgerSheet(XSSFWorkbook workbook, List<Payroll> payrolls) {
        Sheet sheet = workbook.createSheet("상세급여대장");
        CellStyle headerStyle = createHeaderStyle(workbook);
        DataFormat excelDataFormat = workbook.createDataFormat();
        CellStyle textStyle = createDataStyle(workbook, HorizontalAlignment.CENTER);
        CellStyle currencyStyle = createNumberStyle(workbook, excelDataFormat, "#,##0");

        String[] headers = {
                "No.", "성명", "부서", "기본급", "고정연장수당", "추가수당", "상여금", "기타수당", "식대", "차량유지비", "연구개발비", "육아수당", "지급총액",
                "국민연금", "건강보험", "장기요양", "고용보험", "소득세", "지방소득세", "공제총액", "실지급액"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        BigDecimal[] totals = new BigDecimal[headers.length];
        for(int i=0; i<totals.length; i++) totals[i] = BigDecimal.ZERO;

        for (Payroll p : payrolls) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, rowNum - 1, textStyle);
            createCell(row, 1, p.getEmployee().getName(), textStyle);
            createCell(row, 2, p.getEmployee().getDepartment(), textStyle);

            if(p.getGrossPay().compareTo(BigDecimal.ZERO) == 0) {
                createCell(row, 12, "무급 휴직", textStyle);
            } else {
                createNumericCell(row, 3, p.getMonthlyBasicSalary(), currencyStyle);
                createNumericCell(row, 4, p.getFixedOvertimeAllowance(), currencyStyle);
                createNumericCell(row, 5, p.getAdditionalOvertimePremium(), currencyStyle);
                createNumericCell(row, 6, p.getBonus(), currencyStyle);
                createNumericCell(row, 7, p.getOtherAllowance(), currencyStyle);
                createNumericCell(row, 8, p.getMealAllowance(), currencyStyle);
                createNumericCell(row, 9, p.getVehicleMaintenanceFee(), currencyStyle);
                createNumericCell(row, 10, p.getResearchDevelopmentExpense(), currencyStyle);
                createNumericCell(row, 11, p.getChildcareAllowance(), currencyStyle);
                createNumericCell(row, 12, p.getGrossPay(), currencyStyle);
                createNumericCell(row, 13, p.getNationalPensionEmployee(), currencyStyle);
                createNumericCell(row, 14, p.getHealthInsuranceEmployee(), currencyStyle);
                createNumericCell(row, 15, p.getLongTermCareInsuranceEmployee(), currencyStyle);
                createNumericCell(row, 16, p.getEmploymentInsuranceEmployee(), currencyStyle);
                createNumericCell(row, 17, p.getIncomeTax(), currencyStyle);
                createNumericCell(row, 18, p.getLocalIncomeTax(), currencyStyle);
                createNumericCell(row, 19, p.getTotalEmployeeDeduction(), currencyStyle);
                createNumericCell(row, 20, p.getNetPay(), currencyStyle);

                // 합계 계산
                totals[3] = totals[3].add(p.getMonthlyBasicSalary());
                totals[4] = totals[4].add(p.getFixedOvertimeAllowance());
                totals[5] = totals[5].add(p.getAdditionalOvertimePremium());
                totals[6] = totals[6].add(p.getBonus());
                totals[7] = totals[7].add(p.getOtherAllowance());
                totals[8] = totals[8].add(p.getMealAllowance());
                totals[9] = totals[9].add(p.getVehicleMaintenanceFee());
                totals[10] = totals[10].add(p.getResearchDevelopmentExpense());
                totals[11] = totals[11].add(p.getChildcareAllowance());
                totals[12] = totals[12].add(p.getGrossPay());
                totals[13] = totals[13].add(p.getNationalPensionEmployee());
                totals[14] = totals[14].add(p.getHealthInsuranceEmployee());
                totals[15] = totals[15].add(p.getLongTermCareInsuranceEmployee());
                totals[16] = totals[16].add(p.getEmploymentInsuranceEmployee());
                totals[17] = totals[17].add(p.getIncomeTax());
                totals[18] = totals[18].add(p.getLocalIncomeTax());
                totals[19] = totals[19].add(p.getTotalEmployeeDeduction());
                totals[20] = totals[20].add(p.getNetPay());
            }
        }

        CellStyle totalStyle = createNumberStyle(workbook, excelDataFormat, "#,##0");
        totalStyle.setFont(createBoldFont(workbook));
        totalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row totalRow = sheet.createRow(rowNum);
        createCell(totalRow, 0, "합계", totalStyle);
        createCell(totalRow, 1, "", totalStyle);
        createCell(totalRow, 2, "", totalStyle);
        for(int i=3; i<totals.length; i++) {
            createNumericCell(totalRow, i, totals[i], totalStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createEmployerContributionSheet(XSSFWorkbook workbook, List<Payroll> payrolls) {
        Sheet sheet = workbook.createSheet("사업자 부담분");
        CellStyle headerStyle = createHeaderStyle(workbook);
        DataFormat excelDataFormat = workbook.createDataFormat();
        CellStyle textStyle = createDataStyle(workbook, HorizontalAlignment.CENTER);
        CellStyle currencyStyle = createNumberStyle(workbook, excelDataFormat, "#,##0");

        String[] headers = {"직원명", "국민연금", "건강보험", "장기요양", "고용보험", "산재보험", "합계"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        BigDecimal[] totals = new BigDecimal[headers.length];
        for(int i=0; i<totals.length; i++) totals[i] = BigDecimal.ZERO;

        for (Payroll p : payrolls) {
            if (p.getGrossPay().compareTo(BigDecimal.ZERO) == 0) continue;

            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, p.getEmployee().getName(), textStyle);
            createNumericCell(row, 1, p.getNationalPensionEmployer(), currencyStyle);
            createNumericCell(row, 2, p.getHealthInsuranceEmployer(), currencyStyle);
            createNumericCell(row, 3, p.getLongTermCareInsuranceEmployer(), currencyStyle);
            createNumericCell(row, 4, p.getEmploymentInsuranceEmployer(), currencyStyle);
            createNumericCell(row, 5, p.getIndustrialAccidentInsuranceEmployer(), currencyStyle);

            BigDecimal employerTotal = p.getTotalEmployerDeduction();
            createNumericCell(row, 6, employerTotal, currencyStyle);

            totals[1] = totals[1].add(p.getNationalPensionEmployer());
            totals[2] = totals[2].add(p.getHealthInsuranceEmployer());
            totals[3] = totals[3].add(p.getLongTermCareInsuranceEmployer());
            totals[4] = totals[4].add(p.getEmploymentInsuranceEmployer());
            totals[5] = totals[5].add(p.getIndustrialAccidentInsuranceEmployer());
            totals[6] = totals[6].add(employerTotal);
        }

        CellStyle totalStyle = createNumberStyle(workbook, excelDataFormat, "#,##0");
        totalStyle.setFont(createBoldFont(workbook));
        totalStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row totalRow = sheet.createRow(rowNum);
        createCell(totalRow, 0, "총계", totalStyle);
        for (int i = 1; i < totals.length; i++) {
            createNumericCell(totalRow, i, totals[i], totalStyle);
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
        if (empOpt.isEmpty()) return;

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
                    if (payroll.getGrossPay().compareTo(BigDecimal.ZERO) == 0) continue;
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
        DecimalFormat df = new DecimalFormat("#,###");

        document.add(new Paragraph(String.format("%d년 %02d월 급여명세서", lastQueriedYear, lastQueriedMonth))
                .setFontSize(20).setBold().setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("\n"));

        // 사용자 정보 테이블
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2}));
        infoTable.setWidth(UnitValue.createPercentValue(100));
        infoTable.addCell(createPdfCell("성명:", true));
        infoTable.addCell(createPdfCell(payroll.getEmployee().getName(), false));
        infoTable.addCell(createPdfCell("부서:", true));
        infoTable.addCell(createPdfCell(payroll.getEmployee().getDepartment(), false));
        document.add(infoTable);

        // [수정] 입사일, 근무일수, 계산방식 등 추가 정보 테이블
        Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{1, 5}));
        detailsTable.setWidth(UnitValue.createPercentValue(100)).setMarginTop(5);

        LocalDate hireDate = payroll.getEmployee().getHireDate();
        detailsTable.addCell(createPdfCell("입사일:", true));
        detailsTable.addCell(createPdfCell(hireDate != null ? hireDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "-", false));

        YearMonth period = YearMonth.of(lastQueriedYear, lastQueriedMonth);
        long totalWeekdays = 0;
        for(int i=1; i<=period.lengthOfMonth(); i++) {
            DayOfWeek dow = period.atDay(i).getDayOfWeek();
            if(dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                totalWeekdays++;
            }
        }
        int absenceDays = payroll.getUnauthorizedAbsenceDays() + payroll.getUnpaidDays();
        detailsTable.addCell(createPdfCell("근무 정보:", true));
        detailsTable.addCell(createPdfCell(String.format("총 평일 %d일, 실근무 %d일 (결근/무급 %d일)", totalWeekdays, totalWeekdays - absenceDays, absenceDays), false));

        detailsTable.addCell(createPdfCell("급여 산정:", true));
        detailsTable.addCell(createPdfCell("월 총 근로시간 224시간 기준 (기본 209시간 + 고정연장 15시간)", false));
        document.add(detailsTable);
        document.add(new Paragraph("\n"));

        // 지급/공제 테이블
        Table mainTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        mainTable.setWidth(UnitValue.createPercentValue(100));
        mainTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("지급 내역")).setBold().setFontSize(12));
        mainTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("공제 내역")).setBold().setFontSize(12));

        Table earningsTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        addEarningItem(earningsTable, "기본급", payroll.getMonthlyBasicSalary(), df);
        addEarningItem(earningsTable, "고정연장수당", payroll.getFixedOvertimeAllowance(), df);
        addEarningItem(earningsTable, "추가수당", payroll.getAdditionalOvertimePremium(), df);
        addEarningItem(earningsTable, "상여금", payroll.getBonus(), df);
        addEarningItem(earningsTable, "기타수당", payroll.getOtherAllowance(), df);
        addEarningItem(earningsTable, "식대", payroll.getMealAllowance(), df);
        addEarningItem(earningsTable, "차량유지비", payroll.getVehicleMaintenanceFee(), df);
        addEarningItem(earningsTable, "연구개발비", payroll.getResearchDevelopmentExpense(), df);
        addEarningItem(earningsTable, "육아수당", payroll.getChildcareAllowance(), df);
        earningsTable.addCell(createPdfCell("총 지급액", true));
        earningsTable.addCell(createPdfCell(df.format(payroll.getGrossPay()), true, TextAlignment.RIGHT));

        mainTable.addCell(new com.itextpdf.layout.element.Cell().add(earningsTable).setBorder(null));

        Table deductionsTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        addEarningItem(deductionsTable, "국민연금", payroll.getNationalPensionEmployee(), df);
        addEarningItem(deductionsTable, "건강보험", payroll.getHealthInsuranceEmployee(), df);
        addEarningItem(deductionsTable, "장기요양보험", payroll.getLongTermCareInsuranceEmployee(), df);
        addEarningItem(deductionsTable, "고용보험", payroll.getEmploymentInsuranceEmployee(), df);
        addEarningItem(deductionsTable, "소득세", payroll.getIncomeTax(), df);
        addEarningItem(deductionsTable, "지방소득세", payroll.getLocalIncomeTax(), df);
        deductionsTable.addCell(createPdfCell("공제 총액", true));
        deductionsTable.addCell(createPdfCell(df.format(payroll.getTotalEmployeeDeduction()), true, TextAlignment.RIGHT));

        mainTable.addCell(new com.itextpdf.layout.element.Cell().add(deductionsTable).setBorder(null));

        document.add(mainTable);
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("실 지급액: " + df.format(payroll.getNetPay()) + " 원")
                .setFontSize(14).setBold().setTextAlignment(TextAlignment.RIGHT));
        document.close();

        return baos;
    }

    private void addEarningItem(Table table, String title, BigDecimal value, DecimalFormat df) {
        if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
            table.addCell(createPdfCell(title, false));
            table.addCell(createPdfCell(df.format(value), false, TextAlignment.RIGHT));
        }
    }

    private com.itextpdf.layout.element.Cell createPdfCell(String content, boolean isBold, TextAlignment alignment) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell().add(new Paragraph(content));
        if (isBold) cell.setBold();
        cell.setTextAlignment(alignment);
        cell.setBorder(null);
        cell.setPadding(2);
        return cell;
    }

    private com.itextpdf.layout.element.Cell createPdfCell(String content, boolean isBold) {
        return createPdfCell(content, isBold, TextAlignment.LEFT);
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

    private CellStyle createDataStyle(XSSFWorkbook workbook, HorizontalAlignment alignment) {
        CellStyle style = workbook.createCellStyle();
        setBorders(style, BorderStyle.DOTTED);
        style.setAlignment(alignment);
        return style;
    }

    private CellStyle createNumberStyle(XSSFWorkbook workbook, DataFormat format, String formatString) {
        CellStyle style = createDataStyle(workbook, HorizontalAlignment.RIGHT);
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

    private void createCell(Row row, int colNum, String value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int colNum, int value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createNumericCell(Row row, int colNum, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(0);
        }
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
                    if (isSelected) c.setBackground(table.getSelectionBackground().darker());
                }
            }
            if (column >= 2) {
                setHorizontalAlignment(JLabel.RIGHT);
            } else {
                setHorizontalAlignment(JLabel.CENTER);
            }
            return c;
        }
    }
}