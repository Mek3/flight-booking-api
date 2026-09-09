ALTER TABLE reservations
    DROP FOREIGN KEY fk_reservations_flight_instance;

ALTER TABLE reservations
    DROP COLUMN flight_instance_id;