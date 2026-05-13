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
        searchTimer: null,
        customerSearchTimer: null
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
    const taxPolicy = document.querySelector("[data-tax-policy]");
    const pricePolicy = document.querySelector("[data-price-policy]");
    const saleMemo = document.querySelector("[data-sale-memo]");
    const saleSaveMessage = document.querySelector("[data-sale-save-message]");
    const summaryCount = document.querySelector("[data-summary-count]");
    const summaryQuantity = document.querySelector("[data-summary-quantity]");
    const summaryTotal = document.querySelector("[data-summary-total]");
    const summaryDiscount = document.querySelector("[data-summary-discount]");

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

        const response = await fetch(`/api/products?${params.toString()}`);
        if (!response.ok) {
            throw new Error("상품 검색에 실패했습니다.");
        }
        return response.json();
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
            row.innerHTML = `
                <span title="${item.productName}">${item.productName}</span>
                <span></span>
                <span>${item.unit || item.variant || ""}</span>
                <span class="number">${money(item.salePrice)}</span>
                <span class="number">${item.quantity}</span>
                <span class="number">${money(amount)}</span>
                <span class="number">0</span>
                <span class="number">0</span>
                <span></span>
                <span></span>
                <span>${item.code || ""}</span>
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

    function selectCustomer(customer) {
        state.selectedCustomer = customer;
        customerSearchInput.value = customer.name;
        customerResults.hidden = true;
        customerResults.innerHTML = "";
    }

    function renderCustomerResults(customers, keyword) {
        customerResults.innerHTML = "";

        customers.forEach((customer) => {
            const row = document.createElement("button");
            row.type = "button";
            row.className = "customer-result-row";
            row.innerHTML = `<span>${customer.name}</span><small>${customer.phone || ""}</small>`;
            row.addEventListener("click", () => selectCustomer(customer));
            customerResults.appendChild(row);
        });

        if (keyword) {
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

    async function ensureCustomer() {
        if (state.selectedCustomer) {
            return state.selectedCustomer;
        }

        const name = customerSearchInput.value.trim();
        if (!name) {
            throw new Error("고객명을 입력하거나 선택하세요.");
        }

        const customer = await createCustomer(name);
        selectCustomer(customer);
        return customer;
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
                jobSiteId: null,
                paymentType,
                taxPolicy: taxPolicy.value,
                memo: saleMemo.value.trim(),
                priceApplyPolicy: pricePolicy.value,
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
            state.saleItems = [];
            saleMemo.value = "";
            renderSaleLines();
        } catch (error) {
            saleSaveMessage.textContent = error.message;
        }
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
    document.addEventListener("click", (event) => {
        if (!customerResults.contains(event.target) && event.target !== customerSearchInput) {
            customerResults.hidden = true;
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

    renderFavorites();
    renderSaleLines();
    renderProductResults([]);
})();
