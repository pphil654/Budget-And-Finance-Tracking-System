package com.bfts.controller;

import com.bfts.entity.Budget;
import com.bfts.entity.Project;
import com.bfts.repository.BudgetRepository;
import com.bfts.repository.FinancialTransactionRepository;
import com.bfts.repository.ProjectRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ReportController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private FinancialTransactionRepository transactionRepository;

    @GetMapping("/reports")
    public String showReports(Model model, HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        List<Project> projects = projectRepository.findAll();
        List<ProjectReport> projectReports = new ArrayList<>();

        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalExpenses = transactionRepository.getTotalExpenses();

        if (totalExpenses == null) {
            totalExpenses = BigDecimal.ZERO;
        }

        BigDecimal remainingFunds = BigDecimal.ZERO;

        for (Project project : projects) {

            BigDecimal allocatedBudget = project.getAllocatedBudget();

            if (allocatedBudget == null) {
                allocatedBudget = BigDecimal.ZERO;
            }

            Budget budget = budgetRepository
                    .findByProjectId(project.getId())
                    .orElse(null);

            BigDecimal projectBudget = allocatedBudget;
            BigDecimal remainingBalance;

            if (budget != null) {

                if (budget.getTotalAllocated() != null) {
                    projectBudget = budget.getTotalAllocated();
                }

                remainingBalance = budget.getRemainingBalance();

                if (remainingBalance == null) {
                    remainingBalance = BigDecimal.ZERO;
                }

            } else {

                BigDecimal expenses = transactionRepository
                        .getTotalExpensesByProjectId(project.getId());

                if (expenses == null) {
                    expenses = BigDecimal.ZERO;
                }

                remainingBalance = projectBudget.subtract(expenses);
            }

            BigDecimal projectExpenses = transactionRepository
                    .getTotalExpensesByProjectId(project.getId());

            if (projectExpenses == null) {
                projectExpenses = BigDecimal.ZERO;
            }

            BigDecimal utilization = BigDecimal.ZERO;

            if (projectBudget.compareTo(BigDecimal.ZERO) > 0) {
                utilization = projectExpenses
                        .multiply(BigDecimal.valueOf(100))
                        .divide(projectBudget, 2, RoundingMode.HALF_UP);
            }

            projectReports.add(new ProjectReport(
                    project.getProjectCode(),
                    project.getProjectName(),
                    projectBudget,
                    projectExpenses,
                    remainingBalance,
                    utilization
            ));

            totalBudget = totalBudget.add(projectBudget);
            remainingFunds = remainingFunds.add(remainingBalance);
        }

        BigDecimal overallUtilization = BigDecimal.ZERO;

        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            overallUtilization = totalExpenses
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalBudget, 2, RoundingMode.HALF_UP);
        }

        model.addAttribute("projectReports", projectReports);
        model.addAttribute("totalProjects", projects.size());
        model.addAttribute("totalBudget", totalBudget);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("remainingFunds", remainingFunds);
        model.addAttribute("overallUtilization", overallUtilization);
        model.addAttribute(
                "fullName",
                session.getAttribute("loggedInFullName")
        );
        model.addAttribute(
                "role",
                session.getAttribute("loggedInRole")
        );

        return "reports";
    }

    @GetMapping("/reports/export")
    public void exportReports(
            HttpSession session,
            HttpServletResponse response
    ) throws IOException {

        if (session.getAttribute("loggedInUserId") == null) {
            response.sendRedirect("/login");
            return;
        }

        List<Project> projects = projectRepository.findAll();

        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"project-track-financial-reports.csv\""
        );

        // Use only the response writer; do not mix it with getOutputStream().
        response.getOutputStream().write(
                new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}
        );

        PrintWriter writer = new PrintWriter(
                new java.io.OutputStreamWriter(
                        response.getOutputStream(),
                        StandardCharsets.UTF_8
                )
        );

        writer.println(
                "Project Code,Project Name,Allocated Budget," +
                "Total Expenses,Remaining Funds,Utilization (%),Status"
        );

        for (Project project : projects) {

            BigDecimal allocatedBudget = project.getAllocatedBudget();

            if (allocatedBudget == null) {
                allocatedBudget = BigDecimal.ZERO;
            }

            Budget budget = budgetRepository
                    .findByProjectId(project.getId())
                    .orElse(null);

            BigDecimal projectBudget = allocatedBudget;
            BigDecimal remainingBalance;

            if (budget != null) {

                if (budget.getTotalAllocated() != null) {
                    projectBudget = budget.getTotalAllocated();
                }

                remainingBalance = budget.getRemainingBalance();

                if (remainingBalance == null) {
                    remainingBalance = BigDecimal.ZERO;
                }

            } else {

                remainingBalance = projectBudget;
            }

            BigDecimal projectExpenses = transactionRepository
                    .getTotalExpensesByProjectId(project.getId());

            if (projectExpenses == null) {
                projectExpenses = BigDecimal.ZERO;
            }

            if (budget == null) {
                remainingBalance = projectBudget.subtract(projectExpenses);
            }

            BigDecimal utilization = BigDecimal.ZERO;

            if (projectBudget.compareTo(BigDecimal.ZERO) > 0) {
                utilization = projectExpenses
                        .multiply(BigDecimal.valueOf(100))
                        .divide(projectBudget, 2, RoundingMode.HALF_UP);
            }

            String status;

            if (utilization.compareTo(BigDecimal.valueOf(80)) < 0) {
                status = "NORMAL";
            } else if (utilization.compareTo(BigDecimal.valueOf(90)) < 0) {
                status = "WARNING";
            } else if (utilization.compareTo(BigDecimal.valueOf(100)) < 0) {
                status = "CRITICAL";
            } else {
                status = "OVERSPENDING";
            }

            writer.println(
                    csv(project.getProjectCode()) + "," +
                    csv(project.getProjectName()) + "," +
                    projectBudget.toPlainString() + "," +
                    projectExpenses.toPlainString() + "," +
                    remainingBalance.toPlainString() + "," +
                    utilization.toPlainString() + "," +
                    csv(status)
            );
        }

        writer.flush();
    }

    private String csv(String value) {

        if (value == null) {
            return "\"\"";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public static class ProjectReport {

        private final String projectCode;
        private final String projectName;
        private final BigDecimal budget;
        private final BigDecimal expenses;
        private final BigDecimal remainingBalance;
        private final BigDecimal utilization;

        public ProjectReport(
                String projectCode,
                String projectName,
                BigDecimal budget,
                BigDecimal expenses,
                BigDecimal remainingBalance,
                BigDecimal utilization
        ) {
            this.projectCode = projectCode;
            this.projectName = projectName;
            this.budget = budget;
            this.expenses = expenses;
            this.remainingBalance = remainingBalance;
            this.utilization = utilization;
        }

        public String getProjectCode() {
            return projectCode;
        }

        public String getProjectName() {
            return projectName;
        }

        public BigDecimal getBudget() {
            return budget;
        }

        public BigDecimal getExpenses() {
            return expenses;
        }

        public BigDecimal getRemainingBalance() {
            return remainingBalance;
        }

        public BigDecimal getUtilization() {
            return utilization;
        }
    }
}
