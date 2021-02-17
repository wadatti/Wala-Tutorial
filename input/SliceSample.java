public class SliceSample {
    public static void main(String[] args) {
        int x = func1(1);
        int y = func2(2);
        System.out.println(x + y);
    }

    static int func1(int i) {
        return i + 1;
    }

    static int func2(int i) {
        return i + 2;
    }
}
