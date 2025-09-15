public class Test {
    public static void main(String[] args) {
        int a = 5;
        int b = 10;
        int c = add(a, b);
        System.out.println("Result: " + c);
    }

    public static int add(int x, int y) {
        int sum = x + y;
        return sum;
    }
}
