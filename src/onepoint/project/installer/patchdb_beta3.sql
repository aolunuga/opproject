delimiter //

CREATE PROCEDURE upgradeProjects()
BEGIN
  DECLARE complete INT DEFAULT 0;

  DECLARE project_id, portfolio_id BIGINT(20);
  DECLARE start, finish DATE;
  DECLARE budget DOUBLE PRECISION;

  DECLARE projects CURSOR FOR SELECT op_id, op_portfolio, op_start, op_finish, op_budget FROM op_project;

  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET complete = 1;

  OPEN projects;

  REPEAT
    FETCH projects INTO project_id, portfolio_id, start, finish, budget;
    IF NOT complete THEN
       UPDATE op_projectnode SET op_type = 3, op_start = start, op_finish = finish, op_budget = budget where op_id = project_id;
       IF portfolio_id IS NOT NULL THEN
          UPDATE op_projectnode SET op_supernode = portfolio_id WHERE op_id = project_id;
       END IF;
    END IF;
  UNTIL complete END REPEAT;
  
  CLOSE projects;

  COMMIT;

END
//


CREATE PROCEDURE upgradeProjectPlans()
BEGIN
  DECLARE complete INT DEFAULT 0;

  DECLARE project_id BIGINT(20);
  DECLARE calculation_mode TINYINT;

  DECLARE projects CURSOR FOR SELECT op_id, op_calculationmode FROM op_projectnode;

  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET complete = 1;

  OPEN projects;

  REPEAT
    FETCH projects INTO project_id, calculation_mode;
    IF NOT complete THEN
       UPDATE op_projectplan SET op_calculationmode = calculation_mode WHERE op_projectnode = project_id;
    END IF;
  UNTIL complete END REPEAT;
  
  CLOSE projects;

  COMMIT;

END
//


CREATE PROCEDURE upgradePortfolios()
BEGIN
  DECLARE complete INT DEFAULT 0;

  DECLARE portfolio_id, super_portfolio_id BIGINT(20);

  DECLARE portfolios CURSOR FOR SELECT op_id, op_superportfolio FROM op_projectportfolio;

  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET complete = 1;

  OPEN portfolios;

  REPEAT
    FETCH portfolios INTO portfolio_id, super_portfolio_id;
    IF NOT complete THEN
       UPDATE op_projectnode SET op_type = 1 where op_id = portfolio_id;
       IF super_portfolio_id IS NOT NULL THEN
          UPDATE op_projectnode SET op_supernode = super_portfolio_id WHERE op_id = portfolio_id;
       END IF;
    END IF;
  UNTIL complete END REPEAT;
  
  CLOSE portfolios;

  COMMIT;

END
//


CREATE PROCEDURE upgradeGoals()
BEGIN
  DECLARE complete INT DEFAULT 0;

  DECLARE goal_id, project_id BIGINT(20);

  DECLARE goals CURSOR FOR SELECT op_id, op_project FROM op_goal;

  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET complete = 1;

  OPEN goals;

  REPEAT
    FETCH goals INTO goal_id, project_id;
    IF NOT complete THEN
       UPDATE op_goal SET op_projectnode = project_id where op_id = goal_id;
    END IF;
  UNTIL complete END REPEAT;
  
  CLOSE goals;

  COMMIT;

END
//


CREATE PROCEDURE upgradeToDos()
BEGIN
  DECLARE complete INT DEFAULT 0;

  DECLARE todo_id, project_id BIGINT(20);

  DECLARE todos CURSOR FOR SELECT op_id, op_project FROM op_todo;

  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET complete = 1;

  OPEN todos;

  REPEAT
    FETCH todos INTO todo_id, project_id;
    IF NOT complete THEN
       UPDATE op_todo SET op_projectnode = project_id where op_id = todo_id;
    END IF;
  UNTIL complete END REPEAT;
  
  CLOSE todos;

  COMMIT;

END
//


