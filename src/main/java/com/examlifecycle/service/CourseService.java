package com.examlifecycle.service;

import com.examlifecycle.domain.Course;
import com.examlifecycle.dto.CatalogDtos.CourseRequest;
import com.examlifecycle.repository.CourseRepository;
import com.examlifecycle.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final AuditService auditService;

    public CourseService(CourseRepository courseRepository, AuditService auditService) {
        this.courseRepository = courseRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Course create(CurrentUser user, CourseRequest req) {
        Course course = courseRepository.save(new Course(req.code(), req.name(), user.id()));
        auditService.record(user.id(), "CREATE_COURSE", "course", course.getId(), java.util.Map.of("code", req.code()));
        return course;
    }

    public List<Course> listAll() {
        return courseRepository.findAll();
    }
}
