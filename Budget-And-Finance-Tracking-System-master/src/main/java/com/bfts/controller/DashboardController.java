
package com.bfts.controller;

import com.bfts.entity.Budget;
import com.bfts.entity.Project;
import com.bfts.repository.BudgetRepository;
import com.bfts.repository.FinancialTransactionRepository;
import com.bfts.repository.ProjectRepository;

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
    private BudgetRepository budgetRepository;

    @Autowired
    private FinancialTransactionRepository transactionRepository;

    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpSession session) {

        // Check if the user is logged in.
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        List<Project> projects = projectRepository.findAll();

        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal remainingFunds = BigDecimal.ZERO;

        Map<Long, BigDecimal> projectExpenses = new HashMap<>();
        Map<Long, BigDecimal> projectUtilization = new HashMap<>();
        Map<Long, String> projectAlerts = new HashMap<>();

        for (Project project : projects) {

            BigDecimal allocatedBudget = project.getAllocatedBudget();

            if (allocatedBudget == null) {
                allocatedBudget = BigDecimal.ZERO;
            }

            // Get the project's budget record.
            Budget budget = budgetRepository
                    .findByProjectId(project.getId())
                    .orElse(null);

            BigDecimal projectBudget = allocatedBudget;
            BigDecimal projectRemainingBalance;

            if (budget != null) {

                if (budget.getTotalAllocated() != null) {
                    projectBudget = budget.getTotalAllocated();
                }

                projectRemainingBalance = budget.getRemainingBalance();

                if (projectRemainingBalance == null) {
                    projectRemainingBalance = BigDecimal.ZERO;
                }

            } else {

                // Calculate the remaining balance if no budget record exists.
                BigDecimal expenses = transactionRepository
                        .getTotalExpensesByProjectId(project.getId());

                if (expenses == null) {
                    expenses = BigDecimal.ZERO;
                }

                projectRemainingBalance = projectBudget.subtract(expenses);
            }

            totalBudget = totalBudget.add(projectBudget);
            remainingFunds = remainingFunds.add(projectRemainingBalance);

            // Get the project's total expenses.
            BigDecimal expenses = transactionRepository
                    .getTotalExpensesByProjectId(project.getId());

            if (expenses == null) {
                expenses = BigDecimal.ZERO;
            }

            projectExpenses.put(project.getId(), expenses);

            // Calculate project budget utilization.
            BigDecimal utilization = BigDecimal.ZERO;

            if (projectBudget.compareTo(BigDecimal.ZERO) > 0) {
                utilization = expenses
                        .divide(projectBudget, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            projectUtilization.put(project.getId(), utilization);

            // Determine the project's budget alert.
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

            projectAlerts.put(project.getId(), alert);
        }

        // Calculate all recorded expenses.
        BigDecimal totalExpenses = transactionRepository.getTotalExpenses();

        if (totalExpenses == null) {
            totalExpenses = BigDecimal.ZERO;
        }

        // Calculate overall budget utilization.
        BigDecimal budgetUtilization = BigDecimal.ZERO;

        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            budgetUtilization = totalExpenses
                    .divide(totalBudget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Send data to dashboard.html.
        model.addAttribute("projects", projects);
        model.addAttribute("totalProjects", projects.size());

        model.addAttribute("totalBudget", totalBudget);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("remainingFunds", remainingFunds);
        model.addAttribute("budgetUtilization", budgetUtilization);

        model.addAttribute("projectExpenses", projectExpenses);
        model.addAttribute("projectUtilization", projectUtilization);
        model.addAttribute("projectAlerts", projectAlerts);

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
