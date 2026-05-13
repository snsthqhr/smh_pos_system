package samosa_fos.de.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import samosa_fos.de.domain.JobSite;
import samosa_fos.de.dto.jobsite.CreateJobSiteRequest;
import samosa_fos.de.dto.jobsite.JobSiteResponse;
import samosa_fos.de.repository.CustomerRepository;
import samosa_fos.de.repository.JobSiteRepository;

import java.util.List;

@RestController
@RequestMapping("/api/job-sites")
public class JobSiteController {

    private final JobSiteRepository jobSiteRepository;
    private final CustomerRepository customerRepository;

    public JobSiteController(JobSiteRepository jobSiteRepository,
                             CustomerRepository customerRepository) {
        this.jobSiteRepository = jobSiteRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<JobSiteResponse> searchJobSites(@RequestParam Long customerId,
                                                @RequestParam(required = false) String keyword) {
        validateCustomer(customerId);
        String normalizedKeyword = normalize(keyword);

        return jobSiteRepository.findByCustomerIdAndActiveTrue(customerId).stream()
                .filter(jobSite -> normalizedKeyword.isBlank() ||
                        normalize(jobSite.getName()).contains(normalizedKeyword))
                .limit(20)
                .map(JobSiteResponse::new)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public synchronized JobSiteResponse createJobSite(@RequestBody CreateJobSiteRequest request) {
        if (request == null || request.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "고객은 필수입니다.");
        }
        if (normalize(request.getName()).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현장명은 필수입니다.");
        }

        validateCustomer(request.getCustomerId());

        String name = normalize(request.getName());
        JobSite existing = jobSiteRepository.findByCustomerIdAndActiveTrue(request.getCustomerId()).stream()
                .filter(jobSite -> normalize(jobSite.getName()).equals(name))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return new JobSiteResponse(existing);
        }

        JobSite jobSite = new JobSite();
        jobSite.setCustomerId(request.getCustomerId());
        jobSite.setName(name);
        jobSite.setMemo(normalizeNullable(request.getMemo()));
        jobSite.setActive(true);

        return new JobSiteResponse(jobSiteRepository.save(jobSite));
    }

    private void validateCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 고객입니다.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }
}
