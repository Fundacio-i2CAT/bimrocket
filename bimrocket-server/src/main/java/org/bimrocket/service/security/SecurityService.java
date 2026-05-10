/*
 * BIMROCKET
 *
 * Copyright (C) 2021-2025, Ajuntament de Sant Feliu de Llobregat
 *
 * This program is licensed and may be used, modified and redistributed under
 * the terms of the European Public License (EUPL), either version 1.1 or (at
 * your option) any later version as soon as they are approved by the European
 * Commission.
 *
 * Alternatively, you may redistribute and/or modify this program under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either  version 3 of the License, or (at your option)
 * any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the licenses for the specific language governing permissions, limitations
 * and more details.
 *
 * You should have received a copy of the EUPL1.1 and the LGPLv3 licenses along
 * with this program; if not, you may find them at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 * http://www.gnu.org/licenses/
 * and
 * https://www.gnu.org/licenses/lgpl.txt
 */
package org.bimrocket.service.security;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.bimrocket.api.security.Role;
import org.bimrocket.api.security.User;
import org.bimrocket.dao.Dao;
import org.bimrocket.exception.InvalidRequestException;
import org.bimrocket.exception.NotAuthorizedException;
import org.bimrocket.exception.NotFoundException;
import org.bimrocket.service.security.store.SecurityDaoStore;
import org.bimrocket.service.security.store.empty.SecurityEmptyDaoStore;
import org.eclipse.microprofile.config.Config;
import org.bimrocket.service.security.store.SecurityDaoConnection;
import org.bimrocket.util.ExpiringCache;
import static org.bimrocket.service.security.SecurityConstants.*;
import static org.bimrocket.util.TextUtils.getISODate;
import static java.lang.Boolean.FALSE;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.UUID;
import org.bimrocket.dao.expression.Expression;
import org.bimrocket.dao.expression.OrderByExpression;
import org.bimrocket.dao.expression.io.log.LogExpressionPrinter;
import static org.bimrocket.service.security.Credentials.*;
import org.bimrocket.util.EntityDefinition;
import org.bimrocket.util.TextUtils;

/**
 *
 * @author realor
 */
@ApplicationScoped
public class SecurityService
{
  static final Logger LOGGER =
    Logger.getLogger(SecurityService.class.getName());

  static final String BASE = "services.security.";

  public static final Map<String, Field> userFieldMap =
    EntityDefinition.getInstance(User.class).getFieldMap();

  public static final Map<String, Field> roleFieldMap =
    EntityDefinition.getInstance(Role.class).getFieldMap();

  static final String USER_ATTRIBUTE = "_user_";
  static final String CREDENTIALS_ATTRIBUTE = "_credentials_";

// Exceptions

  static final String USER_ALREADY_EXISTS =
    "SEC001: User already exists.";
  static final String USER_NOT_FOUND =
    "SEC002: User not found.";
  static final String USER_IS_NOT_ACTIVE =
    "SEC003: User is not active.";
  static final String CAN_NOT_CHANGE_PASSWORD =
    "SEC004: Can not change password.";
  static final String ID_IS_REQUIRED =
    "SEC005: User id is required.";
  static final String INVALID_PASSWORD_FORMAT =
    "SEC006: Invalid password format.";
  static final String PASSWORD_IS_REQUIRED =
    "SEC007: Password is required.";

  @Inject
  Instance<HttpServletRequest> requestInstance;

  @Inject
  Config config;

  SecurityDaoStore daoStore;
  LdapConnector ldapConnector;

  String adminPassword;

  ExpiringCache<String> credentialsCache;
  ExpiringCache<User> userCache;
  ExpiringCache<Role> roleCache;
  ConcurrentHashMap<Thread, String> userIdByThread;

  long credentialsCacheTimeout; // seconds
  long userCacheTimeout; // seconds
  long roleCacheTimeout; // seconds
  long tokenTimeout; // seconds

