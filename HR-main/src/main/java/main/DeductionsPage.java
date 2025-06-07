package main;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;

public class DeductionsPage extends JPanel {

    private final PayrollManager payrollManager;
    private Employee currentEmployee;
    private Payroll currentPayroll;

    private JComboBox<String> employeeComboBox;
    private JComboBox<Integer> yearComboBox;
    private JComboBox<String> monthComboBox;
    private JButton loadButton;

    private JLabel lblGrossPayValue;
    private JComboBox<IndustrialAccidentRate> rateComboBox;
    private JTextField txtDependents;
    private JButton calculateButton;
    private JButton saveButton;

    private JTable deductionTable;
    private DefaultTableModel tableModel;
    private JLabel lblNetPayValue;

    private final DecimalFormat formatter = new DecimalFormat("#,###");
    private DeductionResult lastDeductionResult;

    private static class IndustrialAccidentRate {
        String name;
        BigDecimal rate;

        IndustrialAccidentRate(String name, String rate) {
            this.name = name;
            this.rate = new BigDecimal(rate);
        }

        @Override
        public String toString() {
            return name + " (" + rate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%)";
        }
    }

    public DeductionsPage(PayrollManager payrollManager) {
        this.payrollManager = payrollManager;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);

        addListeners();
        refreshEmployeeComboBox();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("직원 선택:"));
        employeeComboBox = new JComboBox<>();
        panel.add(employeeComboBox);

        panel.add(Box.createHorizontalStrut(15));

        panel.add(new JLabel("귀속 연/월:"));
        yearComboBox = new JComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear - 5; i <= currentYear + 1; i++) {
            yearComboBox.addItem(i);
        }
        yearComboBox.setSelectedItem(currentYear);
        panel.add(yearComboBox);

        monthComboBox = new JComboBox<>();
        for (int i = 1; i <= 12; i++) {
            monthComboBox.addItem(String.format("%02d", i));
        }
        monthComboBox.setSelectedItem(String.format("%02d", LocalDate.now().getMonthValue()));
        panel.add(monthComboBox);

        loadButton = new JButton("급여 정보 불러오기");
        panel.add(loadButton);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 10));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("계산 조건 입력"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("과세 대상 급여:"), gbc);
        gbc.gridx = 1;
        lblGrossPayValue = new JLabel("0 원");
        lblGrossPayValue.setFont(lblGrossPayValue.getFont().deriveFont(Font.BOLD));
        inputPanel.add(lblGrossPayValue, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("산재보험 업종:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        rateComboBox = new JComboBox<>();
        rateComboBox.addItem(new IndustrialAccidentRate("소프트웨어 개발", "0.007"));
        rateComboBox.addItem(new IndustrialAccidentRate("음식점업", "0.017"));
        rateComboBox.addItem(new IndustrialAccidentRate("건설업", "0.036"));
        inputPanel.add(rateComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        inputPanel.add(new JLabel("부양가족 수 (본인포함):"), gbc);
        gbc.gridx = 1;
        txtDependents = new JTextField("1", 5);
        inputPanel.add(txtDependents, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        calculateButton = new JButton("공제액 재계산");
        saveButton = new JButton("계산 결과 저장");
        saveButton.setEnabled(false);
        buttonPanel.add(calculateButton);
        buttonPanel.add(saveButton);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        inputPanel.add(buttonPanel, gbc);

        gbc.gridy = 4; gbc.weighty = 1.0;
        inputPanel.add(new JLabel(), gbc); // 여백

        JPanel resultPanel = new JPanel(new BorderLayout(10, 10));
        resultPanel.setBorder(BorderFactory.createTitledBorder("공제 내역 결과"));

        String[] columnNames = {"항목", "근로자 부담액", "사업자 부담액"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        deductionTable = new JTable(tableModel);
        deductionTable.setRowHeight(25);

        resultPanel.add(new JScrollPane(deductionTable), BorderLayout.CENTER);

        JPanel netPayPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        netPayPanel.add(new JLabel("차감공제 후 실지급액: "));
        lblNetPayValue = new JLabel("0 원");
        lblNetPayValue.setFont(lblNetPayValue.getFont().deriveFont(Font.BOLD, 16f));
        lblNetPayValue.setForeground(Color.BLUE);
        netPayPanel.add(lblNetPayValue);
        resultPanel.add(netPayPanel, BorderLayout.SOUTH);

        panel.add(inputPanel, BorderLayout.WEST);
        panel.add(resultPanel, BorderLayout.CENTER);
        return panel;
    }

    private void addListeners() {
        loadButton.addActionListener(e -> loadPayrollData());
        calculateButton.addActionListener(e -> calculateDeductions());
        saveButton.addActionListener(e -> saveDeductions());
    }

    private void loadPayrollData() {
        String selectedName = (String) employeeComboBox.getSelectedItem();
        int year = (int) yearComboBox.getSelectedItem();
        int month = Integer.parseInt((String) monthComboBox.getSelectedItem());

        if (selectedName == null || selectedName.equals("등록된 직원이 없습니다.")) {
            return;
        }

        Optional<Employee> empOpt = payrollManager.getEmployeeByName(selectedName);
        if (empOpt.isPresent()) {
            currentEmployee = empOpt.get();
            List<Payroll> payrolls = payrollManager.getPayrollsForPeriod(year, month);
            Optional<Payroll> payrollOpt = payrolls.stream()
                    .filter(p -> p.getEmployeeId() == currentEmployee.getId())
                    .findFirst();

            if (payrollOpt.isPresent()) {
                currentPayroll = payrollOpt.get();
                populateData(currentPayroll);
                saveButton.setEnabled(true);
            } else {
                clearData();
                JOptionPane.showMessageDialog(this, "해당 월에 확정된 급여 정보가 없습니다.\n먼저 '2. 근태/급여 계산'에서 급여 계산 및 저장을 완료해주세요.", "정보 없음", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void calculateDeductions() {
        if (currentPayroll == null) {
            JOptionPane.showMessageDialog(this, "먼저 급여 정보를 불러와주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            BigDecimal grossPay = currentPayroll.getGrossPay();
            IndustrialAccidentRate selectedRate = (IndustrialAccidentRate) rateComboBox.getSelectedItem();
            BigDecimal accidentRate = (selectedRate != null) ? selectedRate.rate : BigDecimal.ZERO;
            int dependents = Integer.parseInt(txtDependents.getText());

            this.lastDeductionResult = DeductionCalculator.calculate(grossPay, accidentRate, dependents);
            displayDeductionResult(this.lastDeductionResult);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "부양가족 수는 숫자로 입력해야 합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveDeductions() {
        if (currentPayroll == null || lastDeductionResult == null) {
            JOptionPane.showMessageDialog(this, "저장할 계산 결과가 없습니다. 먼저 '공제액 재계산'을 실행해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "현재 화면의 공제액 계산 결과를 DB에 저장하시겠습니까?\n1페이지 급여 내역에도 이 결과가 반영됩니다.", "저장 확인", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        IndustrialAccidentRate selectedRate = (IndustrialAccidentRate) rateComboBox.getSelectedItem();
        BigDecimal accidentRate = (selectedRate != null) ? selectedRate.rate : BigDecimal.ZERO;
        int dependents = Integer.parseInt(txtDependents.getText());
        int year = (int) yearComboBox.getSelectedItem();
        int month = Integer.parseInt((String) monthComboBox.getSelectedItem());

        payrollManager.finalizeMonthlyPayAndDeductions(
                currentEmployee.getId(), YearMonth.of(year, month),
                currentPayroll, accidentRate, dependents
        );

        JOptionPane.showMessageDialog(this, "공제액 정보가 성공적으로 저장되었습니다.", "저장 완료", JOptionPane.INFORMATION_MESSAGE);
    }

    private void populateData(Payroll payroll) {
        lblGrossPayValue.setText(formatter.format(payroll.getGrossPay()) + " 원");

        DeductionResult result = new DeductionResult();
        result.nationalPensionEmployee = payroll.getNationalPensionEmployee();
        result.healthInsuranceEmployee = payroll.getHealthInsuranceEmployee();
        result.longTermCareInsuranceEmployee = payroll.getLongTermCareInsuranceEmployee();
        result.employmentInsuranceEmployee = payroll.getEmploymentInsuranceEmployee();
        result.incomeTax = payroll.getIncomeTax();
        result.localIncomeTax = payroll.getLocalIncomeTax();
        result.totalEmployeeDeduction = payroll.getTotalEmployeeDeduction();
        result.nationalPensionEmployer = payroll.getNationalPensionEmployer();
        result.healthInsuranceEmployer = payroll.getHealthInsuranceEmployer();
        result.longTermCareInsuranceEmployer = payroll.getLongTermCareInsuranceEmployer();
        result.employmentInsuranceEmployer = payroll.getEmploymentInsuranceEmployer();
        result.industrialAccidentInsuranceEmployer = payroll.getIndustrialAccidentInsuranceEmployer();
        result.netPay = payroll.getNetPay();

        this.lastDeductionResult = result;
        displayDeductionResult(result);
    }

    private void displayDeductionResult(DeductionResult result) {
        tableModel.setRowCount(0);

        addRow("국민연금", result.nationalPensionEmployee, result.nationalPensionEmployer);
        addRow("건강보험", result.healthInsuranceEmployee, result.healthInsuranceEmployer);
        addRow("장기요양보험", result.longTermCareInsuranceEmployee, result.longTermCareInsuranceEmployer);
        addRow("고용보험", result.employmentInsuranceEmployee, result.employmentInsuranceEmployer);
        addRow("산재보험", BigDecimal.ZERO, result.industrialAccidentInsuranceEmployer);
        tableModel.addRow(new String[]{"-", "-", "-"});
        addRow("소득세", result.incomeTax, BigDecimal.ZERO);
        addRow("지방소득세", result.localIncomeTax, BigDecimal.ZERO);
        tableModel.addRow(new String[]{"-", "-", "-"});

        BigDecimal totalEmployer = result.nationalPensionEmployer.add(result.healthInsuranceEmployer).add(result.longTermCareInsuranceEmployer).add(result.employmentInsuranceEmployer).add(result.industrialAccidentInsuranceEmployer);
        addRow("합계", result.totalEmployeeDeduction, totalEmployer);

        lblNetPayValue.setText(formatter.format(result.netPay) + " 원");
    }

    private void addRow(String item, BigDecimal employeeVal, BigDecimal employerVal) {
        tableModel.addRow(new String[]{
                item,
                formatter.format(employeeVal),
                formatter.format(employerVal)
        });
    }

    private void clearData() {
        currentEmployee = null;
        currentPayroll = null;
        lastDeductionResult = null;
        lblGrossPayValue.setText("0 원");
        txtDependents.setText("1");
        tableModel.setRowCount(0);
        lblNetPayValue.setText("0 원");
        saveButton.setEnabled(false);
    }

    public void refreshEmployeeComboBox() {
        employeeComboBox.removeAllItems();
        try {
            List<Employee> employees = payrollManager.getAllEmployees();
            if (employees.isEmpty()) {
                employeeComboBox.addItem("등록된 직원이 없습니다.");
            } else {
                employees.sort(Comparator.comparing(Employee::getName));
                employees.forEach(employee -> employeeComboBox.addItem(employee.getName()));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "직원 목록 로드 오류: " + e.getMessage(), "데이터베이스 오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}