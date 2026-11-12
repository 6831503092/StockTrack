# Backend Plan (Java / Spring Boot) — Admin Flow

> เอกสารนี้เขียนให้ AI/นักพัฒนาคนอื่นอ่านแล้วทำต่อได้ทันที เน้นฝั่ง Java
> ก่อนเริ่ม ให้อ่าน `information/SRS_StockTrack_content.md` และ `information/StockTrack_schema.dbml` ประกอบ

## 0. Context
- Project: StockTrack (`Project-SRAS`), Spring Boot + JPA + H2 in-memory, Lombok, BCrypt
- Frontend: static HTML ใน `src/main/resources/static/` (admin pages ใน `admin/`) เรียก REST ด้วย fetch
- Package root: `com.sras.backend` → `config`, `controller`, `dto`, `entity`, `enums`, `repository`, `service`
- Seed users (`DataInitializer`): admin 503092 / manager 503069 / staff 503080 / owner 503107, password `1234`
- **สถานะ:** Login เสร็จแล้ว (`AuthService`, `AuthController`, `LoginRequest`, `UserDto`, `ApiResponse`)
  Entity และ Repository ครบ 16 ตารางแล้ว ที่เหลือคือ Service, DTO, Controller ของแต่ละหน้า

## 1. กฎหลัก (ห้ามฝ่าฝืน)
1. **Admin ห้ามแก้ตารางจริงโดยตรง (NFR-05)** ทุกการ Create/Update/Move ต้องสร้างแถวใน `approval_requests` สถานะ `PENDING` ก่อน
2. ข้อมูลที่จะ apply เก็บใน `ApprovalRequest.payload` (JSON string) และ `detail` เป็นข้อความสั้นสำหรับแสดงใน my-requests
3. เมื่อ Manager approve (งานของ Manager ทำทีหลัง) จึงเรียก executor เพื่อ apply ลงตารางจริง ใน `@Transactional` เดียวกับการเปลี่ยนสถานะ
4. **ห้ามรับ userId/createdBy จาก client** ดึงจาก session ผ่าน `AuthService.resolveCurrentUser()` เท่านั้น
5. ทุก endpoint ของ Admin เรียก `authService.requireRole(Role.ADMIN)` ก่อน
6. Response ใช้ `ApiResponse` เดิม `{ success, message, user, data }` ห้ามเปลี่ยนรูปแบบ เพราะ frontend ผูกอยู่
7. ห้าม return Entity ออก controller ให้ใช้ DTO เสมอ โดยเฉพาะ `User` ห้ามหลุด `passwordHash`
8. รหัสผ่านของ user ใหม่ต้อง **BCrypt hash ก่อนใส่ payload** ห้ามเก็บ plain text
9. Validate 2 รอบ คือตอน submit และตอน apply (ข้อมูลอาจเปลี่ยนระหว่างรอ)

## 2. Layering
```
Controller (รับ/ส่ง DTO, เช็ค role)  →  Service (business rule, transaction)
   →  Repository (Spring Data JPA)  →  Entity
```
- Controller บาง ไม่มี logic
- Service ที่ต้องมี: `ApprovalRequestService`, `ApprovalExecutor` (strategy), `AuditLogService`,
  `UserManagementService`, `ProductService`, `ZoneService`, `PurchaseOrderService`, `SalesOrderService`, `SupplierService`, `CustomerService`
- Exception: สร้าง `BusinessException` (400) / `NotFoundException` (404) แล้วจัดการใน `@RestControllerAdvice` กลางตัวเดียว (ปัจจุบัน handler อยู่ใน `AuthController` ให้ย้ายออกมา)

## 3. Phase 0 — Foundation (ต้องทำก่อนทุกหน้า)
| งาน | รายละเอียด |
|---|---|
| เพิ่ม enum | `RequestType` ปัจจุบันมี `PRODUCT_CREATE/UPDATE/DELETE, SUPPLIER_*, CUSTOMER_*, ZONE_CREATE/UPDATE, PRODUCT_MOVE, ROLE_CHANGE, PO_CREATE` **ต้องเพิ่ม `USER_CREATE` และ `SO_CREATE`** และเพิ่ม `TargetType.USER`, `PO`, `SO` ถ้าจำเป็น (เช็คความยาวคอลัมน์ `request_type` = 20, `target_type` = 20) |
| `ApprovalRequestService` | `submit(RequestType, TargetType, Integer targetId, Object payloadDto, String detail)` → serialize ด้วย Jackson `ObjectMapper`, set `requestedBy` จาก session, `requestedDatetime = now`, status PENDING, เขียน audit log |
| `ApprovalRequestService.listMine(search, date)` | ใช้ `findByRequestedByUserIdOrderByRequestedDatetimeDesc` แล้วกรอง search/date (หรือเพิ่ม `@Query`) |
| `ApprovalExecutor` | interface `RequestType supports()` + `void apply(ApprovalRequest r)`; มี implementation ต่อ type; `ApprovalDispatcher` เลือกตัวที่ตรงจาก `Map<RequestType, Executor>` |
| DTO | `ApprovalRequestDto` (requestId, requestType, detail, status, requestedByName, requestedDatetime, decidedByName, decidedDatetime, decisionReason), `SubmitResultDto` (requestId) |
| `GlobalExceptionHandler` | `@RestControllerAdvice` คืน `ApiResponse` (success=false) |
| Test | unit test ของ `ApprovalRequestService` (Mockito) |

