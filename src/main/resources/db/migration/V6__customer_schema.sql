-- Module CustomerManagement — remplace AccessManagement : le schéma suit le module.
-- V1, V4 et V5 restent inchangées ; Flyway refuse toute migration déjà appliquée et modifiée.

ALTER SCHEMA access RENAME TO customer;
