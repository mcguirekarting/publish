package com.manhattan.reconciliation.controller;

import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.model.ReconciliationStatus;
import com.manhattan.reconciliation.model.SystemType;
import com.manhattan.reconciliation.service.ReconciliationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the reconciliation dashboard and UI-based operations.
 */
@Controller
@RequestMapping("/dashboard")
public class ReconciliationDashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationDashboardController.class);
    
    private final ReconciliationService reconciliationService;
    
    @Autowired
    public ReconciliationDashboardController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }
    
    /**
     * Displays the main dashboard.
     */
    @GetMapping
    public String showDashboard(Model model) {
        List<ReconciliationResult> pendingReconciliations = reconciliationService.getPendingReconciliations();
        
        // Get recently processed (non-pending) reconciliations
        List<ReconciliationResult> allReconciliations = reconciliationService.getReconciliationsByStatus(ReconciliationStatus.APPROVED);
        allReconciliations.addAll(reconciliationService.getReconciliationsByStatus(ReconciliationStatus.REJECTED));
        allReconciliations.addAll(reconciliationService.getReconciliationsByStatus(ReconciliationStatus.AUTO_RESOLVED));
        
        // Sort by reconciliation time (most recent first) and limit to 10
        List<ReconciliationResult> recentlyProcessed = allReconciliations.stream()
                .sorted((r1, r2) -> r2.getReconciliationTime().compareTo(r1.getReconciliationTime()))
                .limit(10)
                .toList();
        
        model.addAttribute("pendingReconciliations", pendingReconciliations);
        model.addAttribute("pendingCount", pendingReconciliations.size());
        model.addAttribute("recentlyProcessed", recentlyProcessed);
        model.addAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return "dashboard";
    }
    
    /**
     * Displays reconciliations with a specific status.
     */
    @GetMapping("/status/{status}")
    public String showReconciliationsByStatus(@PathVariable("status") ReconciliationStatus status, Model model) {
        List<ReconciliationResult> results = reconciliationService.getReconciliationsByStatus(status);
        
        model.addAttribute("status", status);
        model.addAttribute("results", results);
        
        return "reconciliation-by-status";
    }
    
    /**
     * Displays details for a specific reconciliation.
     */
    @GetMapping("/details/{id}")
    public String showReconciliationDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<ReconciliationResult> resultOptional = reconciliationService.getReconciliationResultById(id);
        
        if (resultOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Reconciliation with ID " + id + " not found");
            return "redirect:/dashboard";
        }
        
        model.addAttribute("result", resultOptional.get());
        
        return "reconciliation-details";
    }
    
    /**
     * Approves a reconciliation.
     */
    @PostMapping("/approve/{id}")
    public String approveReconciliation(
            @PathVariable("id") Long id,
            @RequestParam("notes") String notes,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        
        try {
            ReconciliationResult result = reconciliationService.approveReconciliation(id, notes);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Successfully approved reconciliation for item " + result.getItemId() + 
                    " at location " + result.getLocationId());
            
        } catch (Exception e) {
            logger.error("Error approving reconciliation with ID " + id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error approving reconciliation: " + e.getMessage());
        }
        
        // Determine where to redirect based on referer
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/details/")) {
            return "redirect:/dashboard";
        } else {
            return "redirect:" + (referer != null ? referer : "/dashboard");
        }
    }
    
    /**
     * Rejects a reconciliation.
     */
    @PostMapping("/reject/{id}")
    public String rejectReconciliation(
            @PathVariable("id") Long id,
            @RequestParam("reason") String reason,
            @RequestParam(value = "overrideAuthority", required = false) String overrideAuthority,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        
        try {
            SystemType overrideSystem = null;
            if (overrideAuthority != null && !overrideAuthority.isEmpty()) {
                overrideSystem = SystemType.valueOf(overrideAuthority);
            }
            
            ReconciliationResult result = reconciliationService.rejectReconciliation(id, reason, overrideSystem);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Successfully rejected reconciliation for item " + result.getItemId() + 
                    " at location " + result.getLocationId());
            
        } catch (Exception e) {
            logger.error("Error rejecting reconciliation with ID " + id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error rejecting reconciliation: " + e.getMessage());
        }
        
        // Determine where to redirect based on referer
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/details/")) {
            return "redirect:/dashboard";
        } else {
            return "redirect:" + (referer != null ? referer : "/dashboard");
        }
    }
    
    /**
     * Generates a downloadable report.
     */
    @GetMapping("/report")
    public String generateReport(RedirectAttributes redirectAttributes) {
        try {
            // For now, just redirect to dashboard with a message
            // In a real implementation, this would call the reporting service
            // and return a file download
            redirectAttributes.addFlashAttribute("successMessage", "Report generation is not implemented yet.");
            
        } catch (Exception e) {
            logger.error("Error generating report", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error generating report: " + e.getMessage());
        }
        
        return "redirect:/dashboard";
    }
}