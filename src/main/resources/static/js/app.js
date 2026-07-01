document.addEventListener("submit", function (event) {
    var form = event.target;
    if (!(form instanceof HTMLFormElement)) {
        return;
    }

    if (form.dataset.swalBypass === "true") {
        return;
    }

    var config = resolveConfirmationConfig(form);
    if (!config) {
        return;
    }

    event.preventDefault();

    if (window.Swal && typeof window.Swal.fire === "function") {
        window.Swal.fire({
            title: config.title,
            text: config.text,
            icon: config.icon || "warning",
            showCancelButton: true,
            confirmButtonText: config.confirmButtonText || "Continue",
            cancelButtonText: config.cancelButtonText || "Cancel",
            reverseButtons: true,
            focusCancel: true,
            confirmButtonColor: "#0f7f34",
            cancelButtonColor: "#8ca095"
        }).then(function (result) {
            if (result.isConfirmed) {
                submitConfirmedForm(form);
            }
        });
        return;
    }

    if (window.confirm(config.title + "\n\n" + config.text)) {
        submitConfirmedForm(form);
    }
}, true);

function submitConfirmedForm(form) {
    form.dataset.swalBypass = "true";
    HTMLFormElement.prototype.submit.call(form);
}

function resolveConfirmationConfig(form) {
    if (form.dataset.confirmTitle || form.dataset.confirmText) {
        return {
            title: form.dataset.confirmTitle || "Are you sure?",
            text: form.dataset.confirmText || "Please confirm this action before continuing.",
            icon: form.dataset.confirmIcon || "warning",
            confirmButtonText: form.dataset.confirmButtonText || "Yes, continue",
            cancelButtonText: form.dataset.confirmCancelText || "Cancel"
        };
    }

    var action = (form.getAttribute("action") || "").toLowerCase();

    if (action.indexOf("/logout") !== -1) {
        return {
            title: "Log out now?",
            text: "Your current session will end and you will need to sign in again to continue.",
            icon: "question",
            confirmButtonText: "Yes, log out",
            cancelButtonText: "Stay signed in"
        };
    }

    if (action.indexOf("/delete") !== -1) {
        return {
            title: "Delete this record?",
            text: "This action removes the selected data and should only be done if you are sure.",
            icon: "warning",
            confirmButtonText: "Yes, delete it",
            cancelButtonText: "Keep it"
        };
    }

    if (action.indexOf("/cancel") !== -1) {
        return {
            title: "Cancel this item?",
            text: "This will stop the current reservation or pending action.",
            icon: "warning",
            confirmButtonText: "Yes, cancel it",
            cancelButtonText: "Go back"
        };
    }

    if (action.indexOf("/return") !== -1) {
        return {
            title: "Mark this book as returned?",
            text: "The circulation record, availability, and any related queue updates will be applied.",
            icon: "question",
            confirmButtonText: "Yes, mark returned",
            cancelButtonText: "Not yet"
        };
    }

    if (action.indexOf("/password") !== -1) {
        return {
            title: "Proceed with password action?",
            text: "Please confirm before changing or resetting the password.",
            icon: "warning",
            confirmButtonText: "Yes, continue",
            cancelButtonText: "Review first"
        };
    }

    return null;
}

window.LatteAndLettersAddress = (function () {
    function normalizeLookupValue(value) {
        return (value || "")
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .replace(/[^a-z0-9]+/g, " ")
            .trim();
    }

    function clearSelectOptions(select) {
        while (select.firstChild) {
            select.removeChild(select.firstChild);
        }
    }

    function setBarangaySelectState(barangay, placeholderText, disabled) {
        clearSelectOptions(barangay);

        var placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = placeholderText;
        placeholder.selected = true;
        barangay.appendChild(placeholder);
        barangay.disabled = !!disabled;
    }

    function populateBarangayOptions(barangay, items, selectedBarangay) {
        clearSelectOptions(barangay);

        var placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = "Select barangay";
        barangay.appendChild(placeholder);

        var selectedLookupValue = normalizeLookupValue(selectedBarangay);
        var matched = false;

        (items || []).forEach(function (item) {
            var option = document.createElement("option");
            option.value = item;
            option.textContent = item;
            if (selectedLookupValue && normalizeLookupValue(item) === selectedLookupValue) {
                option.selected = true;
                matched = true;
            }
            barangay.appendChild(option);
        });

        if (!matched) {
            placeholder.selected = true;
        }

        barangay.disabled = false;
    }

    function initForm(config) {
        if (!config) {
            return;
        }

        var cityMunicipality = config.cityMunicipality;
        var barangay = config.barangay;
        var zipcode = config.zipcode;
        var cityZipCodes = config.cityZipCodes || {};
        var endpoint = config.endpoint;

        if (!cityMunicipality || !barangay || !zipcode || !endpoint) {
            return;
        }
        if (cityMunicipality.dataset.addressBound === "true") {
            return;
        }
        cityMunicipality.dataset.addressBound = "true";

        var barangayRequestToken = 0;
        var initialBarangay = barangay.dataset.selectedBarangay || "";

        function syncZipCode() {
            zipcode.value = cityZipCodes[cityMunicipality.value || ""] || "";
        }

        function loadBarangays(selectedBarangay) {
            syncZipCode();

            if (!cityMunicipality.value) {
                setBarangaySelectState(barangay, "Select city / municipality first", true);
                return Promise.resolve(false);
            }

            var requestToken = ++barangayRequestToken;
            setBarangaySelectState(barangay, "Loading barangays...", true);

            return fetch(endpoint + "?cityMunicipality=" + encodeURIComponent(cityMunicipality.value), {
                headers: {
                    "Accept": "application/json"
                }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("Unable to load barangays.");
                    }
                    return response.json();
                })
                .then(function (items) {
                    if (requestToken !== barangayRequestToken) {
                        return false;
                    }
                    populateBarangayOptions(barangay, items, selectedBarangay || "");
                    return true;
                })
                .catch(function () {
                    if (requestToken !== barangayRequestToken) {
                        return false;
                    }
                    setBarangaySelectState(barangay, "Unable to load barangays", true);
                    return false;
                });
        }

        cityMunicipality.addEventListener("change", function () {
            barangay.dataset.selectedBarangay = "";
            loadBarangays("");
        });

        loadBarangays(initialBarangay);
    }

    return {
        initForm: initForm
    };
})();