CREATE PROCEDURE upgradeWorkSlips()
BEGIN
  DECLARE complete INT DEFAULT 0;

  DECLARE resource_id, user_id BIGINT(20);

  DECLARE resources CURSOR FOR SELECT op_id, op_user FROM op_resource;

  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET complete = 1;

  OPEN resources;

  REPEAT
    FETCH resources INTO resource_id, user_id;
    IF NOT complete THEN
       UPDATE op_workslip SET op_creator = user_id WHERE op_resource = resource_id;
       UPDATE op_workrecord SET op_resource = resource_id WHERE op_resource = resource_id;
    END IF;
  UNTIL complete END REPEAT;
  
  CLOSE resources;

  COMMIT;

END
//


CREATE PROCEDURE upgradeWorkRecords()
BEGIN
  DECLARE complete INT DEFAULT 0;

  DECLARE work_slip_id BIGINT(20);
  DECLARE remaining_effort DOUBLE PRECISION;

  DECLARE work_records CURSOR FOR SELECT op_workslip, op_remainingeffort FROM op_workrecord;

  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET complete = 1;

  OPEN work_records;

  REPEAT
    FETCH work_records INTO work_slip_id, remaining_effort;
    IF NOT complete THEN
       IF remaining_effort = 0 THEN
          UPDATE op_workrecord SET op_completed = 1 WHERE op_id = work_slip_id;
       ELSE
          UPDATE op_workrecord SET op_completed = 0 WHERE op_id = work_slip_id;
       END IF;
    END IF;
  UNTIL complete END REPEAT;
  
  CLOSE work_records;

  COMMIT;

END
//

delimiter ;


alter table op_activitycategory drop foreign key FKE66AE3ABA262B34D;
alter table op_activitycategory drop column op_projectplan;

alter table op_activity drop column op_mode;
alter table op_activityversion drop column op_mode;
alter table op_activity add column ( op_attributes integer );
update op_activity set op_attributes = 0;
commit;
alter table op_activityversion add column ( op_attributes integer );
update op_activityversion set op_attributes = 0;
commit;

alter table op_activity add column ( op_priority tinyint );
update op_activity set op_priority = 0;
commit;
alter table op_activityversion add column ( op_priority tinyint );
update op_activity set op_priority = 0;
commit;

drop table op_task;
drop table op_taskversion;

alter table op_projectnode drop foreign key FKEB70673D594ACD52;
alter table op_projectnode drop column op_template;
alter table op_projectnode add column ( op_type tinyint not null );
alter table op_projectnode add column ( op_start date );
alter table op_projectnode add column ( op_finish date );
alter table op_projectnode add column ( op_budget double precision );
alter table op_projectnode add column ( op_supernode bigint );
alter table op_projectnode add column ( op_templatenode bigint );
create index op_templatenode_idx on op_projectnode (op_templatenode);
create index op_supernode_idx on op_projectnode (op_supernode);
alter table op_projectnode add index FKEB70673DC47B5CE1 (op_supernode), add constraint FKEB70673DC47B5CE1 foreign key (op_supernode) references op_projectnode (op_id);
alter table op_projectnode add index FKEB70673DC69AF0DC (op_templatenode), add constraint FKEB70673DC69AF0DC foreign key (op_templatenode) references op_projectnode (op_id);
-- Set project node type and super node reference
call upgradeProjects();
update op_projectnode set op_budget = 0 where op_budget is null;
commit;
-- Set portfolio node type and super node reference
call upgradePortfolios();
alter table op_projectplan add column( op_calculationmode tinyint(4) );
-- Copy and upgrade calculation modes and copy project to project-node references
call upgradeProjectPlans();
alter table op_projectnode drop column op_calculationmode;
alter table op_projectplan add column( op_progresstracked bit );
update op_projectplan set op_progresstracked = 1;
commit;
alter table op_projectplan add column( op_template bit );
update op_projectplan set op_template = 0;
commit;

alter table op_projectplanversion add column( op_template bit );
update op_projectplanversion set op_template = 0;
commit;
alter table op_projectplanversion add column( op_user bigint );
alter table op_projectplanversion add column( op_creator bigint );
create index op_creator_idx on op_projectplanversion (op_creator);
alter table op_projectplanversion add index FKFC44DFF41EF98878 (op_creator), add constraint FKFC44DFF41EF98878 foreign key (op_creator) references op_user (op_id);

