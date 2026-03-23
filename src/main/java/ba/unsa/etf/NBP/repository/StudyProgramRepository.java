package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.StudyProgram;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StudyProgramRepository {

    private final JdbcTemplate jdbcTemplate;

    public StudyProgramRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<StudyProgram> rowMapper = (rs, rowNum) -> {
        StudyProgram studyProgram = new StudyProgram();
        studyProgram.setId(rs.getLong("ID"));
        studyProgram.setName(rs.getString("NAME"));
        studyProgram.setCode(rs.getString("CODE"));
        studyProgram.setDepartmentId(rs.getLong("DEPARTMENT_ID"));
        studyProgram.setDurationYears(rs.getObject("DURATION_YEARS", Long.class));
        studyProgram.setDegreeType(rs.getString("DEGREE_TYPE"));
        return studyProgram;
    };

    public List<StudyProgram> findAll() {
        String sql = "SELECT * FROM NBP_STUDY_PROGRAM";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<StudyProgram> findById(Long id) {
        String sql = "SELECT * FROM NBP_STUDY_PROGRAM WHERE ID = ?";
        List<StudyProgram> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public void save(StudyProgram studyProgram) {
        String sql = "INSERT INTO NBP_STUDY_PROGRAM (ID, NAME, CODE, DEPARTMENT_ID, DURATION_YEARS, DEGREE_TYPE) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                studyProgram.getId(),
                studyProgram.getName(),
                studyProgram.getCode(),
                studyProgram.getDepartmentId(),
                studyProgram.getDurationYears(),
                studyProgram.getDegreeType());
    }

    public void update(StudyProgram studyProgram) {
        String sql = "UPDATE NBP_STUDY_PROGRAM SET NAME = ?, CODE = ?, DEPARTMENT_ID = ?, DURATION_YEARS = ?, DEGREE_TYPE = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                studyProgram.getName(),
                studyProgram.getCode(),
                studyProgram.getDepartmentId(),
                studyProgram.getDurationYears(),
                studyProgram.getDegreeType(),
                studyProgram.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_STUDY_PROGRAM WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }
}
