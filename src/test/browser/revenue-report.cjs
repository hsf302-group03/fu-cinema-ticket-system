// Run against a fresh test-profile server (H2), never a production database.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');

(async () => {
    const baseURL = process.env.BASE_URL || 'http://localhost:8086';
    const output = path.resolve(process.env.EVIDENCE_DIR || 'docs/screenshots/issue-17');
    await fs.mkdir(output, { recursive: true });
    const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined });
    try {
        const context = await browser.newContext({ baseURL, locale: 'vi-VN', viewport: { width: 1440, height: 1080 } });
        const page = await context.newPage();
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        const capture = name => page.screenshot({ path: path.join(output, name), fullPage: true });
        await page.goto('/admin/reports');
        assert.ok(page.url().endsWith('/login'));
        await page.locator('#username').fill('admin@fucinema.vn');
        await page.locator('#password').fill('Admin@123');
        await Promise.all([page.waitForURL(/\/admin(?:;jsessionid=[^?]+)?$/), page.locator('main button[type="submit"], main button.btn-primary').click()]);
        await page.getByRole('link', { name: 'Báo cáo', exact: true }).click();
        const today = await page.locator('#endDate').inputValue();
        assert.equal(await page.locator('#startDate').inputValue(), today.slice(0, 8) + '01');
        const range = async (start, end) => {
            await page.getByLabel('Từ ngày', { exact: true }).fill(start);
            await page.getByLabel('Đến ngày', { exact: true }).fill(end);
            await Promise.all([page.waitForNavigation(), page.getByRole('button', { name: 'Xem báo cáo' }).click()]);
        };
        await range('2000-01-01', today);
        assert.equal(await page.locator('#total-bookings').innerText(), '3');
        assert.equal(await page.locator('#total-tickets').innerText(), '6');
        assert.equal(await page.locator('#total-revenue').innerText(), '440.000 đ');
        assert.deepEqual(await page.locator('#movie-revenue tbody tr td:first-child').allTextContents(),
            ['Chú Mèo Phiêu Lưu', 'Huyền Thoại Cũ', 'Biệt Đội Siêu Tốc']);
        assert.deepEqual(await page.locator('#confirmed-bookings tbody tr td:first-child').allTextContents(), ['#3', '#2', '#1']);
        assert.equal(await page.locator('#confirmed-bookings tfoot').innerText(), 'Tổng cộng: 3 đơn\t6\t440.000 đ');
        // The stylesheet is an existing shared CDN dependency; ensure evidence captures the real layout.
        await page.waitForFunction(() => getComputedStyle(document.querySelector('.container')).maxWidth === '1320px');
        const typeSizes = await page.evaluate(() => ['h1', 'h2', 'p'].map(tag => getComputedStyle(document.querySelector(tag)).fontSize));
        assert.deepEqual(typeSizes, ['28px', '20px', '16px']);
        await capture('01-revenue-report.png');
        await page.locator('#confirmed-bookings tbody a').first().click();
        assert.ok(page.url().endsWith('/admin/bookings/3'));
        assert.ok((await page.locator('main').innerText()).includes('CONFIRMED'));

        const date = new Date(today + 'T12:00:00Z');
        date.setUTCDate(date.getUTCDate() - 1);
        const yesterday = date.toISOString().slice(0, 10);
        await page.goto('/admin/reports');
        await range(yesterday, yesterday);
        assert.equal(await page.locator('#total-bookings').innerText(), '1');
        assert.equal(await page.locator('#total-tickets').innerText(), '3');
        assert.equal(await page.locator('#total-revenue').innerText(), '210.000 đ');
        await capture('02-single-day.png');

        await range('2000-01-01', '2000-01-02');
        assert.ok((await page.getByRole('status').innerText()).includes('Không có dữ liệu'));
        assert.equal(await page.locator('#total-revenue').innerText(), '0 đ');
        await capture('03-empty-period.png');
        await range('2000-01-02', '2000-01-01');
        assert.ok((await page.getByRole('alert').innerText()).includes('Từ ngày phải trước hoặc bằng đến ngày'));
        assert.equal(await page.locator('#total-revenue').count(), 0);
        assert.equal(await page.locator('#startDate').inputValue(), '2000-01-02');
        await capture('04-invalid-period.png');
        for (const query of ['startDate=bad&endDate=2000-01-02', 'startDate=&endDate=2000-01-02', 'startDate=2000-01-01']) {
            const response = await page.goto('/admin/reports?' + query);
            assert.equal(response.status(), 200);
            assert.equal(await page.getByRole('alert').count(), 1);
        }
        await page.goto(`/admin/reports?startDate=2000-01-01&endDate=${today}`);
        await page.setViewportSize({ width: 390, height: 844 });
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
        const scroller = page.locator('[aria-labelledby="booking-heading"]');
        assert.equal(await page.locator('#booking-scroll-help').isVisible(), true);
        assert.equal(await page.locator('#confirmed-bookings').evaluate(element => getComputedStyle(element).whiteSpace), 'nowrap');
        assert.equal(await scroller.evaluate(element => element.scrollWidth > element.clientWidth), true);
        await scroller.focus();
        await page.keyboard.press('End');
        await scroller.evaluate(element => { element.scrollLeft = element.scrollWidth; });
        assert.equal(await scroller.evaluate(element => element.scrollLeft > 0), true);
        await scroller.evaluate(element => { element.scrollLeft = 0; });
        await page.locator('h1').click();
        await capture('05-mobile-report.png');

        const plainContext = await browser.newContext({ baseURL, javaScriptEnabled: false, storageState: await context.storageState() });
        const plain = await plainContext.newPage();
        await plain.goto(`/admin/reports?startDate=2000-01-01&endDate=${today}`);
        assert.equal(await plain.locator('#total-revenue').innerText(), '440.000 đ');
        await plain.getByLabel('Từ ngày', { exact: true }).fill(yesterday);
        await plain.getByLabel('Đến ngày', { exact: true }).fill(yesterday);
        await Promise.all([plain.waitForNavigation(), plain.getByRole('button', { name: 'Xem báo cáo' }).click()]);
        assert.equal(await plain.locator('#total-revenue').innerText(), '210.000 đ');
        const customer = await browser.newContext({ baseURL });
        const login = await customer.request.post('/login', {
            form: { username: 'an.nv@gmail.com', password: 'Customer@123' }, maxRedirects: 0
        });
        assert.equal(login.status(), 302);
        assert.equal((await customer.request.get('/admin/reports')).status(), 403);
        assert.deepEqual(errors, []);
        console.log('PASS: admin-only access, current-month defaults, movie/order sorting, totals 3 / 6 / 440.000 đ, booking detail link, same-day filter, empty, reversed/malformed/missing dates, mobile overflow, no-JS filter.');
        console.log(`Evidence: ${output}`);
    } finally {
        await browser.close();
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
