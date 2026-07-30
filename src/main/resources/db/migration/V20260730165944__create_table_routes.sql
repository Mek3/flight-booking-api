CREATE TABLE routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    origin_airport_id BIGINT NOT NULL,
    destination_airport_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    deleted_at DATETIME(6) DEFAULT NULL,
    deleted_by VARCHAR(255) DEFAULT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    updated_by VARCHAR(255) DEFAULT NULL,
    CONSTRAINT fk_routes_origin_airport FOREIGN KEY (origin_airport_id) REFERENCES airports(id),
    CONSTRAINT fk_routes_destination_airport FOREIGN KEY (destination_airport_id) REFERENCES airports(id),
    CONSTRAINT chk_routes_different_airports CHECK (origin_airport_id != destination_airport_id),
    CONSTRAINT uk_routes_origin_destination UNIQUE (origin_airport_id, destination_airport_id)
);