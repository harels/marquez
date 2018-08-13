package marquez.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface OwnershipDAO {
  static final Logger LOG = LoggerFactory.getLogger(OwnershipDAO.class);

  @SqlUpdate(
      "INSERT INTO ownerships (job_id, owner_id) "
          + "VALUES ("
          + "(SELECT id FROM jobs WHERE name = :jobName),"
          + "(SELECT id FROM owners WHERE name = :ownerName)"
          + ")")
  @GetGeneratedKeys
  int insert(@Bind("jobName") String jobName, @Bind("ownerName") String ownerName);
}
