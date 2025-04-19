import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;



public class RoundRobin{

    private static volatile boolean lastTaskDone;
    private static Semaphore[] coreStart;
    private static Semaphore[] dispatcherStart;
    private static Semaphore[] taskStart;
    private static Semaphore[] taskFinished;
    private static int quantum;
    private static int numTasks;
    private static int numCores;
    private static Random random;
    private static Dispatcher[] dispatchers;
    private static Core[] cores;
    private static Task[] tasks;
    private static Semaphore readyQueueSem = new Semaphore(1);
    private static int tasksFinished = 0;
    private static Semaphore finishedSem = new Semaphore(1);
    private static Queue<Task> readyQueue = new LinkedList<>();
    private static List<Thread> threads = new ArrayList<>();

public static void main(int[] args)
{
    
    numCores = args[0];
    quantum = args[1];

    lastTaskDone = false;

    //creates random object and sets numTasks
    random = new Random();
    numTasks = random.nextInt(1,26);

    System.out.println("Number of tasks: " + numTasks);
    System.out.println("Number of cores: " + numCores);
    System.out.println("Quantum: " + quantum);

    //initializes object arrays
    dispatchers = new Dispatcher[numCores];
    cores = new Core[numCores];
    tasks = new Task[numTasks];

    //initializes semaphore arrays
    coreStart = new Semaphore[numCores];
    dispatcherStart = new Semaphore[numCores];
    taskStart = new Semaphore[numTasks];
    taskFinished = new Semaphore[numTasks];



    //initializes and starts task objects and threads, adds tasks to array
    for(int i = 0; i < numTasks; i++)
    {
        taskStart[i] = new Semaphore(0);
        taskFinished[i] = new Semaphore(0);
        Task task = new Task(i);
        tasks[i] = task;
        Thread t = new Thread(task);
        threads.add(t);
        t.start();

        readyQueue.add(task);
        System.out.println(task + " has been added to ready queue with " + task.getBurstTime() + " bursts.");
    }


    //populates sem and object arrays, starts threads
    for(int i = 0; i < numCores; i++)
    {
        dispatcherStart[i] = new Semaphore(0);
        Dispatcher dispatcher = new Dispatcher(i);
        dispatchers[i] = dispatcher;
        Thread d = new Thread(dispatcher);
        threads.add(d);
        d.start();

        coreStart[i] = new Semaphore(0);
        Core core = new Core(i);
        cores[i] = core;
        Thread c = new Thread(core);
        threads.add(c);
        c.start();

    }

    while(!lastTaskDone)
    {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("Error while waiting for last task!");
        }
    }

    for (Thread tdc : threads)
    {
        try{
            tdc.join();
        } catch(Exception E)
        {
            System.out.println("Error when joining threads!");
        }
    }

    System.out.println("Simulation Finished!");

    
}






private static class Task implements Runnable
{
    private int id;
    private int burstTime;
    private int remainingBursts;
    private int allotedBursts;
    private int currentBurst;
    private int coreNum;
    private boolean isComplete;

    public Task(int id) {

        this.id = id;

        //tasks burst times are generated at task creation by task
        this.burstTime = random.nextInt(1, 51);
        this.remainingBursts = burstTime;
        this.allotedBursts = 0;
        this.currentBurst = 0;
        isComplete = false;
    }

    @Override
    public void run()
    {

        while(remainingBursts > 0)
        {
            taskStart[id].acquireUninterruptibly();

            //loop for each burst(print info)
            while(currentBurst < allotedBursts)
            {
                System.out.println(this.toString() + " is running! [" + (currentBurst + 1) +" / " + allotedBursts + "] Core# " + coreNum);
                currentBurst++;
                remainingBursts--;
            }

            currentBurst = 0;

            System.out.println(this.toString() + " has finished its bursts.");

            taskFinished[id].release();
            
        }

        isComplete = true;



        System.out.println(this.toString() + " has finished!");
        finishedSem.acquireUninterruptibly();
        tasksFinished++;

        if(tasksFinished == numTasks)
        {
            lastTaskDone = true;

            for(int i = 0; i < numCores; i++)
            {
                coreStart[i].release();
                dispatcherStart[i].release();
            }
        }
        finishedSem.release();

        
    }

    public int getId() {
        return id;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public int getRemainingTime() {
        return remainingBursts;
    }

    public boolean isComplete()
    {
        return isComplete;
    }

    public void setBurst(int bursts)
    {
        this.allotedBursts = bursts;
    }

    public void setCore(int coreNum)
    {
        this.coreNum = coreNum;
    }

    @Override
    public String toString()
    {
        String name = "Task #" + id;
        return name;
    }
}

private static class Dispatcher implements Runnable
{
    private int id;
    private Task newTask = null;

    public Dispatcher(int id) {
        this.id = id;
    }

    @Override
    public void run()
    {

        while(!lastTaskDone)
        {
            //allows dispatcher to wake up
            dispatcherStart[id].acquireUninterruptibly();

            if(lastTaskDone)
            {
                break;
            }

            //checks if newTask needs to be added back to ready queue
            if((!(newTask==null)) && !newTask.isComplete())
            {
                readyQueueSem.acquireUninterruptibly();
                readyQueue.add(newTask);
                readyQueueSem.release();
                System.out.println(newTask + " has been added to the ready queue again");
            }

            newTask = null;

            //pulls task from queue
            readyQueueSem.acquireUninterruptibly();
            newTask = readyQueue.poll();
            readyQueueSem.release();

            if(newTask == null)
            {
                dispatcherStart[id].release();
                continue;
            }

            System.out.println(newTask + " has been removed from ready queue to be placed on Core# " + id);

            //task logic end here

            //assign task and burst to core
            cores[id].setTask(newTask);
            cores[id].setBursts(Math.min(quantum, newTask.getRemainingTime()));
            System.out.println(newTask + " has " + Math.min(quantum, newTask.getRemainingTime()) + " bursts.");
            

            coreStart[id].release();
        }

    }
}

private static class Core implements Runnable
{
    private int id;
    private int bursts;
    private Task currentTask;


    public Core(int id) {
        this.id = id;
    }

    @Override
    public void run()
    {

        //wakes up dispatcher for first run
        dispatcherStart[id].release();

        while(!lastTaskDone)
        {
            coreStart[id].acquireUninterruptibly();

            if(lastTaskDone)
            {
                break;
            }

            //update task.alloted bursts and core
            currentTask.setBurst(bursts);
            currentTask.setCore(id);

            System.out.println("Core# " + id + " is running " + currentTask);

            taskStart[currentTask.getId()].release();

            taskFinished[currentTask.getId()].acquireUninterruptibly();

            dispatcherStart[id].release();
        }
    }

    public void setTask(Task currentTask)
    {
        this.currentTask = currentTask;
    }

    public void setBursts(int bursts)
    {
        this.bursts = bursts;
    }
}

}