-- *** Here are errors: UpgradeGoals/ToDos() fail because of missing op_projectnode columns
alter table op_goal add column ( op_projectnode bigint );
call upgradeGoals();
alter table op_goal drop foreign key FKB45DAEB1B41CCE5B;
alter table op_goal drop column op_project;
create index op_projectnode_idx on op_goal (op_projectnode);
alter table op_goal add index FKB45DAEB1A260F8FF (op_projectnode), add constraint FKB45DAEB1A260F8FF foreign key (op_projectnode) references op_projectnode (op_id);

alter table op_todo add column ( op_projectnode bigint );
call upgradeToDos();
alter table op_todo drop foreign key FKB46397E4B41CCE5B;
alter table op_todo drop column op_project;
create index op_projectnode_idx on op_todo (op_projectnode);
alter table op_todo add index FKB46397E4A260F8FF (op_projectnode), add constraint FKB46397E4A260F8FF foreign key (op_projectnode) references op_projectnode (op_id);

alter table op_activity add column( op_template bit );
update op_activity set op_template = 0;
commit;
alter table op_activityversion add column( op_template bit );
update op_activityversion set op_template = 0;
commit;

drop table op_projecttemplate;
drop table op_project;
drop table op_projectportfolio;

alter table op_activitycategory add column( op_active bit );
update op_activitycategory set op_active = 1;
commit;

create table op_activitycomment (op_id bigint not null, op_name varchar(255), op_text varchar(255), op_sequence integer, op_activity bigint, op_creator bigint, primary key (op_id));
create index op_creator_idx on op_activitycomment (op_creator);
create index op_activity_idx on op_activitycomment (op_activity);
alter table op_activitycomment add index FK2F77BE1248B609D (op_id), add constraint FK2F77BE1248B609D foreign key (op_id) references op_object (op_id);
alter table op_activitycomment add index FK2F77BE121EF98878 (op_creator), add constraint FK2F77BE121EF98878 foreign key (op_creator) references op_user (op_id);
alter table op_activitycomment add index FK2F77BE128122B315 (op_activity), add constraint FK2F77BE128122B315 foreign key (op_activity) references op_activity (op_id);

create table op_document (op_id bigint not null, op_name varchar(255) not null unique, op_content bigint, op_creator bigint, primary key (op_id));
create index op_creator_idx on op_document (op_creator);
create index op_content_idx on op_document (op_content);
alter table op_document add index FKA80D01B9CF8EB1DC (op_content), add constraint FKA80D01B9CF8EB1DC foreign key (op_content) references op_content (op_id);
alter table op_document add index FKA80D01B948B609D (op_id), add constraint FKA80D01B948B609D foreign key (op_id) references op_object (op_id);
alter table op_document add index FKA80D01B91EF98878 (op_creator), add constraint FKA80D01B91EF98878 foreign key (op_creator) references op_user (op_id);

create table op_dynamicresource (op_id bigint not null, op_locale varchar(255), op_name varchar(255), op_value varchar(255), op_object bigint, primary key (op_id));
create index op_object_idx on op_dynamicresource (op_object);
alter table op_dynamicresource add index FK567BF6EF48B609D (op_id), add constraint FK567BF6EF48B609D foreign key (op_id) references op_object (op_id);
alter table op_dynamicresource add index FK567BF6EF1EDB37C1 (op_object), add constraint FK567BF6EF1EDB37C1 foreign key (op_object) references op_object (op_id);

create table op_reporttype (op_id bigint not null, op_name varchar(255) not null unique, primary key (op_id));
alter table op_reporttype add index FK7C93AB8C48B609D (op_id), add constraint FK7C93AB8C48B609D foreign key (op_id) references op_object (op_id);

