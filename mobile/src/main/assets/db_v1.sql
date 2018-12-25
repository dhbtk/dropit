CREATE TABLE computer (
  id           varchar(40) PRIMARY KEY,
  secret       varchar(40),
  name         varchar(40)  NOT NULL,
  ip_address   varchar(255) NOT NULL,
  port         int          NOT NULL,
  token        varchar(40),
  token_status int DEFAULT -1
);