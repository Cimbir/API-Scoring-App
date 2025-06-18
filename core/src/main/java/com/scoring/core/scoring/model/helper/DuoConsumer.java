package com.scoring.core.scoring.model.helper;

@FunctionalInterface
public interface DuoConsumer<A, B> {
    void accept(A a, B b);
}
