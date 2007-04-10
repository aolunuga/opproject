package onepoint.project.team.modules.process;

import onepoint.project.OpProjectSession;

public abstract class OpProcessDefinition {

  // Built-in start and end process states
  public final static String START = "Start";
  public final static String END = "End";

  // *** Maybe provide also followed transition_name?
  public String routeTask(OpProjectSession session, OpCase c, String task_name) {
    // Route synchronously while possible
    String synchronous_route = task_name;
    while (synchronous_route != null) {
      c.setActiveTaskName(synchronous_route);
      session.newBroker().updateObject(c);
      // *** TODO: Commit transaction
      synchronous_route = executeTask(session, c, synchronous_route);
    }
    return synchronous_route;
  }

  // The following method returns a new task name (routing)
  protected abstract String executeTask(OpProjectSession session, OpCase c, String task_name);

}
