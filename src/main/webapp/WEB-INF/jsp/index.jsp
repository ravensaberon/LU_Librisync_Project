<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LU Librisync | Landing</title>
    <style>
        :root {
            --accent-900: #0f7f34;
            --accent-800: #179741;
            --accent-700: #20b24e;
            --accent-100: #e8f8ed;
            --accent-050: #f6fcf7;
            --text-900: #1f2c22;
            --text-700: #6d7a70;
            --line: #d9e7dc;
            --bg: #f1f5f2;
            --panel: #fffefe;
            --white: #ffffff;
            --shadow: 0 24px 60px rgba(15, 127, 52, 0.12);
            --radius-xl: 24px;
            --radius-lg: 16px;
            --hero-image: url('<%= contextPath %>/assets/images/library-hero-placeholder.svg');
        }

        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            min-height: 100vh;
            font-family: "Poppins", "Segoe UI", Tahoma, sans-serif;
            color: var(--text-900);
            background:
                radial-gradient(circle at top left, rgba(32, 178, 78, 0.12), transparent 28%),
                linear-gradient(180deg, #f7fbf8 0%, var(--bg) 100%);
        }

        a {
            color: inherit;
            text-decoration: none;
        }

        .landing-shell {
            width: 100%;
            min-height: 100vh;
            background: var(--white);
        }

        .hero-stage {
            position: relative;
            min-height: 74vh;
            isolation: isolate;
            overflow: hidden;
            border-bottom: 1px solid var(--line);
        }

        .site-nav {
            position: absolute;
            top: 14px;
            left: 26px;
            right: 26px;
            z-index: 4;
            min-height: 58px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 14px;
            padding: 8px 6px;
        }

        .nav-left {
            display: inline-flex;
            align-items: center;
            gap: 12px;
            color: var(--accent-900);
            font-weight: 800;
            letter-spacing: 0.02em;
        }

        .nav-logo {
            width: 36px;
            height: 36px;
            border-radius: 11px;
            background: linear-gradient(135deg, var(--accent-900), var(--accent-700));
            color: var(--white);
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 0.86rem;
            font-weight: 800;
        }

        .nav-right {
            display: inline-flex;
            align-items: center;
            gap: 10px;
        }

        .hero-image {
            position: absolute;
            inset: 0;
            background-image: var(--hero-image);
            background-size: cover;
            background-position: center;
            background-color: #080b11;
            transform: scale(1.015);
            z-index: 1;
        }

        .hero-image::after {
            content: "";
            position: absolute;
            inset: 0;
            background: linear-gradient(90deg, rgba(255, 255, 255, 0.05) 0%, rgba(8, 11, 16, 0.12) 100%);
        }

        .content-blob {
            position: relative;
            z-index: 2;
            width: min(58%, 690px);
            min-height: 74vh;
            padding: 106px 56px 42px 54px;
            background: rgba(255, 255, 255, 0.98);
            border-radius: 0 62% 58% 0 / 0 68% 62% 0;
            border-right: 1px solid rgba(15, 127, 52, 0.10);
            box-shadow: 0 18px 40px rgba(15, 127, 52, 0.08);
        }

        .content-blob h1 {
            margin: 0;
            font-size: clamp(2.25rem, 5vw, 3.5rem);
            line-height: 1.02;
            letter-spacing: -0.03em;
            color: var(--accent-900);
        }

        .content-blob p {
            margin: 18px 0 0;
            max-width: 370px;
            color: var(--text-700);
            font-size: 1.01rem;
            line-height: 1.65;
        }

        .hero-actions {
            margin-top: 28px;
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
        }

        .btn,
        .btn-outline {
            min-height: 44px;
            border-radius: 999px;
            padding: 0 20px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 0.9rem;
            font-weight: 700;
            transition: 0.2s ease;
        }

        .btn {
            color: var(--white);
            background: linear-gradient(135deg, var(--accent-900), var(--accent-700));
            box-shadow: 0 12px 20px rgba(15, 127, 52, 0.22);
        }

        .btn:hover {
            transform: translateY(-1px);
        }

        .btn-outline {
            color: var(--accent-900);
            border: 1px solid rgba(15, 127, 52, 0.18);
            background: rgba(255, 255, 255, 0.72);
        }

        .btn-outline:hover {
            background: var(--accent-050);
        }

        .placeholder-note {
            margin-top: 18px;
            padding: 10px 12px;
            border-radius: 10px;
            background: rgba(15, 127, 52, 0.06);
            border: 1px dashed rgba(15, 127, 52, 0.16);
            color: #496252;
            font-size: 0.78rem;
            line-height: 1.45;
            max-width: 390px;
        }

        .placeholder-note code {
            font-family: Consolas, Monaco, monospace;
            background: rgba(255, 255, 255, 0.8);
            padding: 2px 6px;
            border-radius: 6px;
        }

        .feature-zone {
            padding: 28px 28px 34px;
            background: #f5faf6;
        }

        .feature-head {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 14px;
            margin-bottom: 14px;
        }

        .feature-head h2 {
            margin: 0;
            font-size: 1.05rem;
            color: var(--accent-900);
            letter-spacing: 0.02em;
        }

        .feature-head p {
            margin: 0;
            color: var(--text-700);
            font-size: 0.92rem;
        }

        .feature-carousel {
            position: relative;
            border-radius: var(--radius-xl);
            min-height: 170px;
            border: 1px solid var(--line);
            background: var(--white);
            box-shadow: 0 12px 30px rgba(15, 127, 52, 0.08);
            overflow: hidden;
        }

        .feature-slide {
            position: absolute;
            inset: 0;
            padding: 24px 22px;
            opacity: 0;
            transform: translateX(18px);
            transition: opacity 0.38s ease, transform 0.38s ease;
            display: grid;
            grid-template-columns: auto 1fr;
            align-items: center;
            gap: 16px;
            pointer-events: none;
        }

        .feature-slide.is-active {
            opacity: 1;
            transform: translateX(0);
            pointer-events: auto;
        }

        .feature-icon {
            width: 52px;
            height: 52px;
            border-radius: 14px;
            background: linear-gradient(135deg, var(--accent-900), var(--accent-700));
            color: var(--white);
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 1.2rem;
            font-weight: 700;
        }

        .feature-copy strong {
            display: block;
            margin-bottom: 4px;
            color: var(--accent-900);
            font-size: 1rem;
        }

        .feature-copy span {
            color: var(--text-700);
            line-height: 1.5;
            font-size: 0.93rem;
        }

        .feature-link {
            min-height: 40px;
            padding: 0 16px;
            border-radius: 999px;
            color: var(--white);
            background: linear-gradient(135deg, var(--accent-900), var(--accent-700));
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 0.86rem;
            font-weight: 700;
            white-space: nowrap;
        }

        .carousel-dots {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            margin-top: 14px;
        }

        .carousel-dot {
            width: 9px;
            height: 9px;
            border: none;
            border-radius: 50%;
            background: rgba(15, 127, 52, 0.2);
            cursor: pointer;
            padding: 0;
        }

        .carousel-dot.is-active {
            background: var(--accent-900);
        }

        @media (max-width: 1080px) {
            .content-blob {
                width: min(64%, 600px);
                padding: 94px 42px 32px 42px;
            }
        }

        @media (max-width: 900px) {
            .hero-stage {
                min-height: auto;
            }

            .hero-image {
                position: relative;
                min-height: 320px;
            }

            .content-blob {
                width: 100%;
                min-height: 0;
                border-radius: 0;
                padding: 88px 22px 24px;
                border-right: none;
                border-top: 1px solid var(--line);
            }

            .content-blob p {
                max-width: none;
            }

            .site-nav {
                min-height: 62px;
                left: 14px;
                right: 14px;
                padding: 6px 0;
            }

            .feature-zone {
                padding: 18px 14px 22px;
            }

            .feature-slide {
                grid-template-columns: 1fr;
                align-content: center;
                justify-items: start;
                padding: 20px 16px;
            }

            .feature-link {
                margin-top: 4px;
            }
        }
    </style>
