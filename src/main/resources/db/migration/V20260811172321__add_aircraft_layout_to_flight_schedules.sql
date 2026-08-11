ALTER TABLE flight_schedules
    ADD COLUMN aircraft_layout_id BIGINT NOT NULL;

ALTER TABLE flight_schedules
    ADD CONSTRAINT fk_flight_schedules_aircraft_layout
        FOREIGN KEY (aircraft_layout_id) REFERENCES aircraft_layouts(id);