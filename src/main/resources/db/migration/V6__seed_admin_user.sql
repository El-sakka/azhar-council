-- Default account required by the specification: username "Admin", password "Admin 123" (BCrypt hash).
-- Change the password after the first deployment.
INSERT INTO app_user (username, password_hash, display_name)
VALUES ('Admin', '$2a$10$trk0Hg6n4Lyj6IQ7kjbCzO82DLZeHtB.Su0GW66X87IrHbaysz7Sy', 'مدير القاعدة');
