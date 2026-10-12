-- Initial schema. Derived from Hibernate's DDL for the SQLite dialect, with three changes it cannot express:
-- foreign keys, partial unique indexes on codes (only live rows), and the (series, code) invoice number index.
-- Enum columns carry no CHECK: SQLite cannot alter a CHECK without rebuilding the table.

create table articles (
    id integer,
    code integer not null,
    name varchar(255) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table clients (
    id integer,
    code integer not null,
    business_name varchar(255) not null,
    name varchar(255) not null,
    nif varchar(20),
    email varchar(50),
    address varchar(500),
    city varchar(100),
    province varchar(100),
    postal_code varchar(10),
    phone varchar(20),
    phone2 varchar(20),
    vat_type varchar(255) not null,
    surcharge_type varchar(255) not null,
    default_delivery_note_format varchar(255) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table invoices (
    id integer,
    series varchar(255) not null,
    code integer,
    date timestamp,
    status varchar(255) not null,
    client_id integer not null references clients (id),
    client_business_name varchar(255),
    client_nif varchar(20),
    client_address varchar(500),
    client_city varchar(100),
    client_province varchar(100),
    client_postal_code varchar(10),
    subtotal numeric(10,2),
    vat_percentage numeric(5,2),
    vat_amount numeric(10,2),
    surcharge_percentage numeric(5,2),
    surcharge_amount numeric(10,2),
    total numeric(10,2),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table delivery_notes (
    id integer,
    code integer not null,
    date timestamp not null,
    total numeric(10,2),
    format varchar(255) not null,
    vat_percentage numeric(5,2) not null,
    surcharge_percentage numeric(5,2) not null,
    client_id integer not null references clients (id),
    invoice_id integer references invoices (id),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table delivery_notes_articles (
    id integer,
    article_id integer not null references articles (id),
    delivery_note_id integer not null references delivery_notes (id),
    traceability_code1 varchar(8) not null,
    traceability_code2 varchar(8),
    traceability_code3 varchar(8),
    quantity numeric(10,2),
    price numeric(10,2),
    total numeric(10,2),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table invoice_lines (
    id integer,
    invoice_id integer not null references invoices (id),
    delivery_note_id integer references delivery_notes (id),
    article_id integer references articles (id),
    delivery_note_code integer not null,
    delivery_note_date timestamp not null,
    article_name varchar(255) not null,
    article_code integer not null,
    traceability_code1 varchar(8) not null,
    traceability_code2 varchar(8),
    traceability_code3 varchar(8),
    quantity numeric(10,2),
    price numeric(10,2),
    total numeric(10,2),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table tax_config (
    id integer,
    vat_percentage numeric(5,2) not null,
    surcharge_percentage numeric(5,2) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

create table user_profile (
    id integer,
    full_name varchar(255) not null,
    fiscal_name varchar(255) not null,
    nif varchar(20),
    address varchar(500),
    email varchar(50),
    phone varchar(20),
    smtp_host varchar(255),
    smtp_port integer,
    mail_password_encrypted varchar(500),
    logo_data blob,
    logo_content_type varchar(50),
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

-- A deleted row frees its code; only one live row per code (CONCEPTO.md §7)
create unique index ux_articles_code_live on articles (code) where deleted_at is null;
create unique index ux_clients_code_live on clients (code) where deleted_at is null;
create unique index ux_delivery_notes_code_live on delivery_notes (code) where deleted_at is null;

-- Invoice numbers are never freed, deleted rows included (CONCEPTO.md §5)
create unique index ux_invoices_series_code on invoices (series, code);

create index idx_traceability_code1 on delivery_notes_articles (traceability_code1);
create index idx_traceability_code2 on delivery_notes_articles (traceability_code2);
create index idx_traceability_code3 on delivery_notes_articles (traceability_code3);
