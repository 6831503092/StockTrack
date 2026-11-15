# 📦 StockTrack (Project-SRAS)

ระบบจัดการคลังสินค้า, ใบสั่งซื้อ, ใบสั่งขาย และการอนุมัติแบบบูรณาการ (Enterprise Inventory, Order & Approval Management System)  
พัฒนาด้วย **Spring Boot 3.5.0 (Java 17)** ร่วมกับ **Vanilla HTML5 / Modern CSS / Vanilla JavaScript**

---

## 🏗️ ไฮไลท์สถาปัตยกรรมระบบ (Architecture Highlights)
ระบบได้รับการรีแฟกเตอร์ตามหลัก Clean Architecture และ SOLID Principles อย่างสมบูรณ์:
1. **Declarative Role-Based Security:** กำหนดสิทธิ์ด้วย `@RequireRoles({Role...})` และฉีดผู้ใช้ปัจจุบันด้วย `@CurrentUser User user`
2. **Unified Approval Engine:** ใช้ **Dispatcher Pattern** และ **Template Method Pattern** (`BaseApprovalExecutor`) รองรับคำขอ 12 รูปแบบ พร้อม Typed Payloads และ Transaction Boundary
3. **Standardized Exception Architecture:** คืนค่ารูปแบบเดียวกันด้วย `ApiResponse<T>` ดักจับโดย `GlobalExceptionHandler` รองรับ HTTP 400, 403, 404, 409, 422
4. **Decomposed Domain Services:** แยกบริการเฉพาะทาง เช่น `ReceivingService`, `StockOutService`, `TransferService`, `StockMovementService`, `SupplierService`, `CustomerService`, `ProductQueryService`
5. **Deterministic Audit Logging:** ตาราง `AUDIT_LOGS` บันทึก `target_type` และ `target_id` โดยตรง ควบคุมผ่าน `AuditAction` Enum ปราศจาก Regular Expression ที่เปราะบาง
6. **Frontend Shared Modules:** แยกโมดูลส่วนกลาง `static/js/common/` (`auth.js`, `format.js`, `ui.js`, `table.js`), `static/js/api.js`, และ Design Tokens `static/css/base/` (`tokens.css`, `components.css`)

---

