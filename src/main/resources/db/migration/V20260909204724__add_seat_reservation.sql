CREATE TABLE `seat_reservations` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `created_at` datetime(6) NOT NULL,
                                     `created_by` varchar(255) NOT NULL,
                                     `deleted_at` datetime(6) DEFAULT NULL,
                                     `deleted_by` varchar(255) DEFAULT NULL,
                                     `updated_at` datetime(6) DEFAULT NULL,
                                     `updated_by` varchar(255) DEFAULT NULL,
                                     `seat_id` bigint NOT NULL,
                                     `flight_segment_id` bigint NOT NULL,
                                     `status` varchar(20) NOT NULL,
                                     `held_until` datetime(6) NOT NULL,
                                     `occupied_flag` boolean GENERATED ALWAYS AS (
                                         IF(`deleted_at` IS NULL AND `status` IN ('HELD', 'CONFIRMED'), TRUE, NULL)
                                         ) STORED,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_seat_reservation_seat_occupied` (`seat_id`,`occupied_flag`),
                                     KEY `idx_seat_reservations_segment` (`flight_segment_id`),
                                     KEY `idx_seat_reservations_expiry` (`status`,`held_until`),
                                     CONSTRAINT `fk_seat_reservations_seat` FOREIGN KEY (`seat_id`) REFERENCES `seats` (`id`),
                                     CONSTRAINT `fk_seat_reservations_segment` FOREIGN KEY (`flight_segment_id`) REFERENCES `flight_segments` (`id`)
);