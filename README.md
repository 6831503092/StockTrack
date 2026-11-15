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

## How to Verify CRUD Operations on H2 Database Console

### 1. Connect to H2 Console
1. Ensure the backend server is running (`.\mvnw.cmd spring-boot:run` or `./mvnw spring-boot:run`).
2. Open your browser and go to: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
3. Enter connection settings:
   - **Driver Class:** `org.h2.Driver`
   - **JDBC URL:** `jdbc:h2:mem:stocktrack_db` *(⚠️ Replace default `~/test` with `jdbc:h2:mem:stocktrack_db`)*
   - **User Name:** `sa`
   - **Password:** *(leave empty)*
4. Click **Connect**.

### 2. SQL Queries to Inspect Live CRUD Records

Execute these queries in the H2 SQL editor to inspect data created, updated, and deleted across roles:

```sql
-- [USERS] Check user creation, roles, and status
SELECT user_id, full_name, role, is_active FROM USERS;

-- [PRODUCTS] Check product master data, current stock, and zone assignments
SELECT product_id, product_name, default_cost, selling_price, current_stock, zone_code FROM PRODUCTS;

-- [ZONES] Check warehouse storage zones
SELECT zone_code, zone_name FROM ZONES;

-- [SUPPLIERS] Check suppliers (Soft-delete reflected via is_active = false)
SELECT supplier_id, company_name, zone_code, category, is_active FROM SUPPLIERS;

-- [CUSTOMERS] Check customers (Soft-delete reflected via is_active = false)
SELECT customer_id, customer_name, is_active FROM CUSTOMERS;

-- [ORDERS] Check Purchase Orders (PO) & Sales Orders (SO) lifecycles
SELECT po_id, supplier_id, total_amount, status, created_at FROM PURCHASE_ORDERS;
SELECT so_id, customer_id, total_amount, status, created_at FROM SALES_ORDERS;

-- [APPROVALS] Check pending/approved/rejected governance requests
SELECT request_id, request_type, status, requester_id, detail, created_at FROM APPROVAL_REQUESTS ORDER BY request_id DESC;

-- [AUDIT LOGS] Check complete sequential activity history
SELECT log_id, user_id, action, target_type, target_id, details, created_at FROM AUDIT_LOGS ORDER BY created_at DESC;
```
