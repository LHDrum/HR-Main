package main;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.JOptionPane;
import java.io.File;

public class PayrollManager {
    private static final String DB_APP_FOLDER_NAME = ".HR_Payroll_App_Data";
    private static final String DB_FILE_NAME = "payroll_hr_db";
    private static final String DB_FOLDER_PATH = System.getProperty("user.home") + File.separator + DB_APP_FOLDER_NAME;
    private static final String DB_URL = "jdbc:h2:file:" + DB_FOLDER_PATH + File.separator + DB_FILE_NAME + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static final String PAYROLL_EMPLOYEE_SELECT_FIELDS =
            "p.id as payroll_id, p.employee_id, p.payment_year, p.payment_month, " +
                    "p.monthly_basic_salary, p.bonus, p.fixed_overtime_allowance, p.additional_overtime_premium, " +
                    "p.other_allowance, p.meal_allowance, p.vehicle_maintenance_fee, p.research_development_expense, p.childcare_allowance, " +
                    "p.unpaid_days, p.unauthorized_absence_days, p.national_pension_employee, p.health_insurance_employee, " +
                    "p.long_term_care_insurance_employee, p.employment_insurance_employee, p.income_tax, p.local_income_tax, " +
                    "p.total_employee_deduction, p.national_pension_employer, p.health_insurance_employer, p.long_term_care_insurance_employer, " +
                    "p.employment_insurance_employer, p.industrial_accident_insurance_employer, p.net_pay, " +
                    "e.id as employee_db_id, e.name, e.resident_registration_number, e.phone_number, e.annual_salary, " +
                    "e.address, e.hire_date, e.salary_change_date, e.department, e.work_location, e.site_location";


