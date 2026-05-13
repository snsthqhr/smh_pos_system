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
        searchTimer: null
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
    }

    function addSaleItem(product) {
        state.saleItems.push({ ...product, quantity: 1 });
        renderSaleLines();
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
        createForm.elements.code.focus();
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

    renderFavorites();
    renderSaleLines();
    renderProductResults([]);
})();
