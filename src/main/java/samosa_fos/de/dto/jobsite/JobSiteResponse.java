package samosa_fos.de.dto.jobsite;

import lombok.Getter;
import samosa_fos.de.domain.JobSite;

@Getter
public class JobSiteResponse {

    private final Long id;
    private final Long customerId;
    private final String name;
    private final String memo;

    public JobSiteResponse(JobSite jobSite) {
        this.id = jobSite.getId();
        this.customerId = jobSite.getCustomerId();
        this.name = jobSite.getName();
        this.memo = jobSite.getMemo();
    }
}
