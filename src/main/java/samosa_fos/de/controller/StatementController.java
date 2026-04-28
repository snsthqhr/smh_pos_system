package samosa_fos.de.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import samosa_fos.de.service.StatementDocxService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/statements")
public class StatementController {

    private final StatementDocxService statementDocxService;

    public StatementController(StatementDocxService statementDocxService) {
        this.statementDocxService = statementDocxService;
    }

    /**
     * 단일 판매전표 기준 거래명세표 DOCX 다운로드
     *
     * 예:
     * GET /statements/1/docx
     */
    @GetMapping("/{salesOrderId}/docx")
    public ResponseEntity<byte[]> downloadStatementDocx(@PathVariable Long salesOrderId) {

        byte[] docxBytes = statementDocxService.createStatementDocx(salesOrderId);

        String fileName = "거래명세표_" + salesOrderId + ".docx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docxBytes);
    }
}