create table airports (
    id bigint primary key AUTO_INCREMENT,
    name varchar(255) not null,
    code varchar(10) not null unique,
    city varchar(255) not null,
    country varchar(255) not null,
    `created_at` datetime(6) NOT NULL,
    `created_by` varchar(255) NOT NULL,
    `deleted_at` datetime(6) DEFAULT NULL,
    `deleted_by` varchar(255) DEFAULT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    `updated_by` varchar(255) DEFAULT NULL
);
