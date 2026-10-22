package com.bfts.controller;

import com.bfts.entity.Budget;
import com.bfts.entity.FinancialTransaction;
import com.bfts.entity.Project;
import com.bfts.repository.BudgetRepository;
import com.bfts.repository.FinancialTransactionRepository;
import com.bfts.repository.ProjectRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class TransactionController {

    @Autowired
    private FinancialTransactionRepository transactionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private BudgetRepository budgetRepository;


    // =========================
    // TRANSACTION LIST
    // =========================

    @GetMapping("/transactions")
    public String showTransactions(
            Model model,
            HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        List<FinancialTransaction> transactions =
                transactionRepository.findAll();

        model.addAttribute(
                "transactions",
                transactions
        );

        model.addAttribute(
                "fullName",
                session.getAttribute("loggedInFullName")
        );

        model.addAttribute(
                "role",
                session.getAttribute("loggedInRole")
        );

        return "transactions";
    }


    // =========================
    // ADD TRANSACTION FORM
    // =========================

    @GetMapping("/transactions/new")
    public String showTransactionForm(
            Model model,
            HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        List<Project> projects =
                projectRepository.findAll();

        model.addAttribute(
                "projects",
                projects
        );

        return "transaction-form";
    }


    // =========================
    // SAVE TRANSACTION
    // =========================

    @PostMapping("/transactions/save")
    public String saveTransaction(
            @RequestParam("projectId") Long projectId,
            @RequestParam("transactionType") String transactionType,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("description") String description,
            @RequestParam("transactionDate") LocalDate transactionDate,
            HttpSession session,
            Model model) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }


        // =========================
        // VALIDATE TRANSACTION TYPE
        // =========================

        if (!transactionType.equals("INCOME")
                && !transactionType.equals("EXPENSE")) {

            model.addAttribute(
                    "error",
                    "Invalid transaction type."
            );

            model.addAttribute(
                    "projects",
                    projectRepository.findAll()
            );

            return "transaction-form";
        }


        // =========================
        // VALIDATE AMOUNT
        // =========================

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            model.addAttribute(
                    "error",
                    "Amount must be greater than zero."
            );

            model.addAttribute(
                    "projects",
                    projectRepository.findAll()
            );

            return "transaction-form";
        }


        // =========================
        // FIND PROJECT
        // =========================

        Project project =
                projectRepository
                        .findById(projectId)
                        .orElse(null);

        if (project == null) {

            model.addAttribute(
                    "error",
                    "Selected project was not found."
            );

            model.addAttribute(
                    "projects",
                    projectRepository.findAll()
            );

            return "transaction-form";
        }


        // =========================
        // FIND PROJECT BUDGET
        // =========================

        Budget budget =
                budgetRepository
                        .findByProjectId(projectId)
                        .orElse(null);

        if (budget == null) {

            model.addAttribute(
                    "error",
                    "No budget record was found for this project."
            );

            model.addAttribute(
                    "projects",
                    projectRepository.findAll()
            );

            return "transaction-form";
        }


        // =========================
        // CHECK EXPENSE AGAINST
        // REMAINING BALANCE
        // =========================

        if (transactionType.equals("EXPENSE")) {

            BigDecimal remainingBalance =
                    budget.getRemainingBalance();

            if (remainingBalance == null) {
                remainingBalance = BigDecimal.ZERO;
            }

            if (amount.compareTo(remainingBalance) > 0) {

                model.addAttribute(
                        "error",
                        "Expense amount cannot exceed the project's remaining budget."
                );

                model.addAttribute(
                        "projects",
                        projectRepository.findAll()
                );

                return "transaction-form";
            }
        }


        // =========================
        // CREATE TRANSACTION
        // =========================

        FinancialTransaction transaction =
                new FinancialTransaction();

        transaction.setProject(project);

        transaction.setTransactionType(
                transactionType
        );

        transaction.setAmount(amount);

        transaction.setDescription(
                description
        );

        transaction.setTransactionDate(
                transactionDate
        );


        // =========================
        // SAVE TRANSACTION
        // =========================

        transactionRepository.save(transaction);


        // =========================
        // UPDATE REMAINING BALANCE
        // =========================

        if (transactionType.equals("EXPENSE")) {

            BigDecimal newRemainingBalance =
                    budget.getRemainingBalance()
                            .subtract(amount);

            budget.setRemainingBalance(
                    newRemainingBalance
            );

            budgetRepository.save(budget);
        }


        return "redirect:/transactions";
    }
}