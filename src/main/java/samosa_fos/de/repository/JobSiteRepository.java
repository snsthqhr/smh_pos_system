package samosa_fos.de.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import samosa_fos.de.domain.JobSite;

import java.util.List;

public interface JobSiteRepository extends JpaRepository<JobSite, Long> {

    // 특정 고객에 속한 현장 조회
    // 고객 선택 시 해당 고객의 현장 리스트를 보여줄 때 사용
    List<JobSite> findByCustomerId(Long customerId);

    //활성화된 현장만 조회
    // 사용 중인 현장만 보여줄 때 사용
    List<JobSite> findByActiveTrue();

    // 특정 고객 + 활성화된 현장 조회
    List<JobSite> findByCustomerIdAndActiveTrue(Long customerId);


}
