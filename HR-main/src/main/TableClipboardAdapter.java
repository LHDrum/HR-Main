package main;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;

/**
 * JTable에 Excel과 유사한 복사, 붙여넣기, 잘라내기 기능을 제공하는 어댑터 클래스입니다.
 * JTable의 ActionMap과 InputMap에 단축키(Ctrl+C, V, X)를 등록하여 사용합니다.
 */
public class TableClipboardAdapter implements ActionListener {
    private final JTable table;
    private final Clipboard systemClipboard;

    public TableClipboardAdapter(JTable table) {
        this.table = table;
        systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        // Ctrl+C (복사), Ctrl+V (붙여넣기), Ctrl+X (잘라내기) 단축키를 테이블에 등록합니다.
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false);
        KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK, false);
        KeyStroke cut = KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK, false);

        table.registerKeyboardAction(this, "Copy", copy, JComponent.WHEN_FOCUSED);
        table.registerKeyboardAction(this, "Paste", paste, JComponent.WHEN_FOCUSED);
        table.registerKeyboardAction(this, "Cut", cut, JComponent.WHEN_FOCUSED);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if ("Copy".equals(command)) {
            copyToClipboard(false);
        } else if ("Cut".equals(command)) {
            copyToClipboard(true); // 복사 후 내용을 지웁니다.
        } else if ("Paste".equals(command)) {
            pasteFromClipboard();
        }
    }

    /**
     * 선택된 셀의 내용을 시스템 클립보드에 복사합니다.
     * @param isCut 잘라내기 작업인 경우 true
     */
    private void copyToClipboard(boolean isCut) {
        int[] selectedRows = table.getSelectedRows();
        int[] selectedCols = table.getSelectedColumns();
        if (selectedRows.length == 0 || selectedCols.length == 0) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int row : selectedRows) {
            for (int col : selectedCols) {
                Object value = table.getValueAt(row, col);
                sb.append(value == null ? "" : value.toString());
                if (col < selectedCols[selectedCols.length - 1]) {
                    sb.append("\t"); // 탭으로 열 구분
                }
            }
            sb.append("\n"); // 줄바꿈으로 행 구분
        }

        StringSelection selection = new StringSelection(sb.toString());
        systemClipboard.setContents(selection, selection);

        if (isCut) {
            for (int row : selectedRows) {
                for (int col : selectedCols) {
                    // 날짜 열(0)은 편집 불가하므로 잘라내기에서 제외
                    if (table.isCellEditable(row, col)) {
                        table.setValueAt("", row, col);
                    }
                }
            }
        }
    }

    /**
     * 시스템 클립보드의 내용을 선택된 셀에 붙여넣습니다.
     */
    private void pasteFromClipboard() {
        int startRow = table.getSelectedRow();
        int startCol = table.getSelectedColumn();
        if (startRow < 0 || startCol < 0) {
            return;
        }

        try {
            String clipboardData = (String) systemClipboard.getData(DataFlavor.stringFlavor);
            String[] lines = clipboardData.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String[] values = lines[i].split("\t");
                for (int j = 0; j < values.length; j++) {
                    int row = startRow + i;
                    int col = startCol + j;

                    if (row < table.getRowCount() && col < table.getColumnCount()) {
                        if (table.isCellEditable(row, col)) {
                            table.setValueAt(values[j], row, col);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(table, "붙여넣기 중 오류가 발생했습니다: " + ex.getMessage(), "붙여넣기 오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}