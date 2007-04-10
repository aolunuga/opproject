package onepoint.project.team.modules.process;

import onepoint.express.XEventHandler;
import onepoint.project.OpProjectSession;

import java.util.HashMap;

public class OpProcessTimerEventHandler implements XEventHandler {

	public void processEvent(HashMap event) {
		OpProcessDefinition definition = (OpProcessDefinition)(event.get(OpProcessTimer.DEFINITION));
		OpProjectSession session = (OpProjectSession)(event.get(OpProcessTimer.SESSION));
		OpCase c = (OpCase)(event.get(OpProcessTimer.CASE));
		String task_name = (String)(event.get(OpProcessTimer.TASK_NAME));
		// *** TODO: Maybe check if everything set (ASSERTION)
		// ==> Think about a special XProcessTimerEvent class, too (server-side)
		definition.routeTask(session, c, task_name);
	}

}

