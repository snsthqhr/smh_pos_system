package samosa_fos.de.service;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.dto.statement.StatementItemResponse;
import samosa_fos.de.dto.statement.StatementResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

@Service
@Transactional(readOnly = true)
public class StatementDocxService {

    private final StatementService statementService;

    public StatementDocxService(StatementService statementService) {
        this.statementService = statementService;
    }

    public byte[] createStatementDocx(Long salesOrderId) {

        StatementResponse statement = statementService.getStatement(salesOrderId);

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            createTitle(document);
            createTopInfoTable(document, statement);
            createItemTable(document, statement);
            createFooterTable(document, statement);

            document.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("거래명세표 DOCX 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void createTitle(XWPFDocument document) {
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun run = title.createRun();
        run.setText("거 래 명 세 표");
        run.setBold(true);
        run.setFontSize(24);
        run.setFontFamily("Malgun Gothic");

        XWPFParagraph space = document.createParagraph();
        space.setSpacingAfter(80);
    }

    /**
     * 상단 공급받는자 / 공급자 정보 영역
     */
    private void createTopInfoTable(XWPFDocument document, StatementResponse statement) {

        XWPFTable table = document.createTable(6, 6);
        table.setWidth("100%");

        // 1행
        setCell(table, 0, 0, "공급받는자", true, ParagraphAlignment.CENTER);
        setCell(table, 0, 1, "발행일", true, ParagraphAlignment.CENTER);
        setCell(table, 0, 2, String.valueOf(statement.getIssueDate()), false, ParagraphAlignment.CENTER);
        setCell(table, 0, 3, "공급자", true, ParagraphAlignment.CENTER);
        setCell(table, 0, 4, "사업자번호", true, ParagraphAlignment.CENTER);
        setCell(table, 0, 5, nullToBlank(statement.getSupplierBusinessNumber()), false, ParagraphAlignment.CENTER);

        // 2행
        setCell(table, 1, 0, "", false, ParagraphAlignment.CENTER);
        setCell(table, 1, 1, "거래처명", true, ParagraphAlignment.CENTER);
        setCell(table, 1, 2, nullToBlank(statement.getCustomerName()), false, ParagraphAlignment.CENTER);
        setCell(table, 1, 3, "", false, ParagraphAlignment.CENTER);
        setCell(table, 1, 4, "상호", true, ParagraphAlignment.CENTER);
        setCell(table, 1, 5, nullToBlank(statement.getSupplierName()), false, ParagraphAlignment.CENTER);

        // 3행
        setCell(table, 2, 0, "", false, ParagraphAlignment.CENTER);
        setCell(table, 2, 1, "인수자", true, ParagraphAlignment.CENTER);
        setCell(table, 2, 2, nullToBlank(statement.getReceiverName()), false, ParagraphAlignment.CENTER);
        setCell(table, 2, 3, "", false, ParagraphAlignment.CENTER);
        setCell(table, 2, 4, "대표자", true, ParagraphAlignment.CENTER);
        setCell(table, 2, 5, nullToBlank(statement.getSupplierCeoName()), false, ParagraphAlignment.CENTER);

        // 4행
        setCell(table, 3, 0, "", false, ParagraphAlignment.CENTER);
        setCell(table, 3, 1, "합계금액", true, ParagraphAlignment.CENTER);
        setCell(table, 3, 2, formatMoney(statement.getTotalAmount()), true, ParagraphAlignment.RIGHT);
        setCell(table, 3, 3, "", false, ParagraphAlignment.CENTER);
        setCell(table, 3, 4, "주소", true, ParagraphAlignment.CENTER);
        setCell(table, 3, 5, nullToBlank(statement.getSupplierAddress()), false, ParagraphAlignment.LEFT);

        // 5행
        setCell(table, 4, 0, "", false, ParagraphAlignment.CENTER);
        setCell(table, 4, 1, "", false, ParagraphAlignment.CENTER);
        setCell(table, 4, 2, "", false, ParagraphAlignment.CENTER);
        setCell(table, 4, 3, "", false, ParagraphAlignment.CENTER);
        setCell(table, 4, 4, "업태", true, ParagraphAlignment.CENTER);
        setCell(table, 4, 5, nullToBlank(statement.getSupplierBusinessType()), false, ParagraphAlignment.CENTER);

        // 6행
        setCell(table, 5, 0, "", false, ParagraphAlignment.CENTER);
        setCell(table, 5, 1, "", false, ParagraphAlignment.CENTER);
        setCell(table, 5, 2, "", false, ParagraphAlignment.CENTER);
        setCell(table, 5, 3, "", false, ParagraphAlignment.CENTER);
        setCell(table, 5, 4, "종목", true, ParagraphAlignment.CENTER);
        setCell(table, 5, 5, nullToBlank(statement.getSupplierBusinessItem()), false, ParagraphAlignment.CENTER);

        XWPFParagraph space = document.createParagraph();
        space.setSpacingAfter(100);
    }

    /**
     * 품목 테이블
     */
    private void createItemTable(XWPFDocument document, StatementResponse statement) {

        int rowCount = statement.getItems().size() + 2;
        XWPFTable table = document.createTable(rowCount, 7);
        table.setWidth("100%");

        setCell(table, 0, 0, "No", true, ParagraphAlignment.CENTER);
        setCell(table, 0, 1, "품명", true, ParagraphAlignment.CENTER);
        setCell(table, 0, 2, "규격", true, ParagraphAlignment.CENTER);
        setCell(table, 0, 3, "수량", true, ParagraphAlignment.CENTER);
        setCell(table, 0, 4, "단가", true, ParagraphAlignment.CENTER);
        setCell(table, 0, 5, "공급가액", true, ParagraphAlignment.CENTER);
        setCell(table, 0, 6, "세액", true, ParagraphAlignment.CENTER);

        int rowIndex = 1;

        for (StatementItemResponse item : statement.getItems()) {
            setCell(table, rowIndex, 0, String.valueOf(item.getNo()), false, ParagraphAlignment.CENTER);
            setCell(table, rowIndex, 1, nullToBlank(item.getProductName()), false, ParagraphAlignment.LEFT);
            setCell(table, rowIndex, 2, nullToBlank(item.getSpec()), false, ParagraphAlignment.CENTER);
            setCell(table, rowIndex, 3, String.valueOf(item.getQuantity()), false, ParagraphAlignment.RIGHT);
            setCell(table, rowIndex, 4, formatMoney(item.getUnitPrice()), false, ParagraphAlignment.RIGHT);
            setCell(table, rowIndex, 5, formatMoney(item.getSupplyPrice()), false, ParagraphAlignment.RIGHT);
            setCell(table, rowIndex, 6, formatMoney(item.getTaxPrice()), false, ParagraphAlignment.RIGHT);
            rowIndex++;
        }

        setCell(table, rowIndex, 0, "합계", true, ParagraphAlignment.CENTER);
        setCell(table, rowIndex, 1, "", true, ParagraphAlignment.CENTER);
        setCell(table, rowIndex, 2, "", true, ParagraphAlignment.CENTER);
        setCell(table, rowIndex, 3, "", true, ParagraphAlignment.CENTER);
        setCell(table, rowIndex, 4, "", true, ParagraphAlignment.CENTER);
        setCell(table, rowIndex, 5, formatMoney(statement.getTotalSupplyPrice()), true, ParagraphAlignment.RIGHT);
        setCell(table, rowIndex, 6, formatMoney(statement.getTotalTaxPrice()), true, ParagraphAlignment.RIGHT);

        XWPFParagraph space = document.createParagraph();
        space.setSpacingAfter(100);
    }

    /**
     * 하단 기타사항 영역
     */
    private void createFooterTable(XWPFDocument document, StatementResponse statement) {

        XWPFTable table = document.createTable(4, 2);
        table.setWidth("100%");

        setCell(table, 0, 0, "기타사항", true, ParagraphAlignment.CENTER);
        setCell(table, 0, 1, "", false, ParagraphAlignment.LEFT);

        setCell(table, 1, 0, "입금계좌", true, ParagraphAlignment.CENTER);
        setCell(table, 1, 1, nullToBlank(statement.getBankAccount()), false, ParagraphAlignment.LEFT);

        setCell(table, 2, 0, "예금주", true, ParagraphAlignment.CENTER);
        setCell(table, 2, 1, nullToBlank(statement.getAccountHolder()), false, ParagraphAlignment.LEFT);

        setCell(table, 3, 0, "현재 미수금", true, ParagraphAlignment.CENTER);
        setCell(table, 3, 1, formatMoney(statement.getRemainingArBalance()), false, ParagraphAlignment.RIGHT);
    }

    private void setCell(XWPFTable table,
                         int rowIndex,
                         int cellIndex,
                         String text,
                         boolean bold,
                         ParagraphAlignment alignment) {

        XWPFTableCell cell = table.getRow(rowIndex).getCell(cellIndex);
        cell.removeParagraph(0);

        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(alignment);

        XWPFRun run = paragraph.createRun();
        run.setText(text == null ? "" : text);
        run.setBold(bold);
        run.setFontSize(10);
        run.setFontFamily("Malgun Gothic");
    }

    private String formatMoney(Integer amount) {
        if (amount == null) {
            return "0";
        }

        return new DecimalFormat("#,###").format(amount);
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}