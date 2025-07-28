package ai.voitta.jetbrains.example;

public class Main {
    public static void main(String[] args) {
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
    }

}
