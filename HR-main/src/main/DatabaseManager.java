package main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseManager {

    // 데이터베이스 파일은 사용자 홈 디렉토리 아래 .내앱이름 폴더에 저장됩니다.
    private static final String DB_FOLDER_PATH = System.getProperty("user.home") + "/.내앱이름_데이터";
    private static final String DB_FILE_PATH = DB_FOLDER_PATH + "/mydb"; // .mv.db 확장자는 자동으로 붙음
    private static final String JDBC_URL = "jdbc:h2:" + DB_FILE_PATH;

    public static Connection getConnection() throws SQLException {
        // 데이터베이스 저장 폴더가 없으면 생성
        java.io.File dbFolder = new java.io.File(DB_FOLDER_PATH);
        if (!dbFolder.exists()) {
            dbFolder.mkdirs(); // 폴더 생성
        }
        return DriverManager.getConnection(JDBC_URL, "sa", ""); // "sa", ""는 기본 사용자/비밀번호
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 예: 사용자 정보를 저장할 테이블 (없으면 생성)
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(255)," +
                    "email VARCHAR(255)" +
                    ")";
            stmt.executeUpdate(sql);
            System.out.println("데이터베이스 초기화 완료 (테이블이 준비되었거나 이미 존재합니다).");
            System.out.println("DB 파일 위치: " + DB_FILE_PATH + ".mv.db");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("데이터베이스 초기화 실패!");
        }
    }

    public static void main(String[] args) { // 테스트용 main 메서드
        initializeDatabase();
        // 여기에 여러분의 애플리케이션 시작 코드를 넣으세요.
        // 예를 들어 Swing/JavaFX UI를 시작하는 코드
    }
}