> หมายเหตุ build: sandbox ไม่มีเน็ต ใช้ `.\mvnw -o compile` ตรวจ compile

## 4. แผนรายหน้า (ทำตามลำดับ)

### 4.1 `my-requests.html` — อ่านอย่างเดียว
- `GET /api/requests/my?search=&date=yyyy-MM-dd` → `List<ApprovalRequestDto>`
- Service: `ApprovalRequestService.listMine`
- ผูก JS ในหน้าให้ fetch endpoint นี้แทนข้อมูล mock
- Done เมื่อ: login admin แล้วเห็นเฉพาะ request ของตัวเอง กรอง search/date ได้

### 4.2 `role-management.html`
| Endpoint | Method | Body DTO | ผลตอน submit |
|---|---|---|---|
| `/api/users?role=&search=` | GET | – | `List<UserDto>` (search ตาม fullName หรือ userId) |
| `/api/requests/user-create` | POST | `UserCreateRequest {fullName, username, role, password}` | validate → hash password → submit `USER_CREATE` |
| `/api/requests/role-change` | POST | `RoleChangeRequest {userId, newRole, reason}` | validate → submit `ROLE_CHANGE`, targetId = userId |

- Validate: username ไม่ซ้ำ (`existsByUsername`), role อยู่ใน enum, password ไม่ว่างและความยาวขั้นต่ำ, newRole ≠ role ปัจจุบัน, ห้ามเปลี่ยน role ตัวเอง (ตามนโยบาย), ห้ามมี PENDING ซ้ำของ user เดียวกัน
- Executor `USER_CREATE`: insert `User` ด้วย hash จาก payload
- Executor `ROLE_CHANGE`: update `user.role`
- `userId` ของ user ใหม่เป็น auto-generated (เช็ค strategy ใน entity ถ้า seed ใช้เลขเอง ต้องดูให้ไม่ชนกัน)

### 4.3 `warehouse-zones.html` และ `zone-product.html`
| Endpoint | Method | หมายเหตุ |
|---|---|---|
| `/api/zones` | GET | `List<ZoneDto {zoneId, zoneCode, zoneName, productCount}>` ใช้กับ dropdown |
| `/api/zones/{id}/products?search=` | GET | `List<ProductSummaryDto>` เฉพาะ `isActive=true` |
| `/api/requests/product-move` | POST | `ProductMoveRequest {targetZoneId, items:[{productId, quantity}]}` |

- Validate: product มีจริงและ active, targetZone มีจริง, ต้องไม่ใช่ zone เดิม, quantity ≥ 1 และ ≤ `currentStock`
- Submit `PRODUCT_MOVE` (targetType PRODUCT)
- Executor: สร้าง `StockMovement` ต่อ item (`fromZone`, `toZone`, status `PENDING_VERIFICATION`, ผูก `request`) **ยังไม่ย้ายสต็อกจริง** รอ Staff verify ตาม flow (`countedQty`, `verifiedBy`) แล้วค่อย update `product.zone` เมื่อเป็น `COMPLETED`
- `zone-product.html` ใช้ endpoint เดียวกับ `/api/zones/{id}/products`

### 4.4 `product-management.html`
| Endpoint | Method | หมายเหตุ |
|---|---|---|
| `/api/products?zoneId=&search=` | GET | `ProductDto` (ชื่อ, cost, price, stock, minStockAlert, po/so min-max, zone) |
| `/api/requests/product-create` | POST | `ProductCreateRequest {productName, defaultCost, sellingPrice, zoneId, minStockAlert, poMinQty, poMaxQty, soMinQty, soMaxQty}` |
| `/api/requests/product-update` | POST | `ProductUpdateRequest {productId, ...fields ที่แก้}` |

