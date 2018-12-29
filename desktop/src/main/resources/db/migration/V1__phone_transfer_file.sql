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
  id                varchar(40) PRIMARY KEY,
  created_at        datetime     NOT NULL DEFAULT current_timestamp,
  updated_at        datetime     NOT NULL DEFAULT current_timestamp,
  name              varchar(255) NOT NULL,
  status            varchar(255) NOT NULL,
  send_to_clipboard integer      NOT NULL,
  phone_id          varchar(40)  NOT NULL REFERENCES phone(id)
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
  computer_id                 varchar(40)  NOT NULL,
  computer_secret             varchar(40)  NOT NULL,
  computer_name               varchar(255) NOT NULL,
  transfer_folder_name        varchar(255) NOT NULL,
  root_transfer_folder        varchar(255) NOT NULL,
  server_port                 integer      NOT NULL,
  current_phone_id            varchar(40) REFERENCES phone(id),
  separate_transfer_folders   integer      NOT NULL,
  open_transfer_on_completion integer      NOT NULL,
  show_transfer_action        varchar(255) NOT NULL,
  log_clipboard_transfers     integer      NOT NULL
);

CREATE TABLE file_type_settings (
  mime_type             varchar(255) NOT NULL PRIMARY KEY,
  show_action           varchar(255) NOT NULL,
  clipboard_destination varchar(255) NOT NULL
);

CREATE TABLE clipboard_log (
  id         varchar(40) PRIMARY KEY,
  created_at datetime NOT NULL DEFAULT current_timestamp,
  updated_at datetime NOT NULL DEFAULT current_timestamp,
  content    text     NOT NULL
);