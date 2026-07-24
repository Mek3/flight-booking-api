create table aircraft_models (
    id bigint auto_increment primary key,
    manufacturer varchar(50) NOT NULL,
    model_name varchar (50) NOT NULL,
    max_capacity smallint NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(255) NOT NULL,
    `deleted_at` datetime(6) DEFAULT NULL,
    `deleted_by` varchar(255) DEFAULT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    `updated_by` varchar(255) DEFAULT NULL,
    CONSTRAINT uk_manufacturer_model UNIQUE (manufacturer, model_name)
)