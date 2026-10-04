package com.digibank.controller;

import com.digibank.dto.loan.LoanApplicationRequest;
import com.digibank.dto.loan.LoanRepaymentRequest;
import com.digibank.exception.LoanException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.LoanManagementService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import com.digibank.enums.LoanStatus;
import com.digibank.util.PageSupport;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Locale;

@Controller
public class CustomerLoanController {

    // Service layer used to handle loan-related operations
    private final LoanManagementService loanService;

    public CustomerLoanController(LoanManagementService loanService) {
        this.loanService = loanService;
    }

    // Display customer's loan list with search, status filter and pagination
    @GetMapping("/customer/loans")
    public String loans(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required=false) String q,
            @RequestParam(required=false) LoanStatus status,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="9") int size,
            Model model) {

        // Get search query and customer's loans
        String query = PageSupport.query(q);

        var filtered = loanService.getCustomerLoans(userDetails.getUserId())
                // Filter loans by selected status
                .stream()
                .filter(l -> status == null || l.status() == status)

                // Filter loans by application number or loan type
                .filter(l -> query.isEmpty()
                        || l.applicationNumber().toLowerCase(Locale.ROOT).contains(query)
                        || l.loanType().getDisplayName().toLowerCase(Locale.ROOT).contains(query))
                .toList();

        // Divide loan list into pages
        var result = PageSupport.page(filtered, page, size);

        // Send loan data to the HTML page
        model.addAttribute("loans", result.getContent());
        model.addAttribute("loansPage", result);
        model.addAttribute("query", q == null ? "" : q.trim());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("loanStatuses", LoanStatus.values());

        return "customer/loans/list";
    }


    // Open the new loan application form
    @GetMapping("/customer/loans/new")
    public String newLoan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        // Create an empty loan application request
        if (!model.containsAttribute("loanApplicationRequest")) {
            model.addAttribute("loanApplicationRequest",
                    new LoanApplicationRequest());
        }

        // Load data required for the application form
        model.addAttribute("form",
                loanService.getApplicationForm(userDetails.getUserId()));

        return "customer/loans/apply";
    }


    // Submit a new loan application
    @PostMapping("/customer/loans")
    public String apply(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute LoanApplicationRequest loanApplicationRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Check validation errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("form",
                    loanService.getApplicationForm(userDetails.getUserId()));

            return "customer/loans/apply";
        }

        try {
            // Send application to service layer
            var loan = loanService.apply(
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    loanApplicationRequest);

            // Show success message
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Loan application submitted successfully.");

            // Go to loan details page
            return "redirect:/customer/loans/" + loan.applicationNumber();

        } catch (LoanException ex) {

            // Show error if application fails
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("form",
                    loanService.getApplicationForm(userDetails.getUserId()));

            return "customer/loans/apply";
        }
    }


    // Open a pending loan application for editing
    @GetMapping("/customer/loans/{applicationNumber}/edit")
    public String edit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String applicationNumber,
            Model model,
            RedirectAttributes redirect) {

        try {

            // Get existing pending application
            if (!model.containsAttribute("loanApplicationRequest")) {
                model.addAttribute(
                        "loanApplicationRequest",
                        loanService.getPendingApplicationForEdit(
                                userDetails.getUserId(),
                                applicationNumber));
            }

            // Load form data
            model.addAttribute(
                    "form",
                    loanService.getApplicationForm(
                            userDetails.getUserId()));

            model.addAttribute("applicationNumber", applicationNumber);

            return "customer/loans/edit";

        } catch (LoanException ex) {

            // Redirect if application cannot be edited
            redirect.addFlashAttribute("errorMessage", ex.getMessage());

            return "redirect:/customer/loans/" + applicationNumber;
        }
    }


    // Download the document attached to a loan application
    @GetMapping("/customer/loans/{applicationNumber}/document")
    public ResponseEntity<byte[]> document(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String applicationNumber) {

        // Get customer's loan document
        var document = loanService.getCustomerDocument(
                userDetails.getUserId(),
                applicationNumber);

        // Return document as a downloadable response
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(
                                        document.filename(),
                                        StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        document.contentType())
                .body(document.content());
    }


    // Update a pending loan application
    @PostMapping("/customer/loans/{applicationNumber}/edit")
    public String update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String applicationNumber,
            @Valid @ModelAttribute LoanApplicationRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {

        // Check validation errors
        if (result.hasErrors()) {

            model.addAttribute(
                    "form",
                    loanService.getApplicationForm(
                            userDetails.getUserId()));

            model.addAttribute(
                    "applicationNumber",
                    applicationNumber);

            return "customer/loans/edit";
        }

        try {

            // Update application through service
            loanService.updatePendingApplication(
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    applicationNumber,
                    request);

            redirect.addFlashAttribute(
                    "successMessage",
                    "Loan application updated.");

        } catch (LoanException ex) {

            // Show update error
            redirect.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage());
        }

        return "redirect:/customer/loans/" + applicationNumber;
    }


    // Withdraw a pending loan application
    @PostMapping("/customer/loans/{applicationNumber}/withdraw")
    public String withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String applicationNumber,
            RedirectAttributes redirect) {

        try {

            // Withdraw the selected loan application
            loanService.withdrawPendingApplication(
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    applicationNumber);

            redirect.addFlashAttribute(
                    "successMessage",
                    "Loan application withdrawn.");

        } catch (LoanException ex) {

            // Show error if withdrawal fails
            redirect.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage());
        }

        return "redirect:/customer/loans/" + applicationNumber;
    }


    // Display details of a specific loan
    @GetMapping("/customer/loans/{applicationNumber}")
    public String details(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String applicationNumber,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            // Get selected loan details
            model.addAttribute(
                    "loan",
                    loanService.getCustomerLoan(
                            userDetails.getUserId(),
                            applicationNumber));

            // Get accounts available for loan repayment
            model.addAttribute(
                    "repaymentAccounts",
                    loanService.getRepaymentAccounts(
                            userDetails.getUserId()));

            return "customer/loans/details";

        } catch (LoanException ex) {

            // Redirect if loan cannot be found
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage());

            return "redirect:/customer/loans";
        }
    }


    // Make a payment for a specific loan installment
    @PostMapping(
            "/customer/loans/{applicationNumber}/repayments/{installmentNumber}")
    public String repay(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String applicationNumber,
            @PathVariable int installmentNumber,
            @Valid @ModelAttribute LoanRepaymentRequest loanRepaymentRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        // Check repayment validation errors
        if (bindingResult.hasErrors()) {

            String message = bindingResult.getAllErrors()
                    .getFirst()
                    .getDefaultMessage();

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    message);

            return "redirect:/customer/loans/" + applicationNumber;
        }

        try {

            // Pay the selected installment
            String reference = loanService.payInstallment(
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    applicationNumber,
                    installmentNumber,
                    loanRepaymentRequest);

            // Show payment success and reference number
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Installment paid successfully. Reference: " + reference);

        } catch (LoanException ex) {

            // Show payment error
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage());
        }

        return "redirect:/customer/loans/" + applicationNumber;
    }
}
