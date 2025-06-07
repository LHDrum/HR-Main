package main;

import java.math.BigDecimal;

public class Payroll {
    private int id;
    private int employeeId;
    private Employee employee;

    private Integer paymentYear;
    private Integer paymentMonth;

    private BigDecimal monthlyBasicSalary = BigDecimal.ZERO;
    private BigDecimal bonus = BigDecimal.ZERO;
    private BigDecimal fixedOvertimeAllowance = BigDecimal.ZERO;
    private BigDecimal additionalOvertimePremium = BigDecimal.ZERO;
    private BigDecimal otherAllowance = BigDecimal.ZERO;
    private BigDecimal mealAllowance = BigDecimal.ZERO;
    private BigDecimal vehicleMaintenanceFee = BigDecimal.ZERO;
    private BigDecimal researchDevelopmentExpense = BigDecimal.ZERO;
    private BigDecimal childcareAllowance = BigDecimal.ZERO;

    private int unpaidDays;
    private int unauthorizedAbsenceDays;

    private BigDecimal nationalPensionEmployee = BigDecimal.ZERO;
    private BigDecimal healthInsuranceEmployee = BigDecimal.ZERO;
    private BigDecimal longTermCareInsuranceEmployee = BigDecimal.ZERO;
    private BigDecimal employmentInsuranceEmployee = BigDecimal.ZERO;
    private BigDecimal incomeTax = BigDecimal.ZERO;
    private BigDecimal localIncomeTax = BigDecimal.ZERO;
    private BigDecimal totalEmployeeDeduction = BigDecimal.ZERO;

    private BigDecimal nationalPensionEmployer = BigDecimal.ZERO;
    private BigDecimal healthInsuranceEmployer = BigDecimal.ZERO;
    private BigDecimal longTermCareInsuranceEmployer = BigDecimal.ZERO;
    private BigDecimal employmentInsuranceEmployer = BigDecimal.ZERO;
    private BigDecimal industrialAccidentInsuranceEmployer = BigDecimal.ZERO;

    private BigDecimal netPay = BigDecimal.ZERO;

    public Payroll() {}

    public Payroll(Employee employee, BigDecimal monthlyBasicSalary, BigDecimal bonus,
                   BigDecimal fixedOvertimeAllowance, BigDecimal otherAllowance, BigDecimal mealAllowance,
                   BigDecimal vehicleMaintenanceFee, BigDecimal researchDevelopmentExpense, BigDecimal childcareAllowance) {
        this.employee = employee;
        if (employee != null) {
            this.employeeId = employee.getId();
        }
        this.monthlyBasicSalary = monthlyBasicSalary;
        this.bonus = bonus;
        this.fixedOvertimeAllowance = fixedOvertimeAllowance;
        this.otherAllowance = otherAllowance;
        this.mealAllowance = mealAllowance;
        this.vehicleMaintenanceFee = vehicleMaintenanceFee;
        this.researchDevelopmentExpense = researchDevelopmentExpense;
        this.childcareAllowance = childcareAllowance;
    }

    /**
     * 세전 총 급여(과세/비과세 포함)를 계산하여 반환합니다.
     * @return 세전 총 급여
     */
    public BigDecimal getGrossPay() {
        return (monthlyBasicSalary != null ? monthlyBasicSalary : BigDecimal.ZERO)
                .add(bonus != null ? bonus : BigDecimal.ZERO)
                .add(fixedOvertimeAllowance != null ? fixedOvertimeAllowance : BigDecimal.ZERO)
                .add(additionalOvertimePremium != null ? additionalOvertimePremium : BigDecimal.ZERO)
                .add(otherAllowance != null ? otherAllowance : BigDecimal.ZERO)
                .add(mealAllowance != null ? mealAllowance : BigDecimal.ZERO)
                .add(vehicleMaintenanceFee != null ? vehicleMaintenanceFee : BigDecimal.ZERO)
                .add(researchDevelopmentExpense != null ? researchDevelopmentExpense : BigDecimal.ZERO)
                .add(childcareAllowance != null ? childcareAllowance : BigDecimal.ZERO);
    }