create table op_report (op_id bigint not null, op_type bigint, primary key (op_id));
create index op_type_idx on op_report (op_type);
alter table op_report add index FK25EC15522AE6010C (op_id), add constraint FK25EC15522AE6010C foreign key (op_id) references op_document (op_id);
alter table op_report add index FK25EC1552D27B5971 (op_type), add constraint FK25EC1552D27B5971 foreign key (op_type) references op_reporttype (op_id);

alter table op_workslip add column( op_creator bigint );
create index op_creator_idx on op_workslip (op_creator);
alter table op_workslip add index FK76D1F06F1EF98878 (op_creator), add constraint FK76D1F06F1EF98878 foreign key (op_creator) references op_user (op_id);
-- Replace resource references of work-slips with user references
call upgradeWorkSlips();
alter table op_workslip drop foreign key FK76D1F06F6F56F59C;
alter table op_workslip drop column op_resource;

alter table op_workrecord add column( op_completed bit );
-- Set new column completed according to remaining efforts
call upgradeWorkRecords();

drop procedure upgradeProjects;
drop procedure upgradePortfolios;
drop procedure upgradeProjectPlans;
drop procedure upgradeGoals;
drop procedure upgradeToDos;
drop procedure upgradeWorkSlips;
drop procedure upgradeWorkRecords;

-- Remove all manually generated indexes

drop index op_creator_idx on op_activitycomment;
drop index op_activity_idx on op_activitycomment;
drop index op_activity_idx on op_activityversion;
drop index op_category_idx on op_activityversion;
drop index op_superactivityversion_idx on op_activityversion;
drop index op_planversion_idx on op_activityversion;
drop index op_activity_idx on op_assignment;
drop index op_resource_idx on op_assignment;
drop index op_projectplan_idx on op_assignment;
drop index op_activityversion_idx on op_assignmentversion;
drop index op_planversion_idx on op_assignmentversion;
drop index op_resource_idx on op_assignmentversion;
drop index op_activity_idx on op_attachment;
drop index op_content_idx on op_attachment;
drop index op_projectplan_idx on op_attachment;
drop index op_content_idx on op_attachmentversion;
drop index op_activityversion_idx on op_attachmentversion;
drop index op_planversion_idx on op_attachmentversion;
drop index op_user_idx on op_contact;
drop index op_successoractivity_idx on op_dependency;
drop index op_predecessoractivity_idx on op_dependency;
drop index op_projectplan_idx on op_dependency;
drop index op_successorversion_idx on op_dependencyversion;
drop index op_predecessorversion_idx on op_dependencyversion;
drop index op_planversion_idx on op_dependencyversion;
drop index op_creator_idx on op_document;
drop index op_content_idx on op_document;
drop index op_object_idx on op_dynamicresource;
drop index op_projectnode_idx on op_goal;
drop index op_supergroup_idx on op_groupassignment;
drop index op_subgroup_idx on op_groupassignment;
drop index op_target_idx on op_lock;
drop index op_owner_idx on op_lock;
drop index op_object_idx on op_permission;
drop index op_subject_idx on op_permission;
drop index op_user_idx on op_preference;
drop index op_templatenode_idx on op_projectnode;
drop index op_supernode_idx on op_projectnode;
drop index op_projectnode_idx on op_projectnodeassignment;
drop index op_resource_idx on op_projectnodeassignment;
drop index op_projectnode_idx on op_projectplan;
drop index op_creator_idx on op_projectplanversion;
drop index op_projectplan_idx on op_projectplanversion;
drop index op_type_idx on op_report;
drop index op_user_idx on op_resource;
drop index op_pool_idx on op_resource;
drop index op_superpool_idx on op_resourcepool;
drop index op_projectnode_idx on op_todo;
drop index op_group_idx on op_userassignment;
drop index op_user_idx on op_userassignment;
drop index op_activity_idx on op_workphase;
drop index op_projectplan_idx on op_workphase;
drop index op_activityversion_idx on op_workphaseversion;
drop index op_planversion_idx on op_workphaseversion;
drop index op_workslip_idx on op_workrecord;
drop index op_assignment_idx on op_workrecord;
drop index op_creator_idx on op_workslip;

-- Note: Creating project plans for all project nodes of type project is done on application start-up
