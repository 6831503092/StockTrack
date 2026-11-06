# คู่มือการพัฒนาโปรเจกต์ StockTrack (Development & Architecture Guide)

> อ้างอิงตามข้อกำหนดใน `SRS_StockTrack_content.md` (Version 2: Multi-Role) และโครงสร้างฐานข้อมูล 16 ตารางใน `StockTrack_schema.dbml`

---

## 1. ภาพรวมสถาปัตยกรรม (Tech Stack & Architecture)

- **Back-end:** Java 17/21 + Spring Boot 3.x (Spring Web, Spring Data JPA, Validation, Lombok)
- **Database:** MySQL / PostgreSQL (หรือ H2 Database สำหรับการทดสอบในเครื่อง) — รวม **16 ตาราง**
- **Front-end:** Vanilla HTML5, CSS3 (Responsive Grid/Flexbox), JavaScript (ES6 `fetch()` API)
- **การเชื่อมต่อ:** สื่อสารผ่าน RESTful API ในรูปแบบ JSON

---

## 2. โครงสร้างโฟลเดอร์ (Folder Structure)

แยกโฟลเดอร์ `frontend/` และ `backend/` ออกจากกันอย่างชัดเจน เพื่อให้โค้ดสะอาด (Clean Code) และดูแลรักษาง่ายตามมาตรฐานสากล

```text
StockTrack/
│
├── frontend/                              # [ฝั่งหน้าบ้าน: HTML + CSS + JS]
│   ├── index.html                         # หน้า Login (UI-01) จุดเริ่มต้นของทุก Role
│   ├── css/
│   │   └── style.css                      # CSS กลาง (คุม Theme, Sidebar, Table, Modal, Responsive)
│   ├── js/
│   │   ├── api.js                         # ฟังก์ชันกลางเรียก fetch() ไปหา Backend
│   │   ├── layout.js                      # สร้าง Sidebar อัตโนมัติตาม Role + ป้องกันการเข้าหน้าข้ามสิทธิ์
│   │   ├── admin.js                       # Logic ของ Admin (สร้าง PO/SO, จัดการสินค้า/โซน, ขอเปลี่ยน Role)
│   │   ├── manager.js                     # Logic ของ Manager (Approve/Reject คำขอ, ตั้ง Order Limits)
│   │   ├── staff.js                       # Logic ของ Staff (ตรวจรับ PO, ตรวจจ่าย SO, ยืนยันย้ายโซน)
│   │   └── dashboard.js                   # Logic ของ Dashboard & Action Report (Owner/Manager)
│   └── pages/
│       ├── owner/
│       │   └── action-report.html         # รายงานประวัติการทำงานทั้งหมด (UI-22 / FR-23)
│       ├── manager/
│       │   ├── dashboard.html             # หน้าสรุปยอดขาย/กำไร/สต็อก (Owner และ Manager ใช้ร่วมกัน - UI-02)
│       │   ├── approval-queue.html        # คิวตรวจอนุมัติคำขอจาก Admin (UI-03 / FR-05)
│       │   └── order-limits.html          # ตั้งค่า Min/Max ต่อบิล และจุดเตือนสต็อกต่ำ (UI-20 / FR-10)
│       ├── admin/
│       │   ├── my-requests.html           # ติดตามสถานะคำขอของตัวเอง (UI-04 / FR-04)
│       │   ├── manage-hub.html            # จัดการสินค้า, โซน, Supplier, Customer, Role (UI-15 ถึง UI-18, UI-21)
│       │   ├── purchase-orders.html       # ตาราง PO + Modal สร้าง PO + ดู Detail (UI-05, UI-06, UI-07)
│       │   └── sales-orders.html          # ตาราง SO + Modal สร้าง SO + ดู Detail (UI-10, UI-11, UI-12)
│       └── staff/
│           ├── receiving.html             # รายการรอตรวจรับ + หน้าตรวจนับรับของ (UI-08, UI-09 / FR-16, FR-17)
│           ├── issuing.html               # รายการรอตรวจจ่าย + หน้าตรวจนับจ่ายของ (UI-13, UI-14 / FR-21, FR-22)
│           └── transfer-verify.html       # ตรวจนับยืนยันการย้ายโซน (UI-19 / FR-12)
│
└── backend/                               # [ฝั่งหลังบ้าน: Java Spring Boot]
    ├── pom.xml
    └── src/main/
        ├── java/com/stocktrack/
        │   ├── StocktrackApplication.java
        │   ├── config/
        │   │   ├── CorsConfig.java        # เปิดให้ frontend เรียกใช้ API ข้ามพอร์ตได้
        │   │   └── DataInitializer.java   # สร้างข้อมูล User 4 Roles และ Zone ตั้งต้นอัตโนมัติ
        │   ├── controller/                # รับ-ส่ง JSON กับหน้าเว็บ (แบ่งเป็น 6 Controller หลัก)
        │   │   ├── AuthController.java          # Login / Logout (FR-01, FR-02)
        │   │   ├── MasterDataController.java    # Products, Zones, Suppliers, Customers, Order Limits, Roles
        │   │   ├── ApprovalController.java      # ดูคำขอ, Approve, Reject (FR-04, FR-05)
        │   │   ├── PurchaseController.java      # PO + Goods Receiving Check (FR-13 ถึง FR-17)
        │   │   ├── SalesController.java         # SO + Goods Issue Check (FR-18 ถึง FR-22)
        │   │   └── ReportController.java        # Dashboard + Action Report (FR-06, FR-23)
        │   ├── service/                   # ประมวลผล Business Logic & Transaction (@Transactional)
        │   │   ├── AuthService.java
        │   │   ├── MasterDataService.java
        │   │   ├── ApprovalService.java         # จัดการคำขออนุมัติของ Admin ทั้งหมด
        │   │   ├── PurchaseService.java
        │   │   ├── SalesService.java
        │   │   └── AuditService.java            # บันทึกลงตาราง AUDIT_LOGS ทุกครั้งที่มีการทำงาน
        │   ├── repository/                # Interfaces สืบทอดจาก JpaRepository (16 ตาราง)
        │   ├── entity/                    # คลาส @Entity 16 คลาส ตรงตาม StockTrack_schema.dbml
        │   └── dto/                       # คลาสรับ-ส่งข้อมูล JSON เฉพาะจุด
        └── resources/
            └── application.properties     # ตั้งค่าการเชื่อมต่อ Database
```

