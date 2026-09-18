ALTER TABLE reservations
    DROP FOREIGN KEY `FKix9mwp337byu4ve2jqtjurjy6`;

ALTER TABLE reservations
    DROP COLUMN flight_id;

DROP TABLE flights;