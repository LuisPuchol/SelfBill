alter table tax_config add column valid_from date not null default 0;
create unique index ux_tax_config_valid_from on tax_config (valid_from);
