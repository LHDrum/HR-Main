package main;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*; // java.awt.Font를 기본 Font로 사용
// import org.apache.poi.ss.usermodel.Font; // 이 라인을 삭제하거나 주석 처리합니다.
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

// Apache POI 라이브러리 import (Font 제외)
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// POI 용 Font는 필요할 때 전체 경로로 명시


public class SummaryPage extends JPanel {
    private PayrollManager payrollManager;
    private JTable payrollTable;
    private DefaultTableModel tableModel;
    private PayrollApp payrollApp;

    private JComboBox<Integer> yearComboBox;
    private JComboBox<String> monthComboBox;
    private JButton viewButton;
    private JButton deleteButton;
    private JButton exportToExcelButton;

    private Integer lastQueriedYear = null;
    private Integer lastQueriedMonth = null;
    private JLabel employeeCountLabel;

    private final String[] columnNames = {
            "ID", "직원명", "월 기본급",
            "고정연장수당", "추가 연장 수당", "상여금", "기타수당", "식대",
            "차량유지비", "연구개발비", "육아수당",
            "총 급여"
    };
    private final int[] currencyColumnIndicesForSum = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private java.awt.Font 일반데이터폰트;
    private java.awt.Font 합계행폰트;


    public SummaryPage(PayrollManager payrollManager, PayrollApp payrollApp) {
        this.payrollManager = payrollManager;
        this.payrollApp = payrollApp;
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

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
        viewButton.addActionListener(e -> {
            Integer selectedYear = (Integer) yearComboBox.getSelectedItem();
            String selectedMonthStr = (String) monthComboBox.getSelectedItem();
            if (selectedYear != null && selectedMonthStr != null) {
                try {
                    int selectedMonth = Integer.parseInt(selectedMonthStr);
                    this.lastQueriedYear = selectedYear;
                    this.lastQueriedMonth = selectedMonth;
                    displayInitialEmployeeList(selectedYear, selectedMonth);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "월 형식이 올바르지 않습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        deleteButton = new JButton("선택 행 삭제");
        deleteButton.addActionListener(e -> deleteSelectedPayroll());

        exportToExcelButton = new JButton("엑셀로 내보내기");
        exportToExcelButton.addActionListener(e -> exportTableToExcel());

        // --- 폰트 크기 조정 로직 ---
        java.awt.Font baseFontForEnlargement = viewButton.getFont();
        if (baseFontForEnlargement == null && yearComboBox.getFont() != null) {
            baseFontForEnlargement = yearComboBox.getFont();
        } else if (baseFontForEnlargement == null) {
            baseFontForEnlargement = new JLabel().getFont();
        }

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
        // --- 폰트 크기 조정 로직 끝 ---

        selectionPanel.add(yearLabel);
        selectionPanel.add(yearComboBox);
        selectionPanel.add(monthLabel);
        selectionPanel.add(monthComboBox);
        selectionPanel.add(viewButton);
        selectionPanel.add(deleteButton);
        selectionPanel.add(exportToExcelButton);
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
                if (getRowCount() > 0 && row == getRowCount() -1 && getValueAt(row,0) != null && getValueAt(row,0).equals("합계")) {
                    return false;
                }
                return false;
            }
        };
        payrollTable = new JTable(tableModel);
        payrollTable.setFillsViewportHeight(true);

        int defaultRowHeight = payrollTable.getRowHeight();
        payrollTable.setRowHeight((int)(defaultRowHeight * 1.5));

        // 여기의 baseFont는 employeeCountLabel에서 가져온 java.awt.Font임
        java.awt.Font largerFontBase = baseFont.deriveFont(baseFont.getSize() * 1.3f);
        일반데이터폰트 = largerFontBase.deriveFont(java.awt.Font.PLAIN);
        합계행폰트 = largerFontBase.deriveFont(java.awt.Font.BOLD);

        payrollTable.setFont(일반데이터폰트);

        JTableHeader header = payrollTable.getTableHeader();
        if (header != null) {
            header.setFont(largerFontBase.deriveFont(java.awt.Font.BOLD));
        }

        TableColumn idColumn = payrollTable.getColumnModel().getColumn(0);
        idColumn.setPreferredWidth(50);
        TableColumn nameColumn = payrollTable.getColumnModel().getColumn(1);
        nameColumn.setPreferredWidth(120);
        for (int i = 2; i < payrollTable.getColumnCount(); i++) {
            payrollTable.getColumnModel().getColumn(i).setPreferredWidth(130);
        }

        payrollTable.setDefaultRenderer(Object.class, new SummaryPageTableCellRenderer());

        JScrollPane scrollPane = new JScrollPane(payrollTable);
        add(scrollPane, BorderLayout.CENTER);

        tableModel.setRowCount(0);
        if(payrollManager != null) {
            try { // payrollManager가 초기화되지 않은 상태에서 호출될 가능성 방지
                int totalRegisteredEmployees = payrollManager.getAllEmployees().size();
                employeeCountLabel.setText("전체 " + totalRegisteredEmployees + "명");
            } catch (Exception e) {
                System.err.println("SummaryPage 초기화 중 직원 수 로드 오류: " + e.getMessage());
                employeeCountLabel.setText("전체 -명");
            }
        }
    }

    private void displayInitialEmployeeList(int year, int month) {
        tableModel.setRowCount(0);

        List<Employee> allEmployees = payrollManager.getAllEmployees();
        if (allEmployees.isEmpty()) {
            JOptionPane.showMessageDialog(this, "등록된 직원이 없습니다. 3페이지에서 직원을 먼저 등록해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            employeeCountLabel.setText("전체 0명");
            return;
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        allEmployees.sort(Comparator.comparing(
                emp -> {
                    try {
                        if (emp.getHireDate() != null && !emp.getHireDate().isEmpty()) {
                            return LocalDate.parse(emp.getHireDate(), dateFormatter);
                        }
                    } catch (DateTimeParseException e) { /* 파싱 실패 시 맨 뒤로 */ }
                    return LocalDate.MAX;
                },
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        List<Payroll> periodPayrolls = payrollManager.getPayrollsForPeriod(year, month);
        Map<Integer, Payroll> payrollMap = new HashMap<>();
        for (Payroll p : periodPayrolls) {
            payrollMap.put(p.getEmployeeId(), p);
        }

        employeeCountLabel.setText("조회된 인원: " + payrollMap.size() + "명 / 전체 " + allEmployees.size() + "명");
        AtomicInteger displayIdCounter = new AtomicInteger(1);
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.KOREA);
        long[] columnTotals = new long[columnNames.length];


        for (Employee emp : allEmployees) {
            Object[] rowData = new Object[columnNames.length];
            rowData[0] = displayIdCounter.getAndIncrement();
            rowData[1] = emp.getName();

            Payroll payrollForEmployee = payrollMap.get(emp.getId());

            if (payrollForEmployee != null) {
                boolean isUnpaidLeave = payrollForEmployee.getMonthlyBasicSalary() == 0 &&
                        payrollForEmployee.getFixedOvertimeAllowance() == 0 &&
                        payrollForEmployee.getAdditionalOvertimePremium() == 0 &&
                        payrollForEmployee.getBonus() == 0 &&
                        (payrollForEmployee.getMonthlyBasicSalary() + payrollForEmployee.getFixedOvertimeAllowance() +
                                payrollForEmployee.getAdditionalOvertimePremium() + payrollForEmployee.getBonus() +
                                payrollForEmployee.getOtherAllowance() + payrollForEmployee.getMealAllowance() + payrollForEmployee.getVehicleMaintenanceFee() +
                                payrollForEmployee.getResearchDevelopmentExpense() + payrollForEmployee.getChildcareAllowance()) == 0;

                long displayTotalSalaryForRow = isUnpaidLeave ? 0 :
                        (long)payrollForEmployee.getMonthlyBasicSalary() +
                                payrollForEmployee.getFixedOvertimeAllowance() +
                                payrollForEmployee.getAdditionalOvertimePremium() +
                                payrollForEmployee.getBonus() +
                                payrollForEmployee.getOtherAllowance() +
                                payrollForEmployee.getMealAllowance() +
                                payrollForEmployee.getVehicleMaintenanceFee() +
                                payrollForEmployee.getResearchDevelopmentExpense() +
                                payrollForEmployee.getChildcareAllowance();

                if (isUnpaidLeave) {
                    rowData[2] = "-"; rowData[3] = "-"; rowData[4] = "-"; rowData[5] = "-";
                    rowData[6] = "-"; rowData[7] = "-"; rowData[8] = "-"; rowData[9] = "-";
                    rowData[10] = "-";
                    rowData[11] = "무급 휴직";
                } else {
                    rowData[2] = currencyFormat.format(payrollForEmployee.getMonthlyBasicSalary());
                    rowData[3] = currencyFormat.format(payrollForEmployee.getFixedOvertimeAllowance());
                    rowData[4] = currencyFormat.format(payrollForEmployee.getAdditionalOvertimePremium());
                    rowData[5] = currencyFormat.format(payrollForEmployee.getBonus());
                    rowData[6] = currencyFormat.format(payrollForEmployee.getOtherAllowance());
                    rowData[7] = currencyFormat.format(payrollForEmployee.getMealAllowance());
                    rowData[8] = currencyFormat.format(payrollForEmployee.getVehicleMaintenanceFee());
                    rowData[9] = currencyFormat.format(payrollForEmployee.getResearchDevelopmentExpense());
                    rowData[10] = currencyFormat.format(payrollForEmployee.getChildcareAllowance());
                    rowData[11] = currencyFormat.format(displayTotalSalaryForRow);

                    columnTotals[2] += payrollForEmployee.getMonthlyBasicSalary();
                    columnTotals[3] += payrollForEmployee.getFixedOvertimeAllowance();
                    columnTotals[4] += payrollForEmployee.getAdditionalOvertimePremium();
                    columnTotals[5] += payrollForEmployee.getBonus();
                    columnTotals[6] += payrollForEmployee.getOtherAllowance();
                    columnTotals[7] += payrollForEmployee.getMealAllowance();
                    columnTotals[8] += payrollForEmployee.getVehicleMaintenanceFee();
                    columnTotals[9] += payrollForEmployee.getResearchDevelopmentExpense();
                    columnTotals[10] += payrollForEmployee.getChildcareAllowance();
                    columnTotals[11] += displayTotalSalaryForRow;
                }
            } else {
                for (int i = 2; i < columnNames.length - 1; i++) {
                    rowData[i] = "-";
                }
                rowData[columnNames.length - 1] = "급여 정보 없음";
            }
            tableModel.addRow(rowData);
        }
        addTotalsRow(columnTotals);
    }


    class SummaryPageTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(일반데이터폰트); // 일반데이터폰트는 java.awt.Font 타입이어야 함
            c.setForeground(table.getForeground());
            c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

            if (table.getRowCount() > 0 && row < table.getRowCount()) {
                Object idValue = table.getValueAt(row, 0);
                if (idValue != null && idValue.equals("합계")) {
                    c.setFont(합계행폰트); // 합계행폰트도 java.awt.Font 타입이어야 함
                    c.setBackground(new Color(230, 230, 230));
                    if(isSelected) c.setBackground(table.getSelectionBackground().darker());
                } else {
                    Object totalPayValue = table.getValueAt(row, 11);
                    if (column == 1 && totalPayValue instanceof String && totalPayValue.equals("급여 정보 없음")) {
                        c.setForeground(Color.RED);
                    }
                }
            }
            if (column >= 2) { setHorizontalAlignment(JLabel.RIGHT); }
            else { setHorizontalAlignment(JLabel.LEFT); }
            return c;
        }
    }


    private void deleteSelectedPayroll() {
        int selectedRowView = payrollTable.getSelectedRow();
        if (selectedRowView == -1) { JOptionPane.showMessageDialog(this, "삭제할 행을 선택하세요.", "알림", JOptionPane.WARNING_MESSAGE); return; }
        int modelRow = payrollTable.convertRowIndexToModel(selectedRowView);
        Object idCellValue = tableModel.getValueAt(modelRow, 0);
        if (idCellValue.equals("합계")) { JOptionPane.showMessageDialog(this, "합계 행은 삭제할 수 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE); return; }
        Object totalPayValue = tableModel.getValueAt(modelRow, 11);
        if (totalPayValue instanceof String && (totalPayValue.equals("급여 정보 없음") || totalPayValue.equals("무급 휴직"))){ JOptionPane.showMessageDialog(this, "삭제할 확정된 급여 정보가 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE); return; }
        if (lastQueriedYear == null || lastQueriedMonth == null) { JOptionPane.showMessageDialog(this, "먼저 조회할 연도와 월을 선택하고 조회해주세요.", "알림", JOptionPane.WARNING_MESSAGE); return; }
        String employeeNameToDelete = (String) tableModel.getValueAt(modelRow, 1);
        Optional<Employee> empOpt = payrollManager.getEmployeeByName(employeeNameToDelete);
        if (!empOpt.isPresent()) { JOptionPane.showMessageDialog(this, "삭제할 직원 정보를 DB에서 찾을 수 없습니다 (이름: " + employeeNameToDelete + ").", "오류", JOptionPane.ERROR_MESSAGE); return; }
        int employeeDbIdToDelete = empOpt.get().getId();
        int confirm = JOptionPane.showConfirmDialog(this, "직원 '" + employeeNameToDelete + "'의 " + lastQueriedYear + "년 " + String.format("%02d", lastQueriedMonth) + "월 급여 정보를 삭제하시겠습니까?", "급여 정보 삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = payrollManager.deletePayrollForPeriod(employeeDbIdToDelete, lastQueriedYear, lastQueriedMonth);
            if (success) { JOptionPane.showMessageDialog(this, employeeNameToDelete + "님의 " + lastQueriedYear + "년 " + String.format("%02d", lastQueriedMonth) + "월 급여 정보가 삭제되었습니다.", "삭제 완료", JOptionPane.INFORMATION_MESSAGE); displayInitialEmployeeList(lastQueriedYear, lastQueriedMonth);
            } else { JOptionPane.showMessageDialog(this, "급여 정보 삭제에 실패했거나 해당 기간의 데이터가 없습니다.", "삭제 실패", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void addTotalsRow(long[] totals) {
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.KOREA);
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
            displayInitialEmployeeList(this.lastQueriedYear, this.lastQueriedMonth);
        } else {
            tableModel.setRowCount(0);
            int totalRegisteredEmployees = 0;
            try {
                if(payrollManager != null) totalRegisteredEmployees = payrollManager.getAllEmployees().size();
            } catch (Exception e) { /* 초기 로딩 오류 무시 */ }
            employeeCountLabel.setText("전체 " + totalRegisteredEmployees + "명");
        }
    }

    private void exportTableToExcel() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "내보낼 데이터가 없습니다. 먼저 데이터를 조회해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("엑셀 파일로 저장");
        String defaultFileName = "급여요약";
        if (lastQueriedYear != null && lastQueriedMonth != null) {
            defaultFileName = String.format("급여요약_%d년%02d월.xlsx", lastQueriedYear, lastQueriedMonth);
        } else {
            defaultFileName += ".xlsx";
        }
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel 통합 문서 (*.xlsx)", "xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".xlsx");
            }
            if (fileToSave.exists()) {
                int response = JOptionPane.showConfirmDialog(this, "이미 같은 이름의 파일이 존재합니다. 덮어쓰시겠습니까?", "파일 덮어쓰기 확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response == JOptionPane.NO_OPTION) return;
            }

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("급여 요약");

                CellStyle headerStyle = workbook.createCellStyle();
                // POI 폰트 사용 시에는 org.apache.poi.ss.usermodel.Font 사용
                org.apache.poi.ss.usermodel.Font poiHeaderFont = workbook.createFont();
                poiHeaderFont.setBold(true);
                poiHeaderFont.setFontHeightInPoints((short) 12);
                poiHeaderFont.setColor(IndexedColors.WHITE.getIndex());
                headerStyle.setFont(poiHeaderFont);
                headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                headerStyle.setBorderTop(BorderStyle.THIN); headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN); headerStyle.setBorderRight(BorderStyle.THIN);

                CellStyle dataStyle = workbook.createCellStyle();
                dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                dataStyle.setBorderBottom(BorderStyle.DOTTED); dataStyle.setBorderLeft(BorderStyle.DOTTED);
                dataStyle.setBorderRight(BorderStyle.DOTTED); dataStyle.setBorderTop(BorderStyle.DOTTED);

                CellStyle numberDataStyle = workbook.createCellStyle();
                numberDataStyle.cloneStyleFrom(dataStyle);
                DataFormat excelDataFormat = workbook.createDataFormat();
                numberDataStyle.setDataFormat(excelDataFormat.getFormat("#,##0"));
                numberDataStyle.setAlignment(HorizontalAlignment.RIGHT);

                CellStyle totalsRowStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font poiTotalsFont = workbook.createFont();
                poiTotalsFont.setBold(true);
                poiTotalsFont.setFontHeightInPoints((short) 11);
                totalsRowStyle.setFont(poiTotalsFont);
                totalsRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
                totalsRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                totalsRowStyle.setAlignment(HorizontalAlignment.RIGHT);
                totalsRowStyle.setBorderTop(BorderStyle.MEDIUM); totalsRowStyle.setBorderBottom(BorderStyle.MEDIUM);
                totalsRowStyle.setDataFormat(excelDataFormat.getFormat("#,##0"));

                Row headerRowPOI = sheet.createRow(0);
                headerRowPOI.setHeightInPoints(20);
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    Cell cell = headerRowPOI.createCell(col);
                    cell.setCellValue(tableModel.getColumnName(col));
                    cell.setCellStyle(headerStyle);
                }

                for (int excelRowIdx = 0; excelRowIdx < tableModel.getRowCount(); excelRowIdx++) {
                    Row dataRow = sheet.createRow(excelRowIdx + 1);
                    boolean isTotalsRowCurrent = tableModel.getValueAt(excelRowIdx, 0) != null && tableModel.getValueAt(excelRowIdx, 0).equals("합계");

                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        Object cellValue = tableModel.getValueAt(excelRowIdx, col);
                        Cell cell = dataRow.createCell(col);

                        if (!isTotalsRowCurrent) {
                            cell.setCellStyle(dataStyle);
                        }

                        if (cellValue != null) {
                            if (isTotalsRowCurrent) {
                                cell.setCellStyle(totalsRowStyle);
                                if (col == 0) {
                                    cell.setCellValue((String) cellValue);
                                    CellStyle totalsLabelStyle = workbook.createCellStyle();
                                    totalsLabelStyle.cloneStyleFrom(totalsRowStyle);
                                    totalsLabelStyle.setAlignment(HorizontalAlignment.CENTER);
                                    org.apache.poi.ss.usermodel.Font labelFont = workbook.createFont();
                                    labelFont.setBold(true);
                                    labelFont.setFontHeightInPoints((short)11);
                                    totalsLabelStyle.setFont(labelFont);
                                    cell.setCellStyle(totalsLabelStyle);
                                } else if (col == 1) {
                                    cell.setCellValue("");
                                } else {
                                    try {
                                        String numericString = ((String) cellValue).replace(",", "");
                                        cell.setCellValue(Double.parseDouble(numericString));
                                    } catch (NumberFormatException | ClassCastException ex) {
                                        cell.setCellValue((String) cellValue);
                                    }
                                }
                            } else if (cellValue instanceof String) {
                                String stringValue = (String) cellValue;
                                if (stringValue.equals("무급 휴직") || stringValue.equals("급여 정보 없음") || stringValue.equals("-") || (col == 1 && !stringValue.isEmpty())) {
                                    cell.setCellValue(stringValue);
                                    if (col > 1 && (stringValue.equals("-") || stringValue.equals("무급 휴직") || stringValue.equals("급여 정보 없음"))) {
                                        CellStyle centerAlignStyle = workbook.createCellStyle();
                                        centerAlignStyle.cloneStyleFrom(dataStyle);
                                        centerAlignStyle.setAlignment(HorizontalAlignment.CENTER);
                                        cell.setCellStyle(centerAlignStyle);
                                    }
                                } else {
                                    try {
                                        String numericString = stringValue.replace(",", "");
                                        double doubleValue = Double.parseDouble(numericString);
                                        cell.setCellValue(doubleValue);
                                        cell.setCellStyle(numberDataStyle);
                                    } catch (NumberFormatException ex) {
                                        cell.setCellValue(stringValue);
                                    }
                                }
                            } else if (cellValue instanceof Number) {
                                cell.setCellValue(((Number) cellValue).doubleValue());
                                if (col >= 2) {
                                    cell.setCellStyle(numberDataStyle);
                                } else if (col == 0) {
                                    CellStyle idStyle = workbook.createCellStyle();
                                    idStyle.cloneStyleFrom(dataStyle);
                                    idStyle.setAlignment(HorizontalAlignment.CENTER);
                                    cell.setCellStyle(idStyle);
                                }
                            } else {
                                cell.setCellValue(cellValue.toString());
                            }
                        } else {
                            if (isTotalsRowCurrent && col ==1) {
                                cell.setCellStyle(totalsRowStyle);
                            } else {
                                cell.setCellStyle(dataStyle);
                            }
                        }
                    }
                }

                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    sheet.autoSizeColumn(col);
                }

                try (FileOutputStream fileOut = new FileOutputStream(fileToSave)) {
                    workbook.write(fileOut);
                }
                JOptionPane.showMessageDialog(this, "엑셀 파일이 성공적으로 저장되었습니다:\n" + fileToSave.getAbsolutePath(), "저장 완료", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "엑셀 파일 저장 중 오류가 발생했습니다: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "엑셀 파일 생성 중 예기치 않은 오류가 발생했습니다: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}