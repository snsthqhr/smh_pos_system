package samosa_fos.de.service;

import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;

    public SalesOrderService(SalesOrderRepository salesOrderRepository,SalesOrderItemRepository salesOrderItemRepository){
        
    }


}
