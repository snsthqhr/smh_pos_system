package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import samosa_fos.de.domain.SalesOrder;
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




        return ;
    }

    private void validateLedgerSearchRequest(LedgerSearchRequest request) {
        if(request.getCustomerId()==null){
            throw new IllegalArgumentException("customerId는 필수입니다.");

        }

        if(request.getStartDate() != null && request.getEndDate()!= null){
            if(request.getStartDate().isAfter(request.getEndDate())){
                throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.")
            }
        }

    }

    private List<SalesOrder> getSalesOrdersByCondition (LedgerSearchRequest request){




    }

}




