ALTER TABLE flight_schedules
    ADD COLUMN arrival_day_offset INT NOT NULL DEFAULT 0,
    ADD COLUMN base_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE reservations
    ADD COLUMN flight_instance_id BIGINT NULL AFTER user_id;

ALTER TABLE reservations
    ADD CONSTRAINT fk_reservations_flight_instance
        FOREIGN KEY (flight_instance_id) REFERENCES flight_instances (id);

CREATE INDEX idx_reservations_flight_instance
    ON reservations (flight_instance_id);

ALTER TABLE reservations
    MODIFY COLUMN flight_id BIGINT NULL;