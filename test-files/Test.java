import java.util.Scanner;

public class Test {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a value: ");
        String line = scanner.nextLine();
        System.out.println("The first value is: " + line);
        System.out.println("Enter another value: ");
        line = scanner.nextLine();
        System.out.println("The second value is: " + line);
        scanner.close();
    }
}