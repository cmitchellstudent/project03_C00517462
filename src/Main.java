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
                    }
                }
                case ("3") -> {
                    //NP - SJF
                    System.out.println("Scheduler Algorithm Select: Non-Preemptive Shortest Job First");
                }
                case ("4") -> {
                    //P - SJF. only needs 1 core implementation
                    System.out.println("Scheduler Algorithm Select: Preemptive Shortest Job First");
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
    }

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
                    } while (completedTasks.get() != t);
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
}