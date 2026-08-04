CREATE TABLE aircraft_layouts (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 aircraft_model_id BIGINT NOT NULL,
                                 cabin_class VARCHAR(50) NOT NULL,
                                 seat_capacity INT NOT NULL,
                                 created_at DATETIME(6) NOT NULL,
                                 created_by VARCHAR(255) NOT NULL,
                                 deleted_at DATETIME(6) DEFAULT NULL,
                                 deleted_by VARCHAR(255) DEFAULT NULL,
                                 updated_at DATETIME(6) DEFAULT NULL,
                                 updated_by VARCHAR(255) DEFAULT NULL,
                                 CONSTRAINT fk_layout_aircraft_model FOREIGN KEY (aircraft_model_id) REFERENCES aircraft_models(id)
);

CREATE TABLE flight_schedules (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 flight_number VARCHAR(10) NOT NULL UNIQUE,
                                 departure_airport_id BIGINT NOT NULL,
                                 arrival_airport_id BIGINT NOT NULL,
                                 departure_time TIME NOT NULL,
                                 arrival_time TIME NOT NULL,
                                 days_of_week_mask INT NOT NULL COMMENT 'Bitmask representing operating days (e.g., 1=Mon, 2=Tue, 4=Wed)',
                                 created_at DATETIME(6) NOT NULL,
                                 created_by VARCHAR(255) NOT NULL,
                                 deleted_at DATETIME(6) DEFAULT NULL,
                                 deleted_by VARCHAR(255) DEFAULT NULL,
                                 updated_at DATETIME(6) DEFAULT NULL,
                                 updated_by VARCHAR(255) DEFAULT NULL,
                                 CONSTRAINT fk_schedule_departure_airport FOREIGN KEY (departure_airport_id) REFERENCES airports(id),
                                 CONSTRAINT fk_schedule_arrival_airport FOREIGN KEY (arrival_airport_id) REFERENCES airports(id)
);