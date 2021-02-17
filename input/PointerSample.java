public class PointerSample {
    private static int i = 0;

    public static void main(String[] args) {
        Object obj1 = new Object();
        Object obj2 = new String();
        Object obj3 = new Integer(0);

        if (i == 0) {
            obj1 = obj3;
        } else if (i == 1) {
            obj2 = obj3;
        }
        foo(obj1, obj2);
    }

    private static void foo(Object o1, Object o2) {
    }
}
