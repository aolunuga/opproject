/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project.processes;

import onepoint.project.OpProjectSession;
import onepoint.project.team.modules.process.OpCase;
import onepoint.project.team.modules.process.OpProcessDefinition;
import onepoint.project.team.modules.process.OpProcessTimer;

public class OpStatusUpdateProcessDefinition extends OpProcessDefinition {

  public final static String LOCK_PROJECT = "LockProject";
  public final static String REMINDER_TIMER = "ReminderTimer";
  public final static String REMINDER_CONDITION = "ReminderCondition";
  public final static String REMINDER_NOTIFICATION = "ReminderNotification";
  public final static String COMPLETE_TIMER = "CompleteTimer";
  public final static String COMPLETE_CONDITION = "CompleteCondition";
  public final static String COMPLETE_NOTIFICATION = "CompleteNotification";
  // ...

  // *** Maybe This is somehow linked to a XProcessService
  // ==> We probably at least need a session of some sort?

  // *** How is a case started?
  // ==> Probably by setting a (short) timer that calls executeTask(START)
  // ==> Note: Should be executed as workflow/system-user

  // *** Where do we store status information, i.e., case variables
  // ==> Maybe by using a serialized version of a Java object?
  // *** Probably use a real process-specific class, e.g., XCaseVariables
  // ==> And store as property OpCase.Variables of type Content

  protected String executeTask(OpProjectSession session, OpCase c, String task_name) {
    // *** (1) Lock project definition [built-in user "workflow"/"system"]
    // ==> Throw exception and suspend case if already locked
    // ==> Note: Even better would be "Locked by Status Update Process"
    // *** [Maybe already create working versions for all project resources]
    // ==> [Advantage: No additional "button" to press; just "complete"]
    // *** (2) Notify project resources (users) that process was started
    // ==> Include link to status tool in e-mail notification
    // *** (3) Start timer-1 (configurable time-out)
    // ==> Fires transition on time-out [could return null: Asynchronous]
    // *** (4) Check condition if everyone has completed status update
    // ==> If yes: Go to (8) else go to (5)
    // *** (5) Start timer-2 (configurable time-out)
    // *** (6) Check condition if everyone has completed status update
    // ==> If yes: Go to (8) else go to (7)
    // *** (7) Automatically check-in for those who have not yet completed
    // ==> And, send e-mail to not-completed participants: Process is over
    // *** (8) Notify project manager that process is completed
    // ==> Provide details (who did not complete; percentage) including link
    // *** (9) Probably have again time-out for PM to work on status update
    // ==> Otherwise, the next update could start before this one finished
    // ==> [Note that this is probably only true for scheduled updates]
    // *** (10) Unlock project definition

    if (task_name == START)
      return LOCK_PROJECT;
    else if (task_name == LOCK_PROJECT)
      return executeLockProject(session, c);
    // *** TODO: Probably insert a step here (create working versions)
    else if (task_name == REMINDER_TIMER)
      return executeReminderTimer(session, c);
    else if (task_name == REMINDER_CONDITION)
      return executeReminderCondition(session, c);
    else if (task_name == REMINDER_NOTIFICATION)
      return executeReminderNotification(session, c);
    else if (task_name == COMPLETE_TIMER)
      return executeCompleteTimer(session, c);
    else if (task_name == COMPLETE_CONDITION)
      return executeCompleteCondition(session, c);
    else if (task_name == COMPLETE_NOTIFICATION)
      return executeCompleteNotification(session, c);

    // ==> Start timer for time-out (warning)
    // *** Problem: We cannot block here (waiting for a return value)
    // ==> But, do we have to "wait"?
    // *** Still, we *remain* in this task until user completes it
    // ==> Problem: Every user has to complete "his" (copy of the) task
    return null;
  }

  // *** Maybe better: EVENT-based
  // ==> Event triggers something in the workflow engine, e.g., a transition

  public String executeLockProject(OpProjectSession session, OpCase c) {
    // *** Lock project w/workflow or system user
    // ==> Needs OpProjectSession or at least OpBroker

    // *** Get project-ID from case-variables
    // *** Get project from broker
    // *** Exception if project already locked
    // *** Insert lock for project

    // Synchronous routing to task REMINDER_TIMER
    return REMINDER_TIMER;
  }

  public String executeReminderTimer(OpProjectSession session, OpCase c) {
    // *** Set reminder timer in XTimer
    // ==> But needs a wrapper around XTimer: Need callback w/session, user

    long delay = 24 * 3600 * 1000; // *** 24h; TODO: Should be configurable
    OpProcessTimer.scheduleTask(this, session, c, REMINDER_CONDITION, delay);

    // Asynchronous routing (nothing to be currently done)
    return null;
  }

  public String executeReminderCondition(OpProjectSession session, OpCase c) {
    // *** Synchronous routing possible when executing condition
    // ==> Check if all project participants have completed status update

    // *** Get project-ID from case-variables
    // *** Get project from broker
    // *** Get resources from project
    boolean completed = false;
    // *** Iterate through resources
    // ***    Check if all project-progress objects have Submitted date set

    // *** For each user, a kind of working version should exist
    // ==> We probably need a separate prototype for storing/managing this
    // *** Info for XProjectUpdate/Progress
    // ==> Submitted (Date), suggested durations and completes (Content?)

    if (completed)
      return COMPLETE_NOTIFICATION;
    else
      return REMINDER_NOTIFICATION;

  }

  public String executeReminderNotification(OpProjectSession session, OpCase c) {

    // *** Get project-id from case-variables
    // *** Get project from broker
    // *** Get project resources and respective users from project
    // *** Send e-mail notification w/customizable HTML text to users

    // *** E-Mail API: session.getMailer().sendMessage(OpMailMessage mail)

    return COMPLETE_TIMER;
  }

  public String executeCompleteTimer(OpProjectSession session, OpCase c) {
    return null;
  }

  public String executeCompleteCondition(OpProjectSession session, OpCase c) {
    return null;
  }

  public String executeCompleteNotification(OpProjectSession session, OpCase c) {
    return null;
  }

}
