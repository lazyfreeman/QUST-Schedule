package com.qust.schedule.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.qust.schedule.parser.ZhengFangParser
import com.qust.schedule.util.PreferenceManager

@SuppressLint("SetTextI18n")
class LoginWebViewActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnExtract: Button
    private lateinit var btnConfigCreds: Button
    private lateinit var tvStatus: TextView
    private lateinit var prefManager: PreferenceManager

    private var isExtracting = false
    private var hasAutoLoggedInThisSession = false
    private var hasJumpedFromPortal = false
    private var hasAutoLoginAttempted = false
    private var hasLoginFailed = false
    private var loginFailureReason: String? = null
    private var pendingCapturedUser: String? = null
    private var pendingCapturedPwd: String? = null

    companion object {
        const val EXTRA_RAW_JSON = "extra_raw_json"
        const val QUST_LOGIN_URL = "https://wvpn.qust.edu.cn"
    }

    private fun isEducationalSystem(url: String): Boolean {
        return url.contains("jwglxt") || url.contains("kbcx") || url.contains("xskbcx") || url.contains("xtgl")
    }

    private fun isLoginUrl(url: String): Boolean {
        return url.contains("login") || url.contains("cas") || url.contains("auth") ||
                url.contains("tpass") || url.contains("ipass") || url.contains("vue_main")
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PreferenceManager(this)

        // 构建原生布局
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 顶部操作与提示条
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(0xFF006064.toInt())
        }

        tvStatus = TextView(this).apply {
            text = "正在启动青科大自动化登录管道..."
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(tvStatus)

        btnConfigCreds = Button(this).apply {
            text = "凭据设置"
            textSize = 12f
            setBackgroundColor(0xFF00838F.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                showCredentialsDialog()
            }
        }
        val credParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = 12
        }
        topBar.addView(btnConfigCreds, credParams)

        btnExtract = Button(this).apply {
            text = "手动提取"
            textSize = 12f
            setBackgroundColor(0xFF00838F.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                val currentUrl = webView.url ?: ""
                if (!isEducationalSystem(currentUrl)) {
                    Toast.makeText(this@LoginWebViewActivity, "请先登录并在 WebVPN 门户中点击进入教务系统", Toast.LENGTH_SHORT).show()
                } else {
                    triggerScheduleFetch()
                }
            }
        }
        topBar.addView(btnExtract)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }

        val frameContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        webView = WebView(this)
        frameContainer.addView(webView)

        rootLayout.addView(topBar)
        rootLayout.addView(progressBar)
        rootLayout.addView(frameContainer)
        setContentView(rootLayout)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        initWebView()
        webView.loadUrl(QUST_LOGIN_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        WebView.setWebContentsDebuggingEnabled(true)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.addJavascriptInterface(AndroidJsBridge(), "AndroidBridge")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                // 拦截 WebVPN / 教务系统调用 window.open，强制在宿主 webView 中平滑打开
                val context = view?.context ?: this@LoginWebViewActivity
                val tempWebView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                            val targetUrl = request?.url?.toString() ?: ""
                            if (targetUrl.isNotBlank()) {
                                webView.loadUrl(targetUrl)
                            }
                            return true
                        }
                    }
                }
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = tempWebView
                resultMsg?.sendToTarget()
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                android.util.Log.i("QUST_NAV", "onPageStarted: $url")
                injectInterceptorHook()
                dispatchAutomationForUrl(url, isFinished = false)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                android.util.Log.i("QUST_NAV", "onPageFinished: $url")
                CookieManager.getInstance().flush()
                injectInterceptorHook()
                dispatchAutomationForUrl(url, isFinished = true)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        super.onDestroy()
        CookieManager.getInstance().flush()
    }

    private fun checkAndSavePendingCredentials() {
        val u = pendingCapturedUser
        val p = pendingCapturedPwd
        if (!u.isNullOrBlank() && !p.isNullOrBlank()) {
            if (u != prefManager.getSavedUsername() || p != prefManager.getSavedPassword()) {
                prefManager.saveUserCredentials(u, p, true)
                Toast.makeText(this, "登录成功，已更新保存的账号密码！", Toast.LENGTH_SHORT).show()
            }
            pendingCapturedUser = null
            pendingCapturedPwd = null
        }
    }

    /**
     * 根据当前页面的 URL 智能分发自动化逻辑
     */
    private fun dispatchAutomationForUrl(url: String?, isFinished: Boolean) {
        if (url == null) return

        when {
            // 阶段 1：教务系统环境（已进入教务系统）
            isEducationalSystem(url) -> {
                hasAutoLoggedInThisSession = true
                hasLoginFailed = false
                hasAutoLoginAttempted = false
                checkAndSavePendingCredentials()

                when {
                    // 已经在个人课表页面：直接发起课表提取
                    url.contains("xskbcx") || url.contains("kbcx_cxXskbcxIndex") -> {
                        tvStatus.text = "已进入个人课表页面，正在全自动提取课表..."
                        webView.postDelayed({
                            if (!isFinishing && !isDestroyed) {
                                triggerScheduleFetch()
                            }
                        }, if (isFinished) 500 else 1200)
                    }
                    // 在教务系统首页或菜单框架：立即进入个人课表
                    else -> {
                        tvStatus.text = "已接入教务系统，正在直接进入个人课表..."
                        webView.postDelayed({
                            if (!isFinishing && !isDestroyed) {
                                navigateFromJwglxtToSchedule()
                            }
                        }, if (isFinished) 300 else 800)
                    }
                }
            }

            // 阶段 2：WebVPN 域内 / 统一身份认证
            url.contains("qust.edu.cn") -> {
                if (isLoginUrl(url)) {
                    hasJumpedFromPortal = false

                    if (hasLoginFailed) {
                        tvStatus.text = if (loginFailureReason != null) {
                            "登录失败: $loginFailureReason，请点击【凭据设置】修改或手动输入"
                        } else {
                            "账号或密码可能有误，请点击【凭据设置】修改或手动输入"
                        }
                        if (isFinished) {
                            injectAutoLoginScript(allowSubmit = false)
                        }
                    } else if (hasAutoLoginAttempted) {
                        // 已经尝试过自动登录，页面又回到了或停留在登录页，说明认证失败
                        hasLoginFailed = true
                        tvStatus.text = "自动登录未成功，请检查账号密码（点击【凭据设置】）或手动输入"
                        Toast.makeText(this, "自动登录未成功，请检查学号与密码是否正确", Toast.LENGTH_SHORT).show()
                        if (isFinished) {
                            injectAutoLoginScript(allowSubmit = false)
                        }
                    } else {
                        val savedUser = prefManager.getSavedUsername()
                        val savedPwd = prefManager.getSavedPassword()
                        val isAutoEnabled = prefManager.isAutoLoginEnabled()
                        val canAutoFill = savedUser.isNotBlank() && savedPwd.isNotBlank() && isAutoEnabled

                        if (canAutoFill) {
                            tvStatus.text = "检测到登录页，正在准备自动登录..."
                            if (isFinished) {
                                hasAutoLoginAttempted = true
                                injectAutoLoginScript(allowSubmit = true)
                            }
                        } else {
                            tvStatus.text = "检测到登录页，请在页面中输入账号密码登录"
                            if (isFinished) {
                                injectAutoLoginScript(allowSubmit = false)
                            }
                        }
                    }
                } else {
                    // WebVPN 门户首页：自动寻找“教务管理系统”卡片并跳转
                    hasAutoLoggedInThisSession = true
                    hasLoginFailed = false
                    hasAutoLoginAttempted = false
                    checkAndSavePendingCredentials()

                    tvStatus.text = "已登录 WebVPN，正在自动跳转进入教务系统..."
                    if (!hasJumpedFromPortal) {
                        tryOpenJwglxtFromPortal()
                    }
                }
            }

            else -> {
                tvStatus.text = "当前页面: ${url.take(30)}..."
            }
        }
    }

    /**
     * 自动登录脚本：智能识别青科大统一身份认证/WebVPN登录表单，自动填入凭据并尝试提交
     */
    private fun injectAutoLoginScript(allowSubmit: Boolean) {
        val savedUser = prefManager.getSavedUsername()
        val savedPwd = prefManager.getSavedPassword()
        val isAutoEnabled = prefManager.isAutoLoginEnabled()

        // 转义避免 JS 注入报错
        val jsUser = escapeJs(savedUser)
        val jsPwd = escapeJs(savedPwd)
        val canAutoFill = savedUser.isNotBlank() && savedPwd.isNotBlank() && isAutoEnabled

        val script = """
            (function() {
                var autoFillUser = '$jsUser';
                var autoFillPwd = '$jsPwd';
                var canAutoFill = $canAutoFill;
                var allowSubmit = $allowSubmit;
                var autoLoginDone = false;
                var notifiedCaptcha = false;
                var loginFailedReported = false;

                if (window._qustFillTimer) {
                    clearInterval(window._qustFillTimer);
                    window._qustFillTimer = null;
                }
                if (window._qustObserver) {
                    try { window._qustObserver.disconnect(); } catch(e) {}
                    window._qustObserver = null;
                }

                function checkLoginError() {
                    try {
                        var errorSelectors = [
                            '#msg', '#errorMsg', '#error_msg', '#errormsg', '#showErrorDiv',
                            '.auth_error', '.error_msg', '.error-message', '.err-message',
                            '.el-message--error', '.el-message__content', '.el-form-item__error',
                            '.alert-danger', '.login-error', '.tips-err', '.tips_error',
                            'div[role="alert"]', '.el-alert--error', '.el-notification__content',
                            '.c-form-error', '.form-error', '.error_tip'
                        ];

                        var errorKeywords = [
                            '密码错误', '密码不正确', '用户名或密码', '账号或密码', '学号或密码',
                            '用户名不存在', '账号不存在', '用户不存在', '认证失败', '凭证',
                            '锁定', '验证码错误', '验证码不正确', '验证码有误', '验证码不能为空',
                            'Bad credentials', 'Invalid credentials', 'Login failed'
                        ];

                        for (var i = 0; i < errorSelectors.length; i++) {
                            var els = document.querySelectorAll(errorSelectors[i]);
                            for (var j = 0; j < els.length; j++) {
                                var el = els[j];
                                var t = (el.innerText || el.textContent || '').trim();
                                if (!t) continue;
                                var rect = el.getBoundingClientRect();
                                var style = window.getComputedStyle(el);
                                if (style.display === 'none' || style.visibility === 'hidden') continue;
                                for (var k = 0; k < errorKeywords.length; k++) {
                                    if (t.indexOf(errorKeywords[k]) !== -1) {
                                        return t;
                                    }
                                }
                            }
                        }

                        var spans = document.querySelectorAll('span, p, div, label, font');
                        for (var m = 0; m < spans.length; m++) {
                            var node = spans[m];
                            var text = (node.innerText || node.textContent || '').trim();
                            if (text.length > 0 && text.length <= 60) {
                                for (var n = 0; n < errorKeywords.length; n++) {
                                    if (text.indexOf(errorKeywords[n]) !== -1) {
                                        var r = node.getBoundingClientRect();
                                        var s = window.getComputedStyle(node);
                                        if (s.display !== 'none' && s.visibility !== 'hidden' && (r.width > 0 || r.height > 0)) {
                                            return text;
                                        }
                                    }
                                }
                            }
                        }
                    } catch(e) {}
                    return null;
                }

                function reportErrorIfFound() {
                    if (loginFailedReported) return true;
                    var err = checkLoginError();
                    if (err) {
                        loginFailedReported = true;
                        autoLoginDone = true;
                        window.AndroidBridge.onLoginFailed(err);
                        return true;
                    }
                    return false;
                }

                if (reportErrorIfFound()) {
                    return;
                }

                try {
                    window._qustObserver = new MutationObserver(function() {
                        reportErrorIfFound();
                    });
                    window._qustObserver.observe(document.body || document.documentElement, {
                        childList: true,
                        subtree: true,
                        characterData: true
                    });
                } catch(e) {}

                function hookCredentialCapture() {
                    try {
                        var forms = document.querySelectorAll('form');
                        for (var i = 0; i < forms.length; i++) {
                            forms[i].addEventListener('submit', function() {
                                captureInputs();
                            });
                        }
                        var allButtons = document.querySelectorAll('button, input[type="submit"], .btn-submit, .el-button');
                        for (var j = 0; j < allButtons.length; j++) {
                            var b = allButtons[j];
                            var t = (b.innerText || b.value || '').trim();
                            if (t.indexOf('登录') !== -1 || t.indexOf('Login') !== -1) {
                                b.addEventListener('click', function() {
                                    setTimeout(captureInputs, 200);
                                });
                            }
                        }
                    } catch(e) {}
                }

                function captureInputs() {
                    try {
                        var uEl = document.querySelector('input#username, input[name="username"], input[name="userName"], input[placeholder*="学号"], input[placeholder*="账号"], input[placeholder*="用户名"]');
                        var pEl = document.querySelector('input#password, input[name="password"], input[type="password"]');
                        if (uEl && pEl && uEl.value && pEl.value) {
                            if (uEl.value.trim().length >= 4 && pEl.value.trim().length >= 4) {
                                window.AndroidBridge.onCaptureCredentials(uEl.value.trim(), pEl.value.trim());
                            }
                        }
                    } catch(e) {}
                }

                function setVal(el, val) {
                    if (!el) return;
                    el.focus();
                    var last = el.value;
                    el.value = val;
                    if (el._valueTracker) {
                        el._valueTracker.setValue(last);
                    }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                }

                function tryFillAndSubmit() {
                    hookCredentialCapture();

                    if (reportErrorIfFound()) return;
                    if (autoLoginDone) return;

                    var userEl = document.querySelector('input#username, input[name="username"], input[name="userName"], input[placeholder*="学号"], input[placeholder*="账号"], input[placeholder*="用户名"], input.el-input__inner[type="text"]');
                    var pwdEl = document.querySelector('input#password, input[name="password"], input[type="password"], input.el-input__inner[type="password"]');

                    if (!userEl || !pwdEl) return;

                    if (!canAutoFill) {
                        autoLoginDone = true;
                        return;
                    }

                    if (allowSubmit) {
                        if (!userEl.value || userEl.value === autoFillUser) {
                            setVal(userEl, autoFillUser);
                        }
                        if (!pwdEl.value || pwdEl.value === autoFillPwd) {
                            setVal(pwdEl, autoFillPwd);
                        }

                        // 兼容 ElementUI / Vue 内部数据模型
                        try {
                            var vueMain = document.getElementById('vue_main');
                            if (vueMain && vueMain.__vue__) {
                                vueMain.__vue__.passwordLoginUsername = autoFillUser;
                                vueMain.__vue__.passwordLoginPassword = autoFillPwd;
                            }
                        } catch(e) {}
                    }

                    // 自动勾选记住密码/记住我
                    try {
                        var remMe = document.querySelector('input#rememberMe, input[name="rememberMe"], input[type="checkbox"]');
                        if (remMe && !remMe.checked) {
                            remMe.click();
                        }
                    } catch(e) {}

                    // 检测是否存在可见的验证码输入框
                    var captchaEl = document.querySelector('input#captcha, input[name="captcha"], input[placeholder*="验证码"], input.el-input__inner[placeholder*="验证码"]');
                    var isCaptchaVisible = false;
                    if (captchaEl) {
                        var rect = captchaEl.getBoundingClientRect();
                        var style = window.getComputedStyle(captchaEl);
                        if (rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden') {
                            isCaptchaVisible = true;
                        }
                    }

                    if (isCaptchaVisible && (!captchaEl.value || captchaEl.value.trim() === '')) {
                        if (!notifiedCaptcha) {
                            notifiedCaptcha = true;
                            captchaEl.focus();
                            window.AndroidBridge.onNotifyNeedsCaptcha();
                        }
                        return;
                    }

                    if (!allowSubmit) {
                        autoLoginDone = true;
                        return;
                    }

                    // 自动触发登录按钮提交
                    autoLoginDone = true;
                    window.AndroidBridge.onAutoLoginTriggered();

                    setTimeout(function() {
                        var submitBtn = document.querySelector('button.btn-submit, input.btn-submit, button[type="submit"], input[type="submit"], button.el-button--primary, #drcomIPLogin button');
                        if (!submitBtn) {
                            var buttons = document.querySelectorAll('button, div[role="button"], a');
                            for (var k = 0; k < buttons.length; k++) {
                                var bt = (buttons[k].innerText || buttons[k].textContent || '').trim();
                                if (bt === '登录' || bt === '登 录' || bt === '快速登录') {
                                    submitBtn = buttons[k];
                                    break;
                                }
                            }
                        }
                        if (submitBtn) {
                            submitBtn.click();

                            setTimeout(function() {
                                reportErrorIfFound();
                            }, 1500);

                            setTimeout(function() {
                                if (!reportErrorIfFound()) {
                                    window.AndroidBridge.onCheckLoginStalled();
                                }
                            }, 3500);
                        }
                    }, 500);
                }

                // 轮询重试检测登录框（应对异步渲染页面）
                var checkCount = 0;
                window._qustFillTimer = setInterval(function() {
                    checkCount++;
                    tryFillAndSubmit();
                    if (autoLoginDone || checkCount > 25) {
                        clearInterval(window._qustFillTimer);
                        window._qustFillTimer = null;
                    }
                }, 400);

                tryFillAndSubmit();
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }

    /**
     * WebVPN 门户页面：轮询检测并自动点击进入“教务管理系统”
     */
    private fun tryOpenJwglxtFromPortal() {
        val js = """
            (function() {
                var pollCount = 0;
                var portalTimer = setInterval(function() {
                    pollCount++;
                    try {
                        var items = document.querySelectorAll('a, div.portal-item, div.card, div.resource-item, li, span, button');
                        for (var i = 0; i < items.length; i++) {
                            var el = items[i];
                            var txt = (el.innerText || el.textContent || '').trim();
                            var title = el.getAttribute('title') || '';
                            var href = el.getAttribute('href') || el.href || '';
                            if (txt === '教务管理系统' || txt === '教务系统' || txt.indexOf('教务管理') !== -1 ||
                                title.indexOf('教务管理') !== -1 || title.indexOf('教务系统') !== -1 ||
                                href.indexOf('jwglxt') !== -1) {
                                clearInterval(portalTimer);
                                window.AndroidBridge.onAutoJumpToJwglxt();
                                // 优先直接点击卡片，触发 WebVPN 官方代理与 Referer
                                el.click();
                                return;
                            }
                        }
                    } catch(e) {}

                    if (pollCount > 30) {
                        clearInterval(portalTimer);
                    }
                }, 400);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    /**
     * 教务系统内部：从首页或任意框架页面直接切入“个人课表查询”
     */
    private fun navigateFromJwglxtToSchedule() {
        val js = """
            (function() {
                if (window.location.href.indexOf('xskbcx') !== -1) {
                    return;
                }

                // 1. 调用正方新版官方全局菜单函数（经 onCreateWindow 接管新窗口，保留全部 WebVPN 凭据与 Referer）
                try {
                    if (typeof clickMenu === 'function') {
                        clickMenu('N2151', '/kbcx/xskbcx_cxXskbcxIndex.html', '个人课表查询', 'null');
                        return;
                    }
                } catch(e) {}

                // 2. 尝试查找页面菜单元素点击
                try {
                    var kbMenu = document.querySelector('a[onclick*="N2151"], a[onclick*="xskbcx"]');
                    if (kbMenu) {
                        kbMenu.click();
                        return;
                    }
                    var links = document.querySelectorAll('a, span');
                    for (var i = 0; i < links.length; i++) {
                        var t = (links[i].innerText || links[i].textContent || '').trim();
                        if (t === '个人课表查询') {
                            links[i].click();
                            return;
                        }
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    /**
     * 网络请求拦截 Hook：拦截包含课表数据的 XHR 与 Fetch 响应
     */
    private fun injectInterceptorHook() {
        val hookJs = """
            (function() {
                function hookWindow(w) {
                    try {
                        if (!w || w._qustHooked) return;
                        w._qustHooked = true;

                        try {
                            var links = w.document.querySelectorAll('a[target="_blank"]');
                            for (var k = 0; k < links.length; k++) {
                                links[k].setAttribute('target', '_self');
                            }
                        } catch(e) {}

                        try {
                            w.open = function(url) {
                                if (url) {
                                    w.location.href = url;
                                }
                                return w;
                            };
                        } catch(e) {}

                        var origOpen = w.XMLHttpRequest.prototype.open;
                        w.XMLHttpRequest.prototype.open = function(method, url) {
                            this._qustUrl = url;
                            return origOpen.apply(this, arguments);
                        };

                        var origSend = w.XMLHttpRequest.prototype.send;
                        w.XMLHttpRequest.prototype.send = function() {
                            this.addEventListener('load', function() {
                                try {
                                    var u = this._qustUrl || '';
                                    if (u.indexOf('xskbcx') !== -1 || u.indexOf('cxXsKb') !== -1 || u.indexOf('cxXsgrkb') !== -1) {
                                        if (this.responseText && this.responseText.indexOf('kbList') !== -1) {
                                            window.AndroidBridge.onScheduleReceived(this.responseText);
                                        }
                                    }
                                } catch(e) {}
                            });
                            return origSend.apply(this, arguments);
                        };

                        if (w.fetch) {
                            var origFetch = w.fetch;
                            w.fetch = function() {
                                var url = arguments[0];
                                return origFetch.apply(this, arguments).then(function(response) {
                                    try {
                                        var u = (typeof url === 'string') ? url : (url && url.url ? url.url : '');
                                        if (u.indexOf('xskbcx') !== -1 || u.indexOf('cxXsKb') !== -1 || u.indexOf('cxXsgrkb') !== -1) {
                                            response.clone().text().then(function(text) {
                                                if (text && text.indexOf('kbList') !== -1) {
                                                    window.AndroidBridge.onScheduleReceived(text);
                                                }
                                            }).catch(function(){});
                                        }
                                    } catch(e) {}
                                    return response;
                                });
                            };
                        }
                    } catch(e) {}
                }

                hookWindow(window);
                try {
                    for (var i = 0; i < window.frames.length; i++) {
                        hookWindow(window.frames[i]);
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(hookJs, null)
    }

    /**
     * 主动向教务系统发起课表 POST 请求抓取
     */
    private fun triggerScheduleFetch() {
        val currentUrl = webView.url ?: ""
        if (!isEducationalSystem(currentUrl)) {
            Toast.makeText(this, "请先在页面中登录并进入教务系统", Toast.LENGTH_SHORT).show()
            return
        }

        if (isExtracting) return
        isExtracting = true
        tvStatus.text = "正在全自动向教务系统提取课表数据..."

        // 超时重置保护
        webView.postDelayed({
            if (isExtracting) {
                isExtracting = false
                tvStatus.text = "若未自动获取完成，请点击右侧【手动提取】"
            }
        }, 8000)

        val jsCode = """
            (function() {
                try {
                    window.AndroidBridge.onDebugLog('FETCH', 'Starting schedule fetch on ' + window.location.href);

                    var targets = [window];
                    try {
                        for (var i = 0; i < window.frames.length; i++) {
                            targets.push(window.frames[i]);
                        }
                    } catch(e) {}

                    var xnm = '';
                    var xqm = '';
                    for (var t = 0; t < targets.length; t++) {
                        try {
                            var doc = targets[t].document;
                            if (!doc) continue;
                            var xnmEl = doc.getElementById('xnm');
                            var xqmEl = doc.getElementById('xqm');
                            if (xnmEl && xnmEl.value) xnm = xnmEl.value;
                            if (xqmEl && xqmEl.value) xqm = xqmEl.value;
                            if (xnm && xqm) break;
                        } catch(e) {}
                    }
                    window.AndroidBridge.onDebugLog('FETCH', 'Detected params: xnm=' + xnm + ', xqm=' + xqm);

                    var postData = 'kzlx=ck' + (xnm ? '&xnm=' + encodeURIComponent(xnm) : '') + (xqm ? '&xqm=' + encodeURIComponent(xqm) : '');

                    // 智能计算前缀：针对 WebVPN 代理路径或直连
                    var currentHref = window.location.href;
                    var jwIndex = currentHref.indexOf('/jwglxt');
                    var basePrefix = '';
                    if (jwIndex !== -1) {
                        basePrefix = currentHref.substring(0, jwIndex);
                    }

                    var relativePaths = [
                        '/jwglxt/kbcx/xskbcx_cxXsKb.html?gnmkdm=N2151',
                        '/jwglxt/kbcx/xskbcx_cxXsKb.html?gnmkdm=N215110',
                        '/jwglxt/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151',
                        '/jwglxt/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N215110'
                    ];

                    var urls = [];
                    for (var p = 0; p < relativePaths.length; p++) {
                        if (basePrefix) {
                            urls.push(basePrefix + relativePaths[p]);
                        }
                        urls.push(relativePaths[p]);
                    }

                    function tryRequest(index) {
                        if (index >= urls.length) {
                            window.AndroidBridge.onError('未识别到课表数据，可点击【手动提取】重试');
                            return;
                        }
                        var targetUrl = urls[index];
                        window.AndroidBridge.onDebugLog('FETCH', 'Trying ' + targetUrl);
                        var xhr = new XMLHttpRequest();
                        xhr.open('POST', targetUrl, true);
                        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
                        xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
                        xhr.withCredentials = true;
                        xhr.onload = function() {
                            var resp = xhr.responseText || '';
                            window.AndroidBridge.onDebugLog('FETCH', targetUrl + ' HTTP ' + xhr.status + ', len=' + resp.length);
                            if (xhr.status >= 200 && xhr.status < 300) {
                                if (resp.indexOf('kbList') !== -1) {
                                    window.AndroidBridge.onScheduleReceived(resp);
                                } else {
                                    tryRequest(index + 1);
                                }
                            } else {
                                tryRequest(index + 1);
                            }
                        };
                        xhr.onerror = function() {
                            window.AndroidBridge.onDebugLog('FETCH', targetUrl + ' network error');
                            tryRequest(index + 1);
                        };
                        xhr.send(postData);
                    }

                    tryRequest(0);
                } catch(e) {
                    window.AndroidBridge.onError(e.message || '提取异常');
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsCode, null)
    }

    /**
     * 弹出凭据快速配置对话框
     */
    private fun showCredentialsDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val etUser = EditText(this).apply {
            hint = "学号/工号"
            setText(prefManager.getSavedUsername())
            setSingleLine()
        }
        val etPwd = EditText(this).apply {
            hint = "登录密码"
            setText(prefManager.getSavedPassword())
            setSingleLine()
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(etUser)
        layout.addView(etPwd)

        AlertDialog.Builder(this)
            .setTitle("设置自动登录凭据")
            .setMessage("保存后，当认证Cookie过期时将全自动填入账号密码并登录，实现一键秒级更新。")
            .setView(layout)
            .setPositiveButton("保存并填入") { _, _ ->
                val u = etUser.text.toString().trim()
                val p = etPwd.text.toString().trim()
                if (u.isNotBlank() && p.isNotBlank()) {
                    prefManager.saveUserCredentials(u, p, true)
                    hasAutoLoginAttempted = true
                    hasLoginFailed = false
                    loginFailureReason = null
                    tvStatus.text = "凭据已保存，正在重新执行自动登录..."
                    Toast.makeText(this, "凭据已保存，正在重新执行自动登录", Toast.LENGTH_SHORT).show()
                    injectAutoLoginScript(allowSubmit = true)
                } else {
                    prefManager.clearUserCredentials()
                    hasAutoLoginAttempted = false
                    hasLoginFailed = false
                    loginFailureReason = null
                    tvStatus.text = "已清除凭据，请在页面中手动输入登录"
                    Toast.makeText(this, "已清除凭据", Toast.LENGTH_SHORT).show()
                    injectAutoLoginScript(allowSubmit = false)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun escapeJs(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    inner class AndroidJsBridge {
        @Suppress("unused")
        @JavascriptInterface
        fun onDebugLog(tag: String, msg: String) {
            android.util.Log.i("QUST_DEBUG", "[$tag] $msg")
        }

        @Suppress("unused")
        @JavascriptInterface
        fun onLoginFailed(errorMsg: String) {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                hasLoginFailed = true
                loginFailureReason = errorMsg
                tvStatus.text = "登录失败: $errorMsg，请点击【凭据设置】修改或手动输入"
                Toast.makeText(
                    this@LoginWebViewActivity,
                    "登录失败: $errorMsg\n请检查学号或密码是否正确",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        @Suppress("unused")
        @JavascriptInterface
        fun onCheckLoginStalled() {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                val currentUrl = webView.url ?: ""
                if (isLoginUrl(currentUrl) && !hasAutoLoggedInThisSession) {
                    hasLoginFailed = true
                    tvStatus.text = "登录未成功，请检查账号密码（可点击右上角【凭据设置】修改）或手动登录"
                    Toast.makeText(
                        this@LoginWebViewActivity,
                        "登录未成功，请检查学号与密码是否正确",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        @Suppress("unused")
        @JavascriptInterface
        fun onNotifyNeedsCaptcha() {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                tvStatus.text = "已自动填入学号与密码，请在页面中输入验证码"
                Toast.makeText(this@LoginWebViewActivity, "已自动填入账号密码，请输入图形验证码后登录", Toast.LENGTH_SHORT).show()
            }
        }

        @Suppress("unused")
        @JavascriptInterface
        fun onAutoLoginTriggered() {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                tvStatus.text = "已自动填入凭据，正在全自动提交登录..."
            }
        }

        @Suppress("unused")
        @JavascriptInterface
        fun onAutoJumpToJwglxt() {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                hasJumpedFromPortal = true
                tvStatus.text = "已自动识别到教务系统，正在跳转进入..."
            }
        }

        @Suppress("unused")
        @JavascriptInterface
        fun onCaptureCredentials(username: String, password: String) {
            runOnUiThread {
                if (username.isNotBlank() && password.isNotBlank()) {
                    pendingCapturedUser = username
                    pendingCapturedPwd = password
                    if (!prefManager.hasUserCredentials()) {
                        prefManager.saveUserCredentials(username, password, true)
                        Toast.makeText(this@LoginWebViewActivity, "已为您记住账号密码，下次更新将全自动完成！", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        @Suppress("unused")
        @JavascriptInterface
        fun onScheduleReceived(jsonStr: String) {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                isExtracting = false
                val courses = ZhengFangParser.parseJson(jsonStr)
                if (courses.isNotEmpty()) {
                    Toast.makeText(
                        this@LoginWebViewActivity,
                        "全自动获取成功！已识别到 ${courses.size} 门课程",
                        Toast.LENGTH_SHORT
                    ).show()

                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_RAW_JSON, jsonStr)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    tvStatus.text = "已获取返回，但未找到有效课程，请确认当前学期有课"
                    Toast.makeText(
                        this@LoginWebViewActivity,
                        "未能识别到课程数据，请确保已选课",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        @Suppress("unused")
        @JavascriptInterface
        fun onError(message: String) {
            runOnUiThread {
                isExtracting = false
                tvStatus.text = "自动化提取中: $message"
            }
        }
    }
}