</head>
<body>
    <div class="landing-shell">
        <section class="hero-stage">
            <header class="site-nav">
                <a class="nav-left" href="<%= contextPath %>/">
                    <span class="nav-logo">LU</span>
                    <span>Librisync</span>
                </a>
            </header>

            <div class="hero-image" aria-hidden="true"></div>

            <div class="content-blob">
                <h1>Comprehensive<br>Library Portal</h1>
                <p>Access books, manage borrowings, monitor requests, and support daily library operations through one organized platform for the LU community.</p>

                <div class="hero-actions">
                    <a class="btn" href="<%= contextPath %>/login">Login</a>
                    <a class="btn-outline" href="<%= contextPath %>/register">Create Account</a>
                </div>

            </div>
        </section>

        <section class="feature-zone" id="library">
            <div class="feature-head">
                <h2>Library Features</h2>
            </div>

            <div class="feature-carousel" data-feature-carousel>
                <article class="feature-slide is-active">
                    <span class="feature-icon">BK</span>
                    <div class="feature-copy">
                        <strong>Book Discovery</strong>
                        <span>Browse titles, check availability, and find your next read quickly.</span>
                    </div>
                </article>

                <article class="feature-slide">
                    <span class="feature-icon">TR</span>
                    <div class="feature-copy">
                        <strong>Borrow & Track</strong>
                        <span>View borrowed books and keep your account activity organized.</span>
                    </div>
                </article>

                <article class="feature-slide">
                    <span class="feature-icon">RQ</span>
                    <div class="feature-copy">
                        <strong>Reservations</strong>
                        <span>Queue for unavailable titles and monitor request status updates.</span>
                    </div>
                </article>

                <article class="feature-slide">
                    <span class="feature-icon">PF</span>
                    <div class="feature-copy">
                        <strong>Profile & Account</strong>
                        <span>Manage your account details and recover access anytime.</span>
                    </div>
                </article>
            </div>

            <div class="carousel-dots" data-feature-dots>
                <button class="carousel-dot is-active" type="button" aria-label="Feature 1"></button>
                <button class="carousel-dot" type="button" aria-label="Feature 2"></button>
                <button class="carousel-dot" type="button" aria-label="Feature 3"></button>
                <button class="carousel-dot" type="button" aria-label="Feature 4"></button>
            </div>
        </section>
    </div>

    <script>
        (function () {
            var carousel = document.querySelector("[data-feature-carousel]");
            var slides = carousel ? carousel.querySelectorAll(".feature-slide") : [];
            var dotsWrap = document.querySelector("[data-feature-dots]");
            var dots = dotsWrap ? dotsWrap.querySelectorAll(".carousel-dot") : [];
            var index = 0;
            var timerId;
            var delay = 3500;

            if (!slides.length || slides.length !== dots.length) {
                return;
            }

            function showSlide(nextIndex) {
                index = (nextIndex + slides.length) % slides.length;
                for (var i = 0; i < slides.length; i++) {
                    slides[i].classList.toggle("is-active", i === index);
                    dots[i].classList.toggle("is-active", i === index);
                }
            }

            function play() {
                timerId = window.setInterval(function () {
                    showSlide(index + 1);
                }, delay);
            }

            function restart() {
                window.clearInterval(timerId);
                play();
            }

            for (var i = 0; i < dots.length; i++) {
                (function (dotIndex) {
                    dots[dotIndex].addEventListener("click", function () {
                        showSlide(dotIndex);
                        restart();
                    });
                })(i);
            }

            carousel.addEventListener("mouseenter", function () {
                window.clearInterval(timerId);
            });

            carousel.addEventListener("mouseleave", function () {
                restart();
            });

            play();
        })();
    </script>
</body>
</html>
