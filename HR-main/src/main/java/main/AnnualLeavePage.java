package main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

public class AnnualLeavePage extends JPanel {

    private final PayrollManager payrollManager;
    private Employee currentEmployee;
    private int selectedYear;

    // UI Components
    private JComboBox<String> employeeComboBox;
    private JComboBox<Integer> yearComboBox;
    private JButton loadButton;

    private JLabel lblEmployeeName, lblHireDate, lblServiceYears, lblLeaveBasis;
    private JLabel lblGeneratedDays, lblAdjustmentDays, lblTotalGrantedDays, lblUsedDays, lblRemainingDays;

    private CalendarPanel calendarPanel;
    private JTable usedLeaveTable;
    private DefaultTableModel usedLeaveTableModel;

    private JTextField txtCompensatoryLeave, txtSpecialLeave, txtManualTotalLeave;
    private JButton btnApplyCompensatory, btnApplySpecial, btnApplyManual;
    private JButton btnUseSelectedDays;

    private final DecimalFormat leaveDayFormat = new DecimalFormat("#.##");

    public AnnualLeavePage(PayrollManager payrollManager) {
        this.payrollManager = payrollManager;
        this.selectedYear = LocalDate.now().getYear();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // 상단 패널: 직원 및 연도 선택
        add(createTopPanel(), BorderLayout.NORTH);

        // 중앙 패널: 정보, 달력, 사용기록
        add(createCenterPanel(), BorderLayout.CENTER);

        // 하단 패널: 연차 조정
        add(createBottomPanel(), BorderLayout.SOUTH);

        addListeners();
        refreshEmployeeComboBox();
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("직원 선택:"));
        employeeComboBox = new JComboBox<>();
        topPanel.add(employeeComboBox);

        topPanel.add(Box.createHorizontalStrut(20));