(function () {
    var titleSelectors = [
        ".brand-title",
        ".section-title",
        ".hero-side-title",
        ".metric-label",
        ".info-tile-label",
        ".chart-summary-label",
        ".insight-panel-title",
        ".modal-kicker",
        ".shell-brand-kicker",
        ".shell-brand-title",
        ".shell-panel-kicker",
        ".shell-panel-title",
        ".auth-panel-title",
        ".catalog-title",
        ".profile-hero-title",
        ".dashboard-tab-button span",
        ".support-item strong",
        ".form-label",
        ".hero-card h1",
        ".hero-card h2",
        ".hero-card h3",
        "thead th"
    ];

    var minorWords = {
        a: true,
        an: true,
        and: true,
        as: true,
        at: true,
        but: true,
        by: true,
        for: true,
        in: true,
        nor: true,
        of: true,
        on: true,
        or: true,
        per: true,
        the: true,
        to: true,
        with: true
    };

    function preserveAcronym(word) {
        return /^[A-Z0-9]{2,4}$/.test(word);
    }

    function formatWord(word, isBoundaryWord) {
        if (!/[A-Za-z]/.test(word)) {
            return word;
        }

        if (preserveAcronym(word)) {
            return word;
        }

        var match = word.match(/^([^A-Za-z0-9]*)(.*?)([^A-Za-z0-9]*)$/);
        if (!match) {
            return word;
        }

        var leading = match[1] || "";
        var core = match[2] || "";
        var trailing = match[3] || "";

        if (!core) {
            return word;
        }

        var transformedCore = core.split(/([/-])/).map(function (part) {
            if (part === "/" || part === "-") {
                return part;
            }

            if (preserveAcronym(part)) {
                return part;
            }

            var lowerPart = part.toLowerCase();
            if (!isBoundaryWord && minorWords[lowerPart]) {
                return lowerPart;
            }

            return lowerPart.charAt(0).toUpperCase() + lowerPart.slice(1);
        }).join("");

        return leading + transformedCore + trailing;
    }

    function toDisplayTitleCase(text) {
        if (!text || !text.trim()) {
            return text;
        }

        var parts = text.split(/(\s+)/);
        var wordIndexes = [];

        parts.forEach(function (part, index) {
            if (part && part.trim() && /[A-Za-z]/.test(part)) {
                wordIndexes.push(index);
            }
        });

        if (!wordIndexes.length) {
            return text;
        }

        var firstWordIndex = wordIndexes[0];
        var lastWordIndex = wordIndexes[wordIndexes.length - 1];

        return parts.map(function (part, index) {
            if (!part || /^\s+$/.test(part)) {
                return part;
            }

            return formatWord(part, index === firstWordIndex || index === lastWordIndex);
        }).join("");
    }

    document.querySelectorAll(titleSelectors.join(",")).forEach(function (node) {
        if (node.hasAttribute("data-title-case-skip")) {
            return;
        }

        var rawText = node.textContent;
        if (!rawText || !rawText.trim()) {
            return;
        }

        node.textContent = toDisplayTitleCase(rawText);
    });
})();

(function () {
    var monthPattern = "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)";
    var dateTimePattern = new RegExp("^(" + monthPattern + "\\s+\\d{1,2},\\s+\\d{4})\\s+(\\d{1,2}:\\d{2}\\s*[AP]M)$", "i");

    function hasRenderableTextOnly(cell) {
        return Array.prototype.every.call(cell.childNodes, function (node) {
            if (node.nodeType === Node.TEXT_NODE) {
                return true;
            }
            return node.nodeType === Node.ELEMENT_NODE && node.tagName === "BR";
        });
    }

    document.querySelectorAll("table tbody td").forEach(function (cell) {
        if (!cell || cell.querySelector(".table-datetime")) {
            return;
        }

        if (!hasRenderableTextOnly(cell)) {
            return;
        }

        var rawText = (cell.textContent || "").replace(/\s+/g, " ").trim();
        if (!rawText) {
            return;
        }

        var match = rawText.match(dateTimePattern);
        if (!match) {
            return;
        }

        var stack = document.createElement("span");
        stack.className = "table-datetime";

        var dateLine = document.createElement("span");
        dateLine.className = "table-datetime-date";
        dateLine.textContent = match[1];

        var timeLine = document.createElement("span");
        timeLine.className = "table-datetime-time";
        timeLine.textContent = match[2].replace(/\s+/g, " ").trim();

        stack.appendChild(dateLine);
        stack.appendChild(timeLine);
        cell.textContent = "";
        cell.appendChild(stack);
    });
})();

(function () {
    var sections = document.querySelectorAll("[data-table-search-section]");
    if (!sections.length) {
        return;
    }

    function clearMarks(container) {
        container.querySelectorAll("mark.table-search-mark").forEach(function (mark) {
            var parent = mark.parentNode;
            if (!parent) {
                return;
            }
            parent.replaceChild(document.createTextNode(mark.textContent), mark);
            parent.normalize();
        });
    }

    function highlightElement(container, query) {
        if (!container || !query) {
            return;
        }

        var lowerQuery = query.toLowerCase();
        var walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT, {
            acceptNode: function (node) {
                if (!node.nodeValue || !node.nodeValue.trim()) {
                    return NodeFilter.FILTER_REJECT;
                }
                if (node.parentElement && node.parentElement.closest("mark.table-search-mark")) {
                    return NodeFilter.FILTER_REJECT;
                }
                if (node.parentElement && /^(SCRIPT|STYLE|NOSCRIPT)$/i.test(node.parentElement.tagName)) {
                    return NodeFilter.FILTER_REJECT;
                }
                return NodeFilter.FILTER_ACCEPT;
            }
        });

        var textNodes = [];
        var currentNode = walker.nextNode();
        while (currentNode) {
            textNodes.push(currentNode);
            currentNode = walker.nextNode();
        }

        textNodes.forEach(function (textNode) {
            var value = textNode.nodeValue;
            var lowerValue = value.toLowerCase();
            var matchIndex = lowerValue.indexOf(lowerQuery);
            if (matchIndex === -1) {
                return;
            }

            var fragment = document.createDocumentFragment();
            var cursor = 0;

            while (matchIndex !== -1) {
                if (matchIndex > cursor) {
                    fragment.appendChild(document.createTextNode(value.slice(cursor, matchIndex)));
                }

                var mark = document.createElement("mark");
                mark.className = "table-search-mark";
                mark.textContent = value.slice(matchIndex, matchIndex + query.length);
                fragment.appendChild(mark);

                cursor = matchIndex + query.length;
                matchIndex = lowerValue.indexOf(lowerQuery, cursor);
            }

            if (cursor < value.length) {
                fragment.appendChild(document.createTextNode(value.slice(cursor)));
            }

            if (textNode.parentNode) {
                textNode.parentNode.replaceChild(fragment, textNode);
            }
        });
    }

    sections.forEach(function (section) {
        var input = section.querySelector("[data-table-search-input]");
        var table = section.querySelector("[data-table-search-table]");
        var inputShell = input ? input.closest(".table-search-shell") : null;
        var tbody = table ? table.tBodies[0] : null;

        if (!input || !table || !tbody) {
            return;
        }

        var allRows = Array.prototype.slice.call(tbody.querySelectorAll("tr"));
        var dataRows = allRows.filter(function (row) {
            return !(row.cells.length === 1 && row.cells[0].hasAttribute("colspan"));
        });

        if (!dataRows.length) {
            input.disabled = true;
            if (inputShell) {
                inputShell.hidden = true;
            }
            return;
        }

        if (inputShell) {
            inputShell.hidden = false;
        }

        var emptyMessage = section.getAttribute("data-table-search-empty") || "No matches found on this page.";
        var emptyRow = document.createElement("tr");
        emptyRow.className = "table-search-empty-row";
        emptyRow.hidden = true;

        var emptyCell = document.createElement("td");
        emptyCell.colSpan = table.querySelectorAll("thead th").length || 1;
        emptyCell.textContent = emptyMessage;
        emptyRow.appendChild(emptyCell);
        tbody.appendChild(emptyRow);

        function applySearch() {
            var query = (input.value || "").trim();
            var lowerQuery = query.toLowerCase();
            var visibleCount = 0;

            dataRows.forEach(function (row) {
                row.classList.remove("table-search-row-match");
                Array.prototype.forEach.call(row.cells, function (cell) {
                    clearMarks(cell);
                });

                if (!lowerQuery) {
                    row.hidden = false;
                    visibleCount++;
                    return;
                }

                var rowText = (row.textContent || "").toLowerCase();
                var isMatch = rowText.indexOf(lowerQuery) !== -1;
                row.hidden = !isMatch;

                if (isMatch) {
                    row.classList.add("table-search-row-match");
                    Array.prototype.forEach.call(row.cells, function (cell) {
                        highlightElement(cell, query);
                    });
                    visibleCount++;
                }
            });

            emptyRow.hidden = visibleCount !== 0 || !lowerQuery;
        }

        input.addEventListener("input", applySearch);
    });
})();

