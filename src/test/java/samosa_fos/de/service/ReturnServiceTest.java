package samosa_fos.de.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.ReturnRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReturnServiceTest {

    @Autowired
    ReturnService returnService;

    @Autowired
    SalesOrderRepository salesOrderRepository;

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    ArTxRepository arTxRepository;

    @Test
    @DisplayName("정상 반품 시 returnQuantity가 증가하고 ArTx RETURN이 생성된다")
    void addReturnQuantity_success() {
        // given
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(1L);
        salesOrder.setJobSiteId(null);
        salesOrder.setPaymentType("CREDIT");
        salesOrder.setSalesDate(LocalDate.now());
        salesOrder.setTaxPolicy("NO_TAX");
        salesOrder.setMemo("외상 판매");
        salesOrder.setTotalNetAmount(100000);
        salesOrder.setTotalTaxAmount(0);
        salesOrder.setTotalAmount(100000);
        salesOrder.setActive(true);
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(savedOrder.getId());
        item.setProductId(10L);
        item.setQuantity(5);
        item.setUnitPrice(50000);
        item.setSupplyPrice(250000);
        item.setTaxPrice(0);
        item.setTotalPrice(250000);
        item.setReturnQuantity(0);
        item.setActive(true);
        SalesOrderItem savedItem = salesOrderItemRepository.save(item);

        ReturnRequest request = new ReturnRequest();
        request.setSalesOrderItemId(savedItem.getId());
        request.setReturnQuantity(2);
        request.setMemo("일부 반품");

        // when
        returnService.addReturnQuantity(request);

        // then
        SalesOrderItem findItem = salesOrderItemRepository.findById(savedItem.getId()).orElseThrow();
        assertThat(findItem.getReturnQuantity()).isEqualTo(2);

        List<ArTx> arTxList = arTxRepository.findBySalesOrderId(savedOrder.getId());
        assertThat(arTxList).hasSize(1);

        ArTx returnTx = arTxList.get(0);
        assertThat(returnTx.getTxType()).isEqualTo("RETURN");
        assertThat(returnTx.getCustomerId()).isEqualTo(1L);
        assertThat(returnTx.getSalesOrderId()).isEqualTo(savedOrder.getId());
        assertThat(returnTx.getAmount()).isEqualTo(-100000); // 50000 * 2 * (-1)
        assertThat(returnTx.getMemo()).isEqualTo("일부 반품");
    }

    @Test
    @DisplayName("부가세 별도 판매 반품은 공급가와 부가세를 합친 금액으로 RETURN을 생성한다")
    void addReturnQuantity_addVat_includesTaxInReturnAmount() {
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(101L);
        salesOrder.setPaymentType("CREDIT");
        salesOrder.setSalesDate(LocalDate.now());
        salesOrder.setTaxPolicy("ADD_VAT");
        salesOrder.setTotalNetAmount(30000);
        salesOrder.setTotalTaxAmount(3000);
        salesOrder.setTotalAmount(33000);
        salesOrder.setActive(true);
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(savedOrder.getId());
        item.setProductId(10L);
        item.setQuantity(1);
        item.setUnitPrice(30000);
        item.setSupplyPrice(30000);
        item.setTaxPrice(3000);
        item.setTotalPrice(33000);
        item.setReturnQuantity(0);
        item.setActive(true);
        SalesOrderItem savedItem = salesOrderItemRepository.save(item);

        ReturnRequest request = new ReturnRequest();
        request.setSalesOrderItemId(savedItem.getId());
        request.setReturnQuantity(1);
        request.setMemo("부가세 포함 반품");

        returnService.addReturnQuantity(request);

        ArTx returnTx = arTxRepository.findByCustomerId(101L).stream()
                .filter(tx -> "RETURN".equals(tx.getTxType()))
                .findFirst()
                .orElseThrow();
        assertThat(returnTx.getAmount()).isEqualTo(-33000);
    }

    @Test
    @DisplayName("반품 수량이 0 이하이면 예외가 발생한다")
    void addReturnQuantity_invalidQuantity_throwException() {
        // given
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(1L);
        salesOrder.setPaymentType("CREDIT");
        salesOrder.setSalesDate(LocalDate.now());
        salesOrder.setTaxPolicy("NO_TAX");
        salesOrder.setTotalNetAmount(50000);
        salesOrder.setTotalTaxAmount(0);
        salesOrder.setTotalAmount(50000);
        salesOrder.setActive(true);
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(savedOrder.getId());
        item.setProductId(10L);
        item.setQuantity(3);
        item.setUnitPrice(50000);
        item.setSupplyPrice(150000);
        item.setTaxPrice(0);
        item.setTotalPrice(150000);
        item.setReturnQuantity(0);
        item.setActive(true);
        SalesOrderItem savedItem = salesOrderItemRepository.save(item);

        ReturnRequest request = new ReturnRequest();
        request.setSalesOrderItemId(savedItem.getId());
        request.setReturnQuantity(0);
        request.setMemo("잘못된 반품");

        // when & then
        assertThatThrownBy(() -> returnService.addReturnQuantity(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("반품 수량은 1개 이상이어야 합니다.");
    }

    @Test
    @DisplayName("누적 반품 수량이 판매 수량을 초과하면 예외가 발생한다")
    void addReturnQuantity_overQuantity_throwException() {
        // given
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(1L);
        salesOrder.setPaymentType("CREDIT");
        salesOrder.setSalesDate(LocalDate.now());
        salesOrder.setTaxPolicy("NO_TAX");
        salesOrder.setTotalNetAmount(50000);
        salesOrder.setTotalTaxAmount(0);
        salesOrder.setTotalAmount(50000);
        salesOrder.setActive(true);
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(savedOrder.getId());
        item.setProductId(10L);
        item.setQuantity(5);
        item.setUnitPrice(50000);
        item.setSupplyPrice(250000);
        item.setTaxPrice(0);
        item.setTotalPrice(250000);
        item.setReturnQuantity(4); // 이미 4개 반품됨
        item.setActive(true);
        SalesOrderItem savedItem = salesOrderItemRepository.save(item);

        ReturnRequest request = new ReturnRequest();
        request.setSalesOrderItemId(savedItem.getId());
        request.setReturnQuantity(2); // 총 6개가 되어 초과
        request.setMemo("초과 반품");

        // when & then
        assertThatThrownBy(() -> returnService.addReturnQuantity(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("반품 수량이 판매 수량보다 많습니다.");
    }

    @Test
    @DisplayName("여러 번 반품하면 returnQuantity는 누적되고 ArTx는 요청 단위로 생성된다")
    void addReturnQuantity_multipleTimes_createsSeparateReturnTx() {
        // given
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(1L);
        salesOrder.setPaymentType("CREDIT");
        salesOrder.setSalesDate(LocalDate.now());
        salesOrder.setTaxPolicy("NO_TAX");
        salesOrder.setTotalNetAmount(50000);
        salesOrder.setTotalTaxAmount(0);
        salesOrder.setTotalAmount(50000);
        salesOrder.setActive(true);
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(savedOrder.getId());
        item.setProductId(10L);
        item.setQuantity(5);
        item.setUnitPrice(50000);
        item.setSupplyPrice(250000);
        item.setTaxPrice(0);
        item.setTotalPrice(250000);
        item.setReturnQuantity(0);
        item.setActive(true);
        SalesOrderItem savedItem = salesOrderItemRepository.save(item);

        ReturnRequest firstRequest = new ReturnRequest();
        firstRequest.setSalesOrderItemId(savedItem.getId());
        firstRequest.setReturnQuantity(2);
        firstRequest.setMemo("첫 반품");

        ReturnRequest secondRequest = new ReturnRequest();
        secondRequest.setSalesOrderItemId(savedItem.getId());
        secondRequest.setReturnQuantity(1);
        secondRequest.setMemo("두 번째 반품");

        // when
        returnService.addReturnQuantity(firstRequest);
        returnService.addReturnQuantity(secondRequest);

        // then
        SalesOrderItem findItem = salesOrderItemRepository.findById(savedItem.getId()).orElseThrow();
        assertThat(findItem.getReturnQuantity()).isEqualTo(3); // 누적

        List<ArTx> arTxList = arTxRepository.findBySalesOrderId(savedOrder.getId());
        assertThat(arTxList).hasSize(2);

        ArTx firstTx = arTxList.get(0);
        ArTx secondTx = arTxList.get(1);

        assertThat(firstTx.getTxType()).isEqualTo("RETURN");
        assertThat(firstTx.getAmount()).isEqualTo(-100000); // 2개 반품

        assertThat(secondTx.getTxType()).isEqualTo("RETURN");
        assertThat(secondTx.getAmount()).isEqualTo(-50000); // 1개 반품
    }
}
