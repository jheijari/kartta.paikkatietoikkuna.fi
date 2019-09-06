package flyway.ptistats;

import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import org.oskari.permissions.PermissionService;
import org.oskari.permissions.PermissionServiceMybatisImpl;
import org.oskari.permissions.model.*;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Inserts viewlayer permission for the kunta & seutukunta layers used as regionsets for the new thematic maps
 */
public class V1_13__link_roles_to_sotkanet_layers implements JdbcMigration {
    private static final Logger LOG = LogFactory.getLogger(V1_13__link_roles_to_sotkanet_layers.class);
    private static final int DUMMY_ID = -1;

    public void migrate(Connection connection)
            throws SQLException {
        if (true) {
            // Can't be run to an empty database. Permission handling changed.
            return;
        }
        PermissionService service = new PermissionServiceMybatisImpl();
        for(Resource resToUpdate : getResources()) {
            Optional<Resource> dbRes = service.findResource(ResourceType.maplayer, resToUpdate.getMapping());
            if(dbRes.isPresent()) {
                resToUpdate = dbRes.get();
            }
            for(Role role : getRoles()) {
                if(resToUpdate.hasPermission(role, PermissionType.VIEW_LAYER)) {
                    // already had the permission
                    continue;
                }
                final Permission permission = new Permission();
                permission.setType(PermissionType.VIEW_LAYER);
                permission.setRoleId((int) role.getId());
                resToUpdate.addPermission(permission);
            }
            service.saveResource(resToUpdate);
        }
    }

    // statslayers described as layer resources for permissions handling
    private List<Resource> getResources() {
        List<Resource> list = new ArrayList<>();
        list.add(new OskariLayerResource(DUMMY_ID));
        list.add(new OskariLayerResource(DUMMY_ID));
        list.add(new OskariLayerResource(DUMMY_ID));
        return list;
    }

    private List<Role> getRoles() {
        List<Role> list = new ArrayList<>();
        try {
            // "logged in" user
            list.add(Role.getDefaultUserRole());
            // guest user
            User guest = UserService.getInstance().getGuestUser();
            list.addAll(guest.getRoles());
        } catch (ServiceException ex) {
            LOG.warn(ex, "Couldn't get roles listing for statistics layers permissions setup");
        }
        return list;
    }

}
