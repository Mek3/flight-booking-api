CREATE TABLE `itineraries` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `created_at` datetime(6) NOT NULL,
                               `created_by` varchar(255) NOT NULL,
                               `deleted_at` datetime(6) DEFAULT NULL,
                               `deleted_by` varchar(255) DEFAULT NULL,
                               `updated_at` datetime(6) DEFAULT NULL,
                               `updated_by` varchar(255) DEFAULT NULL,
                               `reservation_id` bigint NOT NULL,
                               `sequence_order` int NOT NULL,
                               `active_flag` boolean GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, TRUE, NULL)) STORED,
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_itinerary_reservation_sequence_active` (`reservation_id`,`sequence_order`,`active_flag`),
                               CONSTRAINT `fk_itineraries_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`)
) ;

CREATE TABLE `flight_segments` (
                                   `id` bigint NOT NULL AUTO_INCREMENT,
                                   `created_at` datetime(6) NOT NULL,
                                   `created_by` varchar(255) NOT NULL,
                                   `deleted_at` datetime(6) DEFAULT NULL,
                                   `deleted_by` varchar(255) DEFAULT NULL,
                                   `updated_at` datetime(6) DEFAULT NULL,
                                   `updated_by` varchar(255) DEFAULT NULL,
                                   `itinerary_id` bigint NOT NULL,
                                   `flight_instance_id` bigint NOT NULL,
                                   `segment_order` int NOT NULL,
                                   `active_flag` boolean GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, TRUE, NULL)) STORED,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_segment_itinerary_order_active` (`itinerary_id`,`segment_order`,`active_flag`),
                                   KEY `idx_flight_segments_flight_instance` (`flight_instance_id`),
                                   CONSTRAINT `fk_flight_segments_itinerary` FOREIGN KEY (`itinerary_id`) REFERENCES `itineraries` (`id`),
                                   CONSTRAINT `fk_flight_segments_flight_instance` FOREIGN KEY (`flight_instance_id`) REFERENCES `flight_instances` (`id`)
) ;