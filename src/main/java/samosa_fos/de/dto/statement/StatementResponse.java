package samosa_fos.de.dto.statement;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
//거래 명세표 헤더 DTO
public class StatementResponse {

    private LocalDate issueDate;      // 발행일
    private Long customerId;
    private String customerName;      // 거래처명
    private String receiverName;      // 인수 담당자

    private Integer totalSupplyPrice; // 공급가 합계
    private Integer totalTaxPrice;    // 세액 합계
    private Integer totalAmount;      // 총 합계 VAT 포함

    // 공급자 정보
    private String supplierBusinessNumber;
    private String supplierName;
    private String supplierCeoName;
    private String supplierAddress;
    private String supplierBusinessType;
    private String supplierBusinessItem;
    private String supplierPhone;
    private String supplierFax;

    // 기타 사항
    private String bankAccount;
    private String accountHolder;
    private Integer remainingArBalance;

    private List<StatementItemResponse> items;

}
