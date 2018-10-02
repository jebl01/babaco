package io.github.jebl01.babaco.matching;

import static io.github.jebl01.babaco.matching.MatchingFunction._case;
import static junit.framework.TestCase.assertEquals;

import io.github.jebl01.babaco.Either;
import io.github.jebl01.babaco.Tuple;

import java.io.IOException;
import java.util.function.Function;

import org.junit.Test;

public class MatchingFunctionTest {
    private Function<Throwable, String> getExceptionConsumerWithDefault() {
        return MatchingFunction.match_(
                MatchingFunction._case(IOException.class, e -> "io"),
                MatchingFunction._case(RuntimeException.class, e -> "runtime"),
                MatchingFunction._case(IllegalArgumentException.class, e -> "illegal"),
                MatchingFunction._default(e -> "default: " + e.getClass().getSimpleName()));
    }

    private Function<Throwable, String> getExceptionConsumerNoDefault() {
        return MatchingFunction.match_(
                MatchingFunction._case(IOException.class, e -> "io"),
                MatchingFunction._case(RuntimeException.class, e -> "runtime"),
                MatchingFunction._case(IllegalArgumentException.class, e -> "illegal"));
    }

    @Test
    public void willInvokeDefaultIfNoMatch() {
        assertEquals("default: IllegalAccessError", getExceptionConsumerWithDefault().apply(new IllegalAccessError()));
    }

    @Test
    public void willMatchExact() {
        assertEquals("io", getExceptionConsumerWithDefault().apply(new IOException()));
    }

    @Test
    public void willMatchOnSuperType() {
        assertEquals("runtime", getExceptionConsumerWithDefault().apply(new IllegalArgumentException()));
        assertEquals("runtime", getExceptionConsumerWithDefault().apply(new ClassCastException()));
    }

    @Test(expected = MatchingFunction.MatchingException.class)
    public void willThrowExceptionIfNoMatch() {
        assertEquals("io", getExceptionConsumerNoDefault().apply(new IllegalAccessError()));
    }

    @Test
    public void canMatchUsingOnlyPredicate() {
        final Function<Tuple.Tuple2<String, String>, String> f = MatchingFunction.match_(
                MatchingFunction._case(s -> s.v1.startsWith("a"), s -> s.v1 + s.v2 + ":first"),
                MatchingFunction._case(s -> s.v1.startsWith("b"), s -> s.v1 + s.v2 + ":second"),
                MatchingFunction._case(s -> s.v1.startsWith("c"), s -> s.v1 + s.v2 + ":third"),
                MatchingFunction._default(s -> s.v1 + s.v2 + ":default")
        );
        assertEquals("a_test:first", f.apply(Tuple.of("a_", "test")));
        assertEquals("b_test:second", f.apply(Tuple.of("b_", "test")));
        assertEquals("c_test:third", f.apply(Tuple.of("c_", "test")));
        assertEquals("no_match:default", f.apply(Tuple.of("no_", "match")));
    }

    @Test
    public void canMatchUsingPredicate() {
        final Function<String, String> f = MatchingFunction.match_(
                MatchingFunction._case(String.class, s -> s.startsWith("a"), s -> s + ":first"),
                MatchingFunction._case(String.class, s -> s.startsWith("b"), s -> s + ":second"),
                MatchingFunction._case(String.class, s -> s.startsWith("c"), s -> s + ":third"),
                MatchingFunction._default(s -> s + ":default")
        );
        assertEquals("a_test:first", f.apply("a_test"));
        assertEquals("b_test:second", f.apply("b_test"));
        assertEquals("c_test:third", f.apply("c_test"));
        assertEquals("no_match:default", f.apply("no_match"));
    }