  User anonymousUser;

  boolean ldapEnabled;

  @PostConstruct
  public void init()
  {
    LOGGER.log(Level.INFO, "Init SecurityService");

    ldapEnabled = config.getValue(BASE + "ldap.enabled", Boolean.class);

    CDI<Object> cdi = CDI.current();

    if (ldapEnabled)
    {
      ldapConnector = cdi.select(LdapConnector.class).get();
      LOGGER.log(Level.INFO, "LDAP enabled: {0}", ldapConnector.getLdapUrl());
    }

    try
    {
      @SuppressWarnings("unchecked")
      Class<SecurityDaoStore> storeClass =
        config.getValue(BASE + "store.class", Class.class);
      daoStore = cdi.select(storeClass).get();
    }
    catch (Exception ex)
    {
      LOGGER.log(Level.SEVERE, "Error initializing SecurityDaoStore [{0}]: {1}",
        new Object[] {
          config.getOptionalValue(BASE + "store.class", String.class).orElse(null),
          ex.toString()
        });
      daoStore = new SecurityEmptyDaoStore();
    }

    LOGGER.log(Level.INFO, "SecurityDaoStore: {0}", daoStore.getClass());

    adminPassword = config.getValue(BASE + "adminPassword", String.class);

    credentialsCacheTimeout = config.getValue(BASE + "credentialsCacheTimeout", Long.class);
    credentialsCache = new ExpiringCache<>(credentialsCacheTimeout * 1000);
    LOGGER.log(Level.INFO, "credentialsCacheTimeout: {0}", credentialsCacheTimeout);

    userCacheTimeout = config.getValue(BASE + "userCacheTimeout", Long.class);
    userCache = new ExpiringCache<>(userCacheTimeout * 1000);
    LOGGER.log(Level.INFO, "userCacheTimeout: {0}", userCacheTimeout);

    roleCacheTimeout = config.getValue(BASE + "roleCacheTimeout", Long.class);
    roleCache = new ExpiringCache<>(roleCacheTimeout * 1000);
    LOGGER.log(Level.INFO, "roleCacheTimeout: {0}", roleCacheTimeout);

    tokenTimeout = config.getValue(BASE + "tokenTimeout", Long.class);
    LOGGER.log(Level.INFO, "tokenTimeout: {0}", tokenTimeout);

    userIdByThread = new ConcurrentHashMap<>();

    anonymousUser = new User();
    anonymousUser.setId(ANONYMOUS_USER);
    anonymousUser.setName(ANONYMOUS_USER);
    anonymousUser.getRoleIds().add(EVERYONE_ROLE);
  }

  @PreDestroy
  public void destroy()
  {
    LOGGER.log(Level.INFO, "Destroying SecurityService");
    daoStore.close();
  }

  public User validateCredentials(String userId, String password)
  {
    LOGGER.log(Level.FINE, "userId: {0}", userId);

    if (ADMIN_USER.equals(userId)) // admin user
    {
      if (!adminPassword.equals(password))
        throw new NotAuthorizedException();

      User user = getUser(userId); // get from store
      if (user == null)
      {
        user = new User();
        user.setId(userId);
        user.setName(userId);
      }
      return user;
    }

    User user = getUser(userId); // get from store
    if (user == null) // user not found in database
    {
      if (ldapConnector == null)
        throw new NotAuthorizedException();

      user = ldapConnector.validateCredentials(userId, password);

      createUser(user);
      userCache.put(userId, user);
    }
    else // user found in database
    {
      if (FALSE.equals(user.getActive()))
        throw new NotAuthorizedException(USER_IS_NOT_ACTIVE);

      if (user.getPasswordHash() == null) // LDAP User
      {
        if (ldapConnector == null)
          throw new NotAuthorizedException();

        ldapConnector.validateCredentials(userId, password);
      }
      else // check hashed password in User
      {
        String passwordHash = Digester.hash(password);

        if (!user.getPasswordHash().equals(passwordHash))
          throw new NotAuthorizedException();
      }
    }
    return user;
  }

  public String createToken(String userId)
  {
    LOGGER.log(Level.FINE, "userId: {0}", userId);

    String tokenValue = UUID.randomUUID().toString().replace("-", "");
    String hash = Digester.hash(tokenValue);

    Token token = Token.create(hash, userId, 2 * tokenTimeout);
    try (var conn = daoStore.getConnection())
    {
      var tokenDao = conn.getTokenDao();
      tokenDao.insert(token);
    }
    return tokenValue;
  }

  public boolean destroyToken(String tokenValue)
  {
    LOGGER.log(Level.FINE, "token: {0}", tokenValue);

    String hash = Digester.hash(tokenValue);
    credentialsCache.remove(hash);

    try (var conn = daoStore.getConnection())
    {
      var tokenDao = conn.getTokenDao();
      return tokenDao.deleteById(hash);
    }
  }

  public List<User> getUsers(Expression filter, List<OrderByExpression> orderBy)
  {
    LOGGER.log(Level.FINE, "filter: {0}", LogExpressionPrinter.toString(filter));

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getUserDao();
      return userDao.find(filter, orderBy);
    }
  }

  public User getUser(String userId)
  {
    LOGGER.log(Level.FINE, "userId: {0}", userId);

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getUserDao();
      return userDao.findById(userId);
    }
  }

  public User createUser(User user)
  {
    LOGGER.log(Level.FINE, "userId: {0}", user.getId());

    //Send true to parameter isNewUser
    validateUser(user, true);

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getUserDao();
      User prevUser = userDao.findById(user.getId());
      if (prevUser != null)
        throw new InvalidRequestException(USER_ALREADY_EXISTS);

      userCache.remove(user.getId()); // User may exist in cache

      String dateString = getISODate();
      user.setCreationDate(dateString);
      user.setModifyDate(dateString);
      if (user.getActive() == null)
      {
        user.setActive(true);
      }
      return userDao.insert(user);
    }
  }

  public User updateUser(User userUpdate)
  {
    String userId = userUpdate.getId();
    LOGGER.log(Level.FINE, "userId: {0}", userId);

    //Send false to parameter isNewUser
    validateUser(userUpdate, false);

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getUserDao();
      User user = userDao.findById(userUpdate.getId());
      if (user == null) throw new NotFoundException(USER_NOT_FOUND);

      userCache.remove(userId); // User may exist in cache

      user.setName(userUpdate.getName());
      user.setEmail(userUpdate.getEmail());
      if (userUpdate.getActive() != null)
      {
        user.setActive(userUpdate.getActive());
      }
      user.setRoleIds(userUpdate.getRoleIds());
      if (!StringUtils.isBlank(userUpdate.getPasswordHash()))
      {
        user.setPasswordHash(userUpdate.getPasswordHash());
      }
      String dateString = getISODate();
      user.setModifyDate(dateString);
      user = userDao.update(user);
      return user;
    }
  }

  public boolean deleteUser(String userId)
  {
    LOGGER.log(Level.FINE, "userId: {0}", userId);
    userCache.remove(userId);

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getUserDao();
      return userDao.deleteById(userId);
    }
  }

  public List<Role> getRoles(Expression filter, List<OrderByExpression> orderBy)
  {
    LOGGER.log(Level.FINE, "filter: {0}", LogExpressionPrinter.toString(filter));

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getRoleDao();
      return userDao.find(filter, orderBy);
    }
  }

  public Role getRole(String roleId)
  {
    LOGGER.log(Level.FINE, "roleId: {0}", roleId);

    try (var conn = daoStore.getConnection())
    {
      var roleDao = conn.getRoleDao();
      return roleDao.findById(roleId);
    }
  }

  public Role createRole(Role role)
  {
    LOGGER.log(Level.FINE, "roleId: {0}", role.getId());

    try (var conn = daoStore.getConnection())
    {
      var roleDao = conn.getRoleDao();
      return roleDao.insert(role);
    }
  }

  public Role updateRole(Role role)
  {
    LOGGER.log(Level.FINE, "roleId: {0}", role.getId());
    roleCache.remove(role.getId());

    try (var conn = daoStore.getConnection())
    {
      var roleDao = conn.getRoleDao();
      return roleDao.update(role);
    }
  }

  public boolean deleteRole(String roleId)
  {
    LOGGER.log(Level.FINE, "roleId: {0}", roleId);
    roleCache.remove(roleId);

    try (var conn = daoStore.getConnection())
    {
      var roleDao = conn.getRoleDao();
      return roleDao.deleteById(roleId);
    }
  }

  public void changePassword(String userId,
    String oldPassword, String newPassword)
  {
    LOGGER.log(Level.FINE, "userId: {0}", userId);

    if (ADMIN_USER.equals(userId) ||
        ANONYMOUS_USER.equals(userId) ||
        StringUtils.isBlank(newPassword))
      throw new InvalidRequestException(CAN_NOT_CHANGE_PASSWORD);

    try (SecurityDaoConnection conn = daoStore.getConnection())
    {
      Dao<User, String> userDao = conn.getUserDao();
      User user = userDao.findById(userId);

      if (user == null ||
          !Objects.equals(Digester.hash(oldPassword), user.getPasswordHash()))
        throw new InvalidRequestException(CAN_NOT_CHANGE_PASSWORD);

      checkPasswordFormat(newPassword);
      user.setPasswordHash(Digester.hash(newPassword));

      userDao.update(user);
    }
  }

  public void setCurrentUserId(String userId)
  {
    Thread currentThread = Thread.currentThread();
    if (userId == null)
    {
      userIdByThread.remove(currentThread);
    }
    else
    {
      userIdByThread.put(currentThread, userId);
    }
  }

  public String getCurrentUserId()
  {
    User user = getCurrentUser();
    return user.getId();
  }

  public void setCredentials(Credentials credentials)
  {
    HttpServletRequest request = requestInstance.get();
    request.setAttribute(CREDENTIALS_ATTRIBUTE, credentials);
  }

  public User getCurrentUser()
  {
    User user = getUserFromThread();
    if (user == null)
    {
      return getUserFromRequest();
    }
    return anonymousUser;
  }

  /* private methods */

  private User getUserFromRequest()
  {
    HttpServletRequest request = requestInstance.get();
    User user = (User)request.getAttribute(USER_ATTRIBUTE);
    if (user != null) return user;

    Credentials credentials =
      (Credentials)request.getAttribute(CREDENTIALS_ATTRIBUTE);
    if (credentials == null) return anonymousUser;

    String userId = credentialsCache.get(credentials.getHash());
    if (userId != null)
    {
      user = userCache.get(userId);
      if (user != null) return user;
    }

    user = getUserFromCredentials(credentials);
    userId = user.getId().trim();
    request.setAttribute(USER_ATTRIBUTE, user);

    if (ANONYMOUS_USER.equals(userId)) return anonymousUser;

    addUserRoles(user);
    credentialsCache.put(credentials.getHash(), userId);
    userCache.put(userId, user);

    LOGGER.log(Level.FINE, "User {0} identified with roles {1}",
      new Object[] { userId, user.getRoleIds() });

    return user;
  }

  private User getUserFromThread()
  {
    String userId = userIdByThread.get(Thread.currentThread());
    if (userId != null)
    {
      User user = userCache.get(userId);
      if (user != null) return user;

      user = getUser(userId);
      addUserRoles(user);

      userCache.put(userId, user);

      return user;
    }
    return null;
  }

  private User getUserFromCredentials(Credentials credentials)
  {
    switch (credentials.getType())
    {
      case BASIC:
        String userPassword = credentials.getValue();
        String decoded = new String(Base64.getDecoder().decode(userPassword));
        String[] userPasswordParts = decoded.split(":");
        String userId = userPasswordParts.length > 0 ? userPasswordParts[0] : null;
        String password = userPasswordParts.length > 1 ? userPasswordParts[1] : null;

        if (userId == null || ANONYMOUS_USER.equals(userId))
          return anonymousUser;

        return validateCredentials(userId, password);

      case BEARER:
      case COOKIE:
        return getUserFromToken(credentials.getHash());
    }
    return anonymousUser;
  }

  private User getUserFromToken(String tokenHash)
  {
    try (var conn = daoStore.getConnection())
    {
      var tokenDao = conn.getTokenDao();
      var userDao = conn.getUserDao();

      var token = tokenDao.findById(tokenHash);

      if (token == null) return anonymousUser;

      Date expirationDate = TextUtils.parseISODate(token.getExpirationDate());
      Date now = new Date();

      if (now.after(expirationDate)) return anonymousUser;

      String userId = token.getUserId();

      User user;
      if (ANONYMOUS_USER.equals(userId))
      {
        return anonymousUser;
      }
      else if (ADMIN_USER.equals(userId))
      {
        user = new User();
        user.setId(userId);
        user.setName(userId);
      }
      else
      {
        user = userDao.findById(userId);
        if (user == null) throw new NotAuthorizedException(USER_NOT_FOUND);

        if (FALSE.equals(user.getActive()))
          throw new NotAuthorizedException(USER_IS_NOT_ACTIVE);
      }

      long timeToExpiration = expirationDate.getTime() - now.getTime();
      if (timeToExpiration < 1000 * tokenTimeout)
      {
        token.updateExpirationDate(2 * tokenTimeout);
        tokenDao.update(token);
      }
      return user;
    }
  }

  private void addUserRoles(User user)
  {
    String userId = user.getId();
    explodeRoles(user.getRoleIds()); // explodeRoles
    user.getRoleIds().add(userId); // add nominal role;
    user.getRoleIds().add(EVERYONE_ROLE);
    user.getRoleIds().add(AUTHENTICATED_ROLE);
    if (ADMIN_USER.equals(userId))
    {
      user.getRoleIds().add(ADMIN_ROLE);
    }
  }

  private void validateUser(User user, boolean isNewUser)
  {
    if (StringUtils.isBlank(user.getId()))
      throw new InvalidRequestException(ID_IS_REQUIRED);

    String password = user.getPassword();
    if (!ldapEnabled && StringUtils.isBlank(password) && isNewUser)
    {
      throw new InvalidRequestException(PASSWORD_IS_REQUIRED);
    }

    if (!StringUtils.isBlank(password))
    {
      checkPasswordFormat(password);
      user.setPasswordHash(Digester.hash(password));
      user.setPassword(null);
    }
  }

  private void checkPasswordFormat(String password)
  {
    String passwordPattern =
      config.getValue(BASE + "passwordPattern", String.class);

    if (!password.matches(passwordPattern))
      throw new InvalidRequestException(INVALID_PASSWORD_FORMAT);
  }

  private void explodeRoles(Set<String> roleIds)
  {
    Stack<String> stack = new Stack<>();
    stack.addAll(roleIds);
    while (!stack.isEmpty())
    {
      String roleId = stack.pop();
      Role role = roleCache.get(roleId);
      if (role == null)
      {
        role = getRole(roleId);
        if (role == null)
        {
          // put non peristent Role in cache
          role = new Role();
          role.setId(roleId);
        }
        roleCache.put(roleId, role);
      }

      for (String subRoleId : role.getRoleIds())
      {
        if (!roleIds.contains(subRoleId))
        {
          stack.add(subRoleId);
          roleIds.add(subRoleId);
        }
      }
    }
  }
}
