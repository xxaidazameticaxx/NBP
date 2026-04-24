package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Department;
import ba.unsa.etf.NBP.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Thin service layer over {@link DepartmentRepository} providing CRUD for departments.
 */
@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    /**
     * Returns every department.
     *
     * @return all departments
     */
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    /**
     * Looks up a department by ID.
     *
     * @param id department ID
     * @return the department, or {@link Optional#empty()} if missing
     */
    public Optional<Department> findById(Long id) {
        return departmentRepository.findById(id);
    }

    /**
     * Inserts a new department row.
     *
     * @param department department to insert
     */
    public void save(Department department) {
        departmentRepository.save(department);
    }

    /**
     * Updates a department row.
     *
     * @param department department with updated fields (ID required)
     */
    public void update(Department department) {
        departmentRepository.update(department);
    }

    /**
     * Deletes a department by ID.
     *
     * @param id department ID
     */
    public void deleteById(Long id) {
        departmentRepository.deleteById(id);
    }
}
