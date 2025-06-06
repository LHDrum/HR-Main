// package main;
package main;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Month;
// import java.util.ArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
// public class CalendarPanel extends JPanel {
public class CalendarPanel extends JPanel {
    private YearMonth currentYearMonth;
    private final JLabel monthLabel;
    private final JPanel calendarGrid;
    // private final List<JCheckBox> dayCheckBoxes = new ArrayList<>();
    private final List<JCheckBox> dayCheckBoxes = new ArrayList<>();
    private final Consumer<Integer> onDayCountChange;
    private final Set<LocalDate> publicHolidays = new HashSet<>();
    // private boolean monthChanged = false; // *** 월 변경 감지 플래그 추가 ***
    private boolean monthChanged = false; // *** 월 변경 감지 플래그 추가 ***

    public CalendarPanel(Consumer<Integer> onDayCountChange) {
        this.onDayCountChange = onDayCountChange;
// currentYearMonth = YearMonth.now();
        currentYearMonth = YearMonth.now();
        initializePublicHolidays(currentYearMonth.getYear());
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("달력"));

        JPanel controlPanel = new JPanel(new BorderLayout());
        JButton prevButton = new JButton("<");
// JButton nextButton = new JButton(">");
        JButton nextButton = new JButton(">");
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));

        controlPanel.add(prevButton, BorderLayout.WEST);
        controlPanel.add(monthLabel, BorderLayout.CENTER);
// controlPanel.add(nextButton, BorderLayout.EAST);
        controlPanel.add(nextButton, BorderLayout.EAST);
        calendarGrid = new JPanel(new GridLayout(0, 7, 2, 2));
        addDayOfWeekLabels();

        add(controlPanel, BorderLayout.NORTH);
        add(calendarGrid, BorderLayout.CENTER);
// prevButton.addActionListener(e -> {
        prevButton.addActionListener(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            this.monthChanged = true; // *** 월 변경 플래그 설정 ***
            initializePublicHolidays(currentYearMonth.getYear()); // 바뀐 연도 기준으로 공휴일 재설정
            updateCalendar();
            // onDayCountChange.accept(0); // 월 변경 시 선택일수 콜백은 updateCalendar 내부에서 처리될 수 있음
        });
// nextButton.addActionListener(e -> {
        nextButton.addActionListener(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            this.monthChanged = true; // *** 월 변경 플래그 설정 ***
            initializePublicHolidays(currentYearMonth.getYear()); // 바뀐 연도 기준으로 공휴일 재설정
            updateCalendar();
            // onDayCountChange.accept(0);
        });
