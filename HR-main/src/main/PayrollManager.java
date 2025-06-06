package main;

import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JOptionPane;
import java.io.File; // File 클래스 import 추가

public class PayrollManager {
    // H2 데이터베이스 파일 경로 설정 (사용자 홈 디렉토리 하위)
    private static final String DB_APP_FOLDER_NAME = ".HR_Payroll_App_Data"; // 애플리케이션 데이터 폴더 이름
    private static final String DB_FILE_NAME = "payroll_hr_db"; // 데이터베이스 파일 이름 (경로 구분자 없이)
    private static final String DB_FOLDER_PATH = System.getProperty("user.home") + File.separator + DB_APP_FOLDER_NAME;
    // H2 연결 URL: jdbc:h2:file:[경로/파일명] -> 파일 기반 DB
    // AUTO_SERVER=TRUE 옵션은 여러 연결을 허용하거나, IDE에서 동시 접근 시 유용할 수 있습니다.
    // DB_CLOSE_DELAY=-1 옵션은 모든 연결이 닫힌 후에도 DB를 바로 닫지 않도록 하여 성능에 도움이 될 수 있습니다.
    private static final String DB_URL = "jdbc:h2:file:" + DB_FOLDER_PATH + File.separator + DB_FILE_NAME + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa"; // H2 기본 사용자
    private static final String DB_PASSWORD = ""; // H2 기본 비밀번호 없음 (또는 원하는 비밀번호 설정 가능)

