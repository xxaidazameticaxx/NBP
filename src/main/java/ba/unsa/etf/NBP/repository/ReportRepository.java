package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.dto.report.CourseAttendanceReportDto;
import ba.unsa.etf.NBP.dto.report.StudentAttendanceReportDto;
import ba.unsa.etf.NBP.model.Report;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for attendance analytics and reporting.
 * <p>
 * Generates attendance summaries for courses and student attendance history,
 * computing presence percentages and session counts.
 */
@Repository
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Report> rowMapper = (rs, rowNum) -> {
        Report report = new Report();
        report.setId(rs.getLong("ID"));
        report.setType(rs.getString("TYPE"));
        report.setContent(rs.getBytes("CONTENT"));
        return report;
    };

    /**
     * Returns all saved reports.
     *
     * @return list of all reports
     */
    public List<Report> findAll() {
        String sql = "SELECT * FROM NBP_REPORT";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Finds a saved report by its ID.
     *
     * @param id report ID
     * @return the report, or empty if not found
     */
    public Optional<Report> findById(Long id) {
        String sql = "SELECT * FROM NBP_REPORT WHERE ID = ?";
        List<Report> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    /**
     * Finds all saved reports matching the given type.
     *
     * @param type report type
     * @return list of matching reports
     */
    public List<Report> findByType(String type) {
        String sql = "SELECT * FROM NBP_REPORT WHERE TYPE = ?";
        return jdbcTemplate.query(sql, rowMapper, type);
    }

    /**
     * Inserts a new report into the database.
     *
     * @param report report to save
     */
    public void save(Report report) {
        String sql = "INSERT INTO NBP_REPORT (TYPE, CONTENT) VALUES (?, ?)";
        jdbcTemplate.update(sql, report.getType(), report.getContent());
    }

    /**
     * Updates an existing report.
     *
     * @param report report with updated fields
     */
    public void update(Report report) {
        String sql = "UPDATE NBP_REPORT SET TYPE = ?, CONTENT = ? WHERE ID = ?";
        jdbcTemplate.update(sql, report.getType(), report.getContent(), report.getId());
    }

    /**
     * Deletes a report by its ID.
     *
     * @param id report ID
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_REPORT WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<CourseAttendanceReportDto> getCourseAttendanceReport(Long courseId) {
        String sql = """
                SELECT e.STUDENT_ID,
                       u.FIRST_NAME || ' ' || u.LAST_NAME AS FULL_NAME,
                       s.INDEX_NUMBER,
                       NVL(ts.TOTAL_SESSIONS, 0) AS TOTAL_SESSIONS,
                       NVL(att.ATTENDED, 0) AS ATTENDED,
                       NVL(ts.TOTAL_SESSIONS, 0) - NVL(att.ATTENDED, 0) AS ABSENT,
                       CASE WHEN NVL(ts.TOTAL_SESSIONS, 0) = 0 THEN 0
                            ELSE ROUND(NVL(att.ATTENDED, 0) * 100.0 / ts.TOTAL_SESSIONS, 2)
                       END AS PERCENTAGE
                FROM NBP_ENROLLMENT e
                JOIN NBP_STUDENT s ON e.STUDENT_ID = s.ID
                JOIN NBP.NBP_USER u ON s.USER_ID = u.ID
                LEFT JOIN (
                    SELECT COUNT(*) AS TOTAL_SESSIONS
                    FROM NBP_COURSE_SESSION
                    WHERE COURSE_ID = ?
                ) ts ON 1 = 1
                LEFT JOIN (
                    SELECT a.STUDENT_ID, COUNT(*) AS ATTENDED
                    FROM NBP_ATTENDANCE a
                    JOIN NBP_COURSE_SESSION cs ON a.COURSE_SESSION_ID = cs.ID
                    WHERE cs.COURSE_ID = ? AND a.IS_PRESENT = 1
                    GROUP BY a.STUDENT_ID
                ) att ON att.STUDENT_ID = e.STUDENT_ID
                WHERE e.COURSE_ID = ?
                ORDER BY u.LAST_NAME, u.FIRST_NAME
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CourseAttendanceReportDto dto = new CourseAttendanceReportDto();
            dto.setStudentId(rs.getLong("STUDENT_ID"));
            dto.setFullName(rs.getString("FULL_NAME"));
            dto.setIndexNumber(rs.getString("INDEX_NUMBER"));
            dto.setTotalSessions(rs.getInt("TOTAL_SESSIONS"));
            dto.setAttended(rs.getInt("ATTENDED"));
            dto.setAbsent(rs.getInt("ABSENT"));
            dto.setPercentage(rs.getDouble("PERCENTAGE"));
            return dto;
        }, courseId, courseId, courseId);
    }

    public List<StudentAttendanceReportDto> getStudentAttendanceReport(Long studentId) {
        String sql = """
                SELECT e.COURSE_ID,
                       c.NAME AS COURSE_NAME,
                       NVL(ts.TOTAL_SESSIONS, 0) AS TOTAL_SESSIONS,
                       NVL(att.ATTENDED, 0) AS ATTENDED,
                       CASE WHEN NVL(ts.TOTAL_SESSIONS, 0) = 0 THEN 0
                            ELSE ROUND(NVL(att.ATTENDED, 0) * 100.0 / ts.TOTAL_SESSIONS, 2)
                       END AS PERCENTAGE
                FROM NBP_ENROLLMENT e
                JOIN NBP_COURSE c ON e.COURSE_ID = c.ID
                LEFT JOIN (
                    SELECT COURSE_ID, COUNT(*) AS TOTAL_SESSIONS
                    FROM NBP_COURSE_SESSION
                    GROUP BY COURSE_ID
                ) ts ON ts.COURSE_ID = e.COURSE_ID
                LEFT JOIN (
                    SELECT cs.COURSE_ID, COUNT(*) AS ATTENDED
                    FROM NBP_ATTENDANCE a
                    JOIN NBP_COURSE_SESSION cs ON a.COURSE_SESSION_ID = cs.ID
                    WHERE a.STUDENT_ID = ? AND a.IS_PRESENT = 1
                    GROUP BY cs.COURSE_ID
                ) att ON att.COURSE_ID = e.COURSE_ID
                WHERE e.STUDENT_ID = ?
                ORDER BY c.NAME
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            StudentAttendanceReportDto dto = new StudentAttendanceReportDto();
            dto.setCourseId(rs.getLong("COURSE_ID"));
            dto.setCourseName(rs.getString("COURSE_NAME"));
            dto.setTotalSessions(rs.getInt("TOTAL_SESSIONS"));
            dto.setAttended(rs.getInt("ATTENDED"));
            dto.setPercentage(rs.getDouble("PERCENTAGE"));
            return dto;
        }, studentId, studentId);
    }
}