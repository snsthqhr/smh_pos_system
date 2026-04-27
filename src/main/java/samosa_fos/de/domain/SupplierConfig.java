package samosa_fos.de.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SupplierConfig {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String businessNumber; // 사업자번호
    private String supplierName;   // 업체명
    private String ceoName;        // 대표자명
    private String address;        // 주소
    private String businessType;   // 업태
    private String businessItem;   // 종목
    private String phone;          // 전화
    private String fax;            // 팩스

    private String bankAccount;    // 입금계좌
    private String accountHolder;  // 예금주

    private Boolean active;

}
