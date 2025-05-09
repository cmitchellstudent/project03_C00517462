import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;




public class Main {
//begin code changes by Connor Mitchell C00517462
    public static void main(String[] args) {
        Random rand = new Random();
        //intended args "-S <1/2/3/4> -C <1/2/3/4>" -C and num. of cores optional (defaults to 1)
        //OR "-C <1/2/3/4> -S <1/2/3/4>" is also accepted.
        int coreIndex = -1;
        int selectionIndex = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-S")) {
                selectionIndex = i;
            }
            if (args[i].equals("-C")) {
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
        if (coreIndex != -1) {
            switch (args[coreIndex + 1]) {
                case ("1") -> {
                    cores = 1;
                    System.out.println("1 Core");
                }
                case ("2") -> {
                    cores = 2;
                    System.out.println("2 Cores");
                }
                case ("3") -> {
                    cores = 3;
                    System.out.println("3 Cores");
                }
                case ("4") -> {
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
            switch (args[selectionIndex + 1]) {
                case ("1") -> {
                    //FCFS
                    System.out.println("Scheduler Algorithm Select: First Come First Served");
                    int t = rand.nextInt(1, 26);
                    AtomicInteger completedTasks = new AtomicInteger(0);
                    Queue<Task> readyQueue = new LinkedList<Task>();
                    for (int i = 0; i < t; i++) {
                        Task newTask = new Task(rand.nextInt(1, 51), i, completedTasks);
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

//end code changes by Connor Mitchell C00517462
//begin code changes by Martin Cook C00102798
                }
                case ("2") -> {
                    //RR. which has format -S 2 <quantum time> -C <cores>
                    System.out.println("Scheduler Algorithm Select: Round Robin");
                    int quantum;
                    try {
                        quantum = Integer.parseInt(args[selectionIndex + 2]);
                    } catch (Exception e) {
                        System.out.println("Please select a time quantum in the range 2-10. Args: -S 2 <quantum> -C <1/2/3/4>");
                        System.exit(0);
                    }
                    quantum = Integer.parseInt(args[selectionIndex + 2]);
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
//end code changes by Martin Cook C00102798
//begin code changes by Jennifer Medina C00462454
                case ("3") -> {
                    //NP - SJF
                    System.out.println("Scheduler Algorithm Select: Non-Preemptive Shortest Job First");

                    int t = rand.nextInt(1, 26);
                    AtomicInteger completedTasks = new AtomicInteger(0);
                    List<Task> rqDisplay = new ArrayList<>(); // used to print tasks in order they arrive
                    // As tasks are added, sort by ascending burst times
                    Queue<Task> readyQueue = new PriorityQueue<>(Comparator.comparingInt(task -> task.burstTime));
                    for (int i = 0; i < t; i++) {
                        Task newTask = new Task(rand.nextInt(1, 51), i, completedTasks);
                        rqDisplay.add(newTask);
                        readyQueue.add(newTask);
                        System.out.println("Creating task thread " + i);
                        newTask.start();
                    }

                    System.out.println("--------------- Ready Queue ---------------");
                    for (Task task : rqDisplay) {
                        System.out.println("ID: " + task.tID + ", Burst Time: " + task.burstTime + ", Progress: " + task.progress);
                    }
                    System.out.println("-------------------------------------------");

                    for (int i = 0; i < cores; i++) {
                        Core newCore = new Core(i);
                        Dispatcher newDispatcher = new Dispatcher(i, readyQueue, newCore, "NP-SJF", t, cores, completedTasks);
                        System.out.println("Creating dispatcher thread " + i);
                        System.out.println("Dispatcher " + i + " | Using CPU " + i);
                        newCore.start();
                        newDispatcher.start();
                    }
                }
//end code changes by Jennifer Medina C00462454
//begin code changes by Taylor Comeaux C0028877
                case "4" -> {
                    System.out.println("Scheduler Algorithm Select: Preemptive - Shortest Job First");
                    int t = rand.nextInt(1, 26);  // number of tasks
                    AtomicInteger completed = new AtomicInteger(0);

                    // 1) Prepare queue, lock, semaphores
                    PriorityQueue<PSJFTask> queue =
                            new PriorityQueue<>(Comparator.comparingInt(PSJFTask::getRemainingTime));
                    ReentrantLock lock = new ReentrantLock(true);
                    Semaphore arrivals = new Semaphore(0);
                    Semaphore coreDone = new Semaphore(0);

                    // 2) Create and start tasks
                    List<PSJFTask> tasks = new ArrayList<>();
                    System.out.println("# threads = " + t);
                    for (int i = 0; i < t; i++) {
                        int burst = rand.nextInt(1, 51);
                        PSJFTask task = new PSJFTask(burst, i, completed);
                        System.out.printf("Main thread | Creating PSJFTask %d (burst=%d)%n", i, burst);
                        task.start();
                        tasks.add(task);
                    }

                    // 3) Staggered arrivals
                    new Thread(() -> {
                        for (PSJFTask task : tasks) {
                            try {
                                Thread.sleep(rand.nextInt(1, 2));
                            } catch (InterruptedException ignored) {
                            }
                            lock.lock();
                            try {
                                queue.add(task);
                                System.out.printf("Task %d arrived; queue:\n", task.getID());
                                printPSJFReadyQueue(queue);
                            } finally {
                                lock.unlock();
                            }
                            arrivals.release();
                        }
                    }).start();

                    // 4) Start core & dispatcher
                    System.out.println("\nMain thread     | Forking PSJF dispatcher and core");
                    System.out.println("Dispatcher 0    | Using CPU 0");
                    System.out.println("Dispatcher 0    | Now releasing dispatchers.\n");

                    PSJFCore coreThread = new PSJFCore(0, coreDone);
                    PSJFDispatcher disp = new PSJFDispatcher(
                            queue, lock, arrivals, coreDone,
                            coreThread, completed, t, 0
                    );

                    coreThread.start();
                    disp.start();
                    break;
                }

            }
        } else {
            System.out.println("Incorrect arguments. expected -S <1/2/3/4> -C <1/2/3/4>.");
        }
    }
//end code changes by Taylor Comeaux C0028877
//begin code changes by Connor Mitchell C00517462
    public static void printReadyQ(Queue<Task> rq){
        System.out.println("--------------- Ready Queue ---------------");
        for (Task t: rq) {
            System.out.println("ID: " + t.tID + ", Burst Time: " + t.burstTime + ", Progress: " + t.progress);
        }
        System.out.println("-------------------------------------------");
    }
//end code changes by Connor Mitchell C00517462
//begin code changes by Taylor Comeaux C0028877
    static void printPSJFReadyQueue(PriorityQueue<PSJFTask> queue) {
        System.out.println("----------- PSJF Ready Queue -----------");
        for (PSJFTask task : queue) {
            System.out.printf("ID: %d, Burst Time: %d, Remaining: %d\n",
                task.getID(), task.burstTime, task.getRemainingTime());
                
        }
        System.out.println("----------------------------------------");
    }
//end code changes by Taylor Comeaux C0028877





//begin code changes by Connor Mitchell C00517462
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
//end code changes by Connor Mitchell C00517462
//begin code changes by Martin Cook C00102798
        public Task(int id) {
        this.tID = id;
        //tasks burst times are generated at task creation
        Random random = new Random();
        this.burstTime = random.nextInt(1, 51);
        }
//end code changes by Martin Cook C00102798
//begin code changes by Connor Mitchell C00517462
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
//end code changes by Connor Mitchell C00517462
//begin code changes by Martin Cook C00102798
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
//end code changes by Martin Cook C00102798
//begin code changes by Connor Mitchell C00517462
    public static class Dispatcher extends Thread {
        int dID;
        int t;
        static AtomicInteger completedTasks;
        Core assignedCore;

        static String algoType;
        static Queue<Task> readyQueue;
        static ReentrantLock readyQLock = new ReentrantLock(true);

        Semaphore dispatcherStart = new Semaphore(1);
        static Semaphore barrier = new Semaphore(0);
        static int barrierCount;
        static int numOfCores = 0;
        static ReentrantLock countMutex = new ReentrantLock();

        public Dispatcher(int ID, Queue<Task> readyQueue, Core core, String algoType, int t, int numOfCores, AtomicInteger completedTasks){
            this.dID = ID;
            this.assignedCore = core;
            assignedCore.currentDispatcher = this;
            Dispatcher.algoType = algoType;
            Dispatcher.readyQueue = readyQueue;
            this.t = t;
            Dispatcher.numOfCores = numOfCores;
            Dispatcher.completedTasks = completedTasks;
            //Dispatcher.dispatcherStart = new Semaphore(numOfCores);
        }

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
                    } while (true);
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
//end code changes by Connor Mitchell C00517462
//begin code changes by Jennifer Medina C00462454
                case("NP-SJF") -> {
                    System.out.println("Dispatcher " + dID + " | Running NP-SJF.");
                    while (completedTasks.get() != t) {
                        dispatcherStart.acquireUninterruptibly();

                        readyQLock.lock(); // only allow this dispatcher to update queue

                        Task currentTask;
                        try {
                            // remove task with the shortest burst time
                            currentTask = readyQueue.remove();
                        } catch (Exception e) {
                            currentTask = null;
                        }

                        readyQLock.unlock();

                        if (currentTask == null) { // no more items left in queue
                            break;
                        }

                        System.out.println("\nDispatcher " + dID + " | Running Task " + currentTask.tID);
                        assignedCore.setCurrentTask(currentTask, currentTask.burstTime);
                        assignedCore.coreStart.release(1);
                    }

                    // barrier to end program when all tasks are completed
                    countMutex.lock();
                    barrierCount--;
                    if (barrierCount == 0){
                        System.out.println("All tasks done, program exiting.");
                        System.exit(0);
                    }
                    countMutex.unlock();
                }
            }
        }
    }
//end code changes by Jennifer Medina C00462454
//begin code changes by Connor Mitchell C00517462
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
                currentDispatcher.dispatcherStart.release(1);
            }
        }

        public void setCurrentTask(Task currentTask, int allottedBurstTime) {
            this.currentTask = currentTask;
            this.allottedBurstTime = allottedBurstTime;
        }
    }
