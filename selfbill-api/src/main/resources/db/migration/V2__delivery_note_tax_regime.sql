-- Regime under which each delivery note's rates were frozen (CONCEPTO.md §3).
-- No real data exists yet: existing development rows take their client's current regime.
alter table delivery_notes add column vat_type varchar(255) not null default 'WITH_VAT';
alter table delivery_notes add column surcharge_type varchar(255) not null default 'WITHOUT_SURCHARGE';
update delivery_notes set
    vat_type = (select c.vat_type from clients c where c.id = delivery_notes.client_id),
    surcharge_type = (select c.surcharge_type from clients c where c.id = delivery_notes.client_id);

-- Foreign key columns: SQLite does not index them on its own
create index idx_delivery_notes_client on delivery_notes (client_id);
create index idx_delivery_notes_invoice on delivery_notes (invoice_id);
create index idx_invoices_client on invoices (client_id);
create index idx_invoice_lines_invoice on invoice_lines (invoice_id);
create index idx_delivery_notes_articles_delivery_note on delivery_notes_articles (delivery_note_id);
