// src/main/java/com/onmarket/cardnews/service/HtmlRenderService.java
package com.onmarket.cardnews.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import org.springframework.stereotype.Service;
import com.microsoft.playwright.options.WaitUntilState; // ✅ 추가

@Service
public class HtmlRenderService {

    public byte[] renderToPngBytes(String html, int width, int height, int dsf) {
        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext ctx = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(width, height)
                    .setDeviceScaleFactor((double) dsf)
            );
            Page page = ctx.newPage();

            page.setContent(html, new Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE) // ✅ 올바른 enum 사용
            );

            // 혹시 폰트/이미지 로딩 지연시 약간 대기
            page.waitForTimeout(200);

            byte[] png = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(false)
                    .setType(ScreenshotType.PNG)
            );

            ctx.close();
            browser.close();
            return png;
        }
    }
}