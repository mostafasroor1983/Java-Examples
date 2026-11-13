package module05.result;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Result<T, E>: a value or a typed error, no exceptions for expected failures
 * (insufficient funds, invalid IBAN, rate limited). Sealed so switches are exhaustive.
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

    record Ok<T, E>(T value) implements Result<T, E> {
        public Ok {
            Objects.requireNonNull(value, "value");
        }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        public Err {
            Objects.requireNonNull(error, "error");
        }
    }

    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    /** Lift an exception-throwing computation into a Result. */
    static <T> Result<T, Exception> attempt(Supplier<? extends T> body) {
        try {
            return ok(body.get());
        } catch (Exception e) {
            return err(e);
        }
    }

    default boolean isOk() {
        return this instanceof Ok;
    }

    default boolean isErr() {
        return this instanceof Err;
    }

    default <U> Result<U, E> map(Function<? super T, ? extends U> fn) {
        if (this instanceof Ok<T, E> ok) return ok(fn.apply(ok.value()));
        return err(((Err<T, E>) this).error());
    }

    default <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> fn) {
        if (this instanceof Ok<T, E> ok) return fn.apply(ok.value());
        return err(((Err<T, E>) this).error());
    }

    default <F> Result<T, F> mapErr(Function<? super E, ? extends F> fn) {
        if (this instanceof Err<T, E> e) return err(fn.apply(e.error()));
        return ok(((Ok<T, E>) this).value());
    }

    default T orElse(T fallback) {
        return this instanceof Ok<T, E> ok ? ok.value() : fallback;
    }

    default T orElseGet(Function<? super E, ? extends T> recover) {
        return this instanceof Ok<T, E> ok ? ok.value() : recover.apply(((Err<T, E>) this).error());
    }

    default T orElseThrow() {
        if (this instanceof Ok<T, E> ok) return ok.value();
        throw new NoSuchElementException("Result is Err: " + ((Err<T, E>) this).error());
    }

    default Optional<T> toOptional() {
        return this instanceof Ok<T, E> ok ? Optional.of(ok.value()) : Optional.empty();
    }

    default Result<T, E> ifOk(Consumer<? super T> c) {
        if (this instanceof Ok<T, E> ok) c.accept(ok.value());
        return this;
    }

    default Result<T, E> ifErr(Consumer<? super E> c) {
        if (this instanceof Err<T, E> e) c.accept(e.error());
        return this;
    }
}
