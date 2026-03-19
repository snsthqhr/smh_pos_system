package samosa_fos.de.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.SalesOrder;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional
public class SalesOrderTest {

    @Autowired
    SalesOrderRepository salesOrderRepository;


    @Test
    @DisplayName("세일즈 오더 헤더 저장 테스트")
    public void saveSalesOrder(){
        //given
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(1L);
        salesOrder.setJobSiteId(100L);
        salesOrder.setPaymentType("CARD");
        salesOrder.setSalesDate(LocalDateTime.now().toLocalDate());
        salesOrder.setTotalAmount(null);
        salesOrder.setTaxPolicy("No_TAX");
        salesOrder.setMemo("배건우 사장 강릉 건");
        salesOrder.setActive(Boolean.TRUE);

        //when

        SalesOrder savedSalesOrder = salesOrderRepository.save(salesOrder);

        //then
        assertThat(salesOrderRepository.findByCustomerId(1L).get(0)).isEqualTo(salesOrder);
        assertThat(savedSalesOrder.getId()).isNotNull();
        assertThat(savedSalesOrder.getId()).isEqualTo(salesOrder.getId());
        assertThat(savedSalesOrder.getSalesDate()).isEqualTo(salesOrder.getSalesDate());
        assertThat(savedSalesOrder.getCustomerId()).isEqualTo(salesOrder.getCustomerId());
    }

}
