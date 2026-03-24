package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Student;
import ba.unsa.etf.NBP.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    public void save(Student student) {
        studentRepository.save(student);
    }

    public void update(Student student) {
        studentRepository.update(student);
    }

    public void deleteById(Long id) {
        studentRepository.deleteById(id);
    }
}