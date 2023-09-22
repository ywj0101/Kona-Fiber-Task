import java.sql.ResultSet;
import java.util.concurrent.*;


public class SyncDatabaseDemo {
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



    public static void main(String[] args) throws Exception {
        threadCount = Integer.valueOf(args[0]);
//        threadCount = 1;
        requestCount = Integer.valueOf(args[1]);
//        requestCount = 10;
        // 1 for ForkJoinPool, 2 for FixedThreadPool
        testOption = Integer.valueOf(args[2]);
//        testOption = 1;
        run();
    }
}
