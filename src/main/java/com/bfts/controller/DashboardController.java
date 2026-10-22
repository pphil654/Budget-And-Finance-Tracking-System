package com.bfts.controller;

import com.bfts.entity.Project;
import com.bfts.repository.ProjectRepository;
import com.bfts.repository.FinancialTransactionRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FinancialTransactionRepository transactionRepository;

    @GetMapping("/dashboard")
    public String showDashboard(
            Model model,
            HttpSession session) {

        // Check if user is logged in
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        // Get all projects
        List<Project> projects = projectRepository.findAll();

        // Calculate total allocated budget
        BigDecimal totalBudget = BigDecimal.ZERO;

        for (Project p : projects) {
            if (p.getAllocatedBudget() != null) {
                totalBudget = totalBudget.add(p.getAllocatedBudget());
            }
        }

        // Get total expenses
        BigDecimal totalExpenses =
                transactionRepository.getTotalExpenses();

        // Calculate remaining funds
        BigDecimal remainingFunds =
                totalBudget.subtract(totalExpenses);

        // Calculate overall budget utilization
        BigDecimal budgetUtilization = BigDecimal.ZERO;

        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {

            budgetUtilization = totalExpenses
                    .divide(totalBudget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        /*
         * ==========================================
         * PROJECT BUDGET ALERTS
         * ==========================================
         */

        Map<Long, BigDecimal> projectExpenses = new HashMap<>();
        Map<Long, BigDecimal> projectUtilization = new HashMap<>();
        Map<Long, String> projectAlerts = new HashMap<>();

        for (Project project : projects) {

            BigDecimal projectBudget =
                    project.getAllocatedBudget();

            if (projectBudget == null) {
                projectBudget = BigDecimal.ZERO;
            }

            BigDecimal expenses =
                    transactionRepository
                            .getTotalExpensesByProjectId(project.getId());

            projectExpenses.put(project.getId(), expenses);

            BigDecimal utilization = BigDecimal.ZERO;

            if (projectBudget.compareTo(BigDecimal.ZERO) > 0) {

                utilization = expenses
                        .divide(
                                projectBudget,
                                4,
                                RoundingMode.HALF_UP
                        )
                        .multiply(BigDecimal.valueOf(100));
            }

            projectUtilization.put(
                    project.getId(),
                    utilization
            );

            /*
             * Determine alert level
             */

            String alert;

            if (utilization.compareTo(BigDecimal.valueOf(100)) >= 0) {

                alert = "OVERSPENDING";

            } else if (utilization.compareTo(BigDecimal.valueOf(90)) >= 0) {

                alert = "CRITICAL";

            } else if (utilization.compareTo(BigDecimal.valueOf(80)) >= 0) {

                alert = "WARNING";

            } else {

                alert = "NORMAL";
            }

            projectAlerts.put(
                    project.getId(),
                    alert
            );
        }

        // Send project information to dashboard
        model.addAttribute("projects", projects);
        model.addAttribute("totalProjects", projects.size());

        // Send overall financial calculations
        model.addAttribute("totalBudget", totalBudget);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("remainingFunds", remainingFunds);
        model.addAttribute("budgetUtilization", budgetUtilization);

        // Send project financial information
        model.addAttribute("projectExpenses", projectExpenses);
        model.addAttribute("projectUtilization", projectUtilization);
        model.addAttribute("projectAlerts", projectAlerts);

        // Send logged-in user information
        model.addAttribute(
                "username",
                session.getAttribute("loggedInUsername")
        );

        model.addAttribute(
                "fullName",
                session.getAttribute("loggedInFullName")
        );

        model.addAttribute(
                "role",
                session.getAttribute("loggedInRole")
        );

        return "dashboard";
    }
}