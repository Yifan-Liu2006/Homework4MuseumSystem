CREATE DATABASE IF NOT EXISTS museum_ticket DEFAULT CHARACTER SET utf8mb4;
USE museum_ticket;

CREATE TABLE visitor (
  visitorID CHAR(20) NOT NULL,
  mobile VARCHAR(20) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status ENUM('正常','禁用') NOT NULL DEFAULT '正常',
  register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_time DATETIME NULL,
  PRIMARY KEY (visitorID), UNIQUE KEY uk_visitor_mobile (mobile)
) ENGINE=InnoDB;

CREATE TABLE real_person (
  personID CHAR(20) NOT NULL,
  visitorID CHAR(20) NOT NULL,
  name VARCHAR(50) NOT NULL,
  id_type ENUM('身份证','港澳台通行证','护照') NOT NULL,
  id_hash CHAR(64) NOT NULL,
  is_self TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (personID), UNIQUE KEY uk_person_identity (id_type, id_hash),
  CONSTRAINT fk_person_visitor FOREIGN KEY (visitorID) REFERENCES visitor(visitorID)
) ENGINE=InnoDB;

CREATE TABLE open_day (
  openDayID CHAR(20) NOT NULL,
  visit_date DATE NOT NULL,
  is_closed TINYINT(1) NOT NULL DEFAULT 0,
  is_holiday TINYINT(1) NOT NULL DEFAULT 0,
  release_time DATETIME NOT NULL,
  status ENUM('未开票','已开票','已关闭') NOT NULL DEFAULT '未开票',
  PRIMARY KEY (openDayID), UNIQUE KEY uk_open_day_date (visit_date)
) ENGINE=InnoDB;

CREATE TABLE visit_slot (
  slotID CHAR(20) NOT NULL,
  openDayID CHAR(20) NOT NULL,
  slot_code VARCHAR(20) NOT NULL,
  checkin_start TIME NOT NULL,
  checkin_end TIME NOT NULL,
  status ENUM('启用','停用') NOT NULL DEFAULT '启用',
  PRIMARY KEY (slotID), UNIQUE KEY uk_slot_code (openDayID, slot_code),
  CONSTRAINT fk_slot_day FOREIGN KEY (openDayID) REFERENCES open_day(openDayID)
) ENGINE=InnoDB;

CREATE TABLE ticket_type (
  ticketTypeID CHAR(20) NOT NULL,
  name VARCHAR(50) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  description VARCHAR(255),
  status ENUM('上架','下架') NOT NULL DEFAULT '上架',
  PRIMARY KEY (ticketTypeID), UNIQUE KEY uk_ticket_type_name (name),
  CONSTRAINT ck_ticket_price CHECK (price >= 0)
) ENGINE=InnoDB;

CREATE TABLE ticket_stock (
  stockID CHAR(20) NOT NULL,
  slotID CHAR(20) NOT NULL,
  ticketTypeID CHAR(20) NOT NULL,
  total_quantity INT UNSIGNED NOT NULL,
  sold_quantity INT UNSIGNED NOT NULL DEFAULT 0,
  locked_quantity INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (stockID), UNIQUE KEY uk_stock_slot_type (slotID, ticketTypeID),
  CONSTRAINT fk_stock_slot FOREIGN KEY (slotID) REFERENCES visit_slot(slotID),
  CONSTRAINT fk_stock_type FOREIGN KEY (ticketTypeID) REFERENCES ticket_type(ticketTypeID),
  CONSTRAINT ck_stock_available CHECK (total_quantity >= sold_quantity + locked_quantity)
) ENGINE=InnoDB;

CREATE TABLE orders (
  ordersID CHAR(20) NOT NULL,
  visitorID CHAR(20) NOT NULL,
  visit_date DATE NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  order_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status ENUM('待支付','已支付','已取消','已退款','已过期') NOT NULL DEFAULT '待支付',
  pay_status ENUM('未支付','已支付','已退款') NOT NULL DEFAULT '未支付',
  payment_deadline DATETIME NOT NULL,
  PRIMARY KEY (ordersID),
  CONSTRAINT fk_order_visitor FOREIGN KEY (visitorID) REFERENCES visitor(visitorID),
  CONSTRAINT ck_order_price CHECK (price >= 0)
) ENGINE=InnoDB;

CREATE TABLE orders_detail (
  detailID CHAR(20) NOT NULL,
  ordersID CHAR(20) NOT NULL,
  personID CHAR(20) NOT NULL,
  stockID CHAR(20) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  verify_status ENUM('未核验','已核验','已作废') NOT NULL DEFAULT '未核验',
  PRIMARY KEY (detailID),
  CONSTRAINT fk_detail_order FOREIGN KEY (ordersID) REFERENCES orders(ordersID),
  CONSTRAINT fk_detail_person FOREIGN KEY (personID) REFERENCES real_person(personID),
  CONSTRAINT fk_detail_stock FOREIGN KEY (stockID) REFERENCES ticket_stock(stockID),
  CONSTRAINT ck_detail_price CHECK (price >= 0)
) ENGINE=InnoDB;

