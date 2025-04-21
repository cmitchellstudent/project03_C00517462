import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;




public class Main {
    public static void main(String[] args) {
        Random rand = new Random();
        //intended args "-S <1/2/3/4> -C <1/2/3/4>" -C and num. of cores optional (defaults to 1)
        //OR "-C <1/2/3/4> -S <1/2/3/4>" is also accepted.
        int coreIndex = -1;
        int selectionIndex = -1;
        //args = new String[] {"-S", "4"};
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-S")){
                selectionIndex = i;
            }
            if (args[i].equals("-C")){
                coreIndex = i;
            }
            //detect any extra nonsense
            if (!args[i].equals("-S") && !args[i].equals("-C")) {
                try {
                    Integer.parseInt(args[i]);
                } catch (Exception e) {
                    System.out.println("Please format args as: -S <1/2/3/4> -C <1/2/3/4> or -C <1/2/3/4> -S <1/2/3/4>");
                    System.exit(0);
                }
            }
        }
        //Core assignment
        int cores;
        if(coreIndex != -1){
            switch (args[coreIndex+1]){
                case ("1") ->{
                    cores = 1;
                    System.out.println("1 Core");
                }
                case ("2") ->{
                    cores = 2;
                    System.out.println("2 Cores");
                }
                case ("3") ->{
                    cores = 3;
                    System.out.println("3 Cores");
                }
                case ("4") ->{
                    cores = 4;
                    System.out.println("4 Cores");
                }
                default -> {
                    System.out.println("Cores unrecognized or out of bounds, defaulting to 1.");
                    cores = 1;
                }
            }
        } else {
            System.out.println("Cores unspecified, defaulting to 1.");
            cores = 1;
        }
        //Selection Assignment
        if (selectionIndex != -1) {
            switch (args[selectionIndex+1]) {
                case ("1") -> {
                    //FCFS
                    System.out.println("Scheduler Algorithm Select: First Come First Served");
                    int t = rand.nextInt(1,26);
                    AtomicInteger completedTasks = new AtomicInteger(0);
                    Queue<Task> readyQueue = new LinkedList<Task>();
                    for (int i = 0; i < t; i++) {
                        Task newTask = new Task(rand.nextInt(1,51), i, completedTasks);
                        readyQueue.add(newTask);
                        System.out.println("Creating task thread " + i);
                        newTask.start();
                    }
                    printReadyQ(readyQueue);
                    for (int i = 0; i < cores; i++) {
                        Core newCore = new Core(i);
                        Dispatcher newDispatcher = new Dispatcher(i, readyQueue, newCore, "FCFS", t, cores, completedTasks);
                        System.out.println("Creating dispatcher thread " + i);
                        System.out.println("Dispatcher " + i + " | Using CPU " + i);
                        newCore.start();
                        newDispatcher.start();
                    }


                }
                case ("2") -> {
                    //RR. which has format -S 2 <quantum time> -C <cores>
                    System.out.println("Scheduler Algorithm Select: Round Robin");
                    int quantum;
                    try {
                        quantum = Integer.parseInt(args[selectionIndex+2]);
                    } catch (Exception e) {
                        System.out.println("Please select a time quantum in the range 2-10. Args: -S 2 <quantum> -C <1/2/3/4>");
                        System.exit(0);
                    }
                    quantum = Integer.parseInt(args[selectionIndex+2]);
                    if (quantum > 10 || quantum < 2) {
                        System.out.println("Please select a time quantum in the range 2-10.");
                        System.exit(0);
                    } else {
                        System.out.println("Quantum: " + quantum);
                        //Do algorithm.
                        int[] RRargs = {cores, quantum};
                        RoundRobin.main(RRargs);
                    }
                }
                case ("3") -> {
                    //NP - SJF
                    System.out.println("Scheduler Algorithm Select: Non-Preemptive Shortest Job First");
                }
                case "4" -> {
                    System.out.println("Scheduler Algorithm Select: Preemptive - Shortest Job First");
                    cores = 1;
                    int t = rand.nextInt(1, 26);
                    AtomicInteger completedTasks = new AtomicInteger(0);
                
                    // Prepare shared PSJF queue + lock + semaphores
                    Dispatcher.psjfQueue = new PriorityQueue<>(Comparator.comparingInt(task -> task.burstTime - task.progress));
                    Dispatcher.psjfLock  = new ReentrantLock(true);
                    Dispatcher.arrivals  = new Semaphore(0);
                    Dispatcher.dispatcherStart = new Semaphore(0); // REQUIRED for proper dispatcher signaling
                
                    // 1) Create all T tasks (they’ll wait on taskStart)
                    List<Task> allTasks = new ArrayList<>();
                    System.out.println("# threads = " + t);
                    for (int i = 0; i < t; i++) {
                        Task task = new Task(rand.nextInt(1, 51), i, completedTasks);
                        System.out.printf("Main thread     | Creating process thread %d%n", i);
                        task.start();
                        allTasks.add(task);
                    }
                
                    // 2) Simulate staggered arrivals
                    new Thread(() -> {
                        for (Task task : allTasks) {
                            try {
                                Thread.sleep(rand.nextInt(1, 6) * 100L);
                            } catch (InterruptedException ignored) {}
                
                            Dispatcher.psjfLock.lock();
                            try {
                                Dispatcher.psjfQueue.add(task);
                                System.out.printf("Task %d arrived; queue:%n", task.tID);
                                printReadyQ(Dispatcher.psjfQueue);
                            } finally {
                                Dispatcher.psjfLock.unlock();
                            }
                
                            Dispatcher.arrivals.release();         // Let dispatcher know task has arrived
                            Dispatcher.dispatcherStart.release();  // Unblock dispatcher for each arrival
                        }
                    }).start();
                    
                
                    // 3) Fork dispatcher + core
                    System.out.println("\nMain thread     | Forking dispatcher 0");
                    System.out.println("Dispatcher 0    | Using CPU 0");
                    System.out.println("Dispatcher 0    | Now releasing dispatchers.\n");
                
                    Core core = new Core(0);
                    Dispatcher dispatcher = new Dispatcher(
                        0,
                        Dispatcher.psjfQueue,
                        core,
                        "PSJF",
                        t,
                        cores,
                        completedTasks,
                        Dispatcher.psjfLock,
                        Dispatcher.arrivals
                    );
                    core.start();
                    dispatcher.start();
                    break;
                }

            }
        } else {
            System.out.println("Incorrect arguments. expected -S <1/2/3/4> -C <1/2/3/4>.");
        }
    }
    public static void printReadyQ(Queue<Task> rq){
        System.out.println("--------------- Ready Queue ---------------");
        for (Task t: rq) {
            System.out.println("ID: " + t.tID + ", Burst Time: " + t.burstTime + ", Progress: " + t.progress);
        }
        System.out.println("-------------------------------------------");
    }









    public static class Task extends Thread{

        int tID;
        boolean isFinished = false;
        static AtomicInteger completedTasks;
        //aka max burst
        int burstTime;
        int currentCoreID;

        //an expression of completed burst time, updates once a cycle
        int progress = 0;

        int allottedBurst;
        Semaphore taskStart = new Semaphore(0);
        Semaphore taskFinish = new Semaphore(0);


        public Task(int burstTime, int ID, AtomicInteger completedTasks){
            this.burstTime = burstTime;
            this.tID = ID;
            Task.completedTasks = completedTasks;
        }

        public Task(int id) {

        this.tID = id;
        //tasks burst times are generated at task creation
        Random random = new Random();
        this.burstTime = random.nextInt(1, 51);
        }

        @Override
        public void run() {
            while (progress < burstTime){
                taskStart.acquireUninterruptibly();
                System.out.println("Task Thread " + tID +
                        " | On CPU: Max Burst=" + burstTime +
                        ", Progress=" + progress + ", Allotted burst=" + allottedBurst);
                int loopCounter = 0;
                while (loopCounter < allottedBurst){
                    System.out.println("Task Thread " + tID + " | Using CPU " + currentCoreID + "; On burst " + (progress+1));

                    progress++;
                    loopCounter++;
                }
                taskFinish.release(1);
            }
            System.out.println("Task Thread " + tID + " | Completed");
            completedTasks.incrementAndGet();
        }


        public void CoreUpdate(int allottedBurst, int cID){
            this.allottedBurst = allottedBurst;
            this.currentCoreID = cID;
        }

        public String toString()
        {
            return "Task# " + tID;
        }

        public int getBurstTime()
        {
            return allottedBurst;
        }

        public boolean isComplete()
        {
            return isFinished;
        }

        public int getRemainingTime()
        {
            return burstTime - progress;
        }

        public int getID()
        {
            return tID;
        }
    }

    public static class Dispatcher extends Thread {
        int dID;
        int t;
        static AtomicInteger completedTasks;
        Core assignedCore;

        static String algoType;
        static Queue<Task> readyQueue;
        static ReentrantLock readyQLock = new ReentrantLock(true);

        static Semaphore dispatcherStart = new Semaphore(1);
        static Semaphore barrier = new Semaphore(0);
        static int barrierCount;
        static int numOfCores = 0;
        static ReentrantLock countMutex = new ReentrantLock();

         // [PSJF] New Priority Queue and Lock for PSJF
         static PriorityQueue<Task> psjfQueue = new PriorityQueue<>(Comparator.comparingInt(t -> t.burstTime - t.progress)); // [PSJF]
         static ReentrantLock psjfLock = new ReentrantLock(true); // [PSJF]
         static boolean isRunning = false; // [PSJF]
         static Task currentTask = null; // [PSJF]
         static Semaphore arrivals;
 



        public Dispatcher(int ID, Queue<Task> readyQueue, Core core, String algoType, int t, int numOfCores, AtomicInteger completedTasks){
            this.dID = ID;
            this.assignedCore = core;
            assignedCore.currentDispatcher = this;
            Dispatcher.algoType = algoType;
            Dispatcher.readyQueue = readyQueue;
            this.t = t;
            Dispatcher.numOfCores = numOfCores;
            Dispatcher.completedTasks = completedTasks;
        }
    
        public Dispatcher(int ID, Queue<Task> readyQueue, Core core, String algoType, int t, int numOfCores, AtomicInteger completedTasks, ReentrantLock psjfLock, Semaphore arrivals) {
        this(ID, readyQueue, core, algoType, t, numOfCores, completedTasks); // Call original constructor
        Dispatcher.psjfLock = psjfLock; // // <-- new line
        Dispatcher.arrivals = arrivals; }// // <-- new line

        @Override
        public void run() {
            countMutex.lock();
            barrierCount++;
            if (barrierCount == numOfCores){
                System.out.println("Dispatcher " + dID + " | Now releasing dispatchers.");
                barrier.release(numOfCores);
            }
            countMutex.unlock();
            barrier.acquireUninterruptibly();


            switch (algoType){
                case ("FCFS") -> {
                    System.out.println("Dispatcher " + dID + " | Running FCFS.");
                    do {
                        dispatcherStart.acquireUninterruptibly();

                        Task selectedTask;
                        readyQLock.lock();
                        try{
                            selectedTask = readyQueue.remove();
                        } catch (Exception e){
                            //Note: this catches when ready queue is empty.
                            //This is an end condition for FCFS but will produce different results for something like P-SJF
                            selectedTask = null;
                        }
                        readyQLock.unlock();

                        if (selectedTask == null){
                            break;
                        }
                        System.out.println("\nDispatcher " + dID + " | Running Task " + selectedTask.tID);
                        assignedCore.setCurrentTask(selectedTask, selectedTask.burstTime);
                        assignedCore.coreStart.release(1);
                    } while (completedTasks.get() != t);
                    countMutex.lock();
                    barrierCount--;
                    if (barrierCount == 0){
                        System.out.println("All tasks done, program exiting.");
                        System.exit(0);
                    }
                    countMutex.unlock();
                }
                case ("PSJF") -> {  // Preemptive Shortest Job First
                    System.out.println("Dispatcher " + dID + " | Running Preemptive Shortest Job First");
                
                    while (true) {
                        if (completedTasks.get() == t) break;  // Exit condition if all tasks are done
                
                        dispatcherStart.acquireUninterruptibly();
                        System.out.println("Dispatcher " + dID + " | Task selection started.");
                
                        arrivals.acquireUninterruptibly();  // Wait until a task arrives
                        System.out.println("Dispatcher " + dID + " | Task arrived.");
                
                        // Select the task safely with synchronization
                        psjfLock.lock();
                        Task selectedTask = null;
                        try {
                            // Poll the task with the shortest burst time from the queue
                            selectedTask = psjfQueue.poll();  // Select the task with the shortest remaining burst time
                            if (selectedTask != null) {
                                System.out.println("Selected task: " + selectedTask.tID);  // Debugging
                            }
                        } finally {
                            psjfLock.unlock();
                        }
                
                        if (selectedTask == null) {
                            System.out.println("No task available for execution. Continuing.");
                            continue;  // If no task is selected, continue
                        }
                
                        int remainingBurst = selectedTask.burstTime - selectedTask.progress;
                
                        // Process the selected task
                        if (remainingBurst > 0) {
                            System.out.printf("Dispatcher %d    | Running process %d (Progress: %d/%d)\n",
                                    dID, selectedTask.tID, selectedTask.progress, selectedTask.burstTime);
                
                            assignedCore.setCurrentTask(selectedTask, 1);  // Run 1 unit
                            assignedCore.coreStart.release();              // Start core
                          
                
                            // After running, check if task is finished or needs more time
                            if (selectedTask.progress < selectedTask.burstTime) {
                                // If the task is not finished, re-add it back to the queue
                                psjfLock.lock();
                                try {
                                    psjfQueue.add(selectedTask);  // Add the task back for future scheduling
                                    arrivals.release();  // Notify that a task is ready to be handled
                                    System.out.println("Re-added task: " + selectedTask.tID);  // Debugging

                                    
                                }finally {
                                    psjfLock.unlock();
                                }
                            } else {
                                // Task is complete, safely increment completedTasks
                                completedTasks.incrementAndGet();
                                System.out.printf("Dispatcher %d    | Task %d completed (%d/%d)\n", 
                                        dID, selectedTask.tID, completedTasks.get(), t);
                            }
                            
                        }
                    }
                
                    // Barrier logic to cleanly exit once all dispatchers are done
                    countMutex.lock();
                    try {
                        barrierCount--;
                        if (barrierCount == 0) {
                            System.out.println("All tasks done, program exiting.");
                            System.exit(0);
                        }
                    } finally {
                        countMutex.unlock();
                        
                    }
                    
                    
                }
                
                
            } 
        }
        
    }

    public static class Core extends Thread{

        int cID;
        Task currentTask;
        Dispatcher currentDispatcher;
        int allottedBurstTime;

        Semaphore coreStart = new Semaphore(0);


        public Core (int ID){
            this.cID = ID;
        }

        @Override
        public void run() {
            while (true){
                coreStart.acquireUninterruptibly();
                currentTask.CoreUpdate(allottedBurstTime, cID);
                currentTask.taskStart.release(1);
                currentTask.taskFinish.acquireUninterruptibly();
                Main.Dispatcher.dispatcherStart.release(1);
            }
        }

        public void setCurrentTask(Task currentTask, int allottedBurstTime) {
            this.currentTask = currentTask;
            this.allottedBurstTime = allottedBurstTime;
        }
    }

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
    private static RRDispatcher[] dispatchers;
    private static RRCore[] cores;
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
    //System.out.println("Quantum: " + quantum);

    //initializes object arrays
    dispatchers = new RRDispatcher[numCores];
    cores = new RRCore[numCores];
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
        System.out.println(task + " has been added to ready queue with " + task.getRemainingTime() + " bursts.");
    }

    printReadyQ(readyQueue);

    //sets atomic int for task completion
    AtomicInteger completedTasks = new AtomicInteger(0);
    Task.completedTasks = completedTasks;


    //populates sem and object arrays, starts threads
    for(int i = 0; i < numCores; i++)
    {
        dispatcherStart[i] = new Semaphore(0);
        RRDispatcher dispatcher = new RRDispatcher(i);
        dispatchers[i] = dispatcher;
        Thread d = new Thread(dispatcher);
        threads.add(d);
        d.start();

        coreStart[i] = new Semaphore(0);
        RRCore core = new RRCore(i);
        cores[i] = core;
        Thread c = new Thread(core);
        threads.add(c);
        c.start();

    }

    while(!(completedTasks.get() >= numTasks))
    {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("Error while waiting for last task!");
        }
    }

    lastTaskDone = true;

    for(int i = 0; i < numCores; i++)
    {
        coreStart[i].release();
        dispatcherStart[i].release();
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

private static class RRDispatcher implements Runnable
{
    private int id;
    private Task newTask = null;

    public RRDispatcher(int id) {
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
            if((!(newTask==null)) && newTask.getRemainingTime() > 0)
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


            //assign task and burst to core
            cores[id].setTask(newTask);
            cores[id].setBursts(Math.min(quantum, newTask.getRemainingTime()));
            System.out.println(newTask + " has " + Math.min(quantum, newTask.getRemainingTime()) + " bursts.");
            

            coreStart[id].release();
        }

    }
}

