/**
 * Test class to demonstrate the enhanced debugging features
 * with rich variable inspection and full stack trace support
 */
public class TestEnhancedDebugFeatures {
    
    private String instanceField = "Hello World";
    private int counter = 0;
    private UserData userData;
    
    public static void main(String[] args) {
        TestEnhancedDebugFeatures test = new TestEnhancedDebugFeatures();
        test.userData = new UserData("John Doe", 30);
        
        // Set breakpoints on the following lines to test enhanced debugging
        test.demonstrateVariableInspection();
        test.demonstrateMethodCalls();
        test.demonstrateObjectHierarchy();
    }
    
    public void demonstrateVariableInspection() {
        String localVariable = "Local Value";
        int localNumber = 42;
        boolean localFlag = true;
        
        // Test point 1: Local variables inspection
        processData(localVariable, localNumber); // Set breakpoint here
    }
    
    public void processData(String input, int value) {
        String processed = input.toUpperCase();
        int doubled = value * 2;
        
        // Test point 2: Method parameters and local processing
        validateAndStore(processed, doubled); // Set breakpoint here
    }
    
    public void validateAndStore(String data, int number) {
        if (data != null && number > 0) {
            counter++;
            instanceField = data + "_" + number;
            
            // Test point 3: Instance field modification
            System.out.println("Stored: " + instanceField); // Set breakpoint here
        }
    }
    
    public void demonstrateMethodCalls() {
        CalculationHelper helper = new CalculationHelper();
        int result = helper.calculate(10, 20);
        
        // Test point 4: Method call results
        System.out.println("Calculation result: " + result); // Set breakpoint here
    }
    
    public void demonstrateObjectHierarchy() {
        userData.updateAge(31);
        String info = userData.getFormattedInfo();
        
        // Test point 5: Object field access and method calls
        System.out.println("User info: " + info); // Set breakpoint here
    }
    
    // Helper classes for testing object inspection
    static class UserData {
        private String name;
        private int age;
        private Address address;
        
        public UserData(String name, int age) {
            this.name = name;
            this.age = age;
            this.address = new Address("123 Main St", "Anytown");
        }
        
        public void updateAge(int newAge) {
            this.age = newAge;
        }
        
        public String getFormattedInfo() {
            return name + " (" + age + ") - " + address.getFullAddress();
        }
    }
    
    static class Address {
        private String street;
        private String city;
        
        public Address(String street, String city) {
            this.street = street;
            this.city = city;
        }
        
        public String getFullAddress() {
            return street + ", " + city;
        }
    }
    
    static class CalculationHelper {
        public int calculate(int a, int b) {
            int intermediate = a + b;
            int result = intermediate * 2;
            return result;
        }
    }
}
