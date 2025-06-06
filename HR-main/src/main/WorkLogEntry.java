package main;

import java.time.LocalDate;
import java.time.LocalTime;

public class WorkLogEntry {
    private int id;
    private int employeeId;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private AttendancePage.WorkStatus status; // AttendancePage의 WorkStatus enum 사용

    // DB 로드용 생성자
    public WorkLogEntry(int id, int employeeId, LocalDate workDate, LocalTime startTime, LocalTime endTime, AttendancePage.WorkStatus status) {
        this.id = id;
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // 새로 저장할 때 사용 (id는 자동 생성)
    public WorkLogEntry(int employeeId, LocalDate workDate, LocalTime startTime, LocalTime endTime, AttendancePage.WorkStatus status) {
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // Getters
    public int getId() { return id; }
    public int getEmployeeId() { return employeeId; }
    public LocalDate getWorkDate() { return workDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public AttendancePage.WorkStatus getStatus() { return status; }

    // Setters (필요에 따라)
    public void setId(int id) { this.id = id; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setStatus(AttendancePage.WorkStatus status) { this.status = status; }
}