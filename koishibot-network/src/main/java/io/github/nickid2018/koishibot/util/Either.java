package io.github.nickid2018.koishibot.util;

public record Either<A, B>(A left, B right) {

    public static <A, B> Either<A, B> left(A left) {
        return new Either<>(left, null);
    }

    public static <A, B> Either<A, B> right(B right) {
        return new Either<>(null, right);
    }

    public boolean isLeft() {
        return left != null;
    }

    public boolean isRight() {
        return right != null;
    }
}
