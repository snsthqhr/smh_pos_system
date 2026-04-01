package samosa_fos.de.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.CustomerPrice;
import samosa_fos.de.domain.Payment;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.CreateSalesOrderItemRequest;
import samosa_fos.de.dto.sales.CreateSalesOrderRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.CustomerPriceRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.time.LocalDate;

@Service
@Transactional
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final CustomerPriceRepository customerPriceRepository;
    private final ProductRepository productRepository;
    private final ArTxRepository arTxRepository;
    private final PaymentRepository paymentRepository;

    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             SalesOrderItemRepository salesOrderItemRepository,
                             CustomerPriceRepository customerPriceRepository,
                             ProductRepository productRepository,
                             ArTxRepository arTxRepository,
                             PaymentRepository paymentRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.customerPriceRepository = customerPriceRepository;
        this.productRepository = productRepository;
        this.arTxRepository = arTxRepository;
        this.paymentRepository = paymentRepository;
    }

    // 기본 베이스는 dto를 활용하여 주입받는다
    public SalesOrder createSalesOrder(CreateSalesOrderRequest request) {

        // 판매전표 헤더 생성
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(request.getCustomerId());
        salesOrder.setJobSiteId(request.getJobSiteId());
        salesOrder.setActive(true);
        salesOrder.setMemo(request.getMemo());
        salesOrder.setTaxPolicy(request.getTaxPolicy());
        salesOrder.setPaymentType(request.getPaymentType());
        salesOrder.setSalesDate(LocalDate.now());

        // 총액 초기화
        salesOrder.setTotalNetAmount(0);
        salesOrder.setTotalTaxAmount(0);
        salesOrder.setTotalAmount(0);

        int totalNetAmount = 0;   // 공급가액
        int totalTaxAmount = 0;   // 부가세 총액
        int totalAmount = 0;      // 최종 금액 총액

        // salesOrderId 생성을 위해 먼저 저장
        SalesOrder savedSalesOrder = salesOrderRepository.save(salesOrder);

        for (CreateSalesOrderItemRequest itemRequest : request.getItems()) {

            SalesOrderItem item = new SalesOrderItem();

            // generate된 키값을 이 아이템에 salesOrderId로 설정
            item.setSalesOrderId(savedSalesOrder.getId());

            // 제품의 아이디
            item.setProductId(itemRequest.getProductId());

            // 제품의 수량
            item.setQuantity(itemRequest.getQuantity());

            // 최종 판매 단가 결정
            item.setUnitPrice(
                    determineUnitPrice(
                            request.getCustomerId(),
                            request.getJobSiteId(),
                            item.getProductId(),
                            itemRequest.getUnitPrice()
                    )
            );

            // 판매 시점에서는 반품이 0
            item.setReturnQuantity(0);

            // 현재 제품의 공급가
            int curItemSupplyPrice = item.getUnitPrice() * item.getQuantity();
            item.setSupplyPrice(curItemSupplyPrice);

            // 이 주문의 공급가 총액
            totalNetAmount += curItemSupplyPrice;

            // 현재 아이템의 세금 계산
            int curItemTaxPrice = 0;
            if ("ADD_VAT".equals(request.getTaxPolicy())) {
                curItemTaxPrice = (int) (curItemSupplyPrice * 0.1);
            }
            item.setTaxPrice(curItemTaxPrice);

            // 이 주문의 세금 총액
            totalTaxAmount += curItemTaxPrice;

            // 현재 아이템의 최종 금액 계산
            int curItemTotalPrice = curItemSupplyPrice + curItemTaxPrice;
            item.setTotalPrice(curItemTotalPrice);

            item.setActive(true);
            salesOrderItemRepository.save(item);

            // 주문 총 금액 갱신
            totalAmount += item.getTotalPrice();

            // 아이템의 가격을 CustomerPrice로 저장할지 안 할지 결정하는 함수 호출
            saveCustomerPricePolicy(
                    request.getCustomerId(),
                    request.getJobSiteId(),
                    item.getProductId(),
                    item.getUnitPrice(),
                    request.getPriceApplyPolicy()
            );
        }

        // 전표 총액 반영
        savedSalesOrder.setTotalNetAmount(totalNetAmount);
        savedSalesOrder.setTotalTaxAmount(totalTaxAmount);
        savedSalesOrder.setTotalAmount(totalAmount);

        // 외상 / 즉시결제 분기
        if ("CREDIT".equals(savedSalesOrder.getPaymentType())) {
            handleCreditSale(
                    savedSalesOrder.getTotalAmount(),
                    savedSalesOrder.getCustomerId(),
                    savedSalesOrder.getId()
            );
        } else {
            handleImmediatePayment(
                    savedSalesOrder.getCustomerId(),
                    savedSalesOrder.getId(),
                    savedSalesOrder.getPaymentType(),
                    savedSalesOrder.getTotalAmount()
            );
        }

        return savedSalesOrder;
    }

    // 즉시 입금 하는 경우
    private void handleImmediatePayment(Long customerId,
                                        Long salesOrderId,
                                        String paymentType,
                                        int totalAmount) {

        Payment payment = new Payment();
        payment.setCustomerId(customerId);
        payment.setSalesOrderId(salesOrderId);
        payment.setPaymentDate(LocalDate.now());
        payment.setActive(true);
        payment.setAmount(totalAmount);
        payment.setPaymentMethod(paymentType);
        payment.setMemo("판매 즉시결제");

        paymentRepository.save(payment);
    }

    // 미수로 하는 경우
    private void handleCreditSale(int totalAmount,
                                  Long customerId,
                                  Long salesOrderId) {

        ArTx arTx = new ArTx();
        arTx.setSalesOrderId(salesOrderId);
        arTx.setActive(true);
        arTx.setAmount(totalAmount);
        arTx.setTxDate(LocalDate.now());
        arTx.setTxType("SALE");
        arTx.setMemo("외상 판매");
        arTx.setCustomerId(customerId);

        arTxRepository.save(arTx);
    }

    public Integer determineUnitPrice(Long customerId,
                                      Long jobSiteId,
                                      Long productId,
                                      Integer requestUnitPrice) {

        // 1. 사용자가 직접 가격 입력한 경우
        if (requestUnitPrice != null) {
            return requestUnitPrice;
        }

        // 2. 현장 가격 조회
        if (jobSiteId != null) {
            CustomerPrice jobSitePrice = customerPriceRepository
                    .findByCustomerIdAndProductIdAndJobSiteIdAndActiveTrue(customerId, productId, jobSiteId)
                    .orElse(null);

            if (jobSitePrice != null) {
                return jobSitePrice.getPrice();
            }
        }

        // 3. 고객 기본 가격 조회
        CustomerPrice customerDefaultPrice = customerPriceRepository
                .findByCustomerIdAndProductIdAndJobSiteIdIsNullAndActiveTrue(customerId, productId)
                .orElse(null);

        if (customerDefaultPrice != null) {
            return customerDefaultPrice.getPrice();
        }

        // 4. 상품 기본 가격 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("해당 상품이 존재하지 않습니다. productId=" + productId)
                );

        if (product.getSalePrice() == null) {
            throw new IllegalArgumentException("상품 기본 가격이 설정되지 않았습니다. productId=" + productId);
        }

        return product.getSalePrice();
    }

    private void saveCustomerPricePolicy(Long customerId,
                                         Long jobSiteId,
                                         Long productId,
                                         Integer unitPrice,
                                         String pricePolicy) {

        // 이번만 적용이면 저장하지 않음
        if ("ONE_TIME_ONLY".equals(pricePolicy)) {
            return;
        }

        // 기본값은 저장하는 정책으로 본다
        if (jobSiteId != null) {
            saveOrUpdateJobSitePrice(customerId, jobSiteId, productId, unitPrice);
        } else {
            saveOrUpdateCustomerDefaultPrice(customerId, productId, unitPrice);
        }
    }

    // 고객 기본 가격 저장 또는 수정
    private void saveOrUpdateCustomerDefaultPrice(Long customerId,
                                                  Long productId,
                                                  Integer unitPrice) {

        CustomerPrice customerPrice = customerPriceRepository
                .findByCustomerIdAndProductIdAndJobSiteIdIsNullAndActiveTrue(customerId, productId)
                .orElse(null);

        if (customerPrice != null) {
            customerPrice.setPrice(unitPrice);
            return;
        }

        CustomerPrice newCustomerPrice = new CustomerPrice();
        newCustomerPrice.setCustomerId(customerId);
        newCustomerPrice.setProductId(productId);
        newCustomerPrice.setJobSiteId(null);
        newCustomerPrice.setPrice(unitPrice);
        newCustomerPrice.setActive(true);

        customerPriceRepository.save(newCustomerPrice);
    }

    // 현장 가격 저장 또는 수정
    private void saveOrUpdateJobSitePrice(Long customerId,
                                          Long jobSiteId,
                                          Long productId,
                                          Integer unitPrice) {

        CustomerPrice customerPrice = customerPriceRepository
                .findByCustomerIdAndProductIdAndJobSiteIdAndActiveTrue(customerId, productId, jobSiteId)
                .orElse(null);

        if (customerPrice != null) {
            customerPrice.setPrice(unitPrice);
            return;
        }

        CustomerPrice newCustomerPrice = new CustomerPrice();
        newCustomerPrice.setCustomerId(customerId);
        newCustomerPrice.setProductId(productId);
        newCustomerPrice.setJobSiteId(jobSiteId);
        newCustomerPrice.setPrice(unitPrice);
        newCustomerPrice.setActive(true);

        customerPriceRepository.save(newCustomerPrice);
    }
}