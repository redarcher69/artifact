package resource.artifact.repositories.database;

import resource.artifact.domains.DataBaseConnectInfo;
import resource.artifact.domains.User;
import resource.artifact.domains.validators.Validator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UserDBRepository extends AbstractDataBaseRepository<Long,User> {

    public UserDBRepository(Validator<User> validator, DataBaseConnectInfo connectInfo, String tableName) {
        super(validator, connectInfo, tableName);
        ReadDataBaseEntries();
    }

    @Override
    public User createEntity(ResultSet resultSet) {
        try {
            User user = new User(resultSet.getString("nume"),
                    resultSet.getString("prenume"),
                    resultSet.getString("password"),
                    resultSet.getString("username"));
            user.setId(resultSet.getLong("id"));
            return user;
        } catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public String addFormat(User entity) {
        return String.format("Insert into %s (nume,prenume,password,username) Values ('%s','%s','%s','%s')",
                getTableName(), entity.getFirstName(), entity.getLastName(),entity.getPassword(),entity.getUsername());
    }

    @Override
    public String delFormat(User entity) {
        return String.format("Delete From %s Where id = %d", getTableName(),entity.getId());
    }

    @Override
    public String updateFormat(User entity) {
        return String.format("Update %s Set nume='%s', prenume='%s', password='%s', username='%s' Where id = %d",
                getTableName(), entity.getFirstName(),entity.getLastName(),entity.getPassword(),entity.getUsername(),entity.getId());
    }

    @Override
    public Optional<User> save(User entity) {
        try{
            Statement statement;
            statement = dataBaseConnection.createStatement();
            String query = String.format("select last_value from %s","public.users_id_seq");
            ResultSet resultSet = statement.executeQuery(query);
            resultSet.next();
            //got the last id from identity
            entity.setId(resultSet.getLong("last_value")+1);


            return super.save(entity);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
