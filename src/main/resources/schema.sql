create table orders (
    id varchar(16) primary key,
    status varchar(16) not null,
    customer_id varchar(16) not null,
    total_cents integer not null
);
