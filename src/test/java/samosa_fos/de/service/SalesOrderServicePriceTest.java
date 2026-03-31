package samosa_fos.de.service;


import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import samosa_fos.de.domain.CustomerPrice;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.CreateSalesOrderItemRequest;
import samosa_fos.de.dto.sales.CreateSalesOrderRequest;
import samosa_fos.de.repository.CustomerPriceRepository;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional
public class SalesOrderServicePriceTest {

    @Autowired
    SalesOrderService salesOrderService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CustomerPriceRepository customerPriceRepository;

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;
    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Test
    @DisplayName("직접 입력 가격이 있으면 직접 입력 가격을 사용한다")
    void createSalesOrderUseRequestUnitPrice() {

        //given
        Product product = new Product();
        product.setCode("P-100");
        product.setProductName("아이생각 수성 내부");
        product.setProductNickname("아이생각");
        product.setVariant("18L");
        product.setUnit("말");
        product.setBrand("삼화");
        product.setCategory("수성");
        product.setCostPrice(35000);
        product.setSalePrice(60000);
        product.setStockQuantity(10);
        //데이터를 저장
        Product savedProduct = productRepository.save(product);

        CreateSalesOrderItemRequest itemRequest = new CreateSalesOrderItemRequest();
        itemRequest.setUnitPrice(52000);
        itemRequest.setQuantity(2);
        itemRequest.setProductId(savedProduct.getId());

        List<CreateSalesOrderItemRequest> itemRequests = new ArrayList<>();
        itemRequests.add(itemRequest);

        CreateSalesOrderRequest request = new CreateSalesOrderRequest();
        request.setItems(itemRequests);
        request.setTaxPolicy("NO_TAX");
        request.setMemo("직접 입력 가격 테스트");
        request.setJobSiteId(100L);

        //when
        //데이터를 저장할때
        SalesOrder savedOrder = salesOrderService.createSalesOrder(request);


        //then
        List<SalesOrderItem> savedItems = salesOrderItemRepository.findBySalesOrderId(savedOrder.getId());
        SalesOrderItem savedItem = savedItems.get(0);

        assertThat(savedItem.getUnitPrice()).isEqualTo(52000);
        assertThat(savedItem.getTotalPrice()).isEqualTo(104000);
        assertThat(savedItem.getSupplyPrice()).isEqualTo(104000);
    }

    @Test
    @DisplayName("직접 입력 가격이 없고 현장이 있으면 직접 입력 가격을 사용한다")
    void creaateSalesOrderUseJobsitePrice(){

        //given
        Product product = new Product();

        product.setCode("P-100");
        product.setProductName("아이생각 수성 내부");
        product.setProductNickname("아이생각");
        product.setVariant("18L");
        product.setUnit("말");
        product.setBrand("삼화");
        product.setCategory("수성");
        product.setCostPrice(35000);
        product.setSalePrice(60000);
        product.setStockQuantity(10);

        Product savedProduct = productRepository.save(product);
        //위에 코드는 기본적으로 등록되어 있는 프로덕트라고 생각하면된다.

        CustomerPrice jobSitePrice = new CustomerPrice();
        jobSitePrice.setCustomerId(1L);
        jobSitePrice.setProductId(savedProduct.getId());
        jobSitePrice.setJobSiteId(100L);
        jobSitePrice.setPrice(27000);
        jobSitePrice.setActive(true);
        customerPriceRepository.save(jobSitePrice);

        CreateSalesOrderItemRequest itemRequest = new CreateSalesOrderItemRequest();
        itemRequest.setProductId(savedProduct.getId());
        itemRequest.setUnitPrice(null);
        itemRequest.setQuantity(2);



        List<CreateSalesOrderItemRequest> items = new ArrayList<>();
        items.add(itemRequest);

        CreateSalesOrderRequest request = new CreateSalesOrderRequest();
        request.setJobSiteId(jobSitePrice.getJobSiteId());
        request.setPaymentType("CARD");
        request.setMemo("현장 가격 테스트");
        request.setCustomerId(jobSitePrice.getCustomerId());
        request.setItems(items);
        request.setTaxPolicy("NO_TAX");

        //when

        SalesOrder savedOrder = salesOrderService.createSalesOrder(request);


        //then
        SalesOrderItem savedItem = salesOrderItemRepository.findBySalesOrderId(savedOrder.getId()).get(0);

        assertThat(savedItem.getUnitPrice()).isEqualTo(27000);
        assertThat(savedItem.getSupplyPrice()).isEqualTo(54000);
        assertThat(savedOrder.getTotalAmount()).isEqualTo(54000);


    }

    @Test
    @DisplayName("현장 가격이 없고 고객 기본 가격이 있으면 고객 기본 가격을 사용한다.")
    void createSalesOrder_useCustomerDefaultPrice() {
        
    }
}
