package samosa_fos.de.service;


import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.dto.statement.StatementResponse;
import samosa_fos.de.dto.statement.StatementItemResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

@Service
@Transactional(readOnly = true)
public class StatementDocxService {

    private final StatementService statementService;

    public  StatementDocxService(StatementService statementService) {
        this.statementService = statementService;
    }

    /**
     * 단일 판매전표 기준 거래명세표 DOCX 생성
     *
     * 현재는 salesOrderId 기준으로 생성한다.
     * 추후에는 customerId + startDate + endDate 기준의 기간 거래명세표로 확장한다.
     */
    public byte[] createStatementDocx(Long salesOrderId)  {

        //1. 거래명세표 데이터 조회
        StatementResponse statement = statementService.getStatement(salesOrderId);
        //2. 워드 문서 생성
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            //3. 문서 제목 생성
            createTitle(document);
            //4. 상단 기본 정보 생성
            createHeaderInfo(document,statement);
            //5. 품목 테이블 생성
            createItemTable(document, statement);
            //6. 하단 기타 정보 생성
            createFooterInfo(document, statement);
            // 7. byte[]로 변환
            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("거래명세표 DOCX 생성 중 오류가 발생했습니다.", e);
        }



    }


    //거래명세표 제목 생성
    private void createTitle(XWPFDocument document) {
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun run = title.createRun();
        run.setText("거 래 명 세 표");
        run.setBold(true);
        run.setFontSize(22);
        run.setFontFamily("Malgun Gothic");

    }

    //상단 거래처 / 공급자 정보 생성
    private void createHeaderInfo(XWPFDocument document, StatementResponse statement){

        XWPFTable table = document.createTable(5,4);
        table.setWidth("100%");

        setCellText(table.getRow(0).getCell(0), "발행일");
        setCellText(table.getRow(0).getCell(1), String.valueOf(statement.getIssueDate()));
        setCellText(table.getRow(0).getCell(2), "사업자번호");
        setCellText(table.getRow(0).getCell(3), nullToBlank(statement.getSupplierBusinessNumber()));

        setCellText(table.getRow(1).getCell(0), "거래처명");
        setCellText(table.getRow(1).getCell(1), nullToBlank(statement.getCustomerName()));
        setCellText(table.getRow(1).getCell(2), "상호");
        setCellText(table.getRow(1).getCell(3), nullToBlank(statement.getSupplierName()));

        setCellText(table.getRow(2).getCell(0), "인수 담당자");
        setCellText(table.getRow(2).getCell(1), nullToBlank(statement.getReceiverName()));
        setCellText(table.getRow(2).getCell(2), "대표자");
        setCellText(table.getRow(2).getCell(3), nullToBlank(statement.getSupplierCeoName()));

        setCellText(table.getRow(3).getCell(0), "합계금액");
        setCellText(table.getRow(3).getCell(1), formatMoney(statement.getTotalAmount()));
        setCellText(table.getRow(3).getCell(2), "주소");
        setCellText(table.getRow(3).getCell(3), nullToBlank(statement.getSupplierAddress()));

        setCellText(table.getRow(4).getCell(0), "비고");
        setCellText(table.getRow(4).getCell(1), "");
        setCellText(table.getRow(4).getCell(2), "업태/종목");
        setCellText(
                table.getRow(4).getCell(3),
                nullToBlank(statement.getSupplierBusinessType()) + " / " +
                        nullToBlank(statement.getSupplierBusinessItem())



        );
    }

    //품목 테이블 생성
    private void createItemTable(XWPFDocument document, StatementResponse statement) {


        XWPFParagraph space = document.createParagraph();
        space.createRun().setText("");

        int rowCount = statement.getItems().size() + 2;
        XWPFTable table = document.createTable(rowCount, 7);
        table.setWidth("100%");

        // 헤더
        setCellText(table.getRow(0).getCell(0), "No");
        setCellText(table.getRow(0).getCell(1), "품명");
        setCellText(table.getRow(0).getCell(2), "규격");
        setCellText(table.getRow(0).getCell(3), "수량");
        setCellText(table.getRow(0).getCell(4), "단가");
        setCellText(table.getRow(0).getCell(5), "공급가액");
        setCellText(table.getRow(0).getCell(6), "세액");

        int rowIndex = 1;

        for (StatementItemResponse item : statement.getItems()) {
            XWPFTableRow row = table.getRow(rowIndex++);

            setCellText(row.getCell(0), String.valueOf(item.getNo()));
            setCellText(row.getCell(1), nullToBlank(item.getProductName()));
            setCellText(row.getCell(2), nullToBlank(item.getSpec()));
            setCellText(row.getCell(3), String.valueOf(item.getQuantity()));
            setCellText(row.getCell(4), formatMoney(item.getUnitPrice()));
            setCellText(row.getCell(5), formatMoney(item.getSupplyPrice()));
            setCellText(row.getCell(6), formatMoney(item.getTaxPrice()));
        }

        // 합계 행
        XWPFTableRow totalRow = table.getRow(rowIndex);
        setCellText(totalRow.getCell(0), "합계");
        setCellText(totalRow.getCell(1), "");
        setCellText(totalRow.getCell(2), "");
        setCellText(totalRow.getCell(3), "");
        setCellText(totalRow.getCell(4), "");
        setCellText(totalRow.getCell(5), formatMoney(statement.getTotalSupplyPrice()));
        setCellText(totalRow.getCell(6), formatMoney(statement.getTotalTaxPrice()));

    }


    /**
     * 하단 기타 정보 생성
     */
    private void createFooterInfo(XWPFDocument document, StatementResponse statement) {

        XWPFParagraph space = document.createParagraph();
        space.createRun().setText("");

        XWPFTable table = document.createTable(3, 2);
        table.setWidth("100%");

        setCellText(table.getRow(0).getCell(0), "입금계좌");
        setCellText(table.getRow(0).getCell(1), nullToBlank(statement.getBankAccount()));

        setCellText(table.getRow(1).getCell(0), "예금주");
        setCellText(table.getRow(1).getCell(1), nullToBlank(statement.getAccountHolder()));

        setCellText(table.getRow(2).getCell(0), "현재 미수금");
        setCellText(table.getRow(2).getCell(1), formatMoney(statement.getRemainingArBalance()));
    }



    // 셀 텍스트 세팅 공통 메서드
    private void setCellText(XWPFTableCell cell, String text) {
        cell.removeParagraph(0);

        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun run = paragraph.createRun();
        run.setText(text == null ? "" : text);
        run.setFontSize(10);
        run.setFontFamily("Malgun Gothic");



    }


    /**
     * 금액 포맷
     */
    private String formatMoney(Integer amount) {
        if (amount == null) {
            return "0";
        }

        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(amount);
    }

    /**
     * null 문자열 처리
     */
    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }


}
