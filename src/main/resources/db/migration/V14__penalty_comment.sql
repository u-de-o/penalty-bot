-- Optional comment on a penalty. Mandatory for negative entries (corrections), which are booked as
-- additional rows; penalties are never changed or deleted.
alter table penalty add column comment text;
