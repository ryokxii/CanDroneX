-- Module DroneManagement — UC-02 : l'identité réseau comprend l'ICCID du support (SIM ou eSIM).

-- Le nom de la colonne suit celui du domaine.
ALTER TABLE drone.drones RENAME COLUMN created_at TO registered_at;

-- NOT NULL sans valeur par défaut : la base livrée ne contient aucun drone,
-- V4 n'amorçant que le client. Une base Phase 1 qui en contient se réinitialise.
ALTER TABLE drone.drones ADD COLUMN iccid varchar(20) NOT NULL;

ALTER TABLE drone.drones
    -- Un support ne sert qu'un seul drone, comme l'IMSI.
    ADD CONSTRAINT uq_drones_iccid UNIQUE (iccid),
    -- UIT-T E.118 : préfixe 89, 19 ou 20 chiffres. Le chiffre de contrôle est vérifié par le domaine.
    ADD CONSTRAINT ck_drones_iccid CHECK (iccid ~ '^89[0-9]{17,18}$');