//end code changes by Connor Mitchell C00517462
//begin code changes by Taylor Comeaux C0028877
    public static class PSJFTask extends Thread {
        private final int tID;
        private final int burstTime;
        private int progress = 0;
        private boolean isFinished = false;
        private static AtomicInteger completedCounter;
        private final Semaphore taskStart  = new Semaphore(0);
        private final Semaphore taskFinish = new Semaphore(0);
    
        public PSJFTask(int burstTime, int tID, AtomicInteger completedCounter) {
            this.burstTime = burstTime;
            this.tID       = tID;
            PSJFTask.completedCounter = completedCounter;
        }
    
        public int  getID()            { return tID; }
        public int  getRemainingTime() { return burstTime - progress; }
        public boolean isFinished()    { return isFinished; }
        public Semaphore getTaskStart()  { return taskStart; }
        public Semaphore getTaskFinish() { return taskFinish; }
    
        // called by core each slice to tell us which core is running 
        private int currentCoreID;
        public void coreUpdate(int coreID) {
            this.currentCoreID = coreID;
        }
    
        @Override
        public void run() {
            while (true) {
                // wait 
                taskStart.acquireUninterruptibly();
    
                // if done, break out
                if (progress >= burstTime) {
                    break;
                }
    
                // do exactly one unit of work
                progress++;
                System.out.printf(
                    "Task %d | Core %d working burst %d/%d%n",
                    tID, currentCoreID, progress, burstTime
                );
    
                // signal
                taskFinish.release();
    
                if (progress >= burstTime) {
                    completedCounter.incrementAndGet();
                    isFinished = true;
                    System.out.printf("Task %d | Completed%n", tID);
                    // leave the loop on next wake
                }
            }
        }}

    public static class PSJFCore extends Thread {
        private final int cID;
        private final Semaphore coreStart;       // dispatcher-core
        private final Semaphore dispatcherDone;  // core-dispatcher
        private PSJFTask currentTask;

        public PSJFCore(int id, Semaphore dispatcherDone) {
            this.cID            = id;
            this.dispatcherDone = dispatcherDone;
            this.coreStart      = new Semaphore(0);
        }

        public void assignTask(PSJFTask task, int i) {
            this.currentTask = task;
        }

        public Semaphore getCoreStart() {
            return coreStart;
        }

        @Override
        public void run() {
            while (true) {
                coreStart.acquireUninterruptibly();    // wait for dispatcher
                currentTask.coreUpdate(cID);
                currentTask.getTaskStart().release();  // run exactly 1 unit
                currentTask.getTaskFinish().acquireUninterruptibly();
                dispatcherDone.release();              // back to dispatcher
            }
        }
    }
    public static class PSJFDispatcher extends Thread {
        private final PriorityQueue<PSJFTask> queue;
        private final ReentrantLock lock;
        private final Semaphore arrivals;
        private final Semaphore coreDone;
        private final PSJFCore core;
        private final AtomicInteger completed;
        private final int totalTasks;
        private final int coreID;

        public PSJFDispatcher(
            PriorityQueue<PSJFTask> queue,
            ReentrantLock lock,
            Semaphore arrivals,
            Semaphore coreDone,
            PSJFCore core,
            AtomicInteger completed,
            int totalTasks,
            int coreID
        ) {
            this.queue = queue;
            this.lock = lock;
            this.arrivals = arrivals;
            this.coreDone = coreDone;
            this.core = core;
            this.completed = completed;
            this.totalTasks = totalTasks;
            this.coreID = coreID;
        }

        @Override
            public void run() {
                while (completed.get() < totalTasks) {
                    // 1) Wait until at least one arrival or re-queue
                    arrivals.acquireUninterruptibly();

                    PSJFTask task;
                    lock.lock();
                    try {
                        task = queue.poll();        // shortest-remaining first
                    } finally {
                        lock.unlock();
                    }
                    if (task == null || task.isFinished() || task.progress == task.burstTime) continue;

                    System.out.printf(
                      "PSJFDispatcher | Dispatching task %d (rem %d)%n",
                      task.getID(), task.getRemainingTime()
                    );

                    // 2) **Slice size = 1**, not the whole remaining time
                    core.assignTask(task, 1);
                    core.getCoreStart().release();

                    // 3) Wait for  one unit to finish
                    coreDone.acquireUninterruptibly();

                    // 4) If not done, re-queue; otherwise count it as complete
                    if (!task.isFinished()) {
                        lock.lock();
                        try {
                            queue.add(task);
                        } finally {
                            lock.unlock();
                        }
                        arrivals.release();
                    } else {
                        System.out.printf(
                          "PSJFDispatcher | Task %d done (%d/%d)%n",
                          task.getID(), completed.get(), totalTasks
                        );
                    }
                }
                System.out.println("PSJFDispatcher | All tasks completed");
                System.exit(0);
            }
        }

//end code changes by Taylor Comeaux C0028877
//begin code changes by Martin Cook C00102798
    public class RoundRobin {

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
//end code changes by Martin Cook C00102798
    }
}