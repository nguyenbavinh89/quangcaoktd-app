package com.quangcaoktd.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Single-Activity WebView wrapper for https://quangcaoktd.com/
 *
 * - Chỉ dùng quyền INTERNET.
 * - Điều hướng nội bộ (cùng domain) được giữ trong WebView.
 * - Các liên kết ra ngoài (tel:, zalo, facebook, Google Maps, ...) được
 *   mở bằng ứng dụng tương ứng trên máy (nếu có) hoặc trình duyệt ngoài.
 * - Có màn hình báo lỗi mất kết nối kèm nút "Thử lại".
 */
public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL = "https://quangcaoktd.com/";
    // Domain (và các subdomain) được phép tải trực tiếp bên trong WebView.
    private static final String HOME_HOST = "quangcaoktd.com";

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View errorView;
    private Button retryButton;

    private boolean isPageError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        WavyHeaderView headerBar = findViewById(R.id.header_bar);
        webView = findViewById(R.id.webview);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        errorView = findViewById(R.id.error_view);
        retryButton = findViewById(R.id.btn_retry);

        // Đẩy phần khuyết camera/loa/pin xuống dưới dải sóng của header
        ViewCompat.setOnApplyWindowInsetsListener(headerBar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            ((WavyHeaderView) v).setStatusBarHeight(top);
            return insets;
        });

        setupWebView();

        swipeRefresh.setColorSchemeColors(0xFF0277A4);
        swipeRefresh.setOnRefreshListener(this::reload);

        retryButton.setOnClickListener(v -> reload());

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(HOME_URL);
        }
    }

    // =========================================================================
    // WEBVIEW SETUP
    // =========================================================================

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " QuangCaoQuocDangApp/1.0");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new AppWebViewClient());
        webView.setWebChromeClient(new AppWebChromeClient());
    }

    private class AppWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleUrlRouting(request.getUrl());
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            isPageError = false;
            showLoading(true);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            showLoading(false);
            if (!isPageError) {
                showErrorView(false);
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            // Chỉ hiển thị màn lỗi khi request lỗi là request chính của trang (main frame)
            if (request.isForMainFrame()) {
                isPageError = true;
                showLoading(false);
                showErrorView(true);
            }
        }

        // Overload cũ (API 21-22, trước khi có WebResourceRequest) — giữ để tương
        // thích ngược đầy đủ; trên API 23+ hệ thống chỉ gọi overload phía trên.
        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            if (failingUrl != null && failingUrl.equals(view.getUrl())) {
                isPageError = true;
                showLoading(false);
                showErrorView(true);
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            // Không bỏ qua lỗi chứng chỉ SSL vì lý do bảo mật — huỷ tải trang.
            handler.cancel();
            isPageError = true;
            showLoading(false);
            showErrorView(true);
        }
    }

    private class AppWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
            if (newProgress >= 100) {
                showLoading(false);
            }
        }

        // Liên kết mở tab mới (target="_blank") -> mở ra ngoài bằng ứng dụng/trình duyệt phù hợp
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
            WebView temp = new WebView(MainActivity.this);
            temp.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView v, String url, Bitmap favicon) {
                    if (url != null && !url.isEmpty() && !"about:blank".equals(url)) {
                        openExternally(Uri.parse(url));
                    }
                    v.stopLoading();
                }
            });
            ((WebView.WebViewTransport) resultMsg.obj).setWebView(temp);
            resultMsg.sendToTarget();
            return true;
        }
    }

    // =========================================================================
    // ĐIỀU HƯỚNG URL
    // =========================================================================

    /**
     * Trả về true nếu URL đã được xử lý (mở app khác / trình duyệt ngoài) và
     * WebView KHÔNG cần tự tải nó. Trả về false để WebView tải URL bình thường.
     */
    private boolean handleUrlRouting(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        if (scheme == null) return false;

        // Số điện thoại, SMS, email, bản đồ -> giao cho app hệ thống xử lý
        if (scheme.equals("tel") || scheme.equals("sms") || scheme.equals("smsto")
                || scheme.equals("mailto") || scheme.equals("geo")
                || scheme.equals("market") || scheme.equals("intent")
                || scheme.equals("whatsapp") || scheme.equals("tg")
                || scheme.equals("fb") || scheme.equals("twitter")) {
            openExternally(uri);
            return true;
        }

        // Các liên kết http/https
        if (scheme.equals("http") || scheme.equals("https")) {
            String host = uri.getHost();
            boolean sameSite = host != null &&
                    (host.equalsIgnoreCase(HOME_HOST) || host.toLowerCase().endsWith("." + HOME_HOST));

            if (sameSite) {
                // Cùng trang web -> để WebView tự tải
                return false;
            }

            // Khác domain (Zalo, Facebook, Google Maps, YouTube, ...) -> mở app/trình duyệt ngoài
            openExternally(uri);
            return true;
        }

        // Các scheme khác (javascript:, about:, data:, blob:, ...) -> để WebView xử lý mặc định
        if (scheme.equals("javascript") || scheme.equals("about")
                || scheme.equals("data") || scheme.equals("blob")) {
            return false;
        }

        // Scheme lạ không xác định -> thử mở ngoài, nếu không có app xử lý thì bỏ qua an toàn
        openExternally(uri);
        return true;
    }

    private void openExternally(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Không tìm thấy ứng dụng để mở liên kết này", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // TRẠNG THÁI GIAO DIỆN
    // =========================================================================

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (!loading) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void showErrorView(boolean show) {
        errorView.setVisibility(show ? View.VISIBLE : View.GONE);
        webView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void reload() {
        isPageError = false;
        showErrorView(false);
        webView.reload();
    }

    // =========================================================================
    // VÒNG ĐỜI
    // =========================================================================

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null
                && webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
