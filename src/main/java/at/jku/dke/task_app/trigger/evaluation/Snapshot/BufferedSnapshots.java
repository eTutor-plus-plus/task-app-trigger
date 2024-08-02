package at.jku.dke.task_app.trigger.evaluation.Snapshot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BufferedSnapshots {

    private static BufferedSnapshots instance;
    private static List<Snapshot> snapshotList;

    private BufferedSnapshots() {
        snapshotList = new ArrayList<>();
    }

    public static BufferedSnapshots getInstance() {
        if (instance == null) {
            instance = new BufferedSnapshots();
            snapshotList = new ArrayList<>();
        }
        return instance;
    }

    public Snapshot getSnapshotByTaskIdAndStatement(long taskId, String executionStatement) {
        for (Snapshot snapshot : snapshotList) {
            if (snapshot.getTaskId() == taskId && snapshot.getExecutionStatement().equals(executionStatement)) {
                return snapshot;
            }
        }
        //no snapshot found
        return null;
    }

    public List<Snapshot> getSnapshotsByTaskId(long taskId) {
        List<Snapshot> snapshots = new ArrayList<>();
        for (Snapshot snapshot : snapshotList) {
            if (snapshot.getTaskId() == taskId) {
                snapshots.add(snapshot);
            }
        }
        return snapshots;
    }

    public void addResult(Snapshot snapshot) {
        snapshotList.add(snapshot);
    }

    public void removeSnapshot(Snapshot snapshot) {
        snapshotList.remove(snapshot);
    }

    public void removeByTaskId(long taskId) {
        Iterator<Snapshot> iterator = snapshotList.iterator();
        while (iterator.hasNext()) {
            Snapshot snapshot = iterator.next();
            if (snapshot.getTaskId() == taskId) {
                iterator.remove();
            }
        }
    }

    public void removeAll() {
        snapshotList.clear();
    }

    public List<Snapshot> getSnapshotList() {
        return snapshotList;
    }

    public boolean containsTask(long taskId) {
        for (Snapshot snapshot : snapshotList) {
            if (snapshot.getTaskId() == taskId) {
                return true;
            }
        }
        return false;
    }

    public void addSnapshots(List<Snapshot> snapshots) {
        snapshotList.addAll(snapshots);
    }
}