private static class RRCore implements Runnable
{
    private int id;
    private int bursts;
    private Task currentTask;


    public RRCore(int id) {
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

            //update task alloted bursts and core
            currentTask.CoreUpdate(bursts, id);

            System.out.println("Core# " + id + " is running " + currentTask);

            currentTask.taskStart.release(1);

            currentTask.taskFinish.acquireUninterruptibly();

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











/*
    public static class Task extends Thread{

        int tID;
        boolean isFinished = false;
        static AtomicInteger completedTasks;
        //aka max burst
        int burstTime;
        int currentCoreID;

        //an expression of completed burst time, updates once a cycle
        int progress = 0;

        int allottedBurst;
        Semaphore taskStart = new Semaphore(0);
        Semaphore taskFinish = new Semaphore(0);


        public Task(int burstTime, int ID, AtomicInteger completedTasks){
            this.burstTime = burstTime;
            this.tID = ID;
            Task.completedTasks = completedTasks;
        }

        public Task(int id) {

        this.tID = id;
        //tasks burst times are generated at task creation
        this.burstTime = random.nextInt(1, 51);
        }

        @Override
        public void run() {
            while (progress < burstTime){
                taskStart.acquireUninterruptibly();
                System.out.println("Task Thread " + tID +
                        " | On CPU: Max Burst=" + burstTime +
                        ", Progress=" + progress + ", Allotted burst=" + allottedBurst);
                int loopCounter = 0;
                while (loopCounter < allottedBurst){
                    System.out.println("Task Thread " + tID + " | Using CPU " + currentCoreID + "; On burst " + (progress+1));

                    progress++;
                    loopCounter++;
                }
                taskFinish.release(1);
            }
            System.out.println("Task Thread " + tID + " | Completed");
            completedTasks.incrementAndGet();
        }
        public void CoreUpdate(int allottedBurst, int cID){
            this.allottedBurst = allottedBurst;
            this.currentCoreID = cID;
        }

        public String toString()
        {
            return "Task# " + tID;
        }

        public int getBurstTime()
        {
            return allottedBurst;
        }

        public boolean isComplete()
        {
            return isFinished;
        }

        public int getRemainingTime()
        {
            return burstTime - progress;
        }

        public int getID()
        {
            return tID;
        }
    }
    */


}


    

    
}