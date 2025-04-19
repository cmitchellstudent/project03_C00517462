
public class Main {
    public static void main(String[] args) {
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
                        int[] RRargs = {cores, quantum};
                        RoundRobin.main(RRargs);
                    }
                }
                case ("3") -> {
                    //NP - SJF
                    System.out.println("Scheduler Algorithm Select: Non-Preemptive Shortest Job First");
                }
                case ("4") -> {
                    //P - SJF
                    System.out.println("Scheduler Algorithm Select: Preemptive Shortest Job First");
                }

            }
        } else {
            System.out.println("Incorrect arguments. expected -S <1/2/3/4> -C <cores>.");
        }
    }

    public static class Task {

    }

    public static class Dispatcher {

    }

    public static class Core {

    }
}