    public PayrollManager() {
        try {
            // 데이터베이스 저장 폴더 생성 (없으면)
            File dbAppFolder = new File(DB_FOLDER_PATH);
            if (!dbAppFolder.exists()) {
                if (dbAppFolder.mkdirs()) {
                    System.out.println("애플리케이션 데이터 폴더 생성됨: " + DB_FOLDER_PATH);
                } else {
                    System.err.println("애플리케이션 데이터 폴더 생성 실패: " + DB_FOLDER_PATH);
                    JOptionPane.showMessageDialog(null,
                            "오류: 애플리케이션 데이터 폴더를 생성할 수 없습니다.\n" + DB_FOLDER_PATH + "\n프로그램을 종료합니다.",
                            "치명적 오류",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(1); // 프로그램 종료
                }
            }

            // H2 드라이버 로드 (최신 JDBC에서는 선택 사항일 수 있으나 명시적으로 포함)
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException cnfe) {
                System.err.println("H2 JDBC 드라이버를 찾을 수 없습니다. h2.jar 파일이 클래스패스에 있는지 확인하세요.");
                cnfe.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "오류: H2 데이터베이스 드라이버를 찾을 수 없습니다.\n프로그램 실행에 필요한 파일이 누락되었습니다.",
                        "드라이버 오류",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1); // 프로그램 종료
            }

            try (Connection conn = getConnection()) { // DriverManager.getConnection을 호출하여 DB 파일 생성 시도
                System.out.println("H2 데이터베이스에 성공적으로 연결되었습니다. 파일 위치: " + DB_FOLDER_PATH + File.separator + DB_FILE_NAME + ".mv.db");
                setupDatabase(); // 테이블 생성 또는 확인 로직 호출
            }

        } catch (SQLException e) {
            System.err.println("### 오류: H2 데이터베이스 연결 또는 초기화 실패 ###");
            System.err.println("오류 원인: " + e.getMessage());
            e.printStackTrace();

            JOptionPane.showMessageDialog(null,
                    "오류: 데이터베이스 초기화에 실패했습니다.\n프로그램을 정상적으로 사용할 수 없습니다.\n오류: " + e.getMessage(),
                    "데이터베이스 오류",
                    JOptionPane.ERROR_MESSAGE);
            // System.exit(1); // <<< 데이터베이스 오류 시 프로그램 종료 여부 결정 (주석 처리 시 UI는 계속 실행)
            System.out.println("### PayrollManager: 데이터베이스 연결/초기화 실패. UI는 계속 진행합니다 (DB 기능 제한됨). ###");
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public boolean addEmployee(Employee employee) {
        if (getEmployeeByResidentRegNo(employee.getResidentRegistrationNumber()).isPresent()) {
            System.err.println("주민등록번호가 중복되는 직원이 이미 존재합니다: " + employee.getResidentRegistrationNumber());
            return false;
        }
        String sql = "INSERT INTO employees (name, resident_registration_number, phone_number, annual_salary, address, hire_date, salary_change_date, department, work_location, site_location) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, employee.getName());
            pstmt.setString(2, employee.getResidentRegistrationNumber());
            pstmt.setString(3, employee.getPhoneNumber());
            pstmt.setInt(4, employee.getAnnualSalary());
            pstmt.setString(5, employee.getAddress());
            pstmt.setString(6, employee.getHireDate());
            pstmt.setString(7, employee.getSalaryChangeDate());
            pstmt.setString(8, employee.getDepartment());
            pstmt.setString(9, employee.getWorkLocation());
            pstmt.setString(10, employee.getSiteLocation());
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        employee.setId(rs.getInt(1));
                    }
                }
                System.out.println(employee.getName() + " 직원 정보가 DB에 추가되었습니다.");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("직원 추가 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Optional<Employee> getEmployeeById(int id) {
        String sql = "SELECT id, name, resident_registration_number, phone_number, annual_salary, address, hire_date, salary_change_date, department, work_location, site_location FROM employees WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Employee(rs.getInt("id"), rs.getString("name"), rs.getString("resident_registration_number"), rs.getString("phone_number"), rs.getInt("annual_salary"), rs.getString("address"), rs.getString("hire_date"), rs.getString("salary_change_date"), rs.getString("department"), rs.getString("work_location"), rs.getString("site_location")));
                }
            }
        } catch (SQLException e) {
            System.err.println("직원 ID 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Employee> getEmployeeByResidentRegNo(String residentRegNo) {
        String sql = "SELECT id, name, resident_registration_number, phone_number, annual_salary, address, hire_date, salary_change_date, department, work_location, site_location FROM employees WHERE resident_registration_number = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, residentRegNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Employee(rs.getInt("id"), rs.getString("name"), rs.getString("resident_registration_number"), rs.getString("phone_number"), rs.getInt("annual_salary"), rs.getString("address"), rs.getString("hire_date"), rs.getString("salary_change_date"), rs.getString("department"), rs.getString("work_location"), rs.getString("site_location")));
                }
            }
        } catch (SQLException e) {
            System.err.println("주민번호 직원 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Employee> getEmployeeByName(String name) {
        String sql = "SELECT id, name, resident_registration_number, phone_number, annual_salary, address, hire_date, salary_change_date, department, work_location, site_location FROM employees WHERE name = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Employee(rs.getInt("id"), rs.getString("name"), rs.getString("resident_registration_number"), rs.getString("phone_number"), rs.getInt("annual_salary"), rs.getString("address"), rs.getString("hire_date"), rs.getString("salary_change_date"), rs.getString("department"), rs.getString("work_location"), rs.getString("site_location")));
                }
            }
        } catch (SQLException e) {
            System.err.println("직원 이름 '" + name + "' 조회 실패: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Employee> getAllEmployees() {
        List<Employee> employeeList = new ArrayList<>();
        String sql = "SELECT id, name, resident_registration_number, phone_number, annual_salary, address, hire_date, salary_change_date, department, work_location, site_location FROM employees";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                employeeList.add(new Employee(rs.getInt("id"), rs.getString("name"), rs.getString("resident_registration_number"), rs.getString("phone_number"), rs.getInt("annual_salary"), rs.getString("address"), rs.getString("hire_date"), rs.getString("salary_change_date"), rs.getString("department"), rs.getString("work_location"), rs.getString("site_location")));
            }
        } catch (SQLException e) {
            System.err.println("모든 직원 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return employeeList;
    }

    public boolean updateEmployeeDetails(Employee employee) {
        String sql = "UPDATE employees SET name = ?, resident_registration_number = ?, phone_number = ?, annual_salary = ?, address = ?, hire_date = ?, salary_change_date = ?, department = ?, work_location = ?, site_location = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, employee.getName());
            pstmt.setString(2, employee.getResidentRegistrationNumber());
            pstmt.setString(3, employee.getPhoneNumber());
            pstmt.setInt(4, employee.getAnnualSalary());
            pstmt.setString(5, employee.getAddress());
            pstmt.setString(6, employee.getHireDate());
            pstmt.setString(7, employee.getSalaryChangeDate());
            pstmt.setString(8, employee.getDepartment());
            pstmt.setString(9, employee.getWorkLocation());
            pstmt.setString(10, employee.getSiteLocation());
            pstmt.setInt(11, employee.getId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println(employee.getName() + "님의 직원 정보가 DB에서 업데이트되었습니다.");
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("직원 정보 업데이트 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteEmployeeById(int employeeId) {
        String sqlDeleteEmployee = "DELETE FROM employees WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlDeleteEmployee)) {
            pstmt.setInt(1, employeeId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("직원 ID " + employeeId + "의 정보가 DB에서 삭제되었습니다.");
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("직원 삭제 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePayroll(Employee employee, int monthlyBasicSalary, int bonus,
                                 int fixedOvertimeAllowance, int otherAllowance, int mealAllowance,
                                 int vehicleMaintenanceFee, int researchDevelopmentExpense, int childcareAllowance) {
        String sqlSelectPayroll = "SELECT id FROM payrolls WHERE employee_id = ? AND payment_year IS NULL AND payment_month IS NULL";
        String sqlUpdatePayroll = "UPDATE payrolls SET monthly_basic_salary = ?, bonus = ?, " +
                "fixed_overtime_allowance = ?, other_allowance = ?, meal_allowance = ?, " +
                "vehicle_maintenance_fee = ?, research_development_expense = ?, childcare_allowance = ?, " +
                "additional_overtime_premium = 0, unpaid_days = 0, unauthorized_absence_days = 0, weekly_holiday_allowance = 0.0 " +
                "WHERE employee_id = ? AND payment_year IS NULL AND payment_month IS NULL";
        String sqlInsertPayroll = "INSERT INTO payrolls (employee_id, monthly_basic_salary, bonus, " +
                "fixed_overtime_allowance, other_allowance, meal_allowance, " +
                "vehicle_maintenance_fee, research_development_expense, childcare_allowance, " +
                "additional_overtime_premium, unpaid_days, unauthorized_absence_days, weekly_holiday_allowance, payment_year, payment_month) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0.0, NULL, NULL)";

        try (Connection conn = getConnection()) {
            int affectedRows = 0;
            try (PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelectPayroll)) {
                pstmtSelect.setInt(1, employee.getId());
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    if (rs.next()) { // 기존 계약 급여 정보가 있으면 업데이트
                        try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdatePayroll)) {
                            pstmtUpdate.setInt(1, monthlyBasicSalary);
                            pstmtUpdate.setInt(2, bonus);
                            pstmtUpdate.setInt(3, fixedOvertimeAllowance);
                            pstmtUpdate.setInt(4, otherAllowance);
                            pstmtUpdate.setInt(5, mealAllowance);
                            pstmtUpdate.setInt(6, vehicleMaintenanceFee);
                            pstmtUpdate.setInt(7, researchDevelopmentExpense);
                            pstmtUpdate.setInt(8, childcareAllowance);
                            pstmtUpdate.setInt(9, employee.getId());
                            affectedRows = pstmtUpdate.executeUpdate();
                            if (affectedRows > 0)
                                System.out.println(employee.getName() + "님의 계약 급여 정보가 DB에서 업데이트되었습니다.");
                        }
                    } else { // 기존 계약 급여 정보가 없으면 새로 삽입
                        try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsertPayroll)) {
                            pstmtInsert.setInt(1, employee.getId());
                            pstmtInsert.setInt(2, monthlyBasicSalary);
                            pstmtInsert.setInt(3, bonus);
                            pstmtInsert.setInt(4, fixedOvertimeAllowance);
                            pstmtInsert.setInt(5, otherAllowance);
                            pstmtInsert.setInt(6, mealAllowance);
                            pstmtInsert.setInt(7, vehicleMaintenanceFee);
                            pstmtInsert.setInt(8, researchDevelopmentExpense);
                            pstmtInsert.setInt(9, childcareAllowance);
                            affectedRows = pstmtInsert.executeUpdate();
                            if (affectedRows > 0)
                                System.out.println(employee.getName() + "님의 새로운 계약 급여 정보가 DB에 생성되었습니다.");
                        }
                    }
                }
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("계약 급여 정보 업데이트/추가 실패 (직원 ID: " + employee.getId() + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Optional<Payroll> getContractualPayroll(int employeeId) {
        String sql = "SELECT p.id, p.employee_id, p.monthly_basic_salary, p.bonus, p.fixed_overtime_allowance, p.other_allowance, p.meal_allowance, p.vehicle_maintenance_fee, p.research_development_expense, p.childcare_allowance, p.additional_overtime_premium, p.unpaid_days, p.unauthorized_absence_days, p.weekly_holiday_allowance FROM payrolls p WHERE p.employee_id = ? AND p.payment_year IS NULL AND p.payment_month IS NULL";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Payroll payroll = new Payroll(rs.getInt("id"), rs.getInt("employee_id"), null, null, rs.getInt("monthly_basic_salary"), rs.getInt("bonus"), rs.getInt("fixed_overtime_allowance"), rs.getInt("other_allowance"), rs.getInt("meal_allowance"), rs.getInt("vehicle_maintenance_fee"), rs.getInt("research_development_expense"), rs.getInt("childcare_allowance"), rs.getInt("additional_overtime_premium"), rs.getInt("unpaid_days"), rs.getInt("unauthorized_absence_days"), rs.getDouble("weekly_holiday_allowance"));
                    return Optional.of(payroll);
                }
            }
        } catch (SQLException e) {
            System.err.println("직원 ID " + employeeId + " 계약 급여 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Payroll> getPayrollsForPeriod(int year, int month) {
        List<Payroll> payrollList = new ArrayList<>();
        String sql = "SELECT p.id, p.employee_id, p.payment_year, p.payment_month, p.monthly_basic_salary, p.bonus, p.fixed_overtime_allowance, p.other_allowance, p.meal_allowance, p.vehicle_maintenance_fee, p.research_development_expense, p.childcare_allowance, p.additional_overtime_premium, p.unpaid_days, p.unauthorized_absence_days, p.weekly_holiday_allowance, e.id AS emp_id, e.name, e.resident_registration_number, e.phone_number, e.annual_salary, e.address, e.hire_date, e.salary_change_date, e.department, e.work_location, e.site_location FROM payrolls p JOIN employees e ON p.employee_id = e.id WHERE p.payment_year = ? AND p.payment_month = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Employee employee = new Employee(rs.getInt("emp_id"), rs.getString("name"), rs.getString("resident_registration_number"), rs.getString("phone_number"), rs.getInt("annual_salary"), rs.getString("address"), rs.getString("hire_date"), rs.getString("salary_change_date"), rs.getString("department"), rs.getString("work_location"), rs.getString("site_location"));
                    Payroll payroll = new Payroll(rs.getInt("id"), rs.getInt("employee_id"), rs.getObject("payment_year") == null ? null : rs.getInt("payment_year"), rs.getObject("payment_month") == null ? null : rs.getInt("payment_month"), rs.getInt("monthly_basic_salary"), rs.getInt("bonus"), rs.getInt("fixed_overtime_allowance"), rs.getInt("other_allowance"), rs.getInt("meal_allowance"), rs.getInt("vehicle_maintenance_fee"), rs.getInt("research_development_expense"), rs.getInt("childcare_allowance"), rs.getInt("additional_overtime_premium"), rs.getInt("unpaid_days"), rs.getInt("unauthorized_absence_days"), rs.getDouble("weekly_holiday_allowance"));
                    payroll.setEmployee(employee);
                    payrollList.add(payroll);
                }
            }
        } catch (SQLException e) {
            System.err.println(year + "년 " + String.format("%02d", month) + "월 급여 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return payrollList;
    }

    public void finalizeMonthlyPay(int employeeId, YearMonth period,
                                   int unpaidDays, int unauthorizedAbsenceDays,
                                   double adjustedBasic,
                                   double adjustedContractualFixedOvertime,
                                   double additionalOvertimePremiumValue,
                                   double adjustedBonus,
                                   double adjustedOther, double adjustedMeal, double adjustedVehicle,
                                   double adjustedResearch, double adjustedChildcare) {
        String selectSql = "SELECT id FROM payrolls WHERE employee_id = ? AND payment_year = ? AND payment_month = ?";
        String updateSql = "UPDATE payrolls SET " +
                "monthly_basic_salary = ?, fixed_overtime_allowance = ?, additional_overtime_premium = ?, " +
                "bonus = ?, other_allowance = ?, meal_allowance = ?, vehicle_maintenance_fee = ?, " +
                "research_development_expense = ?, childcare_allowance = ?, unpaid_days = ?, " +
                "unauthorized_absence_days = ?, weekly_holiday_allowance = ? " + // weekly_holiday_allowance 추가
                "WHERE employee_id = ? AND payment_year = ? AND payment_month = ?";
        String insertSql = "INSERT INTO payrolls (employee_id, payment_year, payment_month, " +
                "monthly_basic_salary, fixed_overtime_allowance, additional_overtime_premium, bonus, " +
                "other_allowance, meal_allowance, vehicle_maintenance_fee, research_development_expense, " +
                "childcare_allowance, unpaid_days, unauthorized_absence_days, weekly_holiday_allowance) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // weekly_holiday_allowance 추가
        try (Connection conn = getConnection()) {
            try (PreparedStatement pstmtSelect = conn.prepareStatement(selectSql)) {
                pstmtSelect.setInt(1, employeeId);
                pstmtSelect.setInt(2, period.getYear());
                pstmtSelect.setInt(3, period.getMonthValue());
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    if (rs.next()) { // 해당 월의 급여 데이터가 이미 있으면 업데이트
                        try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateSql)) {
                            setPayrollStatementParameters(pstmtUpdate, employeeId, period, unpaidDays, unauthorizedAbsenceDays, adjustedBasic, adjustedContractualFixedOvertime, additionalOvertimePremiumValue, adjustedBonus, adjustedOther, adjustedMeal, adjustedVehicle, adjustedResearch, adjustedChildcare, true);
                            pstmtUpdate.executeUpdate();
                        }
                    } else { // 없으면 새로 삽입
                        try (PreparedStatement pstmtInsert = conn.prepareStatement(insertSql)) {
                            setPayrollStatementParameters(pstmtInsert, employeeId, period, unpaidDays, unauthorizedAbsenceDays, adjustedBasic, adjustedContractualFixedOvertime, additionalOvertimePremiumValue, adjustedBonus, adjustedOther, adjustedMeal, adjustedVehicle, adjustedResearch, adjustedChildcare, false);
                            pstmtInsert.executeUpdate();
                        }
                    }
                }
            }
            System.out.println("직원 ID " + employeeId + "의 " + period.toString() + " 최종 확정 급여 정보가 DB에 저장되었습니다.");
        } catch (SQLException e) {
            System.err.println("최종 확정 급여 정보 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setPayrollStatementParameters(PreparedStatement pstmt, int employeeId, YearMonth period,
                                               int unpaidDays, int unauthorizedAbsenceDays,
                                               double adjustedBasic, double adjustedContractualFixedOvertime,
                                               double additionalOvertimePremiumValue, double adjustedBonus,
                                               double adjustedOther, double adjustedMeal, double adjustedVehicle,
                                               double adjustedResearch, double adjustedChildcare, boolean isUpdate) throws SQLException {
        int paramIndex = 1;
        if (!isUpdate) {
            pstmt.setInt(paramIndex++, employeeId);
            pstmt.setInt(paramIndex++, period.getYear());
            pstmt.setInt(paramIndex++, period.getMonthValue());
        }
        pstmt.setInt(paramIndex++, (int) Math.round(adjustedBasic));
        pstmt.setInt(paramIndex++, (int) Math.round(adjustedContractualFixedOvertime));
        pstmt.setInt(paramIndex++, (int) Math.round(additionalOvertimePremiumValue));
        pstmt.setInt(paramIndex++, (int) Math.round(adjustedBonus));
        pstmt.setInt(paramIndex++, (int) Math.round(adjustedOther));
        pstmt.setInt(paramIndex++, (int) Math.round(adjustedMeal));
        pstmt.setInt(paramIndex++, (int) Math.round(adjustedVehicle));
        pstmt.setInt(paramIndex++, (int) Math.round(adjustedResearch));
        pstmt.setInt(paramIndex++, (int) Math.round(adjustedChildcare));
        pstmt.setInt(paramIndex++, unpaidDays);
        pstmt.setInt(paramIndex++, unauthorizedAbsenceDays);
        pstmt.setDouble(paramIndex++, 0.0); // weekly_holiday_allowance (이전 코드에서 0.0으로 고정되어 있었음)
        if (isUpdate) {
            pstmt.setInt(paramIndex++, employeeId);
            pstmt.setInt(paramIndex++, period.getYear());
            pstmt.setInt(paramIndex++, period.getMonthValue());
        }
    }

    public boolean deletePayrollForPeriod(int employeeId, int year, int month) {
        String sql = "DELETE FROM payrolls WHERE employee_id = ? AND payment_year = ? AND payment_month = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, year);
            pstmt.setInt(3, month);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("직원 ID " + employeeId + "의 " + year + "년 " + month + "월 급여 정보 삭제됨.");
                return true;
            } else {
                System.out.println("직원 ID " + employeeId + "의 " + year + "년 " + month + "월 급여 정보 없거나 삭제 안됨.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("직원 ID " + employeeId + "의 " + year + "년 " + month + "월 급여 정보 삭제 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void saveWorkRecords(int employeeId, YearMonth yearMonth, List<WorkLogEntry> records) {
        String deleteSql = "DELETE FROM work_records WHERE employee_id = ? AND YEAR(work_date) = ? AND MONTH(work_date) = ?";
        String insertSql = "INSERT INTO work_records (employee_id, work_date, start_time, end_time, work_status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // 트랜잭션 시작
            try (PreparedStatement pstmtDelete = conn.prepareStatement(deleteSql)) {
                pstmtDelete.setInt(1, employeeId);
                pstmtDelete.setInt(2, yearMonth.getYear());
                pstmtDelete.setInt(3, yearMonth.getMonthValue());
                pstmtDelete.executeUpdate();
            }
            try (PreparedStatement pstmtInsert = conn.prepareStatement(insertSql)) {
                for (WorkLogEntry record : records) {
                    pstmtInsert.setInt(1, record.getEmployeeId());
                    pstmtInsert.setDate(2, Date.valueOf(record.getWorkDate()));
                    pstmtInsert.setTime(3, record.getStartTime() == null ? null : Time.valueOf(record.getStartTime()));
                    pstmtInsert.setTime(4, record.getEndTime() == null ? null : Time.valueOf(record.getEndTime()));
                    pstmtInsert.setString(5, record.getStatus().name()); // Enum의 name()을 사용하여 문자열로 저장
                    pstmtInsert.addBatch();
                }
                pstmtInsert.executeBatch();
            }
            conn.commit(); // 트랜잭션 커밋
            System.out.println("직원 ID " + employeeId + "의 " + yearMonth.toString() + " 근무 기록 저장됨.");
        } catch (SQLException e) {
            System.err.println("근무 기록 저장 실패: " + e.getMessage());
            e.printStackTrace();
            try (Connection connForRollback = getConnection()) { // 롤백을 위한 새 연결 (또는 기존 conn 사용 가능 여부 확인)
                if (connForRollback != null && !connForRollback.getAutoCommit()) { // 오토커밋이 아닐 때만 롤백 시도
                    connForRollback.rollback(); // 트랜잭션 롤백
                    System.err.println("근무 기록 저장 실패로 인해 트랜잭션이 롤백되었습니다.");
                }
            } catch (SQLException ex) {
                System.err.println("롤백 중 오류 발생: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public List<WorkLogEntry> getWorkRecords(int employeeId, YearMonth yearMonth) {
        List<WorkLogEntry> records = new ArrayList<>();
        String sql = "SELECT id, employee_id, work_date, start_time, end_time, work_status FROM work_records WHERE employee_id = ? AND YEAR(work_date) = ? AND MONTH(work_date) = ? ORDER BY work_date ASC";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, yearMonth.getYear());
            pstmt.setInt(3, yearMonth.getMonthValue());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Time startTimeSql = rs.getTime("start_time");
                    Time endTimeSql = rs.getTime("end_time");
                    String statusStr = rs.getString("work_status");
                    AttendancePage.WorkStatus status = AttendancePage.WorkStatus.NORMAL; // 기본값
                    try {
                        if (statusStr != null && !statusStr.isEmpty()) {
                            status = AttendancePage.WorkStatus.valueOf(statusStr);
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("DB work_status 값('" + statusStr + "')을 AttendancePage.WorkStatus Enum으로 변환 중 오류 발생. 기본값(NORMAL)으로 설정합니다.");
                    }
                    WorkLogEntry record = new WorkLogEntry(
                            rs.getInt("id"),
                            rs.getInt("employee_id"),
                            rs.getDate("work_date").toLocalDate(),
                            startTimeSql == null ? null : startTimeSql.toLocalTime(),
                            endTimeSql == null ? null : endTimeSql.toLocalTime(),
                            status
                    );
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("근무 기록 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return records;
    }

    // PayrollManager.java의 setupDatabase() 메서드

    public void setupDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            String createEmployeesTableSQL = "CREATE TABLE IF NOT EXISTS employees (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "resident_registration_number VARCHAR(20) UNIQUE NOT NULL, " +
                    "phone_number VARCHAR(20), " +
                    "annual_salary INT NOT NULL, " +
                    "address VARCHAR(255) DEFAULT NULL, " +
                    "hire_date VARCHAR(10) DEFAULT NULL, " +
                    "salary_change_date VARCHAR(10) DEFAULT NULL, " +
                    "department VARCHAR(50) DEFAULT NULL, " +
                    "work_location VARCHAR(255) DEFAULT NULL, " +
                    "site_location VARCHAR(255) DEFAULT NULL" +
                    ");";
            stmt.executeUpdate(createEmployeesTableSQL);
            System.out.println("'employees' 테이블 준비 완료.");

            String createPayrollsTableSQL = "CREATE TABLE IF NOT EXISTS payrolls (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "employee_id INT NOT NULL, " +
                    "payment_year INT DEFAULT NULL, " +
                    "payment_month INT DEFAULT NULL, " +
                    "monthly_basic_salary INT NOT NULL, " +
                    "bonus INT NOT NULL, " +
                    "fixed_overtime_allowance INT DEFAULT 0, " +
                    "additional_overtime_premium INT DEFAULT 0, " +
                    "other_allowance INT DEFAULT 0, " +
                    "meal_allowance INT DEFAULT 0, " +
                    "vehicle_maintenance_fee INT DEFAULT 0, " +
                    "research_development_expense INT DEFAULT 0, " +
                    "childcare_allowance INT DEFAULT 0, " +
                    "unpaid_days INT DEFAULT 0, " +
                    "unauthorized_absence_days INT DEFAULT 0, " +
                    "weekly_holiday_allowance DOUBLE DEFAULT 0.0, " +
                    "FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE, " +
                    "CONSTRAINT uk_employee_period UNIQUE (employee_id, payment_year, payment_month)" + // 수정된 부분 (이전 답변 참고)
                    ");";
            stmt.executeUpdate(createPayrollsTableSQL);
            System.out.println("'payrolls' 테이블 준비 완료.");

            // ▼▼▼ 오류 발생 지점 수정 ▼▼▼
            // 이전에 'createWorkRecordsTableSQL' 변수가 이미 위에서 payrolls 테이블 생성 후 한 번 더 선언되었습니다.
            // employees, payrolls, work_records 순서로 테이블을 생성하는 것이 일반적이므로,
            // work_records 테이블 생성 SQL을 employees 테이블 생성 직후, 또는 payrolls 테이블 생성 직후로 옮기거나,
            // 변수 이름을 다르게 사용해야 합니다.
            // 여기서는 employees -> payrolls -> work_records 순서로 생성되도록 하고,
            // 이전에 실수로 중복 선언된 부분을 제거하거나 올바른 위치로 이동시켰다고 가정합니다.
            // (만약 아직 work_records 테이블 생성 코드가 없다면 아래와 같이 추가합니다.)

            // createWorkRecordsTableSQL 변수를 employees 테이블 생성 후 바로 선언 및 실행하거나,
            // 또는 payrolls 테이블 생성 후 선언 및 실행합니다.
            // 여기서는 payrolls 테이블 생성 후에 work_records 테이블을 생성하는 것으로 합니다.
            // 만약 위 코드에서 이미 createWorkRecordsTableSQL을 올바르게 선언하고 실행했다면,
            // 아래의 중복된 선언 및 실행 부분을 삭제해야 합니다.

            // 아래 코드는 이전 답변에서 work_records 테이블의 UNIQUE KEY 문법을 수정한 버전입니다.
            // 이 SQL 문이 setupDatabase() 메서드 내에 *한 번만* 정의되고 실행되어야 합니다.
            String createWorkRecordsTableSQL_corrected = "CREATE TABLE IF NOT EXISTS work_records (" + // 변수 이름을 약간 변경하거나, 이전 선언을 제거
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "employee_id INT NOT NULL, " +
                    "work_date DATE NOT NULL, " +
                    "start_time TIME NULL, " +
                    "end_time TIME NULL, " +
                    "work_status VARCHAR(20) NOT NULL, " +
                    "FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE, " +
                    "CONSTRAINT uk_employee_work_date UNIQUE (employee_id, work_date)" + // H2 호환 UNIQUE 제약조건
                    ");";
            stmt.executeUpdate(createWorkRecordsTableSQL_corrected); // 수정한 변수 이름으로 실행
            System.out.println("'work_records' 테이블 준비 완료.");
            // ▲▲▲ 오류 발생 지점 수정 ▲▲▲

        } catch (SQLException e) {
            System.err.println("DB 테이블 준비 중 오류: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "DB 테이블 준비 중 오류: " + e.getMessage(), "DB 오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}