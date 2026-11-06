# StockTrack SRS — Copy-Ready Content (Version 2: Multi-Role)

> วิธีใช้: คัดลอกข้อความในแต่ละหัวข้อ (ภาษาอังกฤษ) ไปวางในช่องที่ตรงกันใน `SRS_template_1_2026.docx`
> ส่วนที่เป็น `<...>` คือข้อมูลที่นายต้องกรอกเอง
> **Diagram ทั้งหมด (ER, Activity/Swimlane, Sitemap, Wireframe) ยังไม่ได้ปรับ รอออกแบบให้เสร็จก่อน** ดูรายการที่ต้องวาดใหม่ท้ายไฟล์

---

# SECTION: Cover Page / Revision / Approval

**Cover:** Software Requirements Specification for **StockTrack** — Inventory, Purchasing and Sales Management System

`<Version 2.0>`, `<Date: Month Year>`, `<Student-Name  Student ID>`

**Revision History**

| No. | Name | Date | Description | Version |
|:---|:---|:---|:---|:---|
| 1 | `<Name>` | `<dd/mm/2026>` | Initial Document (single manager role) | 1.0 |
| 2 | `<Name>` | `<dd/mm/2026>` | Multi-role access, approval workflow, warehouse zones, goods checking | 2.0 |

**Document Approval:** `<Stakeholder / Sponsor>` — Project Sponsor / `<Team Representative>` — Project Manager

---

# 1. Introduction

## 1.1 Business Problem

Right now the business writes down its purchase orders, stock receipts and sales orders by hand on paper. Totals are worked out by hand, stock counts are updated by hand, and anyone who can reach the paper can change it. As a result, mistakes creep into the records, the stock on paper often differs from what is really in the warehouse, and finding an old record takes a long time. Goods are also moved between areas of the warehouse without a record, so nobody knows exactly where a product is. When something goes wrong, nothing shows who did what and when, so the cause is hard to find. Wrong stock and profit figures can lead to selling goods that are not available, forgetting to re-order, and making poor business decisions.

## 1.2 Business Objective

- Move the recording of purchases, stock-in and sales from paper to a web-based system.
- Cut down recording and calculation mistakes by letting the system work out totals and update stock automatically.
- Separate duties between people: staff who handle the goods, admins who prepare orders, a manager who approves them, and an owner who oversees everything.
- Make sure no important change happens without the manager's approval, and keep a record of who did what and when.
- Make sure stock only changes after a staff member has physically checked the goods against the order.
- Keep track of which warehouse zone (for example Z1) each product is kept in.
- Give the manager and the owner a current view of stock, best-selling products and profit, without adding things up by hand.

## 1.3 Product Vision

For the **owner, manager, admins and warehouse staff** of a furniture business, **StockTrack** handles purchase orders, sales orders, goods checking, warehouse zones and master data. Every change made by an admin goes through the manager's approval, and every action is recorded. The goal is to keep stock and money records accurate, traceable and simple to use.

## 1.4 Business Scope

**What Release 1 will include**

The StockTrack system will provide user authentication and role management. There will be four user roles: Owner, Manager, Admin, and Staff. Users can log in and log out, and Admin can request role changes between Staff and Admin when necessary.

The system will include an approval process for data changes. When Admin submits a request to add, edit, or delete data, the Manager must review and approve or reject the request before the changes are applied.

For master data management, Admin can request to add, edit, and delete products. Admin can also request to add, edit, and deactivate supplier and customer information, as well as manage warehouse zones (including moving products between zones, which Staff must verify before the transfer is completed).

The Manager can set minimum and maximum quantity limits for each product in both Purchase Orders and Sales Orders. These limits help control the quantity of products ordered or sold.

For purchasing, Admin can create Purchase Orders by selecting one supplier and adding multiple products. The Manager must approve each Purchase Order before it can proceed to the receiving process.

For receiving, Staff will check the delivered goods against the corresponding Purchase Order. Staff can either accept the entire order, which increases the stock quantity, or reject the entire order.

For sales, Admin can create Sales Orders by selecting one customer and adding multiple products. The Manager must approve each Sales Order before it can proceed to the issuing process.

For issuing, Staff will check the goods against the corresponding Sales Order before sending them to the customer. Staff can either issue the entire order, which decreases the stock quantity, or reject the entire order.

For reporting, the system will provide dashboards for the Owner and Manager to monitor overall operations. The Owner will also be able to view activity reports showing who performed each action in the system.

**What Release 1 will not include**

Release 1 will not support custom user roles or individual per-user permissions beyond the four fixed roles. It will also not allow partial goods receipts, partial order issues, or cancelling an approved Purchase Order or Sales Order.

Financial and payment processes—such as recording payments to suppliers, receiving payments from customers, issuing cash receipts, processing refunds, handling product returns or exchanges as separate transactions, and calculating taxes, discounts, shipping costs, or multiple currencies—will not be included in this release and will be handled outside the system.

