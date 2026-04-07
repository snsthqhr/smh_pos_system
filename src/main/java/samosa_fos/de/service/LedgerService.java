package samosa_fos.de.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.*;

import samosa_fos.de.dto.sales.LedgerRowDto;
import samosa_fos.de.dto.sales.LedgerSearchRequest;
import samosa_fos.de.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LedgerService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final ArTxRepository arTxRepository;

    public LedgerService(SalesOrderRepository salesOrderRepository,
                         SalesOrderItemRepository salesOrderItemRepository,
                         PaymentRepository paymentRepository,
                         ProductRepository productRepository,
                         ArTxRepository arTxRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.arTxRepository = arTxRepository;
    }

    // 거래처 원장 조회
    public List<LedgerRowDto> getLedgerRows(LedgerSearchRequest request) {

        // 1. 조회 조건 검증
        validateLedgerSearchRequest(request);

        // 2. 판매 전표 조회
        List<SalesOrder> salesOrders = getSalesOrdersByCondition(request);

        // 2.5 시작 미수금 잔액 계산
        int openingBalance = calculateOpeningBalance(
                request.getCustomerId(),
                request.getStartDate()
        );

        // 3. 판매 전표를 원장 행으로 변환
        List<LedgerRowDto> ledgerRows = new ArrayList<>();
        ledgerRows.addAll(convertSalesOrdersToLedgerRows(salesOrders));

        // 4. 수금 내역 조회
        List<Payment> payments = getPaymentsByCondition(request);

        // 5. 수금 내역을 원장 행으로 변환
        ledgerRows.addAll(convertPaymentsToLedgerRows(payments));

        // 6. 반품 행 추가 (추후 고도화)
        // ledgerRows.addAll(convertReturnsToLedgerRows(request));

        // 7. 거래일자 기준 정렬
        ledgerRows.sort(
                Comparator.comparing(LedgerRowDto::getTxDate)
                        .thenComparing(row -> row.getSalesOrderId() == null ? Long.MAX_VALUE : row.getSalesOrderId())
                        .thenComparing(row -> Boolean.TRUE.equals(row.getSummaryRow()) ? 1 : 0)
        );

        // 8. 필터 적용
        List<LedgerRowDto> filteredRows = applyFilters(request, ledgerRows);

        // 9. 누적잔액 계산
        calculateRunningBalance(filteredRows, openingBalance);

        return filteredRows;
    }

    private int calculateOpeningBalance(Long customerId, LocalDate startDate) {

        if (startDate == null)
            return 0;

        List<ArTx> dateBeforeArTx = arTxRepository.findByCustomerIdAndTxDateBeforeAndActiveTrue(customerId,startDate);

        return dateBeforeArTx.stream()
                .mapToInt(ArTx::getAmount)
                .sum();

    }

    // 조회 조건 검증
    private void validateLedgerSearchRequest(LedgerSearchRequest request) {
        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException("customerId는 필수입니다.");
        }

        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
            }
        }
    }

    // 조건에 맞는 판매 전표 조회
    private List<SalesOrder> getSalesOrdersByCondition(LedgerSearchRequest request) {

        if (request.getStartDate() != null && request.getEndDate() != null) {
            return salesOrderRepository.findByCustomerIdAndSalesDateBetweenAndActiveTrue(
                    request.getCustomerId(),
                    request.getStartDate(),
                    request.getEndDate()
            );
        }

        return salesOrderRepository.findByCustomerIdAndActiveTrue(request.getCustomerId());
    }

    // 판매 전표 -> 원장 행 변환
    private List<LedgerRowDto> convertSalesOrdersToLedgerRows(List<SalesOrder> salesOrders) {

        List<LedgerRowDto> rows = new ArrayList<>();

        for (SalesOrder salesOrder : salesOrders) {

            List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderId(salesOrder.getId());

            int totalQuantity = 0;

            for (SalesOrderItem item : items) {
                LedgerRowDto row = new LedgerRowDto();

                row.setTxDate(salesOrder.getSalesDate());
                row.setSalesOrderId(salesOrder.getId());
                row.setCustomerId(salesOrder.getCustomerId());

                // 거래구분
                if ("CREDIT".equals(salesOrder.getPaymentType())) {
                    row.setTxType("판매(외상)");
                    row.setArDelta(0); // 아이템 줄에서는 0
                } else {
                    row.setTxType("판매(즉시결제)");
                    row.setArDelta(0); // 아이템 줄에서는 0
                }

                // 상품 정보
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product != null) {
                    row.setProductName(product.getProductName());
                    row.setUnit(product.getUnit());
                } else {
                    row.setProductName("상품 조회 불가");
                    row.setUnit(null);
                }

                // 품목 정보
                row.setUnitPrice(item.getUnitPrice());
                row.setQuantity(item.getQuantity());
                row.setSupplyPrice(item.getSupplyPrice());
                row.setTaxPrice(item.getTaxPrice());
                row.setSaleAmount(item.getTotalPrice());

                // 즉시결제는 각 품목 줄에서도 수금금액 표시
                if ("CREDIT".equals(salesOrder.getPaymentType())) {
                    row.setPaymentAmount(0);
                } else {
                    row.setPaymentAmount(item.getTotalPrice());
                }

                row.setSummaryRow(false);
                row.setMemo(salesOrder.getMemo());

                rows.add(row);

                totalQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
            }

            // 주문 합계 행 추가
            LedgerRowDto summaryRow = createOrderSummaryRow(salesOrder, totalQuantity);
            rows.add(summaryRow);
        }

        return rows;
    }

    // 주문 합계 행 생성
    private LedgerRowDto createOrderSummaryRow(SalesOrder salesOrder, int totalQuantity) {

        LedgerRowDto row = new LedgerRowDto();

        row.setTxDate(salesOrder.getSalesDate());
        row.setSalesOrderId(salesOrder.getId());
        row.setCustomerId(salesOrder.getCustomerId());
        row.setSummaryRow(true);

        row.setTxType("오더합계");
        row.setProductName("[오더 합계]");
        row.setUnit(null);
        row.setUnitPrice(null);
        row.setQuantity(totalQuantity);

        row.setSupplyPrice(salesOrder.getTotalNetAmount());
        row.setTaxPrice(salesOrder.getTotalTaxAmount());
        row.setSaleAmount(salesOrder.getTotalAmount());
        row.setMemo(salesOrder.getMemo());

        if ("CREDIT".equals(salesOrder.getPaymentType())) {
            row.setPaymentAmount(0);
            row.setArDelta(salesOrder.getTotalAmount());
        } else {
            row.setPaymentAmount(salesOrder.getTotalAmount());
            row.setArDelta(0);
        }

        return row;
    }

    // 조건에 맞는 수금 내역 조회
    private List<Payment> getPaymentsByCondition(LedgerSearchRequest request) {

        if (request.getStartDate() != null && request.getEndDate() != null) {
            return paymentRepository.findByCustomerIdAndPaymentDateBetween(
                    request.getCustomerId(),
                    request.getStartDate(),
                    request.getEndDate()
            );
        }

        return paymentRepository.findByCustomerIdAndActiveTrue(request.getCustomerId());
    }

    // 수금 내역 -> 원장 행 변환
    private List<LedgerRowDto> convertPaymentsToLedgerRows(List<Payment> payments) {

        List<LedgerRowDto> rows = new ArrayList<>();

        for (Payment payment : payments) {

            LedgerRowDto row = new LedgerRowDto();

            // 기본 정보
            row.setTxDate(payment.getPaymentDate());
            row.setSalesOrderId(payment.getSalesOrderId());
            row.setCustomerId(payment.getCustomerId());
            row.setTxType("수금");

            // 품목 관련 없음
            row.setProductName(null);
            row.setUnit(null);
            row.setUnitPrice(null);
            row.setQuantity(null);

            // 금액 정보
            row.setSupplyPrice(0);
            row.setTaxPrice(0);
            row.setSaleAmount(0);

            // 수금금액은 표시용
            row.setPaymentAmount(payment.getAmount());

            // Payment에서는 미수금 변화를 직접 반영하지 않음
            // 미수금 변화는 ArTx 기준으로만 처리
            row.setArDelta(0);

            row.setSummaryRow(false);
            row.setMemo(payment.getMemo());

            rows.add(row);
        }

        return rows;
    }

    // 필터 적용
    private List<LedgerRowDto> applyFilters(LedgerSearchRequest request, List<LedgerRowDto> rows) {

        if (Boolean.TRUE.equals(request.getShowAll())) {
            return rows;
        }

        List<LedgerRowDto> filtered = new ArrayList<>();

        for (LedgerRowDto row : rows) {

            String txType = row.getTxType();

            if ("판매(외상)".equals(txType) && Boolean.TRUE.equals(request.getShowCreditSales())) {
                filtered.add(row);
                continue;
            }

            if ("판매(즉시결제)".equals(txType) && Boolean.TRUE.equals(request.getShowImmediateSales())) {
                filtered.add(row);
                continue;
            }

            if ("수금".equals(txType) && Boolean.TRUE.equals(request.getShowPayments())) {
                filtered.add(row);
                continue;
            }

            if ("반품".equals(txType) && Boolean.TRUE.equals(request.getShowReturns())) {
                filtered.add(row);
                continue;
            }

            if ("오더합계".equals(txType)) {
                filtered.add(row);
            }
        }

        return filtered;
    }

    // 누적잔액 계산
    private void calculateRunningBalance(List<LedgerRowDto> rows, int openingBalance) {

        int balance = openingBalance;

        for (LedgerRowDto row : rows) {
            balance += row.getArDelta() == null ? 0 : row.getArDelta();
            row.setBalance(balance);
        }
    }
}