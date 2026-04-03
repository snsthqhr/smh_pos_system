package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.time.LocalDate;

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


    public  void LedgerSearch (LocalDate start, LocalDate end){


    }

}
