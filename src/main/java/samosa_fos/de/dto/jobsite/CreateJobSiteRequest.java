package samosa_fos.de.dto.jobsite;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateJobSiteRequest {

    private Long customerId;
    private String name;
    private String memo;
}
