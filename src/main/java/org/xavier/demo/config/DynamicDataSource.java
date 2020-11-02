package org.xavier.demo.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;

import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class DynamicDataSource extends DruidDataSource {
    private DateBaseProperties dateBaseProperties;
    public final AtomicReference<DruidDataSource> dataSourceAtomicReference = new AtomicReference<DruidDataSource>();
    // 接收引用对象被回收通知
    private final ReferenceQueue<Reference<?>> noticeQueue = new ReferenceQueue();

    public DynamicDataSource(DateBaseProperties dateBaseProperties) throws SQLException {
        watch();
        DruidDataSource druidDataSource = createDataSource(dateBaseProperties);
        // 弱引用
        WeakReference<Integer> weakReference = new WeakReference(druidDataSource, noticeQueue);
        this.dataSourceAtomicReference.set(druidDataSource);
        this.dateBaseProperties = dateBaseProperties;
    }

    public void watch() {
        // 这种写法比较简便，方便演示(本质上额外起一个子线程，去异步监控 noticeQueue 的值)
        CompletableFuture.runAsync(() -> {
            // 有多少引用使用了该队列，就会通知多少次，不会因为多个引用指向了同一个对象就合并成一条通知
            while (true) {
                try {
                    Reference removeTarget = noticeQueue.remove();
                    if (removeTarget != null) {
                        // 到引用对象被投递到队列里后，因为已经被回收完成了 get 方法是无法获取指向对象的
                        System.out.println(removeTarget.getClass().getName() + " - " + removeTarget.get() + " 被指向的对象被回收");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public DateBaseProperties getDateBaseProperties() {
        return dateBaseProperties;
    }

    public void setDateBaseProperties(DateBaseProperties dateBaseProperties) {
        this.dateBaseProperties = dateBaseProperties;
    }

    @Override
    public DruidPooledConnection getConnection() throws SQLException {
        return dataSourceAtomicReference.get().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return dataSourceAtomicReference.get().getConnection(username, password);
    }

    @Override
    public DruidPooledConnection getConnectionDirect(long maxWaitMillis) throws SQLException {
        return dataSourceAtomicReference.get().getConnectionDirect(maxWaitMillis);
    }

    @Override
    public <T> T unwrap(Class<T> iface) {
        return dataSourceAtomicReference.get().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return dataSourceAtomicReference.get().isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() {
        return dataSourceAtomicReference.get().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSourceAtomicReference.get().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) {
        dataSourceAtomicReference.get().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() {
        return dataSourceAtomicReference.get().getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSourceAtomicReference.get().getParentLogger();
    }

    public DruidDataSource createDataSource(DateBaseProperties dateBaseProperties) throws SQLException {
        DruidDataSource next = new DruidDataSource();
        next.setDriverClassName("com.mysql.cj.jdbc.Driver");
        next.setUrl(dateBaseProperties.getJdbcURL());
        next.setUsername(dateBaseProperties.getUser());
        next.setPassword(dateBaseProperties.getPassword());

        next.setMinIdle(dateBaseProperties.getMinIdle());
        next.setMaxActive(dateBaseProperties.getMaxActive());
        next.setMaxWait(dateBaseProperties.getMaxWait());

        // 配置监控 Filter
        next.setFilters("stat,wall");
        Properties properties = new Properties();
        properties.setProperty("druid.stat.mergeSql", "true");
        properties.setProperty("druid.stat.slowSqlMillis", "500");
        next.setConnectProperties(properties);
        return next;
    }

    public DruidDataSource refreshDataSource(DateBaseProperties dateBaseProperties) throws SQLException {
        DruidDataSource old = this.dataSourceAtomicReference.get();
        this.dataSourceAtomicReference.set(createDataSource(dateBaseProperties));
        System.out.println(String.format("当前数据库配置(%d)：%s [user]→ %s",
                this.dataSourceAtomicReference.get().hashCode(),
                dateBaseProperties.getJdbcURL(),
                dateBaseProperties.getUser()));
        this.setDateBaseProperties(dateBaseProperties);
        // 弱引用
        WeakReference<Integer> weakReference = new WeakReference(old, noticeQueue);
        return old;
    }
}