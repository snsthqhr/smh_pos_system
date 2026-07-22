(function () {
    const state = {
        selectedCustomer: null,
        selectedSale: null,
        customerSearchTimer: null,
        paymentCustomerSearchTimer: null,
        customerActiveIndex: -1,
        paymentCustomerActiveIndex: -1,
        paymentSales: []
    };

    const customerInput = document.querySelector("[data-ar-customer-search]");
    const customerResults = document.querySelector("[data-ar-customer-results]");
    const customerClear = document.querySelector("[data-ar-customer-clear]");
    const startDateInput = document.querySelector("[data-ar-start-date]");
    const endDateInput = document.querySelector("[data-ar-end-date]");
    const searchArButton = document.querySelector("[data-search-ar]");
    const currentBalance = document.querySelector("[data-ar-current-balance]");
    const arMessage = document.querySelector("[data-ar-message]");
    const openPaymentModalButton = document.querySelector("[data-open-payment-modal]");
    const paymentModal = document.querySelector("[data-payment-modal]");
    const closePaymentModalButtons = document.querySelectorAll("[data-close-payment-modal]");
    const paymentCustomerLabel = document.querySelector("[data-payment-customer-label]");
    const paymentCustomerInput = document.querySelector("[data-payment-customer-search]");
    const paymentCustomerResults = document.querySelector("[data-payment-customer-results]");
    const paymentSalesOrder = document.querySelector("[data-payment-sales-order]");
    const paymentAmount = document.querySelector("[data-payment-amount]");
    const paymentDueLabel = document.querySelector("[data-payment-due-label]");
    const paymentDueAmount = document.querySelector("[data-payment-due-amount]");
    const paymentMethod = document.querySelector("[data-payment-method]");
    const paymentMemo = document.querySelector("[data-payment-memo]");
    const clearSalesOrderButton = document.querySelector("[data-clear-sales-order]");
    const paymentButton = document.querySelector("[data-register-payment]");
    const paymentMessage = document.querySelector("[data-payment-message]");
    const paymentHistoryRows = document.querySelector("[data-ar-payment-history-rows]");

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

    async function fetchLedgerRows(customerId, options = {}) {
        const params = new URLSearchParams();
        if (customerId) {
            params.set("customerId", customerId);
        }
        if (!options.ignoreDateRange && startDateInput.value) {
            params.set("startDate", startDateInput.value);
        }
        if (!options.ignoreDateRange && endDateInput.value) {
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

    function getCustomerResultRows(resultsElement) {
        return Array.from(resultsElement.querySelectorAll(".customer-result-row[data-customer-index]"));
    }

    function updateCustomerActiveRow(resultsElement, activeIndex) {
        getCustomerResultRows(resultsElement).forEach((row, index) => {
            const active = index === activeIndex;
            row.classList.toggle("is-active", active);
            if (active) {
                row.scrollIntoView({ block: "nearest" });
            }
        });
    }

    function moveCustomerActiveRow(resultsElement, activeIndex, direction) {
        const rows = getCustomerResultRows(resultsElement);
        if (!rows.length) {
            return -1;
        }

        const nextIndex = activeIndex < 0
                ? (direction > 0 ? 0 : rows.length - 1)
                : (activeIndex + direction + rows.length) % rows.length;
        updateCustomerActiveRow(resultsElement, nextIndex);
        return nextIndex;
    }

    function pickCustomerByKeyboard(resultsElement, activeIndex) {
        const rows = getCustomerResultRows(resultsElement);
        if (!rows.length) {
            return false;
        }

        const index = activeIndex >= 0 ? activeIndex : 0;
        rows[index].click();
        return true;
    }

    function renderCustomerResults(customers) {
        const uniqueCustomers = uniqueCustomersByName(customers);
        customerResults.innerHTML = "";
        state.customerActiveIndex = -1;

        if (!uniqueCustomers.length) {
            customerResults.innerHTML = `<button type="button" class="customer-result-row"><span>검색된 고객 없음</span></button>`;
            customerResults.hidden = false;
            return;
        }

        uniqueCustomers.forEach((customer, index) => {
            const row = document.createElement("button");
            row.type = "button";
            row.className = "customer-result-row";
            row.dataset.customerIndex = String(index);
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
        state.customerActiveIndex = -1;
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

    function handleCustomerKeydown(event) {
        if (event.key === "ArrowDown") {
            event.preventDefault();
            state.customerActiveIndex = moveCustomerActiveRow(customerResults, state.customerActiveIndex, 1);
            customerResults.hidden = false;
            return;
        }

        if (event.key === "ArrowUp") {
            event.preventDefault();
            state.customerActiveIndex = moveCustomerActiveRow(customerResults, state.customerActiveIndex, -1);
            customerResults.hidden = false;
            return;
        }

        if (event.key === "Enter") {
            event.preventDefault();
            if (!customerResults.hidden && pickCustomerByKeyboard(customerResults, state.customerActiveIndex)) {
                return;
            }
            searchArButton.click();
            return;
        }

        if (event.key === "Escape") {
            customerResults.hidden = true;
        }
    }

    async function selectCustomer(customer) {
        state.selectedCustomer = customer;
        state.selectedSale = null;
        customerInput.value = customer.name;
        customerClear.hidden = false;
        customerResults.hidden = true;
        customerResults.innerHTML = "";
        paymentCustomerLabel.textContent = customer.name;
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
        updatePaymentDue(null, "선택 기준 미수금");
        clearSelectedSale();
        renderPaymentHistoryRows([]);
    }

    async function showAllCustomersView() {
        state.selectedCustomer = null;
        state.selectedSale = null;
        currentBalance.textContent = "0";
        paymentCustomerLabel.textContent = "고객을 선택하세요.";
        paymentAmount.value = "";
        paymentMemo.value = "";
        paymentButton.disabled = true;
        updatePaymentDue(null, "선택 기준 미수금");
        clearSelectedSale();
        arMessage.textContent = "전체 고객의 최종 미수금액을 조회합니다.";
        paymentMessage.textContent = "";

        try {
            // 고객명이 비어 있는 전체 조회에서는 기간 필터와 수금 이력 목록을 보여주지 않고,
            // ArTx 전체 흐름을 고객별로 합산해 현재 최종 미수금 요약만 표시한다.
            const ledger = await fetchLedgerRows(null, { ignoreDateRange: true });
            renderAllCustomerBalanceRows(ledger);
        } catch (error) {
            arMessage.textContent = error.message;
            renderPaymentHistoryRows([]);
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
        paymentCustomerLabel.textContent = state.selectedCustomer.name;

        const ledger = await fetchLedgerRows(state.selectedCustomer.id);
        renderPaymentHistoryRows(ledger);
    }

    function renderPaymentHistoryRows(rows) {
        paymentHistoryRows.innerHTML = "";

        // 고객을 특정한 경우에는 요청대로 수금일자/금액/수금 후 잔액 이력을 보여준다.
        const paymentRows = rows.filter((row) => row.txType === "수금");
        if (!paymentRows.length) {
            paymentHistoryRows.innerHTML = `<div class="empty-table-state">조회된 수금 이력이 없습니다.</div>`;
            return;
        }

        const balanceByPaymentRow = buildPaymentBalanceMap(rows);

        paymentRows.forEach((row) => {
            const amount = Number(row.paymentAmount || 0) || Math.abs(Number(row.arDelta || 0));
            const customerBalance = balanceByPaymentRow.has(row) ? balanceByPaymentRow.get(row) : row.balance;
            const line = document.createElement("div");
            line.className = "ar-payment-history-row";
            line.innerHTML = `
                <span>${row.txDate || ""}</span>
                <span title="${row.customerName || ""}">${row.customerName || ""}</span>
                <span>${row.salesOrderId || "고객 전체"}</span>
                <span class="number">${money(amount)}</span>
                <span class="number">${money(customerBalance)}</span>
                <span title="${row.memo || ""}">${row.memo || ""}</span>
            `;
            paymentHistoryRows.appendChild(line);
        });
    }

    function renderAllCustomerBalanceRows(rows) {
        paymentHistoryRows.innerHTML = "";

        // 전체 조회 모드에서는 표의 기존 컬럼을 재사용하되,
        // 각 고객의 최종 미수금만 한 줄씩 보여준다.
        const balances = buildCustomerBalanceSummaries(rows);
        const totalBalance = balances.reduce((sum, row) => sum + row.balance, 0);
        currentBalance.textContent = money(totalBalance);

        if (!balances.length) {
            paymentHistoryRows.innerHTML = `<div class="empty-table-state">현재 미수금이 있는 고객이 없습니다.</div>`;
            return;
        }

        balances.forEach((row) => {
            const line = document.createElement("div");
            line.className = "ar-payment-history-row";
            line.innerHTML = `
                <span>${row.lastDate || ""}</span>
                <span title="${row.customerName || ""}">${row.customerName || ""}</span>
                <span>고객 최종</span>
                <span class="number"></span>
                <span class="number">${money(row.balance)}</span>
                <span>최종 미수금</span>
            `;
            paymentHistoryRows.appendChild(line);
        });
    }

    function buildCustomerBalanceSummaries(rows) {
        const summaries = new Map();

        // 프로젝트 기준 정책에 맞춰 고객별 현재 미수금은 ArTx(arDelta) 합계로 계산한다.
        // row.balance는 기간 조회/정렬 조건의 영향을 받을 수 있어 전체 요약에서는 직접 합산한다.
        rows.forEach((row) => {
            const delta = Number(row.arDelta || 0);
            if (delta === 0) {
                return;
            }

            const key = customerKey(row);
            if (!summaries.has(key)) {
                summaries.set(key, {
                    customerName: row.customerName || "",
                    lastDate: row.txDate || "",
                    balance: 0
                });
            }

            const summary = summaries.get(key);
            summary.balance += delta;
            if ((row.txDate || "") > (summary.lastDate || "")) {
                summary.lastDate = row.txDate || "";
            }
        });

        return Array.from(summaries.values())
                .filter((row) => row.balance !== 0)
                .sort((left, right) => {
                    const balanceDiff = right.balance - left.balance;
                    if (balanceDiff !== 0) {
                        return balanceDiff;
                    }
                    return (left.customerName || "").localeCompare(right.customerName || "", "ko-KR");
                });
    }

    function customerKey(row) {
        if (row.customerId != null) {
            return `id:${row.customerId}`;
        }
        return `name:${row.customerName || ""}`;
    }

    function txSortPriority(row) {
        if (row.txType === "오더합계") {
            return 1;
        }
        if (row.txType === "반품") {
            return 2;
        }
        if (row.txType === "수금") {
            return 3;
        }
        return 0;
    }

    function buildPaymentBalanceMap(rows) {
        const balanceByPaymentRow = new Map();
        const rowsByCustomer = new Map();

        rows.forEach((row, index) => {
            if (Number(row.arDelta || 0) === 0 && row.txType !== "수금") {
                return;
            }

            const key = customerKey(row);
            if (!rowsByCustomer.has(key)) {
                rowsByCustomer.set(key, []);
            }
            rowsByCustomer.get(key).push({ row, index });
        });

        rowsByCustomer.forEach((entries) => {
            const first = entries[0].row;
            let balance = Number(first.balance || 0) - Number(first.arDelta || 0);

            entries
                    .slice()
                    .sort((left, right) => {
                        const leftDate = left.row.txDate || "";
                        const rightDate = right.row.txDate || "";
                        if (leftDate !== rightDate) {
                            return leftDate.localeCompare(rightDate);
                        }

                        const leftOrderId = left.row.salesOrderId == null ? Number.MAX_SAFE_INTEGER : Number(left.row.salesOrderId);
                        const rightOrderId = right.row.salesOrderId == null ? Number.MAX_SAFE_INTEGER : Number(right.row.salesOrderId);
                        if (leftOrderId !== rightOrderId) {
                            return leftOrderId - rightOrderId;
                        }

                        const priorityDiff = txSortPriority(left.row) - txSortPriority(right.row);
                        if (priorityDiff !== 0) {
                            return priorityDiff;
                        }

                        return left.index - right.index;
                    })
                    .forEach(({ row }) => {
                        balance += Number(row.arDelta || 0);
                        if (row.txType === "수금") {
                            balanceByPaymentRow.set(row, balance);
                        }
                    });
        });

        return balanceByPaymentRow;
    }

    function clearSelectedSale() {
        state.selectedSale = null;
        paymentSalesOrder.value = "";
        clearSalesOrderButton.disabled = true;
        if (!paymentModal.hidden) {
            updatePaymentDueForSelection();
        }
    }

    async function openPaymentModal() {
        paymentCustomerInput.value = state.selectedCustomer ? state.selectedCustomer.name : "";
        paymentCustomerLabel.textContent = state.selectedCustomer ? state.selectedCustomer.name : "고객을 선택하세요.";
        paymentMessage.textContent = "";
        paymentModal.hidden = false;

        if (state.selectedCustomer) {
            await populatePaymentSalesOptions(state.selectedCustomer.id);
            paymentAmount.focus();
            return;
        }

        renderPaymentSalesOptions([]);
        paymentCustomerInput.focus();
    }

    function closePaymentModal() {
        paymentModal.hidden = true;
        paymentMessage.textContent = "";
        paymentCustomerResults.hidden = true;
        paymentCustomerResults.innerHTML = "";
    }

    function renderPaymentCustomerResults(customers) {
        const uniqueCustomers = uniqueCustomersByName(customers);
        paymentCustomerResults.innerHTML = "";
        state.paymentCustomerActiveIndex = -1;

        if (!uniqueCustomers.length) {
            paymentCustomerResults.innerHTML = `<button type="button" class="customer-result-row"><span>검색된 고객 없음</span></button>`;
            paymentCustomerResults.hidden = false;
            return;
        }

        uniqueCustomers.forEach((customer, index) => {
            const row = document.createElement("button");
            row.type = "button";
            row.className = "customer-result-row";
            row.dataset.customerIndex = String(index);
            row.innerHTML = `<span>${customer.name}</span><small>${customer.phone || "기존 고객"}</small>`;
            row.addEventListener("click", () => selectPaymentCustomer(customer));
            paymentCustomerResults.appendChild(row);
        });

        paymentCustomerResults.hidden = false;
    }

    function handlePaymentCustomerInput() {
        const keyword = paymentCustomerInput.value.trim();
        state.selectedCustomer = null;
        state.selectedSale = null;
        state.paymentCustomerActiveIndex = -1;
        paymentCustomerLabel.textContent = "고객을 선택하세요.";
        paymentButton.disabled = true;
        updatePaymentDue(null, "선택 기준 미수금");
        renderPaymentSalesOptions([]);
        clearTimeout(state.paymentCustomerSearchTimer);

        if (!keyword) {
            paymentCustomerResults.hidden = true;
            paymentCustomerResults.innerHTML = "";
            return;
        }

        state.paymentCustomerSearchTimer = setTimeout(async () => {
            try {
                renderPaymentCustomerResults(await searchCustomers(keyword));
            } catch (error) {
                paymentCustomerResults.innerHTML = `<button type="button" class="customer-result-row"><span>${error.message}</span></button>`;
                paymentCustomerResults.hidden = false;
            }
        }, 180);
    }

    function handlePaymentCustomerKeydown(event) {
        if (event.key === "ArrowDown") {
            event.preventDefault();
            state.paymentCustomerActiveIndex = moveCustomerActiveRow(paymentCustomerResults, state.paymentCustomerActiveIndex, 1);
            paymentCustomerResults.hidden = false;
            return;
        }

        if (event.key === "ArrowUp") {
            event.preventDefault();
            state.paymentCustomerActiveIndex = moveCustomerActiveRow(paymentCustomerResults, state.paymentCustomerActiveIndex, -1);
            paymentCustomerResults.hidden = false;
            return;
        }

        if (event.key === "Enter") {
            event.preventDefault();
            if (!paymentCustomerResults.hidden && pickCustomerByKeyboard(paymentCustomerResults, state.paymentCustomerActiveIndex)) {
                return;
            }
            paymentAmount.focus();
            return;
        }

        if (event.key === "Escape") {
            paymentCustomerResults.hidden = true;
        }
    }

    async function selectPaymentCustomer(customer) {
        state.selectedCustomer = customer;
        state.selectedSale = null;
        customerInput.value = customer.name;
        customerClear.hidden = false;
        paymentCustomerInput.value = customer.name;
        paymentCustomerLabel.textContent = customer.name;
        paymentCustomerResults.hidden = true;
        paymentCustomerResults.innerHTML = "";
        await refreshCustomerAr();
        await populatePaymentSalesOptions(customer.id);
    }

    async function populatePaymentSalesOptions(customerId) {
        const sales = await fetchCreditSales(customerId);
        state.paymentSales = sales.filter((sale) => sale.paymentType === "CREDIT");
        renderPaymentSalesOptions(state.paymentSales);
        await updatePaymentDueForSelection();
    }

    function renderPaymentSalesOptions(sales) {
        paymentSalesOrder.innerHTML = `<option value="">고객 전체 수금</option>`;
        state.selectedSale = null;
        clearSalesOrderButton.disabled = true;

        sales.forEach((sale) => {
            const option = document.createElement("option");
            option.value = String(sale.salesOrderId);
            option.textContent = `전표 ${sale.salesOrderId} · ${sale.salesDate || ""} · ${sale.representativeProductName || ""} · ${money(sale.totalAmount)}`;
            paymentSalesOrder.appendChild(option);
        });
    }

    function handlePaymentSalesOrderChange() {
        const salesOrderId = Number(paymentSalesOrder.value || 0);
        state.selectedSale = state.paymentSales.find((sale) => Number(sale.salesOrderId) === salesOrderId) || null;
        clearSalesOrderButton.disabled = !state.selectedSale;
        updatePaymentDueForSelection();
    }

    function updatePaymentDue(amount, label) {
        paymentDueLabel.textContent = label || "선택 기준 미수금";
        paymentDueAmount.textContent = amount == null ? "0" : money(amount);
    }

    async function updatePaymentDueForSelection() {
        if (!state.selectedCustomer) {
            updatePaymentDue(null, "선택 기준 미수금");
            return;
        }

        if (!state.selectedSale) {
            const balance = await fetchArBalance(state.selectedCustomer.id);
            updatePaymentDue(balance, "고객 전체 미수금");
            return;
        }

        const due = await calculateSalesOrderArBalance(state.selectedCustomer.id, state.selectedSale.salesOrderId);
        updatePaymentDue(due, `전표 ${state.selectedSale.salesOrderId} 미수금`);
    }

    async function calculateSalesOrderArBalance(customerId, salesOrderId) {
        const rows = await fetchLedgerRows(customerId, { ignoreDateRange: true });
        return rows
                .filter((row) => Number(row.salesOrderId) === Number(salesOrderId))
                .reduce((sum, row) => sum + Number(row.arDelta || 0), 0);
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
        closePaymentModal();
    }

    customerInput.addEventListener("input", handleCustomerInput);
    customerInput.addEventListener("keydown", handleCustomerKeydown);
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
    openPaymentModalButton.addEventListener("click", openPaymentModal);
    paymentCustomerInput.addEventListener("input", handlePaymentCustomerInput);
    paymentCustomerInput.addEventListener("keydown", handlePaymentCustomerKeydown);
    paymentSalesOrder.addEventListener("change", handlePaymentSalesOrderChange);
    closePaymentModalButtons.forEach((button) => button.addEventListener("click", closePaymentModal));
    paymentModal.addEventListener("click", (event) => {
        if (event.target === paymentModal) {
            closePaymentModal();
        }
    });
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
        if (!paymentCustomerResults.contains(event.target) && event.target !== paymentCustomerInput) {
            paymentCustomerResults.hidden = true;
        }
    });

    startDateInput.value = firstDayOfMonth();
    endDateInput.value = todayString();
    showAllCustomersView();
})();
