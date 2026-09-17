(function () {
    const state = {
        selectedCustomer: null,
        customerSearchTimer: null,
        customerActiveIndex: -1,
        rows: []
    };

    const customerInput = document.querySelector("[data-ledger-customer-search]");
    const customerResults = document.querySelector("[data-ledger-customer-results]");
    const customerClear = document.querySelector("[data-ledger-customer-clear]");
    const startDateInput = document.querySelector("[data-ledger-start-date]");
    const endDateInput = document.querySelector("[data-ledger-end-date]");
    const searchButton = document.querySelector("[data-search-ledger]");
    const exportWordButton = document.querySelector("[data-export-ledger-word]");
    const exportHwpButton = document.querySelector("[data-export-ledger-hwp]");
    const detailToggleButton = document.querySelector("[data-toggle-ledger-detail]");
    const detailOptions = document.querySelector("[data-ledger-detail-options]");
    const typeFilters = document.querySelectorAll("[data-ledger-type-filter]");
    const columnFilters = document.querySelectorAll("[data-ledger-column-filter]");
    const message = document.querySelector("[data-ledger-message]");
    const ledgerRows = document.querySelector("[data-ledger-rows]");
    const ledgerTable = document.querySelector(".ledger-table");
    const ledgerHeader = document.querySelector(".ledger-table .table-header");

    const columnWidths = {
        date: "92px",
        customer: "112px",
        type: "96px",
        order: "54px",
        product: "minmax(240px, 1.7fr)",
        quantity: "54px",
        unitPrice: "82px",
        sale: "102px",
        payment: "102px",
        return: "102px",
        delta: "98px",
        balance: "102px",
        memo: "minmax(120px, 0.7fr)"
    };

    function money(value) {
        return Number(value || 0).toLocaleString("ko-KR");
    }

    function escapeHtml(value) {
        return String(value || "")
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#39;");
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

    async function resolveCustomerIdForSearch() {
        const keyword = customerInput.value.trim();

        if (!keyword) {
            state.selectedCustomer = null;
            customerClear.hidden = true;
            return null;
        }

        if (state.selectedCustomer && state.selectedCustomer.name === keyword) {
            return state.selectedCustomer.id;
        }

        const customers = uniqueCustomersByName(await searchCustomers(keyword));
        const matched = customers.find((customer) => customer.name === keyword) || customers[0];

        if (!matched) {
            throw new Error("입력한 고객을 찾을 수 없습니다.");
        }

        selectCustomer(matched);
        return matched.id;
    }

    async function fetchLedgerRows() {
        const params = new URLSearchParams();
        const customerId = await resolveCustomerIdForSearch();
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
        const visibleRows = filterRows(rows);
        if (!visibleRows.length) {
            ledgerRows.innerHTML = `<div class="empty-table-state">조회된 원장 내역이 없습니다.</div>`;
            applyColumnVisibility();
            return;
        }

        visibleRows.forEach((row) => {
            const returnAmount = row.txType === "반품" ? Math.abs(Number(row.arDelta || 0)) : 0;
            const element = document.createElement("div");
            element.className = `ledger-row ${row.summaryRow ? "is-summary" : ""}`;
            element.innerHTML = `
                <span data-col="date">${row.txDate || ""}</span>
                <span data-col="customer" title="${row.customerName || ""}">${row.customerName || ""}</span>
                <span data-col="type">${row.txType || ""}</span>
                <span data-col="order">${row.salesOrderId || ""}</span>
                <span data-col="product" title="${row.productName || ""}">${row.productName || ""}</span>
                <span data-col="quantity" class="number">${row.quantity == null ? "" : money(row.quantity)}</span>
                <span data-col="unitPrice" class="number">${row.unitPrice == null ? "" : money(row.unitPrice)}</span>
                <span data-col="sale" class="number">${money(row.saleAmount)}</span>
                <span data-col="payment" class="number">${money(row.paymentAmount)}</span>
                <span data-col="return" class="number">${money(returnAmount)}</span>
                <span data-col="delta" class="number">${money(row.arDelta)}</span>
                <span data-col="balance" class="number strong">${money(row.balance)}</span>
                <span data-col="memo" title="${row.memo || ""}">${row.memo || ""}</span>
            `;
            ledgerRows.appendChild(element);
        });
        ledgerRows.appendChild(createTotalRow(visibleRows));
        applyColumnVisibility();
    }

    function createTotalRow(rows) {
        const hasOrderSummary = rows.some((row) => Boolean(row.summaryRow));
        const totals = rows.reduce((sum, row) => {
            const isOrderSummary = Boolean(row.summaryRow);
            const isCreditSaleLine = row.txType === "판매(외상)" && !isOrderSummary;
            const isPayment = row.txType === "수금";
            const isReturn = row.txType === "반품";
            const isRefundSettlement = row.txType === "환불정산";
            const shouldSumSale = hasOrderSummary ? isOrderSummary : isCreditSaleLine;

            // 오더합계가 보이면 오더합계 기준, 숨겨져 있으면 현재 보이는 판매 품목행 기준으로 합산한다.
            if (shouldSumSale) {
                sum.quantity += Number(row.quantity || 0);
                sum.saleAmount += Number(row.saleAmount || 0);
            }
            if (isPayment) {
                sum.paymentAmount += Number(row.paymentAmount || 0);
            }
            if (isReturn) {
                sum.returnAmount += Math.abs(Number(row.arDelta || 0));
            }
            if (shouldSumSale || isPayment || isReturn || isRefundSettlement) {
                sum.arDelta += Number(row.arDelta || 0);
            }
            return sum;
        }, {
            quantity: 0,
            saleAmount: 0,
            paymentAmount: 0,
            returnAmount: 0,
            arDelta: 0
        });

        const lastBalance = rows.length ? Number(rows[rows.length - 1].balance || 0) : 0;
        const element = document.createElement("div");
        element.className = "ledger-row ledger-total-row";
        element.innerHTML = `
            <span data-col="date"></span>
            <span data-col="customer"></span>
            <span data-col="type">총계</span>
            <span data-col="order"></span>
            <span data-col="product">[조회 총계]</span>
            <span data-col="quantity" class="number">${money(totals.quantity)}</span>
            <span data-col="unitPrice" class="number"></span>
            <span data-col="sale" class="number">${money(totals.saleAmount)}</span>
            <span data-col="payment" class="number">${money(totals.paymentAmount)}</span>
            <span data-col="return" class="number">${money(totals.returnAmount)}</span>
            <span data-col="delta" class="number">${money(totals.arDelta)}</span>
            <span data-col="balance" class="number strong">${money(lastBalance)}</span>
            <span data-col="memo"></span>
        `;
        return element;
    }

    function checkedValues(inputs) {
        return new Set(Array.from(inputs)
                .filter((input) => input.checked)
                .map((input) => input.value));
    }

    function filterRows(rows) {
        const visibleTypes = checkedValues(typeFilters);
        return rows.filter((row) => visibleTypes.has(row.txType || ""));
    }

    function applyColumnVisibility() {
        const visibleColumns = checkedValues(columnFilters);
        document.querySelectorAll("[data-col]").forEach((cell) => {
            cell.hidden = !visibleColumns.has(cell.dataset.col);
        });

        const templateColumns = Array.from(columnFilters)
                .filter((input) => input.checked)
                .map((input) => columnWidths[input.value])
                .join(" ");

        const nextTemplate = templateColumns || columnWidths.date;
        ledgerHeader.style.gridTemplateColumns = nextTemplate;
        ledgerRows.querySelectorAll(".ledger-row").forEach((row) => {
            row.style.gridTemplateColumns = nextTemplate;
        });

        const activeColumns = Array.from(columnFilters).filter((input) => input.checked).length;
        ledgerTable.classList.toggle("is-compact-columns", activeColumns <= 8);
    }

    function visibleCellTexts(container) {
        return Array.from(container.querySelectorAll("[data-col]"))
                .filter((cell) => !cell.hidden)
                .map((cell) => cell.textContent.trim());
    }

    function buildExportRows() {
        const headers = visibleCellTexts(ledgerHeader);
        const rows = Array.from(ledgerRows.querySelectorAll(".ledger-row"))
                .map((row) => ({
                    summary: row.classList.contains("is-summary"),
                    total: row.classList.contains("ledger-total-row"),
                    cells: visibleCellTexts(row)
                }))
                .filter((row) => row.cells.length);

        return { headers, rows };
    }

    function documentDateTime() {
        const now = new Date();
        const pad = (value) => String(value).padStart(2, "0");
        return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`;
    }

    function safeFilePart(value) {
        return String(value || "전체")
                .trim()
                .replace(/[\\/:*?"<>|]/g, "")
                .replace(/\s+/g, "_")
                .slice(0, 30) || "전체";
    }

    function buildLedgerExportHtml() {
        const { headers, rows } = buildExportRows();
        if (!headers.length || !rows.length) {
            throw new Error("추출할 원장 내역이 없습니다. 먼저 조회해 주세요.");
        }

        const customerName = customerInput.value.trim() || "전체 고객";
        const periodText = `${startDateInput.value || "-"} ~ ${endDateInput.value || "-"}`;
        const headerCells = headers.map((header) => `<th>${escapeHtml(header)}</th>`).join("");
        const bodyRows = rows.map((row) => {
            const className = row.total ? "total-row" : (row.summary ? "summary-row" : "");
            const cells = row.cells.map((cell) => `<td>${escapeHtml(cell)}</td>`).join("");
            return `<tr class="${className}">${cells}</tr>`;
        }).join("");

        return `<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>거래처 원장</title>
<style>
@page { size: A4 landscape; margin: 12mm; }
body { font-family: "Malgun Gothic", "Apple SD Gothic Neo", sans-serif; color: #222; }
h1 { margin: 0 0 10px; text-align: center; font-size: 22px; }
.meta { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 10px; font-size: 12px; }
table { width: 100%; border-collapse: collapse; table-layout: fixed; font-size: 10px; page-break-inside: auto; }
thead { display: table-header-group; }
tfoot { display: table-footer-group; }
tr { page-break-inside: avoid; page-break-after: auto; }
th, td { border: 1px solid #8d978f; padding: 4px 5px; word-break: break-all; vertical-align: middle; }
th { background: #dfe8de; font-weight: 700; text-align: center; }
td { text-align: left; }
td:nth-child(n+6) { text-align: right; }
.summary-row { background: #fff8f3; font-weight: 700; }
.total-row { background: #e7eee4; font-weight: 800; }
</style>
</head>
<body>
<h1>거래처 원장</h1>
<div class="meta">
    <span>고객명: ${escapeHtml(customerName)}</span>
    <span>조회기간: ${escapeHtml(periodText)}</span>
    <span>추출일시: ${escapeHtml(documentDateTime())}</span>
</div>
<table>
    <thead><tr>${headerCells}</tr></thead>
    <tbody>${bodyRows}</tbody>
</table>
</body>
</html>`;
    }

    function downloadLedgerExport(extension, mimeType) {
        try {
            const html = buildLedgerExportHtml();
            const customerName = safeFilePart(customerInput.value.trim() || "전체고객");
            const today = todayString().replace(/-/g, "");
            const blob = new Blob(["\ufeff", html], { type: `${mimeType};charset=utf-8` });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;
            link.download = `거래처원장_${customerName}_${today}.${extension}`;
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
        } catch (error) {
            alert(error.message);
        }
    }

    async function searchLedger() {
        message.textContent = "";
        ledgerRows.innerHTML = `<div class="empty-table-state">거래처 원장을 불러오는 중입니다.</div>`;
        state.rows = await fetchLedgerRows();
        renderRows(state.rows);
        const target = customerInput.value.trim() ? customerInput.value.trim() : "전체 고객";
        message.textContent = `${target} 원장 ${state.rows.length}건을 조회했습니다.`;
    }

    function bindEvents() {
        searchButton.addEventListener("click", async () => {
            try {
                await searchLedger();
            } catch (error) {
                message.textContent = error.message;
            }
        });

        detailToggleButton.addEventListener("click", () => {
            detailOptions.hidden = !detailOptions.hidden;
        });

        exportWordButton.addEventListener("click", () => {
            downloadLedgerExport("doc", "application/msword");
        });

        exportHwpButton.addEventListener("click", () => {
            downloadLedgerExport("hwp", "application/x-hwp");
        });

        [...typeFilters, ...columnFilters].forEach((input) => {
            input.addEventListener("change", () => renderRows(state.rows));
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