---

## 3. การจัดการหลาย Role (Owner, Manager, Admin, Staff) ให้โค้ด Clean

### 3.1 ตารางสรุปสิทธิ์และหน้าแรกของแต่ละ Role

| Role | หน้าแรกหลัง Login | เมนูที่เข้าถึงได้ | กฎเหล็กตาม SRS |
|:---|:---|:---|:---|
| **OWNER** | `dashboard.html` | Dashboard, Action Report | ดูอย่างเดียว (Read-only) |
| **MANAGER** | `dashboard.html` | Dashboard, Approval Queue, Order Limits, ดู PO/SO | อนุมัติ/ปฏิเสธคำขอของ Admin, ตั้งค่า Min/Max |
| **ADMIN** | `purchase-orders.html` | My Requests, Role Change, Manage (Zones/Products), PO, SO | ทุกการเพิ่ม/แก้/ลบ ต้องสร้างเป็น `Approval Request` เสมอ |
| **STAFF** | `receiving.html` | Receiving, Issuing, Transfer Product | เป็นคนเดียวที่ทำให้ `current_stock` เปลี่ยนจากการนับของจริง |

### 3.2 หลักการเขียนโค้ดจัดการ Role ไม่ให้ซ้ำซ้อน
1. **ใช้ `layout.js` สร้าง Sidebar อัตโนมัติ:**
   - เมื่อ Login สำเร็จ เก็บ `{ userId, fullName, role }` ไว้ใน `sessionStorage`
   - ทุกหน้า HTML เรียกใช้ `<script src="/js/layout.js"></script>` เพื่อตรวจว่าล็อกอินหรือยัง วาดเมนูเฉพาะของ Role นั้น และถ้าเปิดหน้าข้าม Role ให้ดีดกลับหน้าแรกทันที
2. **ตรวจสิทธิ์ซ้ำที่ฝั่ง Server (`NFR-02`):**
   - ใน `api.js` ให้แนบ Header `X-User-Id` ไปกับทุก `fetch()`
   - ใน Service ของ Spring Boot เรียก `authService.requireRole(userId, "MANAGER")` เพื่อป้องกันการยิง API ข้ามสิทธิ์โดยตรง

---

## 4. ลำดับการพัฒนา: ควรทำ Front-end หรือ Back-end ก่อน?

**คำตอบที่ดีที่สุดคือ: ทำฐานข้อมูลแกนกลางก่อน แล้วทำสลับกันทีละฟีเจอร์ (Vertical Slice)**

อย่าทำ Front-end ให้เสร็จทั้งหมด 20 หน้าแล้วค่อยมาทำ Back-end ทีเดียว เพราะถ้ารูปแบบข้อมูล JSON ไม่ตรงกันจะต้องรื้อแก้ใหม่ทั้งหมด แนะนำให้เดินตาม **6 เฟสนี้**:

