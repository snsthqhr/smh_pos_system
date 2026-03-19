package samosa_fos.de.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter@Setter
public class Customer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long id;//pk


    private String name;//고객이름

    private String phone;//전화번호

    private String address;//주소

    @Column(length = 1000)
    private String memo;//메모

    private Boolean active = true;//사용여부

    public Customer() {}//JPA요구사항임


}
