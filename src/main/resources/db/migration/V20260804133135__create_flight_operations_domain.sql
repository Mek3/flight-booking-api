CREATE TABLE flight_instances (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 flight_schedule_id BIGINT NOT NULL,
                                 aircraft_id BIGINT NOT NULL,
                                 departure_date DATE NOT NULL,
                                 status VARCHAR(30) NOT NULL,
                                 created_at DATETIME(6) NOT NULL,
                                 created_by VARCHAR(255) NOT NULL,
                                 deleted_at DATETIME(6) DEFAULT NULL,
                                 deleted_by VARCHAR(255) DEFAULT NULL,
                                 updated_at DATETIME(6) DEFAULT NULL,
                                 updated_by VARCHAR(255) DEFAULT NULL,
                                 CONSTRAINT fk_instance_schedule FOREIGN KEY (flight_schedule_id) REFERENCES flight_schedules(id),
                                 CONSTRAINT fk_instance_aircraft FOREIGN KEY (aircraft_id) REFERENCES aircrafts(id)
);

CREATE TABLE flight_status_histories (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       flight_instance_id BIGINT NOT NULL,
                                       status VARCHAR(30) NOT NULL,
                                       remarks VARCHAR(255),
                                       created_at DATETIME(6) NOT NULL,
                                       created_by VARCHAR(255) NOT NULL,
                                       deleted_at DATETIME(6) DEFAULT NULL,
                                       deleted_by VARCHAR(255) DEFAULT NULL,
                                       updated_at DATETIME(6) DEFAULT NULL,
                                       updated_by VARCHAR(255) DEFAULT NULL,
                                       CONSTRAINT fk_history_instance FOREIGN KEY (flight_instance_id) REFERENCES flight_instances(id)
);

CREATE TABLE seats (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      flight_instance_id BIGINT NOT NULL,
                      seat_row INT NOT NULL,
                      seat_letter VARCHAR(5) NOT NULL,
                      is_available BOOLEAN DEFAULT TRUE,
                      created_at DATETIME(6) NOT NULL,
                      created_by VARCHAR(255) NOT NULL,
                      deleted_at DATETIME(6) DEFAULT NULL,
                      deleted_by VARCHAR(255) DEFAULT NULL,
                      updated_at DATETIME(6) DEFAULT NULL,
                      updated_by VARCHAR(255) DEFAULT NULL,
                      CONSTRAINT fk_seat_instance FOREIGN KEY (flight_instance_id) REFERENCES flight_instances(id),
                      CONSTRAINT uk_flight_seat UNIQUE (flight_instance_id, seat_row, seat_letter)
);