package samosa_fos.de.repository;


import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
public class ArTxRepositoryTest {

    @Autowired
    ArTxRepository arTxRepository;


}
