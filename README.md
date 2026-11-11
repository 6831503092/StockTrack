# 📦 StockTrack (Project-SRAS)

ระบบจัดการคลังสินค้าและใบสั่งซื้อ (Inventory & Purchase Order Management System)  
พัฒนาด้วย **Spring Boot 3.5 (Java 17)** และ **Vanilla HTML/CSS/JS (Responsive)**

---

## 📚 เอกสารคู่มืออ้างอิงทั้งหมด (Quick Documentation Links)
สามารถคลิกเพื่อเข้าไปอ่านคู่มือฉบับเต็มของแต่ละส่วนได้ที่นี่:
- 📖 [**คู่มือการใช้งาน Git ฉบับละเอียด & วิธีแก้ปัญหาโค้ดชนกัน (`GIT_GUIDE.md`)**](information/GIT_GUIDE.md)
- 🎨 [**คู่มือปรับแต่ง Sidebar และการใส่รูปไอคอนประจำ Role (`SIDEBAR_CUSTOMIZATION_GUIDE.md`)**](information/SIDEBAR_CUSTOMIZATION_GUIDE.md)
- ⚙️ [**เอกสารอธิบายโครงสร้างและ API ระบบหลังบ้าน (`BACKEND_DOCUMENTATION.md`)**](information/BACKEND_DOCUMENTATION.md)

---

## 📑 สารบัญคู่มือการทำงาน (Developer Journey)
> คลิกที่หัวข้อเพื่อวาร์ปไปยังส่วนที่ต้องการได้ทันที:

1. [1. เริ่มต้นจากหน้าเว็บ GitHub: วิธีโคลนโปรเจกต์ลงเครื่อง (Clone & Setup)](#1-เริ่มต้นจากหน้าเว็บ-github-วิธีโคลนโปรเจกต์ลงเครื่อง-clone--setup) 👈 *เริ่มตรงนี้เลย!*
2. [2. การแตก Branch ของตัวเองตาม Role (Git Branching)](#2-การแตก-branch-ของตัวเองตาม-role-git-branching)
3. [3. วิธีเปิดรันโปรเจกต์และบัญชีทดสอบ (Run Server & Test Accounts)](#3-วิธีเปิดรันโปรเจกต์และบัญชีทดสอบ-run-server--test-accounts)
4. [4. การแบ่งหน้าที่และไฟล์ที่ต้องแก้ไข (File Responsibility)](#4-การแบ่งหน้าที่และไฟล์ที่ต้องแก้ไข-file-responsibility)
5. [5. กฎการดีไซน์และโทนสีมาตรฐาน (Design Tokens & Theme)](#5-กฎการดีไซน์และโทนสีมาตรฐาน-design-tokens--theme)
6. [6. วิธีนำรูปไอคอนและรูปโปรไฟล์มาใส่ (Icons & Profile Images)](#6-วิธีนำรูปไอคอนและรูปโปรไฟล์มาใส่-icons--profile-images)
7. [7. วิธีใช้งาน Sidebar และระบบสลับแท็บ (Sidebar & Tab Navigation)](#7-วิธีใช้งาน-sidebar-และระบบสลับแท็บ-sidebar--tab-navigation)
8. [8. วิธีเซฟงาน ส่งขึ้น GitHub และเปิด Pull Request (Save & Submit Work)](#8-วิธีเซฟงาน-ส่งขึ้น-github-และเปิด-pull-request-save--submit-work)

---

## 1. เริ่มต้นจากหน้าเว็บ GitHub: วิธีโคลนโปรเจกต์ลงเครื่อง (Clone & Setup)

สำหรับเพื่อนที่เปิดคอมขึ้นมาและเข้าหน้าเว็บ GitHub เป็นครั้งแรก ให้ทำตามสเต็ปนี้ได้เลยครับ:

### สเต็ปที่ 1.1: กดรับสิทธิ์เป็นทีม (Accept Invitation)
1. เช็คอีเมลของตัวเอง หรือเปิดหน้าแจ้งเตือนบน GitHub จะมีคำเชิญเข้าร่วมโปรเจกต์ **StockTrack**
2. กดปุ่มสีเขียว **"Accept invitation"** (ถ้าไม่กด จะไม่สามารถ Push งานขึ้นได้)
3. เข้าไปยังหน้าโปรเจกต์: [https://github.com/6831503092/StockTrack](https://github.com/6831503092/StockTrack)

### สเต็ปที่ 1.2: คัดลอกลิงก์โปรเจกต์ (Copy URL)
1. ในหน้าแรกของโปรเจกต์ มองหาปุ่มสีเขียวที่เขียนว่า **`<> Code`**
2. แท็บ **HTTPS** ให้คลิกที่ **ไอคอนรูปกระดาษซ้อนกัน (Copy)** เพื่อก๊อปปี้ลิงก์:
   ```text
   https://github.com/6831503092/StockTrack.git
   ```

### สเต็ปที่ 1.3: โคลนโปรเจกต์ลงเครื่องคอมพิวเตอร์
1. เปิดคอม ไปยังโฟลเดอร์ที่คุณต้องการเก็บไฟล์งาน (เช่น `Desktop` หรือ `Documents`)
2. คลิกขวาที่พื้นที่ว่าง เลือก **"Open in Terminal"** (หรือเปิดโปรแกรม `Git Bash`)
3. พิมพ์คำสั่งนี้แล้วกด Enter:
   ```bash
   git clone https://github.com/6831503092/StockTrack.git
   ```
4. รอจนดาวน์โหลดเสร็จ จะมีโฟลเดอร์ชื่อ **`StockTrack`** ปรากฏขึ้นมา

### สเต็ปที่ 1.4: เปิดโปรเจกต์ด้วย VS Code
พิมพ์คำสั่งนี้ใน Terminal เดิมเพื่อเปิดโปรแกรม VS Code ขึ้นมาทันที:
```bash
cd StockTrack
code .
```
*(หรือเปิดโปรแกรม VS Code เอง แล้วไปที่เมนู **File** $\rightarrow$ **Open Folder...** $\rightarrow$ เลือกโฟลเดอร์ `StockTrack`)*

### สเต็ปที่ 1.5: ผูกตัวตน Git (ทำครั้งแรกครั้งเดียว สำหรับคนที่เพิ่งลง Git)
เปิด Terminal ใน VS Code (กด `Ctrl + ` `) แล้วพิมพ์ 2 บรรทัดนี้:
```bash
git config --global user.name "ชื่อภาษาอังกฤษของคุณ"
git config --global user.email "อีเมลที่ใช้สมัคร GitHub"
```

---

## 2. การแตก Branch ของตัวเองตาม Role (Git Branching)

> 📖 *ดูเทคนิคและคำอธิบายเรื่อง Branch เพิ่มเติมได้ที่: [`information/GIT_GUIDE.md`](information/GIT_GUIDE.md)*

เราจะใช้ระบบ **GitHub Flow** โดยห้ามใครเขียนโค้ดลงบนกิ่ง `main` โดยตรง ให้ทุกคนแตก Branch ของตัวเองออกมา:

### 🏷️ กฎการตั้งชื่อ Branch (ขึ้นต้นด้วย `feature/` เสมอ):
- **Admin:** `feature/frontend-admin`
- **Manager:** `feature/frontend-manager`
- **Owner:** `feature/frontend-owner`
- **Staff:** `feature/frontend-staff`

### 🚀 คำสั่งแตกกิ่งของตัวเอง (ใน Terminal ของ VS Code):
```bash
# 1. เช็คว่าอยู่กิ่ง main และดึงของล่าสุด
git checkout main
git pull origin main

# 2. สร้างและสลับไปกิ่งของตัวเอง (เลือกพิมพ์แค่ 1 บรรทัดตามหน้าที่ของตัวเอง)
git checkout -b feature/frontend-staff      # สำหรับคนทำ Staff
# git checkout -b feature/frontend-admin    # สำหรับคนทำ Admin
# git checkout -b feature/frontend-manager  # สำหรับคนทำ Manager
# git checkout -b feature/frontend-owner    # สำหรับคนทำ Owner
```
> 💡 **เช็กก่อนเริ่มพิมพ์โค้ด:** ดูมุมซ้ายล่างสุดของจอ VS Code ต้องขึ้นชื่อกิ่งของตัวเอง (เช่น `feature/frontend-staff`) ห้ามขึ้นว่า `main`

---

## 3. วิธีเปิดรันโปรเจกต์และบัญชีทดสอบ (Run Server & Test Accounts)

โปรเจกต์นี้ตั้งค่า **H2 In-Memory Database** ไว้แล้ว ไม่จำเป็นต้องเปิด XAMPP หรือ MySQL รันได้ทันที (ต้องติดตั้ง **Java 17** ขึ้นไปในเครื่อง)

### 3.1 คำสั่งเปิดรันเซิร์ฟเวอร์
พิมพ์ใน Terminal ของ VS Code:
- **บน Windows:**
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **บน macOS / Linux:**
  ```bash
  ./mvnw spring-boot:run
  ```

เมื่อรันสำเร็จ เข้าใช้งานได้ที่:
- 🌐 **Web Application:** [http://localhost:8080](http://localhost:8080)
- 🗄️ **H2 Database Console:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:stocktrack_db`, User: `sa`, Password: *ว่างไว้*)

### 3.2 บัญชีสำหรับทดสอบ Login (รหัสผ่านคือ `1234` ทุกตำแหน่ง)
ในหน้า Login สามารถกรอกในช่อง **User ID** ด้วย **ตัวเลข ID** หรือ **Username** ก็ได้:

| ตำแหน่ง (Role) | User ID (ตัวเลข) | หรือกรอก Username | Password | หน้า Dashboard ที่ระบบพาไป |
| :--- | :---: | :---: | :---: | :--- |
| **Admin** | `503092` | `admin` | `1234` | `/admin/my-requests.html` |
| **Manager** | `503069` | `manager` | `1234` | `/manager/dashboard.html` |
| **Staff** | `503080` | `staff` | `1234` | `/staff/dashboard.html` |
| **Owner** | `503107` | `owner` | `1234` | `/owner/dashboard.html` |

---

## 4. การแบ่งหน้าที่และไฟล์ที่ต้องแก้ไข (File Responsibility)

ไฟล์ Frontend ทั้งหมดอยู่ที่ `src/main/resources/static/` โดยแบ่งหน้าที่แยกตาม Role ชัดเจน:

| หน้าที่ / Role | ไฟล์ HTML ที่ต้องแก้ไข | ไฟล์ CSS เฉพาะของแต่ละคน |
| :--- | :--- | :--- |
| **Admin** | `src/main/resources/static/admin/my-requests.html` | `src/main/resources/static/css/admin.css` |
| **Manager** | `src/main/resources/static/manager/dashboard.html` | `src/main/resources/static/css/manager.css` |
| **Owner** | `src/main/resources/static/owner/dashboard.html` | `src/main/resources/static/css/owner.css` |
| **Staff** | `src/main/resources/static/staff/dashboard.html` | `src/main/resources/static/css/staff.css` |

> ⚠️ **ข้อตกลงสำคัญในการทำงานร่วมกัน:**
> - **ห้ามแก้ไขไฟล์ส่วนกลาง** ได้แก่ `index.html`, `sidebar.html`, `js/sidebar.js`, `css/style.css`, `css/index.css`, และ `css/sidebar.css` โดยไม่ตกลงกับทีมก่อน เพื่อป้องกัน Git Conflict
> - เขียน CSS ในไฟล์ `css/<role>.css` ของตัวเองเท่านั้น (ถูกนำเข้าสู่ `css/style.css` ให้อัตโนมัติแล้ว)
> - หากต้องการสร้างไฟล์ JS เฉพาะของตัวเอง ให้ตั้งชื่อตาม Role เช่น `js/admin.js`, `js/staff.js`

---

## 5. กฎการดีไซน์และโทนสีมาตรฐาน (Design Tokens & Theme)

เพื่อให้ทุกหน้าในระบบดูเป็นแอปพลิเคชันเดียวกัน **ทุกคนต้องคุมโทนสีให้เหมือนกับหน้า Login (`index.html`)**:
- **ธีมหลัก:** พื้นหลังขาวสะอาด (`#ffffff`) ผสมสีเทาอ่อน (`#f8fafc`) และตัดด้วย **เขียวมิ้นต์ (`#27c690`)**
- **ความโค้งมนและเงา:** การ์ดและกล่องเนื้อหาใช้ `border-radius: 12px; border: 1px solid #eef2f6; box-shadow: 0 1px 3px rgba(0,0,0,0.04);` ส่วนปุ่มและ Input ใช้ `border-radius: 8px` ถึง `10px`
- **ฟอนต์:** ใช้ `'Plus Jakarta Sans'` (ถูกโหลดไว้แล้วในทุกหน้า)

### ตัวแปร CSS กลางที่เรียกใช้ได้ทันที (`css/style.css`):
```css
/* 🌿 สีหลักของระบบ (Mint Green) — ใช้สำหรับปุ่มหลัก, ไฮไลท์, ไอคอน */
var(--primary-color)       /* #27c690 - สีหลัก */
var(--primary-hover)       /* #20b582 - สีเวลาชี้เมาส์ */
var(--primary-active)      /* #1aa072 - สีเวลากด */
var(--primary-light)       /* #e8f8f2 - สีพื้นหลังปุ่ม/ไอคอน/Badge อ่อนๆ */
var(--mint-bg)             /* #edf8f3 - สีพื้นหลังมิ้นต์พาสเทล */

/* ⚪ สีพื้นหลัง เส้นขอบ และตัวอักษร */
var(--text-primary)        /* #1e293b - สีหัวข้อหลัก (กรมท่าเข้ม) */
var(--text-secondary)      /* #64748b - สีคำอธิบายรอง (เทาอมฟ้า) */
var(--text-muted)          /* #94a3b8 - สี Placeholder / ตัวหนังสือจาง */
var(--border-color)        /* #e2e8f0 - สีเส้นขอบ */
var(--border-focus)        /* #27c690 - สีเส้นขอบเมื่อคลิก Input */
var(--bg-white)            /* #ffffff - สีพื้นหลังการ์ด */
var(--bg-page)             /* #f8fafc - สีพื้นหลังหน้าเว็บ */
```

---

## 6. วิธีนำรูปไอคอนและรูปโปรไฟล์มาใส่ (Icons & Profile Images)

### 6.1 การใส่ไอคอนในการ์ด ตาราง หรือปุ่มกด (แนะนำมากที่สุด)
แนะนำให้ใช้ **Inline SVG** (สไตล์เดียวกับ Sidebar) เส้นขนาด `stroke-width="1.8"` ถึง `2`:
- 🌐 ค้นหาและก๊อปปี้โค้ด SVG ฟรี: [Feather Icons](https://feathericons.com/) หรือ [Lucide Icons](https://lucide.dev/icons/)
- **ตัวอย่างโค้ดกล่องไอคอนสีเขียวมิ้นต์:**
  ```html
  <div style="width: 44px; height: 44px; border-radius: 10px; background: var(--primary-light); color: var(--primary-color); display: flex; align-items: center; justify-content: center;">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
      </svg>
  </div>
  ```

### 6.2 การใช้ไฟล์รูปภาพ (`.png`, `.svg`, `.jpg`)
สร้างโฟลเดอร์เก็บไฟล์รูปแยกตาม Role ของตัวเองไว้ที่:
- `src/main/resources/static/<role>/img/` แล้วเรียกใช้ด้วย `<img src="img/my-icon.png">`

### 6.3 วิธีเปลี่ยนไอคอนเมนูบน Sidebar หรือรูป Avatar (`js/sidebar.js`)
> 📖 *ดูคู่มือละเอียดพร้อมตัวอย่างรูปภาพได้ที่: [`SIDEBAR_CUSTOMIZATION_GUIDE.md`](information/SIDEBAR_CUSTOMIZATION_GUIDE.md)*
- `avatar`: เปลี่ยน URL รูปโปรไฟล์ของ Role ตัวเอง
- `menus`: แก้ไขไอคอน `<svg>...</svg>` ของแต่ละเมนูตามต้องการ

---

## 7. วิธีใช้งาน Sidebar และระบบสลับแท็บ (Sidebar & Tab Navigation)

ทุกหน้า Dashboard มีการเรียกใช้ Sidebar อัตโนมัติผ่าน `<script src="../js/sidebar.js"></script>` อยู่แล้ว:

```html
<div id="sidebar-container" data-role="ADMIN" data-active-menu="my-requests"></div>
```

### รูปแบบที่ 1: สลับหน้าแบบ Single-Page (Tab Switching ในไฟล์ `dashboard.html` เดียว)
สร้าง `<section class="role-tab" id="tab-<menu-id>">` ไว้ภายใน `<main class="main-content">` ระบบจะสลับแสดงผลให้อัตโนมัติเมื่อกดเมนู:

```html
<main class="main-content">
    <!-- แท็บแรกแสดงเป็นค่าเริ่มต้น -->
    <section class="role-tab" id="tab-dashboard">
        <h1>Dashboard</h1>
    </section>

    <!-- แท็บอื่นๆ ซ่อนไว้ก่อนด้วย style="display: none;" -->
    <section class="role-tab" id="tab-create-po" style="display: none;">
        <h1>Create Purchase Order</h1>
    </section>
</main>
```

**รายชื่อ `id` ของเมนูแต่ละ Role ใน `sidebar.js`:**
- **ADMIN:** `tab-dashboard`, `tab-create-po`, `tab-my-request`, `tab-users`
- **MANAGER:** `tab-dashboard`, `tab-approvals`, `tab-my-request`
- **OWNER:** `tab-dashboard`, `tab-reports`, `tab-approvals`
- **STAFF:** `tab-dashboard`, `tab-create-po`, `tab-my-request`

### รูปแบบที่ 2: แยกเป็นหลายไฟล์ `.html`
ใส่ `#sidebar-container` แล้วเปลี่ยนค่า `data-active-menu` ให้ตรงกับหน้านั้น:
```html
<div id="sidebar-container" data-role="ADMIN" data-active-menu="users"></div>
```

---

## 8. วิธีเซฟงาน ส่งขึ้น GitHub และเปิด Pull Request (Save & Submit Work)

เมื่อพัฒนาฟีเจอร์ในส่วนของตัวเองเสร็จแล้ว ให้ทำตามขั้นตอนนี้เพื่อส่งงานกลับเข้าสู่กิ่งหลัก:

### สเต็ปที่ 8.1: เซฟงานและส่งขึ้น GitHub
```bash
git add .
git commit -m "feat(staff): ออกแบบตารางตรวจรับสินค้าเรียบร้อย"
git push -u origin feature/frontend-staff
```

### สเต็ปที่ 8.2: รวมโค้ดเข้ากิ่งหลักผ่าน Pull Request (บนเว็บ GitHub)
1. เปิดหน้าเว็บ GitHub: [https://github.com/6831503092/StockTrack](https://github.com/6831503092/StockTrack)
2. คลิกปุ่มสีเขียว **"Compare & pull request"**
3. ตรวจสอบให้ชี้จาก: **`base: main`** $\leftarrow$ **`compare: feature/frontend-<role>`**
4. กด **"Create pull request"** แล้วกด **"Merge pull request"** โค้ดจะรวมเข้าสู่กิ่งหลักทันที

### สเต็ปที่ 8.3: วิธีดึงโค้ดล่าสุดของเพื่อนมาอัปเดตในเครื่อง
```bash
git checkout main
git pull origin main
git checkout feature/frontend-staff
git merge main
```

---

### 🚨 กฎเหล็ก 3 ข้อที่ทุกคนต้องจำ
1. **ห้ามเขียนโค้ดบน `main` โดยตรงเด็ดขาด** ให้ทำในกิ่ง `feature/...` ของตัวเองเสมอ
2. **แก้เฉพาะไฟล์ในโฟลเดอร์ Role และ CSS ของตัวเอง** (`admin/`, `manager/`, `owner/`, `staff/` และ `css/<role>.css`) ตอนกด Merge จะผ่าน 100% โดยไม่ติด Conflict
3. **ถ้าต้องแก้ไฟล์ส่วนกลาง (`js/sidebar.js`, `css/style.css`, `index.html`)** ให้ทักบอกในกลุ่มก่อนทุกครั้ง เพื่อให้เพื่อนดึงโค้ดล่าสุดไปอัปเดตก่อน