(function () {
    var pageShell = document.querySelector(".page-shell");
    var sidebar = pageShell ? pageShell.querySelector(".app-nav") : null;
    var assetBaseUrl = resolveAppBaseUrl();

    if (!pageShell || !sidebar || sidebar.dataset.shellEnhanced === "true") {
        return;
    }
    sidebar.dataset.shellEnhanced = "true";

    var body = document.body;
    var navBrand = sidebar.querySelector(":scope > div:first-child");
    var navLinks = sidebar.querySelector(".nav-links");
    var profileLink = navLinks ? navLinks.querySelector('a[href*="/profile"]') : null;
    var logoutForm = navLinks ? navLinks.querySelector('form[action*="/logout"]') : null;
    var mobileQuery = window.matchMedia("(max-width: 1100px)");
    var collapseStorageKey = "latteandletters.shell.nav.collapsed";

    if (!navLinks || !profileLink || !logoutForm) {
        return;
    }

    if (navBrand) {
        navBrand.classList.add("app-nav-brand");
    }
    normalizeNavigationOrder(navLinks);
    enhanceNavigationLinks(navLinks);

    var shellMeta = buildShellMeta(navBrand, profileLink, navLinks, logoutForm);
    var topbar = buildTopbar(shellMeta);
    var navBackdrop = document.createElement("button");
    navBackdrop.type = "button";
    navBackdrop.className = "shell-nav-backdrop";
    navBackdrop.setAttribute("aria-label", "Close navigation");

    body.insertBefore(topbar, body.firstChild);
    body.insertBefore(navBackdrop, topbar.nextSibling);

    var notificationButton = topbar.querySelector("[data-shell-toggle='notifications']");
    var notificationBadge = topbar.querySelector("[data-shell-notification-badge]");
    var accountButton = topbar.querySelector("[data-shell-toggle='account']");
    var toggleButton = topbar.querySelector("[data-shell-toggle='sidebar']");
    var notificationUi = buildUserNotificationUi(shellMeta, notificationBadge);
    var notificationPanel = notificationUi.panel;
    var accountPanel = buildAccountPanel(shellMeta, profileLink, logoutForm);

    body.appendChild(notificationPanel);
    if (notificationUi.modal) {
        body.appendChild(notificationUi.modal);
    }
    body.appendChild(accountPanel);

    function closePanels() {
        notificationPanel.hidden = true;
        accountPanel.hidden = true;
        notificationButton.setAttribute("aria-expanded", "false");
        accountButton.setAttribute("aria-expanded", "false");
    }

    function togglePanel(button, panel) {
        var shouldOpen = panel.hidden;
        closePanels();
        if (shouldOpen) {
            panel.hidden = false;
            button.setAttribute("aria-expanded", "true");
        }
    }

    function syncSidebarState() {
        closePanels();
        if (mobileQuery.matches) {
            body.classList.remove("shell-nav-collapsed");
            body.classList.remove("shell-nav-open");
            return;
        }

        body.classList.remove("shell-nav-open");
        body.classList.toggle("shell-nav-collapsed", window.localStorage.getItem(collapseStorageKey) === "true");
    }

    toggleButton.addEventListener("click", function () {
        closePanels();

        if (mobileQuery.matches) {
            body.classList.toggle("shell-nav-open");
            return;
        }

        var shouldCollapse = !body.classList.contains("shell-nav-collapsed");
        body.classList.toggle("shell-nav-collapsed", shouldCollapse);
        window.localStorage.setItem(collapseStorageKey, shouldCollapse ? "true" : "false");
    });

    notificationButton.addEventListener("click", function (event) {
        event.stopPropagation();
        togglePanel(notificationButton, notificationPanel);
    });

    accountButton.addEventListener("click", function (event) {
        event.stopPropagation();
        togglePanel(accountButton, accountPanel);
    });

    navBackdrop.addEventListener("click", function () {
        body.classList.remove("shell-nav-open");
        closePanels();
    });

    document.addEventListener("click", function (event) {
        if (!notificationPanel.hidden && !notificationPanel.contains(event.target) && !notificationButton.contains(event.target)) {
            notificationPanel.hidden = true;
            notificationButton.setAttribute("aria-expanded", "false");
        }

        if (!accountPanel.hidden && !accountPanel.contains(event.target) && !accountButton.contains(event.target)) {
            accountPanel.hidden = true;
            accountButton.setAttribute("aria-expanded", "false");
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            closePanels();
            body.classList.remove("shell-nav-open");
        }
    });

    Array.prototype.forEach.call(navLinks.querySelectorAll("a"), function (link) {
        link.addEventListener("click", function () {
            if (mobileQuery.matches) {
                body.classList.remove("shell-nav-open");
            }
            closePanels();
        });
    });

    if (typeof mobileQuery.addEventListener === "function") {
        mobileQuery.addEventListener("change", syncSidebarState);
    } else if (typeof mobileQuery.addListener === "function") {
        mobileQuery.addListener(syncSidebarState);
    }

    syncSidebarState();
    if (typeof notificationUi.refresh === "function") {
        notificationUi.refresh();
    }

    function buildShellMeta(brand, currentProfileLink, currentNavLinks, currentLogoutForm) {
        var kicker = brand ? getTextContent(brand.querySelector(".tag-chip")) : "";
        var title = brand ? getTextContent(brand.querySelector(".brand-title")) : "";
        var activeNavLink = currentNavLinks ? currentNavLinks.querySelector(".nav-pill.active .nav-pill-label, .nav-pill.active") : null;
        var isStudent = currentProfileLink.getAttribute("href").indexOf("/student/") !== -1;
        var csrfInput = currentLogoutForm ? currentLogoutForm.querySelector('input[type="hidden"]') : null;
        var baseHref = currentProfileLink.getAttribute("href").replace(/\/profile(?:#.*)?$/, "");

        return {
            kicker: kicker || (isStudent ? "Student Portal" : "Admin Console"),
            title: "Latte and Letters",
            accountLabel: isStudent ? "Student menu" : "Admin menu",
            accountSubtitle: "Profile, security, and session",
            accountInitial: isStudent ? "S" : "A",
            isStudent: isStudent,
            logoUrl: resolveAssetUrl("/assets/images/logo.png"),
            avatarUrl: isStudent ? currentProfileLink.getAttribute("href") + "/avatar?v=" + Date.now() : "",
            profileHref: currentProfileLink.getAttribute("href"),
            passwordHref: currentProfileLink.getAttribute("href"),
            passwordStateHref: baseHref + "/password/state",
            passwordRequestOtpHref: baseHref + "/password/request-otp",
            passwordResendOtpHref: baseHref + "/password/resend-otp",
            passwordVerifyOtpHref: baseHref + "/password/verify-otp",
            passwordUpdateHref: baseHref + "/password/update",
            notificationPanelHref: baseHref + "/notifications/panel",
            notificationHistoryHref: baseHref + "/notifications/history",
            notificationReadAllHref: baseHref + "/notifications/read-all",
            csrfParamName: csrfInput ? csrfInput.getAttribute("name") : "",
            csrfToken: csrfInput ? csrfInput.value : ""
        };
    }

    function buildTopbar(meta) {
        var topbarElement = document.createElement("header");
        topbarElement.className = "shell-topbar";

        var start = document.createElement("div");
        start.className = "shell-topbar-start";

        var toggle = document.createElement("button");
        toggle.type = "button";
        toggle.className = "shell-toggle-button";
        toggle.setAttribute("aria-label", "Toggle navigation");
        toggle.setAttribute("data-shell-toggle", "sidebar");
        toggle.innerHTML = getShellIcon("menu");

        var brand = document.createElement("div");
        brand.className = "shell-brand";

        var brandMark = document.createElement("span");
        brandMark.className = "shell-brand-mark";
        brandMark.setAttribute("aria-hidden", "true");
        if (meta.logoUrl) {
            var brandImage = document.createElement("img");
            brandImage.src = meta.logoUrl;
            brandImage.alt = "";
            brandMark.appendChild(brandImage);
        }

        var brandCopy = document.createElement("div");
        brandCopy.className = "shell-brand-copy";

        var brandKicker = document.createElement("span");
        brandKicker.className = "shell-brand-kicker";
        brandKicker.textContent = meta.kicker;

        var brandTitle = document.createElement("span");
        brandTitle.className = "shell-brand-title";
        brandTitle.textContent = meta.title;

        brandCopy.appendChild(brandKicker);
        brandCopy.appendChild(brandTitle);
        brand.appendChild(brandMark);
        brand.appendChild(brandCopy);
        start.appendChild(toggle);
        start.appendChild(brand);

        var actions = document.createElement("div");
        actions.className = "shell-topbar-actions";

        var notificationWrap = document.createElement("div");
        notificationWrap.className = "shell-icon-wrap";

        var notification = document.createElement("button");
        notification.type = "button";
        notification.className = "shell-icon-button";
        notification.setAttribute("aria-label", "Open notifications");
        notification.setAttribute("aria-expanded", "false");
        notification.setAttribute("data-shell-toggle", "notifications");
        notification.innerHTML = getShellIcon("bell");
        notificationWrap.appendChild(notification);

        var alertCount = pageShell.querySelectorAll(".alert").length;
        var badge = document.createElement("span");
        badge.className = "shell-alert-badge";
        badge.setAttribute("data-shell-notification-badge", "true");
        badge.hidden = true;
        if (meta.isStudent && alertCount > 0) {
            badge.textContent = alertCount > 9 ? "9+" : String(alertCount);
            badge.hidden = false;
        }
        notificationWrap.appendChild(badge);

        var account = document.createElement("button");
        account.type = "button";
        account.className = "shell-account-button";
        account.setAttribute("aria-label", "Open account menu");
        account.setAttribute("aria-expanded", "false");
        account.setAttribute("data-shell-toggle", "account");

        var avatar = document.createElement("span");
        avatar.className = "shell-account-avatar";
        avatar.textContent = meta.accountInitial;
        account.appendChild(avatar);

        var chevron = document.createElement("span");
        chevron.className = "nav-pill-icon";
        chevron.innerHTML = getShellIcon("chevronDown");
        account.appendChild(chevron);

        if (meta.isStudent && meta.avatarUrl) {
            hydrateAccountAvatar(avatar, meta.avatarUrl);
        }

        actions.appendChild(notificationWrap);
        actions.appendChild(account);
        topbarElement.appendChild(start);
        topbarElement.appendChild(actions);

        return topbarElement;
    }

    function buildNotificationPanel(alertNodes) {
        var panel = document.createElement("div");
        panel.className = "shell-panel";
        panel.hidden = true;

        panel.appendChild(createPanelHeader("Notifications", "Page updates and activity"));

        var list = document.createElement("div");
        list.className = "shell-notification-list";

        if (!alertNodes.length) {
            var emptyItem = document.createElement("div");
            emptyItem.className = "shell-notification-item shell-panel-item-muted";
            emptyItem.textContent = "You're all caught up. No new notifications right now.";
            list.appendChild(emptyItem);
        } else {
            Array.prototype.forEach.call(alertNodes, function (alertNode) {
                list.appendChild(createNotificationItem(alertNode));
            });
        }

        panel.appendChild(list);
        return panel;
    }

    function buildAccountPanel(meta, currentProfileLink, currentLogoutForm) {
        var panel = document.createElement("div");
        panel.className = "shell-panel";
        panel.hidden = true;

        panel.appendChild(createPanelHeader(meta.accountLabel, "Quick account actions"));

        var list = document.createElement("div");
        list.className = "shell-panel-list";
        list.appendChild(createActionLink("profile", "View profile", "Open your account details page.", meta.profileHref));
        if (meta.isStudent) {
            list.appendChild(createPasswordAction(meta));
        } else {
            list.appendChild(createActionLink("shield", "Change password", "Manage your password and account security.", meta.passwordHref));
        }
        list.appendChild(createLogoutAction(currentLogoutForm));

        panel.appendChild(list);
        return panel;
    }

    function buildUserNotificationUi(meta, badgeNode) {
        var panel = document.createElement("div");
        panel.className = "shell-panel shell-panel-wide";
        panel.hidden = true;
        panel.appendChild(createPanelHeader("Notifications", meta.isStudent ? "Recent account and borrowing updates" : "Recent student request activity"));

        var list = document.createElement("div");
        list.className = "shell-notification-list";
        panel.appendChild(list);

        var footer = document.createElement("div");
        footer.className = "shell-panel-footer";

        var markReadButton = document.createElement("button");
        markReadButton.type = "button";
        markReadButton.className = "shell-panel-inline-action";
        markReadButton.textContent = "Mark all read";

        var seeAllButton = document.createElement("button");
        seeAllButton.type = "button";
        seeAllButton.className = "shell-panel-inline-action shell-panel-inline-action-primary";
        seeAllButton.textContent = "See all notifications";

        footer.appendChild(markReadButton);
        footer.appendChild(seeAllButton);
        panel.appendChild(footer);

        var modal = createNotificationHistoryModal();
        var modalList = modal.querySelector("[data-notification-history-list]");
        var modalPagination = modal.querySelector("[data-notification-history-pagination]");
        var modalUnreadCount = modal.querySelector("[data-notification-history-unread]");
        var modalSummary = modal.querySelector("[data-notification-history-summary]");
        var modalMarkReadButton = modal.querySelector("[data-notification-history-read-all]");
        var modalCloseButtons = modal.querySelectorAll("[data-notification-history-close]");
        var currentHistoryPage = 1;

        function setBadge(unreadCount) {
            if (!badgeNode) {
                return;
            }
            if (!unreadCount || unreadCount < 1) {
                badgeNode.hidden = true;
                badgeNode.textContent = "";
                return;
            }
            badgeNode.hidden = false;
            badgeNode.textContent = unreadCount > 9 ? "9+" : String(unreadCount);
        }

        function createLoadingState(targetList, message) {
            targetList.innerHTML = "";
            var item = document.createElement("div");
            item.className = "shell-notification-item shell-panel-item-muted";
            item.textContent = message;
            targetList.appendChild(item);
        }

        function createAdminNotificationItem(notification, compact) {
            var item = document.createElement("a");
            item.className = compact ? "shell-notification-item" : "shell-notification-item shell-notification-item-full";
            item.href = assetBaseUrl.replace(/\/$/, "") + (notification.linkUrl || (meta.isStudent ? "/student/dashboard" : "/admin/dashboard"));

            item.appendChild(createPanelIcon("bell"));

            var copy = document.createElement("span");
            copy.className = "shell-panel-item-copy";

            var title = document.createElement("strong");
            title.textContent = notification.title || "Notification";

            var subtitle = document.createElement("span");
            subtitle.textContent = notification.message || "";

            var metaLine = document.createElement("span");
            metaLine.className = "shell-panel-item-meta";
            metaLine.textContent = (notification.notificationTypeLabel || "Update") + " | " + (notification.createdAtDisplay || "");

            copy.appendChild(title);
            copy.appendChild(subtitle);
            copy.appendChild(metaLine);
            item.appendChild(copy);

            if (!notification.read) {
                var state = document.createElement("span");
                state.className = "tag-chip";
                state.textContent = "New";
                item.appendChild(state);
            }

            return item;
        }

        function renderPanel(data) {
            list.innerHTML = "";
            var items = data && Array.isArray(data.items) ? data.items : [];
            var unreadCount = data && typeof data.unreadCount === "number" ? data.unreadCount : 0;

            setBadge(unreadCount);
            markReadButton.hidden = unreadCount < 1;

            if (!items.length) {
                createLoadingState(list, "You're all caught up. No new notifications right now.");
                return;
            }

            items.forEach(function (notification) {
                list.appendChild(createAdminNotificationItem(notification, true));
            });
        }

        function renderHistory(data) {
            modalList.innerHTML = "";
            modalPagination.innerHTML = "";

            var items = data && Array.isArray(data.items) ? data.items : [];
            var unreadCount = data && typeof data.unreadCount === "number" ? data.unreadCount : 0;
            var totalItems = data && typeof data.totalItems === "number" ? data.totalItems : 0;
            var totalPages = data && typeof data.totalPages === "number" ? data.totalPages : 1;
            var page = data && typeof data.page === "number" ? data.page : 1;

            modalUnreadCount.textContent = "Unread: " + unreadCount;
            modalSummary.textContent = totalItems + " notification" + (totalItems === 1 ? "" : "s");
            modalMarkReadButton.hidden = unreadCount < 1;

            if (!items.length) {
                createLoadingState(modalList, "No notification history yet.");
            } else {
                items.forEach(function (notification) {
                    modalList.appendChild(createAdminNotificationItem(notification, false));
                });
            }

            if (totalPages > 1) {
                if (data.hasPrevious) {
                    modalPagination.appendChild(createPaginationButton("Previous", data.previousPage));
                }

                for (var current = data.startPage; current <= data.endPage; current += 1) {
                    modalPagination.appendChild(createPaginationButton(String(current), current, current === page));
                }

                if (data.hasNext) {
                    modalPagination.appendChild(createPaginationButton("Next", data.nextPage));
                }
            }
        }

        function createPaginationButton(label, page, active) {
            var button = document.createElement("button");
            button.type = "button";
            button.className = active ? "shell-pagination-button is-active" : "shell-pagination-button";
            button.textContent = label;
            button.addEventListener("click", function () {
                loadHistory(page);
            });
            return button;
        }

        function loadPanel() {
            createLoadingState(list, "Loading notifications...");
            return fetch(meta.notificationPanelHref, {
                headers: {
                    "Accept": "application/json"
                }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("Unable to load notification panel.");
                    }
                    return response.json();
                })
                .then(function (data) {
                    renderPanel(data);
                })
                .catch(function () {
                    createLoadingState(list, "Unable to load notifications right now.");
                });
        }

        function loadHistory(page) {
            currentHistoryPage = page || 1;
            createLoadingState(modalList, "Loading notification history...");
            modalPagination.innerHTML = "";

            return fetch(meta.notificationHistoryHref + "?page=" + encodeURIComponent(currentHistoryPage), {
                headers: {
                    "Accept": "application/json"
                }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("Unable to load notification history.");
                    }
                    return response.json();
                })
                .then(function (data) {
                    renderHistory(data);
                })
                .catch(function () {
                    createLoadingState(modalList, "Unable to load notification history right now.");
                });
        }

        function markAllRead() {
            var csrfPayload = "";
            if (meta.csrfParamName && meta.csrfToken) {
                csrfPayload = encodeURIComponent(meta.csrfParamName) + "=" + encodeURIComponent(meta.csrfToken);
            }

            return fetch(meta.notificationReadAllHref, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                    "Accept": "application/json"
                },
                body: csrfPayload
            }).then(function (response) {
                if (!response.ok) {
                    throw new Error("Unable to mark notifications as read.");
                }
                return Promise.all([loadPanel(), loadHistory(currentHistoryPage)]);
            });
        }

        function openModal() {
            modal.hidden = false;
            document.body.classList.add("shell-modal-open");
            loadHistory(currentHistoryPage);
        }

        function closeModal() {
            modal.hidden = true;
            document.body.classList.remove("shell-modal-open");
        }

        markReadButton.addEventListener("click", function () {
            markAllRead();
        });

        modalMarkReadButton.addEventListener("click", function () {
            markAllRead();
        });

        seeAllButton.addEventListener("click", function () {
            panel.hidden = true;
            var shellToggleButton = document.querySelector("[data-shell-toggle='notifications']");
            if (shellToggleButton) {
                shellToggleButton.setAttribute("aria-expanded", "false");
            }
            openModal();
        });

        Array.prototype.forEach.call(modalCloseButtons, function (button) {
            button.addEventListener("click", closeModal);
        });

        modal.addEventListener("click", function (event) {
            if (event.target === modal) {
                closeModal();
            }
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && !modal.hidden) {
                closeModal();
            }
        });

        return {
            panel: panel,
            modal: modal,
            refresh: loadPanel
        };
    }

    function createPanelHeader(title, subtitle) {
        var header = document.createElement("div");
        header.className = "shell-panel-header";

        var kicker = document.createElement("span");
        kicker.className = "shell-panel-kicker";
        kicker.textContent = "Quick access";

        var heading = document.createElement("span");
        heading.className = "shell-panel-title";
        heading.textContent = title;

        var note = document.createElement("span");
        note.className = "shell-panel-subtitle";
        note.textContent = subtitle;

        header.appendChild(kicker);
        header.appendChild(heading);
        header.appendChild(note);
        return header;
    }

    function createNotificationHistoryModal() {
        var modal = document.createElement("div");
        modal.className = "shell-modal";
        modal.hidden = true;

        var card = document.createElement("div");
        card.className = "shell-modal-card";

        var header = document.createElement("div");
        header.className = "shell-modal-header";

        var copy = document.createElement("div");
        copy.className = "shell-modal-copy";

        var title = document.createElement("div");
        title.className = "shell-modal-title";
        title.textContent = "Notification history";

        var subtitle = document.createElement("div");
        subtitle.className = "shell-modal-subtitle";
        subtitle.textContent = "Recent account, borrowing, reservation, and return updates.";

        copy.appendChild(title);
        copy.appendChild(subtitle);

        var actions = document.createElement("div");
        actions.className = "shell-modal-actions";

        var unread = document.createElement("span");
        unread.className = "tag-chip";
        unread.setAttribute("data-notification-history-unread", "true");
        unread.textContent = "Unread: 0";

        var markRead = document.createElement("button");
        markRead.type = "button";
        markRead.className = "shell-panel-inline-action";
        markRead.setAttribute("data-notification-history-read-all", "true");
        markRead.textContent = "Mark all read";

        var close = document.createElement("button");
        close.type = "button";
        close.className = "shell-modal-close";
        close.setAttribute("aria-label", "Close notifications");
        close.setAttribute("data-notification-history-close", "true");
        close.innerHTML = "&times;";

        actions.appendChild(unread);
        actions.appendChild(markRead);
        actions.appendChild(close);
        header.appendChild(copy);
        header.appendChild(actions);

        var summary = document.createElement("div");
        summary.className = "shell-modal-summary";
        summary.setAttribute("data-notification-history-summary", "true");
        summary.textContent = "0 notifications";

        var list = document.createElement("div");
        list.className = "shell-notification-list shell-notification-history-list";
        list.setAttribute("data-notification-history-list", "true");

        var pagination = document.createElement("div");
        pagination.className = "shell-pagination";
        pagination.setAttribute("data-notification-history-pagination", "true");

        card.appendChild(header);
        card.appendChild(summary);
        card.appendChild(list);
        card.appendChild(pagination);
        modal.appendChild(card);
        return modal;
    }

    function createNotificationItem(alertNode) {
        var item = document.createElement("div");
        item.className = "shell-notification-item";

        var iconName = "bell";
        if (alertNode.classList.contains("alert-danger")) {
            iconName = "alert";
        } else if (alertNode.classList.contains("alert-success")) {
            iconName = "check";
        }

        item.appendChild(createPanelIcon(iconName));

        var copy = document.createElement("span");
        copy.className = "shell-panel-item-copy";

        var title = document.createElement("strong");
        title.textContent = alertNode.classList.contains("alert-danger")
                ? "Attention needed"
                : alertNode.classList.contains("alert-success")
                        ? "Success"
                        : "Update";

        var subtitle = document.createElement("span");
        subtitle.textContent = getTextContent(alertNode);

        copy.appendChild(title);
        copy.appendChild(subtitle);
        item.appendChild(copy);

        return item;
    }

    function createActionLink(iconName, title, subtitle, href) {
        var link = document.createElement("a");
        link.className = "shell-panel-item";
        link.href = href;
        link.appendChild(createPanelIcon(iconName));

        var copy = document.createElement("span");
        copy.className = "shell-panel-item-copy";

        var heading = document.createElement("strong");
        heading.textContent = title;

        var note = document.createElement("span");
        note.textContent = subtitle;

        copy.appendChild(heading);
        copy.appendChild(note);
        link.appendChild(copy);
        return link;
    }

    function createPasswordAction(meta) {
        var button = document.createElement("button");
        button.type = "button";
        button.className = "shell-panel-item shell-panel-button";
        button.appendChild(createPanelIcon("shield"));

        var copy = document.createElement("span");
        copy.className = "shell-panel-item-copy";

        var heading = document.createElement("strong");
        heading.textContent = "Change password";

        var note = document.createElement("span");
        note.textContent = "Verify with OTP before setting a new password.";

        copy.appendChild(heading);
        copy.appendChild(note);
        button.appendChild(copy);
        button.addEventListener("click", function () {
            openStudentPasswordModal(meta);
        });
        return button;
    }

    function openStudentPasswordModal(meta) {
        closePanels();

        if (!window.Swal || typeof window.Swal.fire !== "function") {
            window.location.href = meta.passwordHref;
            return;
        }

        var state = {
            hasPendingOtp: false,
            verified: false,
            maskedEmail: "",
            expiresAtEpochMs: null,
            resendAvailableAtEpochMs: null,
            message: "",
            messageType: "info"
        };
        var countdownTimer = null;

        function formatCountdown(targetEpochMs) {
            if (!targetEpochMs) {
                return "not active";
            }

            var remainingMs = targetEpochMs - Date.now();
            if (remainingMs <= 0) {
                return "00:00";
            }

            var totalSeconds = Math.floor(remainingMs / 1000);
            var minutes = Math.floor(totalSeconds / 60);
            var seconds = totalSeconds % 60;
            return String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");
        }

        function mergeState(nextState) {
            state.hasPendingOtp = !!nextState.hasPendingOtp;
            state.verified = !!nextState.verified;
            state.maskedEmail = nextState.maskedEmail || "";
            state.expiresAtEpochMs = nextState.expiresAtEpochMs || null;
            state.resendAvailableAtEpochMs = nextState.resendAvailableAtEpochMs || null;
        }

        function render() {
            var container = document.getElementById("studentPasswordFlow");
            if (!container) {
                return;
            }

            var alertMarkup = "";
            if (state.message) {
                alertMarkup = '<div class="alert alert-' + state.messageType + ' mb-3">' + escapeHtml(state.message) + "</div>";
            }

            var otpPanelMarkup = "";
            if (state.hasPendingOtp || state.verified) {
                var otpPanelDetailMarkup = state.verified
                    ? ""
                    : '<div class="small muted-text">Resend code in <strong id="studentPasswordOtpResend">calculating...</strong></div>';
                otpPanelMarkup =
                    '<div class="otp-panel mb-3">' +
                    '  <div class="otp-panel-icon">' + getShellIcon("shield") + "</div>" +
                    "  <div>" +
                    "    <strong>" + escapeHtml(state.maskedEmail || "Registered email") + "</strong>" +
                    otpPanelDetailMarkup +
                    "  </div>" +
                    "</div>";
            }

            var otpInputMarkup = "";
            if (state.hasPendingOtp && !state.verified) {
                otpInputMarkup =
                    '<div class="mb-3">' +
                    '  <label class="form-label" for="studentPasswordOtpCode">6-digit OTP</label>' +
                    '  <input class="form-control form-control-lg otp-input" id="studentPasswordOtpCode" maxlength="6" inputmode="numeric" pattern="[0-9]{6}" placeholder="Enter OTP">' +
                    "</div>" +
                    '<div class="d-flex flex-wrap gap-2 mb-3">' +
                    '  <button class="btn btn-brand" type="button" id="studentPasswordVerifyOtpButton">Verify OTP</button>' +
                    '  <button class="btn btn-warm" type="button" id="studentPasswordResendOtpButton">Resend OTP</button>' +
                    "</div>";
            }

            var passwordFormMarkup = "";
            if (state.verified) {
                passwordFormMarkup =
                    '<div class="mb-3">' +
                    '  <label class="form-label" for="studentPasswordNew">New password</label>' +
                    '  <div class="auth-input-shell">' +
                    '    <input class="form-control form-control-lg" id="studentPasswordNew" type="password" placeholder="Enter new password">' +
                    '    <button class="auth-password-toggle" type="button" data-password-toggle data-target="studentPasswordNew" aria-label="Show password">' + getShellIcon("eye") + "</button>" +
                    "  </div>" +
                    "</div>" +
                    '<div class="mb-3">' +
                    '  <label class="form-label" for="studentPasswordConfirm">Confirm new password</label>' +
                    '  <div class="auth-input-shell">' +
                    '    <input class="form-control form-control-lg" id="studentPasswordConfirm" type="password" placeholder="Confirm new password">' +
                    '    <button class="auth-password-toggle" type="button" data-password-toggle data-target="studentPasswordConfirm" aria-label="Show password">' + getShellIcon("eye") + "</button>" +
                    "  </div>" +
                    "</div>" +
                    '<button class="btn btn-brand w-100" type="button" id="studentPasswordUpdateButton">Update password</button>';
            }

            var requestButtonMarkup = "";
            if (!state.hasPendingOtp && !state.verified) {
                requestButtonMarkup = '<button class="btn btn-brand w-100" type="button" id="studentPasswordRequestOtpButton">Send OTP</button>';
            }

            container.innerHTML =
                '<p class="muted-text mb-3">For security, we will send an OTP to your registered email before allowing a password change.</p>' +
                alertMarkup +
                otpPanelMarkup +
                otpInputMarkup +
                passwordFormMarkup +
                requestButtonMarkup;

            bindPasswordModalActions(meta);
            updatePasswordModalCountdowns();
        }

        function updatePasswordModalCountdowns() {
            var resendLabel = document.getElementById("studentPasswordOtpResend");
            var resendButton = document.getElementById("studentPasswordResendOtpButton");

            if (resendLabel) {
                resendLabel.textContent = formatCountdown(state.resendAvailableAtEpochMs);
            }
            if (resendButton) {
                resendButton.disabled = !!(state.resendAvailableAtEpochMs && state.resendAvailableAtEpochMs > Date.now());
            }
        }

        function bindPasswordModalActions(currentMeta) {
            var requestButton = document.getElementById("studentPasswordRequestOtpButton");
            var resendButton = document.getElementById("studentPasswordResendOtpButton");
            var verifyButton = document.getElementById("studentPasswordVerifyOtpButton");
            var updateButton = document.getElementById("studentPasswordUpdateButton");

            document.querySelectorAll("[data-password-toggle]").forEach(function (toggleButton) {
                toggleButton.addEventListener("click", function () {
                    var targetId = toggleButton.getAttribute("data-target");
                    var targetInput = targetId ? document.getElementById(targetId) : null;
                    if (!targetInput) {
                        return;
                    }

                    var showPassword = targetInput.type === "password";
                    targetInput.type = showPassword ? "text" : "password";
                    toggleButton.setAttribute("aria-label", showPassword ? "Hide password" : "Show password");
                    toggleButton.innerHTML = showPassword ? getShellIcon("eye-off") : getShellIcon("eye");
                });
            });

            if (requestButton) {
                requestButton.addEventListener("click", function () {
                    postPasswordAction(currentMeta.passwordRequestOtpHref, {});
                });
            }
            if (resendButton) {
                resendButton.addEventListener("click", function () {
                    postPasswordAction(currentMeta.passwordResendOtpHref, {});
                });
            }
            if (verifyButton) {
                verifyButton.addEventListener("click", function () {
                    postPasswordAction(currentMeta.passwordVerifyOtpHref, {
                        otpCode: document.getElementById("studentPasswordOtpCode").value
                    });
                });
            }
            if (updateButton) {
                updateButton.addEventListener("click", function () {
                    postPasswordAction(currentMeta.passwordUpdateHref, {
                        newPassword: document.getElementById("studentPasswordNew").value,
                        confirmPassword: document.getElementById("studentPasswordConfirm").value
                    }, true);
                });
            }
        }

        function escapeHtml(value) {
            return String(value || "")
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#39;");
        }

        function requestPasswordState() {
            return fetch(meta.passwordStateHref, {
                headers: {
                    "Accept": "application/json"
                },
                credentials: "same-origin"
            }).then(function (response) {
                if (!response.ok) {
                    throw new Error("Unable to load password change status.");
                }
                return response.json();
            });
        }

        function postPasswordAction(url, payload, closeOnSuccess) {
            var body = new URLSearchParams();
            if (meta.csrfParamName && meta.csrfToken) {
                body.append(meta.csrfParamName, meta.csrfToken);
            }
            Object.keys(payload || {}).forEach(function (key) {
                body.append(key, payload[key] == null ? "" : payload[key]);
            });

            return fetch(url, {
                method: "POST",
                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
                },
                credentials: "same-origin",
                body: body.toString()
            })
                .then(function (response) {
                    return response.json().catch(function () {
                        return {};
                    }).then(function (json) {
                        if (!response.ok || json.success === false) {
                            throw new Error(json.message || "Unable to complete this action.");
                        }
                        return json;
                    });
                })
                .then(function (json) {
                    mergeState(json);
                    state.message = json.message || "";
                    state.messageType = closeOnSuccess ? "success" : "info";
                    render();

                    if (closeOnSuccess) {
                        window.Swal.close();
                        window.Swal.fire({
                            icon: "success",
                            title: "Password updated",
                            text: json.message || "Password changed successfully.",
                            confirmButtonColor: "#0f7f34"
                        });
                    }
                })
                .catch(function (error) {
                    state.message = error.message || "Unable to complete this action.";
                    state.messageType = "danger";
                    render();
                });
        }

        window.Swal.fire({
            title: "Change password",
            html: '<div id="studentPasswordFlow" class="text-start"></div>',
            showConfirmButton: false,
            showCloseButton: true,
            width: "42rem",
            didOpen: function () {
                requestPasswordState()
                    .then(function (json) {
                        mergeState(json);
                        render();
                    })
                    .catch(function (error) {
                        state.message = error.message || "Unable to load password change status.";
                        state.messageType = "danger";
                        render();
                    });

                countdownTimer = window.setInterval(updatePasswordModalCountdowns, 1000);
            },
            willClose: function () {
                if (countdownTimer) {
                    window.clearInterval(countdownTimer);
                }
            }
        });
    }

    function createLogoutAction(currentLogoutForm) {
        var form = document.createElement("form");
        form.method = (currentLogoutForm.getAttribute("method") || "post").toLowerCase();
        form.action = currentLogoutForm.getAttribute("action") || "";
        form.className = "shell-panel-form";

        Array.prototype.forEach.call(currentLogoutForm.querySelectorAll('input[type="hidden"]'), function (input) {
            form.appendChild(input.cloneNode(true));
        });

        var button = document.createElement("button");
        button.type = "submit";
        button.className = "shell-panel-item shell-panel-button shell-panel-item-danger";
        button.appendChild(createPanelIcon("logout"));

        var copy = document.createElement("span");
        copy.className = "shell-panel-item-copy";

        var heading = document.createElement("strong");
        heading.textContent = "Logout";

        var note = document.createElement("span");
        note.textContent = "End this session and go back to sign in.";

        copy.appendChild(heading);
        copy.appendChild(note);
        button.appendChild(copy);
        form.appendChild(button);
        return form;
    }

    function createPanelIcon(iconName) {
        var icon = document.createElement("span");
        icon.className = "shell-panel-item-icon";
        icon.innerHTML = getShellIcon(iconName);
        return icon;
    }

    function enhanceNavigationLinks(container) {
        Array.prototype.forEach.call(container.querySelectorAll(".nav-pill"), function (item) {
            if (item.querySelector(".nav-pill-icon")) {
                return;
            }

            var label = getTextContent(item);
            item.textContent = "";

            var icon = document.createElement("span");
            icon.className = "nav-pill-icon";
            icon.setAttribute("aria-hidden", "true");
            icon.innerHTML = getShellIcon(resolveNavIcon(item));

            var text = document.createElement("span");
            text.className = "nav-pill-label";
            text.textContent = label;

            item.appendChild(icon);
            item.appendChild(text);
        });
    }

    function normalizeNavigationOrder(container) {
        if (!container) {
            return;
        }

        var profileLink = container.querySelector('a[href*="/profile"]');
        var logoutForm = container.querySelector('form[action*="/logout"]');

        if (profileLink) {
            container.appendChild(profileLink);
        }
        if (logoutForm) {
            container.appendChild(logoutForm);
        }
    }

    function resolveNavIcon(item) {
        var href = (item.getAttribute("href") || "").toLowerCase();
        var label = getTextContent(item).toLowerCase();

        if (href.indexOf("/dashboard") !== -1 || label.indexOf("dashboard") !== -1) {
            return "dashboard";
        }
        if (href.indexOf("/books") !== -1 || label.indexOf("books") !== -1) {
            return "books";
        }
        if (href.indexOf("/issues") !== -1 || label.indexOf("issue") !== -1) {
            return "circulation";
        }
        if (href.indexOf("/catalog") !== -1 || label.indexOf("catalog") !== -1) {
            return "catalog";
        }
        if (href.indexOf("/reservations") !== -1 || label.indexOf("reservation") !== -1) {
            return "reservations";
        }
        if (href.indexOf("/students") !== -1 || label.indexOf("student") !== -1) {
            return "students";
        }
        if (href.indexOf("/fines") !== -1 || label.indexOf("fine") !== -1) {
            return "fines";
        }
        if (href.indexOf("/reports") !== -1 || label.indexOf("report") !== -1) {
            return "reports";
        }
        if (href.indexOf("/references") !== -1 || label.indexOf("categories") !== -1 || label.indexOf("authors") !== -1) {
            return "references";
        }
        if (href.indexOf("/history") !== -1 || label.indexOf("history") !== -1) {
            return "history";
        }
        if (href.indexOf("/profile") !== -1 || label.indexOf("profile") !== -1) {
            return "profile";
        }
        if (label.indexOf("logout") !== -1) {
            return "logout";
        }
        return "menu";
    }

    function getTextContent(node) {
        return node ? node.textContent.replace(/\s+/g, " ").trim() : "";
    }

    function resolveAppBaseUrl() {
        var appScript = Array.prototype.find.call(document.scripts, function (script) {
            return /\/js\/app\.js(?:\?.*)?$/.test(script.src || "");
        });

        if (!appScript || !appScript.src) {
            return window.location.origin;
        }

        return appScript.src.replace(/\/js\/app\.js(?:\?.*)?$/, "");
    }

    function resolveAssetUrl(assetPath) {
        if (!assetPath) {
            return "";
        }

        return assetBaseUrl.replace(/\/$/, "") + assetPath;
    }

    function hydrateAccountAvatar(avatarNode, avatarUrl) {
        var preview = new Image();
        preview.onload = function () {
            avatarNode.textContent = "";
            var image = document.createElement("img");
            image.src = avatarUrl;
            image.alt = "";
            avatarNode.appendChild(image);
        };
        preview.onerror = function () {
            // Keep the initial fallback if the student has no uploaded photo yet.
        };
        preview.src = avatarUrl;
    }

    function getShellIcon(name) {
        var icons = {
            menu: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16"></path><path d="M4 12h16"></path><path d="M4 17h16"></path></svg>',
            bell: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 17h5l-1.4-1.4a2 2 0 0 1-.6-1.4V11a6 6 0 1 0-12 0v3.2a2 2 0 0 1-.6 1.4L4 17h5"></path><path d="M10 17a2 2 0 0 0 4 0"></path></svg>',
            chevronDown: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"></path></svg>',
            dashboard: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="8" height="8" rx="2"></rect><rect x="13" y="3" width="8" height="5" rx="2"></rect><rect x="13" y="10" width="8" height="11" rx="2"></rect><rect x="3" y="13" width="8" height="8" rx="2"></rect></svg>',
            books: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>',
            circulation: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 12 4-4 4 4"></path><path d="M7 8v8a2 2 0 0 0 2 2h3"></path><path d="m21 12-4 4-4-4"></path><path d="M17 16V8a2 2 0 0 0-2-2h-3"></path></svg>',
            catalog: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"></circle><path d="m21 21-4.3-4.3"></path></svg>',
            reservations: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 6h13"></path><path d="M8 12h13"></path><path d="M8 18h13"></path><path d="M3 6h.01"></path><path d="M3 12h.01"></path><path d="M3 18h.01"></path></svg>',
            students: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2"></path><circle cx="9.5" cy="7" r="4"></circle><path d="M20 8v6"></path><path d="M23 11h-6"></path></svg>',
            fines: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="6" width="20" height="12" rx="2"></rect><path d="M6 12h12"></path><path d="M8 9h.01"></path><path d="M16 15h.01"></path></svg>',
            reports: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v18h18"></path><path d="M7 15v-4"></path><path d="M12 15V7"></path><path d="M17 15v-7"></path></svg>',
            references: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 12V7a2 2 0 0 0-2-2h-5"></path><path d="M14 17H6a2 2 0 0 1-2-2V7"></path><path d="M8 7h.01"></path><path d="M8 12h8"></path><path d="M8 17h4"></path><path d="m15 2 4 4-4 4"></path></svg>',
            history: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v5h5"></path><path d="M3.05 13A9 9 0 1 0 6 6.3L3 8"></path><path d="M12 7v5l4 2"></path></svg>',
            profile: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21a8 8 0 0 0-16 0"></path><circle cx="12" cy="8" r="4"></circle></svg>',
            shield: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>',
            eye: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"></path><circle cx="12" cy="12" r="3"></circle></svg>',
            "eye-off": '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 3 18 18"></path><path d="M10.6 10.6a3 3 0 0 0 4.24 4.24"></path><path d="M9.88 5.09A10.94 10.94 0 0 1 12 5c6.5 0 10 7 10 7a13.16 13.16 0 0 1-3.01 3.73"></path><path d="M6.61 6.61C3.73 8.57 2 12 2 12a13.1 13.1 0 0 0 10 7 10.9 10.9 0 0 0 5.23-1.17"></path></svg>',
            logout: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v10"></path><path d="M18.36 5.64a9 9 0 1 1-12.72 0"></path></svg>',
            alert: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 9v4"></path><path d="M12 17h.01"></path><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path></svg>',
            check: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m20 6-11 11-5-5"></path></svg>'
        };

        return icons[name] || icons.menu;
    }
})();