    /**
     * 사업주가 부담하는 4대 보험료의 총합을 계산하여 반환합니다.
     * @return 사업주 부담 보험료 총액
     */
    public BigDecimal getTotalEmployerDeduction() {
        return (nationalPensionEmployer != null ? nationalPensionEmployer : BigDecimal.ZERO)
                .add(healthInsuranceEmployer != null ? healthInsuranceEmployer : BigDecimal.ZERO)
                .add(longTermCareInsuranceEmployer != null ? longTermCareInsuranceEmployer : BigDecimal.ZERO)
                .add(employmentInsuranceEmployer != null ? employmentInsuranceEmployer : BigDecimal.ZERO)
                .add(industrialAccidentInsuranceEmployer != null ? industrialAccidentInsuranceEmployer : BigDecimal.ZERO);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public Integer getPaymentYear() { return paymentYear; }
    public void setPaymentYear(Integer paymentYear) { this.paymentYear = paymentYear; }
    public Integer getPaymentMonth() { return paymentMonth; }
    public void setPaymentMonth(Integer paymentMonth) { this.paymentMonth = paymentMonth; }
    public BigDecimal getMonthlyBasicSalary() { return monthlyBasicSalary; }
    public void setMonthlyBasicSalary(BigDecimal monthlyBasicSalary) { this.monthlyBasicSalary = monthlyBasicSalary; }
    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
    public BigDecimal getFixedOvertimeAllowance() { return fixedOvertimeAllowance; }
    public void setFixedOvertimeAllowance(BigDecimal fixedOvertimeAllowance) { this.fixedOvertimeAllowance = fixedOvertimeAllowance; }
    public BigDecimal getAdditionalOvertimePremium() { return additionalOvertimePremium; }
    public void setAdditionalOvertimePremium(BigDecimal additionalOvertimePremium) { this.additionalOvertimePremium = additionalOvertimePremium; }
    public BigDecimal getOtherAllowance() { return otherAllowance; }
    public void setOtherAllowance(BigDecimal otherAllowance) { this.otherAllowance = otherAllowance; }
    public BigDecimal getMealAllowance() { return mealAllowance; }
    public void setMealAllowance(BigDecimal mealAllowance) { this.mealAllowance = mealAllowance; }
    public BigDecimal getVehicleMaintenanceFee() { return vehicleMaintenanceFee; }
    public void setVehicleMaintenanceFee(BigDecimal vehicleMaintenanceFee) { this.vehicleMaintenanceFee = vehicleMaintenanceFee; }
    public BigDecimal getResearchDevelopmentExpense() { return researchDevelopmentExpense; }
    public void setResearchDevelopmentExpense(BigDecimal researchDevelopmentExpense) { this.researchDevelopmentExpense = researchDevelopmentExpense; }
    public BigDecimal getChildcareAllowance() { return childcareAllowance; }
    public void setChildcareAllowance(BigDecimal childcareAllowance) { this.childcareAllowance = childcareAllowance; }
    public int getUnpaidDays() { return unpaidDays; }
    public void setUnpaidDays(int unpaidDays) { this.unpaidDays = unpaidDays; }
    public int getUnauthorizedAbsenceDays() { return unauthorizedAbsenceDays; }
    public void setUnauthorizedAbsenceDays(int unauthorizedAbsenceDays) { this.unauthorizedAbsenceDays = unauthorizedAbsenceDays; }
    public BigDecimal getNationalPensionEmployee() { return nationalPensionEmployee; }
    public void setNationalPensionEmployee(BigDecimal nationalPensionEmployee) { this.nationalPensionEmployee = nationalPensionEmployee; }
    public BigDecimal getHealthInsuranceEmployee() { return healthInsuranceEmployee; }
    public void setHealthInsuranceEmployee(BigDecimal healthInsuranceEmployee) { this.healthInsuranceEmployee = healthInsuranceEmployee; }
    public BigDecimal getLongTermCareInsuranceEmployee() { return longTermCareInsuranceEmployee; }
    public void setLongTermCareInsuranceEmployee(BigDecimal longTermCareInsuranceEmployee) { this.longTermCareInsuranceEmployee = longTermCareInsuranceEmployee; }
    public BigDecimal getEmploymentInsuranceEmployee() { return employmentInsuranceEmployee; }
    public void setEmploymentInsuranceEmployee(BigDecimal employmentInsuranceEmployee) { this.employmentInsuranceEmployee = employmentInsuranceEmployee; }
    public BigDecimal getIncomeTax() { return incomeTax; }
    public void setIncomeTax(BigDecimal incomeTax) { this.incomeTax = incomeTax; }
    public BigDecimal getLocalIncomeTax() { return localIncomeTax; }
    public void setLocalIncomeTax(BigDecimal localIncomeTax) { this.localIncomeTax = localIncomeTax; }
    public BigDecimal getTotalEmployeeDeduction() { return totalEmployeeDeduction; }
    public void setTotalEmployeeDeduction(BigDecimal totalEmployeeDeduction) { this.totalEmployeeDeduction = totalEmployeeDeduction; }
    public BigDecimal getNationalPensionEmployer() { return nationalPensionEmployer; }
    public void setNationalPensionEmployer(BigDecimal nationalPensionEmployer) { this.nationalPensionEmployer = nationalPensionEmployer; }
    public BigDecimal getHealthInsuranceEmployer() { return healthInsuranceEmployer; }
    public void setHealthInsuranceEmployer(BigDecimal healthInsuranceEmployer) { this.healthInsuranceEmployer = healthInsuranceEmployer; }
    public BigDecimal getLongTermCareInsuranceEmployer() { return longTermCareInsuranceEmployer; }
    public void setLongTermCareInsuranceEmployer(BigDecimal longTermCareInsuranceEmployer) { this.longTermCareInsuranceEmployer = longTermCareInsuranceEmployer; }
    public BigDecimal getEmploymentInsuranceEmployer() { return employmentInsuranceEmployer; }
    public void setEmploymentInsuranceEmployer(BigDecimal employmentInsuranceEmployer) { this.employmentInsuranceEmployer = employmentInsuranceEmployer; }
    public BigDecimal getIndustrialAccidentInsuranceEmployer() { return industrialAccidentInsuranceEmployer; }
    public void setIndustrialAccidentInsuranceEmployer(BigDecimal industrialAccidentInsuranceEmployer) { this.industrialAccidentInsuranceEmployer = industrialAccidentInsuranceEmployer; }
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
}