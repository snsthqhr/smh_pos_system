package samosa_fos.de.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.Customer;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
public class CustomerRepositoryTest {

    @Autowired
    CustomerRepository customerRepository;

    @Test
    @DisplayName("고객 저장 테스트")
    void saveCustomer() {
        // given
        Customer customer = new Customer();
        customer.setName("현대건설");
        customer.setPhone("010-1111-2222");
        customer.setAddress("서울시 강남구");
        customer.setMemo("주요 거래처");
        customer.setActive(true);

        // when
        Customer savedCustomer = customerRepository.save(customer);

        // then
        assertThat(savedCustomer.getId()).isNotNull();
        assertThat(savedCustomer.getName()).isEqualTo("현대건설");
        assertThat(savedCustomer.getPhone()).isEqualTo("010-1111-2222");
    }

    @Test
    @DisplayName("이름 조회 테스트")
    void findByNameContainingTest(){

        //given
        Customer customer1 = new Customer();
        customer1.setName("윤민호");
        customer1.setPhone("010-1111-2222");
        customer1.setAddress("서울시 수정구 성남로");
        customer1.setMemo("");
        customer1.setActive(true);


        Customer customer2 = new Customer();
        customer2.setName("훈팔이");
        customer2.setPhone("010-3584-4127");
        customer2.setAddress("인천시 연수구");
        customer2.setMemo(null);//일부 고객은 메모 없음
        customer2.setActive(true);

        Customer customer3 = new Customer();
        customer3.setName("신희리");
        customer3.setAddress("부산시 해운대구");
        customer3.setPhone("010-6376-5921");
        customer3.setMemo(null); // 일부 고객은 메모 없음
        customer3.setActive(true);

        Customer customer4 = new Customer();
        customer4.setName("훈팔스");
        customer4.setPhone("010-8183-4127");
        customer4.setAddress("인천시 훈팔스");
        customer4.setMemo(null);//일부 고객은 메모 없음
        customer4.setActive(true);


        customerRepository.save(customer1);
        customerRepository.save(customer2);
        customerRepository.save(customer3);
        customerRepository.save(customer4);

        //when

        List<Customer> customers = customerRepository.findByNameContaining("팔");

        //then

        assertThat(customers).isNotNull();
        assertThat(customers.size()).isEqualTo(2);

        for (Customer c : customers){
            System.out.println("고객명: " + c.getName());
            System.out.println("전화번호: " + c.getPhone());
            System.out.println("주소: " + c.getAddress());
            System.out.println("메모: " + c.getMemo());
            System.out.println("----------------------");
        }

    }

    @Test
    @DisplayName("전화번호로 고객 조회 테스트")
    void findByPhone() {

        // given
        Customer customer = new Customer();
        customer.setName("LG도장");
        customer.setPhone("010-7777-8888");
        customer.setAddress("대전시 서구");
        customer.setMemo(null); // 메모 없음
        customer.setActive(true);

        customerRepository.save(customer);

        // when
        Optional<Customer> findCustomer = customerRepository.findByPhone("010-7777-8888");

        // then
        assertThat(findCustomer).isPresent();
        assertThat(findCustomer.get().getName()).isEqualTo("LG도장");
    }

    @Test
    @DisplayName("활성화된 고객만 조회 테스트")
    void findByActiveTrue() {

        // given
        Customer customer1 = new Customer();
        customer1.setName("활성 고객");
        customer1.setPhone("010-9999-0000");
        customer1.setAddress("서울");
        customer1.setMemo(null);
        customer1.setActive(true);

        Customer customer2 = new Customer();
        customer2.setName("비활성 고객");
        customer2.setPhone("010-0000-9999");
        customer2.setAddress("부산");
        customer2.setMemo("거래 중단");
        customer2.setActive(false);

        customerRepository.save(customer1);
        customerRepository.save(customer2);

        // when
        List<Customer> activeCustomers = customerRepository.findByActiveTrue();

        // then
        assertThat(activeCustomers).isNotNull();
        assertThat(activeCustomers).allMatch(customer -> customer.getActive());
    }

}
