package marquez.db.dao;

import java.util.List;
import marquez.api.Job;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.slf4j.Logger;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.slf4j.LoggerFactory;

// @RegisterRowMapper(JobRow.class)
public interface JobDAO extends SqlObject {
  static final Logger LOG = LoggerFactory.getLogger(JobDAO.class);

  @CreateSqlObject
  OwnershipDAO createOwnershipDAO();

  default void insert(final Job job) {
    try (final Handle handle = getHandle()) {
      handle.useTransaction(
          h -> {
            int jobId =
                h.createUpdate(
                        "INSERT INTO jobs (name, nominal_time, category, description)"
                            + " VALUES (:name, :nominalTime, :category, :description)")
                    .bindBean(job)
                    .executeAndReturnGeneratedKeys()
                    .mapTo(int.class)
                    .findOnly();
            int ownershipId = createOwnershipDAO().insert(job.getName(), job.getOwnerName());
            LOG.info("The job that was created id is: " + jobId);
            LOG.info("The ownership Id that was created id is: " + ownershipId);

            h.createUpdate("UPDATE jobs SET current_ownership = :ownershipId WHERE id = :jobId")
                .bind("ownershipId", ownershipId)
                .bind("jobId", jobId)
                .execute();
          });
    } catch (Exception e) {
      // TODO: Add better error handling
      LOG.error(e.getMessage());
    }
  }

  @SqlQuery("SELECT * FROM jobs WHERE name = :name")
  Job findByName(@Bind("name") String name);

  @SqlQuery("SELECT * FROM jobs LIMIT :limit")
  List<Job> findAll(@Bind("limit") int limit);

  @SqlUpdate("UPDATE jobs SET state = :state")
  int updateJobState(@Bind("state") String state);

  default void update(final Job job, final String jobName) {
    if (job.getState() != null) {
      updateJobState(job.getState());
    } else {
      try (final Handle handle = getHandle()) {
        handle.useTransaction(
            h -> {
              final OwnershipDAO ownershipDAO = createOwnershipDAO();
              ownershipDAO.endOwnership(jobName);
              // int jobId = findByName(jobName).getId();
              int jobId =
                  h.createQuery("SELECT id FROM jobs WHERE name = :jobName")
                      .bind("jobName", jobName)
                      .mapTo(int.class)
                      .findOnly();
              int ownershipId = ownershipDAO.insert(jobName, job.getOwnerName());
              h.createUpdate(
                      "UPDATE jobs SET current_ownership = :ownershipId, updated_at = NOW() WHERE id = :jobId")
                  .bind("ownershipId", ownershipId)
                  .bind("jobId", jobId)
                  .execute();
            });
      } catch (Exception e) {
        // TODO: Add better error handling
        LOG.error(e.getMessage());
      }
    }
  }
}
