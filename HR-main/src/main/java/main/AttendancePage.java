package main;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AttendancePage extends JPanel {
    private final PayrollManager payrollManager;
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final SummaryPage summaryPage;
    private final SalaryCalculatorService salaryCalculatorService;

    private JComboBox<String> employeeSearchComboBox;
    private JTable workLogTable;
    private DefaultTableModel tableModel;

    private JTextArea originalContractInfoArea;
    private JTextArea adjustmentsInfoArea;
    private JTextArea finalPayoutInfoArea;

    private CalendarPanel calendarPanel;
    private JLabel totalWorkDaysLabel, calendarWeekdaysLabel, actualWorkWeekdaysLabel, absentWeekdaysLabel;
    private JButton applyToDbButton, clearWorkLogButton, addSelectedDaysButton, syncCalendarButton, processUnpaidLeaveButton;
    private JTextField salaryPercentageField, adHocBonusField;
    private JButton applySalaryPercentageButton, applyAdHocBonusButton;
    private JButton loadButton, backButton, calculateButton, deleteWorkLogEntryButton;

    private BigDecimal currentSalaryPercentage = new BigDecimal("100.0");
    private BigDecimal adHocBonusForCurrentCalculation = BigDecimal.ZERO;
    private boolean adHocBonusApplied = false;

    private Employee currentEmployee;
    private Payroll currentContractPayrollData;
    private CalculationResult lastCalculationResult;

    private final float FONT_SCALE_FACTOR = 1.5f;
    private java.awt.Font enlargedFont;
    private java.awt.Font enlargedFontBold;
    private java.awt.Font enlargedMonospacedFont;

    public enum WorkStatus {
        NORMAL("정상"),
        PAID_HOLIDAY("유급휴일"),
        UNPAID_HOLIDAY("무급휴일"),
        ABSENCE("결근");

        private final String displayName;

        WorkStatus(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public AttendancePage(JPanel mainPanel, CardLayout cardLayout, PayrollManager payrollManager, SummaryPage summaryPage) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;
        this.payrollManager = payrollManager;
        this.summaryPage = summaryPage;
        this.salaryCalculatorService = new SalaryCalculatorService(payrollManager);

        java.awt.Font baseFont = new JLabel().getFont();
        float newBaseSize = baseFont.getSize() * FONT_SCALE_FACTOR;
        enlargedFont = baseFont.deriveFont(newBaseSize);
        enlargedFontBold = baseFont.deriveFont(Font.BOLD, newBaseSize);

        java.awt.Font baseMonospacedFont = new JTextArea().getFont();
        if (baseMonospacedFont == null) baseMonospacedFont = new java.awt.Font("Monospaced", Font.PLAIN, 12);
        enlargedMonospacedFont = baseMonospacedFont.deriveFont(baseMonospacedFont.getSize() * FONT_SCALE_FACTOR);

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbcRoot = new GridBagConstraints();
        gbcRoot.fill = GridBagConstraints.HORIZONTAL;
        gbcRoot.weightx = 1.0;

        JPanel topPanel = new JPanel(new BorderLayout((int) (10 * FONT_SCALE_FACTOR), (int) (10 * FONT_SCALE_FACTOR)));
        JPanel employeeSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, (int) (5 * FONT_SCALE_FACTOR), (int) (5 * FONT_SCALE_FACTOR)));
        JLabel employeeSelectLabel = new JLabel("직원 선택:");
        employeeSelectLabel.setFont(enlargedFont);
        employeeSearchPanel.add(employeeSelectLabel);
        employeeSearchComboBox = new JComboBox<>();
        employeeSearchComboBox.setFont(enlargedFont);
        refreshEmployeeComboBox();
        employeeSearchPanel.add(employeeSearchComboBox);

        loadButton = new JButton("직원 정보 불러오기");
        loadButton.setFont(enlargedFont);
        employeeSearchPanel.add(loadButton);
        topPanel.add(employeeSearchPanel, BorderLayout.CENTER);

        JPanel topRightButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, (int) (5 * FONT_SCALE_FACTOR), (int) (5 * FONT_SCALE_FACTOR)));
        processUnpaidLeaveButton = new JButton("휴직 처리");
        processUnpaidLeaveButton.setFont(enlargedFont);
        topRightButtonsPanel.add(processUnpaidLeaveButton);

        backButton = new JButton("뒤로 가기");
        backButton.setFont(enlargedFont);
        topRightButtonsPanel.add(backButton);
        topPanel.add(topRightButtonsPanel, BorderLayout.EAST);

        gbcRoot.gridx = 0;
        gbcRoot.gridy = 0;
        gbcRoot.weighty = 0;
        add(topPanel, gbcRoot);

        JPanel contentPanel = new JPanel(new BorderLayout((int) (10 * FONT_SCALE_FACTOR), (int) (10 * FONT_SCALE_FACTOR)));

        JPanel leftPanel = new JPanel(new BorderLayout((int) (5 * FONT_SCALE_FACTOR), (int) (5 * FONT_SCALE_FACTOR)));
        totalWorkDaysLabel = new JLabel("달력 선택일: 0일", SwingConstants.CENTER);
        totalWorkDaysLabel.setFont(enlargedFontBold);
        calendarWeekdaysLabel = new JLabel("달력 평일: 0일", SwingConstants.CENTER);
        calendarWeekdaysLabel.setFont(enlargedFont);
        actualWorkWeekdaysLabel = new JLabel("실근무 평일: 0일", SwingConstants.CENTER);
        actualWorkWeekdaysLabel.setFont(enlargedFont);
        absentWeekdaysLabel = new JLabel("결근/무급 평일: 0일", SwingConstants.CENTER);
        absentWeekdaysLabel.setFont(enlargedFont);
        absentWeekdaysLabel.setForeground(Color.RED);

        calendarPanel = new CalendarPanel(this::updateWorkdayInfoLabels);

        JPanel calendarButtonPanel = new JPanel(new GridLayout(2, 1, (int) (5 * FONT_SCALE_FACTOR), (int) (5 * FONT_SCALE_FACTOR)));
        syncCalendarButton = new JButton("달력으로 전체 동기화");
        syncCalendarButton.setFont(enlargedFont);
        addSelectedDaysButton = new JButton("선택일 기록표에 추가");
        addSelectedDaysButton.setFont(enlargedFont);
        calendarButtonPanel.add(syncCalendarButton);
        calendarButtonPanel.add(addSelectedDaysButton);

        leftPanel.add(calendarPanel, BorderLayout.CENTER);
        JPanel leftBottomPanel = new JPanel(new BorderLayout((int) (5 * FONT_SCALE_FACTOR), (int) (5 * FONT_SCALE_FACTOR)));
        JPanel labelsPanel = new JPanel(new GridLayout(4, 1, (int) (5 * FONT_SCALE_FACTOR), (int) (2 * FONT_SCALE_FACTOR)));
        labelsPanel.add(totalWorkDaysLabel);
        labelsPanel.add(calendarWeekdaysLabel);
        labelsPanel.add(actualWorkWeekdaysLabel);
        labelsPanel.add(absentWeekdaysLabel);
        leftBottomPanel.add(labelsPanel, BorderLayout.CENTER);
        leftBottomPanel.add(calendarButtonPanel, BorderLayout.SOUTH);
        leftPanel.add(leftBottomPanel, BorderLayout.SOUTH);

        int calendarPreferredHeight = (int) (450 * FONT_SCALE_FACTOR);
        leftPanel.setPreferredSize(new Dimension((int) (calendarPanel.getPreferredSize().width * 1.1), calendarPreferredHeight));
        contentPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new BorderLayout((int) (10 * FONT_SCALE_FACTOR), (int) (10 * FONT_SCALE_FACTOR)));
        TitledBorder workLogBorder = BorderFactory.createTitledBorder("근무 시간 기록 (시간/상태 비어있으면 급여 계산에서 제외)");
        workLogBorder.setTitleFont(enlargedFontBold);
        rightPanel.setBorder(workLogBorder);

        String[] columnNames = {"날짜", "출근시간", "퇴근시간", "상태"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3) return WorkStatus.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0;
            }
        };
        workLogTable = new JTable(tableModel);
        workLogTable.setFont(enlargedFont);
        workLogTable.getTableHeader().setFont(enlargedFontBold);
        workLogTable.setRowHeight((int) (workLogTable.getRowHeight() * FONT_SCALE_FACTOR));
        workLogTable.setSurrendersFocusOnKeystroke(true);
        new TableClipboardAdapter(workLogTable);

        workLogTable.setShowGrid(true);
        workLogTable.setGridColor(new Color(220, 220, 220));
        workLogTable.setIntercellSpacing(new Dimension(1, 1));

        TableColumn statusColumn = workLogTable.getColumnModel().getColumn(3);
        JComboBox<WorkStatus> statusComboBoxEditor = new JComboBox<>(WorkStatus.values());
        statusComboBoxEditor.setFont(enlargedFont);
        statusColumn.setCellEditor(new DefaultCellEditor(statusComboBoxEditor));
        workLogTable.setDefaultRenderer(Object.class, new CustomDateRenderer(calendarPanel));
        DefaultCellEditor timeEditor = new DefaultCellEditor(new JTextField()) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                JTextField editor = (JTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);
                editor.setFont(enlargedFont);
                editor.setText("");
                return editor;
            }

            @Override
            public Object getCellEditorValue() {
                String value = ((JTextField) getComponent()).getText();
                return formatTimeInput(value);
            }

            private String formatTimeInput(String input) {
                if (input == null || input.trim().isEmpty()) return "";
                if (input.contains(":")) {
                    try {
                        LocalTime.parse(input, DateTimeFormatter.ofPattern("HH:mm"));
                        return input;
                    } catch (Exception e) {
                        return "";
                    }
                }
                String digits = input.replaceAll("\\D", "");
                String formatted;
                switch (digits.length()) {
                    case 1:
                        formatted = "0" + digits + ":00";
                        break;
                    case 2:
                        formatted = digits + ":00";
                        break;
                    case 3:
                        formatted = "0" + digits.substring(0, 1) + ":" + digits.substring(1, 3);
                        break;
                    case 4:
                        formatted = digits.substring(0, 2) + ":" + digits.substring(2, 4);
                        break;
                    default:
                        return "";
                }
                try {
                    LocalTime.parse(formatted, DateTimeFormatter.ofPattern("HH:mm"));
                    return formatted;
                } catch (Exception e) {
                    return "";
                }
            }
        };
        workLogTable.getColumnModel().getColumn(1).setCellEditor(timeEditor);
        workLogTable.getColumnModel().getColumn(2).setCellEditor(timeEditor);

        JScrollPane workLogTableScrollPane = new JScrollPane(workLogTable);

        rightPanel.setPreferredSize(new Dimension((int) (rightPanel.getPreferredSize().width * 1.1), calendarPreferredHeight));
        rightPanel.add(workLogTableScrollPane, BorderLayout.CENTER);

        JPanel tableControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, (int) (5 * FONT_SCALE_FACTOR), (int) (5 * FONT_SCALE_FACTOR)));
        JLabel salaryPercentageLabel = new JLabel("급여 적용 %:");
        salaryPercentageLabel.setFont(enlargedFont);
        tableControlPanel.add(salaryPercentageLabel);
        salaryPercentageField = new JTextField(this.currentSalaryPercentage.stripTrailingZeros().toPlainString(), (int) (4 * FONT_SCALE_FACTOR * 0.8));
        salaryPercentageField.setFont(enlargedFont);
        tableControlPanel.add(salaryPercentageField);
        applySalaryPercentageButton = new JButton("비율 적용");
        applySalaryPercentageButton.setFont(enlargedFont);
        tableControlPanel.add(applySalaryPercentageButton);
        tableControlPanel.add(Box.createHorizontalStrut((int) (10 * FONT_SCALE_FACTOR)));

        JLabel adHocBonusLabel = new JLabel("기간 상여금:");
        adHocBonusLabel.setFont(enlargedFont);
        tableControlPanel.add(adHocBonusLabel);
        adHocBonusField = new JTextField("0", (int) (7 * FONT_SCALE_FACTOR * 0.8));
        adHocBonusField.setFont(enlargedFont);
        tableControlPanel.add(adHocBonusField);
        applyAdHocBonusButton = new JButton("상여금 적용");
        applyAdHocBonusButton.setFont(enlargedFont);
        tableControlPanel.add(applyAdHocBonusButton);
        tableControlPanel.add(Box.createHorizontalStrut((int) (10 * FONT_SCALE_FACTOR)));

        clearWorkLogButton = new JButton("기록 초기화");
        clearWorkLogButton.setFont(enlargedFont);
        tableControlPanel.add(clearWorkLogButton);
        deleteWorkLogEntryButton = new JButton("선택 기록 삭제");
        deleteWorkLogEntryButton.setFont(enlargedFont);
        tableControlPanel.add(deleteWorkLogEntryButton);
        rightPanel.add(tableControlPanel, BorderLayout.SOUTH);

        contentPanel.add(rightPanel, BorderLayout.CENTER);

        gbcRoot.gridy = 1;
        gbcRoot.weighty = 0;
        gbcRoot.fill = GridBagConstraints.BOTH;
        add(contentPanel, gbcRoot);

        JPanel bottomOuterPanel = new JPanel(new BorderLayout((int) (10 * FONT_SCALE_FACTOR), (int) (10 * FONT_SCALE_FACTOR)));
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, (int) (10 * FONT_SCALE_FACTOR), (int) (10 * FONT_SCALE_FACTOR)));
        calculateButton = new JButton("급여 계산 실행");
        Font mainActionFont = new Font("맑은 고딕", Font.BOLD, (int) (16 * FONT_SCALE_FACTOR * 0.9));
        calculateButton.setFont(mainActionFont);
        calculateButton.setBackground(new Color(70, 140, 250));
        calculateButton.setForeground(Color.WHITE);

        applyToDbButton = new JButton("계산결과 DB 적용");
        applyToDbButton.setFont(mainActionFont);
        applyToDbButton.setBackground(new Color(0, 150, 0));
        applyToDbButton.setForeground(Color.WHITE);
        applyToDbButton.setEnabled(false);
        bottomButtonPanel.add(calculateButton);
        bottomButtonPanel.add(applyToDbButton);
        bottomOuterPanel.add(bottomButtonPanel, BorderLayout.NORTH);

        JPanel resultsDisplayPanel = new JPanel(new GridLayout(1, 3, (int) (10 * FONT_SCALE_FACTOR), 0));

        int textAreaRows = (int) (16 * FONT_SCALE_FACTOR * 0.8);
        int textAreaCols = (int) (80 * FONT_SCALE_FACTOR / 3.5);

        originalContractInfoArea = new JTextArea(textAreaRows, textAreaCols);
        originalContractInfoArea.setFont(enlargedMonospacedFont);
        originalContractInfoArea.setEditable(false);
        originalContractInfoArea.setLineWrap(true);
        originalContractInfoArea.setWrapStyleWord(true);
        resultsDisplayPanel.add(new JScrollPane(originalContractInfoArea));

        adjustmentsInfoArea = new JTextArea(textAreaRows, textAreaCols);
        adjustmentsInfoArea.setFont(enlargedMonospacedFont);
        adjustmentsInfoArea.setEditable(false);
        adjustmentsInfoArea.setLineWrap(true);
        adjustmentsInfoArea.setWrapStyleWord(true);
        resultsDisplayPanel.add(new JScrollPane(adjustmentsInfoArea));

        finalPayoutInfoArea = new JTextArea(textAreaRows, textAreaCols);
        finalPayoutInfoArea.setFont(enlargedMonospacedFont);
        finalPayoutInfoArea.setEditable(false);
        finalPayoutInfoArea.setLineWrap(true);
        finalPayoutInfoArea.setWrapStyleWord(true);
        resultsDisplayPanel.add(new JScrollPane(finalPayoutInfoArea));

        bottomOuterPanel.add(resultsDisplayPanel, BorderLayout.CENTER);

        gbcRoot.gridy = 2;
        gbcRoot.weighty = 1.0;
        gbcRoot.fill = GridBagConstraints.BOTH;
        add(bottomOuterPanel, gbcRoot);

        addListeners();
        SwingUtilities.invokeLater(this::updateDetailedWorkdayLabels);
    }

    private void addListeners() {
        backButton.addActionListener(e -> {
            summaryPage.refreshTableData();
            cardLayout.show(mainPanel, "Summary");
        });
        loadButton.addActionListener(e -> loadEmployeeData());
        syncCalendarButton.addActionListener(e -> populateTableFromCalendar(true));
        addSelectedDaysButton.addActionListener(e -> addSelectedCalendarDaysToTable());
        deleteWorkLogEntryButton.addActionListener(e -> deleteWorkRecord());
        clearWorkLogButton.addActionListener(e -> clearWorkLogTableAction());
        calculateButton.addActionListener(e -> runSalaryCalculation());
        applyToDbButton.addActionListener(e -> saveProcessedPayrollDataToDB());
        processUnpaidLeaveButton.addActionListener(e -> processUnpaidLeaveAction());

        applySalaryPercentageButton.addActionListener(e -> {
            try {
                BigDecimal percentage = new BigDecimal(salaryPercentageField.getText().trim());
                if (percentage.compareTo(BigDecimal.ZERO) >= 0) {
                    this.currentSalaryPercentage = percentage;
                    JOptionPane.showMessageDialog(this, "급여 적용 비율이 " + this.currentSalaryPercentage.toPlainString() + "%로 설정되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                    if (currentEmployee != null && currentContractPayrollData != null && !originalContractInfoArea.getText().trim().isEmpty() && !originalContractInfoArea.getText().startsWith("직원을 선택하고")) {
                        runSalaryCalculation();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "급여 적용 비율은 0 이상이어야 합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                    salaryPercentageField.setText(this.currentSalaryPercentage.stripTrailingZeros().toPlainString());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "숫자 형식으로 비율을 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                salaryPercentageField.setText(this.currentSalaryPercentage.stripTrailingZeros().toPlainString());
            }
        });

        applyAdHocBonusButton.addActionListener(e -> {
            try {
                this.adHocBonusForCurrentCalculation = new BigDecimal(adHocBonusField.getText().trim().replace(",", ""));
                this.adHocBonusApplied = true;
                JOptionPane.showMessageDialog(this, "기간 특별 상여금이 " + new DecimalFormat("#,###").format(this.adHocBonusForCurrentCalculation) + "원으로 설정되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                if (currentEmployee != null && currentContractPayrollData != null && !originalContractInfoArea.getText().trim().isEmpty() && !originalContractInfoArea.getText().startsWith("직원을 선택하고")) {
                    runSalaryCalculation();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "숫자 형식으로 상여금을 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                adHocBonusField.setText(new DecimalFormat("#,###").format(this.adHocBonusForCurrentCalculation));
            }
        });
    }

    private void runSalaryCalculation() {
        if (currentEmployee == null || currentContractPayrollData == null) {
            JOptionPane.showMessageDialog(this, "먼저 직원을 선택하고 정보를 불러와주세요.", "경고", JOptionPane.WARNING_MESSAGE);
            return;
        }
        applyToDbButton.setEnabled(false);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<WorkRecord> recordsToCalculate = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String dateStr = (String) tableModel.getValueAt(i, 0);
            String startTimeStr = (String) tableModel.getValueAt(i, 1);
            String endTimeStr = (String) tableModel.getValueAt(i, 2);
            Object statusObj = tableModel.getValueAt(i, 3);
            WorkStatus status;
            if (statusObj instanceof WorkStatus) {
                status = (WorkStatus) statusObj;
            } else {
                JOptionPane.showMessageDialog(this, (i + 1) + "행 상태값 오류", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                LocalDate date = LocalDate.parse(dateStr, dateFormatter);
                if (startTimeStr == null || startTimeStr.trim().isEmpty() || endTimeStr == null || endTimeStr.trim().isEmpty() || status == WorkStatus.UNPAID_HOLIDAY || status == WorkStatus.ABSENCE) {
                    recordsToCalculate.add(new WorkRecord(date, null, null, status, calendarPanel.isPublicHoliday(date), 0));
                    continue;
                }
                LocalTime startTime = LocalTime.parse(startTimeStr, timeFormatter);
                LocalTime endTime = LocalTime.parse(endTimeStr, timeFormatter);
                Duration duration = Duration.between(startTime, endTime);
                if (endTime.isBefore(startTime)) duration = duration.plusDays(1);
                long grossWorkMinutesPerDay = duration.toMinutes();

                long breakMinutesPerDay = (grossWorkMinutesPerDay / (4 * 60)) * 30;
                if (grossWorkMinutesPerDay >= 8 * 60) breakMinutesPerDay = Math.max(breakMinutesPerDay, 60);

                long netWorkMinutesPerDay = Math.max(0, grossWorkMinutesPerDay - breakMinutesPerDay);
                boolean isOriginallyPublicHoliday = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY || calendarPanel.isPublicHoliday(date);
                recordsToCalculate.add(new WorkRecord(date, startTime, endTime, status, isOriginallyPublicHoliday, netWorkMinutesPerDay));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, String.format("%d행 날짜/시간 형식 오류: %s", i + 1, ex.getMessage()), "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        this.lastCalculationResult = salaryCalculatorService.calculateSalary(
                recordsToCalculate, currentContractPayrollData, currentEmployee, calendarPanel.getCurrentYearMonth(),
                this.currentSalaryPercentage, adHocBonusForCurrentCalculation, adHocBonusApplied, calendarPanel.getPublicHolidays()
        );

        printFinalResults(currentContractPayrollData, lastCalculationResult);
        applyToDbButton.setEnabled(true);
    }

    private void printFinalResults(Payroll originalContract, CalculationResult result) {
        if (result == null) return;
        DecimalFormat df = new DecimalFormat("#,###");

        StringBuilder sbOriginal = new StringBuilder();
        sbOriginal.append("===== 원본 계약 정보 (");

        BigDecimal hireRatio = result.getHireProrationRatio();
        if (hireRatio != null && hireRatio.compareTo(BigDecimal.ONE) < 0) {
            BigDecimal hireRatioPercent = hireRatio.multiply(new BigDecimal("100"));
            sbOriginal.append(String.format("중도입사 일할: %.2f%%", hireRatioPercent));
        } else {
            sbOriginal.append("정상월");
        }
        sbOriginal.append(") =====\n");

        BigDecimal prorationRatio = (hireRatio != null) ? hireRatio : BigDecimal.ONE;
        BigDecimal originalProratedTotal = BigDecimal.ZERO;

        BigDecimal[] originalItems = {
                originalContract.getMonthlyBasicSalary(), originalContract.getFixedOvertimeAllowance(),
                originalContract.getBonus(), originalContract.getOtherAllowance(), originalContract.getMealAllowance(),
                originalContract.getVehicleMaintenanceFee(), originalContract.getResearchDevelopmentExpense(), originalContract.getChildcareAllowance()
        };
        String[] itemLabels = {"기본급여", "고정연장수당", "상여금", "기타수당", "식대", "차량유지비", "연구개발비", "육아수당"};

        for (int i = 0; i < originalItems.length; i++) {
            BigDecimal itemValue = originalItems[i] != null ? originalItems[i] : BigDecimal.ZERO;
            BigDecimal proratedItem = itemValue.multiply(prorationRatio);
            originalProratedTotal = originalProratedTotal.add(proratedItem);
            sbOriginal.append(String.format("  %-12s: %12s 원\n", itemLabels[i], df.format(proratedItem.setScale(0, RoundingMode.HALF_UP))));
        }
        sbOriginal.append("  -------------------------------------\n");
        sbOriginal.append(String.format("  원본 계약 총액(일할):%10s 원\n", df.format(originalProratedTotal.setScale(0, RoundingMode.HALF_UP))));
        originalContractInfoArea.setText(sbOriginal.toString());
        originalContractInfoArea.setCaretPosition(0);

        StringBuilder sbAdjustments = new StringBuilder();
        BigDecimal attendanceRatio = result.getAttendanceBasedPaymentRatio();
        BigDecimal attendanceRatioPercent = (attendanceRatio != null) ? attendanceRatio.multiply(new BigDecimal("100")) : BigDecimal.ZERO;
        sbAdjustments.append(String.format("===== 근태 반영 조정 (참고 지급률 %.2f%%) =====\n", attendanceRatioPercent));

        String shortfallTimeStr = String.format("%d시간 %d분", result.getTotalShortfallMinutes() / 60, result.getTotalShortfallMinutes() % 60);
        sbAdjustments.append(String.format("  (-) 근무 부족분   : %10s 원\n      (총 %s 부족)\n", df.format(result.getTotalShortfallMonetaryDeduction().setScale(0, RoundingMode.HALF_UP)), shortfallTimeStr));
        if (result.getWeeklyAbsencePenalty().compareTo(BigDecimal.ZERO) > 0) {
            sbAdjustments.append(String.format("  (-) 주휴수당 차감 : %10s 원\n", df.format(result.getWeeklyAbsencePenalty().setScale(0, RoundingMode.HALF_UP))));
        }
        if (result.getOvertimePremium().compareTo(BigDecimal.ZERO) > 0) {
            sbAdjustments.append(String.format("  (+) 추가 연장수당 : %10s 원\n", df.format(result.getOvertimePremium().setScale(0, RoundingMode.HALF_UP))));
        }
        if (result.getNightPremium().compareTo(BigDecimal.ZERO) > 0) {
            sbAdjustments.append(String.format("  (+) 추가 야간수당 : %10s 원\n", df.format(result.getNightPremium().setScale(0, RoundingMode.HALF_UP))));
        }
        if (result.getHolidayPremium().compareTo(BigDecimal.ZERO) > 0) {
            sbAdjustments.append(String.format("  (+) 추가 휴일수당 : %10s 원\n", df.format(result.getHolidayPremium().setScale(0, RoundingMode.HALF_UP))));
        }
        adjustmentsInfoArea.setText(sbAdjustments.toString());
        adjustmentsInfoArea.setCaretPosition(0);

        StringBuilder sbFinal = new StringBuilder();
        sbFinal.append(String.format("===== 최종 지급 내역 (적용률: %.1f%%) =====\n", this.currentSalaryPercentage));
        BigDecimal finalTotalPay = BigDecimal.ZERO;

        BigDecimal[] finalItems = {
                result.getFinalAdjustedBasicPay(), result.getFinalAdjustedFixedOvertimeAllowance(),
                result.getFinalAdjustedAdditionalOvertimePremium(), result.getFinalAdjustedBonus(),
                result.getFinalAdjustedOtherAllowance(), result.getFinalAdjustedMealAllowance(),
                result.getFinalAdjustedVehicleMaintenanceFee(), result.getFinalAdjustedResearchDevelopmentExpense(),
                result.getFinalAdjustedChildcareAllowance()
        };

        String[] finalItemLabels = {
                "기본급여", "고정연장수당", "추가수당", "상여금", "기타수당", "식대",
                "차량유지비", "연구개발비", "육아수당"
        };

        for (int i = 0; i < finalItems.length; i++) {
            BigDecimal item = finalItems[i];
            if (item != null) {
                finalTotalPay = finalTotalPay.add(item);
                if (item.compareTo(BigDecimal.ZERO) != 0) {
                    sbFinal.append(String.format("  %-14s: %12s 원\n", finalItemLabels[i], df.format(item.setScale(0, RoundingMode.HALF_UP))));
                }
            }
        }

        sbFinal.append("======================================\n");
        sbFinal.append(String.format("  >> 최종 지급액(세전): %10s 원\n", df.format(finalTotalPay.setScale(0, RoundingMode.HALF_UP))));
        sbFinal.append("======================================\n");
        finalPayoutInfoArea.setText(sbFinal.toString());
        finalPayoutInfoArea.setCaretPosition(0);
    }

    public void refreshEmployeeComboBox() {
        String selectedEmployeeName = (String) employeeSearchComboBox.getSelectedItem();
        employeeSearchComboBox.removeAllItems();
        try {
            List<Employee> employees = payrollManager.getAllEmployees();
            if (employees.isEmpty()) {
                employeeSearchComboBox.addItem("등록된 직원이 없습니다.");
            } else {
                employees.sort(Comparator.comparing(Employee::getName));
                for (Employee emp : employees) {
                    employeeSearchComboBox.addItem(emp.getName());
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "직원 목록 로드 오류: " + e.getMessage(), "DB 오류", JOptionPane.ERROR_MESSAGE);
        }

        if (selectedEmployeeName != null) {
            employeeSearchComboBox.setSelectedItem(selectedEmployeeName);
        }
    }

    private void loadEmployeeData() {
        String selectedName = (String) employeeSearchComboBox.getSelectedItem();
        if (selectedName == null || selectedName.equals("등록된 직원이 없습니다.")) {
            clearAllData();
            return;
        }

        Optional<Employee> empOpt = payrollManager.getEmployeeByName(selectedName);
        if (empOpt.isPresent()) {
            this.currentEmployee = empOpt.get();
            Optional<Payroll> payrollOpt = payrollManager.getContractualPayroll(currentEmployee.getId());
            if (payrollOpt.isPresent()) {
                this.currentContractPayrollData = payrollOpt.get();
                populateTableFromCalendar(true);
                runSalaryCalculation();
                applyToDbButton.setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(this, "선택된 직원의 계약 급여 정보가 없습니다.\n'3. 직원 정보' 페이지에서 급여 정보를 입력해주세요.", "정보 없음", JOptionPane.WARNING_MESSAGE);
                clearAllData();
            }
        } else {
            JOptionPane.showMessageDialog(this, "직원 정보를 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            clearAllData();
        }
    }

    private void updateWorkdayInfoLabels(int selectedDays) {
        totalWorkDaysLabel.setText(String.format("달력 선택일: %d일", selectedDays));
        updateDetailedWorkdayLabels();

        if (calendarPanel.isMonthChangedFlag()) {
            if (currentEmployee != null) {
                int option = JOptionPane.showConfirmDialog(this,
                        calendarPanel.getCurrentYearMonth().toString() + "월의 근무 기록을 새로 불러오시겠습니까?\n(기존 테이블 내용은 초기화됩니다)",
                        "월 변경됨", JOptionPane.YES_NO_OPTION);

                if (option == JOptionPane.YES_OPTION) {
                    loadEmployeeData();
                }
            }
            calendarPanel.clearMonthChangedFlag();
        }
    }

    private void updateDetailedWorkdayLabels() {
        List<Map<String, Object>> datesInfo = calendarPanel.getSelectedDatesWithStatus();
        long calendarWeekdays = 0;
        long actualWorkWeekdays = 0;
        long absentWeekdays = 0;

        for (Map<String, Object> info : datesInfo) {
            LocalDate date = (LocalDate) info.get("date");
            DayOfWeek dow = date.getDayOfWeek();
            boolean isHoliday = (boolean) info.get("isPublicHoliday");
            boolean isSelected = (boolean) info.get("isSelected");

            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !isHoliday) {
                calendarWeekdays++;
                if (isSelected) {
                    actualWorkWeekdays++;
                } else {
                    absentWeekdays++;
                }
            }
        }
        calendarWeekdaysLabel.setText(String.format("달력 평일: %d일", calendarWeekdays));
        actualWorkWeekdaysLabel.setText(String.format("실근무 평일: %d일", actualWorkWeekdays));
        absentWeekdaysLabel.setText(String.format("결근/무급 평일: %d일", absentWeekdays));
    }

    private void clearAllData() {
        this.currentEmployee = null;
        this.currentContractPayrollData = null;
        this.lastCalculationResult = null;
        tableModel.setRowCount(0);
        originalContractInfoArea.setText("직원을 선택하고 '직원 정보 불러오기'를 눌러주세요.");
        adjustmentsInfoArea.setText("");
        finalPayoutInfoArea.setText("");
        applyToDbButton.setEnabled(false);
    }

    private void clearWorkLogTableAction() {
        int confirm = JOptionPane.showConfirmDialog(this, "근무 기록표를 정말 초기화하시겠습니까?", "확인", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            tableModel.setRowCount(0);
        }
    }

    private void populateTableFromCalendar(boolean clearFirst) {
        if (clearFirst) {
            tableModel.setRowCount(0);
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, String> settings = payrollManager.loadSettings();
        String defaultStartTime = settings.getOrDefault("defaultStartTime", "09:00");
        String defaultEndTime = settings.getOrDefault("defaultEndTime", "18:00");

        List<Map<String, Object>> datesInfo = calendarPanel.getSelectedDatesWithStatus();
        datesInfo.sort(Comparator.comparing(m -> (LocalDate) m.get("date")));

        for (Map<String, Object> info : datesInfo) {
            LocalDate date = (LocalDate) info.get("date");
            boolean isSelected = (boolean) info.get("isSelected");
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            String dateStr = date.format(dateFormatter);
            String startTimeStr = "";
            String endTimeStr = "";
            WorkStatus status = WorkStatus.ABSENCE;

            if (isSelected) {
                if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY || (boolean) info.get("isPublicHoliday")) {
                    status = WorkStatus.PAID_HOLIDAY;
                } else {
                    status = WorkStatus.NORMAL;
                    startTimeStr = defaultStartTime;
                    endTimeStr = defaultEndTime;
                }
            } else {
                if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY && !(boolean) info.get("isPublicHoliday")) {
                    status = WorkStatus.ABSENCE;
                } else {
                    status = WorkStatus.UNPAID_HOLIDAY;
                }
            }

            tableModel.addRow(new Object[]{dateStr, startTimeStr, endTimeStr, status});
        }
    }

    private void addSelectedCalendarDaysToTable() {
        Set<LocalDate> existingDates = new HashSet<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            existingDates.add(LocalDate.parse((String) tableModel.getValueAt(i, 0)));
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, String> settings = payrollManager.loadSettings();
        String defaultStartTime = settings.getOrDefault("defaultStartTime", "09:00");
        String defaultEndTime = settings.getOrDefault("defaultEndTime", "18:00");

        List<Map<String, Object>> datesInfo = calendarPanel.getSelectedDatesWithStatus();
        datesInfo.stream()
                .filter(info -> (boolean) info.get("isSelected"))
                .map(info -> (LocalDate) info.get("date"))
                .filter(date -> !existingDates.contains(date))
                .sorted()
                .forEach(date -> {
                    tableModel.addRow(new Object[]{
                            date.format(dateFormatter), defaultStartTime, defaultEndTime, WorkStatus.NORMAL
                    });
                });
    }

    private void deleteWorkRecord() {
        int[] selectedRows = workLogTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "삭제할 행을 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        for (int i = selectedRows.length - 1; i >= 0; i--) {
            tableModel.removeRow(selectedRows[i]);
        }
    }

    private void saveProcessedPayrollDataToDB() {
        if (lastCalculationResult == null || currentEmployee == null) {
            JOptionPane.showMessageDialog(this, "저장할 계산 결과가 없습니다. 먼저 급여 계산을 실행해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "현재 계산 결과를 DB에 최종 저장하시겠습니까?\n이 작업은 '1. 급여 내역' 페이지에 반영됩니다.", "최종 저장 확인", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        long unpaidDaysCount = 0;
        long absenceDaysCount = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            WorkStatus status = (WorkStatus) tableModel.getValueAt(i, 3);
            if (status == WorkStatus.UNPAID_HOLIDAY) unpaidDaysCount++;
            if (status == WorkStatus.ABSENCE) absenceDaysCount++;
        }

        // [수정] Payroll 객체를 생성하여 전달
        Payroll finalPayroll = new Payroll();
        CalculationResult res = lastCalculationResult;

        // 급여 정보 설정
        finalPayroll.setMonthlyBasicSalary(res.getFinalAdjustedBasicPay());
        finalPayroll.setFixedOvertimeAllowance(res.getFinalAdjustedFixedOvertimeAllowance());
        finalPayroll.setAdditionalOvertimePremium(res.getFinalAdjustedAdditionalOvertimePremium());
        finalPayroll.setBonus(res.getFinalAdjustedBonus());
        finalPayroll.setOtherAllowance(res.getFinalAdjustedOtherAllowance());
        finalPayroll.setMealAllowance(res.getFinalAdjustedMealAllowance());
        finalPayroll.setVehicleMaintenanceFee(res.getFinalAdjustedVehicleMaintenanceFee());
        finalPayroll.setResearchDevelopmentExpense(res.getFinalAdjustedResearchDevelopmentExpense());
        finalPayroll.setChildcareAllowance(res.getFinalAdjustedChildcareAllowance());

        // 근태 정보 설정
        finalPayroll.setUnpaidDays((int) unpaidDaysCount);
        finalPayroll.setUnauthorizedAbsenceDays((int) absenceDaysCount);

        try {
            // [수정] 변경된 메소드 시그니처에 맞게 호출
            payrollManager.finalizeMonthlyPayAndDeductions(
                    currentEmployee.getId(),
                    calendarPanel.getCurrentYearMonth(),
                    finalPayroll,
                    new BigDecimal("0.007"), // 산재보험료율은 추후 설정에서 가져오도록 변경 가능
                    1 // 부양가족 수도 직원 정보에서 가져오도록 변경 가능
            );

            JOptionPane.showMessageDialog(this, "급여 정보가 성공적으로 저장되었습니다.", "저장 완료", JOptionPane.INFORMATION_MESSAGE);
            applyToDbButton.setEnabled(false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "DB 저장 중 오류 발생: " + e.getMessage(), "DB 오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void processUnpaidLeaveAction() {
        if (currentEmployee == null) {
            JOptionPane.showMessageDialog(this, "먼저 직원을 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "선택된 직원의 이번달 급여를 '무급 휴직'으로 처리하시겠습니까?\n모든 급여 항목이 0으로 계산되며, 근무기록은 '무급휴일'로 채워집니다.",
                "무급 휴직 처리 확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        tableModel.setRowCount(0);
        List<Map<String, Object>> datesInfo = calendarPanel.getSelectedDatesWithStatus();
        datesInfo.sort(Comparator.comparing(m -> (LocalDate) m.get("date")));

        for (Map<String, Object> info : datesInfo) {
            LocalDate date = (LocalDate) info.get("date");
            tableModel.addRow(new Object[]{date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), "", "", WorkStatus.UNPAID_HOLIDAY});
        }

        runSalaryCalculation();
    }

    class CustomDateRenderer extends DefaultTableCellRenderer {
        private final CalendarPanel calendarPanel;

        public CustomDateRenderer(CalendarPanel calendarPanel) {
            this.calendarPanel = calendarPanel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (col == 0 && value instanceof String) {
                try {
                    LocalDate date = LocalDate.parse((String) value);
                    DayOfWeek dayOfWeek = date.getDayOfWeek();
                    if (calendarPanel.isPublicHoliday(date) || dayOfWeek == DayOfWeek.SUNDAY) {
                        c.setForeground(Color.RED);
                    } else if (dayOfWeek == DayOfWeek.SATURDAY) {
                        c.setForeground(Color.BLUE);
                    } else {
                        c.setForeground(table.getForeground());
                    }
                } catch (DateTimeParseException e) {
                    c.setForeground(table.getForeground());
                }
            } else {
                c.setForeground(table.getForeground());
            }

            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            } else {
                c.setBackground(table.getBackground());
            }

            return c;
        }
    }
}