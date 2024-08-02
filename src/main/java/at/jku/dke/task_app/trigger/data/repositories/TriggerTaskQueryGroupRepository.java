package at.jku.dke.task_app.trigger.data.repositories;

import at.jku.dke.task_app.trigger.data.entities.TriggerTaskGroup;
import at.jku.dke.task_app.trigger.data.entities.TriggerTaskGroupQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for entity {@link TriggerTaskGroupQuery}.
 */
public interface TriggerTaskQueryGroupRepository extends JpaRepository<TriggerTaskGroupQuery, UUID>{
    /**
     * Finds all {@link TriggerTaskGroupQuery}s by the specified task group.
     *
     * @param id The task group identifier.
     * @return The {@link TriggerTaskGroupQuery}s.
     */
    List<TriggerTaskGroupQuery> findByTaskGroup_Id(Long id);

    /**
     * Finds a {@link TriggerTaskGroupQuery} by its table name and task group identifier.
     *
     * @param tableName The table name.
     * @param id        The task group identifier.
     * @return The {@link TriggerTaskGroupQuery} if found.
     */
    @Query("select t from TriggerTaskGroupQuery t where upper(t.tableName) = upper(?1) and t.taskGroup.id = ?2")
    Optional<TriggerTaskGroupQuery> findByTableNameIgnoreCaseAndTaskGroup_Id(String tableName, Long id);

    /**
     * Deletes all {@link TriggerTaskGroupQuery}s by the specified task group and not in the specified collection of identifiers.
     *
     * @param taskGroup The task group.
     * @param ids       The identifiers.
     * @return The number of deleted {@link TriggerTaskGroupQuery}s.
     */
    long deleteByTaskGroupAndIdNotIn(TriggerTaskGroup taskGroup, Collection<UUID> ids);
}