- เปิดเฉพาะปุ่มที่หน้า HTML มีจริง ดูก่อนว่าหน้า product-management ใช้ create/update/delete อะไรบ้าง (`PRODUCT_DELETE` = soft delete ตั้ง `isActive=false`)
- Validate: `sellingPrice ≥ defaultCost` (ถ้า SRS ระบุ), min ≤ max, ชื่อไม่ซ้ำ
- `currentStock` ของสินค้าใหม่ = 0 (stock เปลี่ยนผ่าน receiving/issue เท่านั้น)

### 4.5 `purchase-order.html`
| Endpoint | Method | หมายเหตุ |
|---|---|---|
| `/api/purchase-orders?status=&search=&date=` | GET | `PurchaseOrderDto` (+ items) |
| `/api/suppliers` | GET | เฉพาะ active สำหรับ dropdown |
| `/api/products?supplierless=…` | GET | ใช้ `/api/products` เดิมสำหรับเลือกสินค้าใน PO |
| `/api/requests/po-create` | POST | `PoCreateRequest {supplierId, items:[{productId, quantity, unitCost}]}` |

- Validate: supplier active, ทุก item อยู่ในช่วง `poMinQty..poMaxQty` ของ product, quantity และ unitCost > 0
- คำนวณ `subtotal` และ `totalAmount` **ที่ server** ห้ามเชื่อยอดจาก client
- Executor: insert `PurchaseOrder` (status `PENDING`/`APPROVED` ตาม flow ที่ SRS กำหนด) + `PurchaseOrderItem`, ผูก `request`
- `createdBy` มาจาก session (ช่อง readonly ในหน้าแสดงอย่างเดียว)

### 4.6 `sales-order.html`
- คล้าย PO: `GET /api/sales-orders`, `GET /api/customers`, `POST /api/requests/so-create` (`SoCreateRequest {customerId, items:[{productId, quantity}]}`)
- Validate: customer active, quantity อยู่ในช่วง `soMinQty..soMaxQty`, **stock พอ** (`currentStock ≥ quantity`) ตอน submit และเช็คซ้ำตอน apply
- Server เป็นคนกำหนด `unitPrice = product.sellingPrice` และ `unitCostSnapshot = product.defaultCost` ณ ตอน apply แล้วคำนวณ subtotal/total เอง
- ตัดสต็อกจริงเกิดตอน Issue (Staff) ไม่ใช่ตอน apply SO

### 4.7 (ถ้าหน้า HTML ต้องใช้) Supplier / Customer / Zone CRUD
`RequestType` มี `SUPPLIER_*`, `CUSTOMER_*`, `ZONE_CREATE/UPDATE` รอไว้แล้ว ทำตามรูปแบบเดียวกันเมื่อมีหน้า UI รองรับ

## 5. โครงไฟล์ที่คาดว่าจะเพิ่ม
```
controller/  ApprovalRequestController, UserManagementController, ZoneController,
             ProductController, PurchaseOrderController, SalesOrderController, GlobalExceptionHandler
service/     ApprovalRequestService, AuditLogService, UserManagementService, ZoneService,
             ProductService, PurchaseOrderService, SalesOrderService
service/approval/  ApprovalExecutor, ApprovalDispatcher, UserCreateExecutor, RoleChangeExecutor,
                   ProductMoveExecutor, ProductCreateExecutor, PoCreateExecutor, SoCreateExecutor
dto/         ApprovalRequestDto, UserCreateRequest, RoleChangeRequest, ProductMoveRequest, ...
exception/   BusinessException, NotFoundException
```

## 6. Definition of Done (ทุกหน้า)
- [ ] Endpoint ทั้งหมดของหน้านั้นมี `requireRole(ADMIN)`
- [ ] POST create คืน 200 พร้อม `requestId` และแถวใหม่ปรากฏใน `my-requests`
- [ ] ไม่มีการเขียนตารางจริงตอน submit (ตรวจใน H2 console)
- [ ] ไม่มี `passwordHash` ใน response ใดๆ
- [ ] `.\mvnw -o compile` ผ่าน
- [ ] ผูก JS ของหน้า HTML กับ endpoint จริง แล้วเทส flow ด้วยบัญชี admin (503092 / 1234)
- [ ] มี unit test ของ service หลักอย่างน้อย 1 กรณีสำเร็จ และ 1 กรณี validate ล้มเหลว

## 7. ลำดับการทำงานที่แนะนำ
Phase 0 → 4.1 my-requests → 4.2 role-management → 4.3 zones → 4.4 product → 4.5 PO → 4.6 SO
ทำทีละหน้า compile และเทสก่อนไปหน้าถัดไป
