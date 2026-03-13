package com.bank.service;

import com.bank.model.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ─── PDF EXPORT ─────────────────────────────────────────
    public static File exportTransactionsPDF(List<Transaction> transactions, String accountInfo) throws Exception {
        String filename = "transactions_" + LocalDateTime.now().format(FILE_FMT) + ".pdf";
        File file = new File(System.getProperty("user.home") + "/Desktop/" + filename);

        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();

        // Header
        BaseColor headerColor = new BaseColor(20, 27, 45);
        BaseColor accentColor = new BaseColor(0, 212, 170);

        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 22, com.itextpdf.text.Font.BOLD,
                new BaseColor(30, 40, 70));
        com.itextpdf.text.Font subFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL,
                BaseColor.GRAY);
        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD,
                BaseColor.WHITE);
        com.itextpdf.text.Font cellFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.NORMAL,
                new BaseColor(50, 60, 80));

        // Title block
        Paragraph title = new Paragraph("🏦  NeoBank Elite", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph sub = new Paragraph("Relevé de Transactions — " + accountInfo, subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(8);
        doc.add(sub);

        Paragraph date = new Paragraph("Généré le : " + LocalDateTime.now().format(FMT), subFont);
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingAfter(20);
        doc.add(date);

        // Table
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 2f, 3f, 2f, 2f});

        // Column headers
        String[] headers = {"Réf.", "Type", "Description", "Montant", "Solde après"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        // Rows
        boolean alt = false;
        for (Transaction tx : transactions) {
            BaseColor rowColor = alt ? new BaseColor(245, 247, 252) : BaseColor.WHITE;
            alt = !alt;

            addCell(table, tx.getTransactionId(), cellFont, rowColor);
            addCell(table, tx.getType().name(), cellFont, rowColor);
            addCell(table, tx.getDescription() != null ? tx.getDescription() : "-", cellFont, rowColor);

            String amt = (tx.isCredit() ? "+" : "-") + String.format("€%.2f", tx.getAmount());
            BaseColor amtColor = tx.isCredit() ? new BaseColor(0, 150, 100) : new BaseColor(200, 50, 50);
            com.itextpdf.text.Font amtFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, amtColor);
            PdfPCell amtCell = new PdfPCell(new Phrase(amt, amtFont));
            amtCell.setBackgroundColor(rowColor);
            amtCell.setPadding(6);
            amtCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(amtCell);

            addCell(table, String.format("€%.2f", tx.getBalanceAfter()), cellFont, rowColor);
        }

        doc.add(table);

        // Footer summary
        double totalIn  = transactions.stream().filter(Transaction::isCredit).mapToDouble(Transaction::getAmount).sum();
        double totalOut = transactions.stream().filter(t -> !t.isCredit()).mapToDouble(Transaction::getAmount).sum();

        Paragraph summary = new Paragraph(
            "\nTotal crédits : +" + String.format("€%.2f", totalIn) +
            "    |    Total débits : -" + String.format("€%.2f", totalOut) +
            "    |    Nombre de transactions : " + transactions.size(), subFont);
        summary.setSpacingBefore(16);
        summary.setAlignment(Element.ALIGN_CENTER);
        doc.add(summary);

        doc.close();
        return file;
    }

    private static void addCell(PdfPTable table, String text,
                                 com.itextpdf.text.Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    // ─── EXCEL EXPORT ────────────────────────────────────────
    public static File exportTransactionsExcel(List<Transaction> transactions, String accountInfo) throws Exception {
        String filename = "transactions_" + LocalDateTime.now().format(FILE_FMT) + ".xlsx";
        File file = new File(System.getProperty("user.home") + "/Desktop/" + filename);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Transactions");

            // Styles
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle creditStyle = wb.createCellStyle();
            Font creditFont = wb.createFont();
            creditFont.setColor(IndexedColors.GREEN.getIndex());
            creditFont.setBold(true);
            creditStyle.setFont(creditFont);

            CellStyle debitStyle = wb.createCellStyle();
            Font debitFont = wb.createFont();
            debitFont.setColor(IndexedColors.RED.getIndex());
            debitFont.setBold(true);
            debitStyle.setFont(debitFont);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("NeoBank Elite — " + accountInfo);
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short)14);
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("Généré le : " + LocalDateTime.now().format(FMT));

            // Header row
            String[] cols = {"Référence", "Type", "Description", "Montant (€)", "Solde Après (€)"};
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < cols.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 4;
            for (Transaction tx : transactions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(tx.getTransactionId());
                row.createCell(1).setCellValue(tx.getType().name());
                row.createCell(2).setCellValue(tx.getDescription() != null ? tx.getDescription() : "-");

                Cell amtCell = row.createCell(3);
                double amt = tx.isCredit() ? tx.getAmount() : -tx.getAmount();
                amtCell.setCellValue(amt);
                amtCell.setCellStyle(tx.isCredit() ? creditStyle : debitStyle);

                row.createCell(4).setCellValue(tx.getBalanceAfter());
            }

            // Summary row
            Row sumRow = sheet.createRow(rowNum + 1);
            double totalIn  = transactions.stream().filter(Transaction::isCredit).mapToDouble(Transaction::getAmount).sum();
            double totalOut = transactions.stream().filter(t -> !t.isCredit()).mapToDouble(Transaction::getAmount).sum();
            sumRow.createCell(0).setCellValue("Total crédits: +" + String.format("%.2f", totalIn));
            sumRow.createCell(1).setCellValue("Total débits: -" + String.format("%.2f", totalOut));
            sumRow.createCell(2).setCellValue("Nb transactions: " + transactions.size());

            // Auto-size columns
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
        return file;
    }

    // ─── MONTHLY REPORT PDF ──────────────────────────────────
    public static File exportMonthlyReport(List<Client> clients, List<BankAccount> accounts,
                                            List<Transaction> transactions, double totalAssets) throws Exception {
        String filename = "rapport_mensuel_" + LocalDateTime.now().format(FILE_FMT) + ".pdf";
        File file = new File(System.getProperty("user.home") + "/Desktop/" + filename);

        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();

        com.itextpdf.text.Font bigTitle = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 26, com.itextpdf.text.Font.BOLD,
                new BaseColor(20, 27, 45));
        com.itextpdf.text.Font sectionFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD,
                new BaseColor(0, 120, 90));
        com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL,
                new BaseColor(50, 60, 80));

        // Cover
        Paragraph title = new Paragraph("NeoBank Elite", bigTitle);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph subtitle = new Paragraph("Rapport Mensuel — " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")), normalFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(30);
        doc.add(subtitle);

        // KPIs
        doc.add(new Paragraph("Résumé Général", sectionFont));
        doc.add(new Paragraph("  ▸  Actifs Totaux :  " + String.format("€%.2f", totalAssets), normalFont));
        doc.add(new Paragraph("  ▸  Nombre de Clients :  " + clients.size(), normalFont));
        doc.add(new Paragraph("  ▸  Nombre de Comptes :  " + accounts.size(), normalFont));
        doc.add(new Paragraph("  ▸  Transactions ce mois :  " + transactions.size(), normalFont));

        double totalIn  = transactions.stream().filter(Transaction::isCredit).mapToDouble(Transaction::getAmount).sum();
        double totalOut = transactions.stream().filter(t -> !t.isCredit()).mapToDouble(Transaction::getAmount).sum();
        doc.add(new Paragraph("  ▸  Total Crédits :  +" + String.format("€%.2f", totalIn), normalFont));
        doc.add(new Paragraph("  ▸  Total Débits :  -" + String.format("€%.2f", totalOut), normalFont));

        // Clients table
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Top Clients par Solde", sectionFont));
        PdfPTable clientTable = new PdfPTable(3);
        clientTable.setWidthPercentage(100);
        clientTable.setSpacingBefore(8);
        for (String h : new String[]{"Client", "Email", "Solde Total"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, BaseColor.WHITE)));
            c.setBackgroundColor(new BaseColor(20, 27, 45));
            c.setPadding(7);
            c.setBorder(Rectangle.NO_BORDER);
            clientTable.addCell(c);
        }
        clients.stream()
               .sorted((a, b) -> Double.compare(b.getTotalBalance(), a.getTotalBalance()))
               .limit(10)
               .forEach(c -> {
                   addCell(clientTable, c.getFullName(), normalFont, BaseColor.WHITE);
                   addCell(clientTable, c.getEmail(), normalFont, BaseColor.WHITE);
                   addCell(clientTable, String.format("€%.2f", c.getTotalBalance()), normalFont, BaseColor.WHITE);
               });
        doc.add(clientTable);

        doc.add(new Paragraph("\nGénéré automatiquement par NeoBank Elite — " +
                LocalDateTime.now().format(FMT), normalFont));
        doc.close();
        return file;
    }
}
