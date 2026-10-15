-- Module OrderManagement — la ligne fige le service commandé : spécification du catalogue,
-- version et caractéristiques copiées à la soumission.

-- 1. La ligne référence une spécification du catalogue et en fige la version.
--    Ni CHECK sur la liste des offres ni clé étrangère vers catalog : le catalogue
--    est seul propriétaire de l'offre, et un module ne lie pas son schéma à un autre.
ALTER TABLE orders.order_lines DROP CONSTRAINT ck_order_lines_service_type;

ALTER TABLE orders.order_lines RENAME COLUMN service_type TO service_specification_id;

ALTER TABLE orders.order_lines ALTER COLUMN service_specification_id TYPE varchar(32);

ALTER TABLE orders.order_lines
    RENAME CONSTRAINT uq_order_lines_drone_service TO uq_order_lines_drone_specification;

ALTER TABLE orders.order_lines ADD COLUMN service_specification_version integer NOT NULL DEFAULT 1;

ALTER TABLE orders.order_lines ALTER COLUMN service_specification_version DROP DEFAULT;

ALTER TABLE orders.order_lines ADD CONSTRAINT ck_order_lines_specification_version
    CHECK (service_specification_version > 0);

-- 2. Rang de la ligne dans sa commande : la réponse restitue l'ordre de la requête.
ALTER TABLE orders.order_lines ADD COLUMN position smallint;

UPDATE orders.order_lines AS line
SET position = ranked.rank - 1
FROM (
    SELECT order_line_id,
           row_number() OVER (PARTITION BY order_id ORDER BY order_line_id) AS rank
    FROM orders.order_lines
) AS ranked
WHERE line.order_line_id = ranked.order_line_id;

ALTER TABLE orders.order_lines ALTER COLUMN position SET NOT NULL;

ALTER TABLE orders.order_lines
    ADD CONSTRAINT uq_order_lines_position UNIQUE (order_id, position);

-- 3. Les caractéristiques commandées, copiées du catalogue à la soumission et jamais modifiées :
--    une évolution de l'offre ne réécrit pas ce qui a été vendu.
CREATE TABLE orders.order_line_characteristics (
    order_line_id uuid        NOT NULL,
    position      smallint    NOT NULL,
    name          varchar(32) NOT NULL,
    value         varchar(64) NOT NULL,

    CONSTRAINT pk_order_line_characteristics PRIMARY KEY (order_line_id, name),
    -- Reste à l'intérieur de l'agrégat Order, comme fk_order_lines_order.
    CONSTRAINT fk_order_line_characteristics_line FOREIGN KEY (order_line_id)
        REFERENCES orders.order_lines (order_line_id) ON DELETE CASCADE
);
