package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

public class SalesHistorySearchRequest {

    private Long customerId;
    private LocalDate startDate;
    private LocalDate endDage;

}
