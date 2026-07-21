(function () {
    const state = {
        selectedCustomer: null,
        selectedSale: null,
        customerSearchTimer: null
    };

    const customerInput = document.querySelector("[data-ar-customer-search]");
    const customerResults = document.querySelector("[data-ar-customer-results]");
    const customerClear = document.querySelector("[data-ar-customer-clear]");
    const startDateInput = document.querySelector("[data-ar-start-date]");
    const endDateInput = document.querySelector("[data-ar-end-date]");
    const searchArButton = document.querySelector("[data-search-ar]");
    const currentBalance = document.querySelector("[data-ar-current-balance]");
    const arMessage = document.querySelector("[data-ar-message]");
    const summaryIncrease = document.querySelector("[data-ar-summary-increase]");
    const summaryPayment = document.querySelector("[data-ar-summary-payment]");
    const summaryReturn = document.querySelector("[data-ar-summary-return]");
    const summaryBalance = document.querySelector("[data-ar-summary-balance]");
    const paymentCustomerLabel = document.querySelector("[data-payment-customer-label]");
    const paymentSalesOrder = document.querySelector("[data-payment-sales-order]");
    const paymentAmount = document.querySelector("[data-payment-amount]");
    const paymentMethod = document.querySelector("[data-payment-method]");
    const paymentMemo = document.querySelector("[data-payment-memo]");
    const clearSalesOrderButton = document.querySelector("[data-clear-sales-order]");
    const paymentButton = document.querySelector("[data-register-payment]");
    const paymentMessage = document.querySelector("[data-payment-message]");
    const salesRows = document.querySelector("[data-ar-sales-rows]");
    const ledgerRows = document.querySelector("[data-ar-ledger-rows]");

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

    async function fetchArBalance(customerId) {
        const response = await fetch(`/api/customers/${customerId}/ar-balance`);
        if (!response.ok) {
            throw new Error("미수금 조회에 실패했습니다.");
        }

        const data = await response.json();
        return Number(data.currentArBalance || 0);
    }

    async function fetchCreditSales(customerId) {
        const params = new URLSearchParams();
        params.set("paymentType", "CREDIT");
        if (customerId) {
            params.set("customerId", customerId);
        }

        const response = await fetch(`/api/sales-management/sales?${params.toString()}`);
        if (!response.ok) {
            throw new Error("외상 전표 조회에 실패했습니다.");
        }
        return response.json();
    }

    async function fetchLedgerRows(customerId) {
        const params = new URLSearchParams();
        if (customerId) {
            params.set("customerId", customerId);
        }
        if (startDateInput.value) {
            params.set("startDate", startDateInput.value);
        }
        if (endDateInput.value) {
            params.set("endDate", endDateInput.value);
        }

        const response = await fetch(`/api/ar-ledger?${params.toString()}`);
        if (!response.ok) {
            throw new Error("미수 흐름 조회에 실패했습니다.");
        }
        return response.json();
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

    function handleCustomerInput() {
        const keyword = customerInput.value.trim();
        state.selectedCustomer = null;
        state.selectedSale = null;
        customerClear.hidden = true;
        clearTimeout(state.customerSearchTimer);

        if (!keyword) {
            customerResults.hidden = true;
            customerResults.innerHTML = "";
            showAllCustomersView();
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

    async function selectCustomer(customer) {
        state.selectedCustomer = customer;
        state.selectedSale = null;
        customerInput.value = customer.name;
        customerClear.hidden = false;
        customerResults.hidden = true;
        customerResults.innerHTML = "";
        paymentCustomerLabel.textContent = `${customer.name} 수금`;
        paymentButton.disabled = false;
        clearSelectedSale();
        await refreshCustomerAr();
    }

    function clearCustomer() {
        state.selectedCustomer = null;
        state.selectedSale = null;
        customerInput.value = "";
        customerClear.hidden = true;
        customerResults.hidden = true;
        customerResults.innerHTML = "";
        showAllCustomersView();
    }

    function resetArView() {
        currentBalance.textContent = "0";
        paymentCustomerLabel.textContent = "고객을 선택하세요.";
        paymentAmount.value = "";
        paymentMemo.value = "";
        paymentButton.disabled = true;
        clearSelectedSale();
        salesRows.innerHTML = `<div class="empty-table-state">고객을 선택하면 외상 전표가 표시됩니다.</div>`;
        renderLedgerRows([]);
    }

    async function showAllCustomersView() {
        state.selectedCustomer = null;
        state.selectedSale = null;
        currentBalance.textContent = "전체";
        paymentCustomerLabel.textContent = "고객을 선택하면 수금할 수 있습니다.";
        paymentAmount.value = "";
        paymentMemo.value = "";
        paymentButton.disabled = true;
        clearSelectedSale();
        arMessage.textContent = "전체 고객의 외상 전표를 조회합니다.";
        paymentMessage.textContent = "";

        try {
            const sales = await fetchCreditSales(null);
            const ledger = await fetchLedgerRows(null);
            renderSalesRows(sales);
            renderLedgerRows(ledger);
        } catch (error) {
            salesRows.innerHTML = `<div class="empty-table-state">${error.message}</div>`;
            renderLedgerRows([]);
        }
    }

    async function refreshCustomerAr() {
        if (!state.selectedCustomer) {
            resetArView();
            return;
        }

        arMessage.textContent = "";
        paymentMessage.textContent = "";

        const balance = await fetchArBalance(state.selectedCustomer.id);
        currentBalance.textContent = money(balance);
        paymentButton.disabled = balance <= 0;

        const sales = await fetchCreditSales(state.selectedCustomer.id);
        const ledger = await fetchLedgerRows(state.selectedCustomer.id);
        renderSalesRows(sales);
        renderLedgerRows(ledger, balance);
    }

    function renderSalesRows(sales) {
        salesRows.innerHTML = "";
        const creditSales = sales.filter((sale) => sale.paymentType === "CREDIT");

        if (!creditSales.length) {
            salesRows.innerHTML = `<div class="empty-table-state">외상 판매전표가 없습니다.</div>`;
            return;
        }

        creditSales.forEach((sale) => {
            const row = document.createElement("button");
            row.type = "button";
            row.className = "ar-sale-row";
            row.dataset.salesOrderId = sale.salesOrderId;
            row.innerHTML = `
                <span>${sale.salesDate || ""}</span>
                <span>${sale.salesOrderId}</span>
                <span>${sale.customerName || ""}</span>
                <span title="${sale.representativeProductName || ""}">${sale.representativeProductName || ""}</span>
                <span class="number">${money(sale.totalQuantity)}</span>
                <span class="number">${money(sale.totalAmount)}</span>
                <span>${sale.editable ? "수금 전" : "수금/반품 있음"}</span>
            `;
            row.addEventListener("click", () => selectSale(sale));
            salesRows.appendChild(row);
        });
    }

    function selectSale(sale) {
        if (!state.selectedCustomer && sale.customerId) {
            state.selectedCustomer = {
                id: sale.customerId,
                name: sale.customerName || ""
            };
            customerInput.value = sale.customerName || "";
            customerClear.hidden = false;
            paymentCustomerLabel.textContent = `${sale.customerName || "선택 고객"} 수금`;
            paymentButton.disabled = false;
        }

        state.selectedSale = sale;
        paymentSalesOrder.value = `전표 ${sale.salesOrderId}`;
        clearSalesOrderButton.disabled = false;

        document.querySelectorAll(".ar-sale-row").forEach((row) => {
            row.classList.toggle("is-selected", Number(row.dataset.salesOrderId) === Number(sale.salesOrderId));
        });
    }

    function renderLedgerRows(rows, currentArBalance) {
        ledgerRows.innerHTML = "";

        const arRows = rows.filter((row) => Number(row.arDelta || 0) !== 0 || row.txType === "수금");
        renderSummary(arRows, currentArBalance);

        if (!arRows.length) {
            ledgerRows.innerHTML = `<div class="empty-table-state">조회된 미수 흐름이 없습니다.</div>`;
            return;
        }

        arRows.forEach((row) => {
            const delta = Number(row.arDelta || 0);
            const increase = delta > 0 ? delta : 0;
            const decrease = delta < 0 ? Math.abs(delta) : 0;
            const line = document.createElement("div");
            line.className = "ar-ledger-row";
            line.innerHTML = `
                <span>${row.txDate || ""}</span>
                <span>${ledgerTypeLabel(row.txType)}</span>
                <span>${row.salesOrderId || ""}</span>
                <span title="${row.customerName || ""}">${row.customerName || ""}</span>
                <span title="${row.productName || ""}">${row.productName || ""}</span>
                <span class="number">${money(increase)}</span>
                <span class="number">${money(decrease)}</span>
                <span class="number">${money(row.balance)}</span>
                <span title="${row.memo || ""}">${row.memo || ""}</span>
            `;
            ledgerRows.appendChild(line);
        });
    }

    function renderSummary(rows, currentArBalance) {
        const increase = rows
                .filter((row) => Number(row.arDelta || 0) > 0)
                .reduce((sum, row) => sum + Number(row.arDelta || 0), 0);
        const payment = rows
                .filter((row) => row.txType === "수금")
                .reduce((sum, row) => sum + Math.abs(Number(row.arDelta || 0)), 0);
        const returns = rows
                .filter((row) => row.txType === "반품")
                .reduce((sum, row) => sum + Math.abs(Number(row.arDelta || 0)), 0);
        const lastBalance = rows.length ? rows[rows.length - 1].balance : currentArBalance;

        summaryIncrease.textContent = money(increase);
        summaryPayment.textContent = money(payment);
        summaryReturn.textContent = money(returns);
        summaryBalance.textContent = lastBalance == null ? "0" : money(lastBalance);
    }

    function ledgerTypeLabel(txType) {
        if (txType === "오더합계") {
            return "외상판매";
        }
        return txType || "";
    }

    function clearSelectedSale() {
        state.selectedSale = null;
        paymentSalesOrder.value = "고객 전체 수금";
        clearSalesOrderButton.disabled = true;
        document.querySelectorAll(".ar-sale-row").forEach((row) => row.classList.remove("is-selected"));
    }

    async function registerPayment() {
        if (!state.selectedCustomer) {
            throw new Error("수금할 고객을 먼저 선택하세요.");
        }

        const amount = Number(paymentAmount.value || 0);
        if (amount <= 0) {
            throw new Error("수금액은 0보다 커야 합니다.");
        }

        const payload = {
            customerId: state.selectedCustomer.id,
            salesOrderId: state.selectedSale ? state.selectedSale.salesOrderId : null,
            amount,
            paymentMethod: paymentMethod.value,
            memo: paymentMemo.value.trim()
        };

        const response = await fetch("/api/payments", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "수금 저장에 실패했습니다.");
        }

        paymentAmount.value = "";
        paymentMemo.value = "";
        paymentMessage.textContent = "수금이 저장되었습니다.";
        clearSelectedSale();
        await refreshCustomerAr();
    }

    customerInput.addEventListener("input", handleCustomerInput);
    customerClear.addEventListener("click", clearCustomer);
    searchArButton.addEventListener("click", async () => {
        try {
            if (state.selectedCustomer) {
                await refreshCustomerAr();
            } else {
                await showAllCustomersView();
            }
        } catch (error) {
            arMessage.textContent = error.message;
        }
    });
    [startDateInput, endDateInput].forEach((input) => {
        input.addEventListener("keydown", async (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                searchArButton.click();
            }
        });
    });
    clearSalesOrderButton.addEventListener("click", clearSelectedSale);
    paymentButton.addEventListener("click", async () => {
        paymentMessage.textContent = "";
        try {
            await registerPayment();
        } catch (error) {
            paymentMessage.textContent = error.message;
        }
    });

    document.addEventListener("click", (event) => {
        if (!customerResults.contains(event.target) && event.target !== customerInput) {
            customerResults.hidden = true;
        }
    });

    startDateInput.value = firstDayOfMonth();
    endDateInput.value = todayString();
    showAllCustomersView();
})();
