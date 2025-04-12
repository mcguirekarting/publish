package com.manhattan.reconciliation.service.impl;

import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.model.ReconciliationStatus;
import com.manhattan.reconciliation.repository.ReconciliationResultRepository;
import com.manhattan.reconciliation.service.ReportingService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Implementation of the ReportingService interface.
 */
@Service
public class ReportingServiceImpl implements ReportingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportingServiceImpl.class);
    
    private final ReconciliationResultRepository reconciliationResultRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${manhattan.reconciliation.reporting.enabled:true}")
    private boolean reportingEnabled;
    
    @Value("${manhattan.reconciliation.reporting.days-to-include:1}")
    private int daysToInclude;
    
    @Value("${manhattan.reconciliation.reporting.recipients}")
    private String reportRecipients;
    
    @Value("${manhattan.reconciliation.reporting.sender-email}")
    private String senderEmail;
    
    @Value("${manhattan.reconciliation.reporting.subject:Daily Inventory Reconciliation Report}")
    private String reportSubject;
    
    @Autowired
    public ReportingServiceImpl(
            ReconciliationResultRepository reconciliationResultRepository,
            JavaMailSender mailSender,
            TemplateEngine templateEngine) {
        this.reconciliationResultRepository = reconciliationResultRepository;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
    
    /**
     * Scheduled task to send the daily reconciliation report.
     * Runs every day at 6:00 AM (configured in application.properties).
     */
    @Scheduled(cron = "${manhattan.reconciliation.reporting.schedule:0 0 6 * * ?}")
    public void scheduledDailyReport() {
        if (reportingEnabled) {
            logger.info("Starting scheduled daily reconciliation report");
            boolean success = sendDailyReconciliationReport();
            if (success) {
                logger.info("Successfully sent daily reconciliation report");
            } else {
                logger.error("Failed to send daily reconciliation report");
            }
        } else {
            logger.info("Daily reconciliation reporting is disabled");
        }
    }
    
    @Override
    public boolean sendDailyReconciliationReport() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(daysToInclude);
        
        List<String> recipients = Arrays.asList(reportRecipients.split(","));
        
        return sendReconciliationReport(startTime, endTime, recipients);
    }
    
    @Override
    public boolean sendReconciliationReport(
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            List<String> recipients) {
        
        try {
            // Fetch data for the report
            List<ReconciliationResult> results = reconciliationResultRepository
                    .findByReconciliationTimeBetween(startTime, endTime);
            
            // Get counts by status
            Map<ReconciliationStatus, Integer> statusCounts = new HashMap<>();
            for (ReconciliationStatus status : ReconciliationStatus.values()) {
                statusCounts.put(status, 0);
            }
            
            // Get pending items that need approval
            List<ReconciliationResult> pendingItems = reconciliationResultRepository
                    .findByStatus(ReconciliationStatus.PENDING);
            
            // Count results by status
            for (ReconciliationResult result : results) {
                statusCounts.put(result.getStatus(), statusCounts.getOrDefault(result.getStatus(), 0) + 1);
            }
            
            // Create the email content using Thymeleaf
            Context context = new Context();
            context.setVariable("startTime", startTime.format(DateTimeFormatter.ISO_DATE_TIME));
            context.setVariable("endTime", endTime.format(DateTimeFormatter.ISO_DATE_TIME));
            context.setVariable("results", results);
            context.setVariable("pendingItems", pendingItems);
            context.setVariable("statusCounts", statusCounts);
            context.setVariable("totalReconciliations", results.size());
            
            String emailContent = templateEngine.process("email/reconciliation-report", context);
            
            // Generate PDF report
            byte[] pdfReport = generatePdfReport(results, pendingItems, startTime, endTime);
            
            // Send the email with the PDF attachment
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(senderEmail);
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(reportSubject + " - " + startTime.format(DateTimeFormatter.ISO_DATE));
            helper.setText(emailContent, true);
            
            String reportFilename = "reconciliation-report-" + startTime.format(DateTimeFormatter.ISO_DATE) + ".pdf";
            helper.addAttachment(reportFilename, new ByteArrayResource(pdfReport));
            
            mailSender.send(message);
            
            logger.info("Sent reconciliation report to {} recipients", recipients.size());
            return true;
            
        } catch (MessagingException | DocumentException e) {
            logger.error("Error sending reconciliation report", e);
            return false;
        }
    }
    
    /**
     * Generates a PDF report from the reconciliation data.
     */
    private byte[] generatePdfReport(
            List<ReconciliationResult> results,
            List<ReconciliationResult> pendingItems,
            LocalDateTime startTime,
            LocalDateTime endTime) throws DocumentException {
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        
        document.open();
        
        // Add title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Paragraph title = new Paragraph("Inventory Reconciliation Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        // Add date range
        Paragraph dateRange = new Paragraph(
                "Period: " + startTime.format(DateTimeFormatter.ISO_DATE_TIME) + 
                " to " + endTime.format(DateTimeFormatter.ISO_DATE_TIME));
        dateRange.setAlignment(Element.ALIGN_CENTER);
        document.add(dateRange);
        
        document.add(new Paragraph(" ")); // Add some space
        
        // Add summary section
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Paragraph summaryTitle = new Paragraph("Summary", sectionFont);
        document.add(summaryTitle);
        
        document.add(new Paragraph("Total reconciliations: " + results.size()));
        
        // Count by status
        Map<ReconciliationStatus, Integer> statusCounts = new HashMap<>();
        for (ReconciliationResult result : results) {
            statusCounts.put(result.getStatus(), statusCounts.getOrDefault(result.getStatus(), 0) + 1);
        }
        
        for (ReconciliationStatus status : ReconciliationStatus.values()) {
            int count = statusCounts.getOrDefault(status, 0);
            document.add(new Paragraph(status.name() + ": " + count));
        }
        
        document.add(new Paragraph(" ")); // Add some space
        
        // Add pending items that need attention
        Paragraph pendingTitle = new Paragraph("Items Requiring Attention", sectionFont);
        document.add(pendingTitle);
        
        if (pendingItems.isEmpty()) {
            document.add(new Paragraph("No items currently require attention."));
        } else {
            document.add(new Paragraph("The following " + pendingItems.size() + " items require manual review:"));
            document.add(new Paragraph(" "));
            
            // Create table for pending items
            PdfPTable pendingTable = new PdfPTable(6);
            pendingTable.setWidthPercentage(100);
            
            // Add table header
            Stream.of("Item ID", "Location", "MAO Qty", "MAWM Qty", "Discrepancy", "Authority")
                    .forEach(headerTitle -> {
                        PdfPCell header = new PdfPCell();
                        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        header.setBorderWidth(2);
                        header.setPhrase(new Phrase(headerTitle));
                        pendingTable.addCell(header);
                    });
            
            // Add data rows
            for (ReconciliationResult item : pendingItems) {
                pendingTable.addCell(item.getItemId());
                pendingTable.addCell(item.getLocationId());
                pendingTable.addCell(String.valueOf(item.getMaoQuantity()));
                pendingTable.addCell(String.valueOf(item.getMawmQuantity()));
                pendingTable.addCell(String.valueOf(item.getDiscrepancy()));
                pendingTable.addCell(item.getAuthoritySystem().name());
            }
            
            document.add(pendingTable);
        }
        
        document.add(new Paragraph(" ")); // Add some space
        
        // Add all reconciliations
        Paragraph allRecsTitle = new Paragraph("All Reconciliations in Period", sectionFont);
        document.add(allRecsTitle);
        
        if (results.isEmpty()) {
            document.add(new Paragraph("No reconciliations were performed during this period."));
        } else {
            // Create table for all reconciliations
            PdfPTable allRecsTable = new PdfPTable(7);
            allRecsTable.setWidthPercentage(100);
            
            // Add table header
            Stream.of("Item ID", "Location", "MAO Qty", "MAWM Qty", "Discrepancy", "Status", "Time")
                    .forEach(headerTitle -> {
                        PdfPCell header = new PdfPCell();
                        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        header.setBorderWidth(2);
                        header.setPhrase(new Phrase(headerTitle));
                        allRecsTable.addCell(header);
                    });
            
            // Add data rows (limit to 50 most recent to keep PDF reasonable size)
            results.stream()
                    .sorted((r1, r2) -> r2.getReconciliationTime().compareTo(r1.getReconciliationTime()))
                    .limit(50)
                    .forEach(item -> {
                        allRecsTable.addCell(item.getItemId());
                        allRecsTable.addCell(item.getLocationId());
                        allRecsTable.addCell(String.valueOf(item.getMaoQuantity()));
                        allRecsTable.addCell(String.valueOf(item.getMawmQuantity()));
                        allRecsTable.addCell(String.valueOf(item.getDiscrepancy()));
                        allRecsTable.addCell(item.getStatus().name());
                        allRecsTable.addCell(item.getReconciliationTime().format(DateTimeFormatter.ISO_DATE_TIME));
                    });
            
            document.add(allRecsTable);
            
            if (results.size() > 50) {
                document.add(new Paragraph("Note: Only the 50 most recent reconciliations are shown in this report."));
            }
        }
        
        // Add footer
        document.add(new Paragraph(" "));
        Paragraph footer = new Paragraph("Report generated on " + 
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + 
                " by Manhattan Inventory Reconciliation Service");
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
        
        document.close();
        
        return outputStream.toByteArray();
    }
}