#  StockTrack (Project-SRAS)

**StockTrack** is an enterprise inventory, order, and approval management system built with **Spring Boot 3.5.0 (Java 17)** and a modern **Vanilla HTML5 / CSS / JavaScript** frontend. The system manages warehouse zone logistics, procurement (Purchase Orders), sales fulfillment (Sales Orders), a unified 14-type dispatcher approval engine for managers, warehouse execution (goods receiving, stock-out dispatch, and zone transfers) for staff, and an executive dashboard with deterministic audit activity logging for business owners.

---

## Group Members

```
6831503069  Rawat Maneerat
6831503080  Hanka Khosito
6831503092  Jeerasak Chalam
6831503104  Rattapoom El-sibai
6831503107  Suradet Homkaenchan
```

### Member Feature Responsibilities

| Student ID | Full Name | Module / Role | Features & Responsibilities |
| :---: | :--- | :---: | :--- |
| **6831503092** | Jeerasak Chalam | **Admin & Architecture** | System Architecture (Clean Architecture & SOLID), declarative security (`@RequireRoles`, `@CurrentUser`), PO & SO creation workflows, product master data, warehouse zone management, user & role management. |
| **6831503069** | Rawat Maneerat | **Manager** | Centralized approval inbox, multi-type request review and decision engine (Approve/Reject with required reason), manager KPI summary cards. |
| **6831503080** | Hanka Khosito | **Staff (Operations)** | Physical goods receiving workflow (PO quantity verification), stock-out dispatch (SO quantity verification), zone transfer execution, and stock balance adjustments. |
| **6831503104** | Rattapoom El-sibai | **Staff (Frontend / UI)** | Staff dashboard, multi-step inventory wizard workflows (verification & confirmation modals), UI components, and responsive layout styling. |
| **6831503107** | Suradet Homkaenchan | **Owner** | Executive analytics dashboard (total revenue, inventory valuation, profit calculation, best-sellers, top imports), and deterministic audit activity log tracking. |

### End-to-End Workflow Examples

#### 1. Procurement & Receiving (PO Flow)
* **Admin:** Creates Purchase Order (PO) with supplier, items, and target zone $\rightarrow$ Status: `PENDING`.
* **Manager:** Reviews PO budget/items in approval inbox and clicks **Approve** $\rightarrow$ Status: `APPROVED`.
* **Staff:** Verifies physically delivered goods in `receive-items.html` and confirms $\rightarrow$ Adds `+Stock`, Status: `COMPLETED`.
* **Owner:** Real-time inventory valuation updates; audit trail logs the complete step-by-step history.

#### 2. Sales & Stock-Out Dispatch (SO Flow)
* **Admin:** Creates Sales Order (SO) with customer, items, and source zone $\rightarrow$ Status: `PENDING`.
* **Manager:** Checks stock availability and clicks **Approve** $\rightarrow$ Status: `APPROVED`.
* **Staff:** Picks items from physical shelf, checks confirmation box, and confirms stock-out $\rightarrow$ Deducts `-Stock`, Status: `COMPLETED`.
* **Owner:** Total revenue and profit update instantly; top best-sellers refresh; action logged to audit trail.

#### 3. Warehouse Zone & Stock Transfer Flow
* **Admin:** Requests a new zone or product relocation between zones $\rightarrow$ Status: `PENDING`.
* **Manager:** Evaluates warehouse capacity and clicks **Approve** $\rightarrow$ Status: `APPROVED`.
* **Staff:** Physically relocates items between warehouse zones and confirms execution $\rightarrow$ Zone stocks updated.
* **Owner:** Observes real-time zone capacity distribution and reviews all warehouse changes in audit history.

---

### Role Interconnections & System Synergy

1. **Dual-Control Security (Separation of Concerns):**  
   Admin initiates orders but cannot self-approve. Manager controls approvals but cannot tamper with physical stock directly.
2. **Operational Reality Check (Physical vs Digital Sync):**  
   Approval does not alter inventory numbers prematurely. Stock updates only when warehouse Staff verify physical goods on-site.
