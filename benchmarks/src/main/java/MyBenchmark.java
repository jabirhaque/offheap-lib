package com.jabirhaque;

import org.openjdk.jmh.annotations.Benchmark;

public class MyBenchmark {

    @Benchmark
    public int testSomething() {
        int a = 1;
        int b = 2;
        return a + b;
    }
}