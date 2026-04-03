package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.LedgerRowDto;
import samosa_fos.de.dto.sales.LedgerSearchRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class LedgerService {


    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    SalesOrderRepository salesOrderRepository;
    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;
    @Autowired
    ArTxRepository arTxRepository;


    public LedgerService(PaymentRepository paymentRepository,
                         SalesOrderRepository salesOrderRepository,
                         SalesOrderItemRepository salesOrderItemRepository,
                         ArTxRepository arTxRepository) {
        this.paymentRepository = paymentRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.arTxRepository = arTxRepository;


    }


    public  List<LedgerRowDto> LedgerSearch (LedgerSearchRequest request) {

        // request의 고객 아이디 있는지, 시작날짜가 끝나는 날짜보다 작은지 검증
        validateLedgerSearchRequest(request);

        // 판매 전표 조회(고객아이디로만 조회 하는지, 고객 아이디 + 기간으로 조회 하는지)
        List<SalesOrder> salesOrders = getSalesOrdersByCondition(request);

        // 판매 전표 원장 행으로 전환
        List<LedgerRowDto> ledgerRowDtos = new ArrayList<>();
        ledgerRowDtos.addAll(convertSalesOrdersToLedgerRows(salesOrders));




        return ;
    }

    private void validateLedgerSearchRequest(LedgerSearchRequest request) {
        if(request.getCustomerId()==null){
            throw new IllegalArgumentException("customerId는 필수입니다.");

        }

        if(request.getStartDate() != null && request.getEndDate()!= null){
            if(request.getStartDate().isAfter(request.getEndDate())){
                throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
            }
        }

    }

    private List<SalesOrder> getSalesOrdersByCondition (LedgerSearchRequest request){

        //판매 데이터 중 고객아이디가 있고, 시작,끝나는 날짜가 전달 된 경우
        if(request.getStartDate()!=null&&request.getEndDate() !=null){
            return(salesOrderRepository.findByCustomerIdAndSalesDateBetweenAndActiveTrue(
                    request.getCustomerId(),
                    request.getStartDate(),
                    request.getEndDate())
            );


        }
        // 시작,끝 데이터가 없고, 커스터머 아이디만 주어진 경우
        return salesOrderRepository.findByCustomerIdAndActiveTrue(request.getCustomerId());

    }

    private List<LedgerRowDto> convertSalesOrdersToLedgerRows(List<SalesOrder> salesOrders) {



        List<LedgerRowDto> ledgerRowDtos = new ArrayList<>();

        for(SalesOrder salesOrder: salesOrders) {

            List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderId(salesOrder.getId());


            //이 주문에 대한 물건의 총 갯수
            int totalQuantity = 0;

            for(SalesOrderItem item : items) {
                LedgerRowDto row = new LedgerRowDto();
                row.setSalesOrderId(salesOrder.getId());
                row.setCustomerId(salesOrder.getCustomerId());
                row.setTxDate(salesOrder.getSalesDate());

                if("CREDIT".equals(salesOrder.getPaymentType())) {
                    row.setTxType("판매(외상)");
                    row.setArDelta(0); //아이템 줄에는 0
                }
                else{
                    row.setTxType("판매(즉시결제)");
                    row.setArDelta(0); //아이템 줄에는 0
                }

                //품목 정보
                // 현재 ProductRepository를 안쓰니 추후 보강 필요
                //지금은 productNAme, unit은 추후 보강 필요하다.
                row.setProductName("상품명 조회 연결 예정");
                row.setUnit("단위 조회 연결 예정");

                row.setUnitPrice(item.getUnitPrice());
                row.setQuantity(item.getQuantity());
                row.setSupplyPrice(item.getSupplyPrice());
                row.setTaxPrice(item.getTaxPrice());
                row.setSaleAmount(item.getTotalPrice());


                //외상인 경우에는 paymentAmount를 0으로 설정하고,
                //즉시 입금 한 경우에는 페이먼트의 값을 반영해준다.
                if ("CREDIT".equals(salesOrder.getPaymentType())) {
                    row.setPaymentAmount(0);
                } else {
                    row.setPaymentAmount(item.getTotalPrice());
                }

                //각각의 아이템에 오약이 필요 한게아닌, 주문 하나에 대해 오더가 필요함
                row.setSummaryRow(false);
                row.setMemo(row.getMemo());

                ledgerRowDtos.add(row);

                totalQuantity += item.getQuantity() == null ? 0 : item.getQuantity();

            }

            LedgerRowDto summaryRow = createSummaryRow(salesOrder);
            ledgerRowDtos.add(summaryRow);
        }


        return ledgerRowDtos;
    }


    private LedgerRowDto createSummaryRow(SalesOrder salesOrder) {

        LedgerRowDto summaryRow = new LedgerRowDto();

        


    }

}




