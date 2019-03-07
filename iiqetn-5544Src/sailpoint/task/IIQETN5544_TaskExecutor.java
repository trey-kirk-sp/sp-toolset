package sailpoint.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sailpoint.api.SailPointContext;
import sailpoint.api.TaskManager;
import sailpoint.api.Terminator;
import sailpoint.object.Application;
import sailpoint.object.Attributes;
import sailpoint.object.Partition;
import sailpoint.object.Request;
import sailpoint.object.RequestDefinition;
import sailpoint.object.SailPointObject;
import sailpoint.object.Server;
import sailpoint.object.TaskDefinition;
import sailpoint.object.TaskItemDefinition.Type;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;
import sailpoint.tools.xml.XMLObjectFactory;

public class IIQETN5544_TaskExecutor extends AbstractTaskExecutor {
    
    public static final String ARG_TASK_SERVER = "taskServer";
    public static final String ARG_PARTITION_SERVER = "partitionServer";
    public static final String ARG_CHECK_DELETE_SERVER = "checkDeleteServer";
    public static final String ARG_AGGREGATION_TASK = "aggTask";
    public static final String ARG_BUILD_NOW = "buildNow";
    public static final String RET_RESULT = "result";
    public static final String TASK_RESULT_NAME = "IIQETN-5544 - Primary Task";
    
    @Override
    public void execute(SailPointContext context, TaskSchedule schedule,
            TaskResult result, Attributes<String, Object> args)
            throws Exception {
        
        // 1. get & validate the arguments (could they be invalid)
        
        // 2. if 'buildNow' set, continue to build TaskResult & Request objects. Else
        //      build a new TaskDefinition copying these arguments, add an additional arg
        //      buildNow = true. Set the host name to taskServer and schedule the task
        
        // 3. when building, build the TaskResult, set internalStartDate to this server's now; commit
        //      - build the Request objects; set their hostnames appropriately; commit
        //      - at this point, the machine is in motion. Let's see how it plays out.
        
        boolean buildNow = args.getBoolean(ARG_BUILD_NOW);
        if (buildNow) {
            build(context, args, result);
        } else {
            args.put(ARG_BUILD_NOW, true);
            TaskDefinition thisDefinition = result.getDefinition();
            assert(thisDefinition != null);
            String buildTaskName = thisDefinition.getName() + " - build task";
            Server taskServer = getServer(context, args.getString(ARG_TASK_SERVER));
            TaskDefinition buildTask = getBuiltTaskDefinition(context, buildTaskName);
            buildTask.setArguments(new Attributes<String, Object>(args));
            buildTask.setHost(taskServer.getName());
            context.saveObject(buildTask);
            result.setAttribute(RET_RESULT, "Launching build task: " + buildTask.getName());
            context.saveObject(result);
            context.commitTransaction();
            
            TaskManager tm = new TaskManager(context);
            tm.run(buildTask, new Attributes<String, Object>());
        }
        
        
    }
    
    private TaskDefinition getBuiltTaskDefinition(SailPointContext context, String definitionName) throws GeneralException {
        TaskDefinition buildTask = context.getObjectByName(TaskDefinition.class, definitionName);
        delete(context, TaskDefinition.class, definitionName);
        buildTask = new TaskDefinition();
        buildTask.setExecutor(IIQETN5544_TaskExecutor.class);
        buildTask.setName(definitionName);
        buildTask.setArgument(ARG_BUILD_NOW, true);
        
        return buildTask;
    }

