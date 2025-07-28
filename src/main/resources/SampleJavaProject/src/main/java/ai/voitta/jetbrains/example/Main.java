package ai.voitta.jetbrains.example;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        User u = User.builder()
                .name("greg")
                .email("grisha@alum.mit.edu")
                .zip("94306")
                .build();
        String greeting = Utils.getGreeting(u);
        System.out.println(greeting);
        String weather = Utils.getWeather(u);
        System.out.println("The weather is nice, isn't it?");
        System.out.println(weather);

        doSomethingElse();
    }

    private static void doSomethingElse() {
        System.out.println("Doing something else...");

        Thread countingThread = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                System.out.println("Count: " + i);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            System.out.println("Counting thread finished!");
        });

        countingThread.start();
        try {
            countingThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Done with something else.");
    }
}