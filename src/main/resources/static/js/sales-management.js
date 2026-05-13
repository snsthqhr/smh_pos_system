(function () {
    const state = {
        selectedCustomer: null,
        selectedSale: null,
        customerSearchTimer: null
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
    const statementButton = document.querySelector("[data-download-statement]");

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
        if (state.selectedCustomer) {
            params.set("customerId", state.selectedCustomer.id);
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
        detail.memo.readOnly = !sale.editable;

        saveButton.disabled = !sale.editable;
        statementButton.disabled = false;
        renderItemRows();
        message.textContent = sale.editable ? "수정 가능한 전표입니다." : "수금 또는 반품이 있는 전표라 수정할 수 없습니다.";
    }

    function clearDetail() {
        state.selectedSale = null;
        Object.values(detail).forEach((input) => {
            input.value = "";
        });
        itemRows.innerHTML = `<div class="empty-table-state">판매 품목이 선택되면 이곳에서 수정합니다.</div>`;
        saveButton.disabled = true;
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
            itemRows.appendChild(row);
        });
    }

    function collectUpdatePayload() {
        const sale = state.selectedSale;
        return {
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
        saveButton.addEventListener("click", async () => {
            try {
                await saveSelectedSale();
            } catch (error) {
                message.textContent = error.message;
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
