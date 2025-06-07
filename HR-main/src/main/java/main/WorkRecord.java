package main;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 하루의 근무 기록을 나타내는 데이터 클래스입니다.
 * AttendancePage의 내부 클래스에서 외부 클래스로 분리되었습니다.
 */
public class WorkRecord {
    LocalDate date;
    LocalTime startTime;
    LocalTime endTime;
    AttendancePage.WorkStatus status;
    boolean isOriginallyPublicHoliday;
    long netWorkMinutes;

    public WorkRecord(LocalDate date, LocalTime s, LocalTime e, AttendancePage.WorkStatus st, boolean ioph, long netMinutes) {
        this.date = date;
        this.startTime = s;
        this.endTime = e;
        this.status = st;
        this.isOriginallyPublicHoliday = ioph;
        this.netWorkMinutes = netMinutes;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public AttendancePage.WorkStatus getStatus() { return status; }
    public boolean isOriginallyPublicHoliday() { return isOriginallyPublicHoliday; }
    public long getNetWorkMinutes() { return netWorkMinutes; }
}