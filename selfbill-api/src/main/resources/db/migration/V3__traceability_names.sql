-- English name for the traceability codes. V1 keeps the old one: an applied migration does not change.
-- Renaming a column rewrites the indexes on it, but not their names.
alter table delivery_notes_articles rename column trazability_code1 to traceability_code1;
alter table delivery_notes_articles rename column trazability_code2 to traceability_code2;
alter table delivery_notes_articles rename column trazability_code3 to traceability_code3;
alter table invoice_lines rename column trazability_code1 to traceability_code1;
alter table invoice_lines rename column trazability_code2 to traceability_code2;
alter table invoice_lines rename column trazability_code3 to traceability_code3;

drop index idx_trazability_code1;
drop index idx_trazability_code2;
drop index idx_trazability_code3;
create index idx_traceability_code1 on delivery_notes_articles (traceability_code1);
create index idx_traceability_code2 on delivery_notes_articles (traceability_code2);
create index idx_traceability_code3 on delivery_notes_articles (traceability_code3);
