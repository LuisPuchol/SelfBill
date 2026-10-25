-- An issued invoice keeps its own copy of the issuer and of the client code (CONCEPTO.md §6)
alter table invoices add column issuer_fiscal_name varchar(255);
alter table invoices add column issuer_nif varchar(20);
alter table invoices add column issuer_address varchar(500);
alter table invoices add column client_code integer;

-- Invoices already issued (deleted ones too) get the copies from today's profile and client
update invoices set
    issuer_fiscal_name = (select fiscal_name from user_profile limit 1),
    issuer_nif = (select nif from user_profile limit 1),
    issuer_address = (select address from user_profile limit 1),
    client_code = (select code from clients where clients.id = invoices.client_id)
where status = 'ISSUED';