3. **Instant Financial Impact (Real-Time KPIs):**  
   Staff operations directly drive Owner analytics: PO receiving increases inventory asset valuation, while SO stock-out realizes revenue and gross profit.
4. **Deterministic Audit Trail:**  
   Every lifecycle transition (`Admin: Create` $\rightarrow$ `Manager: Approve` $\rightarrow$ `Staff: Execute`) is recorded sequentially with actor, timestamp, and details for 100% traceability.

---

## Live Deployment on GCP (Cloud Run)

- **Live Application URL:** [https://stocktrack-service-url.run.app](https://stocktrack-service-url.run.app) *(Replace with your actual Cloud Run URL)*
- **CI/CD Pipeline:** Automated build and deployment to Google Cloud Run configured via **Cloud Build** ([cloudbuild.yaml](file:///c:/Users/bankt/Project-SRAS/cloudbuild.yaml)) and **Artifact Registry**.

---

## How to Run the Project Locally (Step-by-Step)

### Prerequisites
- **Java 17** or higher (`java -version`)
- **Maven** (included via wrapper `mvnw`)

### Step 1: Run the Server
The project runs out-of-the-box with an embedded **H2 In-Memory Database** (no MySQL installation required).

- **Windows (PowerShell / Command Prompt):**
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **macOS / Linux:**
  ```bash
  ./mvnw spring-boot:run
  ```

### Step 2: Access the Application
- **Web Application:** [http://localhost:8080](http://localhost:8080)
- **H2 Database Console:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)  
  *(JDBC URL: `jdbc:h2:mem:stocktrack_db` | Username: `sa` | Password: leave empty)*

### Step 3: Test Login Accounts
All accounts use password: **`12345678`** (or `1234`). You can log in using either the **User ID** or the **Username**:

| Role | User ID (Numeric) | Username | Password | Default Landing Page |
| :--- | :---: | :---: | :---: | :--- |
| **Admin** | `503092` | `admin` | `12345678` | `/admin/my-requests.html` |
| **Manager** | `503069` | `manager` | `12345678` | `/manager/dashboard.html` |
| **Staff** | `503080` | `staff` | `12345678` | `/staff/dashboard.html` |
| **Owner** | `503107` | `owner` | `12345678` | `/owner/dashboard.html` |

---

## Architecture & Technical Highlights

1. **Declarative Role-Based Security:** Method/Class-level authorization via `@RequireRoles({Role...})` interceptor and `@CurrentUser User` parameter injection, returning standardized `403 Forbidden` on unauthorized access.
2. **Unified Approval Engine:** Dispatcher and Template Method patterns (`ApprovalDispatcher`, `BaseApprovalExecutor<T>`) handling 14 request types with JSON payloads and rollback-safe transaction boundaries.
3. **Domain-Driven Services:** Clean separation of business domains (`ReceivingService`, `StockOutService`, `TransferService`, `StockMovementService`, `PurchaseOrderService`, `SalesOrderService`, `ZoneService`, `OwnerService`).
4. **Deterministic Audit Logging:** Strongly-typed action catalog via `AuditAction` enum logging exact targets and actions into `AUDIT_LOGS` without fragile string regex matching.
5. **Standardized Responses:** Unified API envelope `ApiResponse<T>` with centralized exception handling (`GlobalExceptionHandler`).

---

## Automated Testing

To run the full suite of unit and integration tests:

```bash
# Windows
.\mvnw.cmd test

# macOS / Linux
./mvnw test
```

- **Role Security Tests (`SecurityRoleAccessControllerTest`):** Verifies role enforcement and permission denial across all controllers.
- **Order Lifecycle Tests (`OrderLifecycleIntegrationTest`):** Verifies the complete end-to-end flow: `PO Create -> Approve -> Receive (+Stock)` and `SO Create -> Approve -> Stock-out (-Stock)`.
- **Approval Executor Tests:** Validates payload parsing and business execution across all request types.
