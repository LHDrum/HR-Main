package main;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class SettingsPage extends JPanel {
    private final PayrollManager payrollManager;

    // UI Components
    private JTextField txtStandardWorkHours, txtFixedOvertimeHours;
    private JLabel lblTotalCalcHours;

    private JRadioButton radioFiscalYear, radioHireDate;

    private JCheckBox chkApplyOvertime, chkApplyNightWork, chkApplyHolidayWork;

    private JTextField txtDefaultMealAllowance, txtDefaultChildcareAllowance;
    private JTextField txtDefaultVehicleFee, txtDefaultRdExpense;

    private JTextField txtDefaultStartTime, txtDefaultEndTime;

    private JTextField txtCompanyName, txtDefaultPayday;

    private JButton btnSaveSettings;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public SettingsPage(PayrollManager payrollManager) {
        this.payrollManager = payrollManager;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JScrollPane scrollPane = new JScrollPane(createSettingsPanel());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnSaveSettings = new JButton("설정 저장");
        btnSaveSettings.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        btnSaveSettings.addActionListener(e -> saveSettings());
        bottomPanel.add(btnSaveSettings);

        add(bottomPanel, BorderLayout.SOUTH);

        loadSettings();
    }

    private JPanel createSettingsPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        mainPanel.add(createWorkHoursPanel());
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(createAllowanceTogglePanel());
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(createDefaultValuesPanel());
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(createAnnualLeavePanel());
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(createCompanyInfoPanel());
        mainPanel.add(Box.createVerticalGlue());

        return mainPanel;
    }

    private JPanel createWorkHoursPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("급여 계산 기준 시간 설정"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("기본 근무시간 (A):"), gbc);
        gbc.gridx = 1;
        txtStandardWorkHours = new JTextField("209", 5);
        panel.add(txtStandardWorkHours, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("시간 (예: 209시간)"), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("고정 연장시간 (B):"), gbc);
        gbc.gridx = 1;
        txtFixedOvertimeHours = new JTextField("15", 5);
        panel.add(txtFixedOvertimeHours, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("시간 (급여계산 시 통상시급 산정용)"), gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("통상시급 산정시간 (A+B):"), gbc);
        gbc.gridx = 1;
        lblTotalCalcHours = new JLabel("224 시간");
        lblTotalCalcHours.setFont(lblTotalCalcHours.getFont().deriveFont(Font.BOLD));
        panel.add(lblTotalCalcHours, gbc);

        DocumentListener listener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateTotalHours(); }
            public void removeUpdate(DocumentEvent e) { updateTotalHours(); }
            public void insertUpdate(DocumentEvent e) { updateTotalHours(); }
        };
        txtStandardWorkHours.getDocument().addDocumentListener(listener);
        txtFixedOvertimeHours.getDocument().addDocumentListener(listener);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private void updateTotalHours() {
        try {
            double standard = Double.parseDouble(txtStandardWorkHours.getText());
            double overtime = Double.parseDouble(txtFixedOvertimeHours.getText());
            lblTotalCalcHours.setText(String.format("%.1f 시간", standard + overtime));
        } catch (NumberFormatException e) {
            lblTotalCalcHours.setText("계산 오류");
        }
    }

    private JPanel createAllowanceTogglePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panel.setBorder(createTitledBorder("프리미엄 수당 적용 설정"));
        chkApplyOvertime = new JCheckBox("연장근로수당", true);
        chkApplyNightWork = new JCheckBox("야간근로수당", true);
        chkApplyHolidayWork = new JCheckBox("휴일근로수당", true);
        panel.add(chkApplyOvertime);
        panel.add(chkApplyNightWork);
        panel.add(chkApplyHolidayWork);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private JPanel createDefaultValuesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("신규 직원 등록 및 근태 기록 시 기본값 설정"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("기본 근무 시작시간:"), gbc);
        gbc.gridx = 1; txtDefaultStartTime = new JTextField("09:00", 8); panel.add(txtDefaultStartTime, gbc);
        gbc.gridx = 2; panel.add(new JLabel("기본 근무 종료시간:"), gbc);
        gbc.gridx = 3; txtDefaultEndTime = new JTextField("18:00", 8); panel.add(txtDefaultEndTime, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("식대 기본값:"), gbc);
        gbc.gridx = 1; txtDefaultMealAllowance = new JTextField("200,000", 8); panel.add(txtDefaultMealAllowance, gbc);
        gbc.gridx = 2; panel.add(new JLabel("육아수당 기본값:"), gbc);
        gbc.gridx = 3; txtDefaultChildcareAllowance = new JTextField("0", 8); panel.add(txtDefaultChildcareAllowance, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("차량유지비 기본값:"), gbc);
        gbc.gridx = 1; txtDefaultVehicleFee = new JTextField("0", 8); panel.add(txtDefaultVehicleFee, gbc);
        gbc.gridx = 2; panel.add(new JLabel("연구개발비 기본값:"), gbc);
        gbc.gridx = 3; txtDefaultRdExpense = new JTextField("0", 8); panel.add(txtDefaultRdExpense, gbc);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private JPanel createAnnualLeavePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panel.setBorder(createTitledBorder("연차 발생 기준 설정 (정책 선택)"));
        radioFiscalYear = new JRadioButton("회계연도 기준 (매년 1월 1일 부여)");
        radioHireDate = new JRadioButton("입사일 기준 (1년마다 부여)");
        ButtonGroup group = new ButtonGroup();
        group.add(radioFiscalYear);
        group.add(radioHireDate);
        panel.add(radioFiscalYear);
        panel.add(radioHireDate);
        radioFiscalYear.setSelected(true);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private JPanel createCompanyInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("회사 정보 및 기타 설정"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("회사명:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtCompanyName = new JTextField(30);
        panel.add(txtCompanyName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("급여 지급일:"), gbc);
        gbc.gridx = 1;
        txtDefaultPayday = new JTextField(15);
        panel.add(txtDefaultPayday, gbc);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("맑은 고딕", Font.BOLD, 14), new Color(70, 130, 180));
    }

    private void saveSettings() {
        try {
            Map<String, String> settings = new HashMap<>();
            settings.put("standardWorkHours", txtStandardWorkHours.getText());
            settings.put("fixedOvertimeHours", txtFixedOvertimeHours.getText());
            settings.put("applyOvertime", String.valueOf(chkApplyOvertime.isSelected()));
            settings.put("applyNightWork", String.valueOf(chkApplyNightWork.isSelected()));
            settings.put("applyHolidayWork", String.valueOf(chkApplyHolidayWork.isSelected()));
            settings.put("defaultMealAllowance", txtDefaultMealAllowance.getText().replace(",", ""));
            settings.put("defaultChildcareAllowance", txtDefaultChildcareAllowance.getText().replace(",", ""));
            settings.put("defaultVehicleFee", txtDefaultVehicleFee.getText().replace(",", ""));
            settings.put("defaultRdExpense", txtDefaultRdExpense.getText().replace(",", ""));
            settings.put("defaultStartTime", txtDefaultStartTime.getText());
            settings.put("defaultEndTime", txtDefaultEndTime.getText());
            settings.put("annualLeaveBasis", radioFiscalYear.isSelected() ? "FISCAL" : "HIRE_DATE");
            settings.put("companyName", txtCompanyName.getText());
            settings.put("defaultPayday", txtDefaultPayday.getText());

            payrollManager.saveSettings(settings);
            JOptionPane.showMessageDialog(this, "설정이 성공적으로 저장되었습니다.", "저장 완료", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "설정 저장 중 오류가 발생했습니다: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSettings() {
        Map<String, String> settings = payrollManager.loadSettings();
        txtStandardWorkHours.setText(settings.getOrDefault("standardWorkHours", "209.0"));
        txtFixedOvertimeHours.setText(settings.getOrDefault("fixedOvertimeHours", "15.0"));
        chkApplyOvertime.setSelected(Boolean.parseBoolean(settings.getOrDefault("applyOvertime", "true")));
        chkApplyNightWork.setSelected(Boolean.parseBoolean(settings.getOrDefault("applyNightWork", "true")));
        chkApplyHolidayWork.setSelected(Boolean.parseBoolean(settings.getOrDefault("applyHolidayWork", "true")));

        txtDefaultMealAllowance.setText(formatter.format(Long.parseLong(settings.getOrDefault("defaultMealAllowance", "200000"))));
        txtDefaultChildcareAllowance.setText(formatter.format(Long.parseLong(settings.getOrDefault("defaultChildcareAllowance", "0"))));
        txtDefaultVehicleFee.setText(formatter.format(Long.parseLong(settings.getOrDefault("defaultVehicleFee", "0"))));
        txtDefaultRdExpense.setText(formatter.format(Long.parseLong(settings.getOrDefault("defaultRdExpense", "0"))));

        txtDefaultStartTime.setText(settings.getOrDefault("defaultStartTime", "09:00"));
        txtDefaultEndTime.setText(settings.getOrDefault("defaultEndTime", "18:00"));

        if ("HIRE_DATE".equals(settings.getOrDefault("annualLeaveBasis", "FISCAL"))) {
            radioHireDate.setSelected(true);
        } else {
            radioFiscalYear.setSelected(true);
        }

        txtCompanyName.setText(settings.getOrDefault("companyName", "[주식회사 OO회사]"));
        txtDefaultPayday.setText(settings.getOrDefault("defaultPayday", "매월 25일"));

        updateTotalHours();
    }
}