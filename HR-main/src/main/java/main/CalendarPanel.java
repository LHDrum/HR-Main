package main;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CalendarPanel extends JPanel {
    private YearMonth currentYearMonth;
    private final JLabel monthLabel;
    private final JPanel calendarGrid;
    private final List<JCheckBox> dayCheckBoxes = new ArrayList<>();
    private final Consumer<Integer> onDayCountChange;
    private final Set<LocalDate> publicHolidays = new HashSet<>();
    private boolean monthChanged = false;

    public CalendarPanel(Consumer<Integer> onDayCountChange) {
        this.onDayCountChange = onDayCountChange;
        currentYearMonth = YearMonth.now();
        initializePublicHolidays(currentYearMonth.getYear());
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("달력"));

        JPanel controlPanel = new JPanel(new BorderLayout());
        JButton prevButton = new JButton("<");
        JButton nextButton = new JButton(">");
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));

        controlPanel.add(prevButton, BorderLayout.WEST);
        controlPanel.add(monthLabel, BorderLayout.CENTER);
        controlPanel.add(nextButton, BorderLayout.EAST);
        calendarGrid = new JPanel(new GridLayout(0, 7, 2, 2));
        addDayOfWeekLabels();

        add(controlPanel, BorderLayout.NORTH);
        add(calendarGrid, BorderLayout.CENTER);

        prevButton.addActionListener(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            this.monthChanged = true;
            initializePublicHolidays(currentYearMonth.getYear());
            updateCalendar();
        });

        nextButton.addActionListener(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            this.monthChanged = true;
            initializePublicHolidays(currentYearMonth.getYear());
            updateCalendar();
        });

        // [수정] 생성자에서 직접 호출하는 대신, invokeLater를 사용하여 모든 컴포넌트 초기화 후 updateCalendar가 실행되도록 변경
        // 이렇게 하면 AttendancePage의 calendarPanel 필드가 null일 때 콜백이 호출되는 문제를 방지할 수 있습니다.
        SwingUtilities.invokeLater(this::updateCalendar);
    }

    /**
     * 지정된 연도의 대한민국 공휴일 정보를 초기화합니다.
     * 2025년 기준 공휴일 및 대체공휴일이 적용되었습니다.
     * @param year 공휴일을 초기화할 연도
     */
    private void initializePublicHolidays(int year) {
        publicHolidays.clear();
        // 2025년 기준 공휴일 정보
        publicHolidays.add(LocalDate.of(year, Month.JANUARY, 1)); // 신정
        publicHolidays.add(LocalDate.of(year, Month.JANUARY, 28)); // 설날
        publicHolidays.add(LocalDate.of(year, Month.JANUARY, 29)); // 설날
        publicHolidays.add(LocalDate.of(year, Month.JANUARY, 30)); // 설날
        publicHolidays.add(LocalDate.of(year, Month.MARCH, 1)); // 삼일절
        // 2025년 3.1절은 토요일이므로 대체공휴일 없음. 법령상 토요일은 대체공휴일 적용대상이 아님.
        // 어린이날은 토요일과 겹치면 대체공휴일이 발생하지만, 3.1절/설날/추석은 일요일과 겹칠때만 발생.
        publicHolidays.add(LocalDate.of(year, Month.MAY, 5)); // 어린이날
        publicHolidays.add(LocalDate.of(year, Month.MAY, 6)); // 부처님 오신 날 (2023년부터 대체공휴일 적용 대상 포함)
        publicHolidays.add(LocalDate.of(year, Month.JUNE, 6)); // 현충일
        publicHolidays.add(LocalDate.of(year, Month.AUGUST, 15)); // 광복절
        publicHolidays.add(LocalDate.of(year, Month.OCTOBER, 3)); // 개천절
        publicHolidays.add(LocalDate.of(year, Month.OCTOBER, 6)); // 추석
        publicHolidays.add(LocalDate.of(year, Month.OCTOBER, 7)); // 추석
        publicHolidays.add(LocalDate.of(year, Month.OCTOBER, 8)); // 추석
        publicHolidays.add(LocalDate.of(year, Month.OCTOBER, 9)); // 한글날
        publicHolidays.add(LocalDate.of(year, Month.DECEMBER, 25)); // 크리스마스

        // 2025년 특정 대체 공휴일 (예시: 어린이날이 일요일인 경우)
        // 법령에 따라 동적으로 계산하는 로직이 더 정확하지만, 여기서는 주요 공휴일 기준으로 작성
        LocalDate childrensDay = LocalDate.of(year, 5, 5);
        if (childrensDay.getDayOfWeek() == DayOfWeek.SUNDAY || childrensDay.getDayOfWeek() == DayOfWeek.SATURDAY) {
            publicHolidays.add(childrensDay.plusDays(childrensDay.getDayOfWeek() == DayOfWeek.SUNDAY ? 1 : 2));
        }
    }


    public boolean isPublicHoliday(LocalDate date) {
        return publicHolidays.contains(date);
    }

    public Set<LocalDate> getPublicHolidays() {
        return this.publicHolidays;
    }

    private void addDayOfWeekLabels() {
        String[] days = {"일", "월", "화", "수", "목", "금", "토"};
        for (String day : days) {
            JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
            if (day.equals("일")) dayLabel.setForeground(Color.RED);
            if (day.equals("토")) dayLabel.setForeground(Color.BLUE);
            calendarGrid.add(dayLabel);
        }
    }

    private void updateCalendar() {
        monthLabel.setText(String.format("%d년 %d월", currentYearMonth.getYear(), currentYearMonth.getMonthValue()));
        Component[] components = calendarGrid.getComponents();
        for (int i = 7; i < components.length; i++) {
            calendarGrid.remove(components[i]);
        }
        dayCheckBoxes.clear();

        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekValue = firstDayOfMonth.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < dayOfWeekValue; i++) {
            calendarGrid.add(new JLabel(""));
        }

        for (int day = 1; day <= currentYearMonth.lengthOfMonth(); day++) {
            LocalDate date = currentYearMonth.atDay(day);
            JCheckBox dayCheckBox = new JCheckBox(String.valueOf(day));
            dayCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
            dayCheckBox.setOpaque(false);
            dayCheckBox.addActionListener(e -> onDayCountChange.accept(getSelectedDatesWithStatus().size()));
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            boolean isPubHoliday = isPublicHoliday(date);
            if (dayOfWeek == DayOfWeek.SUNDAY || isPubHoliday) {
                dayCheckBox.setForeground(Color.RED);
                dayCheckBox.setSelected(false);
            } else if (dayOfWeek == DayOfWeek.SATURDAY) {
                dayCheckBox.setForeground(Color.BLUE);
                dayCheckBox.setSelected(false);
            } else {
                dayCheckBox.setForeground(Color.BLACK);
                dayCheckBox.setSelected(true);
            }
            dayCheckBoxes.add(dayCheckBox);
            calendarGrid.add(dayCheckBox);
        }

        revalidate();
        repaint();
        onDayCountChange.accept(getSelectedDatesWithStatus().size());
    }

    public List<Map<String, Object>> getSelectedDatesWithStatus() {
        List<Map<String, Object>> selectedDatesInfo = new ArrayList<>();
        for (JCheckBox checkBox : dayCheckBoxes) {
            if (checkBox.getText().isEmpty()) continue;
            try {
                int dayOfMonth = Integer.parseInt(checkBox.getText());
                LocalDate date = currentYearMonth.atDay(dayOfMonth);
                Map<String, Object> dateInfo = new HashMap<>();
                dateInfo.put("date", date);
                dateInfo.put("isSelected", checkBox.isSelected());
                dateInfo.put("isPublicHoliday", isPublicHoliday(date));
                dateInfo.put("dayOfWeek", date.getDayOfWeek());
                selectedDatesInfo.add(dateInfo);
            } catch (NumberFormatException e) {
                // Ignore non-numeric checkboxes
            }
        }
        return selectedDatesInfo;
    }

    public YearMonth getCurrentYearMonth() {
        return currentYearMonth;
    }

    public boolean isMonthChangedFlag() {
        return this.monthChanged;
    }

    public void clearMonthChangedFlag() {
        this.monthChanged = false;
    }

    public boolean areAllWeekdaysSelected() {
        for (int day = 1; day <= currentYearMonth.lengthOfMonth(); day++) {
            LocalDate date = currentYearMonth.atDay(day);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            boolean isPubHoliday = isPublicHoliday(date);

            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY && !isPubHoliday) {
                boolean foundAndSelected = false;
                for (JCheckBox cb : dayCheckBoxes) {
                    if (cb.getText().equals(String.valueOf(day))) {
                        if (!cb.isSelected()) {
                            return false;
                        }
                        foundAndSelected = true;
                        break;
                    }
                }
                if (!foundAndSelected && !dayCheckBoxes.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}