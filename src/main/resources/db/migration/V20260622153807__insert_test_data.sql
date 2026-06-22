-- 1. Insert Roles
INSERT INTO `roles` VALUES 
(1, '2026-06-22 11:44:42.139208', 'system', NULL, NULL, '2026-06-22 11:44:42.139208', 'system', NULL, 'ROLE_ADMIN'),
(2, '2026-06-22 11:44:42.173905', 'system', NULL, NULL, '2026-06-22 11:44:42.173905', 'system', NULL, 'ROLE_USER');

-- 2. Insert Users
INSERT INTO `users` VALUES 
(1, '2026-06-22 11:44:42.268416', 'system', NULL, NULL, '2026-06-22 11:44:42.268416', 'system', 'paco@example.com', 1, 'Paco', '$2a$10$P3BcyzJ8pJ2zZz9LAa8a4.BzylRGv2xsTDG64bFRQiuYP9MvGTRNS', '555-1234', 'Garcia', 'pacog');

-- 3. Assign Roles to Users (role_id = 2 [ROLE_USER], user_id = 1 [Paco])
INSERT INTO `users_roles` VALUES 
(1, '2026-06-22 11:44:42.337902', 'system', NULL, NULL, '2026-06-22 11:44:42.337902', 'system', 2, 1);

-- 4. Insert Flights
INSERT INTO `flights` VALUES 
(1, '2026-06-22 11:44:42.351450', 'system', NULL, NULL, '2026-06-22 11:44:42.351450', 'system', 150, 'Madrid', '2026-06-23 11:44:42.344912', 'Tokyo', '2026-06-24 01:44:42.344912', 'MAD-TOK-001', 299.99, 0);