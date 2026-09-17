package com.examlifecycle.controller;

import com.examlifecycle.domain.Course;
import com.examlifecycle.domain.Role;
import com.examlifecycle.dto.CatalogDtos.CourseRequest;
import com.examlifecycle.security.AuthUtil;
import com.examlifecycle.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Course create(@Valid @RequestBody CourseRequest req) {
        var user = AuthUtil.current();
        AuthUtil.requireRole(user, Role.PAPER_SETTER);
        return courseService.create(user, req);
    }

    @GetMapping
    public List<Course> list() {
        AuthUtil.current(); // any authenticated user may browse courses
        return courseService.listAll();
    }
}