CREATE TABLE payment_record (
  paymentID CHAR(20) NOT NULL,
  ordersID CHAR(20) NOT NULL,
  channel ENUM('微信支付','支付宝','其他') NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  third_party_no VARCHAR(100),
  status ENUM('待支付','成功','失败','已退款') NOT NULL DEFAULT '待支付',
  paid_at DATETIME NULL,
  PRIMARY KEY (paymentID), UNIQUE KEY uk_payment_order (ordersID), UNIQUE KEY uk_payment_third_party (third_party_no),
  CONSTRAINT fk_payment_order FOREIGN KEY (ordersID) REFERENCES orders(ordersID),
  CONSTRAINT ck_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE entry_voucher (
  voucherID CHAR(20) NOT NULL,
  detailID CHAR(20) NOT NULL,
  voucher_code CHAR(64) NOT NULL,
  generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expired_at DATETIME NOT NULL,
  status ENUM('有效','已使用','已作废','已过期') NOT NULL DEFAULT '有效',
  PRIMARY KEY (voucherID), UNIQUE KEY uk_voucher_detail (detailID), UNIQUE KEY uk_voucher_code (voucher_code),
  CONSTRAINT fk_voucher_detail FOREIGN KEY (detailID) REFERENCES orders_detail(detailID)
) ENGINE=InnoDB;

CREATE TABLE verification_record (
  verificationID CHAR(20) NOT NULL,
  voucherID CHAR(20) NOT NULL,
  workerID CHAR(20),
  verified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  result ENUM('成功','失败') NOT NULL,
  remark VARCHAR(255),
  PRIMARY KEY (verificationID),
  CONSTRAINT fk_verify_voucher FOREIGN KEY (voucherID) REFERENCES entry_voucher(voucherID)
) ENGINE=InnoDB;

CREATE TABLE role (
  roleID CHAR(20) NOT NULL, role_name VARCHAR(50) NOT NULL, permission_description VARCHAR(255),
  PRIMARY KEY (roleID), UNIQUE KEY uk_role_name (role_name)
) ENGINE=InnoDB;

CREATE TABLE worker (
  workerID CHAR(20) NOT NULL, account VARCHAR(50) NOT NULL, password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(50) NOT NULL, roleID CHAR(20) NOT NULL, status ENUM('正常','禁用') NOT NULL DEFAULT '正常',
  PRIMARY KEY (workerID), UNIQUE KEY uk_worker_account (account),
  CONSTRAINT fk_worker_role FOREIGN KEY (roleID) REFERENCES role(roleID)
) ENGINE=InnoDB;

ALTER TABLE verification_record ADD CONSTRAINT fk_verify_worker FOREIGN KEY (workerID) REFERENCES worker(workerID);

CREATE TABLE operation_log (
  logID CHAR(20) NOT NULL, workerID CHAR(20), operation_type VARCHAR(50) NOT NULL,
  operation_object VARCHAR(100) NOT NULL, result VARCHAR(255), ip_address VARCHAR(45), operated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (logID), CONSTRAINT fk_log_worker FOREIGN KEY (workerID) REFERENCES worker(workerID)
) ENGINE=InnoDB;

DELIMITER $$
CREATE PROCEDURE create_ticket_order(
  IN p_ordersID CHAR(20), IN p_detailID CHAR(20), IN p_visitorID CHAR(20), IN p_personID CHAR(20),
  IN p_stockID CHAR(20), IN p_visit_date DATE, IN p_price DECIMAL(10,2), IN p_deadline DATETIME
)
BEGIN
  DECLARE v_available INT;
  DECLARE v_duplicate INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  SELECT total_quantity - sold_quantity - locked_quantity INTO v_available FROM ticket_stock WHERE stockID = p_stockID FOR UPDATE;
  IF v_available IS NULL OR v_available < 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '库存不足'; END IF;
  SELECT COUNT(*) INTO v_duplicate FROM orders_detail d JOIN orders o ON o.ordersID=d.ordersID JOIN ticket_stock s ON s.stockID=d.stockID JOIN visit_slot vs ON vs.slotID=s.slotID JOIN open_day od ON od.openDayID=vs.openDayID WHERE d.personID=p_personID AND od.visit_date=p_visit_date AND vs.slotID=(SELECT slotID FROM ticket_stock WHERE stockID=p_stockID) AND o.status NOT IN ('已取消','已退款','已过期');
  IF v_duplicate > 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '该实名人员已预约此场次'; END IF;
  INSERT INTO orders(ordersID,visitorID,visit_date,price,payment_deadline) VALUES(p_ordersID,p_visitorID,p_visit_date,p_price,p_deadline);
  INSERT INTO orders_detail(detailID,ordersID,personID,stockID,price) VALUES(p_detailID,p_ordersID,p_personID,p_stockID,p_price);
  UPDATE ticket_stock SET locked_quantity=locked_quantity+1 WHERE stockID=p_stockID;
  COMMIT;
END$$
DELIMITER ;