    @Test
    public void canMatchNumberUsingPredicate() {
        final Function<Number, String> matchingFunction = MatchingFunction.match_(
                MatchingFunction._case(Integer.class, i -> i == 1, i -> i + ":first_int"),
                MatchingFunction._case(Integer.class, i -> i == 2, i -> i + ":second_int"),
                MatchingFunction._case(Long.class, l -> l == 1, l -> l + ":first_long"),
                MatchingFunction._case(Long.class, l -> l == 2, l -> l + ":second_long"),
                MatchingFunction._case(Float.class, f -> f.compareTo(1.1f) == 0, f -> f + ":first_float"),
                MatchingFunction._case(Float.class, f -> f.compareTo(2.2f) == 0, l -> l + ":second_float"),
                MatchingFunction._case(Double.class, d -> d.compareTo(1.1d) == 0, d -> d + ":first_double"),
                MatchingFunction._case(Double.class, d -> d.compareTo(2.2d) == 0, d -> d + ":second_double"),
                MatchingFunction._default(n -> n + ":default")
        );
        assertEquals("1:first_int", matchingFunction.apply(1));
        assertEquals("2:second_int", matchingFunction.apply(2));
        assertEquals("1:first_long", matchingFunction.apply(1L));
        assertEquals("2:second_long", matchingFunction.apply(2L));
        assertEquals("1.1:first_float", matchingFunction.apply(1.1f));
        assertEquals("2.2:second_float", matchingFunction.apply(2.2f));
        assertEquals("1.1:first_double", matchingFunction.apply(1.1d));
        assertEquals("2.2:second_double", matchingFunction.apply(2.2d));
        assertEquals("1:default", matchingFunction.apply((short) 1));
    }

    @Test
    public void canMatchOnObject() {
        final Function<Object, String> f = MatchingFunction.match_(
                MatchingFunction._case(String.class, s -> "string with value: " + s),
                MatchingFunction._case(Integer.class, i -> "int with value: " + i),
                MatchingFunction._case(Exception.class, e -> "exception with message: " + e.getMessage()),
                MatchingFunction._default(s -> "unknown")
        );
        assertEquals("string with value: test", f.apply("test"));
        assertEquals("int with value: 42", f.apply(42));
        assertEquals("exception with message: hello", f.apply(new IOException("hello")));
        assertEquals("unknown", f.apply(Either.left("test")));
    }

    @Test
    public void canMatchEitherOnType() {
        Either<String, String> rightStringA = Either.right("A");
        Either<String, String> leftStringA = Either.left("A");
        Either<String, Integer> rightInt1 = Either.right(1);
        Either<Integer, Integer> leftInt1 = Either.left(1);
        final Function<Either, String> f = MatchingFunction.match_(
                MatchingFunction._case(Either.right(String.class), s -> "right string " + s),
                MatchingFunction._case(Either.left(String.class), s -> "left string " + s),
                MatchingFunction._case(Either.right(Integer.class), i -> "right int " + i),
                MatchingFunction._case(Either.left(Integer.class), i -> "left int " + i),
                MatchingFunction._default(s -> s + "no match")
        );
        assertEquals("right string A", f.apply(rightStringA));
        assertEquals("left string A", f.apply(leftStringA));
        assertEquals("right int 1", f.apply(rightInt1));
        assertEquals("left int 1", f.apply(leftInt1));
    }

    @Test
    public void canMatchEitherWithPredicate() {
        Either<String, String> rightStringA = Either.right("A");
        Either<String, String> rightStringB = Either.right("B");
        Either<String, String> leftStringA = Either.left("A");
        Either<String, String> leftStringB = Either.left("B");
        Either<String, Integer> rightInt1 = Either.right(1);
        Either<String, Integer> rightInt2 = Either.right(2);
        Either<Integer, Integer> leftInt1 = Either.left(1);
        Either<Integer, Integer> leftInt2 = Either.left(2);
        final Function<Either, String> f = MatchingFunction.match_(
                MatchingFunction._case(Either.right(String.class), "A"::equals, s -> "right string A"),
                MatchingFunction._case(Either.right(String.class), "B"::equals, s -> "right string B"),
                MatchingFunction._case(Either.left(String.class), "A"::equals, s -> "left string A"),
                MatchingFunction._case(Either.left(String.class), "B"::equals, s -> "left string B"),
                MatchingFunction._case(Either.right(Integer.class), i -> i == 1, i -> "right int 1"),
                MatchingFunction._case(Either.right(Integer.class), i -> i == 2, i -> "right int 2"),
                MatchingFunction._case(Either.left(Integer.class), i -> i == 1, i -> "left int 1"),
                MatchingFunction._case(Either.left(Integer.class), i -> i == 2, i -> "left int 2"),
                MatchingFunction._default(s -> s + "no match")
        );
        assertEquals("right string A", f.apply(rightStringA));
        assertEquals("right string B", f.apply(rightStringB));
        assertEquals("left string A", f.apply(leftStringA));
        assertEquals("left string B", f.apply(leftStringB));
        assertEquals("right int 1", f.apply(rightInt1));
        assertEquals("right int 2", f.apply(rightInt2));
        assertEquals("left int 1", f.apply(leftInt1));
        assertEquals("left int 2", f.apply(leftInt2));
    }
}