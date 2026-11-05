import { test, expect } from '@playwright/test';
import { readFileSync, writeFileSync } from 'node:fs';
const initialAdmin = JSON.parse(readFileSync('../backend/config/initial-admin.json', 'utf8'));
const admin = { username: process.env.E2E_ADMIN_USERNAME || initialAdmin.username, password: process.env.E2E_ADMIN_PASSWORD || initialAdmin.password };
const suffix = process.env.E2E_RUN_ID || Date.now().toString(36);
const managed = `e2e_managed_${suffix}`;
const member = `e2e_member_${suffix}`;
const password = 'Browser-test-password-2026!';
writeFileSync('../backend/config/e2e-users.json', JSON.stringify([managed, member]));

async function login(page: import('@playwright/test').Page, username: string, value: string) {
 await page.goto('/#login');
 await page.getByLabel('Email hoặc tên đăng nhập', { exact: true }).fill(username);
 await page.getByLabel('Mật khẩu', { exact: true }).fill(value);
 await page.getByRole('button', { name: 'Đăng nhập', exact: true }).last().click();
}

test('auth layout is readable on desktop and mobile', async ({ page }) => {
 await page.goto('/#login');
 await expect(page.getByRole('heading', { name: 'Chào bạn trở lại 👋' })).toBeVisible();
 await page.screenshot({ path: 'test-results/auth-desktop.png', fullPage: true });
 await page.setViewportSize({ width: 390, height: 844 });
 await expect(page.getByLabel('Email hoặc tên đăng nhập')).toBeVisible();
 expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
 await page.screenshot({ path: 'test-results/auth-mobile.png', fullPage: true });
});

test('admin creates, locks and resets a member using the interface', async ({ page }) => {
 await page.goto('/#login'); await login(page, admin.username, admin.password);
 await expect(page.getByRole('heading', { name: /Chào, / })).toBeVisible();
 await page.goto('/#users');
 await expect(page.getByRole('heading', { name: 'Quản lý tài khoản', exact: true })).toBeVisible();
 await expect(page.getByRole('table')).toBeVisible();
 await page.screenshot({ path: 'test-results/admin-desktop.png', fullPage: true });
 await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
 const dialog = page.getByRole('dialog');
 await dialog.getByLabel('Tên đăng nhập').fill(managed);
 await dialog.getByLabel('Địa chỉ email').fill(`${managed}@example.test`);
 await dialog.getByLabel('Mật khẩu mới', { exact: true }).fill(password);
 await dialog.getByLabel('Xác nhận mật khẩu', { exact: true }).fill(password);
 await dialog.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
 await expect(dialog).not.toBeVisible();
 await page.getByRole('searchbox', { name: 'Tìm tài khoản' }).count().then(async count => { if (count) await page.getByRole('searchbox', { name: 'Tìm tài khoản' }).fill(managed); else await page.getByLabel('Tìm tài khoản').fill(managed); });
 await page.getByRole('button', { name: 'Tìm', exact: true }).click();
 const row = page.getByRole('row').filter({ hasText: `${managed}@example.test` });
 await expect(row).toBeVisible();
 await row.getByRole('button', { name: 'Sửa', exact: true }).click();
 await dialog.getByLabel('Trạng thái').selectOption('LOCKED');
 await dialog.getByRole('button', { name: 'Lưu thay đổi' }).click();
 await expect(dialog).not.toBeVisible(); await expect(row.getByText('Đã khóa', { exact: true })).toBeVisible();
 await row.getByRole('button', { name: 'Mật khẩu', exact: true }).click();
 await dialog.getByLabel('Mật khẩu mới', { exact: true }).fill('Changed-browser-password!');
 await dialog.getByLabel('Xác nhận mật khẩu', { exact: true }).fill('Changed-browser-password!');
 await dialog.getByRole('button', { name: 'Lưu thay đổi' }).click(); await expect(dialog).not.toBeVisible();
 await page.getByRole('button', { name: 'Đăng xuất', exact: true }).last().click();
 await expect(page.getByRole('heading', { name: /Mỗi tài liệu hay/ })).toBeVisible();
 await login(page, managed, 'Changed-browser-password!');
 await expect(page.getByRole('alert')).toContainText('không hoạt động');
});

test('member registers, uses mobile account screen and changes password', async ({ page }) => {
 await page.setViewportSize({ width: 390, height: 844 });
 await page.goto('/#login'); await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).first().click();
 await page.getByLabel('Tên đăng nhập', { exact: true }).fill(member);
 await page.getByLabel('Địa chỉ email').fill(`${member}@example.test`);
 await page.getByLabel('Mật khẩu', { exact: true }).fill(password);
 await page.getByLabel('Xác nhận mật khẩu', { exact: true }).fill(password);
 await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).last().click();
 await expect(page.getByRole('heading', { name: new RegExp(`Chào, ${member}`) })).toBeVisible();
 await expect(page.getByRole('link', { name: 'Quản trị', exact: true })).toHaveCount(0);
 await page.goto('/#account');
 await expect(page.getByRole('heading', { name: 'Tài khoản của tôi', exact: true })).toBeVisible();
 expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
 await page.screenshot({ path: 'test-results/account-mobile.png', fullPage: true });
 await page.getByLabel('Mật khẩu hiện tại', { exact: true }).fill(password);
 await page.getByLabel('Mật khẩu mới', { exact: true }).fill('Member-updated-password!');
 await page.getByLabel('Xác nhận mật khẩu mới', { exact: true }).fill('Member-updated-password!');
 await page.getByRole('button', { name: 'Cập nhật mật khẩu' }).click();
 await expect(page.getByRole('heading', { name: 'Chào bạn trở lại 👋' })).toBeVisible();
 await login(page, member, 'Member-updated-password!');
 await expect(page.getByRole('heading', { name: 'Tài khoản của tôi', exact: true })).toBeVisible();
 await page.getByRole('button', { name: 'Đăng xuất', exact: true }).last().click();
 await expect(page.getByRole('heading', { name: /Mỗi tài liệu hay/ })).toBeVisible();
});
