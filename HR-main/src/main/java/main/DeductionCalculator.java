package main;

import java.util.Map;

public class DeductionCalculator {

    // 2025년 기준 요율 (가정치 포함)
    private static final double NATIONAL_PENSION_RATE = 0.045; // 각 4.5%
    private static final double HEALTH_INSURANCE_RATE = 0.03545; // 각 3.545%
    private static final double LONG_TERM_CARE_RATE_OF_HEALTH_INS = 0.1295; // 건강보험료의 12.95%
    private static final double EMPLOYMENT_INSURANCE_RATE = 0.009; // 각 0.9%

    // 국민연금 기준소득월액 상/하한 (2025년 기준 가정)
    private static final int NATIONAL_PENSION_MIN_BASE = 370_000;
    private static final int NATIONAL_PENSION_MAX_BASE = 5_900_000;

    public static DeductionResult calculate(double monthlyGrossSalary, double industrialAccidentRate, int dependents) {
        DeductionResult result = new DeductionResult();

        // 1. 국민연금 계산
        int pensionBase = (int) Math.max(NATIONAL_PENSION_MIN_BASE, Math.min(monthlyGrossSalary, NATIONAL_PENSION_MAX_BASE));
        result.nationalPensionEmployee = floorTo10(pensionBase * NATIONAL_PENSION_RATE);
        result.nationalPensionEmployer = floorTo10(pensionBase * NATIONAL_PENSION_RATE);

        // 2. 건강보험 계산
        result.healthInsuranceEmployee = floorTo10(monthlyGrossSalary * HEALTH_INSURANCE_RATE);
        result.healthInsuranceEmployer = floorTo10(monthlyGrossSalary * HEALTH_INSURANCE_RATE);

        // 3. 장기요양보험 계산
        result.longTermCareInsuranceEmployee = floorTo10(result.healthInsuranceEmployee * LONG_TERM_CARE_RATE_OF_HEALTH_INS);
        result.longTermCareInsuranceEmployer = floorTo10(result.healthInsuranceEmployer * LONG_TERM_CARE_RATE_OF_HEALTH_INS);

        // 4. 고용보험 계산
        result.employmentInsuranceEmployee = floorTo10(monthlyGrossSalary * EMPLOYMENT_INSURANCE_RATE);
        result.employmentInsuranceEmployer = floorTo10(monthlyGrossSalary * EMPLOYMENT_INSURANCE_RATE); // 사업주 요율은 실제 더 복잡함 (단순화)

        // 5. 산재보험 계산 (사업주 100% 부담)
        result.industrialAccidentInsuranceEmployer = floorTo10(monthlyGrossSalary * industrialAccidentRate);

        // 6. 근로소득세 계산 (간이세액표 단순화)
        result.incomeTax = calculateIncomeTax(monthlyGrossSalary, dependents);
        result.localIncomeTax = floorTo10(result.incomeTax * 0.1);

        // 7. 합계 계산
        result.totalEmployeeDeduction = result.nationalPensionEmployee + result.healthInsuranceEmployee +
                result.longTermCareInsuranceEmployee + result.employmentInsuranceEmployee +
                result.incomeTax + result.localIncomeTax;

        result.totalEmployerDeduction = result.nationalPensionEmployer + result.healthInsuranceEmployer +
                result.longTermCareInsuranceEmployer + result.employmentInsuranceEmployer +
                result.industrialAccidentInsuranceEmployer;

        result.netPay = monthlyGrossSalary - result.totalEmployeeDeduction;

        return result;
    }

    // 원단위 절사 (10원 단위로 내림)
    private static double floorTo10(double value) {
        return Math.floor(value / 10.0) * 10;
    }

    // 근로소득세 간이세액표 단순화 구현 (2025년 기준, 부양가족 1인 기준)
    private static double calculateIncomeTax(double salary, int dependents) {
        // 실제로는 매우 복잡한 테이블 조회 로직이 필요.
        // 여기서는 예시로 급여 구간에 따른 단순 세율 적용.
        if (salary < 2_500_000) return 0;
        if (salary < 3_000_000) return floorTo10((salary * 0.02));
        if (salary < 4_000_000) return floorTo10((salary * 0.05));
        if (salary < 5_000_000) return floorTo10((salary * 0.08));
        if (salary < 6_000_000) return floorTo10((salary * 0.10));
        return floorTo10((salary * 0.15));
    }
}