// updateCalendar();
        updateCalendar();
    }

    /**
     * 지정된 연도의 대한민국 공휴일 정보를 초기화합니다.
     // * 2025년 기준 공휴일 및 대체공휴일이 적용되었습니다.
     * 2025년 기준 공휴일 및 대체공휴일이 적용되었습니다.
     * @param year 공휴일을 초기화할 연도
     */
    private void initializePublicHolidays(int year) {
        publicHolidays.clear();
// // 2025년 기준 공휴일 정보 (이전과 동일)
        // 2025년 기준 공휴일 정보
        publicHolidays.add(LocalDate.of(year, Month.JANUARY, 1)); // 신정
        publicHolidays.add(LocalDate.of(year, Month.JANUARY, 28)); // 설날
// publicHolidays.add(LocalDate.of(year, Month.JANUARY, 29));
        publicHolidays.add(LocalDate.of(year, Month.JANUARY, 29)); // 설날
        publicHolidays.add(LocalDate.of(year, Month.JANUARY, 30)); // 설날
        publicHolidays.add(LocalDate.of(year, Month.MARCH, 1)); // 삼일절
        if (year == 2025) {
            publicHolidays.add(LocalDate.of(year, Month.MARCH, 3)); // 2025년 삼일절 대체공휴일
// }
        }
        publicHolidays.add(LocalDate.of(year, Month.MAY, 5)); // 어린이날, 부처님오신날
        if (year == 2025) {
            // 2025년은 어린이날과 부처님오신날이 겹쳐 5월 6일이 대체공휴일입니다.
            publicHolidays.add(LocalDate.of(year, Month.MAY, 6)); // 2025년 대체공휴일
        }
        publicHolidays.add(LocalDate.of(year, Month.JUNE, 6)); // 현충일
        publicHolidays.add(LocalDate.of(year, Month.AUGUST, 15)); // 광복절
        publicHolidays.add(LocalDate.of(year, Month.OCTOBER, 3)); // 개천절
// publicHolidays.add(LocalDate.of(year, Month.OCTOBER, 6));
        publicHolidays.add(LocalDate.of(year, Month.OCTOBER, 6)); // 추석
        publicHolidays.add(LocalDate.of(year, Month.OCTOBER, 7)); // 추석
        if (year == 2025) {
            publicHolidays.add(LocalDate.of(year, Month.OCTOBER, 8)); // 2025년 추석 대체공휴일
// }
        }
        publicHolidays.add(LocalDate.of(year, Month.OCTOBER, 9)); // 한글날
        publicHolidays.add(LocalDate.of(year, Month.DECEMBER, 25)); // 크리스마스
// }
    }

    public boolean isPublicHoliday(LocalDate date) {
        return publicHolidays.contains(date);
// }
    }


    private void addDayOfWeekLabels() {
        String[] days = {"일", "월", "화", "수", "목", "금", "토"};
// for (String day : days) {
        for (String day : days) {
            JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
// if (day.equals("일")) dayLabel.setForeground(Color.RED);
            if (day.equals("일")) dayLabel.setForeground(Color.RED);
            if (day.equals("토")) dayLabel.setForeground(Color.BLUE);
            calendarGrid.add(dayLabel);
        }
    }

    private void updateCalendar() {
        monthLabel.setText(String.format("%d년 %d월", currentYearMonth.getYear(), currentYearMonth.getMonthValue()));
// Component[] components = calendarGrid.getComponents();
        Component[] components = calendarGrid.getComponents();
        for (int i = 7; i < components.length; i++) {
            calendarGrid.remove(components[i]);
// }
        }
        dayCheckBoxes.clear();

        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekValue = firstDayOfMonth.getDayOfWeek().getValue() % 7;
// for (int i = 0; i < dayOfWeekValue; i++) {
        for (int i = 0; i < dayOfWeekValue; i++) {
            calendarGrid.add(new JLabel(""));
// }
        }

        for (int day = 1; day <= currentYearMonth.lengthOfMonth(); day++) {
            LocalDate date = currentYearMonth.atDay(day);
// JCheckBox dayCheckBox = new JCheckBox(String.valueOf(day));
            JCheckBox dayCheckBox = new JCheckBox(String.valueOf(day));
            dayCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
            dayCheckBox.setOpaque(false);
            // 체크박스 클릭 시 월 변경 플래그는 건드리지 않고, 선택일 수 변경 콜백만 호출
            dayCheckBox.addActionListener(e -> onDayCountChange.accept(getSelectedDatesWithStatus().size()));
// DayOfWeek dayOfWeek = date.getDayOfWeek();
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            boolean isPubHoliday = isPublicHoliday(date);
            if (dayOfWeek == DayOfWeek.SUNDAY || isPubHoliday) {
                dayCheckBox.setForeground(Color.RED);
// dayCheckBox.setSelected(false);
                dayCheckBox.setSelected(false);
            } else if (dayOfWeek == DayOfWeek.SATURDAY) {
                dayCheckBox.setForeground(Color.BLUE);
// dayCheckBox.setSelected(false);
                dayCheckBox.setSelected(false);
            } else {
                dayCheckBox.setForeground(Color.BLACK);
// dayCheckBox.setSelected(true);
                dayCheckBox.setSelected(true);
            }
            dayCheckBoxes.add(dayCheckBox);
            calendarGrid.add(dayCheckBox);
// }
        }

        revalidate();
        repaint();
// // updateCalendar이 호출될 때마다 (월 변경 포함) onDayCountChange를 호출하여
        // updateCalendar이 호출될 때마다 (월 변경 포함) onDayCountChange를 호출하여
        // AttendancePage에서 필요한 로직(예: refreshWorkLogForCurrentMonth)이 트리거될 수 있도록 함.
// // 이 콜백은 달력의 날짜 체크박스 상태가 아닌, 달력 자체가 변경되었음을 알리는 역할도 겸함.
        // 이 콜백은 달력의 날짜 체크박스 상태가 아닌, 달력 자체가 변경되었음을 알리는 역할도 겸함.
        onDayCountChange.accept(getSelectedDatesWithStatus().size());
// }
    }

    public List<Map<String, Object>> getSelectedDatesWithStatus() {
        List<Map<String, Object>> selectedDatesInfo = new ArrayList<>();
// for (JCheckBox checkBox : dayCheckBoxes) {
        for (JCheckBox checkBox : dayCheckBoxes) {
            if (checkBox.getText().isEmpty()) continue;
// try {
            try {
                int dayOfMonth = Integer.parseInt(checkBox.getText());
// LocalDate date = currentYearMonth.atDay(dayOfMonth);
                LocalDate date = currentYearMonth.atDay(dayOfMonth);
                Map<String, Object> dateInfo = new HashMap<>();
                dateInfo.put("date", date);
                dateInfo.put("isSelected", checkBox.isSelected());
                dateInfo.put("isPublicHoliday", isPublicHoliday(date));
                dateInfo.put("dayOfWeek", date.getDayOfWeek());
                selectedDatesInfo.add(dateInfo);
// } catch (NumberFormatException e) {
            } catch (NumberFormatException e) {
                // 체크박스가 아닌 다른 컴포넌트일 경우 무시
            }
        }
        return selectedDatesInfo;
// }
    }

    public YearMonth getCurrentYearMonth() {
        return currentYearMonth;
// }
    }

    // *** 월 변경 여부 플래그 getter ***
    public boolean isMonthChangedFlag() {
        return this.monthChanged;
// }
    }

    // *** 월 변경 여부 플래그 초기화 메소드 ***
    public void clearMonthChangedFlag() {
        this.monthChanged = false;
// }
    }

    public boolean areAllWeekdaysSelected() {
        for (int day = 1; day <= currentYearMonth.lengthOfMonth(); day++) {
            LocalDate date = currentYearMonth.atDay(day);
// DayOfWeek dayOfWeek = date.getDayOfWeek();
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            boolean isPubHoliday = isPublicHoliday(date);

            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY && !isPubHoliday) {
                boolean foundAndSelected = false;
// for (JCheckBox cb : dayCheckBoxes) {
                for (JCheckBox cb : dayCheckBoxes) {
                    if (cb.getText().equals(String.valueOf(day))) {
                        if (!cb.isSelected()) {
                            return false;
// }
                        }
                        foundAndSelected = true;
// break;
                        break;
                    }
                }
                if (!foundAndSelected && !dayCheckBoxes.isEmpty()) {
                    return false;
// }
                }
            }
        }
        return true;
// }
    }
}