-- P95 du délai entre acceptedAt et activatedAt (§10.4), sur slice_db.
-- Un service non actif à la fin du test compte comme dépassant 5 s.
-- :debut est l'instant de lancement de k6.
WITH services AS (
    SELECT s.service_id,
           s.state,
           min(a.accepted_at) AS accepted_at,
           max(a.activated_at) FILTER (WHERE a.result = 'SUCCESS') AS activated_at
    FROM activation.services s
    JOIN activation.activation_attempts a USING (service_id)
    GROUP BY s.service_id, s.state
    HAVING min(a.accepted_at) >= :'debut'::timestamptz
),
delais AS (
    SELECT CASE WHEN state = 'ACTIVE' THEN extract(epoch FROM activated_at - accepted_at)
                ELSE 'infinity'::float8 END AS delai_s,
           state
    FROM services
)
SELECT count(*)                                                        AS services,
       count(*) FILTER (WHERE state = 'ACTIVE')                        AS actifs,
       count(*) FILTER (WHERE state = 'FAILED')                        AS echecs,
       count(*) FILTER (WHERE state IN ('PENDING', 'UNCONFIRMED'))     AS non_termines,
       round(percentile_cont(0.50) WITHIN GROUP (ORDER BY delai_s)::numeric, 3) AS p50_s,
       round(percentile_cont(0.95) WITHIN GROUP (ORDER BY delai_s)::numeric, 3) AS p95_s,
       round((count(*) FILTER (WHERE delai_s <= 5) * 100.0 / nullif(count(*), 0))::numeric, 2) AS pct_sous_5s
FROM delais;
