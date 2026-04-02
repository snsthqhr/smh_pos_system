package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.Payment;
import samosa_fos.de.dto.sales.RegisterPaymentRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PaymentService {

    private final ArTxRepository arTxRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService ( ArTxRepository arTxRepository,
                            PaymentRepository paymentRepository) {
        this.arTxRepository = arTxRepository;
        this.paymentRepository = paymentRepository;
    }

    public Payment registerPayment(RegisterPaymentRequest request){

        //데이터 검증
        validatePaymentRequest(request);

        //수금 날짜 보정 작업
        LocalDate paymentDate = request.getPaymentDate() != null
                ? request.getPaymentDate()
                : LocalDate.now();

        //  Payment 저장
        Payment payment = new Payment();
        payment.setCustomerId(request.getCustomerId());
        payment.setSalesOrderId(request.getSalesOrderId());
        payment.setPaymentDate(paymentDate);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setMemo(request.getMemo());
        payment.setActive(true);


        Payment savedPayment = paymentRepository.save(payment);

        //payment를 artx로 변환하는 과정 이때 금액은 -를 붙여서 artx를 바꿔주어야 한다.
        ArTx paymentToArTx = new ArTx();
        paymentToArTx.setAmount(-request.getAmount());
        paymentToArTx.setActive(true);
        paymentToArTx.setSalesOrderId(savedPayment.getSalesOrderId());
        paymentToArTx.setMemo(request.getMemo());
        paymentToArTx.setCustomerId(request.getCustomerId());
        paymentToArTx.setTxDate(paymentDate);
        paymentToArTx.setTxType("PAYMENT");// 여기 너무 헷갈린다.
        arTxRepository.save(paymentToArTx);

        //고객의 전체 미수금 계산
        int curArtx = calculateCurrentArBalance(request.getCustomerId());


        return savedPayment;
    }

    void validatePaymentRequest(RegisterPaymentRequest request){

        //수금등록 request로 부터 온 데이터가 이상이 없는지 확인한다.
        if(request.getCustomerId()==null){
            throw new IllegalArgumentException("customerid는 필수입니다.");
        }
        if(request.getAmount()<=0|| request.getAmount() == null){
            throw new IllegalArgumentException("수금 금액이 잘못 되었습니다.");
        }
        if(request.getPaymentMethod()==null ||request.getPaymentMethod().isBlank()){
            throw  new IllegalArgumentException("결제 방식은 필수입니다.");
        }

    }

    int calculateCurrentArBalance(Long customerId){

        List<ArTx> txList = arTxRepository.findByCustomerId(customerId);

        int balance = txList.stream()
                .mapToInt(ArTx::getAmount).sum();
        //ArTx를 객체를 Artx객체 안에 있는 int값으로 변환 해준다.

        return balance;
    }


}
