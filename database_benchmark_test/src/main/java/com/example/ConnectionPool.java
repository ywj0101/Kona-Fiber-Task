package com.example;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionPool {
    private static Lock lock = new ReentrantLock();

    private static ConnectionNode head = null;
    private static final int connectionCount = 16;

    public static ConnectionNode getConnection() {
        lock.lock();
        try {
            if (head != null) {
                ConnectionNode target = head;
                head = head.next;
                return target;
            }
        } finally {
            lock.unlock();
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return null;
    }

    public static void releaseConnection(ConnectionNode node) {
        addConnectionNode(node);
    }

    private static void addConnectionNode(ConnectionNode current) {
        lock.lock();
        try {
            current.next = head;
            head = current;
        } finally {
            lock.unlock();
        }
    }

    public static void initConnectionPool() {
        try {
            for (int i = 0; i < connectionCount; i++) {
                ConnectionNode current = new ConnectionNode();
                addConnectionNode(current);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeConnection() {
        try {
            for (int i = 0; i < connectionCount; i++) {
                ConnectionNode node = getConnection();
                if (node != null) {
                    try {
                        if (node.stm != null) {
                            node.stm.close();
                        }

                        if (node.con != null) {
                            node.con.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}