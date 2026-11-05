
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String showTransactions(Model model, HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        List<FinancialTransaction> transactions =
                transactionRepository.findAll();

        List<Project> projects = projectRepository.findAll();

        model.addAttribute("transactions", transactions);
        model.addAttribute("projects", projects);
        model.addAttribute("fullName",
                session.getAttribute("loggedInFullName"));
        model.addAttribute("role",
                session.getAttribute("loggedInRole"));

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

        model.addAttribute("projects", projectRepository.findAll());

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
            Model model,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        if (!isValidType(transactionType)) {
            model.addAttribute("error", "Invalid transaction type.");
            model.addAttribute("projects", projectRepository.findAll());
            return "transaction-form";
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("error", "Amount must be greater than zero.");
            model.addAttribute("projects", projectRepository.findAll());
            return "transaction-form";
        }

        if (description == null || description.trim().isEmpty()) {
            model.addAttribute("error", "Description is required.");
            model.addAttribute("projects", projectRepository.findAll());
            return "transaction-form";
        }

        if (transactionDate == null) {
            model.addAttribute("error", "Transaction date is required.");
            model.addAttribute("projects", projectRepository.findAll());
            return "transaction-form";
        }

        Project project = projectRepository.findById(projectId).orElse(null);

        if (project == null) {
            model.addAttribute("error", "Selected project was not found.");
            model.addAttribute("projects", projectRepository.findAll());
            return "transaction-form";
        }

        Budget budget = budgetRepository.findByProjectId(projectId).orElse(null);

        if (budget == null) {
            model.addAttribute("error",
                    "No budget record was found for this project.");
            model.addAttribute("projects", projectRepository.findAll());
            return "transaction-form";
        }

        if ("EXPENSE".equals(transactionType)) {
            BigDecimal remaining = safeAmount(budget.getRemainingBalance());

            if (amount.compareTo(remaining) > 0) {
                model.addAttribute("error",
                        "Expense amount cannot exceed the project's remaining budget.");
                model.addAttribute("projects", projectRepository.findAll());
                return "transaction-form";
            }
        }

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setProject(project);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setDescription(description.trim());
        transaction.setTransactionDate(transactionDate);

        transactionRepository.save(transaction);

        if ("EXPENSE".equals(transactionType)) {
            budget.setRemainingBalance(
                    safeAmount(budget.getRemainingBalance()).subtract(amount));
            budgetRepository.save(budget);
        }

        redirectAttributes.addFlashAttribute(
                "success", "Transaction saved successfully.");

        return "redirect:/transactions";
    }

    // =========================
    // SHOW EDIT FORM
    // =========================

    @GetMapping("/transactions/edit/{id}")
    public String showEditForm(
            @PathVariable("id") Long id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        FinancialTransaction transaction =
                transactionRepository.findById(id).orElse(null);

        if (transaction == null) {
            redirectAttributes.addFlashAttribute(
                    "error", "Transaction was not found.");
            return "redirect:/transactions";
        }

        model.addAttribute("transaction", transaction);
        model.addAttribute("projects", projectRepository.findAll());

        return "transaction-edit";
    }

    // =========================
    // UPDATE TRANSACTION
    // =========================

    @PostMapping("/transactions/update/{id}")
    public String updateTransaction(
            @PathVariable("id") Long id,
            @RequestParam("projectId") Long projectId,
            @RequestParam("transactionType") String transactionType,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("description") String description,
            @RequestParam("transactionDate") LocalDate transactionDate,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        FinancialTransaction transaction =
                transactionRepository.findById(id).orElse(null);

        if (transaction == null) {
            redirectAttributes.addFlashAttribute(
                    "error", "Transaction was not found.");
            return "redirect:/transactions";
        }

        if (!isValidType(transactionType)
                || amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0
                || description == null
                || description.trim().isEmpty()
                || transactionDate == null) {

            model.addAttribute("error",
                    "Please provide a valid transaction type, positive amount, description, and date.");
            model.addAttribute("transaction", transaction);
            model.addAttribute("projects", projectRepository.findAll());

            return "transaction-edit";
        }

        Project newProject =
                projectRepository.findById(projectId).orElse(null);

        if (newProject == null) {
            model.addAttribute("error", "Selected project was not found.");
            model.addAttribute("transaction", transaction);
            model.addAttribute("projects", projectRepository.findAll());

            return "transaction-edit";
        }

        Budget oldBudget = budgetRepository
                .findByProjectId(transaction.getProject().getId())
                .orElse(null);

        Budget newBudget = budgetRepository
                .findByProjectId(newProject.getId())
                .orElse(null);

        if (oldBudget == null || newBudget == null) {
            model.addAttribute("error",
                    "A budget record could not be found for the selected project.");
            model.addAttribute("transaction", transaction);
            model.addAttribute("projects", projectRepository.findAll());

            return "transaction-edit";
        }

        // Restore the old expense temporarily before checking the new amount.
        BigDecimal oldAmount = safeAmount(transaction.getAmount());
        boolean oldWasExpense = "EXPENSE".equals(transaction.getTransactionType());
        boolean newIsExpense = "EXPENSE".equals(transactionType);

        if (oldWasExpense) {
            oldBudget.setRemainingBalance(
                    safeAmount(oldBudget.getRemainingBalance()).add(oldAmount));
        }

        // Check the new expense against the available balance.
        BigDecimal availableBalance;

        if (newIsExpense) {
            availableBalance = safeAmount(newBudget.getRemainingBalance());

            if (oldWasExpense
                    && oldBudget.getId().equals(newBudget.getId())) {
                availableBalance = availableBalance.add(oldAmount);
            }

            if (amount.compareTo(availableBalance) > 0) {
                // Restore the original budget state before returning.
                if (oldWasExpense) {
                    oldBudget.setRemainingBalance(
                            safeAmount(oldBudget.getRemainingBalance()).subtract(oldAmount));
                }

                model.addAttribute("error",
                        "Expense amount cannot exceed the project's remaining budget.");
                model.addAttribute("transaction", transaction);
                model.addAttribute("projects", projectRepository.findAll());

                return "transaction-edit";
            }
        }

        // Save the restored old balance if the transaction was an expense.
        if (oldWasExpense) {
            budgetRepository.save(oldBudget);
        }

        // Deduct the new expense from its selected project.
        if (newIsExpense) {
            BigDecimal currentBalance =
                    safeAmount(newBudget.getRemainingBalance());

            newBudget.setRemainingBalance(currentBalance.subtract(amount));
            budgetRepository.save(newBudget);
        }

        transaction.setProject(newProject);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setDescription(description.trim());
        transaction.setTransactionDate(transactionDate);

        transactionRepository.save(transaction);

        redirectAttributes.addFlashAttribute(
                "success", "Transaction updated successfully.");

        return "redirect:/transactions";
    }

    // =========================
    // DELETE TRANSACTION
    // =========================

    @PostMapping("/transactions/delete/{id}")
    public String deleteTransaction(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        FinancialTransaction transaction =
                transactionRepository.findById(id).orElse(null);

        if (transaction == null) {
            redirectAttributes.addFlashAttribute(
                    "error", "Transaction was not found.");
            return "redirect:/transactions";
        }

        // Return the amount to the budget when deleting an expense.
        if ("EXPENSE".equals(transaction.getTransactionType())) {

            Long projectId = transaction.getProject().getId();

            Budget budget = budgetRepository
                    .findByProjectId(projectId)
                    .orElse(null);

            if (budget != null) {
                BigDecimal restoredBalance =
                        safeAmount(budget.getRemainingBalance())
                                .add(safeAmount(transaction.getAmount()));

                budget.setRemainingBalance(restoredBalance);
                budgetRepository.save(budget);
            }
        }

        transactionRepository.delete(transaction);

        redirectAttributes.addFlashAttribute(
                "success", "Transaction deleted successfully.");

        return "redirect:/transactions";
    }

    // =========================
    // HELPER METHODS
    // =========================

    private boolean isValidType(String transactionType) {
        return "INCOME".equals(transactionType)
                || "EXPENSE".equals(transactionType);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