    private void build(SailPointContext context, Attributes<String, Object> args, TaskResult originalResult) throws GeneralException {
        // 3. when building, build the TaskResult, set internalStartDate to this server's now; commit
        //      - build the Request objects; set their hostnames appropriately; commit
        //      - at this point, the machine is in motion. Let's see how it plays out.

        Server taskServer = getServer(context, args.getString(ARG_TASK_SERVER));
        Server partitionServer = getServer(context, args.getString(ARG_PARTITION_SERVER));
        Server checkDeletedServer = getServer(context, args.getString(ARG_CHECK_DELETE_SERVER));
        String aggTaskName = args.getString(ARG_AGGREGATION_TASK);
        TaskDefinition aggregationTask = context.getObject(TaskDefinition.class, aggTaskName);

        if (aggregationTask == null) {
            throw new GeneralException(aggTaskName);
        }
        
        TaskResult aggResult = buildTaskResult(context, aggregationTask, taskServer, originalResult);
        buildPartitions(context, aggregationTask, aggResult, partitionServer, checkDeletedServer);
    }
    
    private Request buildRequest(SailPointContext context, TaskResult result) throws GeneralException {
        
        RequestDefinition definition = context.getObjectByName(RequestDefinition.class, "Aggregate Partition");

        // build Aggregation Request
        Request request = new Request();
        request.setType(Type.Partition);
        request.setLauncher(result.getLauncher());
        request.setDefinition(definition);
        request.setAttribute("taskDefinitionName", result.getDefinition().getName());
        request.setType(Type.Partition);
        request.setTaskResult(result);
        
        /*
        request.setAttribute("applications", "Active_Directory");
        request.setAttribute("checkDeleted", "true");
        request.setAttribute("checkHistory", "false");
        request.setAttribute("checkPolicies", "false");
        request.setAttribute("correlateEntitlements", "false");
        request.setAttribute("correlateOnly", "false");
        request.setAttribute("correlateScope", "false");
        request.setAttribute("deltaAggregation", "false");
        request.setAttribute("enablePartitioning", "true");
        request.setAttribute("haltOnMaxError", "false");
        request.setAttribute("launcher", result.getLauncher());
        request.setAttribute("noAutoCreateApplications", "false");
        request.setAttribute("noAutoCreateScopes", "false");
        request.setAttribute("noManagerCorrelation", "true");
        request.setAttribute("noNeedsRefresh", "false");
        request.setAttribute("noOptimizeReaggregation", "false");
        request.setAttribute("objectsPerPartition", "1000");
        request.setAttribute("promoteManagedAttributes", "false");
        request.setAttribute("refreshCertifications", "false");
        request.setAttribute("refreshScorecard", "false");
        request.setAttribute("sequential", "false");
        request.setAttribute("taskCompletionEmailNotify", "Disabled");
        */
        request.setAttribute("locale", "en_US");
        request.setAttribute("timezone", "America/Chicago");
        return request;
    }
    
    private void buildPartitions(SailPointContext context, TaskDefinition aggTask, TaskResult result, 
            Server partitionServer, Server checkDeletedServer) throws GeneralException {

        // build Partitioning Request - This should be done, right?
        //request.setName("Active_Directory - Accounts 1 to 263");
        //request.setDependentPhase(1);
        //request.setPhase(2);
        
        Attributes<String, Object> baseAttributes = aggTask.getArguments();

        Request partitionRequest = buildRequest(context, result);
        partitionRequest.setName("Partitioning");
        partitionRequest.setDependentPhase(-1);
        partitionRequest.setPhase(1);
        partitionRequest.setAttribute("aggregationPhase", "partition");
        context.saveObject(partitionRequest);

        // build Aggregation Request
        partitionRequest = buildRequest(context, result);
        partitionRequest.setName("Active_Directory - Accounts 1 to 263");
        partitionRequest.setDependentPhase(1);
        partitionRequest.setPhase(2);
        partitionRequest.setHost(partitionServer.getName());
        // copy the attributes, don't use the same object!
        partitionRequest.setAttributes(new Attributes<String, Object>(baseAttributes));
        
        Partition partition = new Partition();
        Application active_directory = context.getObjectByName(Application.class, "Active_Directory");
        // absolutePath is not absolute, wonder if it'll work
        String filePath = (String)active_directory.getAttributeValue("file");
        File file = new File(filePath);
        partition.setAttribute("absolutePath", file.getAbsolutePath());
        partition.setAttribute("endObject", new Integer(263));
        partition.setAttribute("fileName", file.getName());
        // not sure if I need this
        partition.setAttribute("length", new Long(61263));
        partition.setAttribute("startObject", new Integer(0));
        partition.setAttribute("objectType", "account");
        partitionRequest.setAttribute("partition", partition);
        context.saveObject(partitionRequest);
        
        // build Check Deleted Request
        partitionRequest = buildRequest(context, result);
        partitionRequest.setAttributes(new Attributes<String, Object>(baseAttributes));
        partitionRequest.setName("Check Deleted Objects - Active_Directory");
        partitionRequest.setDependentPhase(2);
        partitionRequest.setPhase(3);
        partitionRequest.setAttribute("aggregationPhase", "checkDeleted");
        partitionRequest.setHost(checkDeletedServer.getName());
        context.saveObject(partitionRequest);
        
        // build Finish Agg Request
        partitionRequest = buildRequest(context, result);
        partitionRequest.setAttributes(new Attributes<String, Object>(baseAttributes));
        partitionRequest.setName("Finish Aggregation");
        partitionRequest.setPhase(4);
        partitionRequest.setAttribute("aggregationPhase", "finish");
        context.saveObject(partitionRequest);

        // commit it all
        context.commitTransaction();
    }
    
