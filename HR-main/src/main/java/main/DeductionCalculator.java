package main;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DeductionCalculator {

    // [수정] 모든 요율을 오차 없는 BigDecimal 상수로 정의
    private static final BigDecimal NATIONAL_PENSION_RATE = new BigDecimal("0.045");
    private static final BigDecimal HEALTH_INSURANCE_RATE = new BigDecimal("0.03545");
    private static final BigDecimal LONG_TERM_CARE_RATE_OF_HEALTH_INS = new BigDecimal("0.1295");
    private static final BigDecimal EMPLOYMENT_INSURANCE_RATE = new BigDecimal("0.009");

    private static final BigDecimal NATIONAL_PENSION_MIN_BASE = new BigDecimal("370000");
    private static final BigDecimal NATIONAL_PENSION_MAX_BASE = new BigDecimal("5900000");

    public static DeductionResult calculate(BigDecimal monthlyGrossSalary, BigDecimal industrialAccidentRate, int dependents) {
        DeductionResult result = new DeductionResult();

        // 1. 국민연금 계산
        BigDecimal pensionBase = monthlyGrossSalary.max(NATIONAL_PENSION_MIN_BASE).min(NATIONAL_PENSION_MAX_BASE);
        result.nationalPensionEmployee = floorTo10(pensionBase.multiply(NATIONAL_PENSION_RATE));
        result.nationalPensionEmployer = result.nationalPensionEmployee;

        // 2. 건강보험 계산
        result.healthInsuranceEmployee = floorTo10(monthlyGrossSalary.multiply(HEALTH_INSURANCE_RATE));
        result.healthInsuranceEmployer = result.healthInsuranceEmployee;

        // 3. 장기요양보험 계산
        result.longTermCareInsuranceEmployee = floorTo10(result.healthInsuranceEmployee.multiply(LONG_TERM_CARE_RATE_OF_HEALTH_INS));
        result.longTermCareInsuranceEmployer = result.longTermCareInsuranceEmployee;

        // 4. 고용보험 계산
        result.employmentInsuranceEmployee = floorTo10(monthlyGrossSalary.multiply(EMPLOYMENT_INSURANCE_RATE));
        result.employmentInsuranceEmployer = result.employmentInsuranceEmployee;

        // 5. 산재보험 계산 (사업주 100% 부담)
        result.industrialAccidentInsuranceEmployer = floorTo10(monthlyGrossSalary.multiply(industrialAccidentRate));

        // 6. 근로소득세 계산
        result.incomeTax = calculateIncomeTax(monthlyGrossSalary, dependents);
        result.localIncomeTax = floorTo10(result.incomeTax.multiply(new BigDecimal("0.1")));

        // 7. 합계 계산
        result.totalEmployeeDeduction = result.nationalPensionEmployee
                .add(result.healthInsuranceEmployee)
                .add(result.longTermCareInsuranceEmployee)
                .add(result.employmentInsuranceEmployee)
                .add(result.incomeTax)
                .add(result.localIncomeTax);

        result.totalEmployerDeduction = result.nationalPensionEmployer
                .add(result.healthInsuranceEmployer)
                .add(result.longTermCareInsuranceEmployer)
                .add(result.employmentInsuranceEmployer)
                .add(result.industrialAccidentInsuranceEmployer);

        result.netPay = monthlyGrossSalary.subtract(result.totalEmployeeDeduction);

        return result;
    }

    // 원단위 절사 (10원 단위로 내림)
    private static BigDecimal floorTo10(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.divide(BigDecimal.TEN, 0, RoundingMode.FLOOR).multiply(BigDecimal.TEN);
    }

    // 근로소득세 간이세액표 단순화 구현
    private static BigDecimal calculateIncomeTax(BigDecimal salary, int dependents) {
        if (salary.compareTo(new BigDecimal("2500000")) < 0) return BigDecimal.ZERO;
        if (salary.compareTo(new BigDecimal("3000000")) < 0) return floorTo10(salary.multiply(new BigDecimal("0.02")));
        if (salary.compareTo(new BigDecimal("4000000")) < 0) return floorTo10(salary.multiply(new BigDecimal("0.05")));
        if (salary.compareTo(new BigDecimal("5000000")) < 0) return floorTo10(salary.multiply(new BigDecimal("0.08")));
        if (salary.compareTo(new BigDecimal("6000000")) < 0) return floorTo10(salary.multiply(new BigDecimal("0.10")));
        return floorTo10(salary.multiply(new BigDecimal("0.15")));
    }
}