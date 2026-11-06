# เอกสารอธิบายโครงสร้างและโค้ดระบบหลังบ้าน (Spring Boot Backend Documentation)

เอกสารฉบับนี้อธิบายโครงสร้างสถาปัตยกรรมและโค้ดภายในโปรเจกต์ **StockTrack (Project-SRAS)** อย่างละเอียด ครอบคลุมทั้ง 5 โฟลเดอร์หลัก ได้แก่ `config`, `controller`, `entity`, `enums`, และ `repository`

---

## สารบัญ (Table of Contents)
1. [ภาพรวมสถาปัตยกรรม (Architecture Overview)](#1-ภาพรวมสถาปัตยกรรม-architecture-overview)
2. [โฟลเดอร์ `config` (การตั้งค่าระบบและข้อมูลเริ่มต้น)](#2-โฟลเดอร์-config-การตั้งค่าระบบและข้อมูลเริ่มต้น)
   - [`DataInitializer.java`](#21-ไฟล์-datainitializerjava)
3. [โฟลเดอร์ `controller` (ส่วนควบคุมและรับ-ส่ง API)](#3-โฟลเดอร์-controller-ส่วนควบคุมและรับ-ส่ง-api)
   - [`AuthController.java`](#31-ไฟล์-authcontrollerjava)
4. [โฟลเดอร์ `enums` (ชุดค่าคงที่)](#4-โฟลเดอร์-enums-ชุดค่าคงที่)
   - [`Role.java`](#41-ไฟล์-rolejava)
5. [โฟลเดอร์ `entity` (โครงสร้างตารางฐานข้อมูล)](#5-โฟลเดอร์-entity-โครงสร้างตารางฐานข้อมูล)
   - [`User.java`](#51-ไฟล์-userjava-ตาราง-users)
   - [`Zone.java`](#52-ไฟล์-zonejava-ตาราง-zones)
   - [`Product.java`](#53-ไฟล์-productjava-ตาราง-products)
6. [โฟลเดอร์ `repository` (ตัวจัดการคำสั่งฐานข้อมูล)](#6-โฟลเดอร์-repository-ตัวจัดการคำสั่งฐานข้อมูล)
   - [`UserRepository.java`](#61-ไฟล์-userrepositoryjava)
7. [สรุปขั้นตอนการทำงานร่วมกันทั้งหมด (System Workflow)](#7-สรุปขั้นตอนการทำงานร่วมกันทั้งหมด-system-workflow)

---

## 1. ภาพรวมสถาปัตยกรรม (Architecture Overview)

โปรเจกต์นี้ออกแบบตามมาตรฐาน **Spring Boot Layered Architecture** โดยแบ่งหน้าที่ของแต่ละโฟลเดอร์อย่างชัดเจนเพื่อง่ายต่อการดูแลรักษาและขยายระบบในอนาคต:

```
src/main/java/com/sras/backend/
├── BackendApplication.java      # จุดเริ่มต้นการทำงานของระบบ (Main Class)
├── config/
│   └── DataInitializer.java     # สร้างข้อมูลผู้ใช้งานเริ่มต้นลงฐานข้อมูลอัตโนมัติ
├── controller/
│   └── AuthController.java      # รับ-ส่ง API สำหรับการ Login และ Logout
├── entity/
│   ├── User.java                # ตารางผู้ใช้งาน (users)
│   ├── Zone.java                # ตารางโซนคลังสินค้า (zones)
│   └── Product.java             # ตารางสินค้าและสต็อก (products)
├── enums/
│   └── Role.java                # ค่าคงที่ระดับสิทธิ์ผู้ใช้งาน (OWNER, MANAGER, ADMIN, STAFF)
└── repository/
    └── UserRepository.java      # ตัวจัดการคำสั่งค้นหาและบันทึกข้อมูลในตาราง users
```

---

## 2. โฟลเดอร์ `config` (การตั้งค่าระบบและข้อมูลเริ่มต้น)

**หน้าที่ของโฟลเดอร์ `config`:**  
ใช้เก็บคลาสที่ทำหน้าที่ตั้งค่าระบบ (Configuration) และเตรียมความพร้อมของข้อมูล (Data Initialization / Seeding) ก่อนที่ระบบจะเปิดให้ผู้ใช้งานเข้าถึง

### 2.1 ไฟล์ `DataInitializer.java`
**ที่อยู่ไฟล์:** `src/main/java/com/sras/backend/config/DataInitializer.java`  
**หน้าที่:** ทำหน้าที่เป็น **Data Seeder** คอยตรวจสอบและสร้างบัญชีผู้ใช้เริ่มต้น (`admin` และ `manager`) ลงในฐานข้อมูลโดยอัตโนมัติทุกครั้งที่รันโปรแกรม เพื่อให้พร้อมทดสอบระบบได้ทันที (โดยเฉพาะเมื่อใช้ H2 In-Memory Database ที่ข้อมูลจะถูกล้างใหม่ทุกครั้งที่ปิดโปรแกรม)

#### โค้ดฉบับเต็ม:
```java
package com.sras.backend.config;

import com.sras.backend.entity.User;
import com.sras.backend.enums.Role;
import com.sras.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        seedUserIfMissing("admin", "Admin User", "1234", Role.ADMIN);
        seedUserIfMissing("manager", "Manager User", "1234", Role.MANAGER);
    }

    private void seedUserIfMissing(String username, String fullName, String rawPassword, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User();
            user.setUsername(username);
            user.setFullName(fullName);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setIsActive(true);
            userRepository.save(user);
        }
    }
}
```

#### อธิบายการทำงานทีละส่วน:
1. **`@Component`**: บอกให้ Spring Boot รู้จักคลาสนี้และนำไปจัดการใน Spring Container แบบอัตโนมัติ (Spring Bean)
2. **`@RequiredArgsConstructor`**: Annotation จากไลบรารี Lombok ที่ช่วยสร้าง Constructor สำหรับฉีด Dependency (`userRepository`) เข้ามาให้โดยไม่ต้องเขียน Constructor เอง
3. **`implements CommandLineRunner`**: เป็น Interface ของ Spring Boot ที่กำหนดว่า เมื่อเซิร์ฟเวอร์เริ่มต้นทำงานเสร็จสิ้น ให้เรียกเมธอด `run(String... args)` ทำงานทันที 1 ครั้ง
4. **`BCryptPasswordEncoder`**: เครื่องมือเข้ารหัสรหัสผ่านตามมาตรฐานความปลอดภัย BCrypt เพื่อแปลงรหัสผ่านธรรมดา (Plain Text) เช่น `"1234"` ให้กลายเป็นรหัส Hash ที่อ่านออกไม่ได้ก่อนบันทึกลงฐานข้อมูล
5. **`seedUserIfMissing(...)`**:
   - เช็คด้วย `!userRepository.existsByUsername(username)` ว่าในฐานข้อมูลมีชื่อผู้ใช้นี้หรือยัง
   - หากยังไม่มี จะสร้างอ็อบเจกต์ `User` กำหนดชื่อ, ชื่อเต็ม, รหัสผ่านที่เข้ารหัสแล้ว (`passwordEncoder.encode`), บทบาท (`Role`), และตั้งสถานะ `isActive = true`
   - สั่ง `userRepository.save(user)` เพื่อบันทึกลงตาราง `users`

**ข้อมูลผู้ใช้เริ่มต้นที่ถูกสร้าง:**
| User ID | Username | Password | Full Name | Role | หน้า Dashboard ที่เปิด |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `503092` | `admin` | `1234` | Admin User | `ADMIN` | `admin/dashboard.html` |
| `503069` | `manager` | `1234` | Manager User | `MANAGER` | `manager/dashboard.html` |
| `503080` | `staff` | `1234` | Staff User | `STAFF` | `staff/dashboard.html` |
| `503107` | `owner` | `1234` | Owner User | `OWNER` | `owner/dashboard.html` |

---

## 3. โฟลเดอร์ `controller` (ส่วนควบคุมและรับ-ส่ง API)

**หน้าที่ของโฟลเดอร์ `controller`:**  
ทำหน้าที่เป็น **ด่านหน้า (Presentation / API Layer)** คอยรับคำขอ (HTTP Request) จากฝั่งหน้าเว็บ (Frontend HTML/JS) มาตรวจสอบความถูกต้อง ประมวลผลร่วมกับฐานข้อมูล และส่งผลลัพธ์กลับไปเป็น **JSON (HTTP Response)**

### 3.1 ไฟล์ `AuthController.java`
**ที่อยู่ไฟล์:** `src/main/java/com/sras/backend/controller/AuthController.java`  
**หน้าที่:** จัดการระบบยืนยันตัวตน (Authentication) ได้แก่ การเข้าสู่ระบบ (`/api/login`) และการออกจากระบบ (`/api/logout`)

#### โค้ดฉบับเต็ม:
```java
package com.sras.backend.controller;

import com.sras.backend.entity.User;
import com.sras.backend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public record LoginRequest(String userId, String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req, HttpSession session) {
        Map<String, Object> body = new LinkedHashMap<>();

        if (req == null || req.username() == null || req.username().isBlank()
                || req.password() == null || req.password().isBlank()) {
            body.put("success", false);
            body.put("message", "กรุณากรอกข้อมูลให้ครบถ้วน");
            return ResponseEntity.badRequest().body(body);
        }

        Optional<User> userOpt = findMatchingUser(req.userId(), req.username().trim());
        if (userOpt.isEmpty() || !passwordEncoder.matches(req.password(), userOpt.get().getPasswordHash())) {
            body.put("success", false);
            body.put("message", "Invalid ID or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        User user = userOpt.get();
        session.setAttribute("USER_ID", user.getUserId());
        session.setAttribute("USER_ROLE", user.getRole().name());

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("role", user.getRole().name());

        body.put("success", true);
        body.put("message", "Login successful");
        body.put("user", userInfo);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true));
    }

    private Optional<User> findMatchingUser(String rawUserId, String username) {
        if (rawUserId != null && !rawUserId.isBlank()) {
            try {
                Integer parsedId = Integer.valueOf(rawUserId.trim());
                return userRepository.findByUserIdAndUsernameAndIsActiveTrue(parsedId, username);
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return userRepository.findByUsernameAndIsActiveTrue(username);
    }
}
```

#### อธิบายการทำงานทีละส่วน:
1. **Annotations ระดับคลาส:**
   - `@RestController`: ระบุว่าคลาสนี้เป็น Controller สำหรับสร้าง REST API ซึ่งจะแปลงค่าที่ Return ออกไปให้เป็นรูปแบบ JSON โดยอัตโนมัติ
   - `@RequestMapping("/api")`: กำหนด URL เริ่มต้นของทุก API ในคลาสนี้ให้ขึ้นต้นด้วย `/api`
2. **`public record LoginRequest(...)`**:
   - เป็น Java Record ที่ทำหน้าที่เป็น **DTO (Data Transfer Object)** สำหรับรับค่า JSON (`userId`, `username`, `password`) ที่ส่งมาจากฟอร์ม Login หน้าเว็บ
3. **เมธอด `login` (`@PostMapping("/login")`)**:
   - **URL:** `POST /api/login`
   - **ขั้นตอนที่ 1 (Validation):** ตรวจสอบว่า `username` และ `password` ถูกส่งมาครบหรือไม่ หากว่างเปล่าจะส่งสถานะ `400 Bad Request` กลับไปทันที
   - **ขั้นตอนที่ 2 (Authentication):** ค้นหาผู้ใช้จากฐานข้อมูลผ่าน `findMatchingUser(...)` และตรวจสอบรหัสผ่านด้วย `passwordEncoder.matches(รหัสที่กรอก, รหัสHashในDB)` หากไม่พบผู้ใช้หรือรหัสผิด จะส่งสถานะ `401 Unauthorized`
   - **ขั้นตอนที่ 3 (Session Management):** หากถูกต้อง จะบันทึก `USER_ID` และ `USER_ROLE` เก็บไว้ใน `HttpSession` บนเซิร์ฟเวอร์
   - **ขั้นตอนที่ 4 (Response):** ส่งสถานะ `200 OK` พร้อมข้อมูลผู้ใช้ (`userId`, `username`, `fullName`, `role`) กลับไปให้หน้าเว็บเพื่อใช้เปลี่ยนหน้าไปยัง Dashboard ตามสิทธิ์
4. **เมธอด `logout` (`@PostMapping("/logout")`)**:
   - **URL:** `POST /api/logout`
   - เรียกคำสั่ง `session.invalidate()` เพื่อทำลาย Session ของผู้ใช้บนเซิร์ฟเวอร์ทิ้งทันทีและส่ง `{ "success": true }` กลับไป
5. **เมธอด `findMatchingUser(...)`**:
   - หากผู้ใช้กรอก `userId` มาด้วย ระบบจะแปลงเป็นตัวเลข (`Integer`) แล้วค้นหาจากทั้ง `userId` และ `username` ที่ตรงกันและมีสถานะ `isActive = true`
   - หากไม่ได้กรอก `userId` มา จะค้นหาจาก `username` และ `isActive = true` เพียงอย่างเดียว

---

## 4. โฟลเดอร์ `enums` (ชุดค่าคงที่)

**หน้าที่ของโฟลเดอร์ `enums`:**  
ใช้เก็บคลาสประเภท `enum` (Enumeration) เพื่อจำกัดขอบเขตของค่าข้อมูลให้เป็นไปตามที่ระบบกำหนดไว้เท่านั้น ช่วยป้องกันปัญหาการพิมพ์ผิด (Typo) และเพิ่มความปลอดภัยทางชนิดข้อมูล (Type Safety)

### 4.1 ไฟล์ `Role.java`
**ที่อยู่ไฟล์:** `src/main/java/com/sras/backend/enums/Role.java`  
**หน้าที่:** กำหนดระดับสิทธิ์การใช้งานของผู้ใช้ในระบบคลังสินค้า

#### โค้ดฉบับเต็ม:
```java
package com.sras.backend.enums;

public enum Role {
    OWNER,
    MANAGER,
    ADMIN,
    STAFF
}
```

#### ความหมายของแต่ละระดับสิทธิ์:
- **`OWNER`**: เจ้าของกิจการ / ผู้บริหารระดับสูง
- **`MANAGER`**: ผู้จัดการคลังสินค้า
- **`ADMIN`**: ผู้ดูแลระบบ
- **`STAFF`**: พนักงานปฏิบัติการทั่วไป

---

## 5. โฟลเดอร์ `entity` (โครงสร้างตารางฐานข้อมูล)

**หน้าที่ของโฟลเดอร์ `entity`:**  
เป็นตัวแทนของ **ตารางในฐานข้อมูล (Database Tables)** โดยใช้มาตรฐาน **JPA (Jakarta Persistence API) / Hibernate ORM** ในการแปลงคลาส Java ให้กลายเป็นตารางใน SQL โดยอัตโนมัติ (เมื่อตั้งค่า `spring.jpa.hibernate.ddl-auto=update`)

### คำอธิบาย Annotations พื้นฐานที่ใช้ใน Entity:
- `@Entity`: ระบุว่าคลาสนี้คือ Entity ที่ต้องนำไปสร้างเป็นตารางในฐานข้อมูล
- `@Table(name = "...")`: กำหนดชื่อตารางในฐานข้อมูล
- `@Id`: กำหนดให้ฟิลด์นั้นเป็น **Primary Key (PK)**
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`: กำหนดให้ฐานข้อมูลรันเลข ID เพิ่มขึ้นเองอัตโนมัติ (**Auto-Increment**)
- `@Column(...)`: กำหนดรายละเอียดของคอลัมน์ เช่น ชื่อคอลัมน์ (`name`), ความยาวตัวอักษร (`length`), ห้ามว่าง (`nullable = false`), ห้ามซ้ำ (`unique = true`)
- `@Getter`, `@Setter`, `@NoArgsConstructor`: คำสั่งจาก Lombok ช่วยสร้างเมธอด Getter, Setter และ Constructor เปล่าให้อัตโนมัติโดยไม่ต้องพิมพ์เอง

---

### 5.1 ไฟล์ `User.java` (ตาราง `users`)
**ที่อยู่ไฟล์:** `src/main/java/com/sras/backend/entity/User.java`  
**หน้าที่:** เก็บข้อมูลบัญชีผู้ใช้งานระบบ

#### โค้ดฉบับเต็ม:
```java
package com.sras.backend.entity;

import com.sras.backend.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username", length = 30, nullable = false, unique = true)
    private String username;

    @Column(name = "full_name", length = 50, nullable = false)
    private String fullName;

    @Column(name = "password_hash", length = 60, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 10, nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
```

#### โครงสร้างคอลัมน์ในตาราง `users`:
| ชื่อฟิลด์ใน Java | ชื่อคอลัมน์ใน DB | ชนิดข้อมูล | คุณสมบัติพิเศษ |
| :--- | :--- | :--- | :--- |
| `userId` | `user_id` | `INT` | Primary Key, Auto-Increment |
| `username` | `username` | `VARCHAR(30)` | ห้ามว่าง (`NOT NULL`), ห้ามซ้ำ (`UNIQUE`) |
| `fullName` | `full_name` | `VARCHAR(50)` | ห้ามว่าง (`NOT NULL`) |
| `passwordHash` | `password_hash` | `VARCHAR(60)` | เก็บรหัสผ่าน BCrypt Hash, ห้ามว่าง |
| `role` | `role` | `VARCHAR(10)` | เก็บค่าจาก Enum `Role` เป็นตัวอักษร (`EnumType.STRING`) |
| `isActive` | `is_active` | `BOOLEAN` | สถานะการใช้งาน (ค่าเริ่มต้น = `true`) |

---

### 5.2 ไฟล์ `Zone.java` (ตาราง `zones`)
**ที่อยู่ไฟล์:** `src/main/java/com/sras/backend/entity/Zone.java`  
**หน้าที่:** เก็บข้อมูลโซนหรือพื้นที่จัดเก็บสินค้าภายในคลังสินค้า

#### โค้ดฉบับเต็ม:
```java
package com.sras.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "zones")
@Getter
@Setter
@NoArgsConstructor
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Integer zoneId;

    @Column(name = "zone_code", length = 5, nullable = false, unique = true)
    private String zoneCode;

    @Column(name = "zone_name", length = 30, nullable = false, unique = true)
    private String zoneName;
}
```

#### โครงสร้างคอลัมน์ในตาราง `zones`:
| ชื่อฟิลด์ใน Java | ชื่อคอลัมน์ใน DB | ชนิดข้อมูล | คุณสมบัติพิเศษ |
| :--- | :--- | :--- | :--- |
| `zoneId` | `zone_id` | `INT` | Primary Key, Auto-Increment |
| `zoneCode` | `zone_code` | `VARCHAR(5)` | รหัสโซน ห้ามว่าง และห้ามซ้ำ |
| `zoneName` | `zone_name` | `VARCHAR(30)` | ชื่อโซน ห้ามว่าง และห้ามซ้ำ |

---

### 5.3 ไฟล์ `Product.java` (ตาราง `products`)
**ที่อยู่ไฟล์:** `src/main/java/com/sras/backend/entity/Product.java`  
**หน้าที่:** เก็บข้อมูลรายละเอียดสินค้า ราคา จำนวนสต็อกคงเหลือ เงื่อนไขการแจ้งเตือน และโซนที่จัดเก็บ

#### โค้ดฉบับเต็ม:
```java
package com.sras.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "product_name", length = 50, nullable = false)
    private String productName;

    @Column(name = "default_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal defaultCost;

    @Column(name = "selling_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal sellingPrice;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock = 0;

    @Column(name = "min_stock_alert", nullable = false)
    private Integer minStockAlert = 20;

    @Column(name = "po_min_qty", nullable = false)
    private Integer poMinQty = 1;

    @Column(name = "po_max_qty")
    private Integer poMaxQty;

    @Column(name = "so_min_qty", nullable = false)
    private Integer soMinQty = 1;

    @Column(name = "so_max_qty")
    private Integer soMaxQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
```

#### อธิบายจุดสำคัญใน `Product.java`:
1. **`BigDecimal` (`precision = 10, scale = 2`)**: ใช้เก็บค่าเงิน (`defaultCost` ต้นทุนมาตรฐาน และ `sellingPrice` ราคาขาย) โดยมีจำนวนตัวเลขทั้งหมด 10 หลัก และทศนิยม 2 ตำแหน่ง เพื่อป้องกันปัญหาความคลาดเคลื่อนของทศนิยมจากการใช้ `double` หรือ `float`
2. **การควบคุมสต็อกและคำสั่งซื้อ-ขาย**:
   - `currentStock`: จำนวนสินค้าคงเหลือปัจจุบัน (เริ่มต้น `0`)
   - `minStockAlert`: จุดแจ้งเตือนเมื่อสต็อกต่ำกว่ากำหนด (เริ่มต้น `20`)
   - `poMinQty` / `poMaxQty`: จำนวนสั่งซื้อขั้นต่ำและสูงสุดสำหรับใบสั่งซื้อ (Purchase Order - PO)
   - `soMinQty` / `soMaxQty`: จำนวนขายขั้นต่ำและสูงสุดสำหรับใบสั่งขาย (Sales Order - SO)
3. **ความสัมพันธ์ระหว่างตาราง (`@ManyToOne` และ `@JoinColumn`)**:
   - สินค้าหลายรายการ (`Many`) สามารถถูกจัดเก็บอยู่ในโซนเดียวกัน (`One`) ได้
   - `@JoinColumn(name = "zone_id", nullable = false)`: สร้างคอลัมน์ Foreign Key ชื่อ `zone_id` เชื่อมไปยังตาราง `zones`
   - `fetch = FetchType.LAZY`: ดึงข้อมูล `Zone` ออกมาจากฐานข้อมูลเฉพาะเมื่อมีการเรียกใช้งานจริงเท่านั้น เพื่อประหยัดหน่วยความจำและเพิ่มความเร็ว

---

## 6. โฟลเดอร์ `repository` (ตัวจัดการคำสั่งฐานข้อมูล)

**หน้าที่ของโฟลเดอร์ `repository`:**  
เป็น **Data Access Layer** ที่ทำหน้าที่คุยกับฐานข้อมูลโดยตรง (SELECT, INSERT, UPDATE, DELETE) ผ่าน **Spring Data JPA** โดยช่วยลดการเขียนคำสั่ง SQL ด้วยตัวเอง

### 6.1 ไฟล์ `UserRepository.java`
**ที่อยู่ไฟล์:** `src/main/java/com/sras/backend/repository/UserRepository.java`  
**หน้าที่:** จัดการคำสั่งสืบค้นและบันทึกข้อมูลสำหรับตาราง `users`

#### โค้ดฉบับเต็ม:
```java
package com.sras.backend.repository;

import com.sras.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    Optional<User> findByUserIdAndUsernameAndIsActiveTrue(Integer userId, String username);

    boolean existsByUsername(String username);
}
```

#### อธิบายการทำงานทีละส่วน:
1. **`extends JpaRepository<User, Integer>`**:
   - `<User, Integer>` หมายถึง Repository นี้จัดการกับ Entity `User` ซึ่งมี Primary Key เป็นชนิด `Integer`
   - สืบทอดเมธอดมาตรฐานมาให้พร้อมใช้งานทันที เช่น `.save()`, `.findById()`, `.findAll()`, `.deleteById()`, `.count()`
2. **Derived Query Methods (การสร้างคำสั่ง SQL อัตโนมัติจากชื่อเมธอด)**:
   Spring Data JPA จะแปลชื่อเมธอดให้กลายเป็นคำสั่ง SQL โดยอัตโนมัติ ดังนี้:

   - **`findByUsernameAndIsActiveTrue(String username)`**
     ```sql
     SELECT * FROM users WHERE username = ? AND is_active = true;
     ```
     > คืนค่าเป็น `Optional<User>` เพื่อป้องกันปัญหา `NullPointerException` กรณีค้นหาไม่พบ

   - **`findByUserIdAndUsernameAndIsActiveTrue(Integer userId, String username)`**
     ```sql
     SELECT * FROM users WHERE user_id = ? AND username = ? AND is_active = true;
     ```
     > ใช้ตรวจสอบเมื่อผู้ใช้กรอกทั้งรหัสประจำตัว (User ID) และชื่อผู้ใช้ (Username) ตอนล็อกอิน

   - **`existsByUsername(String username)`**
     ```sql
     SELECT COUNT(*) > 0 FROM users WHERE username = ?;
     ```
     > คืนค่า `true` หากมีชื่อผู้ใช้นี้อยู่แล้วในระบบ หรือ `false` หากยังไม่มี (ถูกเรียกใช้โดย `DataInitializer`)

---

## 7. สรุปขั้นตอนการทำงานร่วมกันทั้งหมด (System Workflow)

### 7.1 ตอนเริ่มรันโปรแกรม (Application Startup)
1. **Hibernate / JPA** อ่านคลาสในโฟลเดอร์ **`entity`** (`User`, `Zone`, `Product`) และ **`enums`** (`Role`) เพื่อสร้างตาราง `users`, `zones`, และ `products` พร้อมผูก Foreign Key ในฐานข้อมูล
2. คลาส **`DataInitializer`** (ในโฟลเดอร์ **`config`**) ทำงานอัตโนมัติ โดยเรียกใช้ **`UserRepository`** เช็คว่ามีบัญชี `admin` และ `manager` หรือยัง หากยังไม่มีจะเข้ารหัสรหัสผ่านและบันทึกลงตาราง `users`

### 7.2 ตอนผู้ใช้งานกดปุ่มเข้าสู่ระบบ (User Login Flow)
1. หน้าเว็บ (`index.html`) ส่งข้อมูล JSON `{ userId, username, password }` มาที่ `POST /api/login`
2. **`AuthController`** (ในโฟลเดอร์ **`controller`**) รับข้อมูลมาตรวจสอบความครบถ้วน
3. **`AuthController`** เรียกใช้ **`UserRepository`** (ในโฟลเดอร์ **`repository`**) เพื่อค้นหาข้อมูลผู้ใช้จากตาราง `users`
4. **`UserRepository`** คืนข้อมูลกลับมาในรูปแบบอ็อบเจกต์ **`User`** (จากโฟลเดอร์ **`entity`**) ซึ่งมีค่าสิทธิ์เป็น **`Role`** (จากโฟลเดอร์ **`enums`**)
5. **`AuthController`** ตรวจสอบรหัสผ่านด้วย BCrypt, บันทึกสถานะลง `HttpSession`, และส่งข้อมูล JSON ตอบกลับไปให้หน้าเว็บพาผู้ใช้เข้าสู่หน้า Dashboard ตามบทบาทหน้าที่

