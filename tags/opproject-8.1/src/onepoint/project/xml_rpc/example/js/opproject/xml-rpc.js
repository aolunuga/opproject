/*
 * utility script for to enable xml-rpc functionality with OnePoint Project.
 */

//import jsolait.*;

var services = [ 'UserService.signOn', 
								 'UserService.signOff', 
								 'UserService.getSignedOnUserData',
								 'MyTasksService.getRootTasks',
								 'MyTasksService.getParentTask',
								 'MyTasksService.getChildTasks',
								 'MyTasksService.getMyTasks' ];

var xmlrpc=null;
try{
    var xmlrpc = imprt("xmlrpc");
}catch(e){
    throw "importing of xmlrpc module failed.";
}

function OpXmlRpc(url) {
	this.service = new xmlrpc.ServiceProxy(url, services);
	this.getServiceNames = getServiceNames
}		
function getServiceNames() {
	return (services);
}
