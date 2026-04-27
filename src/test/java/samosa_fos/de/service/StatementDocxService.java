package samosa_fos.de.service;


import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.dto.statement.StatementResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

            //6. 하단 기타 정보 생성

            // 7. byte[]로 변환

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




    }


}
