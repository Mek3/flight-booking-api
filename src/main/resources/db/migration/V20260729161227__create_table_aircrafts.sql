create table aircrafts(
    id bigint  primary key AUTO_INCREMENT,
    registration_number varchar(50) unique not null,
    id_aircraft_model bigint,
    total_flight_hours int not null,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(255) NOT NULL,
    `deleted_at` datetime(6) DEFAULT NULL,
    `deleted_by` varchar(255) DEFAULT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    `updated_by` varchar(255) DEFAULT NULL,
    
    constraint fk_aircraft_model foreign  key(id_aircraft_model) references aircraft_models(id)
)