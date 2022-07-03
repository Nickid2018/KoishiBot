package io.github.nickid2018.koishibot.util.value;

public class Either<A, B> {

    private final A left;
    private final B right;

    private Either(A left, B right) {
        this.left = left;
        this.right = right;
    }

    public static <A, B> Either<A, B> left(A left) {
        return new Either<>(left, null);
    }

    public static <A, B> Either<A, B> right(B right) {
        return new Either<>(null, right);
    }

    public A getLeft() {
        return left;
    }

    public B getRight() {
        return right;
    }

    public boolean isLeft() {
        return left != null;
    }

    public boolean isRight() {
        return right != null;
    }
}