### เฟสที่ 1: วางโครงสร้างฐานข้อมูลและโปรเจกต์ (Back-end Setup)
1. โหลดโปรเจกต์จาก [start.spring.io](https://start.spring.io/) มาไว้ในโฟลเดอร์ `backend/`
2. ตั้งค่า `application.properties` เชื่อม Database
3. สร้างคลาสใน `entity/` ทั้ง 16 คลาสตาม `StockTrack_schema.dbml` และ `repository/`
4. เขียน `DataInitializer.java` สร้าง User 4 คน (ครบ 4 Roles) และโซน `Z1–Z4` อัตโนมัติ

### เฟสที่ 2: ระบบโครงสร้างหน้าเว็บกลาง + Login (Front-end Core + Back-end Auth)
1. **Front-end:** สร้าง `style.css` (กำหนดสี, ตาราง, ปุ่ม, การย่อหน้าจอ Responsive) และ `layout.js` (โครงสร้าง Sidebar)
2. **Back-end:** สร้าง `AuthController` (`/api/auth/login`)
3. **Front-end:** สร้าง `index.html` เชื่อมกับ API Login ทดสอบล็อกอินทั้ง 4 Roles ว่าแยกไปหน้าของตัวเองถูกต้อง

### เฟสที่ 3: ระบบคำขออนุมัติ (Approval Workflow) + จัดการสินค้าและโซน
1. **Back-end:** สร้าง API ดูสินค้า/โซน และ API ส่งคำขอ (`ApprovalController`, `MasterDataController`)
2. **Front-end:** สร้างหน้า `manage-hub.html`, `my-requests.html` (ของ Admin) และ `approval-queue.html` (ของ Manager)
3. ทดสอบกดขอเพิ่มสินค้าจาก Admin $\rightarrow$ สลับมาล็อกอินเป็น Manager กด Approve $\rightarrow$ สินค้าปรากฏในตาราง

### เฟสที่ 4: สายงานซื้อ (Purchase Order $\rightarrow$ Manager Approve $\rightarrow$ Staff Receiving)
1. **Back-end:** สร้าง `PurchaseController` และ `PurchaseService` (เช็ก PO Min/Max, สร้าง PO, ตรวจรับของและบวกสต็อกด้วย `@Transactional`)
2. **Front-end:** สร้างหน้า `purchase-orders.html` (Admin) และ `receiving.html` (Staff)

### เฟสที่ 5: สายงานขาย (Sales Order $\rightarrow$ Manager Approve $\rightarrow$ Staff Issuing)
1. **Back-end:** สร้าง `SalesController` และ `SalesService` (เช็ก SO Min/Max, เช็กว่าสต็อกพอขายไหม, ตรวจจ่ายของและตัดสต็อก)
2. **Front-end:** สร้างหน้า `sales-orders.html` (Admin) และ `issuing.html` (Staff)

### เฟสที่ 6: หน้า Dashboard และ Action Report
1. **Back-end:** สร้าง `ReportController` คำนวณยอดสต็อก, สินค้าขายดี, กำไรจากบิลที่ `ISSUED` และดึงข้อมูล `AUDIT_LOGS`
2. **Front-end:** สร้างหน้า `dashboard.html` และ `action-report.html`

---

## 5. แนวทางการทำเว็บให้ Responsive รองรับหลายอุปกรณ์ (ตาม NFR-15)

ตาม SRS ข้อ `2.1` และ `NFR-15` ระบบต้องรองรับความกว้างหน้าจอตั้งแต่ **`360px` (มือถือของ Staff ในโกดัง)** ไปจนถึง **`1920px` (จอมอนิเตอร์ของ Admin/Manager)** โดยห้ามมีแถบเลื่อนแนวนอนทั้งหน้าเว็บ

ตั้งกติกากลางในไฟล์ `css/style.css` ตั้งแต่ต้นด้วย 4 เทคนิคนี้:

1. **โครงสร้างหน้าหลัก (Sidebar + Main Content):**
   - **จอคอมพิวเตอร์ (`> 768px`):** แสดง Sidebar อยู่ด้านซ้ายมือ (`width: 90px` หรือ `240px`) และเนื้อหาหลักอยู่ด้านขวา
   - **จอมือถือ (`<= 768px`):** เปลี่ยน Sidebar ให้กลายเป็นแถบเมนูด้านล่างจอ (Bottom Navigation Bar) หรือเมนูพับเก็บ (Hamburger Menu) เพื่อให้เนื้อหาหลักใช้พื้นที่ความกว้างจอได้เต็ม 100%
2. **การ์ดสรุปข้อมูลและโซน (CSS Grid Auto-fit):**
   - ใช้ `display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px;`
   - บนจอคอมจะเรียง 4–5 การ์ดต่อแถว และเมื่อย่อเป็นจอมือถือจะเรียงซ้อนลงมาเป็น 1–2 การ์ดต่อแถวอัตโนมัติโดยไม่ต้องเขียนโค้ดแยกเยอะ
3. **ตารางข้อมูล (Responsive Table Container):**
   - ตารางที่มีหลายคอลัมน์ (เช่น PO List, Product Table) ให้ครอบด้วย `<div class="table-responsive">` ที่ตั้งค่า `overflow-x: auto; width: 100%;`
   - ทำให้บนจอมือถือสามารถปัดเลื่อนดูคอลัมน์ในตารางได้เฉพาะตัวตาราง โดยที่หน้าเว็บหลักไม่ล้นจอ
4. **หน้าต่างป๊อปอัป (Modal):**
   - กำหนดความกว้างด้วย `width: min(92vw, 600px);` เพื่อให้บนคอมกว้าง 600px พอดีตา และบนมือถือหดเหลือ 92% ของหน้าจออัตโนมัติ

