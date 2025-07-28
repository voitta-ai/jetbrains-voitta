public class TestDebugFeatures {
    
    public static void main(String[] args) {
        TestDebugFeatures test = new TestDebugFeatures();
        int result = test.calculateSum(5, 10);
        System.out.println("Result: " + result);
        test.demonstrateLoop();
    }
    
    public int calculateSum(int a, int b) {
        int temp = a + b;  // First executable line
        if (temp > 10) {   // Decision point
            temp *= 2;
        }
        return temp;       // Last executable line
    }
    
    public void demonstrateLoop() {
        for (int i = 0; i < 3; i++) {  // Loop start
            System.out.println("Iteration: " + i);
            if (i == 1) {
                continue;  // Continue statement
            }
        }
        System.out.println("Loop completed");  // Last executable line
    }
}
