// Run against a fresh test-profile server with the standard DataSeeder accounts.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');

(async () => {
    const baseURL = process.env.BASE_URL || 'http://localhost:8085';
    const output = path.resolve(process.env.EVIDENCE_DIR || 'docs/screenshots/issue-14');
    await fs.mkdir(output, { recursive: true });
    const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined });
    try {
        const adult = await browser.newContext({ baseURL, viewport: { width: 1440, height: 1080 } });
        const younger = await browser.newContext({ baseURL, viewport: { width: 1440, height: 1080 } });
        const page = await adult.newPage();
        const stale = await younger.newPage();
        const errors = [];
        for (const tab of [page, stale]) tab.on('pageerror', error => errors.push(error.message));
        const login = async (tab, email) => {
            const response = await tab.context().request.post('/login', {
                form: { username: email, password: 'Customer@123' }, maxRedirects: 0
            });
            assert.equal(response.status(), 302);
        };
        const screenshot = (tab, name) => tab.screenshot({ path: path.join(output, name), fullPage: true });
        await login(page, 'an.nv@gmail.com');
        await login(stale, 'binh.tt@gmail.com');
        await page.goto('/movies/1');
        const seatsUrl = await page.locator('a[href$="/seats"]').first().getAttribute('href');
        assert.ok(seatsUrl);
        await page.goto(seatsUrl);
        await stale.goto(seatsUrl); // Second customer keeps an old, available seat map.
        assert.equal(await page.locator('#seat-E5').isDisabled(), true);
        assert.equal(await page.locator('#seat-E6').isDisabled(), true);
        assert.equal(await page.locator('#seat-F1').isDisabled(), false); // CANCELLED seed booking.
        assert.equal(await page.locator('#confirm-booking').isDisabled(), true);
        await screenshot(page, '01-seat-map.png');
        // Checkboxes are visually hidden but keyboard-accessible; click the public labels.
        await page.locator('label[for="seat-F1"]').click();
        await page.locator('label[for="seat-F2"]').click();
        assert.equal(await page.locator('#booking-total').innerText(), '160.000 đ');
        await screenshot(page, '02-multiple-seats-total.png');
        await stale.locator('label[for="seat-F1"]').click();
        await Promise.all([page.waitForURL(/\/confirmation$/), page.getByRole('button', { name: 'Xác nhận đặt vé' }).click()]);
        assert.ok((await page.locator('main').innerText()).includes('160.000 đ'));
        await screenshot(page, '03-booking-success.png');
        await Promise.all([stale.waitForNavigation(), stale.getByRole('button', { name: 'Xác nhận đặt vé' }).click()]);
        assert.ok((await stale.locator('[role="alert"]').innerText()).includes('Ghế F1 đã bán'));
        assert.equal(await stale.locator('#seat-F1').isDisabled(), true);
        await screenshot(stale, '04-stale-seat-conflict.png');
        await stale.goto('/movies/3');
        await stale.locator('a[href$="/seats"]').first().click();
        await stale.locator('label[for="seat-A1"]').click();
        await Promise.all([stale.waitForNavigation(), stale.getByRole('button', { name: 'Xác nhận đặt vé' }).click()]);
        assert.ok((await stale.locator('[role="alert"]').innerText()).includes('chưa đủ tuổi'));
        await screenshot(stale, '05-age-validation.png');
        await page.goto(seatsUrl);
        for (let n = 1; n <= 9; n++) await page.locator(`label[for="seat-A${n}"]`).click();
        assert.equal(await page.locator('input[name="seatCodes"]:checked').count(), 8);
        assert.ok((await page.locator('#seat-help').innerText()).includes('8 ghế'));
        const mobile = await adult.newPage();
        await mobile.setViewportSize({ width: 390, height: 844 });
        await mobile.goto(seatsUrl);
        await mobile.locator('label[for="seat-A1"]').click();
        assert.equal(await mobile.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true);
        await screenshot(mobile, '06-mobile-seat-map.png');
        // The seat map remains usable when JavaScript is disabled.
        const noJs = await browser.newContext({ baseURL, javaScriptEnabled: false, storageState: await adult.storageState() });
        const plain = await noJs.newPage();
        await plain.goto(seatsUrl);
        await plain.locator('label[for="seat-H1"]').click();
        await Promise.all([plain.waitForURL(/\/confirmation$/), plain.getByRole('button', { name: 'Xác nhận đặt vé' }).click()]);
        assert.ok((await plain.locator('main').innerText()).includes('80.000 đ'));
        assert.deepEqual(errors, []);
        console.log('PASS: sold/cancelled seats, multi-seat total, success, stale conflict, age limit, max 8, mobile overflow, no-JS booking.');
        console.log(`Screenshots saved in ${output}`);
    } finally {
        await browser.close();
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
