package samosa_fos.de.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.domain.SupplierConfig;
import samosa_fos.de.dto.statement.StatementItemResponse;
import samosa_fos.de.dto.statement.StatementResponse;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;
import samosa_fos.de.repository.SupplierConfigRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatementService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ProductRepository productRepository;
    private final SupplierConfigRepository supplierConfigRepository;

    public StatementService(SalesOrderRepository salesOrderRepository,
                            SalesOrderItemRepository salesOrderItemRepository,
                            ProductRepository productRepository,
                            SupplierConfigRepository supplierConfigRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.productRepository = productRepository;
        this.supplierConfigRepository = supplierConfigRepository;
    }

    /**
     * 거래명세표 데이터 생성
     *
     * 현재 1차 버전은 단일 SalesOrder 기준으로 생성한다.
     * 추후에는 customerId + 기간 조건을 받아서 기간 내 판매 품목 전체를 거래명세표에 표시하는 구조로 확장한다.
     */
    public StatementResponse getStatement(Long salesOrderId) {

        // 1. 입력값 검증
        validateSalesOrderId(salesOrderId);

        // 2. 판매 전표 조회
        SalesOrder salesOrder = getSalesOrder(salesOrderId);

        // 3. 판매 전표에 속한 품목 조회
        List<SalesOrderItem> salesOrderItems =
                salesOrderItemRepository.findBySalesOrderId(salesOrder.getId());

        // 4. 공급자 설정 정보 조회
        SupplierConfig supplierConfig = getActiveSupplierConfig();

        // 5. 판매 품목을 거래명세표 품목 DTO로 변환
        List<StatementItemResponse> statementItems =
                convertItemsToStatementItems(salesOrderItems);

        // 6. 거래명세표 합계 계산
        int totalSupplyPrice = calculateTotalSupplyPrice(statementItems);
        int totalTaxPrice = calculateTotalTaxPrice(statementItems);
        int totalAmount = totalSupplyPrice + totalTaxPrice;

        // 7. 거래명세표 응답 DTO 조립
        StatementResponse response = new StatementResponse();

        // 7-1. 발행 및 거래처 정보 세팅
        response.setIssueDate(LocalDate.now());
        response.setCustomerId(salesOrder.getCustomerId());

        // TODO: CustomerRepository 연결 후 실제 거래처명으로 교체
        response.setCustomerName("거래처명 필요");

        // TODO: 인수 담당자 입력 기능 추가 시 실제 값으로 교체
        response.setReceiverName("귀하");

        // 7-2. 합계 정보 세팅
        response.setTotalSupplyPrice(totalSupplyPrice);
        response.setTotalTaxPrice(totalTaxPrice);
        response.setTotalAmount(totalAmount);

        // 7-3. 공급자 정보 세팅
        applySupplierConfig(response, supplierConfig);

        // 7-4. 기타 사항 세팅
        response.setBankAccount(supplierConfig.getBankAccount());
        response.setAccountHolder(supplierConfig.getAccountHolder());

        // TODO: ArService 또는 ArTxRepository 연결 후 현재 미수금으로 교체
        response.setRemainingArBalance(0);

        // 7-5. 품목 정보 세팅
        response.setItems(statementItems);

        return response;
    }

    /**
     * salesOrderId 입력값 검증
     */
    private void validateSalesOrderId(Long salesOrderId) {
        if (salesOrderId == null) {
            throw new IllegalArgumentException("salesOrderId는 필수입니다.");
        }
    }

    /**
     * 판매 전표 조회
     */
    private SalesOrder getSalesOrder(Long salesOrderId) {
        return salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new IllegalArgumentException("판매 전표가 존재하지 않습니다."));
    }

    /**
     * 활성 공급자 설정 조회
     *
     * 거래명세표의 공급자 영역에 들어갈 회사 정보를 가져온다.
     * active=true인 설정이 없으면 거래명세표를 생성할 수 없도록 예외 처리한다.
     */
    private SupplierConfig getActiveSupplierConfig() {
        return supplierConfigRepository.findFirstByActiveTrueOrderByIdDesc()
                .orElseThrow(() -> new IllegalStateException("활성화된 공급자 설정 정보가 없습니다."));
    }

    /**
     * SalesOrderItem 리스트를 StatementItemResponse 리스트로 변환
     */
    private List<StatementItemResponse> convertItemsToStatementItems(List<SalesOrderItem> salesOrderItems) {

        List<StatementItemResponse> statementItems = new ArrayList<>();

        int no = 1;

        for (SalesOrderItem item : salesOrderItems) {

            Product product = productRepository.findById(item.getProductId())
                    .orElse(null);

            StatementItemResponse dto = new StatementItemResponse();

            dto.setNo(no++);
            dto.setProductId(item.getProductId());
            dto.setProductName(product != null ? product.getProductName() : "상품 조회 불가");
            dto.setSpec(product != null ? product.getVariant() : null);
            dto.setUnit(product != null ? product.getUnit() : null);

            dto.setQuantity(item.getQuantity());
            dto.setUnitPrice(item.getUnitPrice());

            dto.setSupplyPrice(item.getSupplyPrice());
            dto.setTaxPrice(item.getTaxPrice());
            dto.setTotalPrice(item.getTotalPrice());

            statementItems.add(dto);
        }

        return statementItems;
    }

    /**
     * 공급가액 합계 계산
     */
    private int calculateTotalSupplyPrice(List<StatementItemResponse> items) {
        return items.stream()
                .mapToInt(item -> item.getSupplyPrice() == null ? 0 : item.getSupplyPrice())
                .sum();
    }

    /**
     * 세액 합계 계산
     */
    private int calculateTotalTaxPrice(List<StatementItemResponse> items) {
        return items.stream()
                .mapToInt(item -> item.getTaxPrice() == null ? 0 : item.getTaxPrice())
                .sum();
    }

    /**
     * 공급자 설정 정보를 거래명세표 응답 DTO에 반영
     */
    private void applySupplierConfig(StatementResponse response, SupplierConfig supplierConfig) {

        response.setSupplierBusinessNumber(supplierConfig.getBusinessNumber());
        response.setSupplierName(supplierConfig.getSupplierName());
        response.setSupplierCeoName(supplierConfig.getCeoName());
        response.setSupplierAddress(supplierConfig.getAddress());
        response.setSupplierBusinessType(supplierConfig.getBusinessType());
        response.setSupplierBusinessItem(supplierConfig.getBusinessItem());
        response.setSupplierPhone(supplierConfig.getPhone());
        response.setSupplierFax(supplierConfig.getFax());
    }
}