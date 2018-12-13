PRAGMA FOREIGN_KEYS = ON;

CREATE TABLE phone (
  id         varchar(40) PRIMARY KEY,
  created_at datetime     NOT NULL DEFAULT current_timestamp,
  updated_at datetime     NOT NULL DEFAULT current_timestamp,
  name       varchar(255) NOT NULL,
  status     varchar(255) NOT NULL,
  token      varchar(40) UNIQUE
);

CREATE TABLE transfer (
  id         varchar(40) PRIMARY KEY,
  created_at datetime     NOT NULL DEFAULT current_timestamp,
  updated_at datetime     NOT NULL DEFAULT current_timestamp,
  name       varchar(255) NOT NULL,
  status     varchar(255) NOT NULL,
  phone_id   varchar(40)  NOT NULL REFERENCES phone(id)
);

CREATE TABLE transfer_file (
  id          varchar(40) PRIMARY KEY,
  created_at  datetime     NOT NULL DEFAULT current_timestamp,
  updated_at  datetime     NOT NULL DEFAULT current_timestamp,
  file_name   varchar(255) NOT NULL,
  mime_type   varchar(255) NOT NULL,
  file_size   bigint       NOT NULL,
  status      varchar(255) NOT NULL,
  transfer_id varchar(40)  NOT NULL REFERENCES transfer(id)
);

CREATE TABLE settings (
  computer_id          varchar(40)  NOT NULL,
  computer_secret      varchar(40)  NOT NULL,
  computer_name        varchar(255) NOT NULL,
  transfer_folder_name varchar(255) NOT NULL,
  root_transfer_folder varchar(255) NOT NULL,
  server_port          integer      NOT NULL
);