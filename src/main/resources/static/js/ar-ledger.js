(function () {
    const state = {
        selectedCustomer: null,
        customerSearchTimer: null,
        customerActiveIndex: -1
    };

    const customerInput = document.querySelector("[data-ledger-customer-search]");
    const customerResults = document.querySelector("[data-ledger-customer-results]");
    const customerClear = document.querySelector("[data-ledger-customer-clear]");
    const startDateInput = document.querySelector("[data-ledger-start-date]");
    const endDateInput = document.querySelector("[data-ledger-end-date]");
    const searchButton = document.querySelector("[data-search-ledger]");
    const message = document.querySelector("[data-ledger-message]");
    const ledgerRows = document.querySelector("[data-ledger-rows]");

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

    function getCustomerRows() {
        return Array.from(customerResults.querySelectorAll(".customer-result-row[data-customer-index]"));
    }

    function updateActiveCustomerRow() {
        getCustomerRows().forEach((row, index) => {
            const active = index === state.customerActiveIndex;
            row.classList.toggle("is-active", active);
            if (active) {
                row.scrollIntoView({ block: "nearest" });
            }
        });
    }

    function moveActiveCustomer(direction) {
        const rows = getCustomerRows();
        if (!rows.length) {
            state.customerActiveIndex = -1;
            return;
        }

        state.customerActiveIndex = state.customerActiveIndex < 0
                ? (direction > 0 ? 0 : rows.length - 1)
                : (state.customerActiveIndex + direction + rows.length) % rows.length;
        updateActiveCustomerRow();
    }

    function pickActiveCustomer() {
        const rows = getCustomerRows();
        if (!rows.length) {
            return false;
        }

        const index = state.customerActiveIndex >= 0 ? state.customerActiveIndex : 0;
        rows[index].click();
        return true;
    }

    function selectCustomer(customer) {
        state.selectedCustomer = customer;
        customerInput.value = customer.name;
        customerClear.hidden = false;
        customerResults.hidden = true;
        customerResults.innerHTML = "";
        state.customerActiveIndex = -1;
    }

    function clearCustomer() {
        state.selectedCustomer = null;
        customerInput.value = "";
        customerClear.hidden = true;
        customerResults.hidden = true;
        customerResults.innerHTML = "";
        state.customerActiveIndex = -1;
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
        }, 160);
    }

    function selectedCustomerIdForSearch() {
        if (!customerInput.value.trim()) {
            state.selectedCustomer = null;
            customerClear.hidden = true;
            return null;
        }
        return state.selectedCustomer ? state.selectedCustomer.id : null;
    }

    async function fetchLedgerRows() {
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

        const response = await fetch(`/api/ar-ledger?${params.toString()}`);
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "거래처 원장 조회에 실패했습니다.");
        }
        return response.json();
    }

    function renderRows(rows) {
        ledgerRows.innerHTML = "";
        if (!rows.length) {
            ledgerRows.innerHTML = `<div class="empty-table-state">조회된 원장 내역이 없습니다.</div>`;
            return;
        }

        rows.forEach((row) => {
            const element = document.createElement("div");
            element.className = `ledger-row ${row.summaryRow ? "is-summary" : ""}`;
            element.innerHTML = `
                <span>${row.txDate || ""}</span>
                <span title="${row.customerName || ""}">${row.customerName || ""}</span>
                <span>${row.txType || ""}</span>
                <span>${row.salesOrderId || ""}</span>
                <span title="${row.productName || ""}">${row.productName || ""}</span>
                <span class="number">${row.quantity == null ? "" : money(row.quantity)}</span>
                <span class="number">${row.unitPrice == null ? "" : money(row.unitPrice)}</span>
                <span class="number">${money(row.saleAmount)}</span>
                <span class="number">${money(row.paymentAmount)}</span>
                <span class="number">${money(row.arDelta)}</span>
                <span class="number strong">${money(row.balance)}</span>
                <span title="${row.memo || ""}">${row.memo || ""}</span>
            `;
            ledgerRows.appendChild(element);
        });
    }

    async function searchLedger() {
        message.textContent = "";
        ledgerRows.innerHTML = `<div class="empty-table-state">거래처 원장을 불러오는 중입니다.</div>`;
        const rows = await fetchLedgerRows();
        renderRows(rows);
        const target = customerInput.value.trim() ? customerInput.value.trim() : "전체 고객";
        message.textContent = `${target} 원장 ${rows.length}건을 조회했습니다.`;
    }

    function bindEvents() {
        searchButton.addEventListener("click", async () => {
            try {
                await searchLedger();
            } catch (error) {
                message.textContent = error.message;
            }
        });

        [startDateInput, endDateInput].forEach((input) => {
            input.addEventListener("keydown", async (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    try {
                        await searchLedger();
                    } catch (error) {
                        message.textContent = error.message;
                    }
                }
            });
        });

        customerInput.addEventListener("input", handleCustomerInput);
        customerInput.addEventListener("keydown", async (event) => {
            if (event.key === "ArrowDown") {
                event.preventDefault();
                moveActiveCustomer(1);
                return;
            }
            if (event.key === "ArrowUp") {
                event.preventDefault();
                moveActiveCustomer(-1);
                return;
            }
            if (event.key === "Enter") {
                event.preventDefault();
                if (!customerResults.hidden && pickActiveCustomer()) {
                    return;
                }
                try {
                    await searchLedger();
                } catch (error) {
                    message.textContent = error.message;
                }
            }
        });

        customerClear.addEventListener("click", clearCustomer);

        document.addEventListener("click", (event) => {
            if (!customerResults.contains(event.target) && event.target !== customerInput) {
                customerResults.hidden = true;
            }
        });
    }

    startDateInput.value = firstDayOfMonth();
    endDateInput.value = todayString();
    bindEvents();
    searchLedger().catch((error) => {
        message.textContent = error.message;
    });
})();
