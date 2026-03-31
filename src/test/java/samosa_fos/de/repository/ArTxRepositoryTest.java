package samosa_fos.de.repository;


import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import samosa_fos.de.domain.ArTx;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@Transactional
public class ArTxRepositoryTest {

    @Autowired
    ArTxRepository arTxRepository;

    @Test
    @DisplayName("고객별 미수금 원장 조회 테스트")
    void findByCustomerId() {

        //given
        ArTx tx1 = new ArTx();
        tx1.setCustomerId(1L);
        tx1.setSalesOrderId(10L);
        tx1.setTxDate(LocalDate.now());
        tx1.setTxType("SALE");
        tx1.setAmount(500000);
        tx1.setActive(true);

        ArTx tx2 = new ArTx();
        tx2.setCustomerId(1L);
        tx2.setSalesOrderId(11L);
        tx2.setTxDate(LocalDate.now());
        tx2.setTxType("PAYMENT");
        tx2.setAmount(-200000);
        tx2.setActive(true);

        ArTx tx3 = new ArTx();
        tx3.setCustomerId(2L);
        tx3.setSalesOrderId(12L);
        tx3.setTxDate(LocalDate.now());
        tx3.setTxType("SALE");
        tx3.setAmount(300000);
        tx3.setActive(true);


        arTxRepository.save(tx1);
        arTxRepository.save(tx2);
        arTxRepository.save(tx3);

        //when

        List<ArTx> txList = arTxRepository.findByCustomerId(1L);

        //then
        assertThat(txList).hasSize(2);
        assertThat(txList).allMatch(tx -> tx.getCustomerId().equals(1L));

    }

    @Test
    @DisplayName("고객 + 거래유형 기준 조회 테스트")
    void findByCustomerIdAndTxType() {
        //given
        ArTx tx1 = new ArTx();
        tx1.setCustomerId(1L);
        tx1.setTxDate(LocalDate.now());
        tx1.setTxType("SALE");
        tx1.setAmount(500000);
        tx1.setActive(true);

        ArTx tx2 = new ArTx();
        tx2.setCustomerId(1L);
        tx2.setTxDate(LocalDate.now());
        tx2.setTxType("PAYMENT");
        tx2.setAmount(-100000);
        tx2.setActive(true);

        ArTx tx3 = new ArTx();
        tx3.setCustomerId(1L);
        tx3.setTxDate(LocalDate.now());
        tx3.setTxType("SALE");
        tx3.setAmount(300000);
        tx3.setActive(true);

        arTxRepository.save(tx1);
        arTxRepository.save(tx2);
        arTxRepository.save(tx3);

        //when
        List<ArTx> arTxes = arTxRepository.findByCustomerIdAndTxType(1L,"SALE");
        //then

        assertThat(arTxes).hasSize(2);
        assertThat(arTxes).allMatch(arTx -> arTx.getTxType().equals("SALE"));

    }

    @Test
    @DisplayName("고객별 현재 미수금 합계 계산 테스트")
    void calculateCurrentArAmount() {
        //given
        ArTx tx1 = new ArTx();
        tx1.setCustomerId(1L);
        tx1.setTxDate(LocalDate.now());
        tx1.setTxType("SALE");
        tx1.setAmount(500000);
        tx1.setActive(true);

        ArTx tx2 = new ArTx();
        tx2.setCustomerId(1L);
        tx2.setTxDate(LocalDate.now());
        tx2.setTxType("PAYMENT");
        tx2.setAmount(-200000);
        tx2.setActive(true);

        ArTx tx3 = new ArTx();
        tx3.setCustomerId(1L);
        tx3.setTxDate(LocalDate.now());
        tx3.setTxType("RETURN");
        tx3.setAmount(-50000);
        tx3.setActive(true);

        arTxRepository.save(tx1);
        arTxRepository.save(tx2);
        arTxRepository.save(tx3);

        //when

        List<ArTx> txList = arTxRepository.findByCustomerId(1L);


        //then

        int balance = txList.stream()
                .mapToInt(ArTx::getAmount).sum();//ArTx를 객체를 Artx객체 안에 있는 int값으로 변환 해준다.

        assertThat(balance).isEqualTo(250000);
    }

}
