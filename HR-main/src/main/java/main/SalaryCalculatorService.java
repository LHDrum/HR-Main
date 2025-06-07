package main;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SalaryCalculatorService {

    private final PayrollManager payrollManager;
    // 계산 정확도를 위해 중간 과정에서 사용할 소수점 자리수를 넉넉하게 설정
    private static final int CALC_SCALE = 8;
    private static final int FINAL_SCALE = 0; // 최종 금액은 원단위로 반올림

    public SalaryCalculatorService(PayrollManager payrollManager) {
        this.payrollManager = payrollManager;
    }

    private long calculateNightMinutes(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) return 0;
        final LocalTime NIGHT_START_THRESHOLD = LocalTime.of(22, 0);
        final LocalTime NIGHT_END_THRESHOLD = LocalTime.of(6, 0);
        long totalNightMinutes = 0;

        // 익일 퇴근 (퇴근 시간이 출근 시간보다 이름)
        if (endTime.isBefore(startTime)) {
            // 출근 시점부터 자정까지의 야간 근무
            if (startTime.isBefore(NIGHT_START_THRESHOLD)) {
                totalNightMinutes += Duration.between(NIGHT_START_THRESHOLD, LocalTime.MAX).toMinutes() + 1; // 22:00 ~ 23:59:59...
            } else {
                totalNightMinutes += Duration.between(startTime, LocalTime.MAX).toMinutes() + 1;
            }
            // 자정부터 퇴근 시점까지의 야간 근무
            if (endTime.isAfter(NIGHT_END_THRESHOLD)) {
                totalNightMinutes += Duration.between(LocalTime.MIN, NIGHT_END_THRESHOLD).toMinutes();
            } else {
                totalNightMinutes += Duration.between(LocalTime.MIN, endTime).toMinutes();
            }
        } else { // 당일 퇴근
            // 새벽 시간대 근무 (00:00 ~ 06:00)
            if (startTime.isBefore(NIGHT_END_THRESHOLD)) {
                LocalTime morningNightEnd = endTime.isBefore(NIGHT_END_THRESHOLD) ? endTime : NIGHT_END_THRESHOLD;
                totalNightMinutes += Duration.between(startTime, morningNightEnd).toMinutes();
            }
            // 저녁 시간대 근무 (22:00 ~ 24:00)
            if (endTime.isAfter(NIGHT_START_THRESHOLD)) {
                LocalTime eveningNightStart = startTime.isAfter(NIGHT_START_THRESHOLD) ? startTime : NIGHT_START_THRESHOLD;
                totalNightMinutes += Duration.between(eveningNightStart, endTime).toMinutes();
            }
        }
        return totalNightMinutes;
    }

    public CalculationResult calculateSalary(List<WorkRecord> records, Payroll contractData, Employee employee,
                                             YearMonth currentPeriod, BigDecimal currentSalaryPercentage, BigDecimal adHocBonus,
                                             boolean adHocBonusApplied, Set<LocalDate> publicHolidays) {
        CalculationResult result = new CalculationResult();

        // --- 0. 설정값 로드 ---
        Map<String, String> settings = payrollManager.loadSettings();
        BigDecimal standardHours = new BigDecimal(settings.getOrDefault("standardWorkHours", "209.0"));
        BigDecimal fixedOvertimeHoursForCalc = new BigDecimal(settings.getOrDefault("fixedOvertimeHours", "15.0"));
        BigDecimal standardTotalHoursForRateCalc = standardHours.add(fixedOvertimeHoursForCalc);
        BigDecimal nominalFixedOvertimeHours = new BigDecimal(settings.getOrDefault("nominalFixedOvertimeHours", "10.0"));

        boolean applyOvertime = Boolean.parseBoolean(settings.getOrDefault("applyOvertime", "true"));
        boolean applyNightWork = Boolean.parseBoolean(settings.getOrDefault("applyNightWork", "true"));
        boolean applyHolidayWork = Boolean.parseBoolean(settings.getOrDefault("applyHolidayWork", "true"));

        // --- 1. 입사일 비례배분 (일할계산) ---
        // [수정] double 대신 BigDecimal로 직접 계산
        BigDecimal hireProrationRatio = BigDecimal.ONE;
        LocalDate hireDate = employee.getHireDate();
        if (hireDate != null && hireDate.getYear() == currentPeriod.getYear() && hireDate.getMonth() == currentPeriod.getMonth() && hireDate.getDayOfMonth() != 1) {
            int totalDaysInMonth = currentPeriod.lengthOfMonth();
            long daysFromHireDateToEndOfMonth = (long) currentPeriod.atEndOfMonth().getDayOfMonth() - hireDate.getDayOfMonth() + 1;

            if (totalDaysInMonth > 0) {
                hireProrationRatio = new BigDecimal(daysFromHireDateToEndOfMonth)
                        .divide(new BigDecimal(totalDaysInMonth), CALC_SCALE, RoundingMode.HALF_UP);
            } else {
                hireProrationRatio = BigDecimal.ZERO;
            }
        }
        result.setHireProrationRatio(hireProrationRatio);

        // 계약정보에 비례배분 적용
        BigDecimal proratedContractBasic = contractData.getMonthlyBasicSalary().multiply(hireProrationRatio);
        BigDecimal proratedContractFixedOT = contractData.getFixedOvertimeAllowance().multiply(hireProrationRatio);
        BigDecimal proratedContractBonus = contractData.getBonus().multiply(hireProrationRatio);
        BigDecimal proratedOtherAllowances = contractData.getOtherAllowance().multiply(hireProrationRatio);
        BigDecimal proratedMealAllowances = contractData.getMealAllowance().multiply(hireProrationRatio);
        BigDecimal proratedVehicleFees = contractData.getVehicleMaintenanceFee().multiply(hireProrationRatio);
        BigDecimal proratedResearchExpenses = contractData.getResearchDevelopmentExpense().multiply(hireProrationRatio);
        BigDecimal proratedChildcareAllowances = contractData.getChildcareAllowance().multiply(hireProrationRatio);

        // --- 2. 통상 시급 계산 ---
        BigDecimal employeeAnnualSalary = employee.getAnnualSalary() != null ? employee.getAnnualSalary() : BigDecimal.ZERO;
        BigDecimal monthlyEquivalent = employeeAnnualSalary.divide(new BigDecimal("12"), CALC_SCALE, RoundingMode.HALF_UP).multiply(hireProrationRatio);

        BigDecimal hourlyRate = BigDecimal.ZERO;
        if (standardTotalHoursForRateCalc.compareTo(BigDecimal.ZERO) > 0) {
            hourlyRate = monthlyEquivalent.divide(standardTotalHoursForRateCalc, CALC_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal minuteRate = hourlyRate.divide(new BigDecimal("60"), CALC_SCALE + 2, RoundingMode.HALF_UP);

        // --- 3. 근무 부족분 시간 및 금액 계산 ---
        long totalShortfallMinutes = 0L;
        for (WorkRecord record : records) {
            DayOfWeek dow = record.getDate().getDayOfWeek();
            boolean isCalendarWeekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !publicHolidays.contains(record.getDate());
            if (isCalendarWeekday && (record.getStatus() == AttendancePage.WorkStatus.ABSENCE || record.getStatus() == AttendancePage.WorkStatus.UNPAID_HOLIDAY)) {
                totalShortfallMinutes += (8 * 60);
            }
        }
        result.setTotalShortfallMinutes(totalShortfallMinutes);
        BigDecimal shortfallDeduction = minuteRate.multiply(new BigDecimal(totalShortfallMinutes));
        result.setTotalShortfallMonetaryDeduction(shortfallDeduction);

        // --- 4. 주휴수당 차감 ---
        BigDecimal weeklyAbsencePenalty = BigDecimal.ZERO;
        final BigDecimal dailyAmountForPenalty = hourlyRate.multiply(new BigDecimal("8"));
        Set<Integer> weeksWithAbsence = new HashSet<>();
        WeekFields weekFields = WeekFields.of(Locale.KOREA); // 한국 기준 주차 계산
        for (WorkRecord rec : records) {
            if (rec.getStatus() == AttendancePage.WorkStatus.ABSENCE) {
                // 주말, 공휴일 결근은 주휴수당에 영향 없음
                if (rec.getDate().getDayOfWeek() != DayOfWeek.SATURDAY && rec.getDate().getDayOfWeek() != DayOfWeek.SUNDAY && !publicHolidays.contains(rec.getDate())) {
                    weeksWithAbsence.add(rec.getDate().get(weekFields.weekOfWeekBasedYear()));
                }
            }
        }
        weeklyAbsencePenalty = dailyAmountForPenalty.multiply(new BigDecimal(weeksWithAbsence.size()));
        result.setWeeklyAbsencePenalty(weeklyAbsencePenalty);

        // --- 5. 추가 수당 계산 (연장, 야간, 휴일) ---
        long totalOvertimeMinutes = 0, totalHolidayMinutes = 0, totalNightMinutes = 0;
        final BigDecimal fixedOtThresholdMinutes = nominalFixedOvertimeHours.multiply(new BigDecimal("60"));

        for (WorkRecord record : records) {
            if (record.getStatus() == AttendancePage.WorkStatus.ABSENCE || record.getStatus() == AttendancePage.WorkStatus.UNPAID_HOLIDAY || record.getNetWorkMinutes() <= 0) continue;

            if (record.isOriginallyPublicHoliday()) {
                totalHolidayMinutes += record.getNetWorkMinutes();
            } else {
                // 평일 8시간 초과 근무분을 연장근무로 계산
                totalOvertimeMinutes += Math.max(0, record.getNetWorkMinutes() - (8 * 60));
            }
            totalNightMinutes += calculateNightMinutes(record.getStartTime(), record.getEndTime());
        }

        BigDecimal overtimePremium = BigDecimal.ZERO;
        BigDecimal holidayPremium = BigDecimal.ZERO;
        BigDecimal nightPremium = BigDecimal.ZERO;

        // 고정연장시간을 초과하는 연장근무에 대해서만 추가 수당 지급 (1.5배)
        if (applyOvertime) {
            // 휴일 근무도 연장근무의 일종으로 간주하여 총합 계산
            BigDecimal totalPremiumEligibleMinutes = new BigDecimal(totalOvertimeMinutes).add(new BigDecimal(totalHolidayMinutes));

            if (totalPremiumEligibleMinutes.compareTo(fixedOtThresholdMinutes) > 0) {
                BigDecimal extraMinutes = totalPremiumEligibleMinutes.subtract(fixedOtThresholdMinutes);
                result.setPaidPremiumMinutes(extraMinutes.longValue());
                // 1.5배 중 0.5배만 추가 지급 (기본 1배는 기본급에 포함 가정)
                overtimePremium = hourlyRate.multiply(extraMinutes.divide(new BigDecimal("60"), CALC_SCALE, RoundingMode.HALF_UP)).multiply(new BigDecimal("0.5"));
            }
        }
        // 휴일근무 가산 수당 (0.5배) - 휴일 근무 자체에 대한 1배는 기본급에 포함
        if (applyHolidayWork) {
            holidayPremium = hourlyRate.multiply(new BigDecimal(totalHolidayMinutes).divide(new BigDecimal("60"), CALC_SCALE, RoundingMode.HALF_UP)).multiply(new BigDecimal("0.5"));
        }
        // 야간근무 가산 수당 (0.5배)
        if (applyNightWork) {
            nightPremium = hourlyRate.multiply(new BigDecimal(totalNightMinutes).divide(new BigDecimal("60"), CALC_SCALE, RoundingMode.HALF_UP)).multiply(new BigDecimal("0.5"));
        }

        result.setOvertimePremium(overtimePremium);
        result.setHolidayPremium(holidayPremium);
        result.setNightPremium(nightPremium);

        // --- 6. 지급률 및 최종 금액 계산 ---
        BigDecimal totalDeduction = shortfallDeduction.add(weeklyAbsencePenalty);
        // [수정] double 나눗셈 대신 BigDecimal 나눗셈 사용
        BigDecimal percMultiplier = currentSalaryPercentage.divide(new BigDecimal("100"), CALC_SCALE, RoundingMode.HALF_UP);

        result.setFinalAdjustedBasicPay(proratedContractBasic.subtract(totalDeduction).multiply(percMultiplier).max(BigDecimal.ZERO));
        result.setFinalAdjustedFixedOvertimeAllowance(proratedContractFixedOT.multiply(percMultiplier).max(BigDecimal.ZERO));
        result.setFinalAdjustedBonus(proratedContractBonus.add(adHocBonusApplied ? adHocBonus : BigDecimal.ZERO).multiply(percMultiplier).max(BigDecimal.ZERO));
        result.setFinalAdjustedOtherAllowance(proratedOtherAllowances.multiply(percMultiplier).max(BigDecimal.ZERO));
        result.setFinalAdjustedMealAllowance(proratedMealAllowances.multiply(percMultiplier).max(BigDecimal.ZERO));
        result.setFinalAdjustedVehicleMaintenanceFee(proratedVehicleFees.multiply(percMultiplier).max(BigDecimal.ZERO));
        result.setFinalAdjustedResearchDevelopmentExpense(proratedResearchExpenses.multiply(percMultiplier).max(BigDecimal.ZERO));
        result.setFinalAdjustedChildcareAllowance(proratedChildcareAllowances.multiply(percMultiplier).max(BigDecimal.ZERO));
        result.setFinalAdjustedAdditionalOvertimePremium(overtimePremium.add(holidayPremium).add(nightPremium).multiply(percMultiplier).max(BigDecimal.ZERO));

        BigDecimal sumProratedItems = proratedContractBasic.add(proratedContractFixedOT).add(proratedContractBonus).add(proratedOtherAllowances).add(proratedMealAllowances).add(proratedVehicleFees).add(proratedResearchExpenses).add(proratedChildcareAllowances);

        // [수정] doubleValue() 호출 제거하고 BigDecimal 타입으로 결과 설정
        if (sumProratedItems.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal attendanceRatio = sumProratedItems.subtract(totalDeduction).divide(sumProratedItems, 4, RoundingMode.HALF_UP);
            result.setAttendanceBasedPaymentRatio(attendanceRatio);
        } else {
            result.setAttendanceBasedPaymentRatio(BigDecimal.ONE);
        }

        return result;
    }
}