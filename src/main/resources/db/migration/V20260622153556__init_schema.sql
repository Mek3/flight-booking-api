-- 1. Independent Tables (No Dependencies)
CREATE TABLE `flights` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(255) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `available_seats` int NOT NULL,
  `departure` varchar(255) NOT NULL,
  `departure_time` datetime(6) NOT NULL,
  `destination` varchar(255) NOT NULL,
  `destination_time` datetime(6) NOT NULL,
  `flight_number` varchar(255) NOT NULL,
  `price` decimal(38,2) NOT NULL,
  `version` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6bx3i9v6ikjiy0ru5ybor8t7` (`flight_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(255) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(255) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `surname` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Dependent Tables (With Foreign Keys)
CREATE TABLE `users_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(255) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `role_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKq3r1u8cne2rw2hkr899xuh7vj` (`user_id`,`role_id`),
  KEY `FKj6m8fwv7oqv74fcehir1a9ffy` (`role_id`),
  CONSTRAINT `FK2o0jvgh89lemvvo17cbqvdxaa` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKj6m8fwv7oqv74fcehir1a9ffy` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reservations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(255) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `number_of_passengers` int NOT NULL,
  `reservation_code` varchar(255) NOT NULL,
  `status` enum('CANCELLED','COMPLETED','CONFIRMED','EXPIRED','PENDING') DEFAULT NULL,
  `total_price` decimal(10,2) NOT NULL,
  `flight_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6g1uj544xgjuyhj6kjh6pka6l` (`reservation_code`),
  KEY `FKix9mwp337byu4ve2jqtjurjy6` (`flight_id`),
  KEY `FKb5g9io5h54iwl2inkno50ppln` (`user_id`),
  CONSTRAINT `FKb5g9io5h54iwl2inkno50ppln` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKix9mwp337byu4ve2jqtjurjy6` FOREIGN KEY (`flight_id`) REFERENCES `flights` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;