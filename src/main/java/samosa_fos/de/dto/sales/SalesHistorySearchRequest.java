package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SalesHistorySearchRequest {

    private Long customerId;
    private LocalDate startDate;
    private LocalDate endDate;

}
