package at.jku.dke.task_app.trigger.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTaskInGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Represents an oracle trigger task.
 */
@Entity
@Table(name = "task")
public class TriggerTask extends BaseTaskInGroup<TriggerTaskGroup> {
    // TODO: add custom task fields (for example see XQuery or BinarySearch task app)

    /**
     * Creates a new instance of class {@link TriggerTask}.
     */
    public TriggerTask() {
    }

    // TODO: add getter and setter for custom task fields
}
