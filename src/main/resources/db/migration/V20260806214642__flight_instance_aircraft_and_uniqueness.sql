ALTER TABLE flight_instances
    MODIFY COLUMN aircraft_id BIGINT NULL;

ALTER TABLE flight_instances
    ADD COLUMN active_flag BOOLEAN
        GENERATED ALWAYS AS (IF(deleted_at IS NULL, TRUE, NULL)) STORED;

ALTER TABLE flight_instances
    ADD CONSTRAINT uk_instance_schedule_date_active
        UNIQUE (flight_schedule_id, departure_date, active_flag);