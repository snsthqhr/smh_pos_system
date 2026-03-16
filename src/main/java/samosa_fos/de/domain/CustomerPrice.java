package samosa_fos.de.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter@Setter
public class CustomerPrice {

    @Id@GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "customer_price_id")
    private Long id;

    private Integer price;// 고객의 가격(null값에 조금 유연하게 대처하기위해)

    private Long JobSiteId;// 고객의 현장 위치

    private Long productId;// 제품의 아이디

    private Long customerId;//고객의 아이디

    private Boolean active = true; // 사용여부


    protected CustomerPrice() {

    }

}
