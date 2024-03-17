package at.jku.dke.task_app.trigger.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseSubmission;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Represents an oracle trigger input.
 */
@Entity
@Table(name = "submission")
public class TriggerSubmission extends BaseSubmission<TriggerTask> {
    // TODO: add custom submission (for example see XQuery or BinarySearch task app)

    /**
     * Creates a new instance of class {@link TriggerSubmission}.
     */
    public TriggerSubmission() {
        this.setId(UUID.randomUUID()); // required as oracle cannot generate UUIDs
    }

    // TODO: add getter and setter for custom submission fields
}
