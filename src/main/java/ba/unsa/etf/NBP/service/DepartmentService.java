package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Department;
import ba.unsa.etf.NBP.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Optional<Department> findById(Long id) {
        return departmentRepository.findById(id);
    }

    public void save(Department department) {
        departmentRepository.save(department);
    }

    public void update(Department department) {
        departmentRepository.update(department);
    }

    public void deleteById(Long id) {
        departmentRepository.deleteById(id);
    }
}
