package com.lamps.sdk.webview

import android.view.MotionEvent
import android.webkit.JavascriptInterface
import kotlin.math.abs

/**
 * 内嵌 WebView 放在可横向翻页的容器（TabLayout + ViewPager）里时，横向滑动先交给页面内容。
 * 当前方向已经滚不动之后，再把后续滑动交回外层。
 *
 * 页面内部的横向滚动不在 WebView 自己的 scrollX 上，所以由注入脚本读取 scrollLeft 后回报。
 */
internal class HorizontalNesting(
    private val webView: LampsWebView,
) {
    private var destroyed = false
    private var scriptInstalled = false
    private var installGeneration = 0
    private var tracking = false
    private var jsReady = false
    private var canLeft = false
    private var canRight = false
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f

    fun onPageStarted() {
        installGeneration += 1
        scriptInstalled = false
        jsReady = false
        canLeft = false
        canRight = false
    }

    fun install() {
        if (destroyed || webView.displayMode != LampsWebView.DISPLAY_MODE_EMBED) return
        val generation = installGeneration
        webView.evaluateJavascript(INSTALL_SCRIPT) {
            if (!destroyed && generation == installGeneration) {
                scriptInstalled = true
            }
        }
    }

    fun destroy() {
        destroyed = true
        tracking = false
    }

    fun onDispatchTouchEvent(event: MotionEvent) {
        if (webView.displayMode != LampsWebView.DISPLAY_MODE_EMBED) return
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tracking = true
                jsReady = false
                canLeft = false
                canRight = false
                downX = event.x
                downY = event.y
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> tracking = false
        }
        lastX = event.x
        lastY = event.y
    }

    /**
     * 在 WebView 处理完触摸之后再决定外层能不能拦截。
     * WebView 自己会按文档滚动改父级拦截，这里用页面回报盖过那个结果。
     */
    fun afterDispatchTouchEvent() {
        if (destroyed || webView.displayMode != LampsWebView.DISPLAY_MODE_EMBED) return
        webView.parent?.requestDisallowInterceptTouchEvent(tracking && shouldClaim())
    }

    /**
     * H5 上报当前触摸点下，页面还能不能往左 / 往右滚。`1` 表示该方向还能滚。
     */
    @JavascriptInterface
    fun report(canScrollLeft: Int, canScrollRight: Int) {
        val left = canScrollLeft == 1
        val right = canScrollRight == 1
        webView.post {
            if (destroyed || !tracking) return@post
            canLeft = left
            canRight = right
            jsReady = true
            afterDispatchTouchEvent()
        }
    }

    private fun shouldClaim(): Boolean {
        val dx = lastX - downX
        val dy = lastY - downY
        if (abs(dx) <= abs(dy)) return true
        if (!scriptInstalled || !jsReady) return true
        return if (dx > 0f) canLeft else canRight
    }

    private companion object {
        /**
         * 从触摸点向上找还能横向滚动的节点。只在能力变化时回调 Native。
         */
        val INSTALL_SCRIPT = """
            (function () {
              if (window.__lampsHorizontalNest) return;
              window.__lampsHorizontalNest = true;
              var EDGE = 2, x0 = 0, y0 = 0, lastL = null, lastR = null;
              function scrollable(el) {
                if (!el || el.nodeType !== 1 || el.scrollWidth - el.clientWidth <= EDGE) return false;
                if (el === document.scrollingElement || el === document.body || el === document.documentElement) return true;
                var ox = getComputedStyle(el).overflowX;
                return ox === 'auto' || ox === 'scroll' || ox === 'overlay';
              }
              function room(node) {
                var left = false, right = false, found = false;
                var el = node && node.nodeType === 1 ? node : (node && node.parentElement);
                while (el) {
                  if (scrollable(el)) {
                    found = true;
                    var max = el.scrollWidth - el.clientWidth;
                    if (el.scrollLeft > EDGE) left = true;
                    if (el.scrollLeft < max - EDGE) right = true;
                  }
                  el = el.parentElement;
                }
                return { found: found, left: left, right: right };
              }
              function send(left, right) {
                if (lastL === left && lastR === right) return;
                lastL = left; lastR = right;
                try { LampsNestedScroll.report(left ? 1 : 0, right ? 1 : 0); } catch (e) {}
              }
              document.addEventListener('touchstart', function (e) {
                var t = e.touches && e.touches[0];
                if (!t) return;
                x0 = t.clientX; y0 = t.clientY; lastL = null; lastR = null;
                var d = room(e.target);
                send(d.left, d.right);
              }, true);
              document.addEventListener('touchmove', function (e) {
                var t = e.touches && e.touches[0];
                if (!t) return;
                var dx = t.clientX - x0, dy = t.clientY - y0;
                if (Math.abs(dy) > Math.abs(dx)) return;
                var d = room(e.target);
                var left = false, right = false;
                if (!d.found && e.defaultPrevented) {
                  left = dx > 0; right = dx < 0;
                } else if (dx > EDGE) {
                  left = d.left;
                } else if (dx < -EDGE) {
                  right = d.right;
                }
                send(left, right);
              }, { passive: true });
            })();
        """.trimIndent()
    }
}
