ALTER TABLE aircraft_layouts
    ADD COLUMN total_rows INT NOT NULL,
    ADD COLUMN seat_letters VARCHAR(10) NOT NULL;