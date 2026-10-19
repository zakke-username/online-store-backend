DROP TABLE IF EXISTS OrderItems;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Products;
DROP TABLE IF EXISTS ProductCategories;
DROP TABLE IF EXISTS SupplierAddresses;
DROP TABLE IF EXISTS CustomerAddresses;
DROP TABLE IF EXISTS Suppliers;
DROP TABLE IF EXISTS Customers;

CREATE TABLE Customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(30),
    version INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE Suppliers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(100),
    phone VARCHAR(30),
    email VARCHAR(255),
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE CustomerAddresses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    street_address VARCHAR(255) NOT NULL,
    postal_code VARCHAR(20),
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100),
    version INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_customeraddress_customer
        FOREIGN KEY (customer_id) REFERENCES Customers(id)
);

CREATE TABLE SupplierAddresses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id INT NOT NULL,
    street_address VARCHAR(255) NOT NULL,
    postal_code VARCHAR(20),
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100),
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_supplieraddress_supplier
        FOREIGN KEY (supplier_id) REFERENCES Suppliers(id)
);

CREATE TABLE ProductCategories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE Products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    category_id INT,
    supplier_id INT,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_product_price CHECK (price >= 0),
    CONSTRAINT chk_product_stock CHECK (stock_quantity >= 0),
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES ProductCategories(id),
    CONSTRAINT fk_product_supplier
        FOREIGN KEY (supplier_id) REFERENCES Suppliers(id)
);

CREATE TABLE Orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    order_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivery_date DATETIME,
    shipping_address_id INT,
    status VARCHAR(50) COLLATE utf8mb4_bin NOT NULL DEFAULT 'NEW',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_orders_customer_date (customer_id, order_date),
    INDEX idx_orders_order_date (order_date),
    INDEX idx_orders_status_date (status, order_date),
    CONSTRAINT chk_order_status
        CHECK (status IN ('NEW', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT fk_order_customer
        FOREIGN KEY (customer_id) REFERENCES Customers(id),
    CONSTRAINT fk_order_shipping_address
        FOREIGN KEY (shipping_address_id) REFERENCES CustomerAddresses(id)
);

CREATE TABLE OrderItems (
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id, product_id),
    CONSTRAINT chk_orderitem_quantity CHECK (quantity > 0),
    CONSTRAINT chk_orderitem_unit_price CHECK (unit_price >= 0),
    CONSTRAINT fk_orderitem_order
        FOREIGN KEY (order_id) REFERENCES Orders(id),
    CONSTRAINT fk_orderitem_product
        FOREIGN KEY (product_id) REFERENCES Products(id)
);

CREATE OR REPLACE VIEW OrderTotals AS
SELECT o.id AS order_id,
       o.customer_id,
       o.order_date,
       o.status,
       COALESCE(SUM(i.quantity), 0) AS item_count,
       COALESCE(SUM(i.quantity * i.unit_price), 0) AS total
FROM Orders o
LEFT JOIN OrderItems i ON i.order_id = o.id
GROUP BY o.id, o.customer_id, o.order_date, o.status;

CREATE OR REPLACE VIEW ProductCatalog AS
SELECT p.id,
       p.name,
       p.description,
       p.price,
       p.stock_quantity,
       p.category_id,
       c.name AS category_name
FROM Products p
LEFT JOIN ProductCategories c ON c.id = p.category_id;
