package onepoint.project.team.modules.process;

import onepoint.express.XTimer;
import onepoint.project.OpProjectSession;

import java.util.HashMap;

public class OpProcessTimer {

	// *** Could also be named "XTaskScheduler"

	// Process timer event arguments
	public final static String DEFINITION = "definition";
	public final static String SESSION = "session";
	public final static String CASE = "case";
	public final static String TASK_NAME = "task_name";

	private static XTimer timer = new XTimer();
	private static OpProcessTimerEventHandler eventHandler = new OpProcessTimerEventHandler();

	public static void scheduleTask(OpProcessDefinition definition, OpProjectSession session, OpCase c, String task_name, long delay) {
		// *** Note: If we want to cancel the timer we have to give it a name
		// ==> Unique name could be maybe case number and task name?
		HashMap event = new HashMap();
		event.put(DEFINITION, definition);
		event.put(SESSION, session);
		event.put(CASE, c);
		event.put(TASK_NAME, task_name);
		timer.setWatch(null, eventHandler, delay, 0, event);
	}

}