In addition, the system will not support multiple warehouses or branches, storage capacity limits for zones, or sub-locations within a zone (the zone code itself serves as the product's location). Restoring a deleted product, managing product images, and sending email or SMS notifications are also excluded from Release 1.

## 1.5 Business Workflow

The business runs through six main flows. (1) **Approval:** an admin never changes data directly. Instead the admin submits a request, and the manager approves or rejects it. A rejected request cannot be edited; the admin submits a new one. (2) **Purchasing:** the admin creates a purchase order, and once the manager approves it, the order is sent to staff for receiving. (3) **Receiving:** when the goods arrive, a staff member checks the type and quantity against the order. If everything matches, the order is *Received* and stock increases. Otherwise the staff member rejects the whole order and records the reason and the quantity actually counted. (4) **Selling:** the admin creates a sales order (blocked if stock is not enough), and once the manager approves it, the order is sent to staff for issuing. (5) **Issuing:** a staff member checks that every item is present and complete. If so, stock decreases and the order is *Issued*. Otherwise the whole order is rejected with a reason and the counted quantity. (6) **Zones and Moves:** when the admin manages zones to move a product to another zone and the manager approves it, a staff member confirms the goods really exist before the move is completed. The owner can see the dashboard and a report of every action, and the manager can see the dashboard as well.

> *[รอ Activity Diagram ใหม่ — ดูท้ายไฟล์]*

## 1.6 Definitions, Acronyms and Abbreviations

| Term | Description |
|:---|:---|
| BR / FR / NFR | Business Requirement / Functional Requirement / Non-Functional Requirement |
| DB | Database |
| PO | Purchase Order — an order the business places with a supplier |
| SO | Sales Order — an order the business sells to a customer |
| Owner | Highest role. Views the dashboard and the action report |
| Manager | Approves or rejects admin requests, sets order limits, views the dashboard |
| Admin | Prepares data and orders. Every action that changes data needs the Manager's approval |
| Staff | Warehouse worker who checks goods when they arrive, leave or are moved |
| Approval Request | A request created by an Admin that waits for the Manager's decision. Status: Pending, Approved, Rejected or Failed |
| Goods Receiving Check | The staff check of arriving goods against a PO |
| Goods Issue Check | The staff check of outgoing goods against an SO |
| Zone / Location | A section of the warehouse with a code, for example Z1 = Beds, Z2 = Cabinets. A product belongs to exactly one zone, and the zone code is the product's location |
| Order Limit | The minimum and maximum quantity of a product allowed on one PO line or one SO line |
| Low stock | A product whose `current_stock` is less than its `min_stock_alert` (default 20) |
| Price snapshot | Unit cost / unit price copied into an order line so later price changes do not affect old orders |
| Soft delete | Marking a record as deleted so it is hidden from new use, while old orders still show it |
| UI | User Interface |
| ER | Entity–Relationship |
| PK / FK | Primary Key / Foreign Key |

---

# 2. Overall Description

## 2.1 Product Perspective

StockTrack is a web application designed to replace paper-based recording within the business. It supports access through a web browser on two main devices: computers and mobile phones. Through the system, users can perform the following actions:

- log in and out, with the screens and actions limited by role
- send, approve and reject requests
- manage products, suppliers, customers and zones
- create purchase orders and check arriving goods
- create sales orders and check outgoing goods
- see the dashboard and a report of who did what

## 2.2 User Classes and Characteristics

| User Class | Description | Role / Goal | Main Functions |
|:---|:---|:---|:---|
| Owner | Runs the business. Needs an overview but does not handle daily work. | To oversee stock, profit and every action in the system. | View dashboard; view action report |
| Manager | Responsible for approving the work of admins and setting order limits. | To keep control of every change and to set limits on ordering and selling. | Approve or reject requests; set order limits; view dashboard |
| Admin | Office worker who prepares data and orders. Basic computer skills. | To prepare products, orders and moves quickly and correctly. | Request add/edit/delete of products; request add/edit/deactivate of suppliers and customers; manage zones; create PO/SO; request role change for Staff and Admin; view supplier and customer data when ordering |
| Staff | Warehouse worker who handles the real goods. Uses a mobile phone or tablet on the floor. | To check that goods match the order and confirm them. | View waiting receiving, issuing and move jobs; view the related PO and SO; check and confirm or reject the goods |

**Permission summary** (✔ = allowed directly, A = needs Manager approval, — = not allowed)

| Function | Owner | Manager | Admin | Staff |
|:---|:---:|:---:|:---:|:---:|
| View Dashboard / Profit | ✔ | ✔ | — | — |
| View action report | ✔ | — | — | — |
| Request role change (Staff / Admin) | — | — | A | — |
| Approve / reject requests | — | ✔ | — | — |
| Products: add / edit / delete | — | — | A | — |
| Suppliers, Customers: add / edit / deactivate | — | — | A | — |
| View supplier and customer data | — | — | ✔ (when ordering) | — |
| Zones: manage | — | — | A | — |
| View PO / SO (read-only for Staff) | — | ✔ | ✔ | ✔ |
| Order limits (min/max) | — | ✔ | — | — |
| Create PO / SO | — | — | A | — |
| Check goods receiving / issuing | — | — | — | ✔ |
| Verify and confirm product move | — | — | — | ✔ |

---

# 3. Software Specific Requirements

## 3.1 Overall Functional Requirements

| FR-No. | Function name | User class | Priority | Dependency |
|:---|:---|:---|:---|:---|
| FR-01 | User Login | All | High | None |
| FR-02 | User Logout | All | Low | FR-01 |
| FR-03 | Request Role Change | Admin | Medium | FR-05 |
| FR-04 | View My Requests | Admin | Medium | FR-01 |
| FR-05 | Approve or Reject Request | Manager | High | FR-01 |
| FR-06 | View Dashboard | Owner, Manager | High | FR-01 |
| FR-07 | Manage Products | Admin | High | FR-05, FR-11 |
| FR-08 | Manage Suppliers | Admin | High | FR-05 |
| FR-09 | Manage Customers | Admin | High | FR-05 |
| FR-10 | Set Order Limits | Manager | Medium | FR-07 |
| FR-11 | Manage Zones | Admin | High | FR-05 |
| FR-12 | Verify and Confirm Product Move | Staff | Medium | FR-11 |
| FR-13 | View Purchase Order List | Admin, Manager | High | FR-01 |
| FR-14 | Create Purchase Order | Admin | High | FR-07, FR-08, FR-10, FR-05 |
| FR-15 | View Purchase Order Detail | Admin, Manager, Staff | High | FR-13, FR-16 |
| FR-16 | View Receiving List | Staff | High | FR-05, FR-14 |
| FR-17 | Check and Confirm Goods Receipt | Staff | High | FR-16 |
| FR-18 | View Sales Order List | Admin, Manager | High | FR-01 |
| FR-19 | Create Sales Order | Admin | High | FR-07, FR-09, FR-10, FR-05 |
| FR-20 | View Sales Order Detail | Admin, Manager, Staff | High | FR-18, FR-21 |
| FR-21 | View Issuing List | Staff | High | FR-05, FR-19 |
| FR-22 | Check and Confirm Goods Issue | Staff | High | FR-21 |
| FR-23 | View Action Report | Owner | High | FR-01 |

## 3.2 Functional Requirements

> **Common rules for every Admin action in this section.** (a) The action does not change any data until the Manager approves it. (b) The system creates an Approval Request with status *Pending* and shows it in FR-04. (c) A *Pending* request never expires. (d) When the Manager approves, the system checks the data again. If the action can no longer be done (for example, the product was deleted or the stock changed in the meantime), the request becomes *Failed* with a message. (e) A *Rejected* or *Failed* request cannot be edited; the Admin must create a new request.

### FR-01 User Login

| FR-01 | User Login |
|:---|:---|
| Description | *The system shall verify the user's ID and password and allow access only to an active account. After login, the user sees only the menus allowed for the user's role.* |
| User Class (Actors) | Owner, Manager, Admin, Staff |
| Priority | High |
| Input | Username ID and Password |
| Output | Home page of the role on success<br>error message "Invalid ID or password" on failure |
| Workflow | 1. User enters ID and password.<br>2. System checks the ID, the password hash and that users.is_active is true.<br>3. If correct, the system opens the home page of the user's role.<br>4. If incorrect or inactive, the system shows the error message and stays on the Login page. |

### FR-02 User Logout

| FR-02 | User Logout |
|:---|:---|
| Description | *The system shall end the user's session and return to the Login page.* |
| User Class (Actors) | All |
| Priority | Low |
| Input | Click Logout |
| Output | Login page<br>Protected pages can no longer be opened without logging in again |
| Workflow | 1. User clicks Logout.<br>2. System ends the session.<br>3. System shows the Login page. |

### FR-03 Request Role Change

| FR-03 | Request Role Change |
|:---|:---|
| Description | *The system shall allow an Admin to request a change of role between Staff and Admin only. The request needs the Manager's approval.* |
| User Class (Actors) | Admin |
| Priority | Medium |
| Input | User whose current role is Staff or Admin; new role (Staff or Admin) |
| Output | Approval Request created<br>The role changes only after approval |
| Workflow | 1. Admin opens Roles.<br>2. Admin selects a person and a new role.<br>3. System refuses any role other than Staff or Admin, and refuses a change of the Admin's own role.<br>4. Otherwise the system creates an Approval Request (FR-05). |

### FR-04 View My Requests

| FR-04 | View My Requests |
|:---|:---|
| Description | *The system shall let the Admin see every request the Admin has submitted and its status.* |
| User Class (Actors) | Admin |
| Priority | Medium |
| Input | Status filter; date range |
| Output | Table of Request ID, Type, Detail, Submitted date, Status (Pending / Approved / Rejected / Failed), Decision reason, Decided by |
| Workflow | 1. Admin opens My Requests.<br>2. System shows the Admin's requests, newest first.<br>3. Admin filters.<br>4. For a Rejected or Failed request, the Admin reads the reason and, if needed, submits a new request. |

### FR-05 Approve or Reject Request

| FR-05 | Approve or Reject Request |
|:---|:---|
| Description | *The system shall let the Manager review each pending Approval Request and approve or reject it.* |
| User Class (Actors) | Manager |
| Priority | High |
| Input | Select a request; Approve, or Reject with a reason (required, 1–255 characters) |
| Output | Request status Approved (and the action carried out), Rejected (nothing changes) or Failed (with message)<br>Decision time and deciding user recorded |
| Workflow | 1. Manager opens the Approval Queue.<br>2. System lists Pending requests, oldest first.<br>3. Manager opens a request and sees who asked, what is requested and the data.<br>4. Manager clicks Approve or Reject.<br>5. On Approve, the system checks the data again and carries out the action in one transaction. If it cannot, the request becomes Failed.<br>6. On Reject, the system keeps the data unchanged and, for an order, sets its status to Rejected. |

### FR-06 View Dashboard

| FR-06 | View Dashboard |
|:---|:---|
| Description | *The system shall show summary figures on the Dashboard: total stock, number of low-stock products, best-selling and most profitable products of the current calendar month, and a weekly sales chart for the last 4 weeks.* |
| User Class (Actors) | Owner, Manager |
| Priority | High |
| Input | Open the Home page; switch between the "Sales" and "Profit" tabs |
| Output | Total stock, Low stock count, Best seller / Most profitable product<br>Bar chart of units sold per week (last 4 weeks)<br>Summary table per product |
| Workflow | 1. User opens Home.<br>2. System counts only sales orders with status Issued.<br>3. System calculates the figures from the sales order items and products.<br>4. System displays the cards, chart and table. Admin and Staff cannot open this page. |

### FR-07 Manage Products

| FR-07 | Manage Products |
|:---|:---|
| Description | *The system shall let the Admin request to add, edit and delete (deactivate) products. Deleting sets is_active = false so the product is hidden from new orders but stays in all old orders and reports.* |
| User Class (Actors) | Admin |
| Priority | High |
| Input | Product name (required); default cost (> 0); selling price (> 0); zone (required, one zone only, for example Z1, which is also the product's location); initial stock (integer ≥ 0, at creation only). The minimum stock alert is set later by the Manager (FR-10) |
| Output | Approval Request<br>After approval: product saved with system-generated Product ID and current_stock = initial stock<br>Confirmation message<br>Validation messages |
| Workflow | 1. Admin opens Product Management.<br>2. Admin clicks Add, Edit or Delete on a row.<br>3. For Add or Edit, Admin fills in the form and clicks Save. For Delete, Admin enters a reason.<br>4. System validates and creates an Approval Request.<br>5. After approval, the system saves the change. For Add, the system generates the Product ID. For Delete, the system sets is_active = false.<br>6. Changing a price does not change existing orders (price snapshot).<br>7. current_stock can only be changed by goods receipt (FR-17) and goods issue (FR-22); the only other time it is set is the initial stock entered when the product is created.<br>8. A product that has stock, or that is on an order that is not finished, cannot be deleted; the request fails with a message. |

### FR-08 Manage Suppliers

| FR-08 | Manage Suppliers |
|:---|:---|
| Description | *The system shall let the Admin view, and request to add, edit and deactivate, suppliers.* |
| User Class (Actors) | Admin |
| Priority | High |
| Input | Company name (required) |
| Output | Supplier list<br>Approval Request<br>Confirmation message<br>Validation messages |
| Workflow | 1. Admin opens Supplier Management.<br>2. Admin clicks Add, or Edit or Deactivate on a row.<br>3. Admin fills in the form and clicks Save.<br>4. System validates and creates an Approval Request.<br>5. After approval, the change is saved. Deactivate hides the supplier from new purchase orders; old orders are not changed. |

### FR-09 Manage Customers

| FR-09 | Manage Customers |
|:---|:---|
| Description | *The system shall let the Admin view, and request to add, edit and deactivate, customers.* |
| User Class (Actors) | Admin |
| Priority | High |
| Input | Customer name (required) |
| Output | Customer list<br>Approval Request<br>Confirmation message<br>Validation messages |
| Workflow | 1. Admin opens Customer Management.<br>2. Admin clicks Add, or Edit or Deactivate on a row.<br>3. Admin fills in the form and clicks Save.<br>4. System validates and creates an Approval Request.<br>5. After approval, the change is saved. Deactivate hides the customer from new sales orders; old orders are not changed. |

### FR-10 Set Order Limits

| FR-10 | Set Order Limits |
|:---|:---|
| Description | *The system shall let the Manager set, for each product, the minimum and maximum quantity allowed per PO line and per SO line, and the low-stock alert level. Changes take effect at once and need no approval.* |
| User Class (Actors) | Manager |
| Priority | Medium |
| Input | Product; PO min and PO max (integers, min ≥ 1, max ≥ min or empty for no limit); SO min and SO max (same rules); minimum stock alert (integer ≥ 0, default 20) |
| Output | Saved limits<br>Confirmation message<br>Validation messages |
| Workflow | 1. Manager opens Order Limits.<br>2. Manager selects a product.<br>3. Manager enters the values and clicks Save.<br>4. System validates and saves.<br>5. Default values are min = 1 and max = no limit.<br>6. The limits are checked when a PO or SO is created (FR-14, FR-19). Orders already created are not affected. |

### FR-11 Manage Zones

| FR-11 | Manage Zones |
|:---|:---|
| Description | *The system shall let the Admin request to add and edit zones, and request to move a product between zones. A zone has a code (for example Z1) and a name. The zone code is also the location of the products kept in it. Capacity limits are not recorded.* |
| User Class (Actors) | Admin |
| Priority | High |
| Input | Zone code (required, unique, for example Z1); zone name (required, unique, for example "Beds"); or product and destination zone when moving a product |
| Output | Zone list<br>Approval Request<br>For a product move, after approval a transfer job with status Pending Verification waiting for Staff (FR-12)<br>Confirmation message |
| Workflow | 1. Admin opens Zones.<br>2. Admin adds or edits a zone, or selects a product to move to another zone.<br>3. System creates an Approval Request.<br>4. After approval, for a zone add/edit the system saves the zone; for a product move the system creates a transfer job with status Pending Verification (the product's zone does not change until FR-12 is completed).<br>5. A zone that holds a product cannot be removed or have its code changed. |

### FR-12 Verify and Confirm Product Move

| FR-12 | Verify and Confirm Product Move |
|:---|:---|
| Description | *The system shall let Staff confirm that the product really exists in the stated quantity before the transfer is completed, or reject the transfer.* |
| User Class (Actors) | Staff |
| Priority | Medium |
| Input | Counted quantity (integer ≥ 0); Confirm, or Reject with a reason (required, 1–255 characters) |
| Output | Transfer job Completed (the product's zone updated), or Rejected with the counted quantity and the reason<br>The move is recorded with the staff user and date/time |
| Workflow | 1. Staff opens Transfer Product.<br>2. Staff selects a job and sees the product, quantity, current zone and destination zone.<br>3. Staff counts the goods and enters the counted quantity.<br>4. If the counted quantity equals current_stock, Staff clicks Confirm and the system updates the zone in one transaction.<br>5. Otherwise Staff must reject with a reason; nothing changes. |

### FR-13 View Purchase Order List

| FR-13 | View Purchase Order List |
|:---|:---|
| Description | *The system shall list purchase orders with their status, and allow searching and filtering by date range and status.* |
| User Class (Actors) | Admin, Manager |
| Priority | High |
| Input | Search text (order ID or supplier name); date range; status filter |
| Output | Table of Date, Order ID, Supplier, Qty, Total Price, Status, Action ([Detail])<br>"Add order" button (Admin only) |
| Workflow | 1. User opens the Purchase Order page.<br>2. System shows orders, newest first.<br>3. User searches or filters.<br>4. User clicks [Detail] to open FR-15, or "Add order" to open FR-14. |

### FR-14 Create Purchase Order

| FR-14 | Create Purchase Order |
|:---|:---|
| Description | *The system shall let the Admin create a purchase order for exactly one supplier containing one or more products, and send it to the Manager for approval. The goods are stored in the zone of each product.* |
| User Class (Actors) | Admin |
| Priority | High |
| Input | One active supplier; one or more lines of (product, quantity as integer, unit cost > 0 with default from default_cost) |
| Output | New PO with a unique Order ID, status Pending Approval, total amount = sum of line subtotals<br>Approval Request<br>Creating user and date/time recorded |
| Workflow | 1. Admin clicks "Add order".<br>2. Admin selects a supplier and can view the supplier's data.<br>3. Admin adds products with quantity and unit cost.<br>4. System checks that each quantity is within the PO min and max of the product (FR-10). If not, it shows "Quantity must be between X and Y" and does not save.<br>5. System calculates subtotals and total.<br>6. Admin clicks Save.<br>7. System saves the PO as Pending Approval and sends the request.<br>8. When the Manager approves, the status becomes Approved (waiting for receiving check); when the Manager rejects, the status becomes Rejected. |

### FR-15 View Purchase Order Detail

| FR-15 | View Purchase Order Detail |
|:---|:---|
| Description | *The system shall show the full detail of one purchase order. Staff can view it but cannot change it.* |
| User Class (Actors) | Admin, Manager, Staff |
| Priority | High |
| Input | Select an order from the list |
| Output | Created by; approved/rejected by with reason; supplier (ID, name); order ID; status; product lines (ID, name, quantity, unit cost, subtotal); total; date/time<br>Receiving check result with counted quantities (if checked) |
| Workflow | 1. User clicks [Detail].<br>2. System shows the review page.<br>3. Buttons depend on status and role: Pending Approval → Manager: Approve / Reject (FR-05); Approved, Received, Rejected, Rejected on Receipt → none. |

### FR-16 View Receiving List

| FR-16 | View Receiving List |
|:---|:---|
| Description | *The system shall list purchase orders that are approved and waiting to be checked, and those already checked.* |
| User Class (Actors) | Staff |
| Priority | High |
| Input | Search text; date range |
| Output | Table of Order Date, Order ID, Supplier, Total Qty, Total Price, Status (Approved = waiting to check, Received, Rejected on Receipt), Action ([Check]) |
| Workflow | 1. Staff opens Receiving.<br>2. System shows POs with status Approved, Received or Rejected on Receipt, with Approved first.<br>3. Staff searches or filters.<br>4. Staff clicks [Check] to open FR-17. |

### FR-17 Check and Confirm Goods Receipt

| FR-17 | Check and Confirm Goods Receipt |
|:---|:---|
| Description | *The system shall let Staff check the arriving goods against the PO and either accept the whole order, which increases the stock, or reject the whole order. Only an Approved PO can be checked, and only once.* |
| User Class (Actors) | Staff |
| Priority | High |
| Input | For each line: counted quantity (integer ≥ 0) and a tick that the product is the right one; Accept, or Reject with a reason (required, 1–255 characters) |
| Output | Receiving check record (checked by, date/time, ordered and counted quantity per line, result)<br>Accepted: current_stock of each product increased by its quantity, PO status Received<br>Rejected: PO status Rejected on Receipt, reason and counted quantities saved, stock unchanged |
| Workflow | 1. Staff opens an Approved PO.<br>2. System shows each product with the ordered quantity.<br>3. Staff counts and enters the real quantity and ticks the right product.<br>4. Accept is available only if every line matches the order exactly.<br>5. If anything is missing, extra or wrong, Staff must click Reject and enter the reason and the counted quantity.<br>6. On Accept, the system creates the record, increases the stock and sets the status. These steps succeed or fail together. |

### FR-18 View Sales Order List

| FR-18 | View Sales Order List |
|:---|:---|
| Description | *The system shall list sales orders with their status, and allow searching and filtering by date range and status.* |
| User Class (Actors) | Admin, Manager |
| Priority | High |
| Input | Search text (order ID or customer name); date range; status filter |
| Output | Table of Order Date, Order ID, Customer, Total Price, Status, Action ([Detail])<br>"Add order" button (Admin only) |
| Workflow | 1. User opens the Sales Order page.<br>2. System shows orders, newest first.<br>3. User searches or filters.<br>4. User clicks [Detail] (FR-20) or "Add order" (FR-19). |

### FR-19 Create Sales Order

| FR-19 | Create Sales Order |
|:---|:---|
| Description | *The system shall let the Admin create a sales order for exactly one customer containing one or more products, block the sale when stock is not enough, and send the order to the Manager for approval.* |
| User Class (Actors) | Admin |
| Priority | High |
| Input | One active customer; one or more lines of (product, quantity as integer) |
| Output | New SO with unique Order ID, status Pending Approval<br>Each line stores unit_price (from selling_price) and unit_cost_snapshot (from default_cost)<br>Total amount; creating user and date/time recorded; Approval Request |
| Workflow | 1. Admin clicks "Add order".<br>2. Admin selects a customer and can view the customer's data.<br>3. Admin adds products and quantities.<br>4. For each line the system checks the quantity against the SO min and max (FR-10) and checks quantity ≤ current_stock.<br>5. If not, it shows "Quantity must be between X and Y" or "Insufficient stock (available: N)" and does not allow saving.<br>6. Admin clicks Save.<br>7. System saves the SO as Pending Approval and sends the request.<br>8. When the Manager approves, the status becomes Approved (waiting for issue check); when the Manager rejects, the status becomes Rejected. Stock is not reduced until the goods are issued. |

### FR-20 View Sales Order Detail

| FR-20 | View Sales Order Detail |
|:---|:---|
| Description | *The system shall show the full detail of one sales order. Staff can view it but cannot change it.* |
| User Class (Actors) | Admin, Manager, Staff |
| Priority | High |
| Input | Select an order from the list |
| Output | Created by; approved/rejected by with reason; customer (ID, name); order ID; status; product lines (ID, name, quantity, unit price, subtotal); total; date/time<br>Issue check result with counted quantities (if checked) |
| Workflow | 1. User clicks [Detail].<br>2. System shows the review page.<br>3. Buttons depend on status and role: Pending Approval → Manager: Approve / Reject (FR-05); Approved, Issued, Rejected, Rejected on Issue → none. |

### FR-21 View Issuing List

| FR-21 | View Issuing List |
|:---|:---|
| Description | *The system shall list sales orders that are approved and waiting to be issued, and those already checked.* |
| User Class (Actors) | Staff |
| Priority | High |
| Input | Search text; date range |
| Output | Table of Order Date, Order ID, Customer, Total Qty, Total Price, Status (Approved = waiting to issue, Issued, Rejected on Issue), Action ([Check]) |
| Workflow | 1. Staff opens Issuing.<br>2. System shows SOs with status Approved, Issued or Rejected on Issue, with Approved first.<br>3. Staff searches or filters.<br>4. Staff clicks [Check] to open FR-22. |

### FR-22 Check and Confirm Goods Issue

| FR-22 | Check and Confirm Goods Issue |
|:---|:---|
| Description | *The system shall let Staff check the goods against the SO and either issue the whole order, which decreases the stock, or reject the whole order. Only an Approved SO can be checked, and only once.* |
| User Class (Actors) | Staff |
| Priority | High |
| Input | For each line: counted quantity available (integer ≥ 0) and a tick that the product is the right one; Issue, or Reject with a reason (required, 1–255 characters) |
| Output | Issue check record (checked by, date/time, ordered and counted quantity per line, result)<br>Issued: current_stock of each product reduced by its quantity, SO status Issued<br>Rejected: SO status Rejected on Issue, reason and counted quantities saved, stock unchanged |
| Workflow | 1. Staff opens an Approved SO.<br>2. System shows each product with the quantity to send and its zone.<br>3. Staff picks the goods, enters the counted quantity and ticks the right product.<br>4. Issue is available only if every line is complete and current_stock is enough.<br>5. If anything is missing or wrong, Staff must click Reject and enter the reason and the counted quantity.<br>6. On Issue, the system saves the record, subtracts the stock and sets the status. These steps succeed or fail together. |

### FR-23 View Action Report

| FR-23 | View Action Report |
|:---|:---|
| Description | *The system shall let the Owner see a report of who did what and when. The report cannot be edited or deleted by anyone.* |
| User Class (Actors) | Owner |
| Priority | High |
| Input | Filter by user, date range and action type |
| Output | Table of Date/Time, User, Role, Action type, Detail (for example Accepted PO 20267, all 11 beds counted)<br>Covers requests (who asked, who approved or rejected), data changes, receiving checks, issue checks, moves and role changes. Login and Logout are not included |
| Workflow | 1. Owner opens the Action Report.<br>2. System shows entries, newest first.<br>3. Owner filters by user, date range or action type.<br>4. The Owner can only view; there are no edit or delete buttons. |

## 3.3 Non-Functional Requirements

| NFR-No. | Name | Requirement Statement | Acceptance Criteria | Verification Method | Related FR |
|:---|:---|:---|:---|:---|:---|
| NFR-01 | Security – Password | The system shall store passwords using a secure one-way hash (e.g., bcrypt) and shall not store plain text. | No password in the database is readable as plain text. | Inspection / Security Test | FR-01 |
| NFR-02 | Security – Role-Based Access | The system shall allow each page and function only for the roles listed in section 2.2, and shall check this on the server, not only by hiding menus. | A user who opens a URL or calls a function outside the user's role is refused. Opening any protected URL without logging in redirects to Login. | Security Test | All FR |
| NFR-03 | Security – Session | The system shall end a session automatically after 30 minutes of inactivity. | After 30 idle minutes the next action requires login again. | Functional Test | FR-01, FR-02 |
| NFR-04 | Security – Privilege Escalation | An Admin shall not be able to give anyone the Manager or Owner role, or change the Admin's own role. | Such a request is refused by the system. | Security Test | FR-03 |
| NFR-05 | Approval Enforcement | No Admin action that changes data shall take effect before the Manager approves it. | Before approval the data is unchanged; after rejection it stays unchanged. | Functional Test | FR-03, FR-07 to FR-09, FR-11, FR-14, FR-19 |
| NFR-06 | Performance – Response | The system shall display list pages and search/filter results within 3 seconds with up to 10,000 orders in the database. | 95% of list/search requests finish within 3 seconds. | Performance Test | FR-13, FR-16, FR-18, FR-21 |
| NFR-07 | Performance – Dashboard | The system shall display the Dashboard within 5 seconds with up to 10,000 sales order lines. | Dashboard is fully displayed within 5 seconds. | Performance Test | FR-06 |
| NFR-08 | Data Integrity – Transaction | Approval, goods receipt, goods issue and product move, together with their stock updates, shall be completed fully or not at all. | After a forced failure during these operations, order status and `current_stock` remain consistent. | Test with failure injection / Inspection | FR-05, FR-12, FR-17, FR-22 |
| NFR-09 | Data Integrity – One Record | An order shall have at most one receiving or issue check, and at most one open Approval Request. | A second check or request for the same order is rejected; the database has unique constraints on these references. | Functional Test / Inspection | FR-17, FR-22 |
| NFR-10 | Data Integrity – Stock | `current_stock` of a product shall never be negative, and shall change only through an accepted goods receipt, an issued goods issue, or the initial stock entered when the product is created. | Any action that would make stock negative is rejected with a message. No other function changes stock. | Functional Test | FR-07, FR-17, FR-19, FR-22 |
| NFR-11 | Data Integrity – Zone | A product shall belong to exactly one zone (its location), and zone codes shall be unique. | The database refuses a second zone for a product and a duplicate zone code. | Functional Test / Inspection | FR-07, FR-11, FR-12 |
| NFR-12 | Data Accuracy – Price Snapshot | Editing a product price shall not change the amounts, costs or profit of existing orders. | After a price change, old orders and the profit report show the old values. | Functional Test | FR-06, FR-07, FR-14, FR-19 |
| NFR-13 | Traceability – Action Record | The system shall record the user and date/time of every request, approval, rejection, check, move and role change, and shall never let anyone edit or delete these records. | Every action in the system appears in the Action Report. No edit or delete function exists for these records or for orders and master data. | Inspection / Functional Test | FR-23 and all others |
| NFR-14 | Usability | A first-time Staff member shall be able to check and accept a goods receipt within 3 minutes after a 10-minute introduction. | At least 4 of 5 test users succeed within 3 minutes. | Usability Test | FR-17, FR-22 |
| NFR-15 | Compatibility / Responsiveness | The system shall work in the latest versions of Chrome, Edge, Safari and Firefox, and the layout shall fit screen widths from 360 px to 1920 px without horizontal scrolling. | All main pages are usable at 360 px and 1920 px in the listed browsers. | Compatibility Test | All FR |
| NFR-16 | Availability | The system shall be available 99% of the time during business hours (08:00–18:00). | Monthly uptime during business hours ≥ 99%. | Monitoring / Inspection | All FR |
| NFR-17 | Concurrency | The system shall support at least 10 simultaneous sessions without exceeding the response times of NFR-06. | Load test with 10 users meets NFR-06. | Performance Test | All FR |
| NFR-18 | Data Format | Dates shall be shown as dd/mm/yyyy (Christian Era), times as 24-hour HH:mm, and money in Thai Baht (฿) with thousand separators and two decimals. | All screens use these formats. | Inspection | All FR |
| NFR-19 | Backup | The database shall be backed up automatically at least once a day and the last 7 backups shall be kept. | A backup exists for each of the last 7 days and can be restored in a test. | Inspection / Restore Test | All FR |

> [!NOTE]
> ตัวเลขใน NFR (3 วินาที, 10 ผู้ใช้, 99% ฯลฯ) เป็นค่าที่เสนอ ปรับได้ตามที่อาจารย์/ลูกค้าต้องการ

---

# 4. User Interface Requirements

## 4.1 Navigation Structure / Sitemap

Every user starts at **Login (`UI-01`)**. After login, the system routes the user to the menu structure of the user's role. All roles have a user icon with **Logout**.

| Role | Main Menu | Sub-pages / Flow |
|:---|:---|:---|
| **Owner** | Dashboard<br>Action Report | —<br>— |
| **Manager** | Dashboard<br>View Requests | —<br>Role, Purchase Order / Sales Order, Product, Zone |
| **Admin** | My Requests<br>Request Role Change<br>Manage<br>Purchase Order<br>Sales Order | —<br>—<br>Zone Detail, Manage Product<br>Create PO $\rightarrow$ Suppliers detail $\rightarrow$ PO Detail<br>Create SO $\rightarrow$ Suppliers detail $\rightarrow$ SO Detail |
| **Staff** | Receiving<br>Issuing<br>Transfer Product | Receiving Check $\rightarrow$ PO Detail<br>Issue Check $\rightarrow$ SO Detail<br>Transfer Product Check $\rightarrow$ Transfer Product Detail |

## 4.2 User Interface Specifications

> Mockup เดิม 6 ภาพยังเป็นของระบบ 1 role ต้องปรับตามหน้าใหม่ ถ้าหน้าไหนไม่มี mockup ให้ใส่ *(none — draw wireframe)*

### UI-01: Login page
**Mockup:** *(none — draw wireframe)*
**Purpose:** Let any user log in.
**Prerequisite:** The user has an active account.
**Use case procedure:**
1. User enters Username ID and password.
2. User clicks "Login".
3. System opens the home page of the user's role.

**Acceptance Criteria**
- The password field hides the characters.
- Wrong ID or password shows "Invalid ID or password".
- Empty fields show a "required" message and are not submitted.
- A deactivated account cannot log in.

**Related Function Requirement:** FR-01, FR-02

### UI-02: Dashboard (Overview and Profit)
**Mockup:** `mockups/01_overview_dashboard.png`
**Purpose:** Show stock, low stock, best-selling and most profitable products and weekly sales.
**Prerequisite:** The user is an Owner or a Manager.
**Use case procedure:**
1. The page shows the cards "Total stock" and "Low stock".
2. The page shows the card "Best seller: month" (or "Most Profitable: month").
3. The bar chart "Weekly Sales (units)" shows the last 4 weeks.
4. The user switches the "Sales" / "Profit" tab.
5. The user clicks the "Low stock" card to see the list.

**Acceptance Criteria**
- Total stock equals the sum of `current_stock` of all active products (`is_active = true`).
- Low stock equals the number of active products with `current_stock < min_stock_alert`.
- Best seller, profit and chart count only *Issued* orders.
- Percent shares of the table add up to 100%.
- Admin and Staff cannot open this page.
- Page loads within 5 seconds.

**Related Function Requirement:** FR-06

### UI-03: Approval Queue
**Mockup:** *(none — draw wireframe)*
**Purpose:** Let the Manager review and decide on Admin requests.
**Prerequisite:** The user is a Manager.
**Use case procedure:**
1. The table shows pending requests: Request ID, Type, Requested by, Date, Detail.
2. Manager clicks a row to see the detail of the request.
3. Manager clicks Approve, or Reject and enters a reason.

**Acceptance Criteria**
- Only *Pending* requests are listed in the queue; decided requests can be found by a filter.
- Reject requires a reason.
- If approval fails, the request shows *Failed* with the message.
- The decision, user and date/time are saved.

**Related Function Requirement:** FR-05

### UI-04: My Requests
**Mockup:** *(none — draw wireframe)*
**Purpose:** Let the Admin follow the status of submitted requests.
**Prerequisite:** The user is an Admin.
**Use case procedure:**
1. The table shows Request ID, Type, Detail, Date, Status and Decision reason.
2. Admin filters by status and date.

**Acceptance Criteria**
- A *Rejected* or *Failed* request has no edit button; only a note to submit a new request.
- Statuses are Pending, Approved, Rejected and Failed.

**Related Function Requirement:** FR-04

### UI-05: Purchase Order list
**Mockup:** `mockups/02_purchase_orders_list.png` *(add Status column and filter)*
**Purpose:** Show all purchase orders and open details or create a new one.
**Prerequisite:** The user is an Admin or a Manager.
**Use case procedure:**
1. The table shows Date, Order ID, Supplier, Qty, Total Price, Status and Action.
2. The user searches and filters by date range and status.
3. The user clicks [Detail] to open UI-07, or the Admin clicks "Add order" to open UI-06.

**Acceptance Criteria**
- Orders are shown newest first.
- Each row shows one of: Pending Approval, Rejected, Approved, Received, Rejected on Receipt.
- "Add order" is shown to Admin only.
- "No results found" is shown when nothing matches.
- Money is shown in Baht with thousand separators; dates as dd/mm/yyyy.

**Related Function Requirement:** FR-13

### UI-06: Create Purchase Order
**Mockup:** *(none — draw wireframe)*
**Purpose:** Create a purchase order for one supplier with many products.
**Prerequisite:** Admin clicked "Add order" on UI-05; at least one active supplier and product exist.
**Use case procedure:**
1. Admin selects a supplier (active only) and can see the supplier's data.
2. Admin clicks "Add product", selects a product and enters quantity and unit cost.
3. System shows the allowed quantity range, each subtotal and the total.
4. Admin clicks Save, or Cancel to discard.

**Acceptance Criteria**
- Only one supplier per order.
- A quantity outside the PO min–max of the product shows "Quantity must be between X and Y" and cannot be saved.
- The goods are stored in the zone of each product; no location is chosen on the order.
- Saving with no product line is rejected with a message.
- After saving, the order has status *Pending Approval*.

**Related Function Requirement:** FR-14

### UI-07: Purchase Order detail
**Mockup:** `mockups/03_purchase_order_review_receipt.png` *(adapt: remove Cash Receipt dialog, update buttons and statuses)*
**Purpose:** Review an order and approve or reject it.
**Prerequisite:** The user clicked [Detail] on UI-05, or Staff opened the order from UI-08 (Staff can only view).
**Use case procedure:**
1. The Review panel shows created-by, approval, supplier, order ID, product lines, total and date/time.
2. *Pending Approval*: Manager clicks Approve or Reject.
3. If the order has been checked, the receiving result and counted quantities are shown.

**Acceptance Criteria**
- Buttons shown depend on role and status (see FR-15).
- After Approve, the status is *Approved* and the order appears in the Receiving list.

**Related Function Requirement:** FR-15

### UI-08: Receiving list
**Mockup:** `mockups/04_stock_in_list.png` *(adapt: Status column, [Check] button)*
**Purpose:** Show purchase orders waiting to be checked or already checked.
**Prerequisite:** The user is Staff.
**Use case procedure:**
1. The table shows Order Date, Order ID, Supplier, Total Qty, Total Price, Status and Action.
2. Staff searches and filters by date range.
3. Staff clicks [Check] to open UI-09, or [Detail] to view the order (UI-07).

**Acceptance Criteria**
- Only orders with status *Approved*, *Received* or *Rejected on Receipt* are listed; *Approved* first.
- Staff can view the whole PO but cannot change it.
- "No results found" is shown when nothing matches.

**Related Function Requirement:** FR-16

### UI-09: Goods Receiving Check
**Mockup:** *(none — draw wireframe)*
**Purpose:** Check arriving goods against the PO and accept or reject the whole order.
**Prerequisite:** Staff clicked [Check] on an *Approved* order.
**Use case procedure:**
1. The page shows each product and ordered quantity.
2. Staff enters the counted quantity and ticks the right product on each line.
3. Staff clicks Accept, or Reject and enters a reason.

**Acceptance Criteria**
- Accept is enabled only when every line matches exactly.
- Reject requires a reason and the counted quantities.
- After Accept, the status is *Received* and stock increases.
- After Reject, the status is *Rejected on Receipt* and stock is unchanged.
- An order that has been checked cannot be checked again.

**Related Function Requirement:** FR-17

### UI-10: Sales Order list
**Mockup:** `mockups/05_sales_orders_list.png` *(add Status column and filter)*
**Purpose:** Show all sales orders and open details or create a new one.
**Prerequisite:** The user is an Admin or a Manager.
**Use case procedure:**
1. The table shows Order Date, Order ID, Customer, Total Price, Status and Action.
2. The user searches and filters by date range and status.
3. The user clicks [Detail] to open UI-12, or the Admin clicks "Add order" to open UI-11.

**Acceptance Criteria**
- Orders are shown newest first.
- Each row shows one of: Pending Approval, Rejected, Approved, Issued, Rejected on Issue.
- "No results found" is shown when nothing matches.
- Money is shown in Baht; dates as dd/mm/yyyy.

**Related Function Requirement:** FR-18

### UI-11: Create Sales Order
**Mockup:** *(none — draw wireframe)*
**Purpose:** Create a sales order for one customer with many products.
**Prerequisite:** Admin clicked "Add order" on UI-10; active customers and products exist.
**Use case procedure:**
1. Admin selects a customer (active only) and can see the customer's data.
2. Admin adds products and quantities; the unit price is shown from the product's selling price.
3. System shows the available stock and the allowed quantity range beside each product, and the total.
4. Admin clicks Save, or Cancel to discard.

**Acceptance Criteria**
- Only one customer per order.
- Quantity outside the SO min–max shows "Quantity must be between X and Y".
- Quantity greater than the available stock shows "Insufficient stock (available: N)" and Save is disabled.
- After saving, the status is *Pending Approval*; stock is not reduced.

**Related Function Requirement:** FR-19

### UI-12: Sales Order detail
**Mockup:** `mockups/06_sales_order_review_receipt.png` *(adapt: remove Cash Receipt section, add approval buttons)*
**Purpose:** Review an order and approve or reject it.
**Prerequisite:** The user clicked [Detail] on UI-10, or Staff opened the order from UI-13 (Staff can only view).
**Use case procedure:**
1. The Review panel shows created-by, approval, customer, order ID, product lines, total and date/time.
2. *Pending Approval*: Manager clicks Approve or Reject.
3. If the order has been checked, the issue result and counted quantities are shown.

**Acceptance Criteria**
- Buttons shown depend on role and status (see FR-20).
- After Approve, the status is *Approved* and the order appears in the Issuing list.

**Related Function Requirement:** FR-20

### UI-13: Issuing list
**Mockup:** *(none — adapt UI-08)*
**Purpose:** Show sales orders waiting to be issued or already checked.
**Prerequisite:** The user is Staff.
**Use case procedure:**
1. The table shows Order Date, Order ID, Customer, Total Qty, Total Price, Status and Action.
2. Staff searches and filters by date range.
3. Staff clicks [Check] to open UI-14, or [Detail] to view the order (UI-12).

**Acceptance Criteria**
- Only orders with status *Approved*, *Issued* or *Rejected on Issue* are listed; *Approved* first.
- Staff can view the whole SO but cannot change it.

**Related Function Requirement:** FR-21

### UI-14: Goods Issue Check
**Mockup:** *(none — draw wireframe)*
**Purpose:** Check outgoing goods against the SO and issue or reject the whole order.
**Prerequisite:** Staff clicked [Check] on an *Approved* order.
**Use case procedure:**
1. The page shows each product, quantity to send and its zone.
2. Staff enters the counted quantity and ticks the right product on each line.
3. Staff clicks Issue, or Reject and enters a reason.

**Acceptance Criteria**
- Issue is enabled only when every line is complete.
- Reject requires a reason and the counted quantities.
- After Issue, the status is *Issued* and stock decreases.
- After Reject, the status is *Rejected on Issue* and stock is unchanged.

**Related Function Requirement:** FR-22

### UI-15: Product Management
**Mockup:** *(none — draw wireframe)*
**Purpose:** View products and request add, edit and delete (deactivate).
**Prerequisite:** The user is an Admin.
**Use case procedure:**
1. The table shows Product ID, Name, Zone, Default cost, Selling price, Current stock, Min. stock alert, Active.
2. Admin searches by name.
3. Admin clicks "Add product" or [Edit], fills in the form and clicks Save; or clicks [Delete] and enters a reason.

**Acceptance Criteria**
- Name and zone (for example Z1) are required; cost and price must be > 0; initial stock is an integer ≥ 0 and is entered only when adding a product.
- Product ID is generated by the system after approval and cannot be typed.
- Current stock cannot be edited after the product is created.
- Every Save or Delete creates an Approval Request and shows a confirmation message.
- Deactivated (`is_active = false`) products do not appear in new orders but remain in old orders.

**Related Function Requirement:** FR-07

### UI-16: Supplier Management
**Mockup:** *(none — draw wireframe)*
**Purpose:** View suppliers and request add, edit and deactivate.
**Prerequisite:** The user is an Admin.
**Use case procedure:**
1. The table shows Supplier ID, Company name, Active.
2. Admin searches, clicks "Add supplier", [Edit] or [Deactivate], fills in the form and clicks Save.

**Acceptance Criteria**
- Company name is required.
- Every change creates an Approval Request.
- Deactivated suppliers do not appear in new POs but remain in old orders.

**Related Function Requirement:** FR-08

### UI-17: Customer Management
**Mockup:** *(none — draw wireframe)*
**Purpose:** View customers and request add, edit and deactivate.
**Prerequisite:** The user is an Admin.
**Use case procedure:**
1. The table shows Customer ID, Customer name, Active.
2. Admin searches, clicks "Add customer", [Edit] or [Deactivate], fills in the form and clicks Save.

**Acceptance Criteria**
- Customer name is required.
- Every change creates an Approval Request.
- Deactivated customers do not appear in new SOs but remain in old orders.

**Related Function Requirement:** FR-09

### UI-18: Zones
**Mockup:** *(none — draw wireframe)*
**Purpose:** View zones, request zone additions or edits, and request moving products between zones.
**Prerequisite:** The user is an Admin.
**Use case procedure:**
1. The page lists each zone (code and name, for example Z1 Beds) with the products kept in it.
2. Admin clicks "Add zone" or [Edit] and fills in the form, or selects a product to move to another zone and clicks Submit.

**Acceptance Criteria**
- Zone code and zone name are unique.
- Every change creates an Approval Request.
- For a product move, the whole stock is moved and waits for Staff verification after Manager approval.
- No capacity field is shown.

**Related Function Requirement:** FR-11

### UI-19: Transfer Product and Verification
**Mockup:** *(none — draw wireframe)*
**Purpose:** Let Staff verify and complete product transfers between zones.
**Prerequisite:** The user is Staff.
**Use case procedure:**
1. The table shows approved transfers: Product, Quantity, From, To, Status.
2. Staff opens a transfer job, counts the goods and enters the counted quantity.
3. Staff clicks Confirm, or Reject and enters a reason.

**Acceptance Criteria**
- Confirm is enabled only when the counted quantity equals the stock in the system.
- Reject requires a reason.
- After Confirm, the product's zone is updated.

**Related Function Requirement:** FR-12

### UI-20: Order Limits
**Mockup:** *(none — draw wireframe)*
**Purpose:** Let the Manager set min/max quantities and low-stock alert level.
**Prerequisite:** The user is a Manager.
**Use case procedure:**
1. The table shows each product with PO min/max, SO min/max and Min. stock alert.
2. Manager edits the values and clicks Save.

**Acceptance Criteria**
- Min is an integer ≥ 1; max is empty or ≥ min.
- Min. stock alert is an integer ≥ 0 (default 20).
- Changes take effect at once and do not affect existing orders.

**Related Function Requirement:** FR-10

### UI-21: Roles
**Mockup:** *(none — draw wireframe)*
**Purpose:** Request changing the role of a Staff or Admin user.
**Prerequisite:** The user is an Admin.
**Use case procedure:**
1. The table shows User ID, Name, Role, Active.
2. Admin selects a Staff or Admin user and a new role of Staff or Admin, then submits a request.

**Acceptance Criteria**
- Only Staff and Admin roles can be selected.
- An Admin cannot change the Admin's own role.
- An Admin's change is shown as a pending request until approved by the Manager.

**Related Function Requirement:** FR-03

### UI-22: Action Report
**Mockup:** *(none — draw wireframe)*
**Purpose:** Let the Owner see who did what.
**Prerequisite:** The user is an Owner.
**Use case procedure:**
1. The table shows Date/Time, User, Role, Action type and Detail.
2. Owner filters by user, date range and action type.

**Acceptance Criteria**
- Entries are read-only; no edit or delete button.
- Login and Logout are not listed.
- Entries are shown newest first.

**Related Function Requirement:** FR-23

---


# Appendix A: Data Dictionary

> วาง ER Diagram ฉบับใหม่ไว้ก่อนตาราง (ยังไม่ได้วาด) แล้วคัดลอกตารางต่อไปนี้ไปวางทีละตาราง
> Data type อิงแบบ MySQL/PostgreSQL ทั่วไป สามารถเปลี่ยนตาม DBMS ที่ใช้ได้
> ตารางใหม่: ZONES, APPROVAL_REQUESTS, RECEIVING_CHECKS, RECEIVING_CHECK_ITEMS, ISSUE_CHECKS, ISSUE_CHECK_ITEMS, STOCK_MOVEMENTS, AUDIT_LOGS (รวมเป็น 16 ตาราง — ตัดตารางใบเสร็จ PURCHASE_RECEIPTS และ SALES_RECEIPTS ออก)

**Relationship summary:** zones 1–N products; zones 1–N stock_movements (from_zone and to_zone); users 1–N approval_requests (requested by); users 1–N approval_requests (decided by, optional); suppliers 1–N purchase_orders; users 1–N purchase_orders (created by); purchase_orders 1–N purchase_order_items; products 1–N purchase_order_items; purchase_orders 1–1 receiving_checks; receiving_checks 1–N receiving_check_items; users 1–N receiving_checks; customers 1–N sales_orders; users 1–N sales_orders (created by); sales_orders 1–N sales_order_items; products 1–N sales_order_items; sales_orders 1–1 issue_checks; issue_checks 1–N issue_check_items; users 1–N issue_checks; products 1–N stock_movements; approval_requests 1–1 stock_movements; users 1–N stock_movements (verified by, optional); users 1–N audit_logs.

### Table: USERS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| USER_ID | User ID | Integer | PK | 1001 |
| FULL_NAME | Full name | Varchar(50) | | Rattapoom |
| PASSWORD_HASH | Hashed password | Varchar(60) | | \$2a\$12\$e8kNZ9qo8uLOic... |
| ROLE | User role (OWNER / MANAGER / ADMIN / STAFF) | Varchar(10) | | ADMIN |
| IS_ACTIVE | Active status flag (1 = active) | Boolean | | 1 |

### Table: ZONES
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| ZONE_ID | Zone ID | Integer | PK | 1 |
| ZONE_CODE | Zone code / location | Varchar(5) | | Z1 |
| ZONE_NAME | Zone name | Varchar(30) | | Beds |

### Table: SUPPLIERS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| SUPPLIER_ID | Supplier ID | Integer | PK | 2001 |
| COMPANY_NAME | Company name | Varchar(50) | | JRScomp. |
| IS_ACTIVE | Active status flag (1 = active) | Boolean | | 1 |

### Table: CUSTOMERS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| CUSTOMER_ID | Customer ID | Integer | PK | 3001 |
| CUSTOMER_NAME | Customer name | Varchar(50) | | RWTcomp |
| IS_ACTIVE | Active status flag (1 = active) | Boolean | | 1 |

### Table: PRODUCTS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| PRODUCT_ID | Product ID | Integer | PK | 5001 |
| PRODUCT_NAME | Product name | Varchar(50) | | Wooden bed |
| DEFAULT_COST | Default purchase cost per unit | Decimal(10,2) | | 800.00 |
| SELLING_PRICE | Selling price per unit | Decimal(10,2) | | 1200.00 |
| CURRENT_STOCK | Current quantity in warehouse | Integer | | 89 |
| MIN_STOCK_ALERT | Low stock alert threshold | Integer | | 20 |
| PO_MIN_QTY | Minimum quantity per PO line | Integer | | 5 |
| PO_MAX_QTY | Maximum quantity per PO line | Integer | | 100 |
| SO_MIN_QTY | Minimum quantity per SO line | Integer | | 1 |
| SO_MAX_QTY | Maximum quantity per SO line | Integer | | 20 |
| ZONE_ID | Warehouse zone ID | Integer | FK | 1 |
| IS_ACTIVE | Active status flag (1 = active) | Boolean | | 1 |

### Table: APPROVAL_REQUESTS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| REQUEST_ID | Request ID | Integer | PK | 501 |
| REQUEST_TYPE | Type of approval request | Varchar(20) | | PO_CREATE |
| DETAIL | Details of the requested change | Varchar(100) | | Create PO 20267 |
| STATUS | Approval status | Varchar(10) | | PENDING |
| REQUESTED_BY_USER_ID | Requester user ID | Integer | FK | 1001 |
| REQUESTED_DATETIME | Request date and time | Datetime | | 2026-09-22 16:00:00 |
| DECIDED_BY_USER_ID | Approver user ID | Integer | FK | 1002 |
| DECIDED_DATETIME | Decision date and time | Datetime | | 2026-09-22 16:30:00 |
| DECISION_REASON | Reason for rejection or failure | Varchar(100) | | Quantity too high |

### Table: PURCHASE_ORDERS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| PO_ID | Purchase order ID | Integer | PK | 20267 |
| SUPPLIER_ID | Supplier ID | Integer | FK | 2001 |
| CREATED_BY_USER_ID | Creator user ID | Integer | FK | 1001 |
| ORDER_DATETIME | Order date and time | Datetime | | 2026-09-22 16:09:00 |
| TOTAL_AMOUNT | Total order amount | Decimal(10,2) | | 8800.00 |
| STATUS | Purchase order status | Varchar(20) | | APPROVED |

### Table: PURCHASE_ORDER_ITEMS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| PO_ITEM_ID | PO item line ID | Integer | PK | 1 |
| PO_ID | Purchase order ID | Integer | FK | 20267 |
| PRODUCT_ID | Product ID | Integer | FK | 5001 |
| QUANTITY | Ordered quantity | Integer | | 11 |
| UNIT_COST | Cost per unit at order time | Decimal(10,2) | | 800.00 |
| SUBTOTAL | Line subtotal amount | Decimal(10,2) | | 8800.00 |

### Table: RECEIVING_CHECKS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| CHECK_ID | Receiving check ID | Integer | PK | 1 |
| PO_ID | Purchase order ID | Integer | FK | 20267 |
| CHECKED_BY_USER_ID | Checker user ID | Integer | FK | 1003 |
| CHECKED_DATETIME | Check date and time | Datetime | | 2026-09-24 10:30:00 |
| RESULT | Inspection result | Varchar(10) | | ACCEPTED |
| REJECT_REASON | Reason for rejection | Varchar(100) | | 2 beds missing |

### Table: RECEIVING_CHECK_ITEMS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| CHECK_ITEM_ID | Check item line ID | Integer | PK | 1 |
| CHECK_ID | Receiving check ID | Integer | FK | 1 |
| PO_ITEM_ID | PO item line ID | Integer | FK | 1 |
| EXPECTED_QTY | Expected quantity | Integer | | 11 |
| COUNTED_QTY | Actually counted quantity | Integer | | 11 |
| IS_CORRECT_PRODUCT | Correct product check flag | Boolean | | 1 |

### Table: SALES_ORDERS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| SO_ID | Sales order ID | Integer | PK | 20267 |
| CUSTOMER_ID | Customer ID | Integer | FK | 3001 |
| CREATED_BY_USER_ID | Creator user ID | Integer | FK | 1001 |
| ORDER_DATETIME | Order date and time | Datetime | | 2026-09-22 14:20:00 |
| TOTAL_AMOUNT | Total order amount | Decimal(10,2) | | 15200.00 |
| STATUS | Sales order status | Varchar(20) | | APPROVED |

### Table: SALES_ORDER_ITEMS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| SO_ITEM_ID | SO item line ID | Integer | PK | 1 |
| SO_ID | Sales order ID | Integer | FK | 20267 |
| PRODUCT_ID | Product ID | Integer | FK | 5001 |
| QUANTITY | Sold quantity | Integer | | 11 |
| UNIT_COST_SNAPSHOT | Cost per unit at order time | Decimal(10,2) | | 800.00 |
| UNIT_PRICE | Selling price per unit | Decimal(10,2) | | 1200.00 |
| SUBTOTAL | Line subtotal amount | Decimal(10,2) | | 13200.00 |

### Table: ISSUE_CHECKS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| CHECK_ID | Issue check ID | Integer | PK | 1 |
| SO_ID | Sales order ID | Integer | FK | 20267 |
| CHECKED_BY_USER_ID | Checker user ID | Integer | FK | 1003 |
| CHECKED_DATETIME | Check date and time | Datetime | | 2026-09-23 10:00:00 |
| RESULT | Inspection result | Varchar(10) | | ISSUED |
| REJECT_REASON | Reason for rejection | Varchar(100) | | 1 bed damaged |

### Table: ISSUE_CHECK_ITEMS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| CHECK_ITEM_ID | Check item line ID | Integer | PK | 1 |
| CHECK_ID | Issue check ID | Integer | FK | 1 |
| SO_ITEM_ID | SO item line ID | Integer | FK | 1 |
| EXPECTED_QTY | Expected quantity | Integer | | 11 |
| COUNTED_QTY | Actually counted quantity | Integer | | 11 |
| IS_CORRECT_PRODUCT | Correct product check flag | Boolean | | 1 |

### Table: STOCK_MOVEMENTS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| MOVEMENT_ID | Stock movement ID | Integer | PK | 1 |
| REQUEST_ID | Approved request ID | Integer | FK | 520 |
| PRODUCT_ID | Product ID | Integer | FK | 5001 |
| FROM_ZONE_ID | Source zone ID | Integer | FK | 1 |
| TO_ZONE_ID | Destination zone ID | Integer | FK | 2 |
| QUANTITY | Move quantity | Integer | | 89 |
| STATUS | Movement status | Varchar(20) | | COMPLETED |
| COUNTED_QTY | Actually counted quantity | Integer | | 89 |
| VERIFIED_BY_USER_ID | Verifier user ID | Integer | FK | 1003 |
| VERIFIED_DATETIME | Verification date and time | Datetime | | 2026-09-25 09:00:00 |
| REJECT_REASON | Reason for rejection | Varchar(100) | | Only 80 found |

### Table: AUDIT_LOGS
| Attribute | Description | Data type | Key | Example |
|:---|:---|:---|:---:|:---|
| LOG_ID | Audit log ID | Integer | PK | 1 |
| USER_ID | User ID | Integer | FK | 1001 |
| ACTION_TYPE | Action type | Varchar(20) | | GOODS_RECEIVED |
| DETAIL | Action details | Varchar(100) | | Accepted PO 20267, all 11 beds counted |
| ACTION_DATETIME | Action date and time | Datetime | | 2026-09-24 10:30:00 |

**Database rules to state under the tables**
- UNIQUE: `RECEIVING_CHECKS.PO_ID`, `ISSUE_CHECKS.SO_ID`, `STOCK_MOVEMENTS.REQUEST_ID`, `ZONES.ZONE_CODE`, `ZONES.ZONE_NAME`.
- `PRODUCTS.CURRENT_STOCK ≥ 0` (CHECK). `PRODUCTS.ZONE_ID` is required (NOT NULL).
- `PO_MIN_QTY`, `SO_MIN_QTY` ≥ 1; a max is NULL or ≥ its min.
- After a product is created, `CURRENT_STOCK` changes only when a receiving check is ACCEPTED or an issue check is ISSUED.
- `APPROVAL_REQUESTS`, `AUDIT_LOGS` and all order and check records can never be updated after the final status or deleted.
- Products, suppliers and customers use `IS_ACTIVE`; no master data is physically deleted.
- Only one PENDING approval request may exist for the same target at a time.

---

# Diagram ที่ต้องวาดใหม่ (ยังไม่ได้ทำ — รอออกแบบเสร็จ)

| Diagram | สิ่งที่เปลี่ยน |
|:---|:---|
| **ER (Appendix A)** | จากเดิม 11 ตารางเป็น 16 ตาราง: เพิ่ม ZONES, APPROVAL_REQUESTS, RECEIVING_CHECKS (+ITEMS), ISSUE_CHECKS (+ITEMS), STOCK_MOVEMENTS, AUDIT_LOGS; ตัด PURCHASE_RECEIPTS และ SALES_RECEIPTS ออก; แก้ USERS (เพิ่ม ROLE), PRODUCTS (zone = location, min/max, IS_ACTIVE), ORDER (เอาฟิลด์การยกเลิกออก) ดูรายการเส้นใน Relationship summary |
| **Activity / Swimlane (1.5)** | แบ่งเลนใหม่: Owner, Manager, Admin, Staff วาดสายซื้อ (Admin สร้าง PO→Manager อนุมัติ→Staff ตรวจรับ Accept/Reject), สายขาย (Admin สร้าง SO→Manager อนุมัติ→Staff ตรวจส่งออก Issue/Reject), สายจัดการโซน/ย้ายสินค้า |
| **Sitemap (4.1)** | แยกเมนูตาม 4 role ตามตารางในหัวข้อ 4.1 และใช้รหัส UI-01 ถึง UI-22 |
| **Wireframe / Mockup** | ต้องวาด UI-01, 03, 04, 06, 09, 11, 13–22 ใหม่; ปรับ mockup เดิม UI-02, 05, 07, 08, 10, 12 |

# ข้อสมมติที่ฉันตั้งไว้เอง (ตรวจและแก้ได้)

1. **Location = รหัสโซน** (เช่น Z1) สินค้า 1 ชนิดมี 1 โซนเท่านั้น ระบุตอนเพิ่มสินค้า ไม่มีชั้นหรือช่องย่อย
2. **การเปลี่ยน Role:** มีเฉพาะ Admin ขอเปลี่ยน Role ระหว่าง Staff และ Admin (รอ Manager อนุมัติ)
3. **Admin จัดการโซน (FR-11)** รวมทั้งการขอเพิ่ม/แก้โซน และการขอย้ายสินค้าไปโซนอื่น (แล้วให้ Staff ตรวจยืนยันใน FR-12)
4. **Stock เริ่มต้น** กำหนดตอนเพิ่มสินค้า หลังจากนั้นเปลี่ยนได้ทางการรับและส่งออกเท่านั้น
5. **สินค้าที่มี stock หรืออยู่ใน order ที่ยังไม่จบ ลบไม่ได้** (คำขอจะ Failed)
6. **Staff เห็น PO/SO ได้ทั้งใบ** (รวมราคา) แต่แก้ไม่ได้
7. **การจ่ายเงินให้ Supplier และการรับเงินจาก Customer จัดการนอกระบบ** เมื่อ Manager อนุมัติ PO หรือ SO แล้ว รายการจะส่งไปให้ Staff ตรวจรับหรือตรวจจ่ายสินค้าทันที
8. **Manager ตั้ง min stock alert** ในหน้า Order Limits โดยไม่ต้องผ่าน approval
