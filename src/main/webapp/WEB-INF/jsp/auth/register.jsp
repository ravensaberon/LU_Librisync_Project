<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Latte and Letters Registration</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260503-register-refresh">
    <style>
        .register-shell {
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 32px 20px;
            background:
                linear-gradient(160deg, rgba(7, 74, 40, 0.94), rgba(20, 114, 60, 0.9));
        }


        .register-form-panel {
            width: 100%;
            max-width: 980px;
            display: flex;
            align-items: stretch;
            justify-content: center;
        }

        .register-form-card {
            width: 100%;
            padding: 36px 40px;
            border-radius: 30px;
            border: 1px solid rgba(20, 74, 44, 0.08);
            background: rgba(255, 255, 255, 0.92);
            box-shadow: 0 28px 60px rgba(22, 51, 34, 0.12);
            backdrop-filter: blur(12px);
        }

        .register-stepper {
            display: grid;
            grid-template-columns: repeat(4, minmax(0, 1fr));
            gap: 12px;
            margin-bottom: 24px;
        }

        .register-step-chip {
            padding: 14px 12px;
            border-radius: 18px;
            border: 1px solid rgba(17, 58, 36, 0.09);
            background: linear-gradient(180deg, #ffffff, #f8fbf8);
            color: #6b7b70;
            text-align: left;
            transition: 0.2s ease;
        }

        .register-step-chip-step {
            display: block;
            font-size: 0.72rem;
            font-weight: 800;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            opacity: 0.72;
            margin-bottom: 4px;
        }

        .register-step-chip-label {
            display: block;
            font-size: 0.92rem;
            font-weight: 700;
            line-height: 1.3;
        }

        .register-step-chip.active {
            border-color: rgba(15, 122, 54, 0.18);
            background: linear-gradient(180deg, #f2fbf4, #ebf8ef);
            color: #183824;
            transform: translateY(-1px);
        }

        .register-step-chip.complete {
            border-color: rgba(28, 119, 63, 0.16);
            background: linear-gradient(180deg, #eaf7ed, #e1f3e7);
            color: #1f653a;
        }

        .register-step-panel[hidden] {
            display: none !important;
        }

        .register-panel-head {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            gap: 16px;
            margin-bottom: 20px;
        }

        .register-panel-kicker {
            font-size: 0.78rem;
            font-weight: 800;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            color: #2f7d49;
            margin-bottom: 6px;
        }

        .register-panel-title {
            font-size: clamp(1.2rem, 2vw, 1.5rem);
            font-weight: 800;
            color: #173522;
            margin-bottom: 6px;
        }

        .register-panel-copy {
            margin: 0;
            color: #617567;
            font-size: 0.95rem;
            line-height: 1.6;
        }

        .register-panel-badge {
            flex-shrink: 0;
            min-width: 104px;
            padding: 12px 14px;
            border-radius: 18px;
            background: linear-gradient(180deg, #fff7e4, #fff0cf);
            border: 1px solid rgba(151, 102, 15, 0.14);
            color: #7c4a00;
            text-align: center;
            font-weight: 700;
            font-size: 0.82rem;
        }

        .register-grid {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 16px;
        }

        .register-block {
            padding: 20px;
            border-radius: 22px;
            border: 1px solid rgba(19, 62, 38, 0.08);
            background: linear-gradient(180deg, #ffffff, #fbfdfb);
        }

        .register-summary,
        .register-check,
        .register-otp-card {
            border-radius: 22px;
            border: 1px solid rgba(19, 62, 38, 0.08);
            background: linear-gradient(180deg, #fbfdfb, #f3f9f4);
            padding: 20px;
        }

        .register-otp-card.verified {
            background: linear-gradient(180deg, #edf9f0, #e5f6ea);
            border-color: rgba(28, 119, 63, 0.16);
        }

        .register-check {
            background: linear-gradient(180deg, #fffaf1, #fff6e5);
            border-color: rgba(151, 102, 15, 0.14);
        }

        .register-actions {
            display: flex;
            justify-content: space-between;
            gap: 12px;
            margin-top: 18px;
        }

        .register-step-note,
        .field-hint,
        .field-error,
        .otp-note {
            font-size: 0.84rem;
            line-height: 1.5;
        }

        .field-hint,
        .otp-note,
        .register-step-note {
            color: #607366;
            margin: 8px 0 0;
        }

        .field-error {
            color: #9d2f2a;
            min-height: 20px;
            margin: 8px 0 0;
        }

        .age-pill {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 12px 14px;
            border-radius: 18px;
            background: #f5f9f5;
            border: 1px solid rgba(19, 62, 38, 0.08);
            color: #183824;
            font-weight: 700;
        }

        .otp-actions {
            display: flex;
            gap: 12px;
            flex-wrap: wrap;
        }

        .otp-status-line {
            display: flex;
            flex-wrap: wrap;
            gap: 10px 14px;
            margin-top: 10px;
            color: #5f7b69;
            font-size: 0.85rem;
        }

        .otp-input-row {
            display: grid;
            grid-template-columns: minmax(0, 1fr) auto;
            gap: 12px;
            margin-top: 16px;
        }

        .otp-success {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 10px 12px;
            border-radius: 14px;
            background: rgba(28, 119, 63, 0.1);
            color: #1f653a;
            font-size: 0.88rem;
            font-weight: 700;
        }

        .register-submit-note {
            margin-top: 10px;
            color: #607366;
            font-size: 0.84rem;
        }

        .register-restore-banner {
            display: none;
            margin-bottom: 16px;
            padding: 12px 14px;
            border-radius: 16px;
            background: #edf7ef;
            border: 1px solid rgba(28, 119, 63, 0.12);
            color: #215638;
            font-size: 0.88rem;
            font-weight: 600;
        }

        @media (max-width: 900px) {
            .register-grid,
            .register-stepper,
            .otp-input-row {
                grid-template-columns: 1fr;
            }

            .register-form-panel {
                padding: 16px;
            }

            .register-form-card {
                padding: 22px 18px;
                border-radius: 24px;
            }

            .register-panel-head,
            .register-actions {
                flex-direction: column;
            }

            .register-panel-badge {
                width: 100%;
            }
        }
    </style>
</head>
<body>
<div class="register-shell">
    <section class="register-form-panel">
        <div class="register-form-card">
            <div class="auth-utility-bar mb-3">
                <a class="auth-back-link" href="${pageContext.request.contextPath}/login">
                    <i class="bi bi-arrow-left"></i>
                    <span>Back to login</span>
                </a>
            </div>

            <div class="auth-panel-heading mb-3">
                <h2 class="auth-panel-title">Create your <span class="auth-panel-title-accent">account</span></h2>
                <p class="auth-panel-copy">Complete each step carefully. The last step verifies your email before enabling the final registration button.</p>
            </div>

            <div class="auth-role-label">College Student</div>

            <c:if test="${not empty error}">
                <div class="alert alert-danger mt-3">${error}</div>
            </c:if>

            <div class="register-restore-banner" id="draftRestoredBanner">
                Your saved registration draft was restored on this device.
            </div>

            <div class="register-stepper" id="registerStepper">
                <div class="register-step-chip active" data-step-chip="0">
                    <span class="register-step-chip-step">Step 1</span>
                    <span class="register-step-chip-label">Personal Details</span>
                </div>
                <div class="register-step-chip" data-step-chip="1">
                    <span class="register-step-chip-step">Step 2</span>
                    <span class="register-step-chip-label">Student Details</span>
                </div>
                <div class="register-step-chip" data-step-chip="2">
                    <span class="register-step-chip-step">Step 3</span>
                    <span class="register-step-chip-label">Address</span>
                </div>
                <div class="register-step-chip" data-step-chip="3">
                    <span class="register-step-chip-step">Step 4</span>
                    <span class="register-step-chip-label">Contact and Verify</span>
                </div>
            </div>

            <form id="registerForm" method="post" action="${pageContext.request.contextPath}/register" novalidate autocomplete="off">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

                <section class="register-step-panel" data-step-panel="0">
                    <div class="register-panel-head">
                        <div>
                            <div class="register-panel-kicker">Identity</div>
                            <div class="register-panel-title">Basic personal information</div>
                            <p class="register-panel-copy">Use your real name as it should appear on your student library account.</p>
                        </div>
                        <div class="register-panel-badge">4 steps total</div>
                    </div>

                    <div class="register-grid">
                        <div class="register-block">
                            <label class="form-label" for="firstName">First name</label>
                            <input class="form-control form-control-lg" id="firstName" name="firstName" type="text" value="${firstNameValue}" maxlength="50" autocapitalize="words" autocomplete="given-name" required>
                            <p class="field-hint">Letters, spaces, apostrophe, period, and hyphen are allowed.</p>
                            <p class="field-error" id="firstNameError"></p>
                        </div>
                        <div class="register-block">
                            <label class="form-label" for="middleName">Middle name</label>
                            <input class="form-control form-control-lg" id="middleName" name="middleName" type="text" value="${middleNameValue}" maxlength="50" autocapitalize="words">
                            <p class="field-hint">Optional. Leave blank if not applicable.</p>
                            <p class="field-error" id="middleNameError"></p>
                        </div>
                    </div>

                    <div class="register-grid mt-3">
                        <div class="register-block">
                            <label class="form-label" for="lastName">Last name</label>
                            <input class="form-control form-control-lg" id="lastName" name="lastName" type="text" value="${lastNameValue}" maxlength="50" autocapitalize="words" autocomplete="family-name" required>
                            <p class="field-hint">Match the surname used in your school records.</p>
                            <p class="field-error" id="lastNameError"></p>
                        </div>
                        <div class="register-block">
                            <label class="form-label" for="suffix">Suffix</label>
                            <input class="form-control form-control-lg" id="suffix" name="suffix" type="text" value="${suffixValue}" maxlength="20" autocapitalize="characters" autocomplete="honorific-suffix">
                            <p class="field-hint">Optional. Example: Jr, Sr, III.</p>
                            <p class="field-error" id="suffixError"></p>
                        </div>
                    </div>

                    <div class="register-grid mt-3">
                        <div class="register-block">
                            <label class="form-label" for="birthDate">Birthday</label>
                            <input class="form-control form-control-lg" id="birthDate" name="birthDate" type="date" value="${birthDateValue}" required>
                            <p class="field-hint">Your age will be computed automatically.</p>
                            <p class="field-error" id="birthDateError"></p>
                        </div>
                    </div>

                    <div class="mt-3">
                        <div class="age-pill">
                            <i class="bi bi-person-badge"></i>
                            <span>Age: <strong id="ageValue">Not yet computed</strong></span>
                        </div>
                    </div>
                </section>

                <section class="register-step-panel" data-step-panel="1" hidden>
                    <div class="register-panel-head">
                        <div>
                            <div class="register-panel-kicker">Enrollment</div>
                            <div class="register-panel-title">Academic details</div>
                            <p class="register-panel-copy">Select the program and year level tied to the account being created.</p>
                        </div>
                        <div class="register-panel-badge">Temporary password is system-generated</div>
                    </div>

                    <div class="register-grid">
                        <div class="register-block">
                            <label class="form-label" for="program">Program</label>
                            <select class="form-select form-select-lg" id="program" name="program" required>
                                <option value="">Select program</option>
                                <c:if test="${not empty programValue and !programOptionLookup[programValue]}">
                                    <option value="${programValue}" selected>${programValue}</option>
                                </c:if>
                                <c:forEach items="${programOptionsByCollege}" var="collegeEntry">
                                    <optgroup label="${collegeEntry.key}">
                                        <c:forEach items="${collegeEntry.value}" var="programOption">
                                            <option value="${programOption}" <c:if test="${programValue == programOption}">selected</c:if>>${programOption}</option>
                                        </c:forEach>
                                    </optgroup>
                                </c:forEach>
                            </select>
                            <p class="field-error" id="programError"></p>
                        </div>
                        <div class="register-block">
                            <label class="form-label" for="yearLevel">Year level</label>
                            <select class="form-select form-select-lg" id="yearLevel" name="yearLevel" required>
                                <option value="">Select year level</option>
                                <c:forEach items="${yearLevelOptions}" var="yearLevelOption">
                                    <option value="${yearLevelOption}" <c:if test="${yearLevelValue == yearLevelOption}">selected</c:if>>${yearLevelOption}</option>
                                </c:forEach>
                            </select>
                            <p class="field-error" id="yearLevelError"></p>
                        </div>
                    </div>

                    <div class="register-summary mt-3">
                        <strong style="display:block;color:#173522;">What happens after registration</strong>
                        <p class="register-step-note mb-0">A temporary password is generated automatically and sent to your email. Your account signs in immediately, then the next page asks you to set a new personal password before using the system.</p>
                    </div>
                </section>

                <section class="register-step-panel" data-step-panel="2" hidden>
                    <div class="register-panel-head">
                        <div>
                            <div class="register-panel-kicker">Location</div>
                            <div class="register-panel-title">Home address in Laguna</div>
                            <p class="register-panel-copy">Choose the official city or municipality first so the barangay and zip code can be validated correctly.</p>
                        </div>
                        <div class="register-panel-badge">Laguna only</div>
                    </div>

                    <div class="register-grid">
                        <div class="register-block">
                            <label class="form-label" for="province">Province</label>
                            <select class="form-select form-select-lg" id="province" name="province" required>
                                <option value="Laguna" <c:if test="${empty provinceValue or provinceValue == 'Laguna'}">selected</c:if>>Laguna</option>
                            </select>
                            <p class="field-error" id="provinceError"></p>
                        </div>
                        <div class="register-block">
                            <label class="form-label" for="cityMunicipality">City / Municipality</label>
                            <select class="form-select form-select-lg" id="cityMunicipality" name="cityMunicipality" required>
                                <option value="">Select city / municipality</option>
                                <c:forEach items="${registrationCityZipCodes}" var="entry">
                                    <option value="${entry.key}" <c:if test="${cityMunicipalityValue == entry.key}">selected</c:if>>${entry.key}</option>
                                </c:forEach>
                            </select>
                            <p class="field-error" id="cityMunicipalityError"></p>
                        </div>
                    </div>

                    <div class="register-grid mt-3">
                        <div class="register-block">
                            <label class="form-label" for="barangay">Barangay</label>
                            <select class="form-select form-select-lg" id="barangay" name="barangay" data-selected-barangay="<c:out value='${barangayValue}'/>" <c:if test="${empty cityMunicipalityValue}">disabled</c:if> required>
                                <option value="">Select city / municipality first</option>
                                <c:if test="${not empty barangayValue}">
                                    <option value="${barangayValue}" selected>${barangayValue}</option>
                                </c:if>
                            </select>
                            <p class="field-error" id="barangayError"></p>
                        </div>
                        <div class="register-block">
                            <label class="form-label" for="street">Street / House No. <span class="text-muted" style="font-weight:400;font-size:0.85em;">(optional)</span></label>
                            <input class="form-control form-control-lg" id="street" name="street" type="text" value="${streetValue}" maxlength="180" autocomplete="address-line1">
                            <p class="field-hint">House number, street, or subdivision. Leave blank if not applicable.</p>
                            <p class="field-error" id="streetError"></p>
                        </div>
                    </div>

                    <div class="register-block mt-3">
                        <label class="form-label" for="zipcode">Zip code</label>
                        <input class="form-control form-control-lg" id="zipcode" name="zipcode" type="text" value="${zipcodeValue}" maxlength="4" readonly required>
                        <p class="field-hint">Auto-filled from the selected city or municipality.</p>
                        <p class="field-error" id="zipcodeError"></p>
                    </div>
                </section>

                <section class="register-step-panel" data-step-panel="3" hidden>
                    <div class="register-panel-head">
                        <div>
                            <div class="register-panel-kicker">Final Step</div>
                            <div class="register-panel-title">Contact details and email verification</div>
                            <p class="register-panel-copy">Your email must be verified with OTP before the final register button becomes clickable.</p>
                        </div>
                        <div class="register-panel-badge">OTP required</div>
                    </div>

                    <div class="register-grid">
                        <div class="register-block">
                            <label class="form-label" for="email">Email address</label>
                            <input class="form-control form-control-lg" id="email" name="email" type="email" value="${emailValue}" maxlength="120" autocomplete="email" required>
                            <p class="field-hint">Use lowercase only. Supported domains include Gmail, Outlook, Yahoo, and school email domains.</p>
                            <p class="field-error" id="emailError"><c:out value="${emailFieldError}"/></p>
                        </div>
                        <div class="register-block">
                            <label class="form-label" for="contactNumber">Contact number</label>
                            <input class="form-control form-control-lg" id="contactNumber" name="contactNumber" type="text" value="${contactNumberValue}" maxlength="20" autocomplete="tel" required>
                            <p class="field-hint">Use 10 to 15 digits. A leading plus sign is allowed.</p>
                            <p class="field-error" id="contactNumberError"><c:out value="${contactNumberFieldError}"/></p>
                        </div>
                    </div>

                    <div class="register-otp-card mt-3" id="otpCard">
                        <div style="display:flex;justify-content:space-between;gap:12px;align-items:flex-start;flex-wrap:wrap;">
                            <div>
                                <strong style="display:block;color:#173522;font-size:1rem;">Email OTP verification</strong>
                                <p class="otp-note mb-0" id="otpDescription">Send a 6-digit code to your email, then verify it here before creating the account.</p>
                            </div>
                            <div class="otp-success" id="otpVerifiedBadge" hidden>
                                <i class="bi bi-patch-check-fill"></i>
                                Email verified
                            </div>
                        </div>

                        <div class="otp-status-line" id="otpStatusLine">
                            <span id="otpMaskedEmailLabel">No verification code sent yet.</span>
                            <span id="otpResendLabel" hidden>Resend in <strong id="otpResendCountdown">--:--</strong></span>
                        </div>

                        <div class="otp-actions mt-3">
                            <button class="btn btn-warm" id="requestOtpButton" type="button">
                                <i class="bi bi-envelope-paper"></i>
                                Send OTP
                            </button>
                            <button class="btn btn-outline-secondary" id="resendOtpButton" type="button" hidden>
                                <i class="bi bi-arrow-repeat"></i>
                                Resend OTP
                            </button>
                        </div>

                        <div class="otp-input-row" id="otpInputRow" hidden>
                            <div>
                                <label class="form-label mt-3" for="otpCode">Verification code</label>
                                <input class="form-control form-control-lg otp-input" id="otpCode" type="text" maxlength="6" inputmode="numeric" pattern="[0-9]{6}" autocomplete="one-time-code" placeholder="Enter 6-digit OTP">
                                <p class="field-error" id="otpCodeError"></p>
                            </div>
                            <div style="display:flex;align-items:end;">
                                <button class="btn btn-brand w-100" id="verifyOtpButton" type="button">Verify OTP</button>
                            </div>
                        </div>
                    </div>

                    <div class="register-check mt-3">
                        <div class="form-check">
                            <input class="form-check-input" id="agree" name="agree" type="checkbox" value="yes" <c:if test="${agreeChecked}">checked</c:if>>
                            <label class="form-check-label" for="agree">
                                I confirm that all registration details are correct and may be used for my student library account.
                            </label>
                        </div>
                        <p class="field-error mb-0" id="agreeError"></p>
                    </div>

                    <p class="register-submit-note" id="registerSubmitNote">Verify your email and confirm your details to enable account creation.</p>
                </section>

                <div class="register-actions">
                    <button class="btn btn-warm" id="previousStepButton" type="button" hidden>Previous</button>
                    <button class="btn btn-brand ms-auto" id="nextStepButton" type="button">Next step</button>
                    <button class="btn btn-brand ms-auto" id="submitButton" type="submit" hidden disabled>Create account and sign in</button>
                </div>
            </form>

            <div class="auth-support-row mt-4">
                <span class="text-muted">Already have an account?</span>
                <a class="auth-support-link" href="${pageContext.request.contextPath}/login">Sign in here</a>
            </div>
        </div>
    </section>
</div>

<script>
    (function () {
        var form = document.getElementById("registerForm");
        if (!form) {
            return;
        }

        var storageKey = "latteandletters.registerDraft.v2";
        var panels = Array.prototype.slice.call(document.querySelectorAll("[data-step-panel]"));
        var chips = Array.prototype.slice.call(document.querySelectorAll("[data-step-chip]"));
        var previousButton = document.getElementById("previousStepButton");
        var nextButton = document.getElementById("nextStepButton");
        var submitButton = document.getElementById("submitButton");
        var draftRestoredBanner = document.getElementById("draftRestoredBanner");
        var csrfInput = form.querySelector("input[name='${_csrf.parameterName}']");
        var currentStep = 0;
        var formSubmitted = false;

        var firstName = document.getElementById("firstName");
        var middleName = document.getElementById("middleName");
        var lastName = document.getElementById("lastName");
        var suffix = document.getElementById("suffix");
        var birthDate = document.getElementById("birthDate");
        var ageValue = document.getElementById("ageValue");
        var program = document.getElementById("program");
        var yearLevel = document.getElementById("yearLevel");
        var province = document.getElementById("province");
        var cityMunicipality = document.getElementById("cityMunicipality");
        var barangay = document.getElementById("barangay");
        var street = document.getElementById("street");
        var zipcode = document.getElementById("zipcode");
        var email = document.getElementById("email");
        var contactNumber = document.getElementById("contactNumber");
        var agree = document.getElementById("agree");
        var otpCode = document.getElementById("otpCode");
        var requestOtpButton = document.getElementById("requestOtpButton");
        var resendOtpButton = document.getElementById("resendOtpButton");
        var verifyOtpButton = document.getElementById("verifyOtpButton");
        var otpInputRow = document.getElementById("otpInputRow");
        var otpCard = document.getElementById("otpCard");
        var otpVerifiedBadge = document.getElementById("otpVerifiedBadge");
        var otpDescription = document.getElementById("otpDescription");
        var otpMaskedEmailLabel = document.getElementById("otpMaskedEmailLabel");
        var otpResendLabel = document.getElementById("otpResendLabel");
        var otpResendCountdown = document.getElementById("otpResendCountdown");
        var registerSubmitNote = document.getElementById("registerSubmitNote");
        var initialBarangay = barangay.dataset.selectedBarangay || "";

        var fields = [firstName, middleName, lastName, suffix, birthDate, program, yearLevel, province, cityMunicipality, barangay, street, zipcode, email, contactNumber, agree];
        var errors = {
            firstName: document.getElementById("firstNameError"),
            middleName: document.getElementById("middleNameError"),
            lastName: document.getElementById("lastNameError"),
            suffix: document.getElementById("suffixError"),
            birthDate: document.getElementById("birthDateError"),
            program: document.getElementById("programError"),
            yearLevel: document.getElementById("yearLevelError"),
            province: document.getElementById("provinceError"),
            cityMunicipality: document.getElementById("cityMunicipalityError"),
            barangay: document.getElementById("barangayError"),
            street: document.getElementById("streetError"),
            zipcode: document.getElementById("zipcodeError"),
            email: document.getElementById("emailError"),
            contactNumber: document.getElementById("contactNumberError"),
            otpCode: document.getElementById("otpCodeError"),
            agree: document.getElementById("agreeError")
        };

        var registerAvailabilityEndpoint = "${pageContext.request.contextPath}/register/availability";
        var barangayEndpoint = "${pageContext.request.contextPath}/register/barangays";
        var otpStateEndpoint = "${pageContext.request.contextPath}/register/otp-state";
        var requestOtpEndpoint = "${pageContext.request.contextPath}/register/request-otp";
        var verifyOtpEndpoint = "${pageContext.request.contextPath}/register/verify-otp";
        var cityZipCodes = {
            <c:forEach items="${registrationCityZipCodes}" var="entry" varStatus="status">
            "${entry.key}": "${entry.value}"<c:if test="${!status.last}">,</c:if>
            </c:forEach>
        };

        var otpState = {
            verified: ${registrationEmailVerified ? 'true' : 'false'},
            hasPendingOtp: ${not empty registrationOtpExpiresAtEpochMs ? 'true' : 'false'},
            maskedEmail: "${empty registrationOtpMaskedEmail ? '' : registrationOtpMaskedEmail}",
            expiresAtEpochMs: ${empty registrationOtpExpiresAtEpochMs ? 'null' : registrationOtpExpiresAtEpochMs},
            resendAvailableAtEpochMs: ${empty registrationOtpResendAvailableAtEpochMs ? 'null' : registrationOtpResendAvailableAtEpochMs},
            email: "${empty emailValue ? '' : emailValue}",
            dispatchMessage: ""
        };

        function setFieldMessage(input, element, message) {
            if (element) {
                element.textContent = message || "";
            }
            if (input) {
                input.classList.toggle("is-invalid", !!message);
            }
        }

        function normalizeName(value) {
            return (value || "").trim().replace(/\s+/g, " ");
        }

        function capitalizeWords(value) {
            return (value || "").toLowerCase().replace(/\b([a-z])/g, function (match) {
                return match.toUpperCase();
            });
        }

        function normalizeContact(value) {
            var raw = (value || "").replace(/[ .\-()]/g, "").trim();
            if (raw.indexOf("+") > 0) {
                raw = raw.replace(/\+/g, "");
            }
            return raw;
        }

        function setOtpState(nextState) {
            otpState = {
                verified: !!nextState.verified,
                hasPendingOtp: !!nextState.hasPendingOtp,
                maskedEmail: nextState.maskedEmail || "",
                expiresAtEpochMs: nextState.expiresAtEpochMs || null,
                resendAvailableAtEpochMs: nextState.resendAvailableAtEpochMs || null,
                email: nextState.email || (email.value || "").trim().toLowerCase(),
                dispatchMessage: nextState.message || nextState.dispatchMessage || ""
            };
            renderOtpState();
            persistDraft();
        }

        function clearOtpStateForEmailChange() {
            setOtpState({
                verified: false,
                hasPendingOtp: false,
                maskedEmail: "",
                expiresAtEpochMs: null,
                resendAvailableAtEpochMs: null,
                email: "",
                dispatchMessage: ""
            });
            otpCode.value = "";
            setFieldMessage(otpCode, errors.otpCode, "");
        }

        function formatCountdown(targetEpochMs) {
            if (!targetEpochMs) {
                return "--:--";
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

        function renderOtpState() {
            var emailValue = (email.value || "").trim().toLowerCase();
            var resendBlocked = !!(otpState.resendAvailableAtEpochMs && otpState.resendAvailableAtEpochMs > Date.now());

            otpCard.classList.toggle("verified", otpState.verified);
            otpVerifiedBadge.hidden = !otpState.verified;
            otpInputRow.hidden = otpState.verified || !otpState.hasPendingOtp;
            resendOtpButton.hidden = !otpState.hasPendingOtp || otpState.verified;
            requestOtpButton.disabled = !isEmailStructurallyValid() || otpState.verified;
            resendOtpButton.disabled = resendBlocked;
            verifyOtpButton.disabled = otpState.verified;

            if (otpState.verified) {
                otpDescription.textContent = "Email verified successfully. You can now create the account once all final details are valid.";
                otpMaskedEmailLabel.textContent = otpState.maskedEmail || emailValue || "Verified email";
            } else if (otpState.hasPendingOtp) {
                otpDescription.textContent = otpState.dispatchMessage || "Enter the OTP sent to your email to unlock account creation.";
                otpMaskedEmailLabel.textContent = otpState.maskedEmail || emailValue || "Verification code sent";
            } else {
                otpDescription.textContent = "Send a 6-digit code to your email, then verify it here before creating the account.";
                otpMaskedEmailLabel.textContent = "No verification code sent yet.";
            }

            otpResendLabel.hidden = !(otpState.hasPendingOtp && !otpState.verified);
            otpResendCountdown.textContent = formatCountdown(otpState.resendAvailableAtEpochMs);

            var finalStepValid = canSubmitCurrentFinalStep();
            var canSubmit = otpState.verified && finalStepValid;
            submitButton.disabled = !canSubmit;
            if (otpState.verified) {
                registerSubmitNote.textContent = canSubmit
                    ? "Email verified. You can now create the account and continue directly to password setup."
                    : "Email verified. Review the remaining required fields before creating the account.";
            } else {
                registerSubmitNote.textContent = "Verify your email and confirm your details to enable account creation.";
            }
        }

        function saveDraftObject(draft) {
            try {
                sessionStorage.setItem(storageKey, JSON.stringify(draft));
            } catch (ignored) {
                // Ignore storage failures.
            }
        }

        function readDraftObject() {
            try {
                var raw = sessionStorage.getItem(storageKey);
                return raw ? JSON.parse(raw) : null;
            } catch (ignored) {
                return null;
            }
        }

        function persistDraft() {
            if (formSubmitted) {
                return;
            }
            var values = {};
            fields.forEach(function (field) {
                if (!field || !field.id) {
                    return;
                }
                values[field.id] = field.type === "checkbox" ? field.checked : field.value;
            });
            values.otpCode = otpCode.value;
            saveDraftObject({
                step: currentStep,
                values: values,
                focusId: document.activeElement && document.activeElement.id ? document.activeElement.id : null
            });
        }

        function clearDraft() {
            try {
                sessionStorage.removeItem(storageKey);
            } catch (ignored) {
                // Ignore storage failures.
            }
        }

        function restoreDraft() {
            var draft = readDraftObject();
            if (!draft || !draft.values) {
                return;
            }

            Object.keys(draft.values).forEach(function (key) {
                var element = document.getElementById(key);
                if (!element) {
                    return;
                }
                if (element.type === "checkbox") {
                    element.checked = !!draft.values[key];
                } else if (key !== "zipcode") {
                    element.value = draft.values[key];
                }
            });

            if (typeof draft.step === "number" && draft.step >= 0 && draft.step < panels.length) {
                currentStep = draft.step;
            }

            if (draftRestoredBanner) {
                draftRestoredBanner.style.display = "block";
            }

            window.setTimeout(function () {
                if (draft.focusId) {
                    var focusedElement = document.getElementById(draft.focusId);
                    if (focusedElement && !focusedElement.disabled && typeof focusedElement.focus === "function") {
                        focusedElement.focus();
                    }
                }
            }, 80);
        }

        function syncZipCode() {
            zipcode.value = cityZipCodes[cityMunicipality.value || ""] || "";
        }

        function validateName(input, label, optional, silent) {
            var normalized = capitalizeWords(normalizeName(input.value));
            if (!silent) {
                input.value = normalized;
            }
            if (!normalized) {
                setFieldMessage(input, errors[input.id], optional ? "" : label + " is required.");
                return optional;
            }
            if (!/^[A-Za-z](?:[A-Za-z .'-]{0,48}[A-Za-z])?$/.test(normalized)) {
                setFieldMessage(input, errors[input.id], "Use letters only. Spaces, apostrophe, hyphen, and period are allowed.");
                return false;
            }
            if ((normalized.replace(/[^A-Za-z]/g, "")).length < 2) {
                setFieldMessage(input, errors[input.id], label + " must be at least 2 letters.");
                return false;
            }
            setFieldMessage(input, errors[input.id], "");
            return true;
        }

        function validateBirthDate(silent) {
            if (!birthDate.value) {
                ageValue.textContent = "Not yet computed";
                setFieldMessage(birthDate, errors.birthDate, silent ? "" : "Birthday is required.");
                return false;
            }

            var selected = new Date(birthDate.value + "T00:00:00");
            if (Number.isNaN(selected.getTime())) {
                ageValue.textContent = "Not yet computed";
                setFieldMessage(birthDate, errors.birthDate, silent ? "" : "Enter a valid birth date.");
                return false;
            }

            var today = new Date();
            var years = today.getFullYear() - selected.getFullYear();
            var monthDiff = today.getMonth() - selected.getMonth();
            var dayDiff = today.getDate() - selected.getDate();
            if (monthDiff < 0 || (monthDiff === 0 && dayDiff < 0)) {
                years--;
            }

            ageValue.textContent = years >= 0 ? String(years) + " years old" : "Not yet computed";
            if (selected > today) {
                setFieldMessage(birthDate, errors.birthDate, "Birth date cannot be in the future.");
                return false;
            }
            if (years < 5 || years > 120) {
                setFieldMessage(birthDate, errors.birthDate, "Age must be between 5 and 120.");
                return false;
            }

            setFieldMessage(birthDate, errors.birthDate, "");
            return true;
        }

        function validateProgram() {
            if (!program.value) {
                setFieldMessage(program, errors.program, "Program is required.");
                return false;
            }
            setFieldMessage(program, errors.program, "");
            return true;
        }

        function validateYearLevel() {
            if (!yearLevel.value) {
                setFieldMessage(yearLevel, errors.yearLevel, "Year level is required.");
                return false;
            }
            setFieldMessage(yearLevel, errors.yearLevel, "");
            return true;
        }

        function validateProvince() {
            if (!province.value) {
                setFieldMessage(province, errors.province, "Province is required.");
                return false;
            }
            setFieldMessage(province, errors.province, "");
            return true;
        }

        function validateCityMunicipality() {
            syncZipCode();
            if (!cityMunicipality.value) {
                setFieldMessage(cityMunicipality, errors.cityMunicipality, "City or municipality is required.");
                return false;
            }
            setFieldMessage(cityMunicipality, errors.cityMunicipality, "");
            return true;
        }

        function validateBarangay() {
            if (!barangay.value) {
                setFieldMessage(barangay, errors.barangay, "Barangay is required.");
                return false;
            }
            setFieldMessage(barangay, errors.barangay, "");
            return true;
        }

        function validateStreet() {
            street.value = (street.value || "").trim().replace(/\s+/g, " ");
            if (street.value && street.value.length < 2) {
                setFieldMessage(street, errors.street, "Street must be at least 2 characters.");
                return false;
            }
            setFieldMessage(street, errors.street, "");
            return true;
        }

        function validateZipCode() {
            if (!zipcode.value) {
                setFieldMessage(zipcode, errors.zipcode, "Zip code is required.");
                return false;
            }
            if (!/^\d{4}$/.test(zipcode.value)) {
                setFieldMessage(zipcode, errors.zipcode, "Zip code must contain 4 digits.");
                return false;
            }
            setFieldMessage(zipcode, errors.zipcode, "");
            return true;
        }

        function validateEmail(silent) {
            var rawValue = (email.value || "").trim();
            var value = rawValue.toLowerCase();
            if (!silent) {
                email.value = value;
            }
            if (!value) {
                setFieldMessage(email, errors.email, "Email address is required.");
                return false;
            }
            if (rawValue !== value) {
                setFieldMessage(email, errors.email, "Use lowercase email only.");
                return false;
            }
            if (!/^[a-z0-9+_.-]+@[a-z0-9.-]+\.[a-z]{2,}$/.test(value) || /^\./.test(value) || /\.\./.test(value)) {
                setFieldMessage(email, errors.email, "Enter a valid email address.");
                return false;
            }
            var domain = value.split("@")[1] || "";
            if (!(/(gmail\.com|yahoo\.com|yahoo\.com\.ph|outlook\.com|hotmail\.com|live\.com|icloud\.com|proton\.me|protonmail\.com|aol\.com|gmx\.com|mail\.com|.*\.edu|.*\.edu\.ph)$/).test(domain)) {
                setFieldMessage(email, errors.email, "Use a supported provider domain or school email.");
                return false;
            }
            setFieldMessage(email, errors.email, "");
            return true;
        }

        function validateContact() {
            contactNumber.value = normalizeContact(contactNumber.value);
            if (!contactNumber.value) {
                setFieldMessage(contactNumber, errors.contactNumber, "Contact number is required.");
                return false;
            }
            if (!/^\+?\d{10,15}$/.test(contactNumber.value)) {
                setFieldMessage(contactNumber, errors.contactNumber, "Use 10 to 15 digits for the contact number.");
                return false;
            }
            setFieldMessage(contactNumber, errors.contactNumber, "");
            return true;
        }

        function validateOtpCode() {
            otpCode.value = (otpCode.value || "").replace(/\D/g, "").slice(0, 6);
            if (!otpCode.value) {
                setFieldMessage(otpCode, errors.otpCode, "Enter the 6-digit verification code.");
                return false;
            }
            if (!/^\d{6}$/.test(otpCode.value)) {
                setFieldMessage(otpCode, errors.otpCode, "OTP must contain exactly 6 digits.");
                return false;
            }
            setFieldMessage(otpCode, errors.otpCode, "");
            return true;
        }

        function validateAgreement() {
            var message = agree.checked ? "" : "Please confirm your registration details before submitting.";
            setFieldMessage(agree, errors.agree, message);
            return !message;
        }

        function validateStep(stepIndex, silent) {
            if (stepIndex === 0) {
                return validateName(firstName, "First name", false, silent)
                    && validateName(middleName, "Middle name", true, silent)
                    && validateName(lastName, "Last name", false, silent)
                    && validateName(suffix, "Suffix", true, silent)
                    && validateBirthDate(silent);
            }
            if (stepIndex === 1) {
                return validateProgram() && validateYearLevel();
            }
            if (stepIndex === 2) {
                return validateProvince()
                    && validateCityMunicipality()
                    && validateBarangay()
                    && validateStreet()
                    && validateZipCode();
            }
            return validateEmail(silent)
                && validateContact()
                && validateAgreement();
        }

        function isEmailStructurallyValid() {
            var rawValue = (email.value || "").trim();
            var value = rawValue.toLowerCase();
            var domain = value.split("@")[1] || "";
            return !!value
                && rawValue === value
                && /^[a-z0-9+_.-]+@[a-z0-9.-]+\.[a-z]{2,}$/.test(value)
                && !/^\./.test(value)
                && !/\.\./.test(value)
                && (/(gmail\.com|yahoo\.com|yahoo\.com\.ph|outlook\.com|hotmail\.com|live\.com|icloud\.com|proton\.me|protonmail\.com|aol\.com|gmx\.com|mail\.com|.*\.edu|.*\.edu\.ph)$/).test(domain);
        }

        function canSubmitCurrentFinalStep() {
            return isEmailStructurallyValid()
                && /^\+?\d{10,15}$/.test(normalizeContact(contactNumber.value))
                && !!agree.checked;
        }

        function updateStepper() {
            panels.forEach(function (panel, index) {
                panel.hidden = index !== currentStep;
            });

            chips.forEach(function (chip, index) {
                chip.classList.toggle("active", index === currentStep);
                chip.classList.toggle("complete", index < currentStep);
            });

            previousButton.hidden = currentStep === 0;
            nextButton.hidden = currentStep === panels.length - 1;
            submitButton.hidden = currentStep !== panels.length - 1;
            renderOtpState();
        }

        function goToStep(stepIndex) {
            currentStep = stepIndex;
            updateStepper();
            persistDraft();
        }

        function populateBarangays(items, selectedBarangay) {
            barangay.innerHTML = "";
            var placeholder = document.createElement("option");
            placeholder.value = "";
            placeholder.textContent = "Select barangay";
            barangay.appendChild(placeholder);

            (items || []).forEach(function (item) {
                var option = document.createElement("option");
                option.value = item;
                option.textContent = item;
                if (selectedBarangay && item === selectedBarangay) {
                    option.selected = true;
                }
                barangay.appendChild(option);
            });

            barangay.disabled = false;
        }

        function loadBarangays(selectedBarangay) {
            syncZipCode();
            if (!cityMunicipality.value) {
                barangay.innerHTML = "<option value=''>Select city / municipality first</option>";
                barangay.disabled = true;
                return Promise.resolve();
            }

            barangay.innerHTML = "<option value=''>Loading barangays...</option>";
            barangay.disabled = true;

            return fetch(barangayEndpoint + "?cityMunicipality=" + encodeURIComponent(cityMunicipality.value), {
                headers: { "Accept": "application/json" }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("Unable to load barangays.");
                    }
                    return response.json();
                })
                .then(function (items) {
                    populateBarangays(items, selectedBarangay || "");
                })
                .catch(function () {
                    barangay.innerHTML = "<option value=''>Unable to load barangays</option>";
                    barangay.disabled = true;
                    setFieldMessage(barangay, errors.barangay, "Unable to load barangays right now. Please try again.");
                });
        }

        function checkAvailability(field, value) {
            return fetch(registerAvailabilityEndpoint + "?field=" + encodeURIComponent(field) + "&value=" + encodeURIComponent(value), {
                headers: { "Accept": "application/json" }
            })
                .then(function (response) {
                    return response.json();
                })
                .then(function (result) {
                    if (!result.valid || !result.available) {
                        throw new Error(result.message || "This value is already in use.");
                    }
                    return true;
                });
        }

        function postFormEndpoint(url, payload) {
            var body = new URLSearchParams();
            Object.keys(payload).forEach(function (key) {
                body.append(key, payload[key] == null ? "" : payload[key]);
            });
            body.append("${_csrf.parameterName}", csrfInput ? csrfInput.value : "");

            return fetch(url, {
                method: "POST",
                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
                },
                body: body.toString()
            }).then(function (response) {
                return response.json().then(function (data) {
                    if (!response.ok) {
                        throw data;
                    }
                    return data;
                });
            });
        }

        function hydrateOtpStateFromServer() {
            var currentEmail = (email.value || "").trim().toLowerCase();
            if (!currentEmail) {
                clearOtpStateForEmailChange();
                return Promise.resolve();
            }
            return fetch(otpStateEndpoint + "?email=" + encodeURIComponent(currentEmail), {
                headers: { "Accept": "application/json" }
            })
                .then(function (response) {
                    return response.json();
                })
                .then(function (result) {
                    setOtpState(result);
                })
                .catch(function () {
                    renderOtpState();
                });
        }

        previousButton.addEventListener("click", function () {
            if (currentStep > 0) {
                goToStep(currentStep - 1);
            }
        });

        nextButton.addEventListener("click", function () {
            if (validateStep(currentStep)) {
                goToStep(currentStep + 1);
            }
        });

        requestOtpButton.addEventListener("click", function () {
            if (!validateEmail()) {
                return;
            }
            requestOtpButton.disabled = true;
            requestOtpButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Sending...';
            postFormEndpoint(requestOtpEndpoint, { email: email.value })
                .then(function (result) {
                    setFieldMessage(email, errors.email, "");
                    setFieldMessage(otpCode, errors.otpCode, "");
                    setOtpState(result);
                    otpCode.focus();
                })
                .catch(function (error) {
                    setFieldMessage(email, errors.email, error.message || "Unable to send OTP right now.");
                    renderOtpState();
                })
                .finally(function () {
                    requestOtpButton.disabled = false;
                    requestOtpButton.innerHTML = '<i class="bi bi-envelope-paper"></i> Send OTP';
                    renderOtpState();
                });
        });

        resendOtpButton.addEventListener("click", function () {
            if (!validateEmail()) {
                return;
            }
            resendOtpButton.disabled = true;
            resendOtpButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Sending...';
            postFormEndpoint(requestOtpEndpoint, { email: email.value })
                .then(function (result) {
                    setFieldMessage(otpCode, errors.otpCode, "");
                    setOtpState(result);
                    otpCode.focus();
                })
                .catch(function (error) {
                    setFieldMessage(otpCode, errors.otpCode, error.message || "Unable to resend OTP right now.");
                    renderOtpState();
                })
                .finally(function () {
                    resendOtpButton.innerHTML = '<i class="bi bi-arrow-repeat"></i> Resend OTP';
                    renderOtpState();
                });
        });

        verifyOtpButton.addEventListener("click", function () {
            if (!validateEmail() || !validateOtpCode()) {
                return;
            }
            verifyOtpButton.disabled = true;
            verifyOtpButton.textContent = "Verifying...";
            postFormEndpoint(verifyOtpEndpoint, {
                email: email.value,
                otpCode: otpCode.value
            })
                .then(function (result) {
                    setFieldMessage(otpCode, errors.otpCode, "");
                    setOtpState(result);
                    otpCode.value = "";
                })
                .catch(function (error) {
                    setFieldMessage(otpCode, errors.otpCode, error.message || "Unable to verify OTP.");
                    renderOtpState();
                })
                .finally(function () {
                    verifyOtpButton.textContent = "Verify OTP";
                    renderOtpState();
                });
        });

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            if (!validateStep(3) || !otpState.verified) {
                if (!otpState.verified) {
                    setFieldMessage(otpCode, errors.otpCode, "Verify your email with OTP before creating your account.");
                }
                return;
            }

            submitButton.disabled = true;
            submitButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Creating account...';

            Promise.all([
                checkAvailability("email", email.value),
                checkAvailability("contactNumber", contactNumber.value)
            ]).then(function () {
                formSubmitted = true;
                clearDraft();
                form.submit();
            }).catch(function (error) {
                var message = error && error.message ? error.message : "Please review your details.";
                if (message.toLowerCase().indexOf("email") !== -1) {
                    setFieldMessage(email, errors.email, message);
                } else if (message.toLowerCase().indexOf("contact") !== -1) {
                    setFieldMessage(contactNumber, errors.contactNumber, message);
                } else {
                    setFieldMessage(otpCode, errors.otpCode, message);
                }
            }).finally(function () {
                if (!formSubmitted) {
                    submitButton.disabled = false;
                    submitButton.textContent = "Create account and sign in";
                }
            });
        });

        [firstName, middleName, lastName, suffix].forEach(function (input) {
            input.addEventListener("input", function () {
                validateName(
                    input,
                    input === middleName ? "Middle name" : input === firstName ? "First name" : input === lastName ? "Last name" : "Suffix",
                    input === middleName || input === suffix,
                    true
                );
                persistDraft();
            });
            input.addEventListener("blur", function () {
                validateName(
                    input,
                    input === middleName ? "Middle name" : input === firstName ? "First name" : input === lastName ? "Last name" : "Suffix",
                    input === middleName || input === suffix
                );
                persistDraft();
            });
        });

        birthDate.addEventListener("change", function () {
            validateBirthDate();
            persistDraft();
        });
        program.addEventListener("change", function () {
            validateProgram();
            persistDraft();
        });
        yearLevel.addEventListener("change", function () {
            validateYearLevel();
            persistDraft();
        });
        province.addEventListener("change", function () {
            validateProvince();
            persistDraft();
        });
        cityMunicipality.addEventListener("change", function () {
            validateCityMunicipality();
            loadBarangays("").finally(function () {
                validateZipCode();
                persistDraft();
            });
        });
        barangay.addEventListener("change", function () {
            validateBarangay();
            persistDraft();
        });
        street.addEventListener("input", function () {
            validateStreet();
            persistDraft();
        });
        street.addEventListener("blur", function () {
            validateStreet();
            persistDraft();
        });
        email.addEventListener("input", function () {
            var normalized = (email.value || "").trim().toLowerCase();
            if (otpState.email && normalized !== otpState.email.toLowerCase()) {
                clearOtpStateForEmailChange();
            }
            validateEmail(true);
            persistDraft();
        });
        email.addEventListener("blur", function () {
            if (!validateEmail()) {
                return;
            }
            checkAvailability("email", email.value)
                .then(function () {
                    setFieldMessage(email, errors.email, "");
                    return hydrateOtpStateFromServer();
                })
                .catch(function (error) {
                    setFieldMessage(email, errors.email, error.message || "This email is already taken.");
                    clearOtpStateForEmailChange();
                });
            persistDraft();
        });
        contactNumber.addEventListener("input", function () {
            validateContact();
            persistDraft();
        });
        contactNumber.addEventListener("blur", function () {
            if (!validateContact()) {
                return;
            }
            checkAvailability("contactNumber", contactNumber.value)
                .then(function () {
                    setFieldMessage(contactNumber, errors.contactNumber, "");
                })
                .catch(function (error) {
                    setFieldMessage(contactNumber, errors.contactNumber, error.message || "This contact number is already used.");
                });
            persistDraft();
        });
        otpCode.addEventListener("input", function () {
            validateOtpCode();
            persistDraft();
        });
        agree.addEventListener("change", function () {
            validateAgreement();
            renderOtpState();
            persistDraft();
        });
        document.addEventListener("focusin", function () {
            persistDraft();
        });

        restoreDraft();
        validateBirthDate(true);
        syncZipCode();
        loadBarangays(barangay.value || initialBarangay).finally(function () {
            validateZipCode();
            hydrateOtpStateFromServer().finally(function () {
                updateStepper();
            });
        });

        window.setInterval(function () {
            renderOtpState();
        }, 1000);
    })();
</script>
</body>
</html>
