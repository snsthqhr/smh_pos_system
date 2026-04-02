package samosa_fos.de.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.Payment;
import samosa_fos.de.dto.sales.RegisterPaymentRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional
class PaymentServiceTest {

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    ArTxRepository arTxRepository;

    @Test
    @DisplayName("정상 수금 등록 테스트")
    void registerPayment_success() {

        //given

        ArTx saleTx = new ArTx();
        saleTx.setCustomerId(1L);
        saleTx.setSalesOrderId(100L);
        saleTx.setTxDate(LocalDate.of(2026, 4, 1));
        saleTx.setTxType("SALE");
        saleTx.setAmount(500000);   // 외상 판매
        saleTx.setMemo("외상 판매");
        saleTx.setActive(true);
        arTxRepository.save(saleTx);

        RegisterPaymentRequest request = new RegisterPaymentRequest();
        request.setCustomerId(1L);
        request.setSalesOrderId(100L);
        request.setPaymentDate(LocalDate.of(2026, 4, 5));
        request.setAmount(200000);
        request.setPaymentMethod("TRANSFER");
        request.setMemo("일부 수금");

        //when
        Payment savedPayment = paymentService.registerPayment(request);

        //then

        assertThat(savedPayment.getId()).isNotNull();
        assertThat(savedPayment.getCustomerId()).isEqualTo(1L);
        assertThat(savedPayment.getSalesOrderId()).isEqualTo(100L);
        assertThat(savedPayment.getAmount()).isEqualTo(200000);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo("TRANSFER");

        List<ArTx> arTxList = arTxRepository.findByCustomerIdAndActiveTrue(1L);


        ArTx paymentTx = arTxList.stream()
                .filter(tx -> "PAYMENT".equals(tx.getTxType()))
                .findFirst()
                .orElseThrow();

        assertThat(paymentTx.getAmount()).isEqualTo(-200000);
        assertThat(paymentTx.getSalesOrderId()).isEqualTo(100L);


    }


    @Test
    @DisplayName("현재 미수금보다 많이 수금하려고 하면 예외 발생")

}