    public PayrollManager() {
        try {
            File dbAppFolder = new File(DB_FOLDER_PATH);
            if (!dbAppFolder.exists()) {
                if (!dbAppFolder.mkdirs()) {
                    JOptionPane.showMessageDialog(null, "데이터 저장 폴더 생성에 실패했습니다. 프로그램을 종료합니다.", "치명적 오류", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
            Class.forName("org.h2.Driver");
            try (Connection conn = getConnection()) {
                setupDatabase();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "데이터베이스 초기화 중 심각한 오류가 발생했습니다: " + e.getMessage(), "DB 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public void setupDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            String createEmployeesTableSQL = "CREATE TABLE IF NOT EXISTS employees (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "resident_registration_number VARCHAR(20) UNIQUE NOT NULL, " +
                    "phone_number VARCHAR(20), " +
                    "annual_salary DECIMAL(19, 4) NOT NULL, " +
                    "address VARCHAR(255) DEFAULT NULL, " +
                    "hire_date DATE DEFAULT NULL, " +
                    "salary_change_date DATE DEFAULT NULL, " +
                    "department VARCHAR(50) DEFAULT NULL, " +
                    "work_location VARCHAR(255) DEFAULT NULL, " +
                    "site_location VARCHAR(255) DEFAULT NULL);";
            stmt.executeUpdate(createEmployeesTableSQL);

            String createPayrollsTableSQL = "CREATE TABLE IF NOT EXISTS payrolls (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, employee_id INT NOT NULL, " +
                    "payment_year INT, payment_month INT, " +
                    "monthly_basic_salary DECIMAL(19, 4) DEFAULT 0, " +
                    "bonus DECIMAL(19, 4) DEFAULT 0, " +
                    "fixed_overtime_allowance DECIMAL(19, 4) DEFAULT 0, " +
                    "additional_overtime_premium DECIMAL(19, 4) DEFAULT 0, " +
                    "other_allowance DECIMAL(19, 4) DEFAULT 0, " +
                    "meal_allowance DECIMAL(19, 4) DEFAULT 0, " +
                    "vehicle_maintenance_fee DECIMAL(19, 4) DEFAULT 0, " +
                    "research_development_expense DECIMAL(19, 4) DEFAULT 0, " +
                    "childcare_allowance DECIMAL(19, 4) DEFAULT 0, " +
                    "unpaid_days INT DEFAULT 0, unauthorized_absence_days INT DEFAULT 0, " +
                    "national_pension_employee DECIMAL(19, 4) DEFAULT 0.0, " +
                    "health_insurance_employee DECIMAL(19, 4) DEFAULT 0.0, " +
                    "long_term_care_insurance_employee DECIMAL(19, 4) DEFAULT 0.0, " +
                    "employment_insurance_employee DECIMAL(19, 4) DEFAULT 0.0, " +
                    "income_tax DECIMAL(19, 4) DEFAULT 0.0, " +
                    "local_income_tax DECIMAL(19, 4) DEFAULT 0.0, " +
                    "total_employee_deduction DECIMAL(19, 4) DEFAULT 0.0, " +
                    "national_pension_employer DECIMAL(19, 4) DEFAULT 0.0, " +
                    "health_insurance_employer DECIMAL(19, 4) DEFAULT 0.0, " +
                    "long_term_care_insurance_employer DECIMAL(19, 4) DEFAULT 0.0, " +
                    "employment_insurance_employer DECIMAL(19, 4) DEFAULT 0.0, " +
                    "industrial_accident_insurance_employer DECIMAL(19, 4) DEFAULT 0.0, " +
                    "net_pay DECIMAL(19, 4) DEFAULT 0.0, " +
                    "FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE, " +
                    "CONSTRAINT uk_employee_period UNIQUE (employee_id, payment_year, payment_month)" +
                    ");";
            stmt.executeUpdate(createPayrollsTableSQL);
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS work_records (id INT PRIMARY KEY AUTO_INCREMENT, employee_id INT NOT NULL, work_date DATE NOT NULL, start_time TIME NULL, end_time TIME NULL, work_status VARCHAR(20) NOT NULL, FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE, CONSTRAINT uk_employee_work_date UNIQUE (employee_id, work_date));");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS app_settings (setting_key VARCHAR(255) PRIMARY KEY, setting_value VARCHAR(255) NOT NULL);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS annual_leaves (id INT PRIMARY KEY AUTO_INCREMENT, employee_id INT NOT NULL, leave_year INT NOT NULL, total_generated_days DECIMAL(10,2) DEFAULT 0.0, adjustment_days DECIMAL(10,2) DEFAULT 0.0, notes VARCHAR(255), FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE, CONSTRAINT uk_employee_leave_year UNIQUE (employee_id, leave_year));");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS leave_usage_records (id INT PRIMARY KEY AUTO_INCREMENT, employee_id INT NOT NULL, leave_date DATE NOT NULL, leave_type VARCHAR(50) NOT NULL, deduct_days DECIMAL(10,2) DEFAULT 1.0, FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE, CONSTRAINT uk_employee_leave_date UNIQUE (employee_id, leave_date));");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean addEmployee(Employee employee, Payroll payroll) {
        String employeeSql = "INSERT INTO employees (name, resident_registration_number, phone_number, annual_salary, address, hire_date, salary_change_date, department, work_location, site_location) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String payrollSql = "INSERT INTO payrolls (employee_id, monthly_basic_salary, fixed_overtime_allowance, bonus, other_allowance, meal_allowance, vehicle_maintenance_fee, research_development_expense, childcare_allowance) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement empPstmt = conn.prepareStatement(employeeSql, Statement.RETURN_GENERATED_KEYS)) {
                empPstmt.setString(1, employee.getName());
                empPstmt.setString(2, employee.getResidentRegistrationNumber());
                empPstmt.setString(3, employee.getPhoneNumber());
                empPstmt.setBigDecimal(4, employee.getAnnualSalary());
                empPstmt.setString(5, employee.getAddress());
                empPstmt.setDate(6, employee.getHireDate() != null ? Date.valueOf(employee.getHireDate()) : null);
                empPstmt.setDate(7, employee.getSalaryChangeDate() != null ? Date.valueOf(employee.getSalaryChangeDate()) : null);
                empPstmt.setString(8, employee.getDepartment());
                empPstmt.setString(9, employee.getWorkLocation());
                empPstmt.setString(10, employee.getSiteLocation());
                empPstmt.executeUpdate();

                try (ResultSet generatedKeys = empPstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int employeeId = generatedKeys.getInt(1);
                        try (PreparedStatement payrollPstmt = conn.prepareStatement(payrollSql)) {
                            payrollPstmt.setInt(1, employeeId);
                            payrollPstmt.setBigDecimal(2, payroll.getMonthlyBasicSalary());
                            payrollPstmt.setBigDecimal(3, payroll.getFixedOvertimeAllowance());
                            payrollPstmt.setBigDecimal(4, payroll.getBonus());
                            payrollPstmt.setBigDecimal(5, payroll.getOtherAllowance());
                            payrollPstmt.setBigDecimal(6, payroll.getMealAllowance());
                            payrollPstmt.setBigDecimal(7, payroll.getVehicleMaintenanceFee());
                            payrollPstmt.setBigDecimal(8, payroll.getResearchDevelopmentExpense());
                            payrollPstmt.setBigDecimal(9, payroll.getChildcareAllowance());
                            payrollPstmt.executeUpdate();
                        }
                    } else {
                        throw new SQLException("직원 ID를 생성하지 못했습니다.");
                    }
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateEmployee(Employee employee, Payroll payroll) {
        String employeeSql = "UPDATE employees SET name=?, resident_registration_number=?, phone_number=?, annual_salary=?, address=?, hire_date=?, salary_change_date=?, department=?, work_location=?, site_location=? WHERE id=?";
        String payrollSql = "UPDATE payrolls SET monthly_basic_salary=?, fixed_overtime_allowance=?, bonus=?, other_allowance=?, meal_allowance=?, vehicle_maintenance_fee=?, research_development_expense=?, childcare_allowance=? WHERE employee_id=? AND payment_year IS NULL AND payment_month IS NULL";
        String checkPayrollSql = "SELECT 1 FROM payrolls WHERE employee_id=? AND payment_year IS NULL AND payment_month IS NULL";
        String insertPayrollSql = "INSERT INTO payrolls (employee_id, monthly_basic_salary, fixed_overtime_allowance, bonus, other_allowance, meal_allowance, vehicle_maintenance_fee, research_development_expense, childcare_allowance) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement empPstmt = conn.prepareStatement(employeeSql)) {
                empPstmt.setString(1, employee.getName());
                empPstmt.setString(2, employee.getResidentRegistrationNumber());
                empPstmt.setString(3, employee.getPhoneNumber());
                empPstmt.setBigDecimal(4, employee.getAnnualSalary());
                empPstmt.setString(5, employee.getAddress());
                empPstmt.setDate(6, employee.getHireDate() != null ? Date.valueOf(employee.getHireDate()) : null);
                empPstmt.setDate(7, employee.getSalaryChangeDate() != null ? Date.valueOf(employee.getSalaryChangeDate()) : null);
                empPstmt.setString(8, employee.getDepartment());
                empPstmt.setString(9, employee.getWorkLocation());
                empPstmt.setString(10, employee.getSiteLocation());
                empPstmt.setInt(11, employee.getId());
                empPstmt.executeUpdate();
            }

            try (PreparedStatement checkStmt = conn.prepareStatement(checkPayrollSql)) {
                checkStmt.setInt(1, employee.getId());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    try (PreparedStatement payrollPstmt = conn.prepareStatement(payrollSql)) {
                        payrollPstmt.setBigDecimal(1, payroll.getMonthlyBasicSalary());
                        payrollPstmt.setBigDecimal(2, payroll.getFixedOvertimeAllowance());
                        payrollPstmt.setBigDecimal(3, payroll.getBonus());
                        payrollPstmt.setBigDecimal(4, payroll.getOtherAllowance());
                        payrollPstmt.setBigDecimal(5, payroll.getMealAllowance());
                        payrollPstmt.setBigDecimal(6, payroll.getVehicleMaintenanceFee());
                        payrollPstmt.setBigDecimal(7, payroll.getResearchDevelopmentExpense());
                        payrollPstmt.setBigDecimal(8, payroll.getChildcareAllowance());
                        payrollPstmt.setInt(9, employee.getId());
                        payrollPstmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertPayrollSql)) {
                        insertStmt.setInt(1, employee.getId());
                        insertStmt.setBigDecimal(2, payroll.getMonthlyBasicSalary());
                        insertStmt.setBigDecimal(3, payroll.getFixedOvertimeAllowance());
                        insertStmt.setBigDecimal(4, payroll.getBonus());
                        insertStmt.setBigDecimal(5, payroll.getOtherAllowance());
                        insertStmt.setBigDecimal(6, payroll.getMealAllowance());
                        insertStmt.setBigDecimal(7, payroll.getVehicleMaintenanceFee());
                        insertStmt.setBigDecimal(8, payroll.getResearchDevelopmentExpense());
                        insertStmt.setBigDecimal(9, payroll.getChildcareAllowance());
                        insertStmt.executeUpdate();
                    }
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteEmployee(int employeeId) {
        String sql = "DELETE FROM employees WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();
        // [수정] SELECT * 대신 별칭을 사용하도록 명시적 컬럼 지정
        String sql = "SELECT id as employee_db_id, name, resident_registration_number, phone_number, annual_salary, address, hire_date, salary_change_date, department, work_location, site_location FROM employees ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                employees.add(mapRowToEmployee(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return employees;
    }

    public Optional<Employee> getEmployeeByName(String name) {
        // [수정] SELECT * 대신 별칭을 사용하도록 명시적 컬럼 지정
        String sql = "SELECT id as employee_db_id, name, resident_registration_number, phone_number, annual_salary, address, hire_date, salary_change_date, department, work_location, site_location FROM employees WHERE name = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToEmployee(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Payroll> getContractualPayroll(int employeeId) {
        String sql = "SELECT " + PAYROLL_EMPLOYEE_SELECT_FIELDS + " FROM payrolls p JOIN employees e ON p.employee_id = e.id WHERE p.employee_id = ? AND p.payment_year IS NULL AND p.payment_month IS NULL";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPayroll(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Payroll> getPayrollsForPeriod(int year, int month) {
        List<Payroll> payrolls = new ArrayList<>();
        String sql = "SELECT " + PAYROLL_EMPLOYEE_SELECT_FIELDS + " FROM payrolls p JOIN employees e ON p.employee_id = e.id WHERE p.payment_year = ? AND p.payment_month = ? ORDER BY e.name";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payrolls.add(mapRowToPayroll(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payrolls;
    }

    public boolean deletePayrollForPeriod(int employeeId, int year, int month) {
        String sql = "DELETE FROM payrolls WHERE employee_id = ? AND payment_year = ? AND payment_month = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, year);
            pstmt.setInt(3, month);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void finalizeMonthlyPayAndDeductions(int employeeId, YearMonth period,
                                                Payroll payroll, BigDecimal industrialAccidentRate, int dependents) {
        DeductionResult result = DeductionCalculator.calculate(payroll.getGrossPay(), industrialAccidentRate, dependents);

        String sqlSelect = "SELECT id FROM payrolls WHERE employee_id = ? AND payment_year = ? AND payment_month = ?";
        String sqlUpdate = "UPDATE payrolls SET monthly_basic_salary=?, bonus=?, fixed_overtime_allowance=?, additional_overtime_premium=?, other_allowance=?, meal_allowance=?, vehicle_maintenance_fee=?, research_development_expense=?, childcare_allowance=?, unpaid_days=?, unauthorized_absence_days=?, national_pension_employee=?, health_insurance_employee=?, long_term_care_insurance_employee=?, employment_insurance_employee=?, income_tax=?, local_income_tax=?, total_employee_deduction=?, national_pension_employer=?, health_insurance_employer=?, long_term_care_insurance_employer=?, employment_insurance_employer=?, industrial_accident_insurance_employer=?, net_pay=? WHERE id = ?";
        String sqlInsert = "INSERT INTO payrolls (employee_id, payment_year, payment_month, monthly_basic_salary, bonus, fixed_overtime_allowance, additional_overtime_premium, other_allowance, meal_allowance, vehicle_maintenance_fee, research_development_expense, childcare_allowance, unpaid_days, unauthorized_absence_days, national_pension_employee, health_insurance_employee, long_term_care_insurance_employee, employment_insurance_employee, income_tax, local_income_tax, total_employee_deduction, national_pension_employer, health_insurance_employer, long_term_care_insurance_employer, employment_insurance_employer, industrial_accident_insurance_employer, net_pay) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            PreparedStatement psSelect = conn.prepareStatement(sqlSelect);
            psSelect.setInt(1, employeeId);
            psSelect.setInt(2, period.getYear());
            psSelect.setInt(3, period.getMonthValue());
            ResultSet rs = psSelect.executeQuery();

            if (rs.next()) {
                int payrollId = rs.getInt("id");
                try(PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                    setAllPayrollParams(psUpdate, payroll, result);
                    psUpdate.setInt(25, payrollId);
                    psUpdate.executeUpdate();
                }
            } else {
                try(PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
                    psInsert.setInt(1, employeeId);
                    psInsert.setInt(2, period.getYear());
                    psInsert.setInt(3, period.getMonthValue());
                    setAllPayrollParams(psInsert, payroll, result);
                    psInsert.executeUpdate();
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "급여 정보 저장 실패: " + e.getMessage(), "DB 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setAllPayrollParams(PreparedStatement pstmt, Payroll p, DeductionResult d) throws SQLException {
        int i = pstmt.getParameterMetaData().getParameterCount() > 25 ? 4 : 1;

        pstmt.setBigDecimal(i++, p.getMonthlyBasicSalary());
        pstmt.setBigDecimal(i++, p.getBonus());
        pstmt.setBigDecimal(i++, p.getFixedOvertimeAllowance());
        pstmt.setBigDecimal(i++, p.getAdditionalOvertimePremium());
        pstmt.setBigDecimal(i++, p.getOtherAllowance());
        pstmt.setBigDecimal(i++, p.getMealAllowance());
        pstmt.setBigDecimal(i++, p.getVehicleMaintenanceFee());
        pstmt.setBigDecimal(i++, p.getResearchDevelopmentExpense());
        pstmt.setBigDecimal(i++, p.getChildcareAllowance());
        pstmt.setInt(i++, p.getUnpaidDays());
        pstmt.setInt(i++, p.getUnauthorizedAbsenceDays());
        pstmt.setBigDecimal(i++, d.nationalPensionEmployee);
        pstmt.setBigDecimal(i++, d.healthInsuranceEmployee);
        pstmt.setBigDecimal(i++, d.longTermCareInsuranceEmployee);
        pstmt.setBigDecimal(i++, d.employmentInsuranceEmployee);
        pstmt.setBigDecimal(i++, d.incomeTax);
        pstmt.setBigDecimal(i++, d.localIncomeTax);
        pstmt.setBigDecimal(i++, d.totalEmployeeDeduction);
        pstmt.setBigDecimal(i++, d.nationalPensionEmployer);
        pstmt.setBigDecimal(i++, d.healthInsuranceEmployer);
        pstmt.setBigDecimal(i++, d.longTermCareInsuranceEmployer);
        pstmt.setBigDecimal(i++, d.employmentInsuranceEmployer);
        pstmt.setBigDecimal(i++, d.industrialAccidentInsuranceEmployer);
        pstmt.setBigDecimal(i++, d.netPay);
    }

    public Map<String, String> loadSettings() {
        Map<String, String> settings = new HashMap<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM app_settings")) {
            while (rs.next()) {
                settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return settings;
    }

    public void saveSettings(Map<String, String> settings) {
        String sql = "MERGE INTO app_settings (setting_key, setting_value) KEY(setting_key) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                pstmt.setString(1, entry.getKey());
                pstmt.setString(2, entry.getValue());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, BigDecimal> getAnnualLeaveSummary(int employeeId, int year) {
        Map<String, BigDecimal> summary = new HashMap<>();
        summary.put("generated", BigDecimal.ZERO);
        summary.put("adjustment", BigDecimal.ZERO);
        summary.put("used", BigDecimal.ZERO);

        String sqlLeave = "SELECT total_generated_days, adjustment_days FROM annual_leaves WHERE employee_id = ? AND leave_year = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlLeave)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, year);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                summary.put("generated", rs.getBigDecimal("total_generated_days"));
                summary.put("adjustment", rs.getBigDecimal("adjustment_days"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String sqlUsage = "SELECT SUM(deduct_days) as total_used FROM leave_usage_records WHERE employee_id = ? AND EXTRACT(YEAR FROM leave_date) = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlUsage)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, year);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                BigDecimal totalUsed = rs.getBigDecimal("total_used");
                summary.put("used", totalUsed != null ? totalUsed : BigDecimal.ZERO);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }

    public void applyLeaveAdjustment(int employeeId, int year, BigDecimal days, String type) {
        String sql = "MERGE INTO annual_leaves (employee_id, leave_year, adjustment_days, notes) KEY(employee_id, leave_year) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            Map<String, BigDecimal> summary = getAnnualLeaveSummary(employeeId, year);
            BigDecimal currentAdjustment = summary.getOrDefault("adjustment", BigDecimal.ZERO);
            BigDecimal newAdjustment = currentAdjustment.add(days);

            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, year);
            pstmt.setBigDecimal(3, newAdjustment);
            pstmt.setString(4, type + " " + days + "일 조정");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setTotalLeaveManually(int employeeId, int year, BigDecimal totalDays, String notes) {
        String sql = "MERGE INTO annual_leaves (employee_id, leave_year, total_generated_days, adjustment_days, notes) KEY(employee_id, leave_year) VALUES (?, ?, ?, 0, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, year);
            pstmt.setBigDecimal(3, totalDays);
            pstmt.setString(4, notes);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<LocalDate> getLeaveUsageRecords(int employeeId, int year) {
        List<LocalDate> usedDates = new ArrayList<>();
        String sql = "SELECT leave_date FROM leave_usage_records WHERE employee_id = ? AND EXTRACT(YEAR FROM leave_date) = ? ORDER BY leave_date";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, year);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                usedDates.add(rs.getDate("leave_date").toLocalDate());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usedDates;
    }

    public boolean saveLeaveUsage(int employeeId, List<LocalDate> dates) {
        String sql = "MERGE INTO leave_usage_records (employee_id, leave_date, leave_type, deduct_days) KEY(employee_id, leave_date) VALUES (?, ?, '연차', 1.0)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for(LocalDate date : dates) {
                pstmt.setInt(1, employeeId);
                pstmt.setDate(2, Date.valueOf(date));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void calculateAndGrantAnnualLeave(Employee employee, int year) {
        LocalDate hireDate = employee.getHireDate();
        if (hireDate == null) return;

        Map<String, String> settings = loadSettings();
        String basis = settings.getOrDefault("annualLeaveBasis", "FISCAL");
        BigDecimal totalGeneratedDays = BigDecimal.ZERO;

        if ("HIRE_DATE".equals(basis)) {
            long yearsOfService = ChronoUnit.YEARS.between(hireDate, LocalDate.of(year, 1, 1));
            if (yearsOfService < 1) {
                long monthsOfService = ChronoUnit.MONTHS.between(hireDate, LocalDate.of(year, 12, 31).plusDays(1));
                totalGeneratedDays = new BigDecimal(Math.min(monthsOfService, 11));
            } else {
                BigDecimal baseLeaveDays = new BigDecimal("15");
                BigDecimal additionalLeaveDays = new BigDecimal((yearsOfService - 1) / 2);
                totalGeneratedDays = baseLeaveDays.add(additionalLeaveDays).min(new BigDecimal("25"));
            }
        } else { // FISCAL (회계연도 기준)
            long yearsOfServiceAsOfYearStart = ChronoUnit.YEARS.between(hireDate, LocalDate.of(year, 1, 1));

            if (hireDate.getYear() == year) { // 입사 당해년도
                long daysInYear = (long) (LocalDate.of(year, 1, 1).isLeapYear() ? 366 : 365);
                long daysWorked = ChronoUnit.DAYS.between(hireDate, LocalDate.of(year, 12, 31)) + 1;
                totalGeneratedDays = new BigDecimal("15").multiply(new BigDecimal(daysWorked)).divide(new BigDecimal(daysInYear), 2, RoundingMode.HALF_UP);
            } else {
                BigDecimal baseLeaveDays = new BigDecimal("15");
                BigDecimal additionalLeaveDays = new BigDecimal((yearsOfServiceAsOfYearStart) / 2);
                totalGeneratedDays = baseLeaveDays.add(additionalLeaveDays).min(new BigDecimal("25"));
            }
        }

        String sql = "MERGE INTO annual_leaves (employee_id, leave_year, total_generated_days) KEY(employee_id, leave_year) VALUES (?, ?, ?)";
        try(Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employee.getId());
            pstmt.setInt(2, year);
            pstmt.setBigDecimal(3, totalGeneratedDays);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    private Employee mapRowToEmployee(ResultSet rs) throws SQLException {
        Date hireDate = rs.getDate("hire_date");
        Date salaryChangeDate = rs.getDate("salary_change_date");

        return new Employee(
                rs.getInt("employee_db_id"),
                rs.getString("name"),
                rs.getString("resident_registration_number"),
                rs.getString("phone_number"),
                rs.getBigDecimal("annual_salary"),
                rs.getString("address"),
                hireDate != null ? hireDate.toLocalDate() : null,
                salaryChangeDate != null ? salaryChangeDate.toLocalDate() : null,
                rs.getString("department"),
                rs.getString("work_location"),
                rs.getString("site_location")
        );
    }

    private Payroll mapRowToPayroll(ResultSet rs) throws SQLException {
        Payroll payroll = new Payroll();
        payroll.setId(rs.getInt("payroll_id"));
        payroll.setEmployeeId(rs.getInt("employee_id"));
        // payment_year와 payment_month는 null일 수 있으므로 getObject 사용
        Integer paymentYear = rs.getObject("payment_year", Integer.class);
        Integer paymentMonth = rs.getObject("payment_month", Integer.class);
        payroll.setPaymentYear(paymentYear);
        payroll.setPaymentMonth(paymentMonth);

        payroll.setMonthlyBasicSalary(rs.getBigDecimal("monthly_basic_salary"));
        payroll.setBonus(rs.getBigDecimal("bonus"));
        payroll.setFixedOvertimeAllowance(rs.getBigDecimal("fixed_overtime_allowance"));
        payroll.setAdditionalOvertimePremium(rs.getBigDecimal("additional_overtime_premium"));
        payroll.setOtherAllowance(rs.getBigDecimal("other_allowance"));
        payroll.setMealAllowance(rs.getBigDecimal("meal_allowance"));
        payroll.setVehicleMaintenanceFee(rs.getBigDecimal("vehicle_maintenance_fee"));
        payroll.setResearchDevelopmentExpense(rs.getBigDecimal("research_development_expense"));
        payroll.setChildcareAllowance(rs.getBigDecimal("childcare_allowance"));
        payroll.setUnpaidDays(rs.getInt("unpaid_days"));
        payroll.setUnauthorizedAbsenceDays(rs.getInt("unauthorized_absence_days"));
        payroll.setNationalPensionEmployee(rs.getBigDecimal("national_pension_employee"));
        payroll.setHealthInsuranceEmployee(rs.getBigDecimal("health_insurance_employee"));
        payroll.setLongTermCareInsuranceEmployee(rs.getBigDecimal("long_term_care_insurance_employee"));
        payroll.setEmploymentInsuranceEmployee(rs.getBigDecimal("employment_insurance_employee"));
        payroll.setIncomeTax(rs.getBigDecimal("income_tax"));
        payroll.setLocalIncomeTax(rs.getBigDecimal("local_income_tax"));
        payroll.setTotalEmployeeDeduction(rs.getBigDecimal("total_employee_deduction"));
        payroll.setNationalPensionEmployer(rs.getBigDecimal("national_pension_employer"));
        payroll.setHealthInsuranceEmployer(rs.getBigDecimal("health_insurance_employer"));
        payroll.setLongTermCareInsuranceEmployer(rs.getBigDecimal("long_term_care_insurance_employer"));
        payroll.setEmploymentInsuranceEmployer(rs.getBigDecimal("employment_insurance_employer"));
        payroll.setIndustrialAccidentInsuranceEmployer(rs.getBigDecimal("industrial_accident_insurance_employer"));
        payroll.setNetPay(rs.getBigDecimal("net_pay"));
        payroll.setEmployee(mapRowToEmployee(rs));
        return payroll;
    }
}