    private void delete(SailPointContext context, Class<? extends SailPointObject> clazz, String name) throws GeneralException {
        SailPointObject toDelete = context.getObjectByName(clazz, name);
        if (toDelete != null) {
            Terminator t = new Terminator(context);
            t.deleteObject(toDelete);
            context.commitTransaction();
        }
    }
    
    
    private TaskResult buildTaskResult(SailPointContext context, TaskDefinition aggTask, Server taskServer, TaskResult originalResult) 
            throws GeneralException {
        // Purge the existing one, if exists
        delete(context, TaskResult.class, TASK_RESULT_NAME);

        // TaskResult needs to define the task hostname (this host), internalStartDate (now), the partition
        // TaskResults, link the definition, and set the owner (whoever launched the original task)
        //
        // Start by first cloning the original. It's got most of the good stuff already
        XMLObjectFactory factory = XMLObjectFactory.getInstance();
        TaskResult mainTaskResult = (TaskResult)factory.clone(originalResult, context);
        mainTaskResult.setId(null);
        mainTaskResult.setName(TASK_RESULT_NAME);
        Attributes<String, Object> args = new Attributes<String, Object>();
        args.put("internalStartDate", new Date().getTime());
        List<TaskResult> partitions = new ArrayList<TaskResult>();
        TaskResult partition = new TaskResult();
        partition.setName("Partitioning");
        partitions.add(partition);
        
        TaskResult aggResult = new TaskResult();
        aggResult.setName("Active_Directory - Accounts 1 to 263");
        partitions.add(aggResult);
        
        TaskResult checkDeleteResult = new TaskResult();
        checkDeleteResult.setName("Check Deleted Objects - Active_Directory");
        partitions.add(checkDeleteResult);
        
        TaskResult finishResult = new TaskResult();
        finishResult.setName("Finish Aggregation");
        partitions.add(finishResult);
        
        args.put("taskResultPartitions", partitions);
        mainTaskResult.setAttributes(args);
        
        mainTaskResult.setDefinition(aggTask);
        
        mainTaskResult.setPartitioned(true);
        mainTaskResult.setType(Type.AccountAggregation); 
        context.saveObject(mainTaskResult);
        context.commitTransaction();
        return mainTaskResult;
    }

    private Server getServer(SailPointContext context, String serverId) throws GeneralException {
        if (Util.isNullOrEmpty(serverId)) {
            throw new GeneralException("Null serverId!");
        }
        Server server = context.getObject(Server.class, serverId);
        if (server == null) {
            throw new GeneralException("Invalid serverId (not found): " + serverId);
        }
        return server;
    }

    @Override
    public boolean terminate() {
        // Ya, we don't particularly GAF about terminating, you whiney, little, ...
        return false;
    }

}