        topPanel.add(new JLabel("조회 연도:"));
        yearComboBox = new JComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear - 5; i <= currentYear + 1; i++) {
            yearComboBox.addItem(i);
        }
        yearComboBox.setSelectedItem(currentYear);
        topPanel.add(yearComboBox);

        loadButton = new JButton("연차 정보 조회");
        topPanel.add(loadButton);

        return topPanel;
    }

    private JSplitPane createCenterPanel() {
        // 좌측: 연차 정보 패널
        JPanel infoPanel = createInfoPanel();

        // 우측: 달력 및 사용기록 패널
        JPanel calendarAndRecordPanel = createCalendarAndRecordPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(infoPanel), calendarAndRecordPanel);
        splitPane.setDividerLocation(350);
        return splitPane;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("연차 현황"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        int y = 0;

        // 직원 기본 정보
        panel.add(createLabel("직원명:"), gbc(gbc, 0, y));
        panel.add(lblEmployeeName = createValueLabel(), gbc(gbc, 1, y++));
        panel.add(createLabel("입사일:"), gbc(gbc, 0, y));
        panel.add(lblHireDate = createValueLabel(), gbc(gbc, 1, y++));
        panel.add(createLabel("근속연수:"), gbc(gbc, 0, y));
        panel.add(lblServiceYears = createValueLabel(), gbc(gbc, 1, y++));
        panel.add(createLabel("산정기준:"), gbc(gbc, 0, y));
        panel.add(lblLeaveBasis = createValueLabel(), gbc(gbc, 1, y++));

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // 연차 상세 정보
        panel.add(createLabel("자동 발생 연차:"), gbc(gbc, 0, y));
        panel.add(lblGeneratedDays = createValueLabel(), gbc(gbc, 1, y++));
        panel.add(createLabel("조정/특별 휴가:"), gbc(gbc, 0, y));
        panel.add(lblAdjustmentDays = createValueLabel(), gbc(gbc, 1, y++));

        Font boldFont = panel.getFont().deriveFont(Font.BOLD);
        JLabel totalLabel = createLabel("총 부여 연차:");
        totalLabel.setFont(boldFont);
        panel.add(totalLabel, gbc(gbc, 0, y));
        lblTotalGrantedDays = createValueLabel();
        lblTotalGrantedDays.setFont(boldFont);
        panel.add(lblTotalGrantedDays, gbc(gbc, 1, y++));

        panel.add(createLabel("사용 연차:"), gbc(gbc, 0, y));
        lblUsedDays = createValueLabel();
        lblUsedDays.setForeground(Color.BLUE);
        panel.add(lblUsedDays, gbc(gbc, 1, y++));

        JLabel remainingLabel = createLabel("잔여 연차:");
        remainingLabel.setFont(boldFont);
        remainingLabel.setForeground(Color.RED);
        panel.add(remainingLabel, gbc(gbc, 0, y));
        lblRemainingDays = createValueLabel();
        lblRemainingDays.setFont(boldFont);
        lblRemainingDays.setForeground(Color.RED);
        panel.add(lblRemainingDays, gbc(gbc, 1, y++));

        // 여백 채우기
        gbc.weighty = 1.0;
        panel.add(new JLabel(), gbc(gbc, 0, y));

        return panel;
    }

    private JPanel createCalendarAndRecordPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        calendarPanel = new CalendarPanel(days -> {}); // 콜백은 사용하지 않음
        panel.add(calendarPanel, BorderLayout.CENTER);

        JPanel usedPanel = new JPanel(new BorderLayout());
        usedPanel.setBorder(BorderFactory.createTitledBorder("연차 사용 기록"));
        usedLeaveTableModel = new DefaultTableModel(new String[]{"사용일자"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        usedLeaveTable = new JTable(usedLeaveTableModel);
        usedPanel.add(new JScrollPane(usedLeaveTable), BorderLayout.CENTER);

        btnUseSelectedDays = new JButton("달력에서 선택한 날짜 연차로 사용");
        usedPanel.add(btnUseSelectedDays, BorderLayout.SOUTH);

        panel.add(usedPanel, BorderLayout.EAST);
        usedPanel.setPreferredSize(new Dimension(200, 0));

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panel.setBorder(BorderFactory.createTitledBorder("연차 조정"));

        panel.add(new JLabel("대체휴가 추가:"));
        txtCompensatoryLeave = new JTextField(4);
        panel.add(txtCompensatoryLeave);
        btnApplyCompensatory = new JButton("적용");
        panel.add(btnApplyCompensatory);

        panel.add(Box.createHorizontalStrut(20));

        panel.add(new JLabel("특별휴가 추가:"));
        txtSpecialLeave = new JTextField(4);
        panel.add(txtSpecialLeave);
        btnApplySpecial = new JButton("적용");
        panel.add(btnApplySpecial);

        panel.add(Box.createHorizontalStrut(20));

        panel.add(new JLabel("총 부여 연차 임의 수정:"));
        txtManualTotalLeave = new JTextField(4);
        panel.add(txtManualTotalLeave);
        btnApplyManual = new JButton("적용");
        panel.add(btnApplyManual);

        return panel;
    }

    private void addListeners() {
        loadButton.addActionListener(e -> loadAnnualLeaveData());

        btnUseSelectedDays.addActionListener(e -> useSelectedLeaveDays());

        btnApplyCompensatory.addActionListener(createAdjustmentListener("대체휴가", txtCompensatoryLeave));
        btnApplySpecial.addActionListener(createAdjustmentListener("특별휴가", txtSpecialLeave));

        btnApplyManual.addActionListener(e -> {
            if(currentEmployee == null) {
                JOptionPane.showMessageDialog(this, "먼저 직원을 조회해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                double totalDays = Double.parseDouble(txtManualTotalLeave.getText());
                int confirm = JOptionPane.showConfirmDialog(this,
                        "총 부여 연차를 " + totalDays + "일로 임의 설정하시겠습니까?\n(기존 자동계산 연차는 무시됩니다)", "임의 수정 확인", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    payrollManager.setTotalLeaveManually(currentEmployee.getId(), selectedYear, totalDays, "수동 설정");
                    loadAnnualLeaveData();
                    txtManualTotalLeave.setText("");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "숫자(일수)를 정확히 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private ActionListener createAdjustmentListener(String type, JTextField field) {
        return e -> {
            if (currentEmployee == null) {
                JOptionPane.showMessageDialog(this, "먼저 직원을 조회해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                double days = Double.parseDouble(field.getText());
                payrollManager.applyLeaveAdjustment(currentEmployee.getId(), selectedYear, days, type);
                loadAnnualLeaveData();
                field.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "숫자(일수)를 정확히 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            }
        };
    }

    private void useSelectedLeaveDays() {
        if (currentEmployee == null) {
            JOptionPane.showMessageDialog(this, "먼저 직원을 조회해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<LocalDate> selectedDates = calendarPanel.getSelectedDatesWithStatus().stream()
                .filter(info -> (Boolean) info.get("isSelected"))
                .map(info -> (LocalDate) info.get("date"))
                .collect(Collectors.toList());

        if (selectedDates.isEmpty()) {
            JOptionPane.showMessageDialog(this, "달력에서 연차로 사용할 날짜를 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                selectedDates.size() + "일의 연차를 사용처리 하시겠습니까?", "연차 사용 확인", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = payrollManager.saveLeaveUsage(currentEmployee.getId(), selectedDates);
            if (success) {
                JOptionPane.showMessageDialog(this, "연차 사용이 기록되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                loadAnnualLeaveData();
            } else {
                JOptionPane.showMessageDialog(this, "연차 사용 기록 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadAnnualLeaveData() {
        String selectedEmployeeName = (String) employeeComboBox.getSelectedItem();
        if (selectedEmployeeName == null || selectedEmployeeName.equals("등록된 직원이 없습니다.")) {
            clearDisplay();
            return;
        }

        Optional<Employee> empOpt = payrollManager.getEmployeeByName(selectedEmployeeName);
        if (!empOpt.isPresent()) {
            clearDisplay();
            return;
        }

        this.currentEmployee = empOpt.get();
        this.selectedYear = (Integer) yearComboBox.getSelectedItem();

        // 연차 계산 로직 호출 (DB에 없으면 생성/업데이트)
        payrollManager.calculateAndGrantAnnualLeave(currentEmployee, selectedYear);

        // 정보 표시
        Map<String, Double> summary = payrollManager.getAnnualLeaveSummary(currentEmployee.getId(), selectedYear);
        double generated = summary.getOrDefault("generated", 0.0);
        double adjustment = summary.getOrDefault("adjustment", 0.0);
        double used = summary.getOrDefault("used", 0.0);
        double total = generated + adjustment;
        double remaining = total - used;

        lblEmployeeName.setText(currentEmployee.getName());
        lblHireDate.setText(currentEmployee.getHireDate());

        LocalDate hireDate = LocalDate.parse(currentEmployee.getHireDate());
        long serviceYears = ChronoUnit.YEARS.between(hireDate, LocalDate.of(selectedYear, 12, 31));
        lblServiceYears.setText(serviceYears + "년");

        String basis = payrollManager.loadSettings().getOrDefault("annualLeaveBasis", "FISCAL");
        lblLeaveBasis.setText("HIRE_DATE".equals(basis) ? "입사일 기준" : "회계연도 기준");

        lblGeneratedDays.setText(leaveDayFormat.format(generated) + " 일");
        lblAdjustmentDays.setText(leaveDayFormat.format(adjustment) + " 일");
        lblTotalGrantedDays.setText(leaveDayFormat.format(total) + " 일");
        lblUsedDays.setText(leaveDayFormat.format(used) + " 일");
        lblRemainingDays.setText(leaveDayFormat.format(remaining) + " 일");

        // 사용기록 테이블 업데이트
        usedLeaveTableModel.setRowCount(0);
        List<LocalDate> usedDates = payrollManager.getLeaveUsageRecords(currentEmployee.getId(), selectedYear);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd (E)");
        for (LocalDate date : usedDates) {
            usedLeaveTableModel.addRow(new Object[]{date.format(dtf)});
        }
    }

    private void clearDisplay() {
        currentEmployee = null;
        lblEmployeeName.setText("-");
        lblHireDate.setText("-");
        lblServiceYears.setText("-");
        lblLeaveBasis.setText("-");
        lblGeneratedDays.setText("-");
        lblAdjustmentDays.setText("-");
        lblTotalGrantedDays.setText("-");
        lblUsedDays.setText("-");
        lblRemainingDays.setText("-");
        usedLeaveTableModel.setRowCount(0);
    }

    public void refreshEmployeeComboBox() {
        employeeComboBox.removeAllItems();
        try {
            List<Employee> employees = payrollManager.getAllEmployees();
            if (employees.isEmpty()){
                employeeComboBox.addItem("등록된 직원이 없습니다.");
            } else {
                employees.sort(Comparator.comparing(Employee::getName));
                employees.forEach(employee -> employeeComboBox.addItem(employee.getName()));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "직원 목록 로드 오류: " + e.getMessage(), "데이터 로드 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private GridBagConstraints gbc(GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x;
        gbc.gridy = y;
        return gbc;
    }

    private JLabel createLabel(String text) {
        return new JLabel(text);
    }

    private JLabel createValueLabel() {
        JLabel label = new JLabel("-");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }
}