CREATE TABLE notification_task (
id serial8 primary key,
chat_id int8 not null,
message text not null,
exec_time timestamp not null
)