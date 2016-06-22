// Autogenerated for User API

#include "../connection.hpp"
#include "../json.hpp"

class UserApi {
private:
  RaptureConnection _connection;
public:
  UserApi(RaptureConnection& connection) : _connection(connection) {}

  json getWhoAmI();
};