## 📑 สารบัญคู่มือการทำงาน (Developer Journey)
1. [1. การติดตั้งและโคลนโปรเจกต์ลงเครื่อง (Clone & Setup)](#1-การติดตั้งและโคลนโปรเจกต์ลงเครื่อง-clone--setup)
2. [2. การแตก Branch ตาม Role (Git Branching)](#2-การแตก-branch-ตาม-role-git-branching)
3. [3. วิธีเปิดรันเซิร์ฟเวอร์และบัญชีทดสอบ (Run Server & Accounts)](#3-วิธีเปิดรันเซิร์ฟเวอร์และบัญชีทดสอบ-run-server--accounts)
4. [4. โครงสร้างไฟล์และขอบเขตงานตาม Role (File Responsibility)](#4-โครงสร้างไฟล์และขอบเขตงานตาม-role-file-responsibility)
5. [5. กฎการดีไซน์และโทนสีมาตรฐาน (Design Tokens & Theme)](#5-กฎการดีไซน์และโทนสีมาตรฐาน-design-tokens--theme)
6. [6. ระบบ Sidebar และการนำทาง (Sidebar Navigation)](#6-ระบบ-sidebar-และการนำทาง-sidebar-navigation)
7. [7. การทดสอบระบบ (Automated Tests & Quality Checks)](#7-การทดสอบระบบ-automated-tests--quality-checks)
8. [8. วิธีส่งงานขึ้น GitHub (Pull Request & Collaboration)](#8-วิธีส่งงานขึ้น-github-pull-request--collaboration)

---

## 1. การติดตั้งและโคลนโปรเจกต์ลงเครื่อง (Clone & Setup)

1. **คัดลอก Repository URL:**
   ```bash
   git clone https://github.com/6831503092/StockTrack.git
   cd StockTrack
   ```
2. **เปิดด้วย VS Code หรือ IntelliJ IDEA:**
   ```bash
   code .
   ```
3. **ตั้งค่าตัวตน Git (ทำครั้งแรกครั้งเดียว):**
   ```bash
   git config --global user.name "Your Name"
   git config --global user.email "your.email@example.com"
   ```

---

## 2. การแตก Branch ตาม Role (Git Branching)

เราใช้ **GitHub Flow** โดยห้ามคอมมิตลงบนกิ่ง `main` โดยตรง ให้ทุกคนแตก Branch ประจำ Role ของตัวเอง:
- **Admin:** `feature/frontend-admin`
- **Manager:** `feature/frontend-manager`
- **Owner:** `feature/frontend-owner`
- **Staff:** `feature/frontend-staff`

```bash
git checkout main
git pull origin main
git checkout -b feature/frontend-staff   # ตัวอย่างสำหรับทีม Staff
```

---

## 3. วิธีเปิดรันเซิร์ฟเวอร์และบัญชีทดสอบ (Run Server & Accounts)

ระบบมาพร้อมกับ **H2 In-Memory Database** ไม่จำเป็นต้องติดตั้ง MySQL ก็สามารถรันได้ทันที (ต้องการ **Java 17** ขึ้นไป):

### 3.1 คำสั่งเปิดเซิร์ฟเวอร์
- **บน Windows:**
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **บน macOS / Linux:**
  ```bash
  ./mvnw spring-boot:run
  ```

### 3.2 URL เข้าใช้งาน
- 🌐 **Web Application:** [http://localhost:8080](http://localhost:8080)
- 🗄️ **H2 Database Console:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)  
  *(JDBC URL: `jdbc:h2:mem:stocktrack_db` | Username: `sa` | Password: ว่างไว้)*

### 3.3 บัญชีสำหรับทดสอบ Login (รหัสผ่านคือ `1234` ทุกตำแหน่ง)
| ตำแหน่ง (Role) | User ID (ตัวเลข) | หรือกรอก Username | Password | หน้าเริ่มต้นที่ระบบพาไป |
| :--- | :---: | :---: | :---: | :--- |
| **Admin** | `503092` | `admin` | `1234` | `/admin/my-requests.html` |
| **Manager** | `503069` | `manager` | `1234` | `/manager/dashboard.html` |
| **Staff** | `503080` | `staff` | `1234` | `/staff/dashboard.html` |
| **Owner** | `503107` | `owner` | `1234` | `/owner/dashboard.html` |

---

## 4. โครงสร้างไฟล์และขอบเขตงานตาม Role (File Responsibility)

ไฟล์ฝั่ง Frontend ทั้งหมดอยู่ที่ไดเรกทอรี `src/main/resources/static/`:

| หน้าที่ / Role | หน้าจอ HTML (`static/<role>/`) | สคริปต์ JavaScript (`static/js/`) | ไฟล์ CSS (`static/css/`) |
| :--- | :--- | :--- | :--- |
| **Admin** | `my-requests.html`<br>`product-management.html`<br>`purchase-order.html`<br>`role-management.html`<br>`sales-order.html`<br>`warehouse-zones.html`<br>`zone-product.html` | `admin/admin.js`<br>`admin/role-management.js` | `pages/admin.css` |
| **Manager** | `dashboard.html`<br>`requests.html` | `manager/manager-dashboard.js`<br>`manager/manager-requests.js`<br>`manager/manager.js` (Facade) | `pages/manager.css` |
| **Staff** | `dashboard.html`<br>`receive-items.html`<br>`stock-out.html`<br>`transfer-items.html` | `staff/staff-dashboard.js`<br>`staff/receive-items.js`<br>`staff/stock-out.js`<br>`staff/transfer-items.js`<br>`staff/staff-wizard.js` | `pages/staff.css` |
| **Owner** | `dashboard.html`<br>`activity-log.html` | `owner/owner-dashboard.js`<br>`owner/owner-activity.js` | `pages/owner.css` |
| **Shared** | `index.html` (Login) | `common/api.js`<br>`common/auth.js`<br>`common/format.js`<br>`common/ui.js`<br>`common/table.js`<br>`common/sidebar.js`<br>`common/sidebar-config.js` | `base/tokens.css`<br>`base/components.css`<br>`layout/sidebar.css`<br>`style.css` |

---

## 5. กฎการดีไซน์และโทนสีมาตรฐาน (Design Tokens & Theme)

เพื่อให้ทุกหน้ามีสไตล์ที่เป็นอันหนึ่งอันเดียวกัน ระบบใช้ Design Tokens ใน `static/css/base/tokens.css` และส่วนประกอบกลางใน `static/css/base/components.css`:
- **ธีมสีหลัก:** พื้นหลังขาวสะอาด (`#ffffff`) สลับเทาอ่อน (`#f8fafc`) เน้นด้วยสีเขียวมิ้นต์ (`#27c690`)
- **ความโค้งมน:** กล่องเนื้อหาและตารางใช้ `border-radius: 12px; border: 1px solid #eef2f6;`
- **ฟอนต์มาตรฐาน:** `'Plus Jakarta Sans'`

### CSS Variables สำคัญ:
```css
var(--primary-color)    /* #27c690 เขียวมิ้นต์หลัก */
var(--primary-hover)    /* #20b582 สีเมื่อ Hover */
var(--primary-light)    /* #e8f8f2 พื้นหลังพาสเทลสำหรับ Badge/Icon */
var(--text-primary)     /* #1e293b กรมท่าเข้มสำหรับหัวข้อ */
var(--text-secondary)   /* #64748b ข้อความรอง */
var(--border-color)     /* #e2e8f0 เส้นขอบการ์ด/ตาราง */
```

---

## 6. ระบบ Sidebar และการนำทาง (Sidebar Navigation)

ทุกหน้า Dashboard เชื่อมโยงกับ Sidebar อัตโนมัติผ่านแท็ก:
```html
<div id="sidebar-container" data-role="ADMIN" data-active-menu="my-requests"></div>
<script src="../js/common/sidebar-config.js"></script>
<script src="../js/common/sidebar.js"></script>
```
คอนฟิกเมนูและไอคอน SVG ถูกแยกเป็นระเบียบไว้ที่ `static/js/common/sidebar-config.js` ทำให้ปรับแต่งและขยายเมนูใหม่ได้ง่ายโดยไม่กระทบโครงสร้างหลัก

---

## 7. การทดสอบระบบ (Automated Tests & Quality Checks)

ระบบมีชุดการทดสอบทั้งระดับ Unit, Controller Security, และ End-to-End Integration:

### 7.1 คำสั่งคอมไพล์และทดสอบ
```bash
# คอมไพล์ซอร์สโค้ดและชุดทดสอบทั้งหมด
.\mvnw.cmd test-compile

# รันชุดการทดสอบทั้งหมด
.\mvnw.cmd test
```

### 7.2 ไฮไลท์ของชุดทดสอบ
1. **Controller Role Security (`SecurityRoleAccessControllerTest`):**
   - ใช้ `@WebMvcTest` ยืนยันว่าการส่งคำขอข้ามสิทธิ์ (เช่น Staff ยิงคำขออนุมัติ หรือแก้ไขบทบาทผู้ใช้) จะถูกปฏิเสธด้วย HTTP `403 Forbidden` เสมอ
2. **Order Lifecycle Integration (`OrderLifecycleIntegrationTest`):**
   - ทดสอบโฟลว์ `PO -> Approve -> Receive` และตรวจสอบการเพิ่มของยอดสต็อกในตาราง `products`
   - ทดสอบโฟลว์ `SO -> Approve -> Stock-out` และตรวจสอบการลดลงของยอดสต็อกอย่างถูกต้อง
3. **Approval Engine Unit Tests (`service/approval/*Test.java`):**
   - ทดสอบตรรกะของ Executor แต่ละตัวอย่างเข้มข้น

---

## 8. วิธีส่งงานขึ้น GitHub (Pull Request & Collaboration)

1. **คอมมิตและพุชขึ้น Branch ของตนเอง:**
   ```bash
   git add .
   git commit -m "feat(role): รายละเอียดงานที่ทำ"
   git push -u origin feature/frontend-<role>
   ```
2. **เปิด Pull Request บน GitHub:**
   - ตรวจสอบให้ Base Branch เป็น `main` และ Compare Branch เป็นกิ่งของตนเอง
   - กด **Create pull request** เพื่อให้ทีมตรวจสอบและรวมโค้ด

> ⚠️ **ข้อควรระวัง:** ปรึกษาทีมก่อนแก้ไขไฟล์ส่วนกลาง (`api.js`, `sidebar.js`, `tokens.css`) เพื่อหลีกเลี่ยงความขัดแย้งของโค้ด (Git Conflict)
