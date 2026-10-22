package com.bfts.controller;

import com.bfts.entity.Project;
import com.bfts.entity.Budget;
import com.bfts.repository.ProjectRepository;
import com.bfts.repository.BudgetRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private BudgetRepository budgetRepository;


    // =========================
    // PROJECT LIST
    // =========================

    @GetMapping("/projects")
    public String showProjects(
            Model model,
            HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        List<Project> projects =
                projectRepository.findAll();

        model.addAttribute("projects", projects);

        model.addAttribute(
                "fullName",
                session.getAttribute("loggedInFullName")
        );

        model.addAttribute(
                "role",
                session.getAttribute("loggedInRole")
        );

        return "projects";
    }


    // =========================
    // ADD PROJECT FORM
    // =========================

    @GetMapping("/projects/new")
    public String showProjectForm(
            Model model,
            HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "project",
                new Project()
        );

        return "project-form";
    }


    // =========================
    // SAVE NEW PROJECT
    // =========================

    @PostMapping("/projects/save")
    public String saveProject(
            Project project,
            HttpSession session,
            Model model) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        if (project.getProjectCode() == null
                || project.getProjectCode().trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Project code is required."
            );

            return "project-form";
        }

        if (project.getProjectName() == null
                || project.getProjectName().trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Project name is required."
            );

            return "project-form";
        }

        if (project.getAllocatedBudget() == null
                || project.getAllocatedBudget()
                .compareTo(BigDecimal.ZERO) < 0) {

            model.addAttribute(
                    "error",
                    "Allocated budget cannot be negative."
            );

            return "project-form";
        }

        boolean codeExists =
                projectRepository
                        .findAll()
                        .stream()
                        .anyMatch(existingProject ->
                                existingProject
                                        .getProjectCode()
                                        .equalsIgnoreCase(
                                                project.getProjectCode()
                                        )
                        );

        if (codeExists) {

            model.addAttribute(
                    "error",
                    "Project code already exists."
            );

            return "project-form";
        }

        // =========================
        // SAVE PROJECT
        // =========================

        projectRepository.save(project);


        // =========================
        // CREATE BUDGET RECORD
        // =========================

        Budget budget = new Budget();

        budget.setProject(project);

        budget.setTotalAllocated(
                project.getAllocatedBudget()
        );

        budget.setRemainingBalance(
                project.getAllocatedBudget()
        );

        budget.setFiscalYear(
                java.time.Year.now().getValue()
        );

        budgetRepository.save(budget);


        return "redirect:/dashboard";
    }


    // =========================
    // EDIT PROJECT FORM
    // =========================

    @GetMapping("/projects/edit/{id}")
    public String showEditProjectForm(
            @PathVariable Long id,
            Model model,
            HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        Project project =
                projectRepository.findById(id)
                        .orElse(null);

        if (project == null) {
            return "redirect:/projects";
        }

        model.addAttribute(
                "project",
                project
        );

        return "project-edit";
    }


    // =========================
    // UPDATE PROJECT
    // =========================

    @PostMapping("/projects/update/{id}")
    public String updateProject(
            @PathVariable Long id,
            Project updatedProject,
            HttpSession session,
            Model model) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        Project existingProject =
                projectRepository.findById(id)
                        .orElse(null);

        if (existingProject == null) {
            return "redirect:/projects";
        }

        if (updatedProject.getProjectCode() == null
                || updatedProject.getProjectCode().trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Project code is required."
            );

            model.addAttribute(
                    "project",
                    existingProject
            );

            return "project-edit";
        }

        if (updatedProject.getProjectName() == null
                || updatedProject.getProjectName().trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Project name is required."
            );

            model.addAttribute(
                    "project",
                    existingProject
            );

            return "project-edit";
        }

        if (updatedProject.getAllocatedBudget() == null
                || updatedProject.getAllocatedBudget()
                .compareTo(BigDecimal.ZERO) < 0) {

            model.addAttribute(
                    "error",
                    "Allocated budget cannot be negative."
            );

            model.addAttribute(
                    "project",
                    existingProject
            );

            return "project-edit";
        }

        boolean codeExists =
                projectRepository
                        .findAll()
                        .stream()
                        .anyMatch(project ->
                                !project.getId().equals(id)
                                && project.getProjectCode()
                                        .equalsIgnoreCase(
                                                updatedProject
                                                        .getProjectCode()
                                        )
                        );

        if (codeExists) {

            model.addAttribute(
                    "error",
                    "Project code already exists."
            );

            model.addAttribute(
                    "project",
                    existingProject
            );

            return "project-edit";
        }


        // =========================
        // UPDATE PROJECT
        // =========================

        existingProject.setProjectCode(
                updatedProject.getProjectCode()
        );

        existingProject.setProjectName(
                updatedProject.getProjectName()
        );

        existingProject.setDescription(
                updatedProject.getDescription()
        );

        existingProject.setAllocatedBudget(
                updatedProject.getAllocatedBudget()
        );

        existingProject.setStatus(
                updatedProject.getStatus()
        );

        projectRepository.save(existingProject);


        // =========================
        // UPDATE BUDGET RECORD
        // =========================

        Budget budget =
                budgetRepository
                        .findByProjectId(id)
                        .orElse(null);

        if (budget != null) {

            BigDecimal oldAllocated =
                    budget.getTotalAllocated();

            BigDecimal newAllocated =
                    updatedProject.getAllocatedBudget();

            BigDecimal difference =
                    newAllocated.subtract(oldAllocated);

            budget.setTotalAllocated(
                    newAllocated
            );

            budget.setRemainingBalance(
                    budget.getRemainingBalance()
                            .add(difference)
            );

            budgetRepository.save(budget);
        }


        return "redirect:/projects";
    }
}