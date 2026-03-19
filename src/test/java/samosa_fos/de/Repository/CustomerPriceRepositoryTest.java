package samosa_fos.de.Repository;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.CustomerPrice;
import samosa_fos.de.repository.CustomerPriceRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class CustomerPriceRepositoryTest {

    @Autowired
    CustomerPriceRepository customerPriceRepository;

    @Test
    @DisplayName("고객 기본 가격 조회 테스트")
    void findCustomerDefaultPrice() {

        // given
        CustomerPrice customerPrice = new CustomerPrice();
        customerPrice.setCustomerId(1L);
        customerPrice.setProductId(10L);
        customerPrice.setJobSiteId(null); // 현장 없는 기본 고객 가격
        customerPrice.setPrice(52000);
        customerPrice.setActive(true);

        customerPriceRepository.save(customerPrice);

        // when
        Optional<CustomerPrice> findPrice =
                customerPriceRepository.findByCustomerIdAndProductIdAndJobSiteIdIsNullAndActiveTrue(1L, 10L);

        // then
        assertThat(findPrice).isPresent();
        assertThat(findPrice.get().getPrice()).isEqualTo(52000);
        assertThat(findPrice.get().getJobSiteId()).isNull();
    }


    @Test
    @DisplayName("고객 + 현장 가격 조회 테스트")
    void findCustomerJobSitePrice() {

        // given
        CustomerPrice customerPrice = new CustomerPrice();
        customerPrice.setCustomerId(1L);
        customerPrice.setProductId(10L);
        customerPrice.setJobSiteId(100L); // 특정 현장 가격
        customerPrice.setPrice(50000);
        customerPrice.setActive(true);

        customerPriceRepository.save(customerPrice);

        // when
        Optional<CustomerPrice> findPrice =
                customerPriceRepository.findByCustomerIdAndProductIdAndJobSiteIdAndActiveTrue(1L, 10L, 100L);
        //해당 고객의 판매한 제품중에 특정한 현장이 없고 그 고객의 기본 가격이 잘 적용 되는지 확인
        // then
        assertThat(findPrice).isPresent();
        assertThat(findPrice.get().getPrice()).isEqualTo(50000);
        assertThat(findPrice.get().getJobSiteId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("특정 고객의 활성 가격 정책 전체 조회 테스트")
    void findByCustomerIdAndActiveTrue() {

        // given
        CustomerPrice price1 = new CustomerPrice();
        price1.setCustomerId(1L);
        price1.setProductId(10L);
        price1.setJobSiteId(null);
        price1.setPrice(52000);
        price1.setActive(true);

        CustomerPrice price2 = new CustomerPrice();
        price2.setCustomerId(1L);
        price2.setProductId(11L);
        price2.setJobSiteId(100L);
        price2.setPrice(48000);
        price2.setActive(true);

        CustomerPrice price3 = new CustomerPrice();
        price3.setCustomerId(2L);
        price3.setProductId(10L);
        price3.setJobSiteId(null);
        price3.setPrice(60000);
        price3.setActive(true);

        CustomerPrice price4 = new CustomerPrice();
        price4.setCustomerId(1L);
        price4.setProductId(12L);
        price4.setJobSiteId(null);
        price4.setPrice(30000);
        price4.setActive(false); // 비활성

        customerPriceRepository.save(price1);
        customerPriceRepository.save(price2);
        customerPriceRepository.save(price3);
        customerPriceRepository.save(price4);

        // when
        List<CustomerPrice> result = customerPriceRepository.findByCustomerIdAndActiveTrue(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(CustomerPrice::getActive);
        assertThat(result).allMatch(price -> price.getCustomerId().equals(1L));
    }


}

