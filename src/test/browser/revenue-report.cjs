// Run against a fresh test-profile server (H2), using the DataSeeder accounts.
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
        const totals = () => page.locator('main .fs-3').allTextContents();
        const range = async (start, end) => {
            await page.locator('[name="startDate"]').fill(start);
            await page.locator('[name="endDate"]').fill(end);
            await Promise.all([page.waitForNavigation(), page.getByRole('button', { name: 'Xem báo cáo' }).click()]);
        };
        await page.goto('/admin/reports');
        assert.ok(page.url().endsWith('/login'));
        await page.locator('#username').fill('admin@fucinema.vn');
        await page.locator('#password').fill('Admin@123');
        await Promise.all([page.waitForURL(/\/admin(?:;jsessionid=[^?]+)?$/), page.locator('main button.btn-primary').click()]);
        await page.getByRole('link', { name: 'Báo cáo', exact: true }).click();
        const today = await page.locator('[name="endDate"]').inputValue();
        assert.equal(await page.locator('[name="startDate"]').inputValue(), today.slice(0, 8) + '01');
        // Include all seed dates even when this script runs in the first days of a month.
        await range('2000-01-01', today);
        assert.deepEqual(await totals(), ['3', '6', '440,000đ']);
        const tables = page.locator('main table');
        assert.deepEqual(await tables.nth(0).locator('tbody tr td:nth-child(2)').allTextContents(),
            ['Chú Mèo Phiêu Lưu', 'Huyền Thoại Cũ', 'Biệt Đội Siêu Tốc']);
        assert.deepEqual(await tables.nth(1).locator('tbody tr td:first-child').allTextContents(), ['3', '2', '1']);
        await page.waitForFunction(() => getComputedStyle(document.querySelector('.container')).maxWidth === '1320px');
        await capture('01-revenue-report.png');
        await tables.nth(1).locator('tbody a').first().click();
        assert.ok(page.url().endsWith('/admin/bookings/3'));
        assert.ok((await page.locator('main').innerText()).includes('CONFIRMED'));

        const date = new Date(today + 'T12:00:00Z');
        date.setUTCDate(date.getUTCDate() - 1);
        const yesterday = date.toISOString().slice(0, 10);
        await page.goto('/admin/reports');
        await range(yesterday, yesterday);
        assert.deepEqual(await totals(), ['1', '3', '210,000đ']);
        await capture('02-single-day.png');
        const lastYear = Number(today.slice(0, 4)) - 1;
        await range(`${lastYear}-01-01`, `${lastYear}-12-31`);
        assert.ok((await page.locator('main').innerText()).includes('Không có dữ liệu trong khoảng thời gian đã chọn.'));
        assert.equal(await page.locator('main table').count(), 0);
        assert.deepEqual(await totals(), []);
        await capture('03-empty-period.png');
        await range('2000-01-02', '2000-01-01');
        assert.ok((await page.getByRole('alert').innerText()).includes('Từ ngày phải trước hoặc bằng đến ngày'));
        assert.equal(await page.locator('[name="startDate"]').inputValue(), '2000-01-02');
        await capture('04-invalid-period.png');
        await page.goto(`/admin/reports?startDate=2000-01-01`);
        assert.equal(await page.locator('[name="endDate"]').inputValue(), today);

        const plainContext = await browser.newContext({ baseURL, javaScriptEnabled: false, storageState: await context.storageState() });
        const plain = await plainContext.newPage();
        await plain.goto(`/admin/reports?startDate=${yesterday}&endDate=${yesterday}`);
        assert.deepEqual(await plain.locator('main .fs-3').allTextContents(), ['1', '3', '210,000đ']);
        const customer = await browser.newContext({ baseURL });
        assert.equal((await customer.request.post('/login', {
            form: { username: 'an.nv@gmail.com', password: 'Customer@123' }, maxRedirects: 0
        })).status(), 302);
        assert.equal((await customer.request.get('/admin/reports')).status(), 403);
        assert.deepEqual(errors, []);
        console.log('PASS: HuongDan.md #17 — month defaults, CONFIRMED-only totals 3 / 6 / 440,000đ, movie/order sorting, detail link, same-day filter, last-year empty period, reversed-date error, missing-date defaults, no-JS rendering, admin-only access.');
        console.log(`Evidence: ${output}`);
    } finally {
        await browser.close();
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
