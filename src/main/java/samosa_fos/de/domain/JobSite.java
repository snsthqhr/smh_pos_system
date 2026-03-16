package samosa_fos.de.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class JobSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "jobsite_id")
    private Long id;


    // 소속 고객 id
    private Long customerId;

    //현장명
    private String name;

    //현장 메모
    @Column(length =1000)
    private String memo;

    //삭제 대신 비활성화
    private Boolean active=true;


}
