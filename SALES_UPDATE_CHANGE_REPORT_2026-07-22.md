# 판매내역 수정 정책 변경 보고서

작성일: 2026-07-22

## 1. 변경 목표

판매내역 관리 화면에서 수금이 있는 전표도 조건부로 수정할 수 있게 변경했다.

기준 정책은 다음과 같다.

- 반품이 있는 전표는 아직 수정 금지
- 외상 전표는 수금이 있어도 수정 허용
- 단, 수정 후 판매금액이 해당 전표에 이미 연결된 수금액보다 작으면 저장 차단
- 미수금/원장은 계속 ArTx 기준 유지
- 즉시결제 전표는 ArTx가 없으므로 연결된 Payment 금액을 수정 후 판매금액으로 맞춤

## 2. 서비스 로직 변경

파일:

- `src/main/java/samosa_fos/de/service/SalesOrderUpdateService.java`

변경 내용:

- 기존 `수금 존재 시 수정 불가` 조건을 제거했다.
- `returnQuantity > 0`인 품목이 있는 전표는 계속 수정 불가로 유지했다.
- 외상 전표(`paymentType = CREDIT`)는 수정 후 총 판매금액과 연결 수금액을 비교한다.
- 수정 후 총 판매금액이 연결 수금액보다 작으면 예외를 발생시킨다.
- 외상 전표 수정 시 `ArTx(SALE)`의 금액과 거래일자를 같이 갱신한다.
- 즉시결제 전표(`CARD`, `CASH`, `TRANSFER`)는 `ArTx(SALE)`이 없으므로 연결된 활성 `Payment` 1건의 금액, 결제일자, 결제방식을 갱신한다.
- 즉시결제 전표에 활성 `Payment`가 여러 건 연결되어 있으면 자동 수정하지 않고 예외를 발생시킨다.

## 3. API/DTO 변경

파일:

- `src/main/java/samosa_fos/de/dto/sales/UpdateSalesOrderRequest.java`
- `src/main/java/samosa_fos/de/repository/PaymentRepository.java`
- `src/main/java/samosa_fos/de/controller/SalesOrderController.java`
- `src/main/java/samosa_fos/de/controller/SalesManagementController.java`

변경 내용:

- 판매 수정 요청에 `salesDate`, `jobSiteId`를 추가했다.
- 전표별 활성 수금 목록 조회용 `findBySalesOrderIdAndActiveTrue`를 추가했다.
- 판매 수정 중 발생한 검증 예외는 400 응답과 한국어 메시지로 내려가도록 처리했다.
- 판매내역 조회의 `editable` 조건에서 수금 존재 여부를 제외하고, 반품 존재 여부만 반영하도록 변경했다.

## 4. UI 변경

파일:

- `src/main/resources/templates/sales-management.html`
- `src/main/resources/static/js/sales-management.js`
- `src/main/resources/static/css/app-ui.css`

변경 내용:

- 판매내역 상세 박스에서 판매일자, 현장, 메모를 수정 가능하게 했다.
- 품목의 판매수량과 판매단가를 수정하면 공급가, 부가세, 합계금액, 결제금액 표시가 즉시 재계산된다.
- 합계금액, 공급가, 부가세, 카드/현금/통장입금/미수금 칸은 직접 입력하지 않고 계산 결과로만 표시한다.
- 현장은 현재 고객의 기존 현장을 검색해서 선택하거나 비울 수 있게 했다.
- 저장 실패 시 오른쪽 메시지뿐 아니라 `alert` 팝업으로도 사유를 보여준다.

## 5. 의도적으로 막아둔 항목

아래 항목은 이번 단계에서 직접 수정 대상으로 열지 않았다.

- 고객명 변경: 기존 수금, ArTx, Payment의 고객 기준이 꼬일 수 있음
- 결제방식 변경: 즉시결제와 외상은 데이터 흐름이 달라 별도 정책 필요
- 현금/카드/통장입금 금액 직접 수정: 현재 SalesOrder에는 결제금액 분할 필드가 없음
- 매출할인 직접 수정: 현재 별도 할인 필드가 없어서 단가 조정 방식이 더 안전함
- 반품 있는 전표 수정: 현재 반품 구조가 A방식이라 원본 판매 수정 시 반품 금액 정합성이 깨질 수 있음

## 6. 검증 결과

성공:

```bash
./gradlew test --tests samosa_fos.de.service.SalesOrderUpdateServiceTest
```

결과: 성공

확인한 케이스:

- 수금/반품 없는 전표 수정 성공
- 수금이 있어도 수정 후 판매금액이 수금액 이상이면 수정 성공
- 수정 후 판매금액이 수금액보다 작으면 수정 실패
- 즉시결제 전표 수정 시 연결 Payment 금액과 일자 갱신
- 반품 있는 전표 수정 실패

참고:

```bash

