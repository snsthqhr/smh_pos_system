(function () {
    const state = {
        selectedCustomer: null,
        selectedSale: null,
        selectedJobSite: null,
        customerSearchTimer: null,
        cancelPreview: null
    };

    const startDateInput = document.querySelector("[data-start-date]");
    const endDateInput = document.querySelector("[data-end-date]");
    const productKeywordInput = document.querySelector("[data-product-keyword]");
    const customerInput = document.querySelector("[data-management-customer-search]");
    const customerResults = document.querySelector("[data-management-customer-results]");
    const customerClear = document.querySelector("[data-management-customer-clear]");
    const paymentTypeSelect = document.querySelector("[data-payment-type]");
    const taxPolicySelect = document.querySelector("[data-tax-policy]");
    const searchButton = document.querySelector("[data-search-sales]");
    const message = document.querySelector("[data-management-message]");
    const historyRows = document.querySelector("[data-sales-history-rows]");
    const itemRows = document.querySelector("[data-sales-item-rows]");
    const saveButton = document.querySelector("[data-save-selected]");
    const cancelButton = document.querySelector("[data-cancel-selected]");
    const statementButton = document.querySelector("[data-download-statement]");
    const cancelSaleModal = document.querySelector("[data-cancel-sale-modal]");
    const cancelSaleCloseButtons = document.querySelectorAll("[data-cancel-sale-close]");
    const confirmSaleCancelButton = document.querySelector("[data-confirm-sale-cancel]");
    const cancelSaleMemo = document.querySelector("[data-cancel-sale-memo]");
    const cancelSaleMessage = document.querySelector("[data-cancel-sale-message]");
    const cancelSaleDescription = document.querySelector("[data-cancel-sale-description]");
    const cancelPreviewFields = {
        saleAmount: document.querySelector("[data-cancel-sale-amount]"),
        directPaymentAmount: document.querySelector("[data-cancel-payment-amount]"),
        returnAmount: document.querySelector("[data-cancel-return-amount]"),
        previousRefundAmount: document.querySelector("[data-cancel-previous-refund]"),
        remainingSaleAmount: document.querySelector("[data-cancel-remaining-sale]"),
        currentArBalance: document.querySelector("[data-cancel-current-balance]"),
        balanceAfterCancellation: document.querySelector("[data-cancel-expected-balance]"),
        refundAmount: document.querySelector("[data-cancel-refund-amount]"),
        balanceAfterProcessing: document.querySelector("[data-cancel-final-balance]")
    };

    const detail = {
        salesDate: document.querySelector("[data-detail-sales-date]"),
        customerName: document.querySelector("[data-detail-customer-name]"),
        jobSiteName: document.querySelector("[data-detail-jobsite-name]"),
        totalAmount: document.querySelector("[data-detail-total-amount]"),
        payableAmount: document.querySelector("[data-detail-payable-amount]"),
        netAmount: document.querySelector("[data-detail-net-amount]"),
        taxAmount: document.querySelector("[data-detail-tax-amount]"),
        cash: document.querySelector("[data-detail-cash]"),
        transfer: document.querySelector("[data-detail-transfer]"),
        card: document.querySelector("[data-detail-card]"),
        credit: document.querySelector("[data-detail-credit]"),
        memo: document.querySelector("[data-detail-memo]")
    };
    const jobSiteResults = document.querySelector("[data-detail-jobsite-results]");

    function money(value) {
        return Number(value || 0).toLocaleString("ko-KR");
    }

    function todayString() {
        return new Date().toISOString().slice(0, 10);
    }

    function firstDayOfMonth() {
        const now = new Date();
        return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
    }

    function normalizeName(value) {
        return String(value || "").trim().replace(/\s+/g, "");
    }

    function paymentLabel(value) {
        return {
            CREDIT: "외상",
            CARD: "카드",
            CASH: "현금",
            TRANSFER: "통장입금"
        }[value] || value || "";
    }

    function taxLabel(value) {
        return {
            ADD_VAT: "부가세 별도",
            NO_TAX: "부가세 없음"
        }[value] || value || "";
    }

    async function searchCustomers(keyword) {
        const params = new URLSearchParams();
        if (keyword) {
            params.set("keyword", keyword);
        }

        const response = await fetch(`/api/customers?${params.toString()}`);
        if (!response.ok) {
            throw new Error("고객 검색에 실패했습니다.");
        }
        return response.json();
    }

    async function searchJobSites(customerId, keyword) {
        const params = new URLSearchParams();
        params.set("customerId", customerId);
        if (keyword) {
            params.set("keyword", keyword);
        }

        const response = await fetch(`/api/job-sites?${params.toString()}`);
        if (!response.ok) {
            throw new Error("현장 검색에 실패했습니다.");
        }
        return response.json();
    }

    function selectCustomer(customer) {
        state.selectedCustomer = customer;
        customerInput.value = customer.name;
        customerClear.hidden = false;
        customerResults.hidden = true;
        customerResults.innerHTML = "";
    }

    function clearCustomer() {
        state.selectedCustomer = null;
        customerInput.value = "";
        customerClear.hidden = true;
        customerResults.hidden = true;
        customerResults.innerHTML = "";
    }

    function selectedCustomerIdForSearch() {
        if (!customerInput.value.trim()) {
            state.selectedCustomer = null;
            customerClear.hidden = true;
            return null;
        }
        return state.selectedCustomer ? state.selectedCustomer.id : null;
    }

    function uniqueCustomersByName(customers) {
        const seen = new Set();
        return customers.filter((customer) => {
            const key = normalizeName(customer.name);
            if (seen.has(key)) {
                return false;
            }
            seen.add(key);
            return true;
        });
    }

    function renderCustomerResults(customers) {
        const uniqueCustomers = uniqueCustomersByName(customers);
        customerResults.innerHTML = "";

        if (!uniqueCustomers.length) {
            customerResults.innerHTML = `<button type="button" class="customer-result-row"><span>검색된 고객 없음</span></button>`;
            customerResults.hidden = false;
            return;
        }

        uniqueCustomers.forEach((customer) => {
            const row = document.createElement("button");
            row.type = "button";
            row.className = "customer-result-row";
            row.innerHTML = `<span>${customer.name}</span><small>${customer.phone || "기존 고객"}</small>`;
            row.addEventListener("click", () => selectCustomer(customer));
            customerResults.appendChild(row);
        });
        customerResults.hidden = false;
    }

    function handleCustomerSearchInput() {
        const keyword = customerInput.value.trim();
        state.selectedCustomer = null;
        customerClear.hidden = true;
        clearTimeout(state.customerSearchTimer);

        if (!keyword) {
            customerResults.hidden = true;
            customerResults.innerHTML = "";
            return;
        }

        state.customerSearchTimer = setTimeout(async () => {
            try {
                renderCustomerResults(await searchCustomers(keyword));
            } catch (error) {
                customerResults.innerHTML = `<button type="button" class="customer-result-row"><span>${error.message}</span></button>`;
                customerResults.hidden = false;
            }
        }, 180);
    }

    async function searchSales() {
        message.textContent = "";
        historyRows.innerHTML = `<div class="empty-table-state">판매내역을 불러오는 중입니다.</div>`;

        const params = new URLSearchParams();
        const customerId = selectedCustomerIdForSearch();
        if (customerId) {
            params.set("customerId", customerId);
        }
        if (startDateInput.value) {
            params.set("startDate", startDateInput.value);
        }
        if (endDateInput.value) {
            params.set("endDate", endDateInput.value);
        }
        if (productKeywordInput.value.trim()) {
            params.set("productKeyword", productKeywordInput.value.trim());
        }
        if (paymentTypeSelect.value) {
            params.set("paymentType", paymentTypeSelect.value);
        }
        if (taxPolicySelect.value) {
            params.set("taxPolicy", taxPolicySelect.value);
        }

        const response = await fetch(`/api/sales-management/sales?${params.toString()}`);
        if (!response.ok) {
            throw new Error("판매내역 조회에 실패했습니다.");
        }

        const sales = await response.json();
        renderSalesRows(sales);
        if (sales.length) {
            selectSale(sales[0]);
        } else {
            clearDetail();
        }
    }

    function renderSalesRows(sales) {
        historyRows.innerHTML = "";
        if (!sales.length) {
            historyRows.innerHTML = `<div class="empty-table-state">조회된 판매내역이 없습니다.</div>`;
            return;
        }

        sales.forEach((sale) => {
            const row = document.createElement("button");
            row.type = "button";
            row.className = "history-row";
            row.dataset.salesOrderId = sale.salesOrderId;
            row.innerHTML = `
                <span>본점</span>
                <span>${sale.salesDate || ""}</span>
                <span>POS</span>
                <span>${sale.salesOrderId}</span>
                <span>${paymentLabel(sale.paymentType)}</span>
                <span>${sale.customerId || ""}</span>
                <span title="${sale.customerName || ""}">${sale.customerName || ""}</span>
                <span title="${sale.representativeProductName || ""}">${sale.representativeProductName || ""}</span>
                <span class="number">${money(sale.totalQuantity)}</span>
                <span class="number">${money(sale.totalAmount)}</span>
                <span>${sale.editable ? "수정 가능" : "잠김"}</span>
                <span>${taxLabel(sale.taxPolicy)}</span>
            `;
            row.addEventListener("click", () => selectSale(sale));
            historyRows.appendChild(row);
        });
    }

    function selectSale(sale) {
        state.selectedSale = structuredClone(sale);
        // 판매관리 화면에서 현장은 텍스트 임의 입력이 아니라 기존 현장 선택값으로 관리한다.
        // 저장 시 jobSiteId를 넘겨야 하므로 화면 표시명과 선택 id를 같이 들고 있는다.
        state.selectedJobSite = sale.jobSiteId ? { id: sale.jobSiteId, name: sale.jobSiteName || "" } : null;
        document.querySelectorAll(".history-row").forEach((row) => {
            row.classList.toggle("is-selected", Number(row.dataset.salesOrderId) === Number(sale.salesOrderId));
        });

        detail.salesDate.value = sale.salesDate || "";
        detail.customerName.value = sale.customerName || "";
        detail.jobSiteName.value = sale.jobSiteName || "";
        detail.totalAmount.value = money(sale.totalAmount);
        detail.payableAmount.value = money(sale.totalAmount);
        detail.netAmount.value = money(sale.totalNetAmount);
        detail.taxAmount.value = money(sale.totalTaxAmount);
        detail.cash.value = sale.paymentType === "CASH" ? money(sale.totalAmount) : "0";
        detail.transfer.value = sale.paymentType === "TRANSFER" ? money(sale.totalAmount) : "0";
        detail.card.value = sale.paymentType === "CARD" ? money(sale.totalAmount) : "0";
        detail.credit.value = sale.paymentType === "CREDIT" ? money(sale.totalAmount) : "0";
        detail.memo.value = sale.memo || "";

        // 2026-07-22 판매수정 UI 정책:
        // 반품 없는 전표만 편집 가능하게 열고, 실제 수금액 초과 검증은 저장 API에서 다시 검사한다.
        detail.salesDate.readOnly = !sale.editable;
        detail.jobSiteName.readOnly = !sale.editable;
        detail.memo.readOnly = !sale.editable;

        saveButton.disabled = !sale.editable;
        cancelButton.disabled = false;
        statementButton.disabled = false;
        renderItemRows();
        refreshCalculatedTotals();
        message.textContent = sale.editable ? "수정 가능한 전표입니다." : "반품이 있는 전표라 수정할 수 없습니다.";
    }

    function clearDetail() {
        state.selectedSale = null;
        Object.values(detail).forEach((input) => {
            input.value = "";
        });
        state.selectedJobSite = null;
        jobSiteResults.hidden = true;
        jobSiteResults.innerHTML = "";
        itemRows.innerHTML = `<div class="empty-table-state">판매 품목이 선택되면 이곳에서 수정합니다.</div>`;
        saveButton.disabled = true;
        cancelButton.disabled = true;
        statementButton.disabled = true;
    }

    function renderItemRows() {
        const sale = state.selectedSale;
        itemRows.innerHTML = "";
        if (!sale || !sale.items.length) {
            itemRows.innerHTML = `<div class="empty-table-state">판매 품목이 없습니다.</div>`;
            return;
        }

        sale.items.forEach((item) => {
            const row = document.createElement("div");
            row.className = "line-item-row";
            row.dataset.salesOrderItemId = item.salesOrderItemId;
            row.innerHTML = `
                <span>${item.productCode || ""}</span>
                <span title="${item.productName || ""}">${item.productName || ""}</span>
                <span>${item.unit || ""}</span>
                <span class="number">0</span>
                <input type="number" min="1" value="${item.quantity || 1}" data-item-quantity ${sale.editable ? "" : "readonly"}>
                <input type="number" min="0" value="${item.unitPrice || 0}" data-item-unit-price ${sale.editable ? "" : "readonly"}>
                <span class="number" data-item-total>${money(item.totalPrice)}</span>
                <span class="number">0</span>
                <span class="number">0</span>
                <span></span>
                <span class="number">0</span>
            `;
            row.querySelector("[data-item-quantity]").addEventListener("input", refreshCalculatedTotals);
            row.querySelector("[data-item-unit-price]").addEventListener("input", refreshCalculatedTotals);
            itemRows.appendChild(row);
        });
    }

    function numberFromInput(input) {
        return Number(String(input.value || "0").replaceAll(",", "")) || 0;
    }

    function refreshCalculatedTotals() {
        const sale = state.selectedSale;
        if (!sale) {
            return;
        }

        let netAmount = 0;
        let taxAmount = 0;
        let totalAmount = 0;

        // 합계금액/공급가/부가세/결제금액은 직접 입력하지 않고
        // 품목 수량과 단가를 기준으로 화면에서 즉시 다시 계산한다.
        itemRows.querySelectorAll(".line-item-row").forEach((row) => {
            const quantity = numberFromInput(row.querySelector("[data-item-quantity]"));
            const unitPrice = numberFromInput(row.querySelector("[data-item-unit-price]"));
            const supplyPrice = quantity * unitPrice;
            const itemTax = sale.taxPolicy === "ADD_VAT" ? Math.floor(supplyPrice * 0.1) : 0;
            const itemTotal = supplyPrice + itemTax;

            row.querySelector("[data-item-total]").textContent = money(itemTotal);
            netAmount += supplyPrice;
            taxAmount += itemTax;
            totalAmount += itemTotal;
        });

        detail.totalAmount.value = money(totalAmount);
        detail.payableAmount.value = money(totalAmount);
        detail.netAmount.value = money(netAmount);
        detail.taxAmount.value = money(taxAmount);
        detail.cash.value = sale.paymentType === "CASH" ? money(totalAmount) : "0";
        detail.transfer.value = sale.paymentType === "TRANSFER" ? money(totalAmount) : "0";
        detail.card.value = sale.paymentType === "CARD" ? money(totalAmount) : "0";
        detail.credit.value = sale.paymentType === "CREDIT" ? money(totalAmount) : "0";
    }

    function ensureSelectedJobSite() {
        const name = detail.jobSiteName.value.trim();
        if (!name) {
            state.selectedJobSite = null;
            return null;
        }

        // 현장명을 임의 텍스트로 저장하면 jobSiteId가 없어지므로,
        // 기존 현장 검색 결과에서 고른 값만 저장하도록 막는다.
        if (state.selectedJobSite && normalizeName(state.selectedJobSite.name) === normalizeName(name)) {
            return state.selectedJobSite.id;
        }
        throw new Error("현장은 검색 결과에서 선택하거나 비워주세요.");
    }

    function collectUpdatePayload() {
        const sale = state.selectedSale;
        if (!detail.salesDate.value) {
            throw new Error("판매일자는 필수입니다.");
        }

        // 서버에 저장하는 값은 판매일자/현장/메모/품목 수량/단가까지만 보낸다.
        // 총액, 공급가, 부가세, 결제금액은 서버 서비스에서 다시 계산한다.
        return {
            salesDate: detail.salesDate.value || null,
            jobSiteId: ensureSelectedJobSite(),
            memo: detail.memo.value.trim(),
            items: Array.from(itemRows.querySelectorAll(".line-item-row")).map((row) => {
                const salesOrderItemId = Number(row.dataset.salesOrderItemId);
                const item = sale.items.find((value) => Number(value.salesOrderItemId) === salesOrderItemId);
                return {
                    salesOrderItemId,
                    productId: item.productId,
                    quantity: Number(row.querySelector("[data-item-quantity]").value || 0),
                    unitPrice: Number(row.querySelector("[data-item-unit-price]").value || 0)
                };
            })
        };
    }

    async function saveSelectedSale() {
        if (!state.selectedSale || !state.selectedSale.editable) {
            return;
        }

        const response = await fetch(`/api/sales-orders/${state.selectedSale.salesOrderId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(collectUpdatePayload())
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "판매 수정에 실패했습니다.");
        }

        message.textContent = "판매 내용이 저장되었습니다.";
        await searchSales();
    }

    function closeCancelSaleModal() {
        state.cancelPreview = null;
        cancelSaleModal.hidden = true;
        cancelSaleMessage.textContent = "";
        confirmSaleCancelButton.disabled = true;
    }

    function renderCancelPreview(preview) {
        Object.entries(cancelPreviewFields).forEach(([key, element]) => {
            element.textContent = money(preview[key]);
        });

        const paymentText = paymentLabel(preview.paymentType);
        cancelSaleDescription.textContent = preview.paymentType === "CREDIT"
                ? `전표 ${preview.salesOrderId}번을 취소합니다. 고객 전체 미수금을 기준으로 추가 환불액을 계산했습니다.`
                : `전표 ${preview.salesOrderId}번을 취소하고 ${paymentText} 결제금액을 고객에게 돌려줍니다.`;
        confirmSaleCancelButton.textContent = Number(preview.refundAmount || 0) > 0
                ? "취소 및 환불 처리"
                : "전표 취소";
        confirmSaleCancelButton.disabled = false;
        cancelSaleMessage.textContent = preview.message || "금액을 확인한 뒤 처리해 주세요.";
    }

    async function openCancelSaleModal() {
        if (!state.selectedSale) {
            return;
        }

        state.cancelPreview = null;
        cancelSaleMemo.value = "";
        cancelSaleMessage.textContent = "취소 예상 금액을 계산하는 중입니다.";
        confirmSaleCancelButton.disabled = true;
        cancelSaleModal.hidden = false;

        const response = await fetch(`/api/sales-orders/${state.selectedSale.salesOrderId}/cancel-preview`);
        if (!response.ok) {
            const text = await response.text();
            closeCancelSaleModal();
            throw new Error(text || "전표 취소 예상 금액을 조회하지 못했습니다.");
        }

        state.cancelPreview = await response.json();
        renderCancelPreview(state.cancelPreview);
    }

    async function confirmCancelSelectedSale() {
        if (!state.selectedSale || !state.cancelPreview) {
            return;
        }

        const refundAmount = Number(state.cancelPreview.refundAmount || 0);
        if (refundAmount > 0) {
            const refundMethod = state.cancelPreview.paymentType === "CREDIT"
                    ? ""
                    : ` (${paymentLabel(state.cancelPreview.paymentType)} 결제 취소)`;
            const ok = window.confirm(`${money(refundAmount)}원을 고객에게 돌려준 것을 확인한 뒤 전표를 취소합니다.${refundMethod}\n계속하시겠습니까?`);
            if (!ok) {
                return;
            }
        }

        confirmSaleCancelButton.disabled = true;
        cancelSaleMessage.textContent = "전표 취소를 저장하는 중입니다.";

        const response = await fetch(`/api/sales-orders/${state.selectedSale.salesOrderId}/cancel`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                expectedRefundAmount: refundAmount,
                memo: cancelSaleMemo.value.trim() || "판매전표 취소"
            })
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "전표 취소에 실패했습니다.");
        }

        const result = await response.json();
        closeCancelSaleModal();
        message.textContent = result.message || "전표가 취소되었습니다.";
        alert(result.message || "전표가 취소되었습니다.");
        clearDetail();
        await searchSales();
    }

    function openStatement() {
        if (!state.selectedSale) {
            return;
        }
        window.location.href = `/statements/${state.selectedSale.salesOrderId}/docx`;
    }

    function bindEvents() {
        searchButton.addEventListener("click", async () => {
            try {
                await searchSales();
            } catch (error) {
                message.textContent = error.message;
            }
        });

        [startDateInput, endDateInput, productKeywordInput, paymentTypeSelect, taxPolicySelect].forEach((input) => {
            input.addEventListener("keydown", async (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    try {
                        await searchSales();
                    } catch (error) {
                        message.textContent = error.message;
                    }
                }
            });
        });

        customerInput.addEventListener("input", handleCustomerSearchInput);
        customerClear.addEventListener("click", clearCustomer);
        document.addEventListener("click", (event) => {
            if (!customerResults.contains(event.target) && event.target !== customerInput) {
                customerResults.hidden = true;
            }
        });

        document.querySelector("[data-open-pos]").addEventListener("click", () => {
            window.location.href = "/pos";
        });
        statementButton.addEventListener("click", openStatement);
        cancelButton.addEventListener("click", async () => {
            try {
                await openCancelSaleModal();
            } catch (error) {
                message.textContent = error.message;
                alert(error.message);
            }
        });
        confirmSaleCancelButton.addEventListener("click", async () => {
            try {
                await confirmCancelSelectedSale();
            } catch (error) {
                cancelSaleMessage.textContent = error.message;
                confirmSaleCancelButton.disabled = false;
                alert(error.message);
            }
        });
        cancelSaleCloseButtons.forEach((button) => button.addEventListener("click", closeCancelSaleModal));
        cancelSaleModal.addEventListener("click", (event) => {
            if (event.target === cancelSaleModal) {
                closeCancelSaleModal();
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !cancelSaleModal.hidden) {
                closeCancelSaleModal();
            }
        });
        saveButton.addEventListener("click", async () => {
            try {
                await saveSelectedSale();
            } catch (error) {
                message.textContent = error.message;
                alert(error.message);
            }
        });

        detail.jobSiteName.addEventListener("input", async () => {
            if (!state.selectedSale || !state.selectedSale.editable) {
                return;
            }

            // 현장은 현재 전표 고객에게 등록된 현장만 검색해서 선택할 수 있다.
            // 고객 변경은 Payment/ArTx의 고객 기준과 충돌할 수 있어 이번 범위에서 열지 않았다.
            state.selectedJobSite = null;
            const keyword = detail.jobSiteName.value.trim();
            if (!keyword) {
                jobSiteResults.hidden = true;
                jobSiteResults.innerHTML = "";
                return;
            }
            try {
                const jobSites = await searchJobSites(state.selectedSale.customerId, keyword);
                jobSiteResults.innerHTML = "";
                if (!jobSites.length) {
                    jobSiteResults.innerHTML = `<button type="button" class="customer-result-row"><span>검색된 현장 없음</span></button>`;
                }
                jobSites.forEach((jobSite) => {
                    const row = document.createElement("button");
                    row.type = "button";
                    row.className = "customer-result-row";
                    row.innerHTML = `<span>${jobSite.name}</span><small>기존 현장</small>`;
                    row.addEventListener("click", () => {
                        state.selectedJobSite = jobSite;
                        detail.jobSiteName.value = jobSite.name;
                        jobSiteResults.hidden = true;
                        jobSiteResults.innerHTML = "";
                    });
                    jobSiteResults.appendChild(row);
                });
                jobSiteResults.hidden = false;
            } catch (error) {
                message.textContent = error.message;
            }
        });

        document.addEventListener("click", (event) => {
            if (!jobSiteResults.contains(event.target) && event.target !== detail.jobSiteName) {
                jobSiteResults.hidden = true;
            }
        });
    }

    startDateInput.value = firstDayOfMonth();
    endDateInput.value = todayString();
    bindEvents();
    searchSales().catch((error) => {
        message.textContent = error.message;
    });
})();
