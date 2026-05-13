package samosa_fos.de.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import samosa_fos.de.domain.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 고객 이름으로 검색
    // POS에서 고객 검색할 때 사용
    List<Customer> findByNameContaining(String name);

    Optional<Customer> findFirstByNameAndActiveTrue(String name);

    // 전화번호로 고객 조회
    // 동일 번호 고객 확인 또는 빠른 조회
    Optional<Customer> findByPhone(String phone);


    // 활성화된 고객만 조회
    // 거래 중인 고객만 리스트로 보여줄 때 사용
    List<Customer> findByActiveTrue();

}
