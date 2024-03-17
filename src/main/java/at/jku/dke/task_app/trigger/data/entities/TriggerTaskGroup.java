package at.jku.dke.task_app.trigger.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTaskGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Represents an oracle trigger task group.
 */
@Entity
@Table(name = "task_group")
public class TriggerTaskGroup extends BaseTaskGroup {
    // TODO: add custom task group fields (for example see XQuery or BinarySearch task app)

    /**
     * Creates a new instance of class {@link TriggerTaskGroup}.
     */
    public TriggerTaskGroup() {
    }

    // TODO: add getter and setter for custom task group fields
}
