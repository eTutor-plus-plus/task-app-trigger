package at.jku.dke.task_app.trigger.config;

import com.zaxxer.hikari.HikariDataSource;

public class TriggerDatasource implements AutoCloseable {
    private HikariDataSource dataSource;
    private boolean lock;

    public TriggerDatasource(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.lock = false;
    }

    public void setDataSource(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    @Override
    public void close() throws Exception {
        this.dataSource.close();
    }
}
