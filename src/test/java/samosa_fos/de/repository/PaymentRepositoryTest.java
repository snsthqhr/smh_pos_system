package samosa_fos.de.repository;


import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import samosa_fos.de.domain.Payment;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PaymentRepositoryTest {

    @Autowired
    PaymentRepository paymentRepository;


    @Test
    @DisplayName("고객별 수금 내역 조회 테스트")
    void findByCustomerId() {

        //given
        Payment payment1 = new Payment();
        payment1.setCustomerId(1L);
        payment1.setSalesOrderId(10L);
        payment1.setPaymentDate(LocalDate.now());
        payment1.setAmount(100000);
        payment1.setPaymentMethod("CASH");
        payment1.setMemo("현금 수금");
        payment1.setActive(true);

        Payment payment2 = new Payment();
        payment2.setCustomerId(1L);
        payment2.setSalesOrderId(11L);
        payment2.setPaymentDate(LocalDate.now());
        payment2.setAmount(200000);
        payment2.setPaymentMethod("CARD");
        payment2.setMemo("카드 수금");
        payment2.setActive(true);

        Payment payment3 = new Payment();
        payment3.setCustomerId(2L);
        payment3.setSalesOrderId(12L);
        payment3.setPaymentDate(LocalDate.now());
        payment3.setAmount(300000);
        payment3.setPaymentMethod("TRANSFER");
        payment3.setMemo("계좌이체");
        payment3.setActive(true);

        paymentRepository.save(payment1);
        paymentRepository.save(payment2);
        paymentRepository.save(payment3);



        //when
        List<Payment> payments = paymentRepository.findByCustomerId(1L);
        //then
        assertThat(payments).hasSize(2);
        assertThat(payments).allMatch(payment -> payment.getCustomerId().equals(1L));


    }

}
