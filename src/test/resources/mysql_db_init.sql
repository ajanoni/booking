DROP TABLE IF EXISTS `customers`;
CREATE TABLE `customers` (
  `id` varchar(36) NOT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `reservations`;
CREATE TABLE `reservations` (
  `id` varchar(36) NOT NULL,
  `customer_id` varchar(36) DEFAULT NULL,
  `arrival_date` datetime DEFAULT NULL,
  `departure_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_CUSTOMER_idx` (`customer_id`),
  KEY `IDX_ARRIVAL` (`arrival_date`),
  KEY `IDX_DEPARTURE` (`departure_date`),
  CONSTRAINT `FK_CUSTOMER` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
