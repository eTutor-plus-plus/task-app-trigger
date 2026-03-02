package at.jku.dke.task_app.trigger.evaluation.Snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SnapshotCleanupTask {

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotCleanupTask.class);

    //run once a day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void clearOldSnapshots() {
        BufferedSnapshots.getInstance().removeAll();
    }
}
