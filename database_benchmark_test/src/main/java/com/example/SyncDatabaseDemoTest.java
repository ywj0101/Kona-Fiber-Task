package com.example;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.sql.ResultSet;
import java.util.concurrent.*;

@Fork(1)
@Warmup(iterations = 3, time = 20)
@Measurement(iterations = 5, time = 20)
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SyncDatabaseDemoTest {
    @Param({"1000","2000"})
    private int threadCountInput;
    @Param({"100000","200000","300000","400000","500000"})
    private int requestCountInput;
    @Param({"1","2"})
    private int testOptionInput;
    private static ExecutorService db_executor;
    private static ExecutorService e;

    private static int threadCount;
    private static int requestCount;

    private static int testOption;

    private static ExecutorService scheduler;

    public static String execQuery(String sql) throws InterruptedException, ExecutionException {
        String queryResult = "";
        try {
            ConnectionNode node;
            do {
                node = ConnectionPool.getConnection();
            } while (node == null);
            ResultSet rs = node.stm.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("id");
                String hello = rs.getString("hello");
                String response = rs.getString("response");

                queryResult += "id: " + id + " hello:" + hello + " response: "+ response + "\n";
            }

            rs.close();
            ConnectionPool.releaseConnection(node);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return queryResult;
    }

    public static String submitQuery(String sql) throws InterruptedException, ExecutionException {
        CompletableFuture<String> future = new CompletableFuture<>();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    // 任务完成
                    future.complete(execQuery(sql));
                } catch (Exception e) {

                }
            }
        };
        db_executor.execute(r);

        return future.get();
    }

    public static void testSyncQuery() throws Exception {
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(requestCount);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    startSignal.await();

                    submitQuery("select * from hello");

                    doneSignal.countDown();
                } catch (Exception e) {

                }
            }
        };

        for (int i = 0; i < requestCount; i++) {
            e.execute(r);
        }

        // wait for all task is committed
        startSignal.countDown();
        // wait for all task is finished
        doneSignal.await();

        e.shutdown();
        e.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        db_executor.shutdown();
        scheduler.shutdown();
    }

    public static void initExecutor() {
        ThreadFactory factory;

        if (testOption == 1) {
//            factory = Thread.ofVirtual().factory();
            scheduler = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        }else {
            scheduler = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        factory = Thread.ofVirtual().scheduler(scheduler).factory();

        e = Executors.newFixedThreadPool(threadCount, factory);

        // an independent thread pool which has 16 threads
        db_executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }

    public static void run() throws Exception {
        initExecutor();
        ConnectionPool.initConnectionPool();

        testSyncQuery();

        ConnectionPool.closeConnection();
    }

    @Benchmark
    public void test() throws Exception {
        threadCount = threadCountInput;
        requestCount = requestCountInput;
        testOption = testOptionInput;
        run();
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(SyncDatabaseDemoTest.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
