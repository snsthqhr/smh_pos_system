package samosa_fos.de.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import samosa_fos.de.domain.SalesOrder;

@SpringBootTest
public class SalesOrderTest {

    @Autowired
    SalesOrderRepository salesOrderRepository;

}
