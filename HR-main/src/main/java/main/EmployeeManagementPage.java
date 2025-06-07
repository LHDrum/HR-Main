package main;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class EmployeeManagementPage extends JPanel {
    private final PayrollManager payrollManager;
    private final Runnable returnToSummaryPage;
    private final PayrollApp payrollApp;


    private JComboBox<String> departmentComboBox;
    private JTextField nameField, residentRegNoField, phoneField, addressField, hireDateField, salaryChangeDateField;
    private JTextField annualSalaryField, hourlyWageField, monthlyBasicSalaryField, fixedOvertimeAllowanceField;
    private JTextField bonusField, otherAllowanceField, mealAllowanceField, vehicleMaintenanceFeeField;
    private JTextField researchDevelopmentExpenseField, childcareAllowanceField, totalMonthlyPayField;
    private JTextField workLocationField, siteLocationField;

    private JTextField searchField;
    private JButton searchButton, addButton, updateButton, deleteButton, clearButton, backButton;
    private JButton addRowButton;
    private JButton exportTableButton;
    private JButton createContractButton;


    private JTable employeeTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<TableModel> sorter;
    private int selectedEmployeeDbId = -1;
    private List<Employee> displayedEmployeesInTableOrder;
    private Set<Integer> modifiedRowModelIndices;

    private boolean isFormModified = false;

    private final DecimalFormat numberFormatter = new DecimalFormat("#,###");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private boolean isUpdatingFromForm = false;
    private boolean isUpdatingFromTable = false;
    private boolean isProgrammaticChange = false;

    private final float FONT_SCALE_FACTOR = 1.2f;
    private java.awt.Font enlargedFont;
    private java.awt.Font enlargedFontBold;
    private java.awt.Font totalsRowFont;

    private static final BigDecimal TOTAL_WORK_HOURS_DIVISOR = new BigDecimal("224.0");
    private static final BigDecimal FIXED_OVERTIME_HOURS_PARAM = new BigDecimal("10.0");
    private static final BigDecimal OT_MULTIPLIER = new BigDecimal("1.5");

    private static final int COL_NO = 0; private static final int COL_HIRE_DATE = 1; private static final int COL_NAME = 2;
    private static final int COL_DEPARTMENT = 3; private static final int COL_RESIDENT_REG_NO = 4; private static final int COL_PHONE = 5;
    private static final int COL_ADDRESS = 6; private static final int COL_ANNUAL_SALARY = 7; private static final int COL_MONTHLY_BASIC = 8;
    private static final int COL_FIXED_OVERTIME = 9; private static final int COL_ADDITIONAL_OVERTIME = 10; private static final int COL_BONUS = 11;
    private static final int COL_OTHER_ALLOWANCE = 12; private static final int COL_MEAL_ALLOWANCE = 13; private static final int COL_VEHICLE_FEE = 14;
    private static final int COL_RESEARCH_EXPENSE = 15; private static final int COL_CHILDCARE_ALLOWANCE = 16; private static final int COL_TOTAL_MONTHLY_PAY = 17;

    private final int[] sumColumnIndices = {
            COL_ANNUAL_SALARY, COL_MONTHLY_BASIC, COL_FIXED_OVERTIME, COL_ADDITIONAL_OVERTIME,
            COL_BONUS, COL_OTHER_ALLOWANCE, COL_MEAL_ALLOWANCE, COL_VEHICLE_FEE,
            COL_RESEARCH_EXPENSE, COL_CHILDCARE_ALLOWANCE, COL_TOTAL_MONTHLY_PAY
    };

    private int scale(int value) {
        return (int) (value * FONT_SCALE_FACTOR);
    }

    public EmployeeManagementPage(PayrollManager payrollManager, PayrollApp payrollApp, Runnable returnToSummaryPage) {
        this.payrollManager = payrollManager;
        this.payrollApp = payrollApp;
        this.returnToSummaryPage = returnToSummaryPage;

        java.awt.Font baseFont = new JLabel().getFont();
        float newBaseSize = baseFont.getSize() * FONT_SCALE_FACTOR;
        enlargedFont = baseFont.deriveFont(newBaseSize);
        enlargedFontBold = baseFont.deriveFont(Font.BOLD, newBaseSize);
        totalsRowFont = enlargedFontBold;

        this.displayedEmployeesInTableOrder = new ArrayList<>();
        this.modifiedRowModelIndices = new HashSet<>();
        setLayout(new BorderLayout(scale(10), scale(10)));
        initComponents();
        loadEmployeeTable("");
        addFormListeners();
        addTableListeners();
    }

    private String formatNumber(BigDecimal number) {
        if (number == null) return "0";
        return numberFormatter.format(number);
    }

    private BigDecimal parseFormattedNumber(String formattedNumber) throws NumberFormatException {
        if (formattedNumber == null || formattedNumber.trim().isEmpty() || formattedNumber.equals("-") || formattedNumber.equals("오류")) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(numberFormatter.parse(formattedNumber.trim()).toString());
        } catch (ParseException e) {
            try {
                return new BigDecimal(formattedNumber.trim().replace(",", ""));
            } catch (NumberFormatException nfe) {
                System.err.println("Error parsing number: " + formattedNumber + " - " + nfe.getMessage());
                throw nfe;
            }
        }
    }

    private BigDecimal parseValueToBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null || value.toString().trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return parseFormattedNumber(value.toString());
    }

    private class AutoFormattingFilter extends DocumentFilter {
        private final int maxDigits;
        private final String formatType;

        public AutoFormattingFilter(int maxDigits, String formatType) {
            this.maxDigits = maxDigits;
            this.formatType = formatType;
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            Document doc = fb.getDocument();
            String currentText = doc.getText(0, doc.getLength());
            String newText = currentText.substring(0, offset) + (text == null ? "" : text) + currentText.substring(offset + length);
            String digits = newText.replaceAll("\\D", "");

            if (digits.length() > maxDigits) {
                return;
            }

            String formatted = "";
            switch (formatType) {
                case "RRN": formatted = formatRRN(digits); break;
                case "PHONE": formatted = formatPhone(digits); break;
                case "DATE": formatted = formatDate(digits); break;
                default: formatted = digits; break;
            }

            fb.replace(0, doc.getLength(), formatted, attrs);
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, string, attr);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            replace(fb, offset, length, "", null);
        }

        private String formatRRN(String digits) {
            if (digits.length() > 6) return digits.substring(0, 6) + "-" + digits.substring(6);
            return digits;
        }

        private String formatDate(String digits) {
            if (digits.length() > 6) return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6);
            if (digits.length() > 4) return digits.substring(0, 4) + "-" + digits.substring(4);
            return digits;
        }

        private String formatPhone(String digits) {
            if (digits.length() < 3) return digits;
            if (digits.startsWith("02")) {
                if (digits.length() < 7) return "02-" + digits.substring(2);
                if (digits.length() <= 10) return "02-" + digits.substring(2, Math.min(6, digits.length() - 4)) + "-" + digits.substring(digits.length() - 4);
                return "02-1234-5678"; // fallback for > 10
            } else {
                if (digits.length() < 8) return digits.substring(0, 3) + "-" + digits.substring(3);
                if (digits.length() <= 11) return digits.substring(0, 3) + "-" + digits.substring(3, Math.min(7, digits.length() - 4)) + "-" + digits.substring(digits.length() - 4);
                return "010-1234-5678"; // fallback for > 11
            }
        }
    }

    private void formatNumericFieldOnFocusLost(JTextField field) {
        if (isProgrammaticChange || isUpdatingFromTable) return;
        try {
            String text = field.getText().replace(",", "");
            if (!text.isEmpty()) {
                BigDecimal value = new BigDecimal(text);
                isProgrammaticChange = true;
                field.setText(formatNumber(value));
                isProgrammaticChange = false;
            } else {
                isProgrammaticChange = true;
                field.setText(formatNumber(BigDecimal.ZERO));
                isProgrammaticChange = false;
            }
        } catch (NumberFormatException ex) {
            isProgrammaticChange = true;
            field.setText(formatNumber(BigDecimal.ZERO));
            isProgrammaticChange = false;
        }
    }

    private void initComponents() {
        JPanel topFormPanel = new JPanel(new GridLayout(1, 2, scale(20), 0));
        topFormPanel.setBorder(BorderFactory.createEmptyBorder(scale(5),scale(5),scale(5),scale(5)));

        JPanel employeeInfoPanel = new JPanel(new GridBagLayout());
        TitledBorder empInfoBorder = BorderFactory.createTitledBorder("직원 기본 정보");
        empInfoBorder.setTitleFont(enlargedFontBold);
        employeeInfoPanel.setBorder(empInfoBorder);

        GridBagConstraints gbcEmp = new GridBagConstraints();
        gbcEmp.insets = new Insets(scale(4), scale(5), scale(4), scale(5));
        gbcEmp.fill = GridBagConstraints.HORIZONTAL;
        gbcEmp.weightx = 1.0;

        JLabel searchLabel = new JLabel("직원 검색");
        searchLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 0; employeeInfoPanel.add(searchLabel, gbcEmp);

        searchField = new JTextField(scale(15));
        searchField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 0; gbcEmp.gridwidth = 2; employeeInfoPanel.add(searchField, gbcEmp);

        searchButton = new JButton("검색");
        searchButton.setFont(enlargedFont);
        gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0; gbcEmp.anchor = GridBagConstraints.LINE_START;
        gbcEmp.gridx = 3; gbcEmp.gridy = 0; gbcEmp.gridwidth = 1; employeeInfoPanel.add(searchButton, gbcEmp);

        JLabel hireDateLabel = new JLabel("입사일:");
        hireDateLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 1; employeeInfoPanel.add(hireDateLabel, gbcEmp);

        hireDateField = new JTextField(scale(10));
        hireDateField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 1; employeeInfoPanel.add(hireDateField, gbcEmp);

        JLabel salaryChangeDateLabel = new JLabel("급여변동일:");
        salaryChangeDateLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 2; gbcEmp.gridy = 1; employeeInfoPanel.add(salaryChangeDateLabel, gbcEmp);

        salaryChangeDateField = new JTextField(scale(10));
        salaryChangeDateField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 3; gbcEmp.gridy = 1; employeeInfoPanel.add(salaryChangeDateField, gbcEmp);

        JLabel departmentLabel = new JLabel("부서:");
        departmentLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 2; employeeInfoPanel.add(departmentLabel, gbcEmp);

        String[] departments = {"", "임원", "UI/UX", "프론트", "백엔드", "경영지원"};
        departmentComboBox = new JComboBox<>(departments);
        departmentComboBox.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 2; employeeInfoPanel.add(departmentComboBox, gbcEmp);

        JLabel nameLabel = new JLabel("이름:");
        nameLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 2; gbcEmp.gridy = 2; employeeInfoPanel.add(nameLabel, gbcEmp);

        nameField = new JTextField(scale(10));
        nameField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 3; gbcEmp.gridy = 2; employeeInfoPanel.add(nameField, gbcEmp);

        JLabel residentRegNoLabel = new JLabel("주민번호:");
        residentRegNoLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 3; employeeInfoPanel.add(residentRegNoLabel, gbcEmp);

        residentRegNoField = new JTextField(scale(10));
        residentRegNoField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 3; employeeInfoPanel.add(residentRegNoField, gbcEmp);

        JLabel phoneLabel = new JLabel("전화번호:");
        phoneLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 2; gbcEmp.gridy = 3; employeeInfoPanel.add(phoneLabel, gbcEmp);

        phoneField = new JTextField(scale(10));
        phoneField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 3; gbcEmp.gridy = 3; employeeInfoPanel.add(phoneField, gbcEmp);

        JLabel addressLabel = new JLabel("주소:");
        addressLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 4; employeeInfoPanel.add(addressLabel, gbcEmp);

        addressField = new JTextField(scale(25));
        addressField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 4; gbcEmp.gridwidth = 3; employeeInfoPanel.add(addressField, gbcEmp);
        gbcEmp.gridwidth = 1;

        JLabel workLocationLabel = new JLabel("근무지:");
        workLocationLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 5; employeeInfoPanel.add(workLocationLabel, gbcEmp);

        workLocationField = new JTextField(scale(25));
        workLocationField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 5; gbcEmp.gridwidth = 3; employeeInfoPanel.add(workLocationField, gbcEmp);
        gbcEmp.gridwidth = 1;

        JLabel siteLocationLabel = new JLabel("소재지:");
        siteLocationLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 6; employeeInfoPanel.add(siteLocationLabel, gbcEmp);

        siteLocationField = new JTextField(scale(25));
        siteLocationField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 6; gbcEmp.gridwidth = 3; employeeInfoPanel.add(siteLocationField, gbcEmp);
        gbcEmp.gridwidth = 1;

        gbcEmp.gridy = 7; gbcEmp.weighty = 1.0; gbcEmp.fill = GridBagConstraints.BOTH;
        employeeInfoPanel.add(new JLabel(), gbcEmp);

        topFormPanel.add(employeeInfoPanel);

        JPanel salaryInfoPanel = new JPanel(new GridBagLayout());
        TitledBorder salaryInfoBorder = BorderFactory.createTitledBorder("급여 상세 정보");
        salaryInfoBorder.setTitleFont(enlargedFontBold);
        salaryInfoPanel.setBorder(salaryInfoBorder);

        GridBagConstraints gbcSal = new GridBagConstraints();
        gbcSal.insets = new Insets(scale(4), scale(5), scale(4), scale(5));
        gbcSal.fill = GridBagConstraints.HORIZONTAL;
        gbcSal.weightx = 1.0;

        JLabel annualSalaryLabel = new JLabel("연봉:");
        annualSalaryLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 0; gbcSal.gridy = 0; salaryInfoPanel.add(annualSalaryLabel, gbcSal);

        annualSalaryField = new JTextField(scale(10));
        annualSalaryField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 1; gbcSal.gridy = 0; salaryInfoPanel.add(annualSalaryField, gbcSal);

        JLabel hourlyWageLabel = new JLabel("시급(월급/224h):");
        hourlyWageLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 2; gbcSal.gridy = 0; salaryInfoPanel.add(hourlyWageLabel, gbcSal);

        hourlyWageField = new JTextField(scale(8));
        hourlyWageField.setFont(enlargedFont);
        hourlyWageField.setEditable(false);
        hourlyWageField.setForeground(Color.BLUE);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 3; gbcSal.gridy = 0; salaryInfoPanel.add(hourlyWageField, gbcSal);

        JLabel monthlyBasicSalaryLabel = new JLabel("기본급(재산정):");
        monthlyBasicSalaryLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 0; gbcSal.gridy = 1; salaryInfoPanel.add(monthlyBasicSalaryLabel, gbcSal);

        monthlyBasicSalaryField = new JTextField(scale(10));
        monthlyBasicSalaryField.setFont(enlargedFont);
        monthlyBasicSalaryField.setEditable(false);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 1; gbcSal.gridy = 1; salaryInfoPanel.add(monthlyBasicSalaryField, gbcSal);

        JLabel fixedOvertimeAllowanceLabel = new JLabel("고정연장수당(재산정):");
        fixedOvertimeAllowanceLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 2; gbcSal.gridy = 1; salaryInfoPanel.add(fixedOvertimeAllowanceLabel, gbcSal);

        fixedOvertimeAllowanceField = new JTextField(scale(8));
        fixedOvertimeAllowanceField.setFont(enlargedFont);
        fixedOvertimeAllowanceField.setEditable(false);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 3; gbcSal.gridy = 1; salaryInfoPanel.add(fixedOvertimeAllowanceField, gbcSal);

        JLabel bonusLabel = new JLabel("상여금:");
        bonusLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 0; gbcSal.gridy = 2; salaryInfoPanel.add(bonusLabel, gbcSal);

        bonusField = new JTextField(scale(10));
        bonusField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 1; gbcSal.gridy = 2; salaryInfoPanel.add(bonusField, gbcSal);

        JLabel otherAllowanceLabel = new JLabel("기타수당:");
        otherAllowanceLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 2; gbcSal.gridy = 2; salaryInfoPanel.add(otherAllowanceLabel, gbcSal);

        otherAllowanceField = new JTextField(scale(8));
        otherAllowanceField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 3; gbcSal.gridy = 2; salaryInfoPanel.add(otherAllowanceField, gbcSal);

        JLabel mealAllowanceLabel = new JLabel("식대:");
        mealAllowanceLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 0; gbcSal.gridy = 3; salaryInfoPanel.add(mealAllowanceLabel, gbcSal);

        mealAllowanceField = new JTextField(scale(10));
        mealAllowanceField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 1; gbcSal.gridy = 3; salaryInfoPanel.add(mealAllowanceField, gbcSal);

        JLabel vehicleMaintenanceFeeLabel = new JLabel("차량유지비:");
        vehicleMaintenanceFeeLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 2; gbcSal.gridy = 3; salaryInfoPanel.add(vehicleMaintenanceFeeLabel, gbcSal);

        vehicleMaintenanceFeeField = new JTextField(scale(8));
        vehicleMaintenanceFeeField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 3; gbcSal.gridy = 3; salaryInfoPanel.add(vehicleMaintenanceFeeField, gbcSal);

        JLabel researchDevelopmentExpenseLabel = new JLabel("연구개발비:");
        researchDevelopmentExpenseLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 0; gbcSal.gridy = 4; salaryInfoPanel.add(researchDevelopmentExpenseLabel, gbcSal);

        researchDevelopmentExpenseField = new JTextField(scale(10));
        researchDevelopmentExpenseField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 1; gbcSal.gridy = 4; salaryInfoPanel.add(researchDevelopmentExpenseField, gbcSal);

        JLabel childcareAllowanceLabel = new JLabel("육아수당:");
        childcareAllowanceLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 2; gbcSal.gridy = 4; salaryInfoPanel.add(childcareAllowanceLabel, gbcSal);

        childcareAllowanceField = new JTextField(scale(8));
        childcareAllowanceField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 3; gbcSal.gridy = 4; salaryInfoPanel.add(childcareAllowanceField, gbcSal);

        JLabel totalMonthlyPayLabel = new JLabel("총 월 급여:");
        totalMonthlyPayLabel.setFont(enlargedFontBold);
        gbcSal.gridx = 0; gbcSal.gridy = 5; gbcSal.gridwidth = 1;
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        salaryInfoPanel.add(totalMonthlyPayLabel, gbcSal);

        totalMonthlyPayField = new JTextField(scale(10));
        totalMonthlyPayField.setFont(enlargedFontBold);
        totalMonthlyPayField.setForeground(Color.BLUE);
        totalMonthlyPayField.setEditable(false);
        gbcSal.gridx = 1; gbcSal.gridy = 5;
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        salaryInfoPanel.add(totalMonthlyPayField, gbcSal);

        gbcSal.gridx = 2; gbcSal.gridy = 5; salaryInfoPanel.add(new JLabel(" "), gbcSal);
        gbcSal.gridx = 3; gbcSal.gridy = 5; salaryInfoPanel.add(new JLabel(" "), gbcSal);


        gbcSal.gridy = 6; gbcSal.weighty = 1.0; gbcSal.fill = GridBagConstraints.BOTH;
        salaryInfoPanel.add(new JLabel(), gbcSal);

        topFormPanel.add(salaryInfoPanel);
        add(topFormPanel, BorderLayout.NORTH);

        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, scale(10), scale(10)));
        createContractButton = new JButton("근로계약서 작성");
        createContractButton.setFont(enlargedFont);
        addRowButton = new JButton("+ 행 추가");
        addRowButton.setFont(enlargedFont);
        exportTableButton = new JButton("표 엑셀 저장");
        exportTableButton.setFont(enlargedFont);
        addButton = new JButton("직원 등록");
        addButton.setFont(enlargedFont);
        updateButton = new JButton("수정사항 저장");
        updateButton.setFont(enlargedFont);
        updateButton.setEnabled(false);
        deleteButton = new JButton("직원 삭제");
        deleteButton.setFont(enlargedFont);
        clearButton = new JButton("입력 초기화");
        clearButton.setFont(enlargedFont);
        backButton = new JButton("뒤로 가기");
        backButton.setFont(enlargedFont);

        bottomButtonPanel.add(createContractButton);
        bottomButtonPanel.add(addRowButton);
        bottomButtonPanel.add(exportTableButton);
        bottomButtonPanel.add(addButton);
        bottomButtonPanel.add(updateButton);
        bottomButtonPanel.add(deleteButton);
        bottomButtonPanel.add(clearButton);
        bottomButtonPanel.add(backButton);
        add(bottomButtonPanel, BorderLayout.SOUTH);

        String[] tableColumnNames = { "No.", "입사일", "이름", "부서", "주민번호", "전화번호", "주소", "연봉", "기본급", "고정연장수당", "추가 연장 수당", "상여금", "기타수당", "식대", "차량유지비", "연구개발비", "육아수당", "총 월 급여" };
        tableModel = new DefaultTableModel(tableColumnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                if (getRowCount() <= 0 || row >= getRowCount()) return false;
                Object noValue = getValueAt(row, COL_NO);
                if (noValue != null && noValue.toString().equals("합계")) {
                    return false;
                }
                switch (column) {
                    case COL_NO:
                    case COL_MONTHLY_BASIC:
                    case COL_FIXED_OVERTIME:
                    case COL_ADDITIONAL_OVERTIME:
                    case COL_TOTAL_MONTHLY_PAY:
                        return false;
                    default:
                        return true;
                }
            }
        };

        employeeTable = new JTable(tableModel) {
            @Override
            public boolean editCellAt(int row, int column, java.util.EventObject e) {
                boolean result = super.editCellAt(row, column, e);
                if (result) {
                    final Component editor = getEditorComponent();
                    if (editor instanceof JTextComponent) {
                        if (e instanceof KeyEvent) {
                            ((JTextComponent) editor).setText(String.valueOf(((KeyEvent) e).getKeyChar()));
                        } else {
                            SwingUtilities.invokeLater(((JTextComponent) editor)::selectAll);
                        }
                    }
                }
                return result;
            }
        };
        new TableClipboardAdapter(employeeTable);
        employeeTable.setSurrendersFocusOnKeystroke(true);

        employeeTable.setFont(enlargedFont);
        employeeTable.getTableHeader().setFont(enlargedFontBold);
        employeeTable.setDefaultRenderer(Object.class, new TotalsAwareNumberRenderer(enlargedFont, totalsRowFont, sumColumnIndices));

        FontMetrics fm = employeeTable.getFontMetrics(enlargedFont);
        int fontHeight = fm.getHeight();
        int rowPadding = scale(6);
        employeeTable.setRowHeight(fontHeight + rowPadding);

        employeeTable.setShowGrid(true);
        employeeTable.setGridColor(new Color(220,220,220));
        employeeTable.setIntercellSpacing(new Dimension(1,1));

        employeeTable.setFillsViewportHeight(true);
        sorter = new TableRowSorter<>(tableModel);
        employeeTable.setRowSorter(sorter);
        TableColumnModel tcm = employeeTable.getColumnModel();

        tcm.getColumn(COL_NO).setPreferredWidth(scale(40));
        tcm.getColumn(COL_HIRE_DATE).setPreferredWidth(scale(90));
        tcm.getColumn(COL_NAME).setPreferredWidth(scale(80));
        tcm.getColumn(COL_DEPARTMENT).setPreferredWidth(scale(80));
        tcm.getColumn(COL_RESIDENT_REG_NO).setPreferredWidth(scale(120));
        tcm.getColumn(COL_PHONE).setPreferredWidth(scale(110));
        tcm.getColumn(COL_ADDRESS).setPreferredWidth(scale(200));

        for (int i = COL_ANNUAL_SALARY; i < tableColumnNames.length ; i++) {
            try {
                if (i == COL_ANNUAL_SALARY) tcm.getColumn(i).setPreferredWidth(scale(90));
                else if (i == COL_MONTHLY_BASIC || i == COL_FIXED_OVERTIME || i == COL_ADDITIONAL_OVERTIME) tcm.getColumn(i).setPreferredWidth(scale(100));
                else if (i == COL_TOTAL_MONTHLY_PAY) tcm.getColumn(i).setPreferredWidth(scale(110));
                else tcm.getColumn(i).setPreferredWidth(scale(90));
            } catch (ArrayIndexOutOfBoundsException e){
                System.err.println("테이블 컬럼 (" + i + ") 설정 오류 (존재하지 않는 컬럼 인덱스): " + e.getMessage());
            }
        }

        JScrollPane scrollPane = new JScrollPane(employeeTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addFormListeners() {
        ((AbstractDocument) hireDateField.getDocument()).setDocumentFilter(new AutoFormattingFilter(8, "DATE"));
        ((AbstractDocument) salaryChangeDateField.getDocument()).setDocumentFilter(new AutoFormattingFilter(8, "DATE"));
        ((AbstractDocument) residentRegNoField.getDocument()).setDocumentFilter(new AutoFormattingFilter(13, "RRN"));
        ((AbstractDocument) phoneField.getDocument()).setDocumentFilter(new AutoFormattingFilter(11, "PHONE"));

        DocumentListener formEditListener = new DocumentListener() {
            private void handleUpdate() {
                if (isProgrammaticChange || isUpdatingFromForm) return;
                isFormModified = true;
                updateButton.setEnabled(true);
                recalculateSalaryComponents();
            }
            public void insertUpdate(DocumentEvent e) { handleUpdate(); }
            public void removeUpdate(DocumentEvent e) { handleUpdate(); }
            public void changedUpdate(DocumentEvent e) { handleUpdate(); }
        };

        Component[] employeeInfoComponents = ((JPanel)nameField.getParent()).getComponents();
        for(Component c : employeeInfoComponents) {
            if (c instanceof JTextField) {
                ((JTextField)c).getDocument().addDocumentListener(formEditListener);
            }
        }

        Component[] salaryInfoComponents = ((JPanel)annualSalaryField.getParent()).getComponents();
        for(Component c : salaryInfoComponents) {
            if (c instanceof JTextField && ((JTextField)c).isEditable()) {
                ((JTextField)c).getDocument().addDocumentListener(formEditListener);
            }
        }

        departmentComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !isProgrammaticChange && !isUpdatingFromForm) {
                isFormModified = true;
                updateButton.setEnabled(true);
            }
        });

        FocusAdapter numericFormatter = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                formatNumericFieldOnFocusLost((JTextField)e.getSource());
            }
        };

        annualSalaryField.addFocusListener(numericFormatter);
        bonusField.addFocusListener(numericFormatter);
        otherAllowanceField.addFocusListener(numericFormatter);
        mealAllowanceField.addFocusListener(numericFormatter);
        vehicleMaintenanceFeeField.addFocusListener(numericFormatter);
        researchDevelopmentExpenseField.addFocusListener(numericFormatter);
        childcareAllowanceField.addFocusListener(numericFormatter);

        searchButton.addActionListener(e -> loadEmployeeTable(searchField.getText()));
        clearButton.addActionListener(e -> clearForm());
        backButton.addActionListener(e -> returnToSummaryPage.run());
        addButton.addActionListener(e -> addEmployee());
        updateButton.addActionListener(e -> saveChanges());
        deleteButton.addActionListener(e -> deleteSelectedEmployee());
        createContractButton.addActionListener(e -> createContract());
        addRowButton.addActionListener(e -> {
            tableModel.insertRow(tableModel.getRowCount() - 1, new Object[tableModel.getColumnCount()]);
        });
    }

    private void saveChanges() {
        if (employeeTable.isEditing() && !employeeTable.getCellEditor().stopCellEditing()) {
            JOptionPane.showMessageDialog(this, "셀 입력 값에 오류가 있습니다. 수정을 완료하거나 취소해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Set<Integer> employeeIdsToUpdate = new HashSet<>();
        if (isFormModified && selectedEmployeeDbId != -1) {
            employeeIdsToUpdate.add(selectedEmployeeDbId);
        }
        for (int modelRow : modifiedRowModelIndices) {
            if (modelRow < displayedEmployeesInTableOrder.size()) {
                employeeIdsToUpdate.add(displayedEmployeesInTableOrder.get(modelRow).getId());
            }
        }

        if (employeeIdsToUpdate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "수정된 항목이 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int successCount = 0;
        Set<String> failedNames = new HashSet<>();

        for (int empId : employeeIdsToUpdate) {
            try {
                Employee empToSave;
                Payroll payrollToSave;

                if (empId == selectedEmployeeDbId) {
                    empToSave = getEmployeeFromForm();
                    payrollToSave = getPayrollFromForm();
                } else {
                    int modelRow = findModelRowByEmployeeId(empId);
                    if (modelRow == -1) continue;

                    empToSave = getEmployeeFromTableRow(modelRow);
                    BigDecimal annualSalary = empToSave.getAnnualSalary();
                    BigDecimal monthlyGross = annualSalary.divide(new BigDecimal("12"), 0, RoundingMode.HALF_UP);
                    BigDecimal hourlyRate = monthlyGross.divide(TOTAL_WORK_HOURS_DIVISOR, 0, RoundingMode.HALF_UP);
                    BigDecimal fixedOvertime = hourlyRate.multiply(FIXED_OVERTIME_HOURS_PARAM).multiply(OT_MULTIPLIER).setScale(0, RoundingMode.HALF_UP);
                    BigDecimal basicPay = monthlyGross.subtract(fixedOvertime);

                    payrollToSave = getPayrollFromTableRow(modelRow);
                    payrollToSave.setMonthlyBasicSalary(basicPay);
                    payrollToSave.setFixedOvertimeAllowance(fixedOvertime);
                }

                if (payrollManager.updateEmployee(empToSave, payrollToSave)) {
                    successCount++;
                } else {
                    failedNames.add(empToSave.getName());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                payrollManager.getAllEmployees().stream()
                        .filter(e -> e.getId() == empId).findFirst()
                        .ifPresent(e -> failedNames.add(e.getName() + " (" + ex.getMessage() + ")"));
            }
        }

        if (failedNames.isEmpty()) {
            JOptionPane.showMessageDialog(this, successCount + "명의 직원 정보가 성공적으로 수정되었습니다.", "수정 완료", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "성공: " + successCount + "건\n실패: " + failedNames.size() + "건 (직원: " + String.join(", ", failedNames) + ")",
                    "부분 성공", JOptionPane.ERROR_MESSAGE);
        }

        modifiedRowModelIndices.clear();
        isFormModified = false;
        updateButton.setEnabled(false);
        updateButton.setText("수정사항 저장");
        loadEmployeeTable(searchField.getText());
    }

    private int findModelRowByEmployeeId(int empId) {
        for (int i = 0; i < displayedEmployeesInTableOrder.size(); i++) {
            if (displayedEmployeesInTableOrder.get(i).getId() == empId) {
                return i;
            }
        }
        return -1;
    }

    // ... 이하 나머지 코드는 이전과 동일 ...

    public void loadEmployeeTable(String searchTerm) {
        isUpdatingFromTable = true;
        tableModel.setRowCount(0);
        displayedEmployeesInTableOrder.clear();
        modifiedRowModelIndices.clear();

        List<Employee> employees = payrollManager.getAllEmployees().stream()
                .filter(e -> searchTerm.isEmpty() || e.getName().contains(searchTerm) || e.getDepartment().contains(searchTerm))
                .sorted(Comparator.comparing(Employee::getName))
                .collect(Collectors.toList());

        BigDecimal[] totals = new BigDecimal[tableModel.getColumnCount()];
        for (int i = 0; i < totals.length; i++) {
            totals[i] = BigDecimal.ZERO;
        }

        int rowNum = 1;
        for (Employee emp : employees) {
            displayedEmployeesInTableOrder.add(emp);
            Optional<Payroll> payrollOpt = payrollManager.getContractualPayroll(emp.getId());
            if (payrollOpt.isPresent()) {
                Payroll payroll = payrollOpt.get();
                Object[] rowData = {
                        rowNum++,
                        emp.getHireDate() != null ? emp.getHireDate().format(dateFormatter) : "",
                        emp.getName(),
                        emp.getDepartment(),
                        emp.getResidentRegistrationNumber(),
                        emp.getPhoneNumber(),
                        emp.getAddress(),
                        emp.getAnnualSalary(),
                        payroll.getMonthlyBasicSalary(),
                        payroll.getFixedOvertimeAllowance(),
                        payroll.getAdditionalOvertimePremium(),
                        payroll.getBonus(),
                        payroll.getOtherAllowance(),
                        payroll.getMealAllowance(),
                        payroll.getVehicleMaintenanceFee(),
                        payroll.getResearchDevelopmentExpense(),
                        payroll.getChildcareAllowance(),
                        payroll.getGrossPay()
                };
                tableModel.addRow(rowData);

                for(int idx : sumColumnIndices) {
                    if(rowData[idx] instanceof BigDecimal) {
                        totals[idx] = totals[idx].add((BigDecimal)rowData[idx]);
                    }
                }
            }
        }

        Object[] totalsRow = new Object[tableModel.getColumnCount()];
        totalsRow[COL_NO] = "합계";
        for(int idx : sumColumnIndices) {
            totalsRow[idx] = totals[idx];
        }
        tableModel.addRow(totalsRow);

        isUpdatingFromTable = false;
    }

    private void addTableListeners() {
        employeeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !isUpdatingFromForm) {
                int selectedViewRow = employeeTable.getSelectedRow();
                if (selectedViewRow != -1) {
                    int modelRow = employeeTable.convertRowIndexToModel(selectedViewRow);
                    if(modelRow >= 0 && modelRow < displayedEmployeesInTableOrder.size()){
                        Employee emp = displayedEmployeesInTableOrder.get(modelRow);
                        Optional<Payroll> payrollOpt = payrollManager.getContractualPayroll(emp.getId());
                        payrollOpt.ifPresent(p -> {
                            selectedEmployeeDbId = emp.getId();
                            populateForm(emp, p);
                        });
                    } else {
                        clearForm();
                    }
                }
            }
        });

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && !isUpdatingFromForm && !isUpdatingFromTable) {
                int modelRow = e.getFirstRow();
                if (modelRow >= displayedEmployeesInTableOrder.size()) return;

                modifiedRowModelIndices.add(modelRow);
                updateButton.setEnabled(true);
            }
        });
    }

    private void recalculateSalaryComponents() {
        if(isProgrammaticChange) return;

        try {
            BigDecimal annualSalary = parseFormattedNumber(annualSalaryField.getText());
            BigDecimal bonus = parseFormattedNumber(bonusField.getText());
            BigDecimal otherAllowance = parseFormattedNumber(otherAllowanceField.getText());
            BigDecimal mealAllowance = parseFormattedNumber(mealAllowanceField.getText());
            BigDecimal vehicleFee = parseFormattedNumber(vehicleMaintenanceFeeField.getText());
            BigDecimal researchExpense = parseFormattedNumber(researchDevelopmentExpenseField.getText());
            BigDecimal childcareAllowance = parseFormattedNumber(childcareAllowanceField.getText());

            BigDecimal totalAllowances = bonus.add(otherAllowance).add(mealAllowance).add(vehicleFee).add(researchExpense).add(childcareAllowance);

            BigDecimal monthlyGross = annualSalary.divide(new BigDecimal("12"), 0, RoundingMode.HALF_UP);
            BigDecimal hourlyRate = monthlyGross.divide(TOTAL_WORK_HOURS_DIVISOR, 0, RoundingMode.HALF_UP);
            BigDecimal fixedOvertime = hourlyRate.multiply(FIXED_OVERTIME_HOURS_PARAM).multiply(OT_MULTIPLIER).setScale(0, RoundingMode.HALF_UP);
            BigDecimal basicPay = monthlyGross.subtract(fixedOvertime);

            isProgrammaticChange = true;
            hourlyWageField.setText(formatNumber(hourlyRate));
            monthlyBasicSalaryField.setText(formatNumber(basicPay));
            fixedOvertimeAllowanceField.setText(formatNumber(fixedOvertime));
            totalMonthlyPayField.setText(formatNumber(basicPay.add(fixedOvertime).add(totalAllowances)));
            isProgrammaticChange = false;

        } catch (NumberFormatException ex) {
            isProgrammaticChange = true;
            hourlyWageField.setText("오류");
            monthlyBasicSalaryField.setText("오류");
            fixedOvertimeAllowanceField.setText("오류");
            totalMonthlyPayField.setText("오류");
            isProgrammaticChange = false;
        }
    }

    private void clearForm() {
        isProgrammaticChange = true;
        selectedEmployeeDbId = -1;

        for(Component c : ((JPanel)nameField.getParent()).getComponents()){
            if(c instanceof JTextField) ((JTextField) c).setText("");
        }
        departmentComboBox.setSelectedIndex(0);

        for(Component c : ((JPanel)annualSalaryField.getParent()).getComponents()){
            if(c instanceof JTextField) ((JTextField) c).setText("");
        }

        Map<String, String> settings = payrollManager.loadSettings();
        mealAllowanceField.setText(formatNumber(new BigDecimal(settings.getOrDefault("defaultMealAllowance", "200000"))));
        childcareAllowanceField.setText(formatNumber(new BigDecimal(settings.getOrDefault("defaultChildcareAllowance", "0"))));

        isProgrammaticChange = false;
        recalculateSalaryComponents();
        employeeTable.clearSelection();
        isFormModified = false;
        updateButton.setEnabled(false);
    }

    private void populateForm(Employee emp, Payroll payroll) {
        isProgrammaticChange = true;
        isUpdatingFromForm = true;

        hireDateField.setText(emp.getHireDate() != null ? emp.getHireDate().format(dateFormatter) : "");
        salaryChangeDateField.setText(emp.getSalaryChangeDate() != null ? emp.getSalaryChangeDate().format(dateFormatter) : "");
        departmentComboBox.setSelectedItem(emp.getDepartment() != null ? emp.getDepartment() : "");
        nameField.setText(emp.getName());
        residentRegNoField.setText(emp.getResidentRegistrationNumber());
        phoneField.setText(emp.getPhoneNumber());
        addressField.setText(emp.getAddress());
        workLocationField.setText(emp.getWorkLocation());
        siteLocationField.setText(emp.getSiteLocation());

        annualSalaryField.setText(formatNumber(emp.getAnnualSalary()));
        monthlyBasicSalaryField.setText(formatNumber(payroll.getMonthlyBasicSalary()));
        fixedOvertimeAllowanceField.setText(formatNumber(payroll.getFixedOvertimeAllowance()));
        bonusField.setText(formatNumber(payroll.getBonus()));
        otherAllowanceField.setText(formatNumber(payroll.getOtherAllowance()));
        mealAllowanceField.setText(formatNumber(payroll.getMealAllowance()));
        vehicleMaintenanceFeeField.setText(formatNumber(payroll.getVehicleMaintenanceFee()));
        researchDevelopmentExpenseField.setText(formatNumber(payroll.getResearchDevelopmentExpense()));
        childcareAllowanceField.setText(formatNumber(payroll.getChildcareAllowance()));

        BigDecimal monthlyGrossFromAnnual = emp.getAnnualSalary().divide(new BigDecimal("12"), 0, RoundingMode.HALF_UP);
        BigDecimal hourlyRate = monthlyGrossFromAnnual.divide(TOTAL_WORK_HOURS_DIVISOR, 0, RoundingMode.HALF_UP);
        hourlyWageField.setText(formatNumber(hourlyRate));

        totalMonthlyPayField.setText(formatNumber(payroll.getGrossPay()));

        isProgrammaticChange = false;
        isUpdatingFromForm = false;
        isFormModified = false;
        updateButton.setEnabled(false);
    }

    private void addEmployee() {
        if(nameField.getText().trim().isEmpty() || residentRegNoField.getText().trim().length() < 14 || annualSalaryField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름, 주민번호(13자리), 연봉은 필수 입력 항목입니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Employee emp = getEmployeeFromForm();
        Payroll payroll = getPayrollFromForm();

        if(payrollManager.addEmployee(emp, payroll)) {
            JOptionPane.showMessageDialog(this, "직원 정보가 성공적으로 등록되었습니다.", "등록 완료", JOptionPane.INFORMATION_MESSAGE);
            loadEmployeeTable("");
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "직원 등록에 실패했습니다. (주민번호 중복 등)", "등록 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedEmployee() {
        if(selectedEmployeeDbId == -1) {
            JOptionPane.showMessageDialog(this, "삭제할 직원을 테이블에서 선택해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "'" + nameField.getText() + "' 직원의 모든 정보를 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if(confirm == JOptionPane.YES_OPTION) {
            if(payrollManager.deleteEmployee(selectedEmployeeDbId)) {
                JOptionPane.showMessageDialog(this, "직원 정보가 삭제되었습니다.", "삭제 완료", JOptionPane.INFORMATION_MESSAGE);
                loadEmployeeTable("");
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "직원 정보 삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createContract() {
        if (selectedEmployeeDbId == -1) {
            JOptionPane.showMessageDialog(this, "근로계약서를 작성할 직원을 선택해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Employee emp = getEmployeeFromForm();
        Payroll payroll = getPayrollFromForm();

        payrollApp.showEmploymentContractPage(emp, payroll);
    }

    private Employee getEmployeeFromForm() {
        LocalDate hire = null, salaryChange = null;
        try { hire = LocalDate.parse(hireDateField.getText(), dateFormatter); } catch (Exception e) {}
        try { salaryChange = LocalDate.parse(salaryChangeDateField.getText(), dateFormatter); } catch (Exception e) {}

        Employee emp = new Employee(
                nameField.getText(),
                residentRegNoField.getText(),
                phoneField.getText(),
                parseFormattedNumber(annualSalaryField.getText()),
                addressField.getText(),
                hire, salaryChange,
                (String)departmentComboBox.getSelectedItem(),
                workLocationField.getText(),
                siteLocationField.getText()
        );
        if(selectedEmployeeDbId != -1) emp.setId(selectedEmployeeDbId);
        return emp;
    }

    private Payroll getPayrollFromForm() {
        recalculateSalaryComponents();
        return new Payroll(
                null,
                parseFormattedNumber(monthlyBasicSalaryField.getText()),
                parseFormattedNumber(bonusField.getText()),
                parseFormattedNumber(fixedOvertimeAllowanceField.getText()),
                parseFormattedNumber(otherAllowanceField.getText()),
                parseFormattedNumber(mealAllowanceField.getText()),
                parseFormattedNumber(vehicleMaintenanceFeeField.getText()),
                parseFormattedNumber(researchDevelopmentExpenseField.getText()),
                parseFormattedNumber(childcareAllowanceField.getText())
        );
    }

    private Employee getEmployeeFromTableRow(int modelRow) {
        Employee originalEmp = displayedEmployeesInTableOrder.get(modelRow);
        LocalDate hire = null;
        try {
            Object dateObj = tableModel.getValueAt(modelRow, COL_HIRE_DATE);
            if (dateObj != null && !dateObj.toString().isEmpty()) {
                hire = LocalDate.parse(dateObj.toString(), dateFormatter);
            }
        } catch (Exception e) {}

        Employee emp = new Employee(
                tableModel.getValueAt(modelRow, COL_NAME).toString(),
                tableModel.getValueAt(modelRow, COL_RESIDENT_REG_NO).toString(),
                tableModel.getValueAt(modelRow, COL_PHONE).toString(),
                parseValueToBigDecimal(tableModel.getValueAt(modelRow, COL_ANNUAL_SALARY)),
                tableModel.getValueAt(modelRow, COL_ADDRESS).toString(),
                hire,
                originalEmp.getSalaryChangeDate(),
                tableModel.getValueAt(modelRow, COL_DEPARTMENT).toString(),
                originalEmp.getWorkLocation(),
                originalEmp.getSiteLocation()
        );
        emp.setId(originalEmp.getId());
        return emp;
    }

    private Payroll getPayrollFromTableRow(int modelRow) {
        Payroll payroll = new Payroll();
        payroll.setMonthlyBasicSalary(parseValueToBigDecimal(tableModel.getValueAt(modelRow, COL_MONTHLY_BASIC)));
        payroll.setFixedOvertimeAllowance(parseValueToBigDecimal(tableModel.getValueAt(modelRow, COL_FIXED_OVERTIME)));
        payroll.setBonus(parseValueToBigDecimal(tableModel.getValueAt(modelRow, COL_BONUS)));
        payroll.setOtherAllowance(parseValueToBigDecimal(tableModel.getValueAt(modelRow, COL_OTHER_ALLOWANCE)));
        payroll.setMealAllowance(parseValueToBigDecimal(tableModel.getValueAt(modelRow, COL_MEAL_ALLOWANCE)));
        payroll.setVehicleMaintenanceFee(parseValueToBigDecimal(tableModel.getValueAt(modelRow, COL_VEHICLE_FEE)));
        payroll.setResearchDevelopmentExpense(parseValueToBigDecimal(tableModel.getValueAt(modelRow, COL_RESEARCH_EXPENSE)));
        payroll.setChildcareAllowance(parseValueToBigDecimal(tableModel.getValueAt(modelRow, COL_CHILDCARE_ALLOWANCE)));
        return payroll;
    }

    class TotalsAwareNumberRenderer extends DefaultTableCellRenderer {
        private final Font normalFont;
        private final Font totalsFont;
        private final int[] numericColumnIndices;
        private final DecimalFormat formatter = new DecimalFormat("#,###");

        public TotalsAwareNumberRenderer(Font normalFont, Font totalsFont, int[] numericColumnIndices) {
            this.normalFont = normalFont;
            this.totalsFont = totalsFont;
            this.numericColumnIndices = numericColumnIndices;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            boolean isTotalsRow = table.getValueAt(row, COL_NO) != null && "합계".equals(table.getValueAt(row, COL_NO).toString());

            c.setFont(isTotalsRow ? totalsFont : normalFont);
            c.setBackground(isSelected ? table.getSelectionBackground() : (isTotalsRow ? new Color(230, 240, 255) : Color.WHITE));

            boolean isNumeric = false;
            for(int idx : numericColumnIndices) {
                if(column == idx) {
                    isNumeric = true;
                    break;
                }
            }

            if(isNumeric) {
                setHorizontalAlignment(JLabel.RIGHT);
                if(value instanceof BigDecimal) {
                    setText(formatter.format(value));
                } else if (value instanceof Number) {
                    setText(formatter.format(value));
                } else {
                    setText(value != null ? value.toString() : "");
                }
            } else {
                setHorizontalAlignment(JLabel.CENTER);
                setText(value != null ? value.toString() : "");
            }

            return c;
        }
    }
}
