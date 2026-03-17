package samosa_fos.de.Repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.JobSite;
import samosa_fos.de.repository.JobSiteRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class JobSiteRepositoryTest {

    @Autowired
    JobSiteRepository jobSiteRepository;

    @Test
    @DisplayName("현장 저장 테스트")
    void saveJobSite() {

        // given
        JobSite jobSite = new JobSite();
        jobSite.setCustomerId(1L);
        jobSite.setName("송도 힐스테이트");
        jobSite.setMemo("101동 내부도장");
        jobSite.setActive(true);

        // when
        JobSite savedJobSite = jobSiteRepository.save(jobSite);

        // then
        assertThat(savedJobSite.getId()).isNotNull();
        assertThat(savedJobSite.getCustomerId()).isEqualTo(1L);
        assertThat(savedJobSite.getName()).isEqualTo("송도 힐스테이트");

    }

    @Test
    @DisplayName("고객 id로 현장 조회 테스트")
    void findByCustomerId() {

        // given
        JobSite jobSite1 = new JobSite();
        jobSite1.setCustomerId(1L);
        jobSite1.setName("송도 힐스테이트");
        jobSite1.setMemo("101동 내부도장");
        jobSite1.setActive(true);

        JobSite jobSite2 = new JobSite();
        jobSite2.setCustomerId(1L);
        jobSite2.setName("부산 롯데캐슬");
        jobSite2.setMemo(null);
        jobSite2.setActive(true);

        JobSite jobSite3 = new JobSite();
        jobSite3.setCustomerId(2L);
        jobSite3.setName("평택 공장");
        jobSite3.setMemo("외벽 우레탄");
        jobSite3.setActive(true);

        jobSiteRepository.save(jobSite1);
        jobSiteRepository.save(jobSite2);
        jobSiteRepository.save(jobSite3);

        // when
        List<JobSite> jobSites = jobSiteRepository.findByCustomerId(1L);

        //then
        assertThat(jobSites).hasSize(2);
        assertThat(jobSites).extracting("name")
                .contains("송도 힐스테이트", "부산 롯데캐슬");
        assertThat(jobSites).extracting("name")
                .doesNotContain("평택 공장");


    }


    @Test
    @DisplayName("활성화된 현장만 조회 테스트")
    void findByActiveTrue() {

        // given
        JobSite jobSite1 = new JobSite();
        jobSite1.setCustomerId(1L);
        jobSite1.setName("활성 현장");
        jobSite1.setMemo(null);
        jobSite1.setActive(true);

        JobSite jobSite2 = new JobSite();
        jobSite2.setCustomerId(1L);
        jobSite2.setName("비활성 현장");
        jobSite2.setMemo("사용 중지");
        jobSite2.setActive(false);

        jobSiteRepository.save(jobSite1);
        jobSiteRepository.save(jobSite2);

        // when
        List<JobSite> activeJobSites = jobSiteRepository.findByActiveTrue();

        // then
        assertThat(activeJobSites).isNotEmpty();

             // 조회된 모든 현장은 active=true 여야 함
        assertThat(activeJobSites).allMatch(JobSite::getActive);

             // 이름으로도 확인
        assertThat(activeJobSites).extracting("name").contains("활성 현장");
        assertThat(activeJobSites).extracting("name").doesNotContain("비활성 현장");
    }

    @Test
    @DisplayName("특정 고객의 활성 현장만 조회 테스트")
    void findByCustomerIdAndActiveTrue() {
        // given
        JobSite jobSite1 = new JobSite();
        jobSite1.setCustomerId(1L);
        jobSite1.setName("고객1 활성 현장");
        jobSite1.setMemo(null);
        jobSite1.setActive(true);

        JobSite jobSite2 = new JobSite();
        jobSite2.setCustomerId(1L);
        jobSite2.setName("고객1 비활성 현장");
        jobSite2.setMemo(null);
        jobSite2.setActive(false);

        JobSite jobSite3 = new JobSite();
        jobSite3.setCustomerId(2L);
        jobSite3.setName("고객2 활성 현장");
        jobSite3.setMemo(null);
        jobSite3.setActive(true);

        jobSiteRepository.save(jobSite1);
        jobSiteRepository.save(jobSite2);
        jobSiteRepository.save(jobSite3);
        //when
        List<JobSite> jobSites = jobSiteRepository.findByCustomerIdAndActiveTrue(1L);

        //then
        assertThat(jobSites).hasSize(1);
        assertThat(jobSites).extracting("name").contains("고객1 활성 현장");
        assertThat(jobSites).extracting("name").
                doesNotContain("고객1 비활성 현장","고객2 활성 현장");
        assertThat(jobSites).allMatch(JobSite::getActive);

    }



}
