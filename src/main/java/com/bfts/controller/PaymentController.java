package com.bfts.controller;

import com.bfts.entity.Payment;
import com.bfts.entity.Project;
import com.bfts.repository.PaymentRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private static final List<String> VALID_STATUSES =
            Arrays.asList("PENDING", "COMPLETED", "CANCELLED");

    private static final List<String> VALID_PAYMENT_METHODS =
            Arrays.asList("CASH", "BANK_TRANSFER", "GCASH", "CHECK", "OTHER");

    // DISPLAY PAYMENTS WITH SEARCH AND FILTERS
    @GetMapping("/payments")
    public String showPayments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            Model model,
            HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        List<Payment> allPayments = paymentRepository.findAll();

        List<Payment> filteredPayments = allPayments.stream()
                .filter(payment -> {
                    if (keyword == null || keyword.isBlank()) {
                        return true;
                    }

                    String searchKeyword = keyword.trim()
                            .toLowerCase(Locale.ROOT);

                    String projectName = "";

                    if (payment.getProject() != null
                            && payment.getProject().getProjectName() != null) {
                        projectName = payment.getProject()
                                .getProjectName()
                                .toLowerCase(Locale.ROOT);
                    }

                    String referenceNumber = payment.getReferenceNumber() != null
                            ? payment.getReferenceNumber().toLowerCase(Locale.ROOT)
                            : "";

                    return projectName.contains(searchKeyword)
                            || referenceNumber.contains(searchKeyword);
                })
                .filter(payment -> {
                    if (status == null || status.isBlank()
                            || status.equalsIgnoreCase("ALL")) {
                        return true;
                    }

                    return payment.getStatus() != null
                            && payment.getStatus().equalsIgnoreCase(status.trim());
                })
                .filter(payment -> {
                    if (paymentMethod == null || paymentMethod.isBlank()
                            || paymentMethod.equalsIgnoreCase("ALL")) {
                        return true;
                    }

                    return payment.getPaymentMethod() != null
                            && payment.getPaymentMethod()
                                    .equalsIgnoreCase(paymentMethod.trim());
                })
                .collect(Collectors.toList());

        long completedPayments = allPayments.stream()
                .filter(payment -> payment.getStatus() != null
                        && payment.getStatus().equalsIgnoreCase("COMPLETED"))
                .count();

        long pendingPayments = allPayments.stream()
                .filter(payment -> payment.getStatus() != null
                        && payment.getStatus().equalsIgnoreCase("PENDING"))
                .count();

        model.addAttribute("payments", filteredPayments);
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("payment", new Payment());

        model.addAttribute("totalPayments", allPayments.size());
        model.addAttribute("completedPayments", completedPayments);
        model.addAttribute("pendingPayments", pendingPayments);

        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("selectedStatus", status == null ? "ALL" : status);
        model.addAttribute(
                "selectedPaymentMethod",
                paymentMethod == null ? "ALL" : paymentMethod
        );

        model.addAttribute(
                "fullName",
                session.getAttribute("loggedInFullName")
        );

        model.addAttribute(
                "role",
                session.getAttribute("loggedInRole")
        );

        return "payments";
    }

    // ADD PAYMENT
    @PostMapping("/payments/add")
    public String addPayment(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "amount", required = false) BigDecimal amount,
            @RequestParam(value = "paymentDate", required = false) LocalDate paymentDate,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "referenceNumber", required = false) String referenceNumber,
            @RequestParam(value = "description", required = false) String description,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        if (projectId == null
                || amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0
                || paymentDate == null
                || !isValidPaymentMethod(paymentMethod)) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please provide a project, a valid amount greater than zero, a payment date, and a valid payment method."
            );

            return "redirect:/payments";
        }

        Project project = projectRepository.findById(projectId).orElse(null);

        if (project == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "The selected project could not be found."
            );

            return "redirect:/payments";
        }

        Payment payment = new Payment();
        payment.setProject(project);
        payment.setAmount(amount);
        payment.setPaymentDate(paymentDate);
        payment.setPaymentMethod(normalizePaymentMethod(paymentMethod));
        payment.setReferenceNumber(cleanText(referenceNumber));
        payment.setDescription(cleanText(description));
        payment.setStatus("PENDING");

        paymentRepository.save(payment);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Payment record added successfully."
        );

        return "redirect:/payments";
    }

    // OPEN EDIT PAYMENT PAGE
    @GetMapping("/payments/edit/{id}")
    public String editPayment(
            @PathVariable("id") Long id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        Payment payment = paymentRepository.findById(id).orElse(null);

        if (payment == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Payment record not found."
            );

            return "redirect:/payments";
        }

        model.addAttribute("payment", payment);
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("fullName", session.getAttribute("loggedInFullName"));
        model.addAttribute("role", session.getAttribute("loggedInRole"));

        return "edit-payment";
    }

    // UPDATE PAYMENT
    @PostMapping("/payments/update/{id}")
    public String updatePayment(
            @PathVariable("id") Long id,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "amount", required = false) BigDecimal amount,
            @RequestParam(value = "paymentDate", required = false) LocalDate paymentDate,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "referenceNumber", required = false) String referenceNumber,
            @RequestParam(value = "description", required = false) String description,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        Payment payment = paymentRepository.findById(id).orElse(null);

        if (payment == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Payment record not found."
            );

            return "redirect:/payments";
        }

        String normalizedStatus = status == null
                ? ""
                : status.trim().toUpperCase(Locale.ROOT);

        if (projectId == null
                || amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0
                || paymentDate == null
                || !isValidPaymentMethod(paymentMethod)
                || !VALID_STATUSES.contains(normalizedStatus)) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please provide valid payment information. Amount must be greater than zero, and the payment method and status must be valid."
            );

            return "redirect:/payments/edit/" + id;
        }

        Project project = projectRepository.findById(projectId).orElse(null);

        if (project == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "The selected project could not be found."
            );

            return "redirect:/payments/edit/" + id;
        }

        payment.setProject(project);
        payment.setAmount(amount);
        payment.setPaymentDate(paymentDate);
        payment.setPaymentMethod(normalizePaymentMethod(paymentMethod));
        payment.setStatus(normalizedStatus);
        payment.setReferenceNumber(cleanText(referenceNumber));
        payment.setDescription(cleanText(description));

        paymentRepository.save(payment);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Payment record updated successfully."
        );

        return "redirect:/payments";
    }

    // DELETE PAYMENT
    @PostMapping("/payments/delete/{id}")
    public String deletePayment(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        Payment payment = paymentRepository.findById(id).orElse(null);

        if (payment == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Payment record not found."
            );

            return "redirect:/payments";
        }

        paymentRepository.delete(payment);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Payment record deleted successfully."
        );

        return "redirect:/payments";
    }

    // VALIDATE PAYMENT METHOD
    private boolean isValidPaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return false;
        }

        return VALID_PAYMENT_METHODS.contains(
                paymentMethod.trim().toUpperCase(Locale.ROOT)
        );
    }

    // NORMALIZE PAYMENT METHOD
    private String normalizePaymentMethod(String paymentMethod) {
        return paymentMethod.trim().toUpperCase(Locale.ROOT);
    }

    // CLEAN OPTIONAL TEXT FIELDS
    private String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
