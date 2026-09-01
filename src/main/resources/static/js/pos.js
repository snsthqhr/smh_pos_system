(function () {
    const storageKey = "samhwa.pos.favoriteGroups.v1";
    const defaults = [
        { name: "에나멜", items: [] },
        { name: "수성", items: ["아이생각 수성 외부"] },
        { name: "우레탄", items: [] },
        { name: "로라", items: [] },
        {
            name: "실리콘",
            items: ["우레탄실리콘", "유성실리콘", "일반수성실리콘", "탄성코킹실리콘", "실리콘 총", "실리콘충"]
        },
        { name: "신나", items: [] },
        { name: "붓", items: [] },
        { name: "기타", items: [] }
    ];

    const state = {
        groups: loadGroups(),
        activeName: "실리콘",
        saleItems: [],
        selectedCustomer: null,
        generalCustomer: null,
        selectedJobSite: null,
        searchTimer: null,
        customerSearchTimer: null,
        jobSiteSearchTimer: null,
        jobSiteActiveIndex: -1,
        returnCustomer: null,
        returnSales: [],
        selectedReturnOrder: null,
        returnCurrentBalance: 0,
        returnSearchTimer: null
    };

    const categoryWrap = document.querySelector("[data-favorite-categories]");
    const itemWrap = document.querySelector("[data-favorite-items]");
    const activeCategoryName = document.querySelector("[data-active-category-name]");
    const activeItemCount = document.querySelector("[data-active-item-count]");
    const favoriteModal = document.querySelector("[data-favorite-modal]");
    const modalCategoryList = document.querySelector("[data-modal-category-list]");
    const modalItemList = document.querySelector("[data-modal-item-list]");
    const modalSelectedCategory = document.querySelector("[data-modal-selected-category]");
    const categoryForm = document.querySelector("[data-category-form]");
    const itemForm = document.querySelector("[data-item-form]");

    const productSearchInput = document.querySelector("[data-product-search-input]");
    const productResults = document.querySelector("[data-product-results]");
    const saleLines = document.querySelector("[data-sale-lines]");
    const emptySaleState = document.querySelector("[data-empty-sale-state]");

    const pickerModal = document.querySelector("[data-product-picker-modal]");
    const pickerAlias = document.querySelector("[data-picker-alias]");
    const pickerSearchInput = document.querySelector("[data-picker-search-input]");
    const pickerSearchButton = document.querySelector("[data-picker-search-button]");
    const pickerResults = document.querySelector("[data-picker-results]");

    const createModal = document.querySelector("[data-product-create-modal]");
    const createForm = document.querySelector("[data-product-create-form]");
    const createMessage = document.querySelector("[data-product-create-message]");
    const customerSearchInput = document.querySelector("[data-customer-search-input]");
    const customerResults = document.querySelector("[data-customer-results]");
    const customerClear = document.querySelector("[data-customer-clear]");
    const selectedCustomerBadge = document.querySelector("[data-selected-customer-badge]");
    const jobSiteSearchInput = document.querySelector("[data-jobsite-search-input]");
    const jobSiteResults = document.querySelector("[data-jobsite-results]");
    const jobSiteClear = document.querySelector("[data-jobsite-clear]");
    const selectedJobSiteBadge = document.querySelector("[data-selected-jobsite-badge]");
    const taxPolicy = document.querySelector("[data-tax-policy]");
    const pricePolicy = document.querySelector("[data-price-policy]");
    const saleMemo = document.querySelector("[data-sale-memo]");
    const saleSaveMessage = document.querySelector("[data-sale-save-message]");
    const summaryCount = document.querySelector("[data-summary-count]");
    const summaryQuantity = document.querySelector("[data-summary-quantity]");
    const summaryTotal = document.querySelector("[data-summary-total]");
    const summaryDiscount = document.querySelector("[data-summary-discount]");
    const clearSaleButton = document.querySelector("[data-clear-sale]");
    const arBalanceInput = document.querySelector("[data-ar-balance-input]");
    const returnOpenButton = document.querySelector("[data-return-open]");
    const returnModal = document.querySelector("[data-return-modal]");
    const returnCloseButtons = document.querySelectorAll("[data-return-close]");
    const returnCustomerSearch = document.querySelector("[data-return-customer-search]");
    const returnCustomerResults = document.querySelector("[data-return-customer-results]");
    const returnSalesOrderSelect = document.querySelector("[data-return-sales-order]");
    const returnItems = document.querySelector("[data-return-items]");
    const returnCurrentBalance = document.querySelector("[data-return-current-balance]");
    const returnTotalAmount = document.querySelector("[data-return-total-amount]");
    const returnExpectedBalance = document.querySelector("[data-return-expected-balance]");
    const returnSettlementNeeded = document.querySelector("[data-return-settlement-needed]");


    function openReturnModal() {
        resetReturnModal();
        returnModal.hidden = false;
        returnCustomerSearch?.focus();
    }

    function closeReturnModal() {
        returnModal.hidden = true;
    }



    function loadGroups() {
        const saved = localStorage.getItem(storageKey);
        if (!saved) {
            return structuredClone(defaults);
        }

        try {
            const parsed = JSON.parse(saved);
            return Array.isArray(parsed) && parsed.length > 0 ? parsed : structuredClone(defaults);
        } catch (error) {
            return structuredClone(defaults);
        }
    }

    function saveGroups() {
        localStorage.setItem(storageKey, JSON.stringify(state.groups));
    }

    function getActiveGroup() {
        return state.groups.find((group) => group.name === state.activeName) || state.groups[0];
    }

    function money(value) {
        return Number(value || 0).toLocaleString("ko-KR");
    }

    function renderFavorites() {
        const activeGroup = getActiveGroup();
        state.activeName = activeGroup.name;

        categoryWrap.innerHTML = "";
        state.groups.forEach((group) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = group.name === state.activeName ? "is-active" : "";
            button.innerHTML = `<strong>${group.name}</strong><small>${group.items.length}개 품목</small>`;
            button.addEventListener("click", () => {
                state.activeName = group.name;
                renderFavorites();
                renderFavoriteModal();
            });
            categoryWrap.appendChild(button);
        });

        itemWrap.innerHTML = "";
        activeGroup.items.forEach((itemName) => {
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = itemName;
            button.addEventListener("click", () => openProductPicker(itemName, activeGroup.name));
            itemWrap.appendChild(button);
        });

        for (let i = activeGroup.items.length; i < 12; i += 1) {
            const emptyButton = document.createElement("button");
            emptyButton.type = "button";
            emptyButton.className = "empty-slot";
            emptyButton.textContent = "";
            itemWrap.appendChild(emptyButton);
        }

        activeCategoryName.textContent = activeGroup.name;
        activeItemCount.textContent = `${activeGroup.items.length}개`;
    }

    function renderFavoriteModal() {
        const activeGroup = getActiveGroup();
        modalSelectedCategory.textContent = activeGroup.name;

        modalCategoryList.innerHTML = "";
        state.groups.forEach((group) => {
            const row = document.createElement("button");
            row.type = "button";
            row.className = group.name === state.activeName ? "is-active" : "";
            row.textContent = `${group.name} (${group.items.length})`;
            row.addEventListener("click", () => {
                state.activeName = group.name;
                renderFavorites();
                renderFavoriteModal();
            });
            modalCategoryList.appendChild(row);
        });

        modalItemList.innerHTML = "";
        activeGroup.items.forEach((itemName, index) => {
            const row = document.createElement("div");
            row.className = "modal-item-row";
            row.innerHTML = `<span>${itemName}</span>`;

            const removeButton = document.createElement("button");
            removeButton.type = "button";
            removeButton.textContent = "삭제";
            removeButton.addEventListener("click", () => {
                activeGroup.items.splice(index, 1);
                saveGroups();
                renderFavorites();
                renderFavoriteModal();
            });

            row.appendChild(removeButton);
            modalItemList.appendChild(row);
        });

        if (activeGroup.items.length === 0) {
            const empty = document.createElement("div");
            empty.className = "modal-empty";
            empty.textContent = "등록된 품목 없음";
            modalItemList.appendChild(empty);
        }
    }

    function openFavoriteModal() {
        renderFavoriteModal();
        favoriteModal.hidden = false;
        itemForm.elements.itemName.focus();
    }

    function closeFavoriteModal() {
        favoriteModal.hidden = true;
    }

    async function searchProducts(keyword, category) {
        const params = new URLSearchParams();
        if (keyword) {
            params.set("keyword", keyword);
        }
        if (category) {
            params.set("category", category);
        }
        const priceCustomer = await getPriceCustomerForSearch();
        if (priceCustomer) {
            params.set("customerId", priceCustomer.id);
        }
        if (state.selectedJobSite) {
            params.set("jobSiteId", state.selectedJobSite.id);
        }

        const response = await fetch(`/api/products?${params.toString()}`);
        if (!response.ok) {
            throw new Error("상품 검색에 실패했습니다.");
        }
        return response.json();
    }

    async function getPriceCustomerForSearch() {
        if (state.selectedCustomer) {
            return state.selectedCustomer;
        }
        if (customerSearchInput.value.trim()) {
            return null;
        }
        return ensureGeneralCustomer();
    }

    async function ensureGeneralCustomer() {
        if (state.generalCustomer) {
            return state.generalCustomer;
        }

        const customers = await searchCustomers("일반");
        const exactCustomer = uniqueCustomersByName(customers)
                .find((customer) => normalizeName(customer.name) === normalizeName("일반"));
        state.generalCustomer = exactCustomer || await createCustomer("일반");
        return state.generalCustomer;
    }

    function renderProductResults(products) {
        productResults.innerHTML = `
            <div class="product-result-header">
                <span>품번</span>
                <span>품명</span>
                <span>규격</span>
                <span>단위</span>
                <span>재고</span>
                <span>판매단가</span>
            </div>
        `;

        if (!products.length) {
            const empty = document.createElement("div");
            empty.className = "product-result-empty";
            empty.textContent = "검색된 상품이 없습니다.";
            productResults.appendChild(empty);
            return;
        }

        products.forEach((product) => {
            const row = document.createElement("button");
            row.type = "button";
            row.className = "product-result-row";
            row.innerHTML = productCells(product);
            row.addEventListener("click", () => addSaleItem(product));
            productResults.appendChild(row);
        });
    }

    function productCells(product) {
        return `
            <span>${product.code || ""}</span>
            <span title="${product.productName || ""}">${product.productName || ""}</span>
            <span>${product.variant || ""}</span>
            <span>${product.unit || ""}</span>
            <span class="number">${money(product.stockQuantity)}</span>
            <span class="number">${money(product.salePrice)}</span>
        `;
    }

    function renderSaleLines() {
        saleLines.innerHTML = "";

        if (!state.saleItems.length) {
            saleLines.hidden = true;
            emptySaleState.hidden = false;
            renderSummary();
            return;
        }

        saleLines.hidden = false;
        emptySaleState.hidden = true;

        state.saleItems.forEach((item) => {
            const amount = Number(item.salePrice || 0) * Number(item.quantity || 1);
            const row = document.createElement("div");
            row.className = "sale-line";
            row.dataset.productId = item.id;
            row.innerHTML = `
                <span title="${item.productName}">${item.productName}</span>
                <span></span>
                <span>${item.unit || item.variant || ""}</span>
                <input type="number" min="0" class="line-price-input" value="${Number(item.salePrice || 0)}" data-unit-price="${item.id}">
                <div class="qty-control">
                    <button type="button" data-qty-minus="${item.id}">-</button>
                    <input type="number" min="0" step="1" value="${Number(item.quantity || 1)}" data-qty-input="${item.id}">
                    <button type="button" data-qty-plus="${item.id}">+</button>
                </div>
                <span class="number">${money(amount)}</span>
                <span class="number">0</span>
                <span class="number">0</span>
                <span></span>
                <span></span>
                <button type="button" class="line-remove-button" data-remove-item="${item.id}">삭제</button>
            `;
            saleLines.appendChild(row);
        });

        renderSummary();
    }

    function renderSummary() {
        const totalQuantity = state.saleItems.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
        const totalAmount = state.saleItems.reduce(
                (sum, item) => sum + Number(item.salePrice || 0) * Number(item.quantity || 0),
                0
        );

        summaryCount.textContent = money(state.saleItems.length);
        summaryQuantity.textContent = money(totalQuantity);
        summaryTotal.textContent = money(totalAmount);
        summaryDiscount.textContent = "0";
    }

    function addSaleItem(product) {
        const existing = state.saleItems.find((item) => item.id === product.id);
        if (existing) {
            existing.quantity += 1;
        } else {
            state.saleItems.push({ ...product, quantity: 1 });
        }
        renderSaleLines();
    }

    function changeQuantity(productId, delta) {
        const item = state.saleItems.find((saleItem) => Number(saleItem.id) === Number(productId));
        if (!item) {
            return;
        }

        item.quantity += delta;
        if (item.quantity <= 0) {
            removeSaleItem(productId);
            return;
        }
        renderSaleLines();
    }

    function setQuantity(productId, value) {
        const item = state.saleItems.find((saleItem) => Number(saleItem.id) === Number(productId));
        if (!item) {
            return;
        }

        const quantity = Math.floor(Number(value || 0));
        if (quantity <= 0) {
            removeSaleItem(productId);
            return;
        }

        item.quantity = quantity;
        renderSaleLines();
    }

    function changeUnitPrice(productId, value) {
        const item = state.saleItems.find((saleItem) => Number(saleItem.id) === Number(productId));
        if (!item) {
            return;
        }

        const unitPrice = Number(value || 0);
        item.salePrice = unitPrice < 0 ? 0 : unitPrice;
        renderSaleLines();
    }

    function removeSaleItem(productId) {
        state.saleItems = state.saleItems.filter((item) => Number(item.id) !== Number(productId));
        renderSaleLines();
    }

    function clearSaleItems() {
        state.saleItems = [];
        saleSaveMessage.textContent = "";
        saleSaveMessage.className = "sale-save-message";
        renderSaleLines();
    }

    function resetSaleFormAfterSave() {
        state.saleItems = [];
        saleMemo.value = "";
        productSearchInput.value = "";
        taxPolicy.checked = false;
        pricePolicy.checked = false;
        renderProductResults([]);
        clearCustomer();
        renderSaleLines();
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

    async function createCustomer(name) {
        const response = await fetch("/api/customers", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name })
        });

        if (!response.ok) {
            throw new Error("고객 등록에 실패했습니다.");
        }
        return response.json();
    }

    async function fetchArBalance(customerId) {
        if (!customerId) {
            return 0;
        }

        const response = await fetch(`/api/customers/${customerId}/ar-balance`);
        if (!response.ok) {
            throw new Error("미수금 조회에 실패했습니다.");
        }

        const data = await response.json();
        return Number(data.currentArBalance || 0);
    }

    async function refreshArBalance(customerId) {
        if (!arBalanceInput) {
            return;
        }

        try {
            const balance = await fetchArBalance(customerId);
            arBalanceInput.value = money(balance);
        } catch (error) {
            arBalanceInput.value = "조회 실패";
        }
    }

    function selectCustomer(customer) {
        state.selectedCustomer = customer;
        clearJobSite();
        customerSearchInput.value = customer.name;
        customerClear.hidden = false;
        selectedCustomerBadge.hidden = false;
        selectedCustomerBadge.textContent = `선택된 고객: ${customer.name}`;
        customerResults.hidden = true;
        customerResults.innerHTML = "";
        refreshArBalance(customer.id);
    }

    function clearCustomer() {
        state.selectedCustomer = null;
        clearJobSite();
        customerSearchInput.value = "";
        customerClear.hidden = true;
        selectedCustomerBadge.hidden = true;
        selectedCustomerBadge.textContent = "";
        customerResults.hidden = true;
        customerResults.innerHTML = "";
        if (arBalanceInput) {
            arBalanceInput.value = "0";
        }
    }

    async function searchJobSites(keyword) {
        if (!state.selectedCustomer) {
            throw new Error("고객을 먼저 선택하세요.");
        }

        const params = new URLSearchParams();
        params.set("customerId", state.selectedCustomer.id);
        if (keyword) {
            params.set("keyword", keyword);
        }

        const response = await fetch(`/api/job-sites?${params.toString()}`);
        if (!response.ok) {
            throw new Error("현장 검색에 실패했습니다.");
        }
        return response.json();
    }

    async function createJobSite(name) {
        if (!state.selectedCustomer) {
            throw new Error("고객을 먼저 선택하세요.");
        }

        const response = await fetch("/api/job-sites", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                customerId: state.selectedCustomer.id,
                name
            })
        });

        if (!response.ok) {
            throw new Error("현장 등록에 실패했습니다.");
        }
        return response.json();
    }

    function selectJobSite(jobSite) {
        state.selectedJobSite = jobSite;
        jobSiteSearchInput.value = jobSite.name;
        jobSiteClear.hidden = false;
        selectedJobSiteBadge.hidden = false;
        selectedJobSiteBadge.textContent = `선택된 현장: ${jobSite.name}`;
        jobSiteResults.hidden = true;
        jobSiteResults.innerHTML = "";
        state.jobSiteActiveIndex = -1;
    }

    function clearJobSite() {
        state.selectedJobSite = null;
        if (!jobSiteSearchInput) {
            return;
        }
        jobSiteSearchInput.value = "";
        jobSiteClear.hidden = true;
        selectedJobSiteBadge.hidden = true;
        selectedJobSiteBadge.textContent = "";
        jobSiteResults.hidden = true;
        jobSiteResults.innerHTML = "";
        state.jobSiteActiveIndex = -1;
    }

    function setActiveJobSiteRow(index) {
        const rows = Array.from(jobSiteResults.querySelectorAll(".customer-result-row:not(:disabled)"));
        if (rows.length === 0) {
            state.jobSiteActiveIndex = -1;
            return;
        }

        const nextIndex = (index + rows.length) % rows.length;
        rows.forEach((row, rowIndex) => {
            row.classList.toggle("is-active", rowIndex === nextIndex);
        });
        state.jobSiteActiveIndex = nextIndex;
        rows[nextIndex].scrollIntoView({ block: "nearest" });
    }

    function activateJobSiteRow() {
        const rows = Array.from(jobSiteResults.querySelectorAll(".customer-result-row:not(:disabled)"));
        if (rows.length === 0 || state.jobSiteActiveIndex < 0) {
            return false;
        }

        rows[state.jobSiteActiveIndex].click();
        return true;
    }

    function renderJobSiteResults(jobSites, keyword) {
        jobSiteResults.innerHTML = "";
        state.jobSiteActiveIndex = -1;
        const hasExactMatch = jobSites.some((jobSite) => normalizeName(jobSite.name) === normalizeName(keyword));

        jobSites.forEach((jobSite) => {
            const row = document.createElement("button");
            row.type = "button";
            row.className = "customer-result-row";
            row.innerHTML = `<span>${jobSite.name}</span><small>기존 현장</small>`;
            row.addEventListener("click", () => selectJobSite(jobSite));
            jobSiteResults.appendChild(row);
        });

        if (keyword && !hasExactMatch) {
            const createRow = document.createElement("button");
            createRow.type = "button";
            createRow.className = "customer-result-row";
            createRow.innerHTML = `<span>신규 현장으로 등록: ${keyword}</span><small>Enter</small>`;
            createRow.addEventListener("click", async () => {
                const jobSite = await createJobSite(keyword);
                selectJobSite(jobSite);
            });
            jobSiteResults.appendChild(createRow);
        }

        jobSiteResults.hidden = false;
        setActiveJobSiteRow(0);
    }

    function handleJobSiteSearchInput() {
        const keyword = jobSiteSearchInput.value.trim();
        state.selectedJobSite = null;
        clearTimeout(state.jobSiteSearchTimer);

        if (!state.selectedCustomer) {
            jobSiteResults.innerHTML = `<button type="button" class="customer-result-row"><span>고객을 먼저 선택하세요.</span></button>`;
            jobSiteResults.hidden = false;
            return;
        }

        state.jobSiteSearchTimer = setTimeout(async () => {
            try {
                const jobSites = await searchJobSites(keyword);
                renderJobSiteResults(jobSites, keyword);
            } catch (error) {
                jobSiteResults.innerHTML = `<button type="button" class="customer-result-row"><span>${error.message}</span></button>`;
                jobSiteResults.hidden = false;
            }
        }, 180);
    }

    function renderCustomerResults(customers, keyword) {
        customerResults.innerHTML = "";
        const uniqueCustomers = uniqueCustomersByName(customers);
        const hasExactMatch = uniqueCustomers.some((customer) => normalizeName(customer.name) === normalizeName(keyword));

        uniqueCustomers.forEach((customer) => {
            const row = document.createElement("button");
            row.type = "button";
            row.className = "customer-result-row";
            row.innerHTML = `<span>${customer.name}</span><small>${customer.phone || "기존 고객"}</small>`;
            row.addEventListener("click", () => selectCustomer(customer));
            customerResults.appendChild(row);
        });

        if (keyword && !hasExactMatch) {
            const createRow = document.createElement("button");
            createRow.type = "button";
            createRow.className = "customer-result-row";
            createRow.innerHTML = `<span>신규 고객으로 등록: ${keyword}</span><small>Enter</small>`;
            createRow.addEventListener("click", async () => {
                const customer = await createCustomer(keyword);
                selectCustomer(customer);
            });
            customerResults.appendChild(createRow);
        }

        customerResults.hidden = false;
    }

    function handleCustomerSearchInput() {
        const keyword = customerSearchInput.value.trim();
        state.selectedCustomer = null;
        clearTimeout(state.customerSearchTimer);

        if (!keyword) {
            customerResults.hidden = true;
            customerResults.innerHTML = "";
            return;
        }

        state.customerSearchTimer = setTimeout(async () => {
            try {
                const customers = await searchCustomers(keyword);
                renderCustomerResults(customers, keyword);
            } catch (error) {
                customerResults.innerHTML = `<button type="button" class="customer-result-row"><span>${error.message}</span></button>`;
                customerResults.hidden = false;
            }
        }, 180);
    }

    function resetReturnModal() {
        state.returnCustomer = null;
        state.returnSales = [];
        state.selectedReturnOrder = null;
        state.returnCurrentBalance = 0;
        if (returnCustomerSearch) {
            returnCustomerSearch.value = "";
        }
        if (returnCustomerResults) {
            returnCustomerResults.hidden = true;
            returnCustomerResults.innerHTML = "";
        }
        renderReturnSalesOptions();
        renderReturnItems(null);
        renderReturnSummary(0);
    }

    async function fetchReturnCreditSales(customerId) {
        const params = new URLSearchParams();
        params.set("customerId", customerId);
        params.set("paymentType", "CREDIT");

        const response = await fetch(`/api/sales-management/sales?${params.toString()}`);
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "외상판매 전표 조회에 실패했습니다.");
        }
        return response.json();
    }

    async function fetchReturnCustomerBalance(customerId) {
        const response = await fetch(`/api/customers/${customerId}/ar-balance`);
        if (!response.ok) {
            throw new Error("고객 미수금 조회에 실패했습니다.");
        }
        const data = await response.json();
        return Number(data.currentArBalance || 0);
    }

    function renderReturnCustomerResults(customers) {
        if (!returnCustomerResults) {
            return;
        }

        returnCustomerResults.innerHTML = "";
        uniqueCustomersByName(customers).forEach((customer) => {
            const row = document.createElement("button");
            row.type = "button";
            row.className = "customer-result-row";
            row.innerHTML = `<span>${customer.name}</span><small>${customer.phone || "기존 고객"}</small>`;
            row.addEventListener("click", () => selectReturnCustomer(customer));
            returnCustomerResults.appendChild(row);
        });

        if (!returnCustomerResults.children.length) {
            returnCustomerResults.innerHTML = `<button type="button" class="customer-result-row"><span>검색된 고객 없음</span></button>`;
        }
        returnCustomerResults.hidden = false;
    }

    async function selectReturnCustomer(customer) {
        state.returnCustomer = customer;
        state.selectedReturnOrder = null;
        if (returnCustomerSearch) {
            returnCustomerSearch.value = customer.name;
        }
        if (returnCustomerResults) {
            returnCustomerResults.hidden = true;
            returnCustomerResults.innerHTML = "";
        }

        renderReturnItems(null);
        renderReturnSummary(0);
        try {
            const [balance, sales] = await Promise.all([
                fetchReturnCustomerBalance(customer.id),
                fetchReturnCreditSales(customer.id)
            ]);
            state.returnCurrentBalance = balance;
            state.returnSales = sales;
            renderReturnSalesOptions();
            renderReturnSummary(0);
        } catch (error) {
            state.returnSales = [];
            renderReturnSalesOptions(error.message);
        }
    }

    function renderReturnSalesOptions(errorMessage) {
        if (!returnSalesOrderSelect) {
            return;
        }

        returnSalesOrderSelect.innerHTML = `<option value="">외상판매 전표 선택</option>`;
        if (errorMessage) {
            const option = document.createElement("option");
            option.value = "";
            option.textContent = errorMessage;
            returnSalesOrderSelect.appendChild(option);
            returnSalesOrderSelect.disabled = true;
            return;
        }

        returnSalesOrderSelect.disabled = !state.returnCustomer;
        state.returnSales.forEach((sale) => {
            const option = document.createElement("option");
            option.value = String(sale.salesOrderId);
            option.textContent = `전표 ${sale.salesOrderId} · ${sale.salesDate || ""} · ${sale.representativeProductName || ""} · ${money(sale.totalAmount)}`;
            returnSalesOrderSelect.appendChild(option);
        });
    }

    function selectReturnOrder(salesOrderId) {
        state.selectedReturnOrder = state.returnSales.find((sale) => String(sale.salesOrderId) === String(salesOrderId)) || null;
        renderReturnItems(state.selectedReturnOrder);
        renderReturnSummary(calculateReturnTotal());
    }

    function renderReturnItems(order) {
        if (!returnItems) {
            return;
        }

        if (!order) {
            returnItems.innerHTML = `<div class="empty-table-state">전표를 선택하면 품목이 표시됩니다.</div>`;
            return;
        }

        const rows = order.items || [];
        if (!rows.length) {
            returnItems.innerHTML = `<div class="empty-table-state">반품할 품목이 없습니다.</div>`;
            return;
        }

        returnItems.innerHTML = "";
        rows.forEach((item) => {
            const soldQuantity = Number(item.quantity || 0);
            const returnedQuantity = Number(item.returnQuantity || 0);
            const availableQuantity = Math.max(soldQuantity - returnedQuantity, 0);
            const row = document.createElement("div");
            row.className = "return-item-row";
            row.innerHTML = `
                <span title="${item.productName || ""}">${item.productName || ""}</span>
                <span class="number">${money(soldQuantity)}</span>
                <span class="number">${money(returnedQuantity)}</span>
                <span class="number">${money(availableQuantity)}</span>
                <span class="number">${money(item.unitPrice)}</span>
                <span>
                    <input type="number" min="0" max="${availableQuantity}" value="0"
                           data-return-quantity-input="${item.salesOrderItemId}"
                           data-return-unit-price="${item.unitPrice || 0}">
                </span>
                <span class="number" data-return-line-amount="${item.salesOrderItemId}">0</span>
            `;
            returnItems.appendChild(row);
        });
    }

    function calculateReturnTotal() {
        if (!returnItems) {
            return 0;
        }

        return Array.from(returnItems.querySelectorAll("[data-return-quantity-input]"))
                .reduce((total, input) => {
                    const quantity = Math.max(numberValue(input.value), 0);
                    const max = numberValue(input.max);
                    const safeQuantity = max > 0 ? Math.min(quantity, max) : 0;
                    const unitPrice = numberValue(input.dataset.returnUnitPrice);
                    const lineAmount = safeQuantity * unitPrice;
                    const amountCell = returnItems.querySelector(`[data-return-line-amount="${input.dataset.returnQuantityInput}"]`);
                    if (amountCell) {
                        amountCell.textContent = money(lineAmount);
                    }
                    return total + lineAmount;
                }, 0);
    }

    function renderReturnSummary(returnAmount) {
        const expectedBalance = state.returnCurrentBalance - returnAmount;
        const settlementNeeded = expectedBalance < 0 ? Math.abs(expectedBalance) : 0;

        if (returnCurrentBalance) {
            returnCurrentBalance.textContent = money(state.returnCurrentBalance);
        }
        if (returnTotalAmount) {
            returnTotalAmount.textContent = money(returnAmount);
        }
        if (returnExpectedBalance) {
            returnExpectedBalance.textContent = money(expectedBalance);
        }
        if (returnSettlementNeeded) {
            returnSettlementNeeded.textContent = money(settlementNeeded);
        }
    }

    function handleReturnCustomerSearchInput() {
        const keyword = returnCustomerSearch.value.trim();
        state.returnCustomer = null;
        state.returnSales = [];
        state.selectedReturnOrder = null;
        state.returnCurrentBalance = 0;
        clearTimeout(state.returnSearchTimer);
        renderReturnSalesOptions();
        renderReturnItems(null);
        renderReturnSummary(0);

        if (!keyword) {
            returnCustomerResults.hidden = true;
            returnCustomerResults.innerHTML = "";
            return;
        }

        state.returnSearchTimer = setTimeout(async () => {
            try {
                renderReturnCustomerResults(await searchCustomers(keyword));
            } catch (error) {
                returnCustomerResults.innerHTML = `<button type="button" class="customer-result-row"><span>${error.message}</span></button>`;
                returnCustomerResults.hidden = false;
            }
        }, 180);
    }

    async function ensureCustomer() {
        if (state.selectedCustomer) {
            return state.selectedCustomer;
        }

        // 고객명을 비워 둔 POS 판매는 불특정 일반 소비자 판매로 본다.
        // 기존 고객 생성 흐름을 그대로 사용해서 "일반" 고객이 없으면 1회 생성하고, 있으면 재사용한다.
        const name = customerSearchInput.value.trim() || "일반";
        if (normalizeName(name) === normalizeName("일반")) {
            const generalCustomer = await ensureGeneralCustomer();
            selectCustomer(generalCustomer);
            return generalCustomer;
        }

        const customers = await searchCustomers(name);
        const exactCustomer = uniqueCustomersByName(customers)
                .find((customer) => normalizeName(customer.name) === normalizeName(name));
        if (exactCustomer) {
            selectCustomer(exactCustomer);
            return exactCustomer;
        }

        const customer = await createCustomer(name);
        selectCustomer(customer);
        return customer;
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

    function normalizeName(value) {
        return String(value || "").trim().replace(/\s+/g, "");
    }

    async function saveSale(paymentType) {
        saleSaveMessage.textContent = "";
        saleSaveMessage.className = "sale-save-message";

        if (!state.saleItems.length) {
            saleSaveMessage.textContent = "판매 품목을 먼저 선택하세요.";
            return;
        }

        try {
            const customer = await ensureCustomer();
            const payload = {
                customerId: customer.id,
                jobSiteId: state.selectedJobSite ? state.selectedJobSite.id : null,
                paymentType,
                taxPolicy: taxPolicy.checked ? "ADD_VAT" : "NO_TAX",
                memo: saleMemo.value.trim(),
                priceApplyPolicy: pricePolicy.checked ? "ONE_TIME_ONLY" : "SAVE_PRICE",
                items: state.saleItems.map((item) => ({
                    productId: item.id,
                    quantity: item.quantity,
                    unitPrice: Number(item.salePrice || 0)
                }))
            };

            const response = await fetch("/api/sales-orders", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const text = await response.text();
                throw new Error(text || "판매 저장에 실패했습니다.");
            }

            const saved = await response.json();
            saleSaveMessage.textContent = `판매 저장 완료: 전표 ${saved.salesOrderId}, 합계 ${money(saved.totalAmount)}원`;
            saleSaveMessage.className = "sale-save-message success";
            resetSaleFormAfterSave();
            productSearchInput.focus();
        } catch (error) {
            saleSaveMessage.textContent = error.message;
        }
    }

    function showJobSiteSuggestions() {
        if (!state.selectedCustomer || jobSiteResults.hidden === false) {
            return;
        }

        clearTimeout(state.jobSiteSearchTimer);
        state.jobSiteSearchTimer = setTimeout(async () => {
            try {
                const jobSites = await searchJobSites(jobSiteSearchInput.value.trim());
                renderJobSiteResults(jobSites, jobSiteSearchInput.value.trim());
            } catch (error) {
                jobSiteResults.innerHTML = `<button type="button" class="customer-result-row" disabled><span>${error.message}</span></button>`;
                jobSiteResults.hidden = false;
            }
        }, 80);
    }

    async function handleProductSearchInput() {
        const keyword = productSearchInput.value.trim();
        clearTimeout(state.searchTimer);

        if (!keyword) {
            renderProductResults([]);
            productResults.querySelector(".product-result-empty").textContent = "상품코드나 품명을 입력하면 검색됩니다.";
            return;
        }

        state.searchTimer = setTimeout(async () => {
            try {
                const products = await searchProducts(keyword);
                renderProductResults(products);
            } catch (error) {
                renderProductSearchError(error.message);
            }
        }, 180);
    }

    function renderProductSearchError(message) {
        productResults.innerHTML = `
            <div class="product-result-header">
                <span>품번</span><span>품명</span><span>규격</span><span>단위</span><span>재고</span><span>판매단가</span>
            </div>
            <div class="product-result-empty">${message}</div>
        `;
    }

    async function openProductPicker(alias, category) {
        pickerAlias.textContent = alias;
        pickerSearchInput.value = alias;
        pickerModal.hidden = false;
        await renderPickerResults(alias, category);
        pickerSearchInput.focus();
    }

    async function renderPickerResults(keyword, category) {
        pickerResults.innerHTML = `<div class="modal-empty">상품을 찾는 중입니다.</div>`;
        try {
            const products = await searchProducts(keyword, category);
            pickerResults.innerHTML = "";

            if (!products.length) {
                pickerResults.innerHTML = `<div class="modal-empty">매칭되는 실제 상품이 없습니다. 검색어를 바꿔보세요.</div>`;
                return;
            }

            products.forEach((product) => {
                const row = document.createElement("button");
                row.type = "button";
                row.className = "picker-product-row";
                row.innerHTML = productCells(product);
                row.addEventListener("click", () => {
                    addSaleItem(product);
                    closeProductPicker();
                });
                pickerResults.appendChild(row);
            });
        } catch (error) {
            pickerResults.innerHTML = `<div class="modal-empty">${error.message}</div>`;
        }
    }

    function closeProductPicker() {
        pickerModal.hidden = true;
    }

    function openCreateModal() {
        createMessage.textContent = "";
        createMessage.className = "form-message";
        createForm.reset();
        createForm.elements.category.value = state.activeName || "기타";
        createModal.hidden = false;
        createForm.elements.productName.focus();
    }

    function closeCreateModal() {
        createModal.hidden = true;
    }

    async function createProduct(event) {
        event.preventDefault();
        createMessage.textContent = "";
        createMessage.className = "form-message";

        const formData = new FormData(createForm);
        const payload = {
            code: formData.get("code"),
            productName: formData.get("productName"),
            productNickname: formData.get("productNickname"),
            variant: formData.get("variant"),
            unit: formData.get("unit"),
            brand: formData.get("brand"),
            category: formData.get("category"),
            costPrice: numberValue(formData.get("costPrice")),
            salePrice: numberValue(formData.get("salePrice")),
            stockQuantity: numberValue(formData.get("stockQuantity"))
        };

        try {
            const response = await fetch("/api/products", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const text = await response.text();
                throw new Error(text || "상품 등록에 실패했습니다.");
            }

            const product = await response.json();
            createMessage.textContent = "상품이 등록되었습니다. 판매 목록에 추가했습니다.";
            createMessage.className = "form-message success";
            addSaleItem(product);
            productSearchInput.value = product.productName;
            renderProductResults([product]);
        } catch (error) {
            createMessage.textContent = error.message;
        }
    }

    function numberValue(value) {
        const number = Number(value);
        return Number.isFinite(number) ? number : 0;
    }

    categoryForm.addEventListener("submit", (event) => {
        event.preventDefault();
        const input = categoryForm.elements.categoryName;
        const categoryName = input.value.trim();
        if (!categoryName) {
            return;
        }

        const exists = state.groups.some((group) => group.name === categoryName);
        if (!exists) {
            state.groups.push({ name: categoryName, items: [] });
        }
        state.activeName = categoryName;
        input.value = "";
        saveGroups();
        renderFavorites();
        renderFavoriteModal();
    });

    itemForm.addEventListener("submit", (event) => {
        event.preventDefault();
        const input = itemForm.elements.itemName;
        const itemName = input.value.trim();
        if (!itemName) {
            return;
        }

        const activeGroup = getActiveGroup();
        activeGroup.items.push(itemName);
        input.value = "";
        saveGroups();
        renderFavorites();
        renderFavoriteModal();
    });

    document.querySelector("[data-favorite-register]").addEventListener("click", openFavoriteModal);
    document.querySelectorAll("[data-favorite-close]").forEach((button) => {
        button.addEventListener("click", closeFavoriteModal);
    });
    document.querySelector("[data-favorite-reset]").addEventListener("click", () => {
        state.groups = structuredClone(defaults);
        state.activeName = "실리콘";
        saveGroups();
        renderFavorites();
        renderFavoriteModal();
    });
    favoriteModal.addEventListener("click", (event) => {
        if (event.target === favoriteModal) {
            closeFavoriteModal();
        }
    });

    productSearchInput.addEventListener("input", handleProductSearchInput);
    saleLines.addEventListener("click", (event) => {
        const minusId = event.target.dataset.qtyMinus;
        const plusId = event.target.dataset.qtyPlus;
        const removeId = event.target.dataset.removeItem;
        const unitPriceId = event.target.dataset.unitPrice;
        const qtyInputId = event.target.dataset.qtyInput;

        if (minusId) {
            changeQuantity(minusId, -1);
        }
        if (plusId) {
            changeQuantity(plusId, 1);
        }
        if (unitPriceId) {
            event.target.select();
        }
        if (qtyInputId) {
            event.target.select();
        }
        if (removeId) {
            removeSaleItem(removeId);
        }
    });
    saleLines.addEventListener("change", (event) => {
        const unitPriceId = event.target.dataset.unitPrice;
        const qtyInputId = event.target.dataset.qtyInput;
        if (unitPriceId) {
            changeUnitPrice(unitPriceId, event.target.value);
        }
        if (qtyInputId) {
            setQuantity(qtyInputId, event.target.value);
        }
    });
    saleLines.addEventListener("keydown", (event) => {
        const unitPriceId = event.target.dataset.unitPrice;
        const qtyInputId = event.target.dataset.qtyInput;
        if (unitPriceId && event.key === "Enter") {
            event.preventDefault();
            changeUnitPrice(unitPriceId, event.target.value);
            productSearchInput.focus();
        }
        if (qtyInputId && event.key === "Enter") {
            event.preventDefault();
            setQuantity(qtyInputId, event.target.value);
            productSearchInput.focus();
        }
    });
    clearSaleButton.addEventListener("click", clearSaleItems);
    customerSearchInput.addEventListener("input", handleCustomerSearchInput);
    customerSearchInput.addEventListener("keydown", async (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            const keyword = customerSearchInput.value.trim();
            if (keyword && !state.selectedCustomer) {
                const customer = await createCustomer(keyword);
                selectCustomer(customer);
            }
        }
    });
    customerClear.addEventListener("click", clearCustomer);
    jobSiteSearchInput.addEventListener("input", handleJobSiteSearchInput);
    jobSiteSearchInput.addEventListener("keydown", async (event) => {
        if (event.key === "ArrowDown") {
            event.preventDefault();
            if (jobSiteResults.hidden) {
                showJobSiteSuggestions();
                return;
            }
            setActiveJobSiteRow(state.jobSiteActiveIndex + 1);
        }
        if (event.key === "ArrowUp") {
            event.preventDefault();
            setActiveJobSiteRow(state.jobSiteActiveIndex - 1);
        }
        if (event.key === "Escape") {
            jobSiteResults.hidden = true;
            state.jobSiteActiveIndex = -1;
        }
        if (event.key === "Enter") {
            event.preventDefault();
            if (!jobSiteResults.hidden && activateJobSiteRow()) {
                return;
            }
            const keyword = jobSiteSearchInput.value.trim();
            if (keyword && !state.selectedJobSite) {
                const jobSite = await createJobSite(keyword);
                selectJobSite(jobSite);
            }
        }
    });
    jobSiteSearchInput.addEventListener("focus", showJobSiteSuggestions);
    jobSiteClear.addEventListener("click", clearJobSite);
    document.addEventListener("click", (event) => {
        if (!customerResults.contains(event.target) && event.target !== customerSearchInput) {
            customerResults.hidden = true;
        }
        if (!jobSiteResults.contains(event.target) && event.target !== jobSiteSearchInput) {
            jobSiteResults.hidden = true;
        }
    });

    pickerSearchButton.addEventListener("click", () => renderPickerResults(pickerSearchInput.value.trim(), state.activeName));
    pickerSearchInput.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            renderPickerResults(pickerSearchInput.value.trim(), state.activeName);
        }
    });
    document.querySelectorAll("[data-product-picker-close]").forEach((button) => {
        button.addEventListener("click", closeProductPicker);
    });
    pickerModal.addEventListener("click", (event) => {
        if (event.target === pickerModal) {
            closeProductPicker();
        }
    });

    returnOpenButton?.addEventListener("click", openReturnModal);
    returnCloseButtons.forEach((button) => {
        button.addEventListener("click", closeReturnModal);
    });
    returnModal?.addEventListener("click", (event) => {
        if (event.target === returnModal) {
            closeReturnModal();
        }
    });
    returnCustomerSearch?.addEventListener("input", handleReturnCustomerSearchInput);
    returnSalesOrderSelect?.addEventListener("change", (event) => {
        selectReturnOrder(event.target.value);
    });
    returnItems?.addEventListener("input", (event) => {
        if (event.target.dataset.returnQuantityInput) {
            renderReturnSummary(calculateReturnTotal());
        }
    });

    document.querySelector("[data-product-create-open]").addEventListener("click", openCreateModal);
    document.querySelectorAll("[data-product-create-close]").forEach((button) => {
        button.addEventListener("click", closeCreateModal);
    });
    createModal.addEventListener("click", (event) => {
        if (event.target === createModal) {
            closeCreateModal();
        }
    });
    createForm.addEventListener("submit", createProduct);
    document.querySelectorAll("[data-save-sale]").forEach((button) => {
        button.addEventListener("click", () => saveSale(button.dataset.saveSale));
    });
    document.querySelector("[data-open-ar-management]").addEventListener("click", () => {
        window.location.href = "/ar-management";
    });
    document.querySelector("[data-open-sales-management]").addEventListener("click", () => {
        window.open("/sales-management", "_blank", "noopener");
    });

    renderFavorites();
    renderSaleLines();
    renderProductResults([]);
})();
