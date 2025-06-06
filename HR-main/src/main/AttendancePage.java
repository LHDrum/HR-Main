package main;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AttendancePage extends JPanel {
    private final PayrollManager payrollManager;
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final SummaryPage summaryPage;

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

    private double currentSalaryPercentage = 100.0;
    private double adHocBonusForCurrentCalculation = 0.0;
    private boolean adHocBonusApplied = false;

    private Employee currentEmployee;
    private Payroll currentContractPayrollData; // This will hold values consistent with EmployeeManagementPage

    private int missedOrUnpaidOrAbsentWeekdayCountForDB = 0;
    private double lastCalculatedHireProrationRatio = 1.0;
    private double lastCalculatedAttendanceBasedDisplayPaymentRatio = 1.0;
    private double lastCalculatedPremiumAdditions = 0.0;
    private double lastCalculatedWeeklyAbsencePenalty = 0.0;
    private long lastCalculatedPaidPremiumMinutes = 0L;
    private long lastCalculatedTotalShortfallMinutes = 0L;


    private double finalAdjustedBasicPay, finalAdjustedFixedOvertimeAllowance, finalAdjustedBonus;
    private double finalAdjustedOtherAllowance, finalAdjustedMealAllowance, finalAdjustedVehicleMaintenanceFee;
    private double finalAdjustedResearchDevelopmentExpense, finalAdjustedChildcareAllowance;
    private double finalAdjustedAdditionalOvertimePremium;

    private final float FONT_SCALE_FACTOR = 1.5f;
    private java.awt.Font enlargedFont;
    private java.awt.Font enlargedFontBold;
    private java.awt.Font enlargedMonospacedFont;

    // 기준 시간 정의
    private static final double STANDARD_TOTAL_HOURS_FOR_RATE_CALC = 224.0; // "총근무시간" (통상시급 계산용 분모)
    // 계약상 명목상의 고정 연장 시간 (예: 10시간). 추가 연장 수당 발생 기준점.
    private static final double NOMINAL_FIXED_OVERTIME_HOURS = 10.0;


    public enum WorkStatus { NORMAL("정상"), PAID_HOLIDAY("유급휴일"), UNPAID_HOLIDAY("무급휴일"), ABSENCE("결근"); private final String displayName; WorkStatus(String displayName) { this.displayName = displayName; } @Override public String toString() { return displayName; } }
    private static class WorkRecord { LocalDate date; LocalTime startTime; LocalTime endTime; WorkStatus status; boolean isOriginallyPublicHoliday; long netWorkMinutes; WorkRecord(LocalDate date, LocalTime s, LocalTime e, WorkStatus st, boolean ioph, long netMinutes) { this.date = date; this.startTime = s; this.endTime = e; this.status = st; this.isOriginallyPublicHoliday = ioph; this.netWorkMinutes = netMinutes; } }

    public AttendancePage(JPanel mainPanel, CardLayout cardLayout, PayrollManager payrollManager, SummaryPage summaryPage) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;
        this.payrollManager = payrollManager;
        this.summaryPage = summaryPage;

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

        // --- UI Components Initialization (상단 패널, 달력, 근무기록 테이블 등) ---
        // ... (UI 초기화 코드는 이전과 동일하게 유지) ...
        // --- 상단 패널 (직원 선택, 뒤로가기 등) ---
        JPanel topPanel = new JPanel(new BorderLayout( (int)(10*FONT_SCALE_FACTOR) , (int)(10*FONT_SCALE_FACTOR) ));
        JPanel employeeSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, (int)(5*FONT_SCALE_FACTOR), (int)(5*FONT_SCALE_FACTOR)));
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

        JPanel topRightButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, (int)(5*FONT_SCALE_FACTOR), (int)(5*FONT_SCALE_FACTOR)));
        processUnpaidLeaveButton = new JButton("휴직 처리");
        processUnpaidLeaveButton.setFont(enlargedFont);
        topRightButtonsPanel.add(processUnpaidLeaveButton);

        backButton = new JButton("뒤로 가기");
        backButton.setFont(enlargedFont);
        topRightButtonsPanel.add(backButton);
        topPanel.add(topRightButtonsPanel, BorderLayout.EAST);

        gbcRoot.gridx = 0; gbcRoot.gridy = 0; gbcRoot.weighty = 0;
        add(topPanel, gbcRoot);

        // --- 중앙 컨텐츠 패널 (달력 + 근무 기록) ---
        JPanel contentPanel = new JPanel(new BorderLayout((int)(10*FONT_SCALE_FACTOR), (int)(10*FONT_SCALE_FACTOR)));

        JPanel leftPanel = new JPanel(new BorderLayout((int)(5*FONT_SCALE_FACTOR),(int)(5*FONT_SCALE_FACTOR)));
        totalWorkDaysLabel = new JLabel("달력 선택일: 0일", SwingConstants.CENTER);
        totalWorkDaysLabel.setFont(enlargedFontBold);
        calendarWeekdaysLabel = new JLabel("달력 평일: 0일", SwingConstants.CENTER);
        calendarWeekdaysLabel.setFont(enlargedFont);
        actualWorkWeekdaysLabel = new JLabel("실근무 평일: 0일", SwingConstants.CENTER);
        actualWorkWeekdaysLabel.setFont(enlargedFont);
        absentWeekdaysLabel = new JLabel("결근/무급 평일: 0일", SwingConstants.CENTER);
        absentWeekdaysLabel.setFont(enlargedFont);
        absentWeekdaysLabel.setForeground(Color.RED);

        calendarPanel = new CalendarPanel(selectedDays -> { updateWorkdayInfoLabels(selectedDays); if (currentEmployee != null && calendarPanel.isMonthChangedFlag()) { refreshWorkLogForCurrentMonth(false); calendarPanel.clearMonthChangedFlag(); } });


        JPanel calendarButtonPanel = new JPanel(new GridLayout(2,1,(int)(5*FONT_SCALE_FACTOR),(int)(5*FONT_SCALE_FACTOR)));
        syncCalendarButton = new JButton("달력으로 전체 동기화");
        syncCalendarButton.setFont(enlargedFont);
        addSelectedDaysButton = new JButton("선택일 기록표에 추가");
        addSelectedDaysButton.setFont(enlargedFont);
        calendarButtonPanel.add(syncCalendarButton);
        calendarButtonPanel.add(addSelectedDaysButton);

        leftPanel.add(calendarPanel, BorderLayout.CENTER);
        JPanel leftBottomPanel = new JPanel(new BorderLayout((int)(5*FONT_SCALE_FACTOR),(int)(5*FONT_SCALE_FACTOR)));
        JPanel labelsPanel = new JPanel(new GridLayout(4,1,(int)(5*FONT_SCALE_FACTOR),(int)(2*FONT_SCALE_FACTOR)));
        labelsPanel.add(totalWorkDaysLabel);
        labelsPanel.add(calendarWeekdaysLabel);
        labelsPanel.add(actualWorkWeekdaysLabel);
        labelsPanel.add(absentWeekdaysLabel);
        leftBottomPanel.add(labelsPanel, BorderLayout.CENTER);
        leftBottomPanel.add(calendarButtonPanel, BorderLayout.SOUTH);
        leftPanel.add(leftBottomPanel, BorderLayout.SOUTH);

        int calendarPreferredHeight = (int)(450 * FONT_SCALE_FACTOR);
        leftPanel.setPreferredSize(new Dimension((int)(calendarPanel.getPreferredSize().width * 1.1), calendarPreferredHeight));
        contentPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new BorderLayout((int)(10*FONT_SCALE_FACTOR), (int)(10*FONT_SCALE_FACTOR)));
        TitledBorder workLogBorder = BorderFactory.createTitledBorder("근무 시간 기록 (시간/상태 비어있으면 급여 계산에서 제외)");
        workLogBorder.setTitleFont(enlargedFontBold);
        rightPanel.setBorder(workLogBorder);

        String[] columnNames = {"날짜", "출근시간", "퇴근시간", "상태"};
        tableModel = new DefaultTableModel(columnNames, 0) { @Override public Class<?> getColumnClass(int columnIndex) { if (columnIndex == 3) return WorkStatus.class; return String.class; } @Override public boolean isCellEditable(int row, int column) { return column != 0; } };
        workLogTable = new JTable(tableModel);
        workLogTable.setFont(enlargedFont);
        workLogTable.getTableHeader().setFont(enlargedFontBold);
        workLogTable.setRowHeight((int)(workLogTable.getRowHeight() * FONT_SCALE_FACTOR));
        workLogTable.setSurrendersFocusOnKeystroke(true);
        new TableClipboardAdapter(workLogTable);

        workLogTable.setShowGrid(true);
        workLogTable.setGridColor(new Color(220,220,220));
        workLogTable.setIntercellSpacing(new Dimension(1,1));

        TableColumn statusColumn = workLogTable.getColumnModel().getColumn(3);
        JComboBox<WorkStatus> statusComboBoxEditor = new JComboBox<>(WorkStatus.values());
        statusComboBoxEditor.setFont(enlargedFont);
        statusColumn.setCellEditor(new DefaultCellEditor(statusComboBoxEditor));
        workLogTable.setDefaultRenderer(Object.class, new CustomDateRenderer(calendarPanel));
        DefaultCellEditor timeEditor = new DefaultCellEditor(new JTextField()) { @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) { JTextField editor = (JTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column); editor.setFont(enlargedFont); editor.setText(""); return editor; } @Override public Object getCellEditorValue() { String value = ((JTextField) getComponent()).getText(); return formatTimeInput(value); } private String formatTimeInput(String input) { if (input == null || input.trim().isEmpty()) return ""; if (input.contains(":")) { try { LocalTime.parse(input, DateTimeFormatter.ofPattern("HH:mm")); return input; } catch (Exception e) { return ""; } } String digits = input.replaceAll("\\D", ""); String formatted; switch (digits.length()) { case 1: formatted = "0" + digits + ":00"; break; case 2: formatted = digits + ":00"; break; case 3: formatted = "0" + digits.substring(0, 1) + ":" + digits.substring(1, 3); break; case 4: formatted = digits.substring(0, 2) + ":" + digits.substring(2, 4); break; default: return ""; } try { LocalTime.parse(formatted, DateTimeFormatter.ofPattern("HH:mm")); return formatted; } catch (Exception e) { return ""; } } };
        workLogTable.getColumnModel().getColumn(1).setCellEditor(timeEditor);
        workLogTable.getColumnModel().getColumn(2).setCellEditor(timeEditor);

        JScrollPane workLogTableScrollPane = new JScrollPane(workLogTable);

        rightPanel.setPreferredSize(new Dimension((int)(rightPanel.getPreferredSize().width * 1.1) , calendarPreferredHeight));
        rightPanel.add(workLogTableScrollPane, BorderLayout.CENTER);

        JPanel tableControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, (int)(5*FONT_SCALE_FACTOR), (int)(5*FONT_SCALE_FACTOR)));
        JLabel salaryPercentageLabel = new JLabel("급여 적용 %:");
        salaryPercentageLabel.setFont(enlargedFont);
        tableControlPanel.add(salaryPercentageLabel);
        salaryPercentageField = new JTextField(String.format("%.0f",this.currentSalaryPercentage), (int)(4*FONT_SCALE_FACTOR*0.8));
        salaryPercentageField.setFont(enlargedFont);
        tableControlPanel.add(salaryPercentageField);
        applySalaryPercentageButton = new JButton("비율 적용");
        applySalaryPercentageButton.setFont(enlargedFont);
        tableControlPanel.add(applySalaryPercentageButton);
        tableControlPanel.add(Box.createHorizontalStrut((int)(10*FONT_SCALE_FACTOR)));

        JLabel adHocBonusLabel = new JLabel("기간 상여금:");
        adHocBonusLabel.setFont(enlargedFont);
        tableControlPanel.add(adHocBonusLabel);
        adHocBonusField = new JTextField("0", (int)(7*FONT_SCALE_FACTOR*0.8));
        adHocBonusField.setFont(enlargedFont);
        tableControlPanel.add(adHocBonusField);
        applyAdHocBonusButton = new JButton("상여금 적용");
        applyAdHocBonusButton.setFont(enlargedFont);
        tableControlPanel.add(applyAdHocBonusButton);
        tableControlPanel.add(Box.createHorizontalStrut((int)(10*FONT_SCALE_FACTOR)));

        clearWorkLogButton = new JButton("기록 초기화");
        clearWorkLogButton.setFont(enlargedFont);
        tableControlPanel.add(clearWorkLogButton);
        deleteWorkLogEntryButton = new JButton("선택 기록 삭제");
        deleteWorkLogEntryButton.setFont(enlargedFont);
        tableControlPanel.add(deleteWorkLogEntryButton);
        rightPanel.add(tableControlPanel, BorderLayout.SOUTH);

        contentPanel.add(rightPanel, BorderLayout.CENTER);

        gbcRoot.gridy = 1; gbcRoot.weighty = 0;
        gbcRoot.fill = GridBagConstraints.BOTH;
        add(contentPanel, gbcRoot);


        // --- 하단 결과 패널 ---
        JPanel bottomOuterPanel = new JPanel(new BorderLayout((int)(10*FONT_SCALE_FACTOR),(int)(10*FONT_SCALE_FACTOR)));
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, (int)(10*FONT_SCALE_FACTOR), (int)(10*FONT_SCALE_FACTOR)));
        calculateButton = new JButton("급여 계산 실행");
        Font mainActionFont = new Font("맑은 고딕", Font.BOLD, (int)(16*FONT_SCALE_FACTOR*0.9));
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


        JPanel resultsDisplayPanel = new JPanel(new GridLayout(1, 3, (int)(10*FONT_SCALE_FACTOR), 0));

        int textAreaRows = (int)(16 * FONT_SCALE_FACTOR * 0.8);
        int textAreaCols = (int)(80 * FONT_SCALE_FACTOR / 3.5);

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

        gbcRoot.gridy = 2; gbcRoot.weighty = 1.0;
        gbcRoot.fill = GridBagConstraints.BOTH;
        add(bottomOuterPanel, gbcRoot);


        // --- 이벤트 리스너 ---
        backButton.addActionListener(e -> { summaryPage.refreshTableData(); cardLayout.show(mainPanel, "Summary"); });
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
                double percentage = Double.parseDouble(salaryPercentageField.getText().trim());
                if (percentage >= 0) { this.currentSalaryPercentage = percentage; JOptionPane.showMessageDialog(this, "급여 적용 비율이 " + String.format("%.2f", percentage) + "%로 설정되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE); if (currentEmployee != null && currentContractPayrollData != null && !originalContractInfoArea.getText().trim().isEmpty() && !originalContractInfoArea.getText().startsWith("직원을 선택하고")) { runSalaryCalculation(); }
                } else { JOptionPane.showMessageDialog(this, "급여 적용 비율은 0 이상이어야 합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE); salaryPercentageField.setText(String.format("%.0f",this.currentSalaryPercentage)); }
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "숫자 형식으로 비율을 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE); salaryPercentageField.setText(String.format("%.0f",this.currentSalaryPercentage)); }
        });

        applyAdHocBonusButton.addActionListener(e -> {
            try {
                double bonusAmount = Double.parseDouble(adHocBonusField.getText().trim().replace(",", ""));
                this.adHocBonusForCurrentCalculation = bonusAmount;
                this.adHocBonusApplied = true;
                JOptionPane.showMessageDialog(this, "기간 특별 상여금이 " + new DecimalFormat("#,###").format(bonusAmount) + "원으로 설정되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                if (currentEmployee != null && currentContractPayrollData != null && !originalContractInfoArea.getText().trim().isEmpty() && !originalContractInfoArea.getText().startsWith("직원을 선택하고")) { runSalaryCalculation(); }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "숫자 형식으로 상여금을 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                adHocBonusField.setText(new DecimalFormat("#,###").format(this.adHocBonusForCurrentCalculation));
            }
        });
        SwingUtilities.invokeLater(this::updateDetailedWorkdayLabels);
    }


    private long calculateNonNightWorkMinutes(LocalTime workStartTime, LocalTime workEndTime, long grossWorkMinutesInPeriod) {
        if (grossWorkMinutesInPeriod <= 0) return 0;
        long nonNightMinutes = 0;
        LocalTime currentTime = workStartTime;
        final LocalTime NIGHT_START_THRESHOLD = LocalTime.of(22, 0);
        final LocalTime NIGHT_END_THRESHOLD = LocalTime.of(6, 0);

        for (long i = 0; i < grossWorkMinutesInPeriod; i++) {
            boolean isDuringNight;
            if (NIGHT_START_THRESHOLD.isAfter(NIGHT_END_THRESHOLD)) { // 22:00 > 06:00 (true)
                isDuringNight = currentTime.compareTo(NIGHT_START_THRESHOLD) >= 0 || currentTime.compareTo(NIGHT_END_THRESHOLD) < 0;
            } else {
                isDuringNight = currentTime.compareTo(NIGHT_START_THRESHOLD) >= 0 && currentTime.compareTo(NIGHT_END_THRESHOLD) < 0;
            }

            if (!isDuringNight) nonNightMinutes++;
            currentTime = currentTime.plusMinutes(1);
        }
        return nonNightMinutes;
    }


    private void runSalaryCalculation() {
        if (currentEmployee == null || currentContractPayrollData == null) { JOptionPane.showMessageDialog(this, "먼저 직원을 선택하고 정보를 불러와주세요.", "경고", JOptionPane.WARNING_MESSAGE); return; }
        applyToDbButton.setEnabled(false);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm"); DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<WorkRecord> recordsToCalculate = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String dateStr = (String) tableModel.getValueAt(i, 0); String startTimeStr = (String) tableModel.getValueAt(i, 1); String endTimeStr = (String) tableModel.getValueAt(i, 2); Object statusObj = tableModel.getValueAt(i, 3); WorkStatus status;
            if (statusObj instanceof WorkStatus) { status = (WorkStatus) statusObj; } else { JOptionPane.showMessageDialog(this, (i + 1) + "행 상태값 오류 ("+ (statusObj != null ? statusObj.toString() : "null") +")", "오류", JOptionPane.ERROR_MESSAGE); return; }
            try { LocalDate date = LocalDate.parse(dateStr, dateFormatter);
                if (startTimeStr == null || startTimeStr.trim().isEmpty() || endTimeStr == null || endTimeStr.trim().isEmpty() || status == WorkStatus.UNPAID_HOLIDAY || status == WorkStatus.ABSENCE) {
                    recordsToCalculate.add(new WorkRecord(date, null, null, status, false, 0));
                    continue;
                }
                LocalTime startTime = LocalTime.parse(startTimeStr, timeFormatter); LocalTime endTime = LocalTime.parse(endTimeStr, timeFormatter);
                Duration duration = Duration.between(startTime, endTime); if (endTime.isBefore(startTime)) duration = duration.plusDays(1); long grossWorkMinutesPerDay = duration.toMinutes();

                long nonNightWorkMinutes = calculateNonNightWorkMinutes(startTime, endTime, grossWorkMinutesPerDay);
                long breakMinutesPerDay = (nonNightWorkMinutes / (4 * 60)) * 30;
                if (nonNightWorkMinutes >= 8 * 60) breakMinutesPerDay = Math.max(breakMinutesPerDay, 60);

                long netWorkMinutesPerDay = grossWorkMinutesPerDay - breakMinutesPerDay;
                netWorkMinutesPerDay = Math.max(0, netWorkMinutesPerDay);

                boolean isOriginallyPublicHoliday = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY || calendarPanel.isPublicHoliday(date);
                recordsToCalculate.add(new WorkRecord(date, startTime, endTime, status, isOriginallyPublicHoliday, netWorkMinutesPerDay));
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, String.format("%d행 날짜/시간 형식 오류: %s", i + 1, ex.getMessage()),"입력 오류", JOptionPane.ERROR_MESSAGE); return; }
        }

        Set<LocalDate> calendarSelectedWeekdays = calendarPanel.getSelectedDatesWithStatus().stream()
                .filter(info -> (Boolean)info.get("isSelected"))
                .map(info -> (LocalDate)info.get("date"))
                .filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY && !calendarPanel.isPublicHoliday(date))
                .collect(Collectors.toSet());

        this.missedOrUnpaidOrAbsentWeekdayCountForDB = (int) calendarSelectedWeekdays.stream().filter(calWd -> {
            return recordsToCalculate.stream().noneMatch(rec -> rec.date.equals(calWd) &&
                    (rec.status == WorkStatus.NORMAL || rec.status == WorkStatus.PAID_HOLIDAY) &&
                    rec.startTime != null && rec.endTime != null && !rec.startTime.toString().isEmpty() && !rec.endTime.toString().isEmpty());
        }).count();

        updateDetailedWorkdayLabels();
        calculateAndStoreSalary(recordsToCalculate, currentContractPayrollData, currentEmployee); // Pass currentEmployee
        applyToDbButton.setEnabled(true);
    }

    private void calculateAndStoreSalary(List<WorkRecord> records, Payroll contractData, Employee employee) { // Added Employee parameter
        // 0. Reset last calculated values
        this.lastCalculatedHireProrationRatio = 1.0;
        this.lastCalculatedAttendanceBasedDisplayPaymentRatio = 1.0;
        this.lastCalculatedPremiumAdditions = 0.0;
        this.lastCalculatedWeeklyAbsencePenalty = 0.0;
        this.lastCalculatedPaidPremiumMinutes = 0L;
        this.lastCalculatedTotalShortfallMinutes = 0L;

        // 1. Hire Proration
        if (employee != null && employee.getHireDate() != null && !employee.getHireDate().isEmpty()) {
            try {
                LocalDate hireDate = LocalDate.parse(employee.getHireDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                YearMonth currentPeriod = calendarPanel.getCurrentYearMonth();
                if (hireDate.getYear() == currentPeriod.getYear() && hireDate.getMonth() == currentPeriod.getMonth() && hireDate.getDayOfMonth() != 1) {
                    int totalDaysInMonth = currentPeriod.lengthOfMonth();
                    long daysFromHireDateToEndOfMonth = (long)currentPeriod.atEndOfMonth().getDayOfMonth() - hireDate.getDayOfMonth() + 1;
                    this.lastCalculatedHireProrationRatio = (totalDaysInMonth > 0) ? ((double) daysFromHireDateToEndOfMonth / totalDaysInMonth) : 0;
                }
            } catch (DateTimeParseException ex) { System.err.println("일할계산 입사일 파싱 오류: " + ex.getMessage()); }
        }

        // Prorate original contract items from Payroll object (which reflects EmployeeManagementPage logic)
        double proratedContractBasicOriginal = contractData.getMonthlyBasicSalary() * this.lastCalculatedHireProrationRatio;
        double proratedContractFixedOTOriginal = contractData.getFixedOvertimeAllowance() * this.lastCalculatedHireProrationRatio;
        double proratedContractBonus = contractData.getBonus() * this.lastCalculatedHireProrationRatio;
        double proratedOtherAllowances = contractData.getOtherAllowance() * this.lastCalculatedHireProrationRatio;
        double proratedMealAllowances = contractData.getMealAllowance() * this.lastCalculatedHireProrationRatio;
        double proratedVehicleFees = contractData.getVehicleMaintenanceFee() * this.lastCalculatedHireProrationRatio;
        double proratedResearchExpenses = contractData.getResearchDevelopmentExpense() * this.lastCalculatedHireProrationRatio;
        double proratedChildcareAllowances = contractData.getChildcareAllowance() * this.lastCalculatedHireProrationRatio;

        // 2. Calculate "통상 시급" (Ordinary Hourly Wage) based on Employee's Annual Salary
        // This is the rate (e.g. 13,393) the user refers to, derived from Annual Salary.
        double employeeAnnualSalary = (employee != null) ? employee.getAnnualSalary() : 0;
        double monthlyEquivalentFromAnnual = (employeeAnnualSalary / 12.0) * this.lastCalculatedHireProrationRatio;

        double employeeOrdinaryHourlyRate = (STANDARD_TOTAL_HOURS_FOR_RATE_CALC > 0 && monthlyEquivalentFromAnnual > 0) ?
                (monthlyEquivalentFromAnnual / STANDARD_TOTAL_HOURS_FOR_RATE_CALC) : 0;
        double employeeOrdinaryMinuteRate = employeeOrdinaryHourlyRate / 60.0;


        // 3. Calculate total shortfall minutes
        this.lastCalculatedTotalShortfallMinutes = 0L;
        for (WorkRecord record : records) {
            DayOfWeek dow = record.date.getDayOfWeek();
            boolean isCalendarWeekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !calendarPanel.isPublicHoliday(record.date);
            if (isCalendarWeekday) {
                if (record.status == WorkStatus.ABSENCE || record.status == WorkStatus.UNPAID_HOLIDAY) {
                    this.lastCalculatedTotalShortfallMinutes += (8 * 60);
                } else if (record.status == WorkStatus.NORMAL && record.netWorkMinutes < (8*60)) { // Includes 0 minutes if times are empty
                    this.lastCalculatedTotalShortfallMinutes += ((8*60) - record.netWorkMinutes);
                }
            }
        }

        // 4. Total Shortfall Monetary Value (for "근로부족분", "결근", "무급휴가")
        // This uses the employeeOrdinaryMinuteRate (연봉 기반 통상시급 * 1)
        double totalShortfallMonetaryDeduction = this.lastCalculatedTotalShortfallMinutes * employeeOrdinaryMinuteRate;

        // 5. Weekly Holiday Allowance Deduction (주휴수당 차감 - using employeeOrdinaryHourlyRate * 1)
        this.lastCalculatedWeeklyAbsencePenalty = 0.0;
        final double dailyAmountForWeeklyHolidayPenalty = employeeOrdinaryHourlyRate * 8.0; // 연봉 기반 통상시급 * 8시간 * 1
        Set<Integer> penalizedWeeks = new HashSet<>();
        Set<Integer> weeksWithAbsence = new HashSet<>();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (WorkRecord rec : records) {
            if (rec.status == WorkStatus.ABSENCE) {
                DayOfWeek dow = rec.date.getDayOfWeek();
                if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !calendarPanel.isPublicHoliday(rec.date)) {
                    weeksWithAbsence.add(rec.date.get(weekFields.weekOfWeekBasedYear()));
                }
            }
        }
        for (Integer weekNum : weeksWithAbsence) {
            if (!penalizedWeeks.contains(weekNum)) {
                this.lastCalculatedWeeklyAbsencePenalty += dailyAmountForWeeklyHolidayPenalty;
                penalizedWeeks.add(weekNum);
            }
        }

        // 6. Additional Overtime Premium (추가 연장 수당 - using employeeOrdinaryHourlyRate * 1.5)
        this.lastCalculatedPremiumAdditions = 0;
        this.lastCalculatedPaidPremiumMinutes = 0L;
        long totalActualOvertimeOrHolidayMinutesInMonth = 0L;

        final double monthlyFixedOtThresholdMinutes = NOMINAL_FIXED_OVERTIME_HOURS * 60.0;

        for (WorkRecord record : records) {
            if (record.status == WorkStatus.ABSENCE || record.status == WorkStatus.UNPAID_HOLIDAY) continue;
            long dailyNetWorkMinutes = record.netWorkMinutes;
            if (dailyNetWorkMinutes <= 0) continue;
            long dailyOvertimeOrHolidayMinutesEligibleForPremium = 0;
            boolean isActualHolidayWork = record.isOriginallyPublicHoliday ||
                    (record.date.getDayOfWeek() == DayOfWeek.SATURDAY && record.status == WorkStatus.NORMAL && record.netWorkMinutes > 0) ||
                    (record.status == WorkStatus.PAID_HOLIDAY && record.netWorkMinutes > 0);
            if (isActualHolidayWork) {
                dailyOvertimeOrHolidayMinutesEligibleForPremium = dailyNetWorkMinutes;
            } else {
                dailyOvertimeOrHolidayMinutesEligibleForPremium = Math.max(0, dailyNetWorkMinutes - (8 * 60));
            }
            totalActualOvertimeOrHolidayMinutesInMonth += dailyOvertimeOrHolidayMinutesEligibleForPremium;
        }

        if (totalActualOvertimeOrHolidayMinutesInMonth > monthlyFixedOtThresholdMinutes) {
            long extraOvertimeMinutes = totalActualOvertimeOrHolidayMinutesInMonth - (long)monthlyFixedOtThresholdMinutes;
            this.lastCalculatedPaidPremiumMinutes = Math.max(0, extraOvertimeMinutes);
            double actualAdditionalOvertimeHours = this.lastCalculatedPaidPremiumMinutes / 60.0;
            this.lastCalculatedPremiumAdditions = employeeOrdinaryHourlyRate * actualAdditionalOvertimeHours * 1.5; // 연봉 기반 통상시급 * 추가시간 * 1.5
        }


        // 7. Distribute Shortfall Deduction Proportionally
        // These are the base amounts for proportional deduction (already prorated for hire date, reflects EMP page logic)
        double itemBasicForProportion = proratedContractBasicOriginal;
        double itemFixedOTForProportion = proratedContractFixedOTOriginal;
        double itemMealForProportion = proratedMealAllowances;
        double itemChildcareForProportion = proratedChildcareAllowances;
        double itemVehicleForProportion = proratedVehicleFees;
        double itemResearchForProportion = proratedResearchExpenses;

        double sumOfReducibleItems = itemBasicForProportion + itemFixedOTForProportion +
                itemMealForProportion + itemChildcareForProportion +
                itemVehicleForProportion + itemResearchForProportion;

        Map<String, Double> proportionalDeductions = new HashMap<>();
        if (sumOfReducibleItems > 0) {
            proportionalDeductions.put("Basic", totalShortfallMonetaryDeduction * (itemBasicForProportion / sumOfReducibleItems));
            proportionalDeductions.put("FixedOT", totalShortfallMonetaryDeduction * (itemFixedOTForProportion / sumOfReducibleItems));
            proportionalDeductions.put("Meal", totalShortfallMonetaryDeduction * (itemMealForProportion / sumOfReducibleItems));
            proportionalDeductions.put("Childcare", totalShortfallMonetaryDeduction * (itemChildcareForProportion / sumOfReducibleItems));
            proportionalDeductions.put("Vehicle", totalShortfallMonetaryDeduction * (itemVehicleForProportion / sumOfReducibleItems));
            proportionalDeductions.put("Research", totalShortfallMonetaryDeduction * (itemResearchForProportion / sumOfReducibleItems));
        } else {
            for(String key : new String[]{"Basic", "FixedOT", "Meal", "Childcare", "Vehicle", "Research"}){
                proportionalDeductions.put(key, 0.0);
            }
        }


        // 8. Interim Payment Components (before percentage application)
        double interimBasicPay = itemBasicForProportion - proportionalDeductions.getOrDefault("Basic", 0.0) - this.lastCalculatedWeeklyAbsencePenalty;
        double interimFixedOT = itemFixedOTForProportion - proportionalDeductions.getOrDefault("FixedOT", 0.0);
        double interimMeal = itemMealForProportion - proportionalDeductions.getOrDefault("Meal", 0.0);
        double interimChildcare = itemChildcareForProportion - proportionalDeductions.getOrDefault("Childcare", 0.0);
        double interimVehicle = itemVehicleForProportion - proportionalDeductions.getOrDefault("Vehicle", 0.0);
        double interimResearch = itemResearchForProportion - proportionalDeductions.getOrDefault("Research", 0.0);

        double interimBonus = proratedContractBonus;
        double interimOther = proratedOtherAllowances;
        double interimAdditionalOT_premium = this.lastCalculatedPremiumAdditions;
        double adHocBonusToAdd = adHocBonusApplied ? this.adHocBonusForCurrentCalculation : 0;

        // For display ratio:
        double sumProratedItemsForDisplayRatio = itemBasicForProportion + itemFixedOTForProportion + proratedContractBonus +
                proratedOtherAllowances + itemMealForProportion + itemVehicleForProportion +
                itemResearchForProportion + itemChildcareForProportion;

        if (sumProratedItemsForDisplayRatio > 0) {
            this.lastCalculatedAttendanceBasedDisplayPaymentRatio = Math.max(0, (sumProratedItemsForDisplayRatio - totalShortfallMonetaryDeduction - this.lastCalculatedWeeklyAbsencePenalty) / sumProratedItemsForDisplayRatio);
        } else if (totalShortfallMonetaryDeduction > 0 || this.lastCalculatedWeeklyAbsencePenalty > 0) {
            this.lastCalculatedAttendanceBasedDisplayPaymentRatio = 0;
        } else {
            this.lastCalculatedAttendanceBasedDisplayPaymentRatio = 1.0;
        }


        // 9. Apply Salary Percentage
        double percMultiplier;
        double amountAddedToOtherAllowanceDueToExcessPercentage = 0;
        double baseForExcessPercentageCalc = proratedContractBasicOriginal + proratedContractFixedOTOriginal + proratedContractBonus +
                proratedOtherAllowances + proratedMealAllowances + proratedVehicleFees +
                proratedResearchExpenses + proratedChildcareAllowances; // This is the sum of all EMP-defined prorated items


        if (this.currentSalaryPercentage <= 100.0) {
            percMultiplier = this.currentSalaryPercentage / 100.0;
        } else {
            percMultiplier = 1.0;
            double excessMultiplier = (this.currentSalaryPercentage - 100.0) / 100.0;
            amountAddedToOtherAllowanceDueToExcessPercentage = baseForExcessPercentageCalc * excessMultiplier;
        }

        this.finalAdjustedBasicPay = interimBasicPay * percMultiplier;
        this.finalAdjustedFixedOvertimeAllowance = interimFixedOT * percMultiplier;
        this.finalAdjustedBonus = (interimBonus + adHocBonusToAdd) * percMultiplier;

        this.finalAdjustedOtherAllowance = interimOther * percMultiplier + amountAddedToOtherAllowanceDueToExcessPercentage;
        this.finalAdjustedMealAllowance = interimMeal * percMultiplier;
        this.finalAdjustedVehicleMaintenanceFee = interimVehicle * percMultiplier;
        this.finalAdjustedResearchDevelopmentExpense = interimResearch * percMultiplier;
        this.finalAdjustedChildcareAllowance = interimChildcare * percMultiplier;

        this.finalAdjustedAdditionalOvertimePremium = interimAdditionalOT_premium * percMultiplier;

        // Ensure non-negative final values
        this.finalAdjustedBasicPay = Math.max(0, this.finalAdjustedBasicPay);
        this.finalAdjustedFixedOvertimeAllowance = Math.max(0, this.finalAdjustedFixedOvertimeAllowance);
        this.finalAdjustedBonus = Math.max(0, this.finalAdjustedBonus);
        this.finalAdjustedOtherAllowance = Math.max(0, this.finalAdjustedOtherAllowance);
        this.finalAdjustedMealAllowance = Math.max(0, this.finalAdjustedMealAllowance);
        this.finalAdjustedVehicleMaintenanceFee = Math.max(0, this.finalAdjustedVehicleMaintenanceFee);
        this.finalAdjustedResearchDevelopmentExpense = Math.max(0, this.finalAdjustedResearchDevelopmentExpense);
        this.finalAdjustedChildcareAllowance = Math.max(0, this.finalAdjustedChildcareAllowance);
        this.finalAdjustedAdditionalOvertimePremium = Math.max(0, this.finalAdjustedAdditionalOvertimePremium);

        printFinalResults(contractData, totalShortfallMonetaryDeduction, amountAddedToOtherAllowanceDueToExcessPercentage);
    }

    // printFinalResults, saveProcessedPayrollDataToDB, and other helper methods remain largely the same,
    // but their display text might need slight adjustments to reflect that "통상시급" is used for deductions.
    // For example, in printFinalResults, when displaying shortfall deduction, clarify it's based on "통상시급(*1)".

    private void printFinalResults(Payroll originalContract, double shortfallMonetaryDeductionValue, double excessToOtherAllowance) {
        DecimalFormat df = new DecimalFormat("#,###");

        // 1. 원본 계약 정보 (입사일 비례배분된 계약상 금액 표시)
        StringBuilder sbOriginal = new StringBuilder();
        sbOriginal.append("===== 원본 계약 정보 (");
        if (this.lastCalculatedHireProrationRatio < 1.0 && this.lastCalculatedHireProrationRatio >= 0) {
            sbOriginal.append(String.format("중도입사 일할: %.2f%%", this.lastCalculatedHireProrationRatio * 100));
        } else {
            sbOriginal.append("정상월");
        }
        sbOriginal.append(") =====\n");
        double originalProratedBasic = originalContract.getMonthlyBasicSalary() * this.lastCalculatedHireProrationRatio;
        double originalProratedFixedOT = originalContract.getFixedOvertimeAllowance() * this.lastCalculatedHireProrationRatio;
        double originalProratedBonus = originalContract.getBonus() * this.lastCalculatedHireProrationRatio;
        double originalProratedOther = originalContract.getOtherAllowance() * this.lastCalculatedHireProrationRatio;
        double originalProratedMeal = originalContract.getMealAllowance() * this.lastCalculatedHireProrationRatio;
        double originalProratedVehicle = originalContract.getVehicleMaintenanceFee() * this.lastCalculatedHireProrationRatio;
        double originalProratedResearch = originalContract.getResearchDevelopmentExpense() * this.lastCalculatedHireProrationRatio;
        double originalProratedChildcare = originalContract.getChildcareAllowance() * this.lastCalculatedHireProrationRatio;

        sbOriginal.append(String.format("  기본급여        : %12s 원\n", df.format(Math.round(originalProratedBasic))));
        sbOriginal.append(String.format("  고정연장수당    : %12s 원\n", df.format(Math.round(originalProratedFixedOT))));
        sbOriginal.append(String.format("  상여금          : %12s 원\n", df.format(Math.round(originalProratedBonus))));
        sbOriginal.append(String.format("  기타수당        : %12s 원\n", df.format(Math.round(originalProratedOther))));
        sbOriginal.append(String.format("  식대            : %12s 원\n", df.format(Math.round(originalProratedMeal))));
        sbOriginal.append(String.format("  차량유지비      : %12s 원\n", df.format(Math.round(originalProratedVehicle))));
        sbOriginal.append(String.format("  연구개발비      : %12s 원\n", df.format(Math.round(originalProratedResearch))));
        sbOriginal.append(String.format("  육아수당        : %12s 원\n", df.format(Math.round(originalProratedChildcare))));
        sbOriginal.append("  -------------------------------------\n");
        double originalContractTotalProrated = originalProratedBasic + originalProratedFixedOT + originalProratedBonus +
                originalProratedOther + originalProratedMeal + originalProratedVehicle +
                originalProratedResearch + originalProratedChildcare;
        sbOriginal.append(String.format("  원본 계약 총액(일할):%10s 원\n", df.format(Math.round(originalContractTotalProrated))));
        originalContractInfoArea.setText(sbOriginal.toString());
        originalContractInfoArea.setCaretPosition(0);

        // 2. 조정 내역
        StringBuilder sbAdjustments = new StringBuilder();
        sbAdjustments.append(String.format("===== 근태 반영 조정 (참고 지급률 %.2f%%) =====\n", this.lastCalculatedAttendanceBasedDisplayPaymentRatio * 100));

        String shortfallTimeStr = String.format("%d시간 %d분", this.lastCalculatedTotalShortfallMinutes / 60, this.lastCalculatedTotalShortfallMinutes % 60);
        sbAdjustments.append(String.format("  (-) 근무 부족분   : %10s 원\n      (총 %s 부족, 통상시급(*1) 기준, 비율 차감됨)\n", df.format(Math.round(shortfallMonetaryDeductionValue)), shortfallTimeStr));

        if (this.lastCalculatedWeeklyAbsencePenalty > 0) {
            sbAdjustments.append(String.format("  (-) 주휴수당 차감 : %10s 원\n      (결근 주 발생, 통상시급(*1) 기준 차감)\n", df.format(Math.round(this.lastCalculatedWeeklyAbsencePenalty))));
        }
        if (this.lastCalculatedPremiumAdditions > 0 || this.lastCalculatedPaidPremiumMinutes > 0) {
            String premiumTimeStr = String.format("%d시간 %d분", this.lastCalculatedPaidPremiumMinutes / 60, this.lastCalculatedPaidPremiumMinutes % 60);
            sbAdjustments.append(String.format("  (+) 추가 연장수당 : %10s 원\n      (고정연장시간 초과 %s, 통상시급 * 1.5배 적용)\n", df.format(Math.round(this.lastCalculatedPremiumAdditions)), premiumTimeStr));
        }
        if (this.currentSalaryPercentage > 100.0 && excessToOtherAllowance > 0) {
            sbAdjustments.append(String.format("  (+) 급여비율 초과분: %10s 원\n      (기타수당에 가산, 원본계약총액 기준 %.0f%%)\n", df.format(Math.round(excessToOtherAllowance)), (this.currentSalaryPercentage - 100.0) ));
        }
        boolean noChanges = Math.abs(shortfallMonetaryDeductionValue) < 1 &&
                this.lastCalculatedWeeklyAbsencePenalty == 0 &&
                (this.lastCalculatedPremiumAdditions == 0 && this.lastCalculatedPaidPremiumMinutes == 0) &&
                !(this.currentSalaryPercentage > 100.0 && excessToOtherAllowance > 0);
        if (noChanges) {
            sbAdjustments.append("  * 주요 변동 내역 없음 (근태 100% 및 적용률 100% 가정 시)\n");
        }
        adjustmentsInfoArea.setText(sbAdjustments.toString());
        adjustmentsInfoArea.setCaretPosition(0);

        // 3. 최종 지급액
        StringBuilder sbFinal = new StringBuilder();
        sbFinal.append(String.format("===== 최종 지급 내역 (적용률: %.1f%%) =====\n", this.currentSalaryPercentage));
        if (adHocBonusApplied && this.adHocBonusForCurrentCalculation != 0) {
            double finalAdHocBonus = this.adHocBonusForCurrentCalculation * (this.currentSalaryPercentage > 100 ? 1.0 : this.currentSalaryPercentage/100.0);
            sbFinal.append(String.format("  (기간 특별 상여: %s 원 포함)\n", df.format(Math.round(finalAdHocBonus))));
        }
        if (this.currentSalaryPercentage > 100.0 && excessToOtherAllowance > 0) {
            sbFinal.append(String.format("  (급여비율 초과분 %.0f%% 기타수당에 가산됨)\n",(this.currentSalaryPercentage - 100.0)));
        }
        sbFinal.append(String.format("  기본급여        : %12s 원\n", df.format(Math.round(this.finalAdjustedBasicPay))));
        sbFinal.append(String.format("  고정연장수당    : %12s 원\n", df.format(Math.round(this.finalAdjustedFixedOvertimeAllowance))));
        if (this.finalAdjustedAdditionalOvertimePremium > 0 || this.lastCalculatedPremiumAdditions > 0) {
            sbFinal.append(String.format("  추가연장수당    : %12s 원\n", df.format(Math.round(this.finalAdjustedAdditionalOvertimePremium))));
        }
        sbFinal.append(String.format("  상여금          : %12s 원\n", df.format(Math.round(this.finalAdjustedBonus))));
        sbFinal.append(String.format("  기타수당        : %12s 원\n", df.format(Math.round(this.finalAdjustedOtherAllowance))));
        sbFinal.append(String.format("  식대            : %12s 원\n", df.format(Math.round(this.finalAdjustedMealAllowance))));
        sbFinal.append(String.format("  차량유지비      : %12s 원\n", df.format(Math.round(this.finalAdjustedVehicleMaintenanceFee))));
        sbFinal.append(String.format("  연구개발비      : %12s 원\n", df.format(Math.round(this.finalAdjustedResearchDevelopmentExpense))));
        sbFinal.append(String.format("  육아수당        : %12s 원\n", df.format(Math.round(this.finalAdjustedChildcareAllowance))));
        sbFinal.append("======================================\n");
        double finalTotalPayForDisplay = this.finalAdjustedBasicPay + this.finalAdjustedFixedOvertimeAllowance + this.finalAdjustedAdditionalOvertimePremium + this.finalAdjustedBonus + this.finalAdjustedOtherAllowance + this.finalAdjustedMealAllowance + this.finalAdjustedVehicleMaintenanceFee + this.finalAdjustedResearchDevelopmentExpense + this.finalAdjustedChildcareAllowance;
        sbFinal.append(String.format("  >> 최종 지급액(세전): %10s 원\n", df.format(Math.round(finalTotalPayForDisplay))));
        sbFinal.append("======================================\n");
        finalPayoutInfoArea.setText(sbFinal.toString());
        finalPayoutInfoArea.setCaretPosition(0);
    }

    private void saveProcessedPayrollDataToDB() {
        if (currentEmployee == null || currentContractPayrollData == null) { JOptionPane.showMessageDialog(this, "먼저 직원을 선택하고 급여 계산을 실행해주세요.", "경고", JOptionPane.WARNING_MESSAGE); return; }
        if (!applyToDbButton.isEnabled()) { JOptionPane.showMessageDialog(this, "먼저 '급여 계산 실행' 버튼을 눌러 최신 결과로 갱신해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE); return; }

        int unpaidDaysForDB = 0;
        int unauthorizedAbsenceDaysForDB = 0;

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        List<WorkLogEntry> workLogEntries = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                String dateStr = (String) tableModel.getValueAt(i, 0);
                String startTimeStr = (String) tableModel.getValueAt(i, 1);
                String endTimeStr = (String) tableModel.getValueAt(i, 2);
                WorkStatus status = (WorkStatus) tableModel.getValueAt(i, 3);
                LocalDate workDate = LocalDate.parse(dateStr, dateFormatter);
                LocalTime startTime = (startTimeStr == null || startTimeStr.trim().isEmpty()) ? null : LocalTime.parse(startTimeStr, timeFormatter);
                LocalTime endTime = (endTimeStr == null || endTimeStr.trim().isEmpty()) ? null : LocalTime.parse(endTimeStr, timeFormatter);
                workLogEntries.add(new WorkLogEntry(currentEmployee.getId(), workDate, startTime, endTime, status));

                DayOfWeek dow = workDate.getDayOfWeek();
                boolean isCalendarWeekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !calendarPanel.isPublicHoliday(workDate);
                if(isCalendarWeekday && (status == WorkStatus.ABSENCE || status == WorkStatus.UNPAID_HOLIDAY)) {
                    unpaidDaysForDB++;
                }
                if(isCalendarWeekday && status == WorkStatus.ABSENCE) {
                    unauthorizedAbsenceDaysForDB++;
                }
            } catch (Exception e) {
                System.err.println("근무 기록 DB 저장용 변환 오류: " + e.getMessage());
            }
        }
        payrollManager.saveWorkRecords(currentEmployee.getId(), calendarPanel.getCurrentYearMonth(), workLogEntries);

        payrollManager.finalizeMonthlyPay(
                currentEmployee.getId(),
                calendarPanel.getCurrentYearMonth(),
                unpaidDaysForDB,
                unauthorizedAbsenceDaysForDB,
                this.finalAdjustedBasicPay,
                this.finalAdjustedFixedOvertimeAllowance,
                this.finalAdjustedAdditionalOvertimePremium,
                this.finalAdjustedBonus,
                this.finalAdjustedOtherAllowance,
                this.finalAdjustedMealAllowance,
                this.finalAdjustedVehicleMaintenanceFee,
                this.finalAdjustedResearchDevelopmentExpense,
                this.finalAdjustedChildcareAllowance
        );
        JOptionPane.showMessageDialog(this, calendarPanel.getCurrentYearMonth().toString() + "의 계산된 급여 및 근무 기록이 DB에 성공적으로 적용되었습니다.", "적용 완료", JOptionPane.INFORMATION_MESSAGE);
        applyToDbButton.setEnabled(false);
    }

    private void processUnpaidLeaveAction() {
        if (currentEmployee == null) { JOptionPane.showMessageDialog(this, "먼저 직원을 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE); return; }
        YearMonth currentPeriod = calendarPanel.getCurrentYearMonth();
        int confirm = JOptionPane.showConfirmDialog(this, currentEmployee.getName() + " 님의 " + currentPeriod.toString() + " 기간을 무급 휴직으로 처리하시겠습니까?\n해당 월의 모든 근무 기록이 삭제되고, 모든 급여 항목이 0으로 DB에 저장됩니다.", "휴직 처리 확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            tableModel.setRowCount(0);
            payrollManager.saveWorkRecords(currentEmployee.getId(), currentPeriod, new ArrayList<>());

            int weekdaysInMonth = 0;
            LocalDate dateInMonthLoop = currentPeriod.atDay(1);
            LocalDate lastDayOfMonthInLoop = currentPeriod.atEndOfMonth();
            while (!dateInMonthLoop.isAfter(lastDayOfMonthInLoop)) {
                DayOfWeek day = dateInMonthLoop.getDayOfWeek();
                if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !calendarPanel.isPublicHoliday(dateInMonthLoop)) {
                    weekdaysInMonth++;
                }
                dateInMonthLoop = dateInMonthLoop.plusDays(1);
            }
            payrollManager.finalizeMonthlyPay(currentEmployee.getId(), currentPeriod,
                    weekdaysInMonth, weekdaysInMonth,
                    0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0);

            String unpaidLeaveMsg = currentEmployee.getName() + " 님의 " + currentPeriod.toString() + " 기간이 무급 휴직으로 처리되었습니다.\n1페이지 요약에서 해당 연/월로 조회 시 '무급 휴직'으로 표시됩니다.";
            originalContractInfoArea.setText(unpaidLeaveMsg);
            adjustmentsInfoArea.setText("");
            finalPayoutInfoArea.setText("");
            applyToDbButton.setEnabled(false);
            updateDetailedWorkdayLabels();
            JOptionPane.showMessageDialog(this, "휴직 처리가 완료되었습니다.", "처리 완료", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void filterWorkLogTableForMidMonthHire(LocalDate hireDate) {
        if (tableModel == null || hireDate == null) return;
        DateTimeFormatter tableDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
            String dateStr = (String) tableModel.getValueAt(i, 0);
            if (dateStr == null || dateStr.trim().isEmpty()) continue;
            try {
                LocalDate recordDate = LocalDate.parse(dateStr, tableDateFormatter);
                if (recordDate.isBefore(hireDate)) {
                    tableModel.removeRow(i);
                }
            } catch (DateTimeParseException ex) {
                System.err.println("테이블 날짜 파싱 오류 (중도입사자 필터링용 메소드): " + dateStr + " - " + ex.getMessage());
            }
        }
    }

    private void addSelectedCalendarDaysToTable() {
        if (currentEmployee == null) { JOptionPane.showMessageDialog(this, "먼저 직원을 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE); return; }
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> calendarInfos = calendarPanel.getSelectedDatesWithStatus();
        Set<LocalDate> existingTableDates = new HashSet<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                existingTableDates.add(LocalDate.parse((String) tableModel.getValueAt(i, 0), dateFormatter));
            } catch (Exception ignored) {}
        }
        int addedCount = 0;
        for (Map<String, Object> info : calendarInfos) {
            LocalDate date = (LocalDate) info.get("date");
            if ((Boolean) info.get("isSelected") && !existingTableDates.contains(date)) {
                WorkStatus initialStatus;
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                if (calendarPanel.isPublicHoliday(date)) initialStatus = WorkStatus.PAID_HOLIDAY;
                else if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) initialStatus = WorkStatus.NORMAL;
                else initialStatus = WorkStatus.NORMAL;
                tableModel.addRow(new Object[]{date.format(dateFormatter), "09:00", "18:00", initialStatus});
                addedCount++;
            }
        }
        if (addedCount > 0) {
            JOptionPane.showMessageDialog(this, addedCount + "일의 근무 기록이 추가되었습니다.", "기록 추가 완료", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "새로 추가할 날짜가 없거나 이미 기록표에 존재합니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
        }

        sortTableByDate();
        if (currentEmployee != null && currentEmployee.getHireDate() != null && !currentEmployee.getHireDate().isEmpty()) {
            try {
                LocalDate hireDate = LocalDate.parse(currentEmployee.getHireDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                YearMonth currentTableMonth = calendarPanel.getCurrentYearMonth();
                if (hireDate.getYear() == currentTableMonth.getYear() && hireDate.getMonth() == currentTableMonth.getMonth()) {
                    filterWorkLogTableForMidMonthHire(hireDate);
                }
            } catch (DateTimeParseException e) {
                System.err.println("입사일 파싱 오류 (중도입사자 필터링 중 - addSelectedCalendarDaysToTable): " + currentEmployee.getHireDate());
            }
        }
        updateDetailedWorkdayLabels();
        applyToDbButton.setEnabled(false);
    }

    private void sortTableByDate() {
        List<Object[]> rowDataList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            rowDataList.add(new Object[]{tableModel.getValueAt(i,0), tableModel.getValueAt(i,1), tableModel.getValueAt(i,2), tableModel.getValueAt(i,3)});
        }
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try {
            rowDataList.sort(Comparator.comparing(row -> {
                try {
                    return LocalDate.parse((String)row[0], dateFormatter);
                } catch (DateTimeParseException e) {
                    System.err.println("날짜 정렬 중 파싱 오류: " + row[0]);
                    return LocalDate.MIN;
                }
            }));
        } catch (Exception e) {
            System.err.println("테이블 날짜 정렬 중 예외 발생: " + e.getMessage());
        }

        tableModel.setRowCount(0);
        for (Object[] row : rowDataList) {
            tableModel.addRow(row);
        }
    }

    private void clearWorkLogTableAction() {
        if (currentEmployee == null) { JOptionPane.showMessageDialog(this, "먼저 직원을 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "정말로 현재 직원의 모든 근무시간 기록을 초기화하고\n달력 기준으로 새로고침 하시겠습니까?", "근무 기록 초기화 확인", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            tableModel.setRowCount(0);
            populateTableFromCalendar(false);
            updateDetailedWorkdayLabels();
            applyToDbButton.setEnabled(false);
            String clearedMsg = "근무시간 기록이 초기화되었습니다. 필요시 다시 계산해주세요.";
            originalContractInfoArea.setText(clearedMsg);
            adjustmentsInfoArea.setText("");
            finalPayoutInfoArea.setText("");
        }
    }

    private void updateWorkdayInfoLabels(Integer selectedDaysCountInCalendar) {
        if (totalWorkDaysLabel != null) totalWorkDaysLabel.setText(String.format("달력 선택일: %d일", selectedDaysCountInCalendar));
        updateDetailedWorkdayLabels();
    }

    private void updateDetailedWorkdayLabels() {
        if (calendarPanel == null || tableModel == null) return;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> calendarSelectedDatesInfo = calendarPanel.getSelectedDatesWithStatus();

        Set<LocalDate> calendarSelectedWeekdays = calendarSelectedDatesInfo.stream()
                .filter(info -> (Boolean)info.get("isSelected"))
                .map(info -> (LocalDate)info.get("date"))
                .filter(date -> date.getDayOfWeek()!=DayOfWeek.SATURDAY && date.getDayOfWeek()!=DayOfWeek.SUNDAY && !calendarPanel.isPublicHoliday(date))
                .collect(Collectors.toSet());
        long calendarSelectedWeekdaysCount = calendarSelectedWeekdays.size();

        Set<LocalDate> tableActuallyWorkedWeekdays = new HashSet<>();
        int currentMonthAbsenceOrUnpaidCountForLabel = 0;

        for (int i=0; i<tableModel.getRowCount(); i++) {
            try {
                Object dateObj=tableModel.getValueAt(i,0);
                Object statusObj=tableModel.getValueAt(i,3);
                if(dateObj==null || statusObj==null) continue;

                LocalDate dateInRow=LocalDate.parse(dateObj.toString(),dateFormatter);
                String startTimeStr=(String)tableModel.getValueAt(i,1);
                String endTimeStr=(String)tableModel.getValueAt(i,2);
                WorkStatus status=(WorkStatus)statusObj;

                boolean isWeekday = dateInRow.getDayOfWeek()!=DayOfWeek.SATURDAY && dateInRow.getDayOfWeek()!=DayOfWeek.SUNDAY && !calendarPanel.isPublicHoliday(dateInRow);

                if(isWeekday) {
                    if (startTimeStr!=null && !startTimeStr.trim().isEmpty() && endTimeStr!=null && !endTimeStr.trim().isEmpty() &&
                            (status==WorkStatus.NORMAL || status==WorkStatus.PAID_HOLIDAY)) {
                        tableActuallyWorkedWeekdays.add(dateInRow);
                    } else if (status == WorkStatus.ABSENCE || status == WorkStatus.UNPAID_HOLIDAY ||
                            ( (status==WorkStatus.NORMAL) &&
                                    (startTimeStr == null || startTimeStr.trim().isEmpty() || endTimeStr == null || endTimeStr.trim().isEmpty()) )) {
                        currentMonthAbsenceOrUnpaidCountForLabel++;
                    }
                }
            } catch (Exception e) { System.err.println("Error parsing table row for detailed labels: "+e.getMessage());}
        }

        this.missedOrUnpaidOrAbsentWeekdayCountForDB = currentMonthAbsenceOrUnpaidCountForLabel;


        if (calendarWeekdaysLabel != null) calendarWeekdaysLabel.setText(String.format("달력상 평일: %d일", calendarSelectedWeekdaysCount));
        if (actualWorkWeekdaysLabel != null) actualWorkWeekdaysLabel.setText(String.format("실근무 평일: %d일", tableActuallyWorkedWeekdays.size()));
        if (absentWeekdaysLabel != null) absentWeekdaysLabel.setText(String.format("결근/무급 평일: %d일", this.missedOrUnpaidOrAbsentWeekdayCountForDB));
    }

    private void populateTableFromCalendar(boolean showMessage) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> calendarInfos = calendarPanel.getSelectedDatesWithStatus();
        Map<LocalDate, Object[]> existingTableData = new HashMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                LocalDate date = LocalDate.parse((String) tableModel.getValueAt(i, 0), dateFormatter);
                existingTableData.put(date, new Object[]{tableModel.getValueAt(i,1), tableModel.getValueAt(i,2), tableModel.getValueAt(i,3)});
            } catch (Exception e) {}
        }
        tableModel.setRowCount(0);
        List<Map<String, Object>> sortedCalendarInfos = new ArrayList<>(calendarInfos);
        sortedCalendarInfos.sort(Comparator.comparing(info -> (LocalDate)info.get("date")));
        for (Map<String, Object> info : sortedCalendarInfos) {
            LocalDate date = (LocalDate) info.get("date");
            if ((Boolean) info.get("isSelected")) {
                Object[] preservedData = existingTableData.get(date);
                if (preservedData != null) {
                    tableModel.addRow(new Object[]{date.format(dateFormatter), preservedData[0], preservedData[1], preservedData[2]});
                } else {
                    WorkStatus initialStatus;
                    DayOfWeek dayOfWeek = date.getDayOfWeek();
                    if (calendarPanel.isPublicHoliday(date)) initialStatus = WorkStatus.PAID_HOLIDAY;
                    else if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) initialStatus = WorkStatus.NORMAL;
                    else initialStatus = WorkStatus.NORMAL;
                    tableModel.addRow(new Object[]{date.format(dateFormatter), "09:00", "18:00", initialStatus});
                }
            }
        }

        sortTableByDate();
        if (currentEmployee != null && currentEmployee.getHireDate() != null && !currentEmployee.getHireDate().isEmpty()) {
            try {
                LocalDate hireDate = LocalDate.parse(currentEmployee.getHireDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                YearMonth currentTableMonth = calendarPanel.getCurrentYearMonth();
                if (hireDate.getYear() == currentTableMonth.getYear() && hireDate.getMonth() == currentTableMonth.getMonth()) {
                    filterWorkLogTableForMidMonthHire(hireDate);
                }
            } catch (DateTimeParseException e) {
                System.err.println("입사일 파싱 오류 (중도입사자 필터링 중 - populateTableFromCalendar): " + currentEmployee.getHireDate());
            }
        }

        if (showMessage) JOptionPane.showMessageDialog(this, "달력과 근무 기록표를 동기화했습니다.", "동기화 완료", JOptionPane.INFORMATION_MESSAGE);
        updateDetailedWorkdayLabels();
        applyToDbButton.setEnabled(false);
    }

    private void refreshWorkLogForCurrentMonth(boolean showMessage) {
        if (currentEmployee == null) return;
        tableModel.setRowCount(0);
        YearMonth currentMonth = calendarPanel.getCurrentYearMonth();
        List<WorkLogEntry> savedRecords = payrollManager.getWorkRecords(currentEmployee.getId(), currentMonth);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        if (!savedRecords.isEmpty()) {
            for (WorkLogEntry entry : savedRecords) {
                tableModel.addRow(new Object[]{entry.getWorkDate().format(dateFormatter), entry.getStartTime()!=null ? entry.getStartTime().format(timeFormatter):"", entry.getEndTime()!=null ? entry.getEndTime().format(timeFormatter):"", entry.getStatus()});
            }
            if (showMessage) JOptionPane.showMessageDialog(this, currentMonth.toString()+"월의 저장된 근무 기록을 불러왔습니다.", "기록 로드 완료", JOptionPane.INFORMATION_MESSAGE);
        } else {
            populateTableFromCalendar(false);
            if (showMessage) JOptionPane.showMessageDialog(this, currentMonth.toString()+"월의 저장된 근무 기록이 없어 달력 기준으로 새로 표시합니다.", "기록 없음", JOptionPane.INFORMATION_MESSAGE);
        }

        sortTableByDate();
        if (currentEmployee != null && currentEmployee.getHireDate() != null && !currentEmployee.getHireDate().isEmpty()) {
            try {
                LocalDate hireDate = LocalDate.parse(currentEmployee.getHireDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                if (hireDate.getYear() == currentMonth.getYear() && hireDate.getMonth() == currentMonth.getMonth()) {
                    filterWorkLogTableForMidMonthHire(hireDate);
                }
            } catch (DateTimeParseException e) {
                System.err.println("입사일 파싱 오류 (중도입사자 필터링 중 - refreshWorkLogForCurrentMonth): " + currentEmployee.getHireDate());
            }
        }

        updateDetailedWorkdayLabels();
        applyToDbButton.setEnabled(false);
    }

    private void deleteWorkRecord() {
        int selectedRow = workLogTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = workLogTable.convertRowIndexToModel(selectedRow);
            tableModel.removeRow(modelRow);
            applyToDbButton.setEnabled(false);
        } else {
            JOptionPane.showMessageDialog(this, "삭제할 기록을 테이블에서 선택하세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
        }
        updateDetailedWorkdayLabels();
    }

    private void loadEmployeeData() {
        String selectedEmployeeName = (String) employeeSearchComboBox.getSelectedItem();
        if (selectedEmployeeName == null || selectedEmployeeName.isEmpty() || selectedEmployeeName.equals("등록된 직원이 없습니다.")) { JOptionPane.showMessageDialog(this, "조회할 직원을 선택해주세요.", "경고", JOptionPane.WARNING_MESSAGE); return; }
        Optional<Employee> empOpt = payrollManager.getEmployeeByName(selectedEmployeeName);
        if (empOpt.isPresent()) {
            currentEmployee = empOpt.get();
            Optional<Payroll> payrollOpt = payrollManager.getContractualPayroll(currentEmployee.getId());
            if (payrollOpt.isPresent()) {
                currentContractPayrollData = payrollOpt.get();
                if (currentContractPayrollData.getEmployee() == null && currentEmployee != null) currentContractPayrollData.setEmployee(currentEmployee);

                double contractualTotal = currentContractPayrollData.getMonthlyBasicSalary() +
                        currentContractPayrollData.getFixedOvertimeAllowance() +
                        currentContractPayrollData.getBonus() +
                        currentContractPayrollData.getOtherAllowance() +
                        currentContractPayrollData.getMealAllowance() +
                        currentContractPayrollData.getVehicleMaintenanceFee() +
                        currentContractPayrollData.getResearchDevelopmentExpense() +
                        currentContractPayrollData.getChildcareAllowance();

                JOptionPane.showMessageDialog(this, String.format("%s 님의 계약 정보를 불러왔습니다. (계약 급여 총액: %s원)\n달력에서 작업할 연/월을 확인하고 근무 기록을 입력하세요.", currentEmployee.getName(), new DecimalFormat("#,###").format(contractualTotal)), "정보 로드 완료", JOptionPane.INFORMATION_MESSAGE);

                refreshWorkLogForCurrentMonth(true);

                String initialLoadMsg = String.format("%s 님의 %s 근무 기록을 확인/수정하거나 달력에서 동기화한 후,\n'급여 계산 실행' 버튼을 누르세요.", currentEmployee.getName(), calendarPanel.getCurrentYearMonth().toString());
                originalContractInfoArea.setText(initialLoadMsg);
                adjustmentsInfoArea.setText("");
                finalPayoutInfoArea.setText("");
                applyToDbButton.setEnabled(false);
            } else {
                clearEmployeeData();
                JOptionPane.showMessageDialog(this, "해당 직원의 계약 급여 정보를 찾을 수 없습니다. [직원/급여 관리] 페이지에서 먼저 등록해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            clearEmployeeData();
            JOptionPane.showMessageDialog(this, "해당 직원을 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
        this.currentSalaryPercentage = 100.0;
        if(salaryPercentageField != null) salaryPercentageField.setText(String.format("%.0f",this.currentSalaryPercentage));
        this.adHocBonusForCurrentCalculation = 0.0;
        this.adHocBonusApplied = false;
        if(adHocBonusField != null) adHocBonusField.setText("0");
    }

    private void clearEmployeeData() {
        currentEmployee = null;
        currentContractPayrollData = null;
        tableModel.setRowCount(0);
        String promptMsg = "직원을 선택하고 정보를 불러와 주세요.";
        originalContractInfoArea.setText(promptMsg);
        adjustmentsInfoArea.setText("");
        finalPayoutInfoArea.setText("");
        if (calendarWeekdaysLabel!=null) calendarWeekdaysLabel.setText("달력 평일: 0일");
        if (actualWorkWeekdaysLabel!=null) actualWorkWeekdaysLabel.setText("실근무 평일: 0일");
        if (absentWeekdaysLabel!=null) absentWeekdaysLabel.setText("결근 평일: 0일");
        applyToDbButton.setEnabled(false);
        this.currentSalaryPercentage = 100.0;
        if(salaryPercentageField != null) salaryPercentageField.setText(String.format("%.0f",this.currentSalaryPercentage));
        this.adHocBonusForCurrentCalculation = 0.0;
        this.adHocBonusApplied = false;
        if(adHocBonusField != null) adHocBonusField.setText("0");
        this.missedOrUnpaidOrAbsentWeekdayCountForDB = 0;
        this.lastCalculatedHireProrationRatio = 1.0;
        this.lastCalculatedAttendanceBasedDisplayPaymentRatio = 1.0;
        this.lastCalculatedPremiumAdditions = 0.0;
        this.lastCalculatedWeeklyAbsencePenalty = 0.0;
        this.lastCalculatedPaidPremiumMinutes = 0L;
        this.lastCalculatedTotalShortfallMinutes = 0L;
        updateDetailedWorkdayLabels();
    }

    public void refreshEmployeeComboBox() {
        employeeSearchComboBox.removeAllItems();
        try {
            List<Employee> employees = payrollManager.getAllEmployees();
            if (employees.isEmpty()){
                employeeSearchComboBox.addItem("등록된 직원이 없습니다.");
            } else {
                employees.sort(Comparator.comparing(Employee::getName));
                employees.forEach(employee -> employeeSearchComboBox.addItem(employee.getName()));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "직원 목록 로드 오류: " + e.getMessage(), "데이터 로드 오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    static class CustomDateRenderer extends DefaultTableCellRenderer {
        private final CalendarPanel calendarPanelRef;
        public CustomDateRenderer(CalendarPanel calendarPanel) { this.calendarPanelRef = calendarPanel; }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column == 0 && value != null) {
                try {
                    LocalDate date = LocalDate.parse(value.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    DayOfWeek dayOfWeek = date.getDayOfWeek();
                    boolean isPubHoliday = calendarPanelRef != null && calendarPanelRef.isPublicHoliday(date);
                    if (dayOfWeek == DayOfWeek.SUNDAY || isPubHoliday) c.setBackground(new Color(255, 220, 220));
                    else if (dayOfWeek == DayOfWeek.SATURDAY) c.setBackground(new Color(220, 220, 255));
                    else c.setBackground(table.getBackground());
                } catch (Exception e) { c.setBackground(table.getBackground()); }
            } else c.setBackground(table.getBackground());
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            } else c.setForeground(table.getForeground());
            return c;
        }
    }
}