# คู่มือการปรับแต่ง Sidebar และการใส่รูปไอคอน/รูปโปรไฟล์ในแต่ละ Role
*(StockTrack - Sidebar Customization & Image Guide)*

เอกสารฉบับนี้เป็นคู่มือสอนการปรับแต่งแถบเมนูด้านข้าง (Sidebar) ของแต่ละบทบาทผู้ใช้ (`ADMIN`, `MANAGER`, `OWNER`, `STAFF`) รวมถึงวิธีนำไฟล์รูปภาพหรือไอคอนมาใส่ในเมนูและรูปโปรไฟล์อย่างละเอียด

---

## สารบัญ (Table of Contents)
1. [โครงสร้างการทำงานของ Sidebar](#1-โครงสร้างการทำงานของ-sidebar)
2. [ขั้นตอนที่ 1: การจัดเตรียมและวางไฟล์รูปภาพ](#ขั้นตอนที่-1-การจัดเตรียมและวางไฟล์รูปภาพ)
3. [ขั้นตอนที่ 2: วิธีเปลี่ยนไอคอนเมนูเป็นรูปภาพ](#ขั้นตอนที่-2-วิธีเปลี่ยนไอคอนเมนูเป็นรูปภาพ)
4. [ขั้นตอนที่ 3: วิธีเปลี่ยนรูปโปรไฟล์ (Avatar) ของแต่ละ Role](#ขั้นตอนที่-3-วิธีเปลี่ยนรูปโปรไฟล์-avatar-ของแต่ละ-role)
5. [ขั้นตอนที่ 4: วิธีเพิ่ม / ลบ / จัดเรียงเมนูของแต่ละ Role](#ขั้นตอนที่-4-วิธีเพิ่ม--ลบ--จัดเรียงเมนูของแต่ละ-role)
6. [ตัวอย่างโค้ดฉบับสมบูรณ์ (ก่อนและหลังแก้ไข)](#6-ตัวอย่างโค้ดฉบับสมบูรณ์-ก่อนและหลังแก้ไข)
7. [ข้อแนะนำและเทคนิคการแสดงผล (Best Practices)](#7-ข้อแนะนำและเทคนิคการแสดงผล-best-practices)

---

## 1. โครงสร้างการทำงานของ Sidebar

ในโปรเจกต์นี้ แถบเมนูด้านข้างถูกออกแบบเป็น **Modular Component** เพื่อให้แก้ไขที่จุดเดียวแล้วอัปเดตทุกหน้าอัตโนมัติ โดยมีไฟล์ที่เกี่ยวข้องดังนี้:

| ที่อยู่ไฟล์ | หน้าที่ |
| :--- | :--- |
| `src/main/resources/static/js/sidebar.js` | **ไฟล์หลักที่ใช้ตั้งค่าเมนู ไอคอน รูปโปรไฟล์ ของทุก Role** |
| `src/main/resources/static/sidebar.html` | โครงร่าง HTML ต้นแบบของ Sidebar |
| `src/main/resources/static/css/sidebar.css` | สไตล์ตกแต่ง ความกว้าง สี และ Effect ต่างๆ |

---

## ขั้นตอนที่ 1: การจัดเตรียมและวางไฟล์รูปภาพ

ในระบบ Spring Boot โฟลเดอร์ `src/main/resources/static/` คือโฟลเดอร์ Root ของเว็บไซต์ เมื่อเรานำไฟล์ไปวาง จะสามารถเรียกใช้ผ่านเบราว์เซอร์ได้โดยตรง

### 1.1 สร้างโฟลเดอร์เก็บรูป
ให้สร้างโฟลเดอร์สำหรับเก็บรูปภาพไว้ที่:
```
src/main/resources/static/images/
```
*(หากต้องการจัดระเบียบ สามารถแยกโฟลเดอร์ย่อย เช่น `images/icons/` สำหรับไอคอนเมนู และ `images/avatars/` สำหรับรูปโปรไฟล์ได้)*

### 1.2 ตัวอย่างการวางไฟล์รูปภาพ
```
src/main/resources/static/
└── images/
    ├── icons/
    │   ├── dashboard.png
    │   ├── inventory.png
    │   ├── users.png
    │   ├── orders.png
    │   └── delivery.png
    └── avatars/
        ├── admin.png
        ├── manager.png
        ├── owner.png
        └── staff.png
```

---

## ขั้นตอนที่ 2: วิธีเปลี่ยนไอคอนเมนูเป็นรูปภาพ

เปิดไฟล์ **`src/main/resources/static/js/sidebar.js`** และค้นหาตัวแปร `ROLE_CONFIGS`

### 2.1 โค้ดเดิม (ใช้ SVG)
```javascript
{
    id: 'dashboard',
    title: 'Dashboard',
    url: 'admin.html#dashboard',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path></svg>`
}
```

### 2.2 โค้ดใหม่ (เปลี่ยนเป็นแท็ก `<img>`)
ให้แทนที่ค่าใน `icon:` ด้วยแท็ก `<img>` แล้วระบุ Path รูปภาพ:

```javascript
{
    id: 'dashboard',
    title: 'Dashboard',
    url: 'admin.html#dashboard',
    icon: `<img src="/images/icons/dashboard.png" width="22" height="22" alt="Dashboard" style="object-fit: contain;">`
}
```

> **คำอธิบายพารามิเตอร์ของแท็ก `<img>`:**
> - `src="/images/icons/dashboard.png"`: ที่อยู่ไฟล์รูปภาพ (เริ่มด้วย `/` เสมอ)
> - `width="22" height="22"`: ขนาดมาตรฐานที่พอดีกับปุ่มใน Sidebar
> - `style="object-fit: contain;"`: ปรับสัดส่วนรูปภาพไม่ให้บิดเบี้ยว

---

## ขั้นตอนที่ 3: วิธีเปลี่ยนรูปโปรไฟล์ (Avatar) ของแต่ละ Role

ในไฟล์ `sidebar.js` แต่ละบทบาทจะมีฟิลด์ **`avatar:`** กำหนดไว้อยู่แล้ว

### เปลี่ยนจาก URL ภายนอกมาใช้รูปในเครื่อง:
```javascript
const ROLE_CONFIGS = {
    ADMIN: {
        title: 'Admin',
        roleClass: 'role-admin',
        // 👇 เปลี่ยนตรงนี้
        avatar: '/images/avatars/admin.png',
        homeUrl: 'admin.html',
        menus: [ ... ]
    },
    MANAGER: {
        title: 'Manager',
        roleClass: 'role-manager',
        // 👇 เปลี่ยนตรงนี้
        avatar: '/images/avatars/manager.png',
        homeUrl: 'manager.html',
        menus: [ ... ]
    },
    OWNER: {
        title: 'Owner',
        roleClass: 'role-owner',
        // 👇 เปลี่ยนตรงนี้
        avatar: '/images/avatars/owner.png',
        homeUrl: 'owner.html',
        menus: [ ... ]
    },
    STAFF: {
        title: 'Staff',
        roleClass: 'role-staff',
        // 👇 เปลี่ยนตรงนี้
        avatar: '/images/avatars/staff.png',
        homeUrl: 'staff.html',
        menus: [ ... ]
    }
};
```

---

## ขั้นตอนที่ 4: วิธีเพิ่ม / ลบ / จัดเรียงเมนูของแต่ละ Role

ในแต่ละ Role จะมีอาร์เรย์ `menus: [ ... ]` คุณสามารถปรับแต่งรายการเมนูได้อิสระ:

### 4.1 ถ้าต้องการ "ลบเมนู" ออก:
เพียงแค่ลบก้อนอ็อบเจกต์ `{ id: ..., title: ..., url: ..., icon: ... }` ของเมนูนั้นออก

### 4.2 ถ้าต้องการ "เพิ่มเมนูใหม่":
เพิ่มก้อนอ็อบเจกต์ต่อท้ายเข้าไปใน `menus` เช่น เพิ่มเมนู "การตั้งค่า (Settings)" ให้ Admin:

```javascript
{
    id: 'settings',
    title: 'System Settings',
    url: '#settings',
    icon: `<img src="/images/icons/settings.png" width="22" height="22" alt="Settings">`
}
```

---

## 6. ตัวอย่างโค้ดฉบับสมบูรณ์ (ก่อนและหลังแก้ไข)

### ตัวอย่างการปรับแต่ง Role `STAFF`:

```javascript
STAFF: {
    title: 'Staff',
    roleClass: 'role-staff',
    // 1. เปลี่ยนรูปโปรไฟล์
    avatar: '/images/avatars/staff.png',
    homeUrl: 'staff.html',
    menus: [
        // 2. เมนูที่ 1: ภาพรวม
        {
            id: 'dashboard',
            title: 'Dashboard',
            url: 'staff.html#dashboard',
            icon: `<img src="/images/icons/dashboard.png" width="22" height="22" alt="Dashboard">`
        },
        // 3. เมนูที่ 2: ตรวจนับสินค้า
        {
            id: 'products',
            title: 'Product Stock Check',
            url: '#products',
            icon: `<img src="/images/icons/box.png" width="22" height="22" alt="Stock Check">`
        },
        // 4. เมนูที่ 3: เบิกจ่ายและจัดส่ง
        {
            id: 'shipping',
            title: 'Delivery & Picking',
            url: '#shipping',
            icon: `<img src="/images/icons/truck.png" width="22" height="22" alt="Delivery">`
        }
    ]
}
```

---

## 7. ข้อแนะนำและเทคนิคการแสดงผล (Best Practices)

1. **ประเภทไฟล์รูปภาพที่แนะนำ:**
   - **ไอคอนเมนู:** แนะนำให้ใช้ไฟล์ **`.png` พื้นหลังโปร่งใส (Transparent)** หรือไฟล์ **`.svg`** เพื่อไม่ให้มีกรอบสีขาวติดมา
   - **รูปโปรไฟล์:** แนะนำให้ใช้รูปสี่เหลี่ยมจัตุรัส (1:1) เช่น ขนาด 200x200 พิกเซล ตัวระบบจะตัดขอบเป็นวงกลมให้อัตโนมัติ

2. **เมื่อแก้ไขโค้ด JavaScript แล้วรูปไม่เปลี่ยน:**
   - เบราว์เซอร์อาจจะจำ Cache เก่าของไฟล์ `sidebar.js` หรือรูปเดิมไว้
   - **วิธีแก้:** ให้กดปุ่ม **`Ctrl + F5`** (หรือ `Ctrl + Shift + R`) บนคีย์บอร์ดที่หน้าเว็บ เพื่อสั่งโหลดไฟล์ใหม่แบบไม่ใช้ Cache

