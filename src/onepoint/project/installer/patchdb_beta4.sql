delimiter //

CREATE PROCEDURE upgradeWorkRecords06b4()
BEGIN
  DECLARE complete INT DEFAULT 0;

  DECLARE work_record_id BIGINT(20);
  DECLARE actual_effort DOUBLE PRECISION;
  DECLARE assignment_id BIGINT(20);
  DECLARE resource_id BIGINT(20);
  DECLARE hourly_rate DOUBLE PRECISION;

  DECLARE work_records CURSOR FOR SELECT op_id, op_actualeffort, op_assignment FROM op_workrecord;

  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET complete = 1;

  OPEN work_records;

  REPEAT
    FETCH work_records INTO work_record_id, actual_effort, assignment_id;
    IF NOT complete THEN
       SELECT op_resource FROM op_assignment WHERE op_id = assignment_id INTO resource_id;
       SELECT op_hourlyrate FROM op_resource WHERE op_id = resource_id INTO hourly_rate;
       UPDATE op_workrecord SET op_personnelcosts = actual_effort * hourly_rate WHERE op_id = work_record_id;
    END IF;
  UNTIL complete END REPEAT;
  
  CLOSE work_records;

  COMMIT;

END
//

delimiter ;


create table op_schedule (op_id bigint not null, op_name varchar(255) unique, op_description varchar(255), op_start date, op_unit integer, op_interval integer, op_mask integer, op_lastexecuted date, primary key (op_id));
alter table op_schedule add index FK4B16C33548B609D (op_id), add constraint FK4B16C33548B609D foreign key (op_id) references op_object (op_id);

update op_activity set op_priority = 0 where op_priority is null;
commit;
update op_activityversion set op_priority = 0 where op_priority is null;
commit;

alter table op_workrecord add column ( op_personnelcosts double precision );
call upgradeWorkRecords06b4();

alter table op_user add column ( op_authenticationtype tinyint );
update op_user set op_authenticationtype = 0;
commit;

create index op_activity_start_i on op_activity (op_start);
create index op_activity_finish_i on op_activity (op_finish);
create index op_activityversion_start_i on op_activityversion (op_start);
create index op_activityversion_finish_i on op_activityversion (op_finish);
create index op_dynamicresource_name_i on op_dynamicresource (op_name);
create index op_object_created_i on op_object (Created);
create index op_projectplan_finish_i on op_projectplan (op_finish);
create index op_projectplan_start_i on op_projectplan (op_start);
create index op_projectplanversion_finish_i on op_projectplanversion (op_finish);
create index op_projectplanversion_start_i on op_projectplanversion (op_start);
create index op_workphase_start_i on op_workphase (op_start);
create index op_workphase_finish_i on op_workphase (op_finish);
create index op_workphaseversion_finish_i on op_workphaseversion (op_finish);
create index op_workphaseversion_start_i on op_workphaseversion (op_start);
create index op_workslip_date_i on op_workslip (op_date);


drop procedure upgradeWorkRecords06b4;
