window.LuLibrisyncQr = (function () {
    var DEFAULT_FORMATS = ["qr_code"];

    function setStatus(element, message, isWarning) {
        if (!element) {
            return;
        }

        element.textContent = message;
        element.classList.toggle("warn", Boolean(isWarning));
    }

    function renderQr(container, value, options) {
        var settings = options || {};

        if (!container) {
            return null;
        }

        container.innerHTML = "";

        if (!value) {
            container.textContent = settings.emptyText || "No QR code available.";
            return null;
        }

        if (typeof window.QRious !== "function") {
            container.textContent = settings.errorText || "QR rendering is unavailable right now.";
            return null;
        }

        var canvas = document.createElement("canvas");

        try {
            new window.QRious({
                element: canvas,
                value: value,
                size: settings.size || 220,
                level: settings.level || "H",
                foreground: settings.foreground || "#0f7f34",
                background: settings.background || "#ffffff",
                padding: settings.padding == null ? 16 : settings.padding
            });
            container.appendChild(canvas);
            return canvas;
        } catch (error) {
            container.textContent = settings.errorText || "Unable to render this QR code.";
            return null;
        }
    }

    function normalizeFilename(value, fallback) {
        var normalized = (value || "")
            .toLowerCase()
            .replace(/[^a-z0-9]+/g, "-")
            .replace(/^-+|-+$/g, "");

        return normalized || fallback || "qr-code";
    }

    function downloadCanvas(canvas, filename) {
        if (!canvas || typeof canvas.toDataURL !== "function") {
            return;
        }

        var link = document.createElement("a");
        link.href = canvas.toDataURL("image/png");
        link.download = filename || "qr-code.png";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    function decodeQrImageFile(file) {
        if (!file) {
            return Promise.reject(new Error("Choose a QR image first."));
        }

        if (typeof window.jsQR !== "function") {
            return Promise.reject(new Error("QR image decoding is unavailable right now."));
        }

        return new Promise(function (resolve, reject) {
            var image = new Image();
            var objectUrl = URL.createObjectURL(file);

            image.onload = function () {
                var canvas = document.createElement("canvas");
                var context = canvas.getContext("2d", { willReadFrequently: true });

                try {
                    canvas.width = image.naturalWidth || image.width;
                    canvas.height = image.naturalHeight || image.height;
                    context.drawImage(image, 0, 0, canvas.width, canvas.height);

                    var imageData = context.getImageData(0, 0, canvas.width, canvas.height);
                    var qrResult = window.jsQR(imageData.data, canvas.width, canvas.height, {
                        inversionAttempts: "attemptBoth"
                    });

                    if (qrResult && qrResult.data) {
                        resolve(qrResult.data);
                    } else {
                        reject(new Error("No QR code was found in the selected image."));
                    }
                } catch (error) {
                    reject(new Error("Unable to read the selected QR image."));
                } finally {
                    URL.revokeObjectURL(objectUrl);
                }
            };

            image.onerror = function () {
                URL.revokeObjectURL(objectUrl);
                reject(new Error("Unable to open the selected image."));
            };

            image.src = objectUrl;
        });
    }

    function isQrOnlyFormatList(formats) {
        return Array.isArray(formats)
            && formats.length > 0
            && formats.every(function (format) {
                return format === "qr_code";
            });
    }

    function decodeBarcodeFromImageFile(file) {
        if (!file) {
            return Promise.reject(new Error("Choose an image first."));
        }

        return new Promise(function (resolve, reject) {
            var objectUrl = URL.createObjectURL(file);

            // Try ZXing first — handles all 1D barcodes and QR from images
            if (window.ZXingBrowser && typeof window.ZXingBrowser.BrowserMultiFormatReader === "function") {
                var reader = new window.ZXingBrowser.BrowserMultiFormatReader();
                reader.decodeFromImageUrl(objectUrl)
                    .then(function (result) {
                        URL.revokeObjectURL(objectUrl);
                        var text = extractDetectedText(result);
                        if (text) {
                            resolve(text);
                        } else {
                            reject(new Error("No barcode was found in the selected image."));
                        }
                    })
                    .catch(function () {
                        URL.revokeObjectURL(objectUrl);
                        reject(new Error("No barcode was found in the selected image. Try a clearer photo with the barcode fully visible."));
                    });
                return;
            }

            // Fallback: jsQR for QR-only images
            if (typeof window.jsQR === "function") {
                var image = new Image();
                image.onload = function () {
                    var canvas = document.createElement("canvas");
                    var context = canvas.getContext("2d", { willReadFrequently: true });
                    canvas.width = image.naturalWidth || image.width;
                    canvas.height = image.naturalHeight || image.height;
                    context.drawImage(image, 0, 0, canvas.width, canvas.height);
                    var imageData = context.getImageData(0, 0, canvas.width, canvas.height);
                    var qrResult = window.jsQR(imageData.data, canvas.width, canvas.height, { inversionAttempts: "attemptBoth" });
                    URL.revokeObjectURL(objectUrl);
                    if (qrResult && qrResult.data) {
                        resolve(qrResult.data);
                    } else {
                        reject(new Error("No barcode was found in the selected image."));
                    }
                };
                image.onerror = function () {
                    URL.revokeObjectURL(objectUrl);
                    reject(new Error("Unable to open the selected image."));
                };
                image.src = objectUrl;
                return;
            }

            URL.revokeObjectURL(objectUrl);
            reject(new Error("Barcode image decoding is unavailable in this browser."));
        });
    }

    function extractDetectedText(result) {
        if (!result) {
            return "";
        }

        if (typeof result === "string") {
            return result;
        }

        if (typeof result.getText === "function") {
            return result.getText();
        }

        return result.rawValue || result.text || "";
    }

    function createScanner(options) {
        var settings = options || {};
        var videoElement = settings.videoElement;
        var statusElement = settings.statusElement;
        var barcodeFormats = settings.formats || DEFAULT_FORMATS;
        var qrOnlyMode = isQrOnlyFormatList(barcodeFormats);
        var fallbackCanvas = document.createElement("canvas");
        var fallbackContext = fallbackCanvas.getContext("2d", { willReadFrequently: true });
        var detector = null;
        var activeStream = null;
        var animationFrameId = null;
        var isRunning = false;
        var zxingReader = null;
        var zxingControls = null;

        function stop() {
            isRunning = false;

            if (animationFrameId) {
                cancelAnimationFrame(animationFrameId);
                animationFrameId = null;
            }

            if (activeStream) {
                activeStream.getTracks().forEach(function (track) {
                    track.stop();
                });
                activeStream = null;
            }

            if (zxingControls && typeof zxingControls.stop === "function") {
                zxingControls.stop();
                zxingControls = null;
            }

            if (zxingReader && typeof zxingReader.reset === "function") {
                try {
                    zxingReader.reset();
                } catch (error) {
                    // Ignore reset failures from third-party readers during teardown.
                }
            }

            zxingReader = null;

            if (videoElement) {
                videoElement.srcObject = null;
            }

            detector = null;
        }

        function handleDetectedValue(rawValue) {
            stop();

            if (typeof settings.onDetected === "function") {
                settings.onDetected(rawValue);
            }
        }

        async function createNativeDetector() {
            if (!("BarcodeDetector" in window)) {
                return null;
            }

            try {
                if (typeof window.BarcodeDetector.getSupportedFormats === "function") {
                    var supportedFormats = await window.BarcodeDetector.getSupportedFormats();
                    var compatibleFormats = barcodeFormats.filter(function (format) {
                        return supportedFormats.indexOf(format) !== -1;
                    });

                    if (compatibleFormats.length > 0) {
                        return new window.BarcodeDetector({
                            formats: compatibleFormats
                        });
                    }

                    if (qrOnlyMode) {
                        return new window.BarcodeDetector({
                            formats: ["qr_code"]
                        });
                    }

                    return null;
                }

                return new window.BarcodeDetector({
                    formats: barcodeFormats
                });
            } catch (error) {
                return null;
            }
        }

        async function startZxingScanner() {
            if (!window.ZXingBrowser || typeof window.ZXingBrowser.BrowserMultiFormatReader !== "function") {
                return false;
            }

            try {
                zxingReader = new window.ZXingBrowser.BrowserMultiFormatReader();
                zxingControls = await zxingReader.decodeFromConstraints({
                    video: {
                        facingMode: { ideal: "environment" }
                    },
                    audio: false
                }, videoElement, function (result, error) {
                    var decodedText = extractDetectedText(result);
                    if (decodedText) {
                        handleDetectedValue(decodedText);
                        return;
                    }

                    if (error && typeof settings.onScanError === "function") {
                        settings.onScanError(error);
                    }
                });

                isRunning = true;
                setStatus(
                    statusElement,
                    settings.liveMessage || "Scanner is live. Align the code inside the frame and hold still for a moment.",
                    false
                );
                return true;
            } catch (error) {
                zxingReader = null;
                zxingControls = null;
                return false;
            }
        }

        async function scanLoop() {
            if (!isRunning) {
                return;
            }

            if (!videoElement || !videoElement.srcObject || videoElement.readyState < 2) {
                animationFrameId = window.requestAnimationFrame(scanLoop);
                return;
            }

            try {
                if (detector) {
                    var detectedCodes = await detector.detect(videoElement);
                    if (detectedCodes && detectedCodes.length > 0 && detectedCodes[0].rawValue) {
                        handleDetectedValue(detectedCodes[0].rawValue);
                        return;
                    }
                } else if (typeof window.jsQR === "function" && fallbackContext) {
                    var width = videoElement.videoWidth || 0;
                    var height = videoElement.videoHeight || 0;

                    if (width > 0 && height > 0) {
                        fallbackCanvas.width = width;
                        fallbackCanvas.height = height;
                        fallbackContext.drawImage(videoElement, 0, 0, width, height);
                        var imageData = fallbackContext.getImageData(0, 0, width, height);
                        var qrResult = window.jsQR(imageData.data, width, height, {
                            inversionAttempts: "attemptBoth"
                        });

                        if (qrResult && qrResult.data) {
                            handleDetectedValue(qrResult.data);
                            return;
                        }
                    }
                }
            } catch (error) {
                if (typeof settings.onScanError === "function") {
                    settings.onScanError(error);
                }
            }

            animationFrameId = window.requestAnimationFrame(scanLoop);
        }

        async function start() {
            stop();

            if (!navigator.mediaDevices || typeof navigator.mediaDevices.getUserMedia !== "function") {
                setStatus(
                    statusElement,
                    settings.unsupportedMessage || "This device does not support camera scanning in the browser.",
                    true
                );
                return false;
            }

            detector = await createNativeDetector();

            if (!detector && !qrOnlyMode && await startZxingScanner()) {
                return true;
            }

            if (!detector && (!qrOnlyMode || typeof window.jsQR !== "function")) {
                setStatus(
                    statusElement,
                    settings.unsupportedMessage || "Live code scanning is unavailable in this browser.",
                    true
                );
                return false;
            }

            try {
                activeStream = await navigator.mediaDevices.getUserMedia({
                    video: {
                        facingMode: { ideal: "environment" }
                    },
                    audio: false
                });

                videoElement.srcObject = activeStream;
                await videoElement.play();
                isRunning = true;

                if (detector) {
                    setStatus(
                        statusElement,
                        settings.liveMessage || "Scanner is live. Align the code inside the frame and hold still for a moment.",
                        false
                    );
                } else {
                    setStatus(
                        statusElement,
                        settings.qrFallbackMessage || "QR-only scanning is active. Aim the camera at a LU Librisync QR label.",
                        false
                    );
                }

                animationFrameId = window.requestAnimationFrame(scanLoop);
                return true;
            } catch (error) {
                setStatus(
                    statusElement,
                    settings.permissionMessage || "Camera access was blocked or unavailable. Please allow camera use, then try again.",
                    true
                );
                return false;
            }
        }

        async function decodeFile(file) {
            stop();

            try {
                var decodedValue = await decodeQrImageFile(file);
                setStatus(
                    statusElement,
                    settings.fileSuccessMessage || "QR image decoded successfully. Applying it now.",
                    false
                );
                handleDetectedValue(decodedValue);
                return true;
            } catch (error) {
                setStatus(
                    statusElement,
                    error && error.message ? error.message : (settings.fileErrorMessage || "Unable to read the selected QR image."),
                    true
                );
                return false;
            }
        }

        return {
            decodeFile: decodeFile,
            start: start,
            stop: stop,
            setStatus: function (message, isWarning) {
                setStatus(statusElement, message, isWarning);
            }
        };
    }

    return {
        createScanner: createScanner,
        decodeBarcodeFromImageFile: decodeBarcodeFromImageFile,
        downloadCanvas: downloadCanvas,
        normalizeFilename: normalizeFilename,
        renderQr: renderQr,
        setStatus: setStatus
    };
})();
