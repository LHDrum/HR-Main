package main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class EmploymentContractPage extends JPanel {

    private PayrollApp payrollApp;
    private Employee currentEmployee;
    private Payroll contractualPayroll;

    // UI 컴포넌트 선언
    private JLabel lblContractTitle;
    private JLabel lblCompanyName;
    private JLabel lblIntro1;

    // 근로자 정보 표시용
    private JLabel lblEmployeeNameValue, lblResidentRegNoValue, lblAddressValue, lblPhoneNumberValue;
    private JLabel lblDepartmentValue, lblWorkLocationValue, lblSiteLocationValue;
    private JLabel lblContractPeriodValue;

    // 임금 정보 표시용
    private JLabel lblAnnualSalaryValue, lblMonthlyBasicSalaryValue, lblFixedOvertimeAllowanceValue;
    private JLabel lblBonusValue, lblOtherAllowanceValue, lblMealAllowanceValue, lblVehicleMaintenanceFeeValue;
    private JLabel lblResearchDevelopmentExpenseValue, lblChildcareAllowanceValue;

    // 계약 조건 입력용
    private JTextField txtContractStartDate, txtContractEndDate;
    private JTextField txtWorkHoursStart, txtWorkHoursEnd, txtRestHours;
    private JTextField txtWagePaymentDate;
    private JTextArea txtOtherTerms;
    private JTextField txtContractSignDate;

    // 서명란
    private JLabel lblEmployerSign;

    private JButton btnSaveChanges;
    private JButton btnPreview;
    private JButton btnGoBack;

    private final DecimalFormat numberFormatter = new DecimalFormat("#,###");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private boolean isProgrammaticChange = false;

    private final float FONT_SCALE_FACTOR = 1.2f;
    private java.awt.Font enlargedFont;
    private java.awt.Font enlargedFontBold;
    private java.awt.Font enlargedTitleFont;
    private java.awt.Font enlargedSectionTitleFont;
    private java.awt.Font enlargedHtmlFont;


    private int scale(int value) {
        return (int) (value * FONT_SCALE_FACTOR);
    }
    private float scale(float value) { return value * FONT_SCALE_FACTOR; }


    public EmploymentContractPage(PayrollApp payrollApp) {
        this.payrollApp = payrollApp;

        java.awt.Font baseFont = new JLabel().getFont();
        float newBaseSize = baseFont.getSize() * FONT_SCALE_FACTOR;
        enlargedFont = baseFont.deriveFont(newBaseSize);
        enlargedFontBold = baseFont.deriveFont(Font.BOLD, newBaseSize);
        enlargedTitleFont = baseFont.deriveFont(Font.BOLD, scale(24f));
        enlargedSectionTitleFont = baseFont.deriveFont(Font.BOLD, scale(16f));
        enlargedHtmlFont = baseFont.deriveFont(scale(14f));

        setLayout(new BorderLayout(scale(10), scale(10)));
        setBorder(new EmptyBorder(scale(15), scale(30), scale(15), scale(30)));

        initComponents();
        addListeners();
    }

    private void initComponents() {
        lblContractTitle = new JLabel("표 준 근 로 계 약 서", SwingConstants.CENTER);
        lblContractTitle.setFont(enlargedTitleFont);
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.add(lblContractTitle);
        add(titlePanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(scale(6), scale(8), scale(6), scale(8));
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int yPos = 0;

        JLabel lblEmployerTitle = new JLabel("사 업 주:"); lblEmployerTitle.setFont(enlargedFont);
        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;
        contentPanel.add(lblEmployerTitle, gbc);
        gbc.gridx = 1; gbc.gridy = yPos; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.WEST;
        lblCompanyName = new JLabel(); // 동적으로 설정될 예정
        lblCompanyName.setFont(enlargedFont);
        contentPanel.add(lblCompanyName, gbc);
        yPos++;

        JLabel lblEmployerAddressTitle = new JLabel("주소:"); lblEmployerAddressTitle.setFont(enlargedFont);
        gbc.gridx = 0; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.EAST;
        contentPanel.add(lblEmployerAddressTitle, gbc);
        gbc.gridx = 1; gbc.gridy = yPos; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.WEST;
        JLabel lblEmployerAddressValue = new JLabel("[회사 주소 입력 필요]"); // 이 부분도 설정으로 뺄 수 있음
        lblEmployerAddressValue.setFont(enlargedFont);
        contentPanel.add(lblEmployerAddressValue, gbc);
        yPos++;
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 4; gbc.insets = new Insets(scale(10),0,scale(10),0);
        contentPanel.add(new JSeparator(), gbc);
        yPos++;
        gbc.insets = new Insets(scale(6), scale(8), scale(6), scale(8));

        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 4;
        lblIntro1 = new JLabel();
        lblIntro1.setFont(enlargedHtmlFont);
        contentPanel.add(lblIntro1, gbc);
        yPos++;

        TitledBorder employeeInfoBorder = BorderFactory.createTitledBorder("제1조 (근로자 인적사항)");
        employeeInfoBorder.setTitleFont(enlargedSectionTitleFont);
        JPanel employeeInfoPanel = new JPanel(new GridBagLayout());
        employeeInfoPanel.setBorder(employeeInfoBorder);
        GridBagConstraints gbcEmp = new GridBagConstraints();
        gbcEmp.insets = new Insets(scale(3),scale(5),scale(3),scale(5));
        gbcEmp.anchor = GridBagConstraints.WEST;

        addLabelAndValue(employeeInfoPanel, "성명:", lblEmployeeNameValue = new JLabel(), gbcEmp, 0, 0);
        addLabelAndValue(employeeInfoPanel, "주민등록번호:", lblResidentRegNoValue = new JLabel(), gbcEmp, 2, 0);
        addLabelAndValue(employeeInfoPanel, "주소:", lblAddressValue = new JLabel(), gbcEmp, 0, 1, 3);
        addLabelAndValue(employeeInfoPanel, "연락처:", lblPhoneNumberValue = new JLabel(), gbcEmp, 0, 2);
        addLabelAndValue(employeeInfoPanel, "부서:", lblDepartmentValue = new JLabel(), gbcEmp, 2, 2);

        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 4; gbc.weightx = 1.0;
        contentPanel.add(employeeInfoPanel, gbc);
        yPos++;

        TitledBorder contractTermsBorder = BorderFactory.createTitledBorder("제2조 (계약 조건)");
        contractTermsBorder.setTitleFont(enlargedSectionTitleFont);
        JPanel contractTermsPanel = new JPanel(new GridBagLayout());
        contractTermsPanel.setBorder(contractTermsBorder);
        GridBagConstraints gbcTerms = new GridBagConstraints();
        gbcTerms.insets = new Insets(scale(3),scale(5),scale(3),scale(5));
        gbcTerms.anchor = GridBagConstraints.WEST;
        gbcTerms.fill = GridBagConstraints.HORIZONTAL;

        int cTermsY = 0;
        addLabelAndValue(contractTermsPanel, "계약 기간:", lblContractPeriodValue = new JLabel(), gbcTerms, 0, cTermsY++, 3);

        addLabelAndField(contractTermsPanel, "시작일:", txtContractStartDate = new JTextField(scale(10)), gbcTerms, 0, cTermsY);

        JPanel endDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, scale(5), 0));
        endDatePanel.setOpaque(false);
        txtContractEndDate = new JTextField(scale(10));
        txtContractEndDate.setFont(enlargedFont);
        JLabel lblAutoExtension = new JLabel("까지 별도의 계약이 체결되지 않으면 자동 연장된 것으로 본다.");
        lblAutoExtension.setFont(enlargedFont);
        endDatePanel.add(txtContractEndDate);
        endDatePanel.add(lblAutoExtension);
        addLabelAndComponent(contractTermsPanel, "만료일:", endDatePanel, gbcTerms, 2, cTermsY++, 2);


        addLabelAndValue(contractTermsPanel, "근무지:", lblWorkLocationValue = new JLabel(), gbcTerms, 0, cTermsY);
        addLabelAndValue(contractTermsPanel, "소재지:", lblSiteLocationValue = new JLabel(), gbcTerms, 2, cTermsY++);


        JPanel workHoursOuterPanel = new JPanel(new GridBagLayout());
        workHoursOuterPanel.setOpaque(false);
        GridBagConstraints gbcWorkRest = new GridBagConstraints();
        gbcWorkRest.insets = new Insets(0,0,0,scale(10));
        gbcWorkRest.anchor = GridBagConstraints.WEST;

        JLabel lblWorkHoursTitle = new JLabel("근무 시간:"); lblWorkHoursTitle.setFont(enlargedFont);
        JPanel workHoursPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0)); workHoursPanel.setOpaque(false);
        txtWorkHoursStart = new JTextField(scale(5)); txtWorkHoursStart.setFont(enlargedFont);
        JLabel lblTilde = new JLabel(" ~ "); lblTilde.setFont(enlargedFont);
        txtWorkHoursEnd = new JTextField(scale(5)); txtWorkHoursEnd.setFont(enlargedFont);
        workHoursPanel.add(txtWorkHoursStart); workHoursPanel.add(lblTilde); workHoursPanel.add(txtWorkHoursEnd);
        gbcWorkRest.gridx=0; gbcWorkRest.gridy=0; workHoursOuterPanel.add(lblWorkHoursTitle, gbcWorkRest);
        gbcWorkRest.gridx=1; gbcWorkRest.gridy=0; workHoursOuterPanel.add(workHoursPanel, gbcWorkRest);

        JLabel lblRestHoursTitle = new JLabel("휴게 시간:"); lblRestHoursTitle.setFont(enlargedFont);
        txtRestHours = new JTextField(scale(12)); txtRestHours.setFont(enlargedFont);
        gbcWorkRest.gridx=2; gbcWorkRest.gridy=0; gbcWorkRest.insets = new Insets(0,scale(20),0,scale(5));
        workHoursOuterPanel.add(lblRestHoursTitle, gbcWorkRest);
        gbcWorkRest.gridx=3; gbcWorkRest.gridy=0; gbcWorkRest.insets = new Insets(0,0,0,0);
        gbcWorkRest.weightx = 1.0; gbcWorkRest.fill = GridBagConstraints.HORIZONTAL;
        workHoursOuterPanel.add(txtRestHours, gbcWorkRest);


        gbcTerms.gridx = 0; gbcTerms.gridy = cTermsY++; gbcTerms.gridwidth = 4; gbcTerms.fill = GridBagConstraints.HORIZONTAL;
        contractTermsPanel.add(workHoursOuterPanel, gbcTerms);
        gbcTerms.gridwidth = 1; gbcTerms.fill = GridBagConstraints.NONE;


        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 4;
        contentPanel.add(contractTermsPanel, gbc);
        yPos++;


        TitledBorder salaryTermsBorder = BorderFactory.createTitledBorder("제3조 (임금 조건)");
        salaryTermsBorder.setTitleFont(enlargedSectionTitleFont);
        JPanel salaryTermsPanel = new JPanel(new GridBagLayout());
        salaryTermsPanel.setBorder(salaryTermsBorder);
        GridBagConstraints gbcSalary = new GridBagConstraints();
        gbcSalary.insets = new Insets(scale(3),scale(5),scale(3),scale(5));
        gbcSalary.anchor = GridBagConstraints.WEST;

        int sTermsY = 0;
        addLabelAndValue(salaryTermsPanel, "연봉:", lblAnnualSalaryValue = new JLabel(), gbcSalary, 0, sTermsY);
        addLabelAndValue(salaryTermsPanel, "월 기본급(209h):", lblMonthlyBasicSalaryValue = new JLabel(), gbcSalary, 2, sTermsY++);
        addLabelAndValue(salaryTermsPanel, "고정연장수당(10h):", lblFixedOvertimeAllowanceValue = new JLabel(), gbcSalary, 0, sTermsY);
        addLabelAndValue(salaryTermsPanel, "상여금:", lblBonusValue = new JLabel(), gbcSalary, 2, sTermsY++);
        addLabelAndValue(salaryTermsPanel, "기타수당:", lblOtherAllowanceValue = new JLabel(), gbcSalary, 0, sTermsY);
        addLabelAndValue(salaryTermsPanel, "식대:", lblMealAllowanceValue = new JLabel(), gbcSalary, 2, sTermsY++);
        addLabelAndValue(salaryTermsPanel, "차량유지비:", lblVehicleMaintenanceFeeValue = new JLabel(), gbcSalary, 0, sTermsY);
        addLabelAndValue(salaryTermsPanel, "연구개발비:", lblResearchDevelopmentExpenseValue = new JLabel(), gbcSalary, 2, sTermsY++);
        addLabelAndValue(salaryTermsPanel, "육아수당:", lblChildcareAllowanceValue = new JLabel(), gbcSalary, 0, sTermsY);

        addLabelAndField(salaryTermsPanel, "임금 지급일:", txtWagePaymentDate = new JTextField(scale(10)), gbcSalary, 2, sTermsY++);


        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 4;
        contentPanel.add(salaryTermsPanel, gbc);
        yPos++;

        JLabel lblOtherTermsTitle = new JLabel("제4조 (기타)"); lblOtherTermsTitle.setFont(enlargedSectionTitleFont);
        gbc.gridx = 0; gbc.gridy = yPos++; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(scale(15), 0, scale(5), 0);
        contentPanel.add(lblOtherTermsTitle, gbc);
        gbc.insets = new Insets(scale(6), scale(8), scale(6), scale(8));

        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 0.5;
        txtOtherTerms = new JTextArea(scale(5), scale(30));
        txtOtherTerms.setFont(enlargedFont);
        txtOtherTerms.setLineWrap(true);
        txtOtherTerms.setWrapStyleWord(true);
        JScrollPane otherTermsScrollPane = new JScrollPane(txtOtherTerms);
        otherTermsScrollPane.getVerticalScrollBar().setUnitIncrement(scale(16));
        contentPanel.add(otherTermsScrollPane, gbc);
        yPos++;
        gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblSignDateTitle = new JLabel("계약체결일:"); lblSignDateTitle.setFont(enlargedFont);
        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(lblSignDateTitle, gbc);
        gbc.gridx = 1; gbc.gridy = yPos; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        txtContractSignDate = new JTextField(scale(10)); txtContractSignDate.setFont(enlargedFont);
        contentPanel.add(txtContractSignDate, gbc);
        yPos++;

        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 4; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(scale(20), scale(5), scale(10), scale(5));
        JLabel lblSignOff = new JLabel("상기 근로 조건에 동의하며 본 계약을 성실히 이행할 것을 서약합니다.");
        lblSignOff.setFont(enlargedFont);
        contentPanel.add(lblSignOff, gbc);
        yPos++;

        lblEmployerSign = new JLabel(); // 동적으로 설정될 예정
        lblEmployerSign.setFont(enlargedFont);
        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        contentPanel.add(lblEmployerSign, gbc);
        JLabel lblEmployeeSign = new JLabel("근로자 (성명) :                         (서명 또는 인)"); lblEmployeeSign.setFont(enlargedFont);
        gbc.gridx = 2; gbc.gridy = yPos; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        contentPanel.add(lblEmployeeSign, gbc);
        yPos++;

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(scale(16));
        scrollPane.getHorizontalScrollBar().setUnitIncrement(scale(16));
        add(scrollPane, BorderLayout.CENTER);


        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, scale(10), scale(5)));
        btnSaveChanges = new JButton("계약 정보 저장"); btnSaveChanges.setFont(enlargedFont);
        btnPreview = new JButton("미리보기"); btnPreview.setFont(enlargedFont);
        btnGoBack = new JButton("뒤로 가기"); btnGoBack.setFont(enlargedFont);
        bottomButtonPanel.add(btnSaveChanges);
        bottomButtonPanel.add(btnPreview);
        bottomButtonPanel.add(btnGoBack);
        add(bottomButtonPanel, BorderLayout.SOUTH);
    }

    private void addLabelAndValue(JPanel panel, String labelText, JLabel valueLabel, GridBagConstraints gbc, int x, int y) {
        addLabelAndValue(panel, labelText, valueLabel, gbc, x, y, 1);
    }
    private void addLabelAndValue(JPanel panel, String labelText, JLabel valueLabel, GridBagConstraints gbc, int x, int y, int gridwidth) {
        JLabel label = new JLabel(labelText); label.setFont(enlargedFont);
        gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(label, gbc);
        valueLabel.setFont(enlargedFont);
        gbc.gridx = x + 1; gbc.gridy = y; gbc.gridwidth = gridwidth; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = (gridwidth > 1 ? 1.0 : 0.5);
        panel.add(valueLabel, gbc);
    }

    private void addLabelAndField(JPanel panel, String labelText, JTextField textField, GridBagConstraints gbc, int x, int y) {
        addLabelAndField(panel, labelText, textField, gbc, x, y, 1);
    }
    private void addLabelAndField(JPanel panel, String labelText, JTextField textField, GridBagConstraints gbc, int x, int y, int gridwidth) {
        JLabel label = new JLabel(labelText); label.setFont(enlargedFont);
        gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(label, gbc);
        textField.setFont(enlargedFont);
        gbc.gridx = x + 1; gbc.gridy = y; gbc.gridwidth = gridwidth; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = (gridwidth > 1 ? 1.0 : 0.5);
        panel.add(textField, gbc);
    }


    private void addLabelAndComponent(JPanel panel, String labelText, Component component, GridBagConstraints gbc, int x, int y, int gridwidth) {
        JLabel label = new JLabel(labelText); label.setFont(enlargedFont);
        gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(label, gbc);

        gbc.gridx = x + 1; gbc.gridy = y; gbc.gridwidth = gridwidth; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = (gridwidth > 1 ? 1.0 : 0.5);
        panel.add(component, gbc);
    }


    private void addListeners() {
        btnGoBack.addActionListener(e -> {
            if (payrollApp != null) {
                payrollApp.showCard("EmployeeManagement");
            }
        });

        btnSaveChanges.addActionListener(e -> saveContractData());
        btnPreview.addActionListener(e -> previewContract());
    }

    public void loadContractData(Employee employee, Payroll contractualPayroll) {
        this.currentEmployee = employee;
        this.contractualPayroll = contractualPayroll;
        isProgrammaticChange = true;

        // --- 설정값 로드 ---
        Map<String, String> settings = payrollApp.getPayrollManager().loadSettings();
        String companyName = settings.getOrDefault("companyName", "[주식회사 OO회사]");
        String defaultPayday = settings.getOrDefault("defaultPayday", "매월 25일");

        // --- UI 컴포넌트에 설정값 적용 ---
        lblCompanyName.setText(companyName);
        txtWagePaymentDate.setText(defaultPayday);
        lblEmployerSign.setText("사업주 (회사명) : " + companyName + " (직인)");


        if (employee == null) {
            clearAllFields(); // 필드 초기화
            // 빈 계약서 양식의 회사명 업데이트
            if (lblIntro1 != null) {
                lblIntro1.setText("<html><body style='text-align:center; font-family:\"" + enlargedHtmlFont.getFamily() + "\"; font-size:" + enlargedHtmlFont.getSize() + "pt;'>" +
                        companyName + " (이하 “회사”라 칭함)과 근로자 OOO (이하 “근로자”라 칭함)은 다음과 같이 근로계약을 체결한다.</body></html>");
            }
            isProgrammaticChange = false;
            return;
        }

        if (lblIntro1 != null) {
            lblIntro1.setText("<html><body style='text-align:center; font-family:\"" + enlargedHtmlFont.getFamily() + "\"; font-size:" + enlargedHtmlFont.getSize() + "pt;'>" +
                    companyName + " (이하 “회사”라 칭함)과 근로자 " + employee.getName() +" (이하 “근로자”라 칭함)은 다음과 같이 근로계약을 체결한다.</body></html>");
        }

        lblEmployeeNameValue.setText(employee.getName());
        lblResidentRegNoValue.setText(employee.getResidentRegistrationNumber());
        lblAddressValue.setText(employee.getAddress());
        lblPhoneNumberValue.setText(employee.getPhoneNumber());
        lblDepartmentValue.setText(employee.getDepartment());
        lblContractPeriodValue.setText(employee.getHireDate() != null ? employee.getHireDate() : "-");
        lblWorkLocationValue.setText(employee.getWorkLocation() != null ? employee.getWorkLocation() : "-");
        lblSiteLocationValue.setText(employee.getSiteLocation() != null ? employee.getSiteLocation() : "-");

        if (contractualPayroll != null) {
            lblAnnualSalaryValue.setText(numberFormatter.format(employee.getAnnualSalary()) + " 원");
            lblMonthlyBasicSalaryValue.setText(numberFormatter.format(contractualPayroll.getMonthlyBasicSalary()) + " 원");
            lblFixedOvertimeAllowanceValue.setText(numberFormatter.format(contractualPayroll.getFixedOvertimeAllowance()) + " 원");
            lblBonusValue.setText(numberFormatter.format(contractualPayroll.getBonus()) + " 원");
            lblOtherAllowanceValue.setText(numberFormatter.format(contractualPayroll.getOtherAllowance()) + " 원");
            lblMealAllowanceValue.setText(numberFormatter.format(contractualPayroll.getMealAllowance()) + " 원");
            lblVehicleMaintenanceFeeValue.setText(numberFormatter.format(contractualPayroll.getVehicleMaintenanceFee()) + " 원");
            lblResearchDevelopmentExpenseValue.setText(numberFormatter.format(contractualPayroll.getResearchDevelopmentExpense()) + " 원");
            lblChildcareAllowanceValue.setText(numberFormatter.format(contractualPayroll.getChildcareAllowance()) + " 원");
        } else {
            lblAnnualSalaryValue.setText(numberFormatter.format(employee.getAnnualSalary()) + " 원 (계약급여정보없음)");
            String zeroWon = numberFormatter.format(0) + " 원";
            lblMonthlyBasicSalaryValue.setText(zeroWon); lblFixedOvertimeAllowanceValue.setText(zeroWon);
            lblBonusValue.setText(zeroWon); lblOtherAllowanceValue.setText(zeroWon);
            lblMealAllowanceValue.setText(zeroWon); lblVehicleMaintenanceFeeValue.setText(zeroWon);
            lblResearchDevelopmentExpenseValue.setText(zeroWon); lblChildcareAllowanceValue.setText(zeroWon);
        }

        String effectiveStartDateStr = employee.getHireDate();
        if (employee.getSalaryChangeDate() != null && !employee.getSalaryChangeDate().isEmpty()) {
            try {
                LocalDate.parse(employee.getSalaryChangeDate(), dateFormatter);
                effectiveStartDateStr = employee.getSalaryChangeDate();
            } catch (DateTimeParseException e) {
                System.err.println("급여변동일(" + employee.getSalaryChangeDate() + ") 포맷 오류. 입사일을 계약 시작 기준으로 사용합니다.");
            }
        }

        if (effectiveStartDateStr != null && !effectiveStartDateStr.isEmpty()) {
            try {
                LocalDate effectiveStartDate = LocalDate.parse(effectiveStartDateStr, dateFormatter);
                txtContractStartDate.setText(dateFormatter.format(effectiveStartDate));
                txtContractSignDate.setText(dateFormatter.format(effectiveStartDate));

                LocalDate calculatedEndDate = effectiveStartDate.plusYears(1).minusDays(1);
                txtContractEndDate.setText(dateFormatter.format(calculatedEndDate));

            } catch (DateTimeParseException e) {
                txtContractStartDate.setText(effectiveStartDateStr);
                txtContractEndDate.setText("");
                txtContractSignDate.setText(effectiveStartDateStr);
                System.err.println("계약 시작일/종료일/체결일 설정 중 날짜 파싱 오류: " + effectiveStartDateStr);
            }
        } else {
            txtContractStartDate.setText("");
            txtContractEndDate.setText("");
            txtContractSignDate.setText(dateFormatter.format(LocalDate.now()));
        }

        txtWorkHoursStart.setText("09:00");
        txtWorkHoursEnd.setText("18:00");
        txtRestHours.setText("12:00 - 13:00 (1시간)");
        txtOtherTerms.setText(" - 기타 근로조건은 회사 취업규칙 및 관계 법령에 따른다.\n - 본 계약서에 명시되지 아니한 사항은 근로기준법 등 노동관계법령 및 회사의 취업규칙에 따른다.");

        isProgrammaticChange = false;
    }

    private void saveContractData() {
        if (currentEmployee == null) {
            JOptionPane.showMessageDialog(this, "먼저 직원을 선택하거나, 계약 정보를 입력할 대상이 없습니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String contractStartDate = txtContractStartDate.getText();
        String workHours = txtWorkHoursStart.getText() + " - " + txtWorkHoursEnd.getText();
        String wagePaymentDate = txtWagePaymentDate.getText();
        String contractSignDate = txtContractSignDate.getText();

        if (contractStartDate.isEmpty() || workHours.length() < 10 || wagePaymentDate.isEmpty() || contractSignDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "시작일, 근무 시간, 임금 지급일, 계약 체결일은 필수 입력 항목입니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        System.out.println("--- 계약 정보 저장 (콘솔 출력) ---");
        System.out.println("직원 ID: " + currentEmployee.getId() + ", 이름: " + currentEmployee.getName());
        System.out.println("계약 기간(입사일): " + lblContractPeriodValue.getText());
        System.out.println("시작일: " + contractStartDate);
        System.out.println("만료일: " + txtContractEndDate.getText());
        System.out.println("근무 시간: " + workHours);
        System.out.println("휴게 시간: " + txtRestHours.getText());
        System.out.println("임금 지급일: " + wagePaymentDate);
        System.out.println("기타 조건: " + txtOtherTerms.getText());
        System.out.println("계약 체결일: " + contractSignDate);
        System.out.println("---------------------------------");

        JOptionPane.showMessageDialog(this, "계약 정보가 임시로 처리되었습니다 (DB 저장 기능 미구현).", "저장 알림", JOptionPane.INFORMATION_MESSAGE);
    }

    private void previewContract() {
        if (currentEmployee == null) {
            JOptionPane.showMessageDialog(this, "표시할 직원 정보가 없습니다. 먼저 직원을 선택해주세요.", "정보 없음", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StringBuilder previewText = new StringBuilder();
        previewText.append("표 준 근 로 계 약 서\n\n");
        previewText.append(lblCompanyName.getText()).append(" (이하 “회사”라 칭함)과 근로자 ").append(currentEmployee.getName()).append(" (이하 “근로자”라 칭함)은 다음과 같이 근로계약을 체결한다.\n\n");

        previewText.append("제1조 (근로자 인적사항)\n");
        previewText.append("  성 명: ").append(lblEmployeeNameValue.getText()).append("\n");
        previewText.append("  주민등록번호: ").append(lblResidentRegNoValue.getText()).append("\n");
        previewText.append("  주 소: ").append(lblAddressValue.getText()).append("\n");
        previewText.append("  연 락 처: ").append(lblPhoneNumberValue.getText()).append("\n");
        previewText.append("  부 서: ").append(lblDepartmentValue.getText()).append("\n\n");

        previewText.append("제2조 (계약 조건)\n");
        previewText.append("  계약 기간: ").append(lblContractPeriodValue.getText()).append("\n");
        previewText.append("  시작일: ").append(txtContractStartDate.getText()).append("\n");
        previewText.append("  만료일: ").append(txtContractEndDate.getText()).append(" (까지 별도의 계약이 체결되지 않으면 자동 연장된 것으로 본다.)\n");
        previewText.append("  근 무 지: ").append(lblWorkLocationValue.getText()).append("\n");
        previewText.append("  소 재 지: ").append(lblSiteLocationValue.getText()).append("\n");
        previewText.append("  근무 시간: ").append(txtWorkHoursStart.getText()).append(" ~ ").append(txtWorkHoursEnd.getText()).append("\n");
        previewText.append("  휴게 시간: ").append(txtRestHours.getText()).append("\n\n");

        previewText.append("제3조 (임금 조건)\n");
        if (contractualPayroll != null) {
            previewText.append("  연 봉: ").append(lblAnnualSalaryValue.getText()).append("\n");
            previewText.append("  월 기본급(209h): ").append(lblMonthlyBasicSalaryValue.getText()).append("\n");
            previewText.append("  고정연장수당(10h): ").append(lblFixedOvertimeAllowanceValue.getText()).append("\n");
            previewText.append("  상 여 금: ").append(lblBonusValue.getText()).append("\n");
            previewText.append("  기타수당: ").append(lblOtherAllowanceValue.getText()).append("\n");
            previewText.append("  식 대: ").append(lblMealAllowanceValue.getText()).append("\n");
            previewText.append("  차량유지비: ").append(lblVehicleMaintenanceFeeValue.getText()).append("\n");
            previewText.append("  연구개발비: ").append(lblResearchDevelopmentExpenseValue.getText()).append("\n");
            previewText.append("  육아수당: ").append(lblChildcareAllowanceValue.getText()).append("\n");
        } else {
            previewText.append("  연 봉: ").append(lblAnnualSalaryValue.getText()).append("\n");
            previewText.append("  (계약 급여 정보 로드 필요)\n");
        }
        previewText.append("  임금지급일: ").append(txtWagePaymentDate.getText()).append("\n\n");


        previewText.append("제4조 (기타)\n");
        previewText.append("  ").append(txtOtherTerms.getText().replace("\n", "\n  ")).append("\n\n");

        previewText.append("계약체결일: ").append(txtContractSignDate.getText()).append("\n\n");
        previewText.append("상기 근로 조건에 동의하며 본 계약을 성실히 이행할 것을 서약합니다.\n\n");
        previewText.append(lblEmployerSign.getText()).append("\n");
        previewText.append("근로자 (성명) : ").append(currentEmployee.getName()).append(" (서명 또는 인)\n");


        JTextArea previewArea = new JTextArea(previewText.toString());
        previewArea.setFont(enlargedFont.deriveFont(enlargedFont.getSize2D() * 0.9f));
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(previewArea);
        scrollPane.setPreferredSize(new Dimension(scale(600), scale(700)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(scale(16));
        scrollPane.getHorizontalScrollBar().setUnitIncrement(scale(16));

        JOptionPane.showMessageDialog(this, scrollPane, "근로계약서 미리보기", JOptionPane.INFORMATION_MESSAGE);
    }


    private void clearAllFields() {
        isProgrammaticChange = true;

        // 직원 정보 초기화
        lblEmployeeNameValue.setText("-"); lblResidentRegNoValue.setText("-"); lblAddressValue.setText("-");
        lblPhoneNumberValue.setText("-"); lblDepartmentValue.setText("-");
        lblContractPeriodValue.setText("-");
        lblWorkLocationValue.setText("-"); lblSiteLocationValue.setText("-");

        // 급여 정보 초기화
        String na = "- 원";
        lblAnnualSalaryValue.setText(na); lblMonthlyBasicSalaryValue.setText(na); lblFixedOvertimeAllowanceValue.setText(na);
        lblBonusValue.setText(na); lblOtherAllowanceValue.setText(na); lblMealAllowanceValue.setText(na);
        lblVehicleMaintenanceFeeValue.setText(na); lblResearchDevelopmentExpenseValue.setText(na); lblChildcareAllowanceValue.setText(na);

        // 입력 필드 초기화
        txtContractStartDate.setText(""); txtContractEndDate.setText("");
        txtWorkHoursStart.setText("09:00"); txtWorkHoursEnd.setText("18:00"); txtRestHours.setText("12:00 - 13:00 (1시간)");
        txtOtherTerms.setText(" - 기타 근로조건은 회사 취업규칙 및 관계 법령에 따른다.\n - 본 계약서에 명시되지 아니한 사항은 근로기준법 등 노동관계법령 및 회사의 취업규칙에 따른다.");
        txtContractSignDate.setText(dateFormatter.format(LocalDate.now()));

        isProgrammaticChange = false;
    }

    public void displayPageForEmployee(Employee employee, Payroll contractualPayroll) {
        loadContractData(employee, contractualPayroll);